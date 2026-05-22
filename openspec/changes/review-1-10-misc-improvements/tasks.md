## 1. Change password while authenticated (N1)

- [x] 1.1 Add `User.changePassword(newPasswordHash, ...)` domain method (or extend existing setPassword if already present); emit `PasswordChangedEvent`
- [x] 1.2 Application service `PasswordChangeService.changePassword(userId, currentPassword, newPassword)`: verify current password via `passwordEncoder.matches`, validate new password via existing `PasswordValidator`, update aggregate, persist
- [x] 1.3 REST endpoint `POST /api/me/password-change` with body `{ currentPassword, newPassword }`; gated to authenticated users; returns 204 No Content on success, 400 with error code on validation failures
- [x] 1.4 Integration tests: success path, wrong current password, weak new password, complexity rule failures
- [x] 1.5 Frontend: add "Změnit heslo" button + dialog in `MyProfile`; reuse password complexity validation logic from password setup form
- [x] 1.6 Frontend tests for the dialog flow
- [ ] 1.7 Browser smoke test post-deploy

## 2. Filter events by year (N4)

- [ ] 2.1 Frontend: add year selector dropdown in events list filter bar (range: currentYear-10 to currentYear+2 plus "no year" option)
- [ ] 2.2 On year change: set URL query params `dateFrom=YYYY-01-01`, `dateTo=YYYY-12-31`; switch time-window selector to "Vše"
- [ ] 2.3 On clear: remove `dateFrom`/`dateTo`; restore time-window to previous value
- [ ] 2.4 On URL load: derive selected year from `dateFrom`/`dateTo` if range covers exactly one calendar year; otherwise selector shows "no year"
- [ ] 2.5 Frontend tests for selector, URL roundtrip, clear behavior
- [ ] 2.6 Localization label `eventsFilterYear = "Rok"`
- [ ] 2.7 Browser smoke test

## 3. Rename "Koordinátor" → "Vedoucí" (K1)

- [ ] 3.1 Audit `frontend/src/localization/labels.ts` for entries referencing "Koordinátor" / `coordinator`; replace label values with "Vedoucí" / "Vedoucí akce" as appropriate
- [ ] 3.2 Audit backend HAL+FORMS templates (`EventController` etc.) — if any field label string is hard-coded as "Koordinátor", replace with "Vedoucí"
- [ ] 3.3 Visual smoke test: events list, filter bar, event detail, event form → all four places now show "Vedoucí"
- [ ] 3.4 Verify API field names (`eventCoordinatorId`, etc.) remain unchanged in OpenAPI; verify ORIS import code is not affected (uses field names, not labels)

## 4. Documentation

- [ ] 4.1 Sync spec changes into `openspec/specs/users/spec.md` and `openspec/specs/events/spec.md` after archiving
