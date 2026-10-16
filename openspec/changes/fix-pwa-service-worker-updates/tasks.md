## 1. Backend – cache headers and kill-switch

- [ ] 1.1 Extend `WebContentRoutingIntegrationTest` (or a new test in `common.ui`) with header assertions: `Cache-Control: no-cache` for `/`, `/index.html`, an SPA route (e.g. `/members`), `/auth/callback` and `/silent-renew.html`; the `/assets/**` immutable policy and the 1h icon policy stay unchanged (red)
- [ ] 1.2 Add the `/index.html`, `/silent-renew.html` resource handler with `CacheControl.noCache()` in `FrontendConfiguration`; drop the `/workbox-*.js` and `/registerSW.js` patterns (green)
- [ ] 1.3 Test for profile `!pwa`: `/manifest.webmanifest` → 404; `/sw.js` → 200, `text/javascript`, `no-cache`, body contains `unregister` (red)
- [ ] 1.4 Add `classpath:/pwa/sw-disabled.js` kill-switch (design D5) and serve it from `PwaDisabledController`; remove the `/workbox-{hash}.js` mapping (green)

## 2. Frontend – service worker

- [ ] 2.1 Create `src/sw.ts` (design D1 + D3): `install` → `skipWaiting()`, `activate` → delete `workbox-*` / `klabis-fonts` caches, `clients.claim()`, navigate window clients only if a legacy cache was found; type it via `/// <reference lib="webworker" />`
- [ ] 2.2 Switch `vite.config.ts` to `strategies: 'injectManifest'` (`srcDir: 'src'`, `filename: 'sw.ts'`, `injectManifest.injectionPoint: undefined`); remove the `workbox` block and `registerType`; add `manifest.id: '/'`
- [ ] 2.3 `npm run build` and confirm `dist/sw.js` contains no precache list, no `NavigationRoute`, and no `workbox-*.js` is emitted; `dist/manifest.webmanifest` contains `"id":"/"`

## 3. Frontend – registration and stale-bundle reload

- [ ] 3.1 Unit tests for `src/pwa/registerServiceWorker.ts`: skips in non-PROD, skips when the manifest probe is non-OK or `serviceWorker` is unsupported, registers `/sw.js` with `{scope: '/', updateViaCache: 'none'}` otherwise, and `getServiceWorkerRegistration()` resolves accordingly (red)
- [ ] 3.2 Implement `registerServiceWorker.ts` (design D2) and call it once from `main.tsx` outside the React tree (green)
- [ ] 3.3 Unit tests for `src/pwa/staleBundleReload.ts`: no fetch when hidden < 15 min; reload when the fetched `index.html` references a different `/assets/index-*.js`; no reload when equal or the fetch fails (red)
- [ ] 3.4 Implement `staleBundleReload.ts` (design D6) and install it from `main.tsx` in PROD only (green)
- [ ] 3.5 Run frontend tests, lint and `npm run build` (not just `tsc`)

## 4. Publish resources

- [ ] 4.1 `npm run refresh-backend-server-resources`; remove the stale `workbox-*.js` from `backend/src/main/resources/static/`
- [ ] 4.2 Run the backend tests of the `common` module

## 5. Manual verification (Chrome DevTools → Application)

- [ ] 5.1 Migration: check out a pre-change build with the `pwa` profile, open the app so the old SW is installed, deploy this change, navigate → the new SW activates without closing the tab, `workbox-precache-*` caches are gone, and the page reloaded once
- [ ] 5.2 Update propagation: change a visible string, redeploy, reload once → the new string is shown (no "one load behind")
- [ ] 5.3 Kill-switch: run with the `pwa` profile off on a client that has the SW installed → the SW is unregistered, caches are deleted, and the app still works
- [ ] 5.4 Installability: the install prompt is offered on desktop Chrome/Edge and Android Chrome; if not, apply the empty fetch-listener fallback (design Risks)
- [ ] 5.5 OAuth2: login, logout and silent renew still work with the SW active
- [ ] 5.6 Confirm the `pwa` profile is active in the production deployment configuration
