## Context

The PWA was introduced in `3954c610` with `vite-plugin-pwa` (`generateSW`, precache of the whole build and `navigateFallback: '/index.html'`) plus an in-app update prompt. `74d736e1` removed the prompt and the `useRegisterSW` call and switched to `registerType: 'autoUpdate'`, but kept `injectRegister: false`. With `injectRegister: false` the plugin does **not** set `skipWaiting` / `clientsClaim` (see `vite-plugin-pwa/dist/index.js`, `injectRegister === 'auto'` condition). The current state is:

| Client | What happens |
|---|---|
| New visitor | No SW is ever registered (manifest only). |
| Visitor with SW from Apr–Jun builds | Old SW serves precached `index.html` on every navigation. A new SW installs and then waits forever, because nothing sends `SKIP_WAITING` and a reload does not activate a waiting worker. It activates only after all windows are closed, which rarely happens for an installed app on mobile. |
| `pwa` profile off | `/sw.js` returns 404. A failed update check keeps the old registration alive, so stale clients stay stale. |
| Any browser | `index.html` has `max-age=3600, public`. After a deploy CI deletes old hashed assets, so a cached shell can load 404 scripts and render a blank page. |

The only bundle is `index-<hash>.js`. There are no dynamic imports, so chunk-load failures beyond the shell itself are not a concern.

## Goals / Non-Goals

**Goals:**
- A deployed release reaches every client on its next navigation or launch. An installed app that is resumed from the background picks it up when it is resumed.
- A registered, self-updating service worker exists on every PWA-enabled client, as the foundation for web push.
- Existing stuck clients recover automatically, without user action.
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
  manifest: { id: '/', /* existing fields */ },
})
```

`injectionPoint: undefined` skips `self.__WB_MANIFEST` injection, so no build asset list ends up in the worker. Verify this against the installed plugin version during implementation. `sw.ts` contains only `install` → `skipWaiting()`, `activate` → `clients.claim()` + D3 migration, and later the push handlers.

*Alternatives:*
- Keep `generateSW` with `globPatterns: []` and no `navigateFallback`. This still emits a Workbox runtime and cannot host push handlers without `importScripts`.
- Use `registerType: 'autoUpdate'` + `injectRegister: 'auto'`. This keeps the precache, so clients stay "one load behind" and the stale-shell problem stays.

Without a fetch handler, every request goes straight to the network and HTTP caching (D4) alone controls freshness. Current Chromium does not require a fetch handler for installability. Verify on Android Chrome and desktop Chrome/Edge (task 5.x).

### D2: Own registration module instead of `virtual:pwa-register`

`src/pwa/registerServiceWorker.ts`:
- runs only when `import.meta.env.PROD` is true,
- probes `HEAD /manifest.webmanifest` and skips registration on non-OK (`pwa` profile off),
- calls `navigator.serviceWorker.register('/sw.js', { scope: '/', updateViaCache: 'none' })`,
- exposes a `getServiceWorkerRegistration(): Promise<ServiceWorkerRegistration | null>` that the notification change will reuse.

It is called once from `main.tsx`, outside React, so StrictMode cannot double-register. Workbox-window is not needed because there is no prompt and no `SKIP_WAITING` message.

### D3: One-time migration of clients with the old precaching worker

In `activate` the new worker deletes every cache whose name starts with `workbox-` or equals `klabis-fonts`. If at least one existed, it was replacing the old precaching worker, so it calls `client.navigate(client.url)` for all window clients. After that, the shells come from the network. The next activations find no such caches and do not reload.

The stuck clients' waiting worker is replaced: the browser's update check fetches the new `sw.js` (byte-different), installs it as the new waiting/installing worker, and `skipWaiting()` activates it immediately.

### D4: Cache-Control policy

| Resource | Policy |
|---|---|
| `/assets/**` (hashed) | `max-age=31536000, public, immutable` (unchanged) |
| `/sw.js`, `/manifest.webmanifest` | `no-cache, must-revalidate` (unchanged); drop the `/workbox-*.js` and `/registerSW.js` patterns |
| `/index.html` (also reached via `/`, `SpaFallbackFilter` forward, `/auth/callback` forward) | **`no-cache`** (new) |
| `/silent-renew.html` | **`no-cache`** (new) |
| other static files (icons, favicon) | `max-age=3600, public` (unchanged) |

Implemented as an additional `addResourceHandler("/index.html", "/silent-renew.html")` with `CacheControl.noCache()` before the `/**` handler. `PathPattern` specificity makes it win over `/**`. Forwards go through the same handler mapping, so they inherit the header. `no-cache` keeps ETag/Last-Modified revalidation, so the cost is a 304 per navigation.

### D5: Kill-switch worker when the `pwa` profile is off

`PwaDisabledController` keeps returning 404 for `/manifest.webmanifest`, so the app is not installable. For `/sw.js` it returns a static script (`classpath:/pwa/sw-disabled.js`, `Content-Type: text/javascript`, `Cache-Control: no-cache`). The script:
1. calls `skipWaiting()`,
2. deletes the caches named in D3,
3. calls `registration.unregister()`,
4. navigates the window clients.

The frontend never registers it because the D2 manifest probe fails. Browsers only receive it through the update check of an already-installed worker. The `/workbox-{hash}.js` mapping is removed.

*Alternative:* drop the `pwa` profile and always enable PWA. This is out of scope here (see Open Questions).

### D6: Reload a stale bundle when an installed app is resumed

`src/pwa/staleBundleReload.ts`: on `visibilitychange` → `visible`, if the page was hidden for ≥ 15 minutes, it fetches `/index.html` with `cache: 'no-store'`, extracts the `/assets/index-*.js` script `src` and compares it with the `src` of the module script in the current document. If they differ, it calls `location.reload()`. It is cheap, needs no build-id plumbing, and runs only in PROD. The 15-minute threshold keeps a quick app switch (e.g. copying a code from SMS) from losing form input.

## Risks / Trade-offs

- **A one-time reload of open windows during migration (D3/D5) can lose unsaved form input.** → It happens once per client, only for clients with the old worker, and usually on the navigation that triggered the update check.
- **Without a fetch handler some browser might refuse install.** → Manual verification task. Fallback: add an empty `fetch` listener (no `respondWith`). Chromium logs a "no-op fetch handler" warning but installs.
- **`no-cache` on HTML adds one conditional request per navigation.** → Negligible at the expected load of 10–20 concurrent users.
- **D6 reload after ≥ 15 min hidden can still interrupt a half-filled form.** → Acceptable: the alternative is an old bundle talking to a new API.
- **`injectionPoint: undefined` might not be supported by the installed plugin version.** → Fallback: reference `self.__WB_MANIFEST` in a no-op expression (`void self.__WB_MANIFEST`) and keep `globPatterns: []`.

## Migration Plan

1. Deploy backend + frontend together (single artifact).
2. Clients with the old worker: the next navigation triggers an update check → the new worker activates → caches are deleted → the windows reload once → fresh shell.
3. Rollback: redeploy the previous artifact. The old `generateSW` worker would re-install with its precache; no data is at risk.

## Open Questions

- Keep the `pwa` Spring profile as a deployment switch, or enable PWA unconditionally? Default in this change: **keep**. Production must have `pwa` active for installation, so verify the production config.
- Keep `orientation: 'portrait'` in the manifest? It locks rotation on tablets. Unchanged in this change.
