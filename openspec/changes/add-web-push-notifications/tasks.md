## 1. Setup (prerequisite: `fix-pwa-service-worker-updates` merged)

- [x] 1.1 Confirm the latest `com.zerodeplibs:zerodep-web-push-java` via `maven-metadata.xml`, add it to `backend/build.gradle`; verify with `./gradlew compileJava`
- [x] 1.2 Create the Modulith module `com.klabis.notifications` (`package-info.java`, published `PushNotifications` + `PushMessage`) and the `klabis.notifications.vapid.*` properties; the module verification test (`ApplicationModules.verify()`) passes
- [x] 1.3 Generate a dev VAPID key pair and wire `KLABIS_VAPID_*` into `runLocalEnvironment.sh`; document key generation and rotation in `backend/CLAUDE.md` (operations note)

## 2. Slice: notification settings page reachable from home

- [x] 2.1 Spec-first: add `docs/openapi/spec/notifications.yaml` with `getNotificationSettings` and the `NotificationSettings` / `NotificationDevice` schemas (design API chapter), register it in `klabis.yaml` + codegen; `./gradlew openapiBundle -PopenapiCheck` and `npm run openapi` pass
- [x] 2.2 `NotificationDevice` aggregate + `notification_device` table in `V001__initial_schema.sql` + memento/JDBC repository (keys stored as `EncryptedString`); domain unit tests + repository test (red → green)
- [x] 2.3 Controller `getNotificationSettings` returning the VAPID public key and only the caller's devices; `@WebMvcTest` covers own-devices-only and HAL `self` (red → green)
- [x] 2.4 Root link processor `notifications`, present only when VAPID keys are configured; tests for both cases (red → green)
- [x] 2.5 Frontend: route `/notifications`, `NotificationSettingsPage` with device states `unsupported` / `ios-needs-install` / `denied` / `off` / `on`, home page tile "Notifikace" driven by the root link, labels in `labels.ts`; Vitest for each state and for tile visibility (red → green)

## 3. Slice: enabling notifications on the current device

- [x] 3.1 Spec: `registerNotificationDevice` + `RegisterNotificationDeviceRequest` (endpoint `https` only, length limits) and the template on `NotificationSettings`; regenerate the bundle + frontend types
- [x] 3.2 Backend: register with an idempotent upsert by endpoint, reassignment from another user, and a description derived from `User-Agent`; domain tests for reassignment + `@WebMvcTest` for validation and the 201 `Location` (red → green)
- [x] 3.3 Frontend: the "Zapnout notifikace na tomto zařízení" click handler requests the permission, subscribes via `getServiceWorkerRegistration()` and submits the template; the current device is marked by endpoint. Vitest: no permission prompt on page load, the denied path shows the explanation, the granted path registers (red → green)

## 4. Slice: test notification delivered and clickable

- [x] 4.1 Backend `PushNotifications` implementation: async sender (after commit), VAPID-signed `aes128gcm` push, TTL 24 h, JSON payload `{title, body, url, tag}`; a no-op bean when the keys are missing. Unit test with a stubbed HTTP push endpoint asserting the encrypted request and VAPID header (red → green)
- [x] 4.2 Spec + backend `sendTestNotification` (template only when ≥ 1 device) sends "Notifikace fungují" with target `/notifications` to all the caller's devices; `@WebMvcTest` for template presence and dispatch (red → green)
- [x] 4.3 `src/sw.ts`: `push` → `showNotification` with icon/badge/tag/data.url; `notificationclick` → focus + navigate an existing window or `openWindow`; unit-test the handlers with a mocked `ServiceWorkerGlobalScope` (red → green)
- [x] 4.4 Frontend "Poslat testovací notifikaci" button + success message; Vitest (red → green)

## 5. Slice: disabling, removing and auto-cleanup

- [x] 5.1 Spec + backend `removeNotificationDevice` (owner-only, a foreign id behaves as not found) and the per-item template; `@WebMvcTest` (red → green)
- [x] 5.2 Sender deletes a device on a push-service `404` / `410` response; unit test (red → green)
- [x] 5.3 Frontend: "Vypnout notifikace na tomto zařízení" (remove + `unsubscribe()`) and "Odebrat" on other devices; Vitest (red → green)
- [x] 5.4 `sw.ts` `pushsubscriptionchange` → notify an open client to re-register; the SPA re-registers on the settings page and app start when the endpoint changed; Vitest (red → green)

## 6. Slice: logout stops notifications on the device

- [x] 6.1 `AuthContext2.logout` performs a best-effort `pushManager` `unsubscribe()` before `signoutRedirect`, and never blocks logout on failure; Vitest (red → green)

## 7. Integration and QA

- [x] 7.1 Run the backend `notifications` module tests and the frontend tests + `npm run build`; `npm run refresh-backend-server-resources`
- [ ] 7.2 Manual QA with the dev VAPID keys on `localhost:3000` (PWA dev mode) and `:8443`: enable on desktop Chrome and Firefox, test notification with the app closed, click opens `/notifications`, remove a device, logout stops delivery — partially done in Playwright Chromium on :3000 (enable, real FCM delivery, remove, disable, logout), see testing/01_push-notifications-testing.md; Firefox, :8443, app closed and notification click remain manual
- [ ] 7.3 Manual QA on a real Android phone (installed PWA) and an iPhone (iOS ≥ 16.4, added to the home screen), plus Safari without install showing the install hint
- [ ] 7.4 Production checklist: VAPID secrets configured, GDPR record mentions push subscriptions and third-party push services
