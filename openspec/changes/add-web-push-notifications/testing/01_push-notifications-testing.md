# Web Push Notifications - QA Testing

Environment: Playwright Chromium on http://localhost:3000, backend https://localhost:8443 with dev VAPID keys.
Out of scope: real push delivery / notification click (needs a browser with a push service), real Android/iPhone (7.3), production checklist (7.4).

## Scenarios

### Home page entry
- [x] **HOME-1**: Member ZBM9500 sees the "Notifikace" tile on the home page; clicking it opens /notifications
- [x] **HOME-2**: Admin ZBM9000 sees the "Notifikace" tile
- [x] **HOME-3**: "Notifikace" does not appear as a main menu item

### Settings page
- [x] **PAGE-1**: Opening /notifications shows no permission prompt and (with default permission) offers "Zapnout notifikace na tomto zařízení"
- [x] **PAGE-2**: With no devices, "Poslat testovací notifikaci" is not shown
- [x] **PAGE-3**: Permission blocked in the browser → page states notifications are blocked and how to allow them; enable button hidden
- [x] **PAGE-4**: Browser without push support → page states the browser cannot receive notifications; enable button hidden

### Enable / devices
- [x] **EN-1**: Clicking enable with permission granted subscribes and registers the device; page shows enabled + device listed and marked as current (env limitation possible)
- [ ] **EN-2**: Clicking enable with permission denied shows the blocked explanation and does not add a device
- [x] **DEV-1**: Another registered device (registered via API) is listed with description and enable date, not marked as current
- [x] **DEV-2**: "Odebrat" on another device removes it from the list
- [x] **DEV-3**: Devices of another user are not visible (admin does not see member's devices)

### Test notification
- [x] **TEST-1**: With ≥ 1 device, "Poslat testovací notifikaci" is shown; clicking it shows a confirmation

### Disable / logout
- [x] **DIS-1**: "Vypnout notifikace na tomto zařízení" removes the current device and the page shows disabled state (env limitation possible)
- [x] **OUT-1**: Logout completes normally (not blocked by push unsubscribe)

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| HOME-1 | PASS | tile shown, opens /notifications |
| HOME-3 | PASS | not in main menu |
| PAGE-1 | PASS | no prompt on load, enable button shown |
| PAGE-2 | PASS | no test button without devices |
| DEV-1 | PASS | API-registered devices listed ("Chrome · Linux", date), not marked current |
| TEST-1 | PASS | confirmation shown; FCM answered 404 for fake endpoints → devices auto-removed (auto-cleanup verified) |
| DEV-2 | PASS | "Odebrat" removes the device (HIGH simplify fix confirmed against real backend) |
| PAGE-3 | PASS | blocked state explained, device list + test button stay |
| EN-2 | SKIP | Playwright cannot answer the OS prompt; blocked state covered by PAGE-3 + Vitest |
| EN-1 | FAIL | **Issue #1**: dev SW `dev-sw.js` contains an ES import but was registered as classic worker → "ServiceWorker script evaluation failed", enable always fails on :3000 |
| OUT-1 | PASS | logout not blocked (member) |
| others | SKIP | interrupted — fixing issue #1 |

Fix #1: `devOptions.type: 'module'` + register with `{type: 'module'}` in dev only (frontend).

### Iteration 2
| Scenario | Result | Note |
|----------|--------|------|
| HOME-2 | PASS | admin sees tile, not in menu |
| DEV-3 | PASS | admin does not see member's devices |
| PAGE-2 | PASS | |
| EN-1 | PASS | real FCM subscription created, device listed as "Toto zařízení", endpoint stored |
| TEST-1 | PASS | **real delivery**: SW showed "Notifikace fungují" (body, icon, tag `/notifications`, data.url) |
| DIS-1 | PASS | device removed, subscription unsubscribed, stored endpoint cleared |
| OUT-1 | PASS | subscription + stored endpoint cleared on logout; backend row stays until next 410 (by design) |
| PAGE-4 | PASS | PushManager removed → "Tento prohlížeč nepodporuje push notifikace.", list + test button stay |
| HOME-1, HOME-3, DEV-1, DEV-2 | PASS | member retest |

### Observations (out of scope)
- **Pre-existing logout race** (`AuthContext2.logout` + `ProtectedRoute`): `removeUser()` fires before `signoutRedirect()`, `ProtectedRoute` calls `login()` and its `/oauth2/authorize` navigation can supersede `/connect/logout` → user is silently logged back in (reproduced for ZBM9000; member logout succeeded). Push unsubscribe part works. Recommend a separate fix (e.g. don't `removeUser()` before `signoutRedirect`).
- Notification click handling (focus/navigate) not testable in Playwright (no OS notification UI) — covered by Vitest; verify manually (7.2/7.3).
- HAL-FORMS renders string length limits of `registerNotificationDevice` as `type: "range"` — no functional impact (no form rendered).
