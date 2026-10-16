## Context

The PWA was introduced in `3954c610` with `vite-plugin-pwa` (`generateSW`, precache of the whole build and `navigateFallback: '/index.html'`) plus an in-app update prompt. `74d736e1` removed the prompt and the `useRegisterSW` call and switched to `registerType: 'autoUpdate'`, but kept `injectRegister: false`. With `injectRegister: false` the plugin does **not** set `skipWaiting` / `clientsClaim` (see `vite-plugin-pwa/dist/index.js`, `injectRegister === 'auto'` condition). The current state is:

| Client | What happens |
|---|---|
| New visitor | No SW is ever registered (manifest only). |
| Visitor with SW from Apr–Jun builds | Old SW serves precached `index.html` on every navigation. A new SW installs and then waits forever, because nothing sends `SKIP_WAITING` and a reload does not activate a waiting worker. It activates only after all windows are closed, which rarely happens for an installed app on mobile. |
| `pwa` profile off (local/test only) | `/sw.js` returns 404. A failed update check keeps the old registration alive, so stale clients stay stale. |
| Any browser | `index.html` has `max-age=3600, public`. After a deploy CI deletes old hashed assets, so a cached shell can load 404 scripts and render a blank page. |

The only bundle is `index-<hash>.js`. There are no dynamic imports, so chunk-load failures beyond the shell itself are not a concern.

## Goals / Non-Goals

**Goals:**
- A deployed release reaches every client on its next navigation or launch. An installed app that is resumed from the background picks it up when it is resumed.
- A registered, self-updating service worker exists on every PWA-enabled client, as the foundation for web push.
- Clean browsers are the target. A browser with the old precaching worker still recovers without user action after one extra navigation (D3).
- Correct `Cache-Control` for every PWA-relevant resource.

**Non-Goals:**
- Offline mode, precaching or runtime caching. These are documented for later in `docs/pwa-offline-mode.md`.
- Push or local notifications (separate change `add-web-push-notifications`).
- An in-app "new version available" UI.

## Decisions

### D1: No precache, no fetch handler — `injectManifest` with a hand-written `src/sw.ts`

`vite-plugin-pwa` stays, for manifest generation, `<link rel="manifest">` injection and compiling `src/sw.ts` → `dist/sw.js`. Config:

```ts
VitePWA({
  strategies: 'injectManifest',
  srcDir: 'src',
  filename: 'sw.ts',
  injectRegister: false,
  injectManifest: { injectionPoint: undefined },
  devOptions: { enabled: true },              // D7
  manifest: { id: '/', /* existing fields */ },
})
```

`injectionPoint: undefined` skips `self.__WB_MANIFEST` injection, so no build asset list ends up in the worker. Verify this against the installed plugin version during implementation. `sw.ts` contains only `install` → `skipWaiting()`, `activate` → `clients.claim()`, and later the push handlers.

*Alternatives:*
- Keep `generateSW` with `globPatterns: []` and no `navigateFallback`. This still emits a Workbox runtime and cannot host push handlers without `importScripts`.
- Use `registerType: 'autoUpdate'` + `injectRegister: 'auto'`. This keeps the precache, so clients stay "one load behind" and the stale-shell problem stays.

Without a fetch handler, every request goes straight to the network and HTTP caching (D4) alone controls freshness. Current Chromium does not require a fetch handler for installability. Verify on Android Chrome and desktop Chrome/Edge (task 5.x).

### D2: Own registration module instead of `virtual:pwa-register`

`src/pwa/registerServiceWorker.ts`:
- skips registration when `serviceWorker` is unsupported,
- calls `navigator.serviceWorker.register(url, { scope: '/', updateViaCache: 'none' })` with `url = import.meta.env.DEV ? '/dev-sw.js?dev-sw' : '/sw.js'`. The dev URL is where `vite-plugin-pwa` serves the worker when `devOptions.enabled` (D7). `sw.ts` has no `import` statements, so the default `classic` type works in both modes and in non-Chromium browsers.
- exposes a `getServiceWorkerRegistration(): Promise<ServiceWorkerRegistration | null>` that the notification change will reuse.

It is called once from `main.tsx`, outside React, so StrictMode cannot double-register. Workbox-window is not needed because there is no prompt and no `SKIP_WAITING` message.

### D3: No migration code for clients with the old precaching worker

The worker carries no one-time cleanup: the change assumes clean browsers. A browser that still has the old precaching worker heals itself without extra code. Its next navigation is still served from the old precache, but the update check that follows fetches the new (byte-different) `sw.js`. The new worker replaces any waiting worker, `skipWaiting()` activates it and `clients.claim()` takes over. From the following navigation on, the shell comes from the network. The old `workbox-*` / `klabis-fonts` caches stay as unused storage until the site data is cleared.

