## 1. Change password while authenticated (N1)

- [x] 1.1 Add `User.changePassword(newPasswordHash, ...)` domain method (or extend existing setPassword if already present); emit `PasswordChangedEvent`
- [x] 1.2 Application service `PasswordChangeService.changePassword(userId, currentPassword, newPassword)`: verify current password via `passwordEncoder.matches`, validate new password via existing `PasswordValidator`, update aggregate, persist
- [x] 1.3 REST endpoint `POST /api/me/password-change` with body `{ currentPassword, newPassword }`; gated to authenticated users; returns 204 No Content on success, 400 with error code on validation failures
- [x] 1.4 Integration tests: success path, wrong current password, weak new password, complexity rule failures
- [x] 1.5 Frontend: add "Změnit heslo" button + dialog in `MyProfile`; reuse password complexity validation logic from password setup form
- [x] 1.6 Frontend tests for the dialog flow
- [ ] 1.7 Browser smoke test post-deploy

## 2. Filter events by year (N4)

- [x] 2.1 Frontend: add year selector dropdown in events list filter bar (range: currentYear-10 to currentYear+2 plus "no year" option)
- [x] 2.2 On year change: set URL query params `dateFrom=YYYY-01-01`, `dateTo=YYYY-12-31`; switch time-window selector to "Vše"
- [x] 2.3 On clear: remove `dateFrom`/`dateTo`; restore time-window to previous value
- [x] 2.4 On URL load: derive selected year from `dateFrom`/`dateTo` if range covers exactly one calendar year; otherwise selector shows "no year"
- [x] 2.5 Frontend tests for selector, URL roundtrip, clear behavior
- [x] 2.6 Localization label `eventsFilterYear = "Rok"`
- [ ] 2.7 Browser smoke test

## 3. Rename "Koordinátor" → "Vedoucí" (K1)

- [x] 3.1 Audit `frontend/src/localization/labels.ts` for entries referencing "Koordinátor" / `coordinator`; replace label values with "Vedoucí" / "Vedoucí akce" as appropriate
- [x] 3.2 Audit backend HAL+FORMS templates (`EventController` etc.) — if any field label string is hard-coded as "Koordinátor", replace with "Vedoucí"
- [x] 3.3 Visual smoke test: events list, filter bar, event detail, event form → all four places now show "Vedoucí" — NOTE: the filter bar has no coordinator filter control (API supports `coordinator` query param but no UI exposes it); that fourth place does not exist and is out of scope for K1 (separate feature). The three real UI places (table header, detail section, form field) are verified by tests in `labels.test.ts`.
- [x] 3.4 Verify API field names (`eventCoordinatorId`, etc.) remain unchanged in OpenAPI; verify ORIS import code is not affected (uses field names, not labels)

## 4. Documentation

- [x] 4.1 Sync spec changes into `openspec/specs/users/spec.md` and `openspec/specs/events/spec.md` after archiving
