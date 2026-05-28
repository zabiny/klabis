## 1. Backend — expose transaction affordance on registration rows

- [x] 1.1 Write failing controller/representation test: GET registrations list as user with FINANCE:MANAGE returns each row with an affordance (`recordTransaction`) pointing to the registered member's account transaction endpoint.
- [x] 1.2 Write failing controller test: GET registrations list as user WITHOUT FINANCE:MANAGE returns rows without the affordance.
- [x] 1.3 Implement representation processor / HAL+FORMS affordance emitter on event registration row resource that conditionally exposes the transaction affordance based on `FINANCE:MANAGE`.
- [x] 1.4 Verify direct API call to the underlying transaction endpoint already enforces `FINANCE:MANAGE` (it does in `member-accounts`); add an explicit integration test exercising the affordance URL end-to-end.
- [x] 1.5 Refactor: ensure no duplication between the member-list affordance emitter and the new registrations-list affordance emitter; extract shared helper if natural.

## 2. Frontend — open unified transaction dialog from a registration row

- [x] 2.1 Write failing component test: registrations list row shows the transaction action when the row resource exposes the `recordTransaction` affordance.
- [x] 2.2 Write failing component test: registrations list row hides the action when the affordance is absent (member without FINANCE:MANAGE).
- [x] 2.3 Wire the existing unified transaction dialog (reused from `member-accounts`) to open from a registration row, passing the affordance's target so identity/balance load correctly.
- [x] 2.4 Write failing test: after successful submit, dialog closes and user remains on the same registrations list (no navigation).
- [x] 2.5 Implement the post-submit behavior so the dialog closes without navigation; ensure the registrations list refresh (if any) is appropriate.

## 3. Frontend — prefill note with event name

- [x] 3.1 Write failing test: when dialog is opened from a registrations list, the note field is prefilled with the event name (or a short event-identifying label).
- [x] 3.2 Write failing test: when dialog is opened from member-accounts contexts (member list, account page), no event-derived prefill is applied (existing behavior preserved).
- [x] 3.3 Implement note prefill propagation through the dialog API without leaking event context to other call sites.

## 4. End-to-end verification

- [x] 4.1 Manual / Playwright happy path: log in as finance manager, open an event with registrations, charge entry fee for a member, verify transaction appears in that member's account history with the prefilled event note.
- [x] 4.2 Manual / Playwright authorization path: log in as a regular club member, open the same registrations list, verify no transaction action is shown.
- [x] 4.3 Verify member-accounts existing flows (member list inline dialog, account page) still work — regression check.
