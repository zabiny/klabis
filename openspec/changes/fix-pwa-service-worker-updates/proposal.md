## Why

Users of the installable app (PWA) stay on stale frontend releases for days or weeks. The precaching service worker serves an old `index.html` for every navigation, and its replacement never activates. Since commit `74d736e1` nothing registers the service worker or sends `SKIP_WAITING`. `registerType: 'autoUpdate'` has no effect together with `injectRegister: false`. On top of that, `index.html` is served with `Cache-Control: max-age=3600, public`, so even browsers without a service worker can load an outdated shell for up to an hour after a deploy. That shell then references deleted hashed assets and renders a blank page.

Offline mode is not a requirement. The precache is therefore all cost and no benefit, and it must go before web push notifications (which need a reliably registered and updating service worker) can be built on top of it.

## What Changes

```mermaid
flowchart LR
    subgraph Before
        A[Navigation] --> B[SW NavigationRoute<br/>precached index.html]
        B --> C[stale shell]
        D[new sw.js] --> E[waiting forever]
    end
    subgraph After
        F[Navigation] --> G[network<br/>index.html no-cache]
        H[new sw.js] --> I[skipWaiting + clientsClaim<br/>no fetch handler]
    end
```

- **Frontend – service worker:** switch `vite-plugin-pwa` from `generateSW` to `injectManifest` with a hand-written `src/sw.ts`. It has no precache, no navigation route and no fetch handler. It calls `skipWaiting()` + `clients.claim()`. On activation it deletes leftover `workbox-precache-*` / runtime caches. If it found any, it reloads the open windows once to evict stale shells.
- **Frontend – registration:** always register `/sw.js` (scope `/`, `updateViaCache: 'none'`) when the PWA manifest is available, from a small module the future notification feature can reuse.
- **Frontend – stale bundle on resume:** an installed app that is resumed from the background keeps old JS in memory. When the app becomes visible again after being hidden for a while and a newer build is deployed, it reloads.
- **Frontend – manifest:** add `id: '/'`. Drop the now-obsolete `workbox` options (`globPatterns`, `navigateFallback*`, `runtimeCaching`).
- **Backend – cache headers:** serve `index.html` (direct, `/`, SPA forwards, `/auth/callback`) and `silent-renew.html` with `Cache-Control: no-cache`. The existing policies for `/assets/**`, `sw.js` and the manifest stay.
- **Backend – PWA disabled profile:** when the `pwa` profile is off, `/sw.js` serves a kill-switch service worker instead of 404. A 404 leaves already-installed workers alive. The manifest keeps returning 404.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/non-functional-requirements/spec.md`: no requirement on PWA, service worker, offline or HTTP caching.
- `openspec/specs/users-authentication/spec.md`: unaffected. The OAuth2 flows (`/oauth2`, `/login`, `/auth/callback`, `silent-renew.html`) keep working the same way. Only the caching of the shell changes.
- `openspec/specs/application-navigation/spec.md`, `openspec/specs/dashboard/spec.md`: unaffected. No navigation or UI change.
- No other spec mentions PWA installation, service workers or offline behavior (grep for `pwa|service worker|offline|install`).

**Why no spec update is needed:**
The change fixes delivery infrastructure: how fast a deployed release reaches the browser. Features, API contracts, authorization and UI stay the same. The app remains installable under the `pwa` profile and not installable without it. Offline support was never specified and is not offered today in any working form.

## Impact

- `frontend/vite.config.ts`, new `frontend/src/sw.ts`, new SW registration and stale-bundle modules, `frontend/src/main.tsx`, `frontend/tsconfig*.json` (WebWorker lib for `sw.ts`).
- `backend/.../common/ui/FrontendConfiguration.java`, `PwaDisabledController.java`, new kill-switch script resource.
- Regenerated static resources in `backend/src/main/resources/static/` (`sw.js`; `workbox-*.js` no longer emitted).
- One-time migration: existing clients with the old precaching worker get reloaded once after the new worker activates.
- Developer docs: see `docs/pwa-offline-mode.md` for how to re-introduce precaching later.