*Alternative:* delete those caches in `activate` and reload the windows once. This was rejected: it is code for a transition only, and its `workbox-` prefix match would also wipe the caches of a future precaching worker (`docs/pwa-offline-mode.md`).

### D4: Cache-Control policy

| Resource | Policy |
|---|---|
| `/assets/**` (hashed) | `max-age=31536000, public, immutable` (unchanged) |
| `/sw.js`, `/manifest.webmanifest` | `no-cache, must-revalidate` (unchanged); drop the `/workbox-*.js` and `/registerSW.js` patterns |
| `/index.html` (also reached via `/`, `SpaFallbackFilter` forward, `/auth/callback` forward) | **`no-cache`** (new) |
| `/silent-renew.html` | **`no-cache`** (new) |
| other static files (icons, favicon) | `max-age=3600, public` (unchanged) |

Implemented as an additional `addResourceHandler("/index.html", "/silent-renew.html")` with `CacheControl.noCache()` before the `/**` handler. `PathPattern` specificity makes it win over `/**`. Forwards go through the same handler mapping, so they inherit the header. `no-cache` keeps ETag/Last-Modified revalidation, so the cost is a 304 per navigation.

### D5: Remove the `pwa` profile

The `pwa` profile (`PwaDisabledController`, which returned 404 for the manifest and worker when the profile was off) was added because the precaching worker made local development on `:8443` painful: after a rebuild, the old shell kept being served. With D1 the worker caches nothing, so that cause is gone. The profile was active in production, so removing it changes nothing there.

- Delete `PwaDisabledController`. The manifest and `sw.js` are served in every profile with the D4 headers.
- Local/test environments that never had the profile start registering the (inert) worker. Developers with an old precaching worker in their browser on `:8443` heal as described in D3, or clear the site data (DevTools → Application → Storage → Clear site data).
- **Emergency rollback** without a profile: build with `vite-plugin-pwa` `selfDestroying: true`. It emits a `sw.js` that unregisters itself and clears caches. A 404 on `/sw.js` must never be used for this, because browsers keep the old registration.

*Alternative:* keep the profile with a kill-switch worker instead of 404. This was rejected because it adds a runtime switch that no environment needs any more.

### D7: Development mode

`devOptions: { enabled: true }` serves the manifest and the worker (compiled from `src/sw.ts`) on the Vite dev server at `/dev-sw.js?dev-sw`. Because the worker has no fetch handler, it cannot interfere with HMR or the `/api` proxy. `localhost` is a secure context, so permission and push subscription also work there, which the notification change relies on. Changing `sw.ts` triggers a full page reload (plugin behavior), and HMR inside the worker is not supported.

### D6: Reload a stale bundle when an installed app is resumed

`src/pwa/staleBundleReload.ts`: on `visibilitychange` → `visible`, if the page was hidden for ≥ 15 minutes, it fetches `/index.html` with `cache: 'no-store'`, extracts the `/assets/index-*.js` script `src` and compares it with the `src` of the module script in the current document. If they differ, it calls `location.reload()`. It is cheap, needs no build-id plumbing, and runs only in PROD. The 15-minute threshold keeps a quick app switch (e.g. copying a code from SMS) from losing form input.

## Risks / Trade-offs

- **A browser with the old precaching worker shows the old release for one more navigation and keeps its old caches (D3).** → Acceptable: the change assumes clean browsers, and clearing the site data removes the leftovers.
- **Without a fetch handler some browser might refuse install.** → Manual verification task. Fallback: add an empty `fetch` listener (no `respondWith`). Chromium logs a "no-op fetch handler" warning but installs.
- **`no-cache` on HTML adds one conditional request per navigation.** → Negligible at the expected load of 10–20 concurrent users.
- **D6 reload after ≥ 15 min hidden can still interrupt a half-filled form.** → Acceptable: the alternative is an old bundle talking to a new API.
- **`injectionPoint: undefined` might not be supported by the installed plugin version.** → Fallback: reference `self.__WB_MANIFEST` in a no-op expression (`void self.__WB_MANIFEST`) and keep `globPatterns: []`.

## Migration Plan

1. Deploy backend + frontend together (single artifact).
2. Clients with the old worker (not expected): the next navigation triggers an update check → the new worker activates → the navigation after that gets a fresh shell (D3).
3. Remove `pwa` from the production `SPRING_PROFILES_ACTIVE` (harmless if left: the profile no longer exists in code).
4. Rollback: redeploy the previous artifact (with the `pwa` profile re-added). The old `generateSW` worker would re-install with its precache; no data is at risk.

## Open Questions

- Keep `orientation: 'portrait'` in the manifest? It locks rotation on tablets. Unchanged in this change.
