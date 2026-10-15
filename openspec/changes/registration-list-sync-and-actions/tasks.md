# Tasks

## 1. Synchronisation state in the registrations list

- [x] 1.1 Backend: add `_links.sync` to enrolled rows in `RegistrationSummaryPostprocessor`, sharing the lookup/link logic with `RegistrationDetailsPostprocessor`; verify with WebMvcTest: enrolled row has `sync` link, non-enrolled row has none
- [x] 1.2 Frontend: render `SyncStatusIndicator mode="icon"` in the actions cell of `RegistrationsTable` (`EventDetailPage.tsx`) for rows with `_links.sync`; verify with vitest (indicator present only on rows with the link) and `npm run build`
- [x] 1.3 Update `event-registrations` main-spec-facing docs/tests that asserted "no sync state in the list" (grep for it) and verify the suite is green

## 2. Edit own registration from the events list

- [ ] 2.1 Frontend: add an "Upravit přihlášku" (Pencil) row action in `EventsPage.tsx` shown when the row has the `editRegistration` template; it opens `EventRegistrationDialog` with a link built from the normalized `template.target`; add label in `localization/labels.ts`; verify with vitest (action shown only with the template, opens the dialog with the expected link) and `npm run build`

## 3. Unregister from the edit dialog

- [ ] 3.1 Frontend: in edit mode `EventRegistrationDialog` renders an "Odhlásit" button when the loaded registration has the `unregisterFromEvent` template; click switches the dialog to the template's confirmation (reusing the generic template-form component from the events list); success closes the dialog and calls `onChanged` (rename of `onRegistered` across hosts); dismissing the confirmation leaves the registration unchanged; verify with vitest in `EventRegistrationDialog.test.tsx` (button shown/hidden, confirm → callback, dismiss → no change) and `npm run build`
