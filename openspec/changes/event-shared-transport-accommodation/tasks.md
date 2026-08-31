# Tasks — Event Shared Transport & Accommodation

Vertical slices. The API spec is updated first (Section 1); once the generated
interfaces and frontend types are refreshed, backend (Sections 2–5) and frontend
(Section 6) proceed in parallel. Section 7 is joint verification.

## 1. API spec first (shared prerequisite — blocks all parallel work)

- [x] 1.1 `docs/openapi/spec/events.yaml`: add `sharedTransportEnabled` / `sharedAccommodationEnabled` (`boolean`, editable) to `EventDto`.
- [x] 1.2 Add optional `sharedTransportEnabled` / `sharedAccommodationEnabled` (`boolean`, default false) to `CreateEventRequest`.
- [x] 1.3 Add `sharedTransportEnabled` / `sharedAccommodationEnabled` as `type: [boolean, 'null']` JsonNullable partial-update fields to `UpdateEventRequest`.
- [x] 1.4 Add optional `wantsSharedTransport` / `wantsSharedAccommodation` (`boolean`, default false) to `RegisterEventRequest` and `EditRegistrationRequest`.
- [x] 1.5 Add `wantsSharedTransport` / `wantsSharedAccommodation` (`boolean`) to `RegistrationDto`.
- [x] 1.6 Add a READ_ONLY `sharedServicesSummary` property to `EventDto` (`SharedServicesSummaryDto` with optional `sharedTransport` / `sharedAccommodation` sub-objects, each a `SharedServiceCountDto { count: integer }`). Controller populates it only for coordinator / EVENTS:REGISTRATIONS on ACTIVE events with ≥1 offer enabled; `@JsonInclude(NON_NULL)` omits it otherwise. Not `x-hal-embedded` (reserved for collections).
- [x] 1.7 Update `x-hal-links.accommodation-list` description on `getEvent`: "Present only when the event offers shared accommodation and the caller is the event coordinator or has EVENTS:REGISTRATIONS".
- [x] 1.8 Update `updateEvent` HAL-FORMS template to include the two `shared*Enabled` properties; update `registerForEvent` (on `getEvent`) and `updateRegistration` (on `getRegistration`) templates so the `wantsShared*` properties are present only when the matching offer is enabled.
- [x] 1.9 Regenerate: run `openapiBundle` (refresh `klabis-full.json`) and the frontend type generation. Commit the spec + regenerated artifacts so backend and frontend can branch from a common baseline.

## 2. Backend — event offer flags (vertical slice: manager enables offers)

- [x] 2.1 Add `shared_transport_enabled` / `shared_accommodation_enabled` `BOOLEAN NOT NULL DEFAULT FALSE` columns (+ `COMMENT ON COLUMN`) to the `events.events` `CREATE TABLE` in `V001__initial_schema.sql`.
- [x] 2.2 Domain test (red): `Event.createEvent` with both flags set persists them; default is `false` when the command omits them.
- [x] 2.3 Add `sharedTransportEnabled` / `sharedAccommodationEnabled` fields to `Event`, extend `CreateEvent` and `UpdateEvent` command records, `reconstruct` signature, and the create/update logic (green + refactor). _Deviation: old-arity `reconstruct` / `registerMember` / `withChanges` / `CreateEvent` kept as delegating overloads defaulting to false, to avoid a ~60-call-site sweep._
- [x] 2.4 Domain test: `Event.updateEvent` toggles a flag on a DRAFT and an ACTIVE event; toggling on a FINISHED / CANCELLED event raises the existing "cannot be modified" rule.
- [x] 2.5 Domain test: turning `sharedAccommodationEnabled` off while registrations have `wantsSharedAccommodation = true` succeeds and does **not** mutate those registrations; turning it back on leaves the choices intact. _Deviation: `EventRegistration` boolean fields + the `event_registrations` columns (task 3.1) pulled forward here so this compiles._
- [x] 2.6 Extend `EventMemento` (`@Column` fields, `from` / `toEvent`) for the two flags; JDBC round-trip test.
- [x] 2.7 Controller/API test: `createEvent` and `updateEvent` accept and echo the flags via `EventDto`; `updateEvent` with the field absent leaves it unchanged.

## 3. Backend — registration choices (vertical slice: member picks offers)

- [x] 3.1 Add `wants_shared_transport` / `wants_shared_accommodation` `BOOLEAN NOT NULL DEFAULT FALSE` columns (+ comments) to `events.event_registrations` `CREATE TABLE` in `V001__initial_schema.sql`. _(done in phase 2 as prerequisite for task 2.5)_
- [x] 3.2 Domain test (red): `Event.registerMember` records both choice flags; defaults to `false` when omitted.
- [x] 3.3 Extend `EventRegistration` (two `boolean` fields), `CreateEventRegistration`, `create` / `reconstruct` / `withChanges` _(phase 2)_, **and** `Event.registerMember` + `RegisterCommand` + `EditRegistrationCommand` + `Event.editRegistration` + `EventRegistrationService.registerMember` signatures (green + refactor). _Deviation: also extended `RegisterCommand` + service (controller builds a command, not a direct call); 2-arg delegating constructors keep old call sites._
- [x] 3.4 Domain test: `editRegistration` updates the two choice flags under the open-registration window; refused after the deadline / on/after the event date (reuses existing guard).
- [x] 3.5 `EventRegistrationMemento` done in phase 2; round-trip verified by `EventJdbcRepositoryTest.SharedServiceFlagsPersistence`.
- [x] 3.6 Controller/API test: `registerForEvent` and `editRegistration` accept the flags; `RegistrationDto` returns them **only when the event enables the offer** (Open Question 3 → omit gated fields, `@JsonInclude(NON_NULL)`).

## 4. Backend — count summary on event detail (vertical slice: coordinator sees counts)

- [ ] 4.1 Add `sharedTransportCount()` / `sharedAccommodationCount()` query methods to `Event` (plain filter over `registrations`); unit test with a mix of choices.
- [ ] 4.2 In `EventController.getEvent`, assemble `sharedServicesSummary` at runtime: include a sub-object per **enabled** offer, only when the event is ACTIVE and `EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority` is true; omit the whole object otherwise. Deliver via `HalResponseContext` like the embedded registrations.
- [ ] 4.3 Controller/API test matrix: (a) coordinator on ACTIVE event with both offers → both counts, including a `count: 0` case; (b) EVENTS:REGISTRATIONS user → same; (c) plain member → no summary; (d) DRAFT event → no summary; (e) offer disabled → its sub-object absent; (f) stored `wants*=true` on a since-disabled offer → excluded from the count.

## 5. Backend — accommodation list filter + gate (vertical slice: filtered list)

- [ ] 5.1 Controller/API test (red): `getAccommodationList` (HAL and `text/csv`) on an event with `sharedAccommodationEnabled = true` returns only registrations with `wantsSharedAccommodation = true`.
- [ ] 5.2 Filter `assembleAccommodationItems` in `EventController` to `wantsSharedAccommodation`.
- [ ] 5.3 Add the flag precondition to `loadAuthorizedEventForAccommodation`: when `!event.sharedAccommodationEnabled()` throw `AccessDeniedException` for both variants. Settle Open Question 2 (403 vs 404) here — default 403.
- [ ] 5.4 Emit the `accommodation-list` HAL link in `getEvent` only when the flag is on **and** the caller is authorized; controller test for link presence/absence.
- [ ] 5.5 Test: existing "unauthorized user" and "placeholder for missing identity card" / "CSV leaves missing values empty" scenarios still pass against the filtered row set.

## 6. Frontend (parallel with Sections 2–5 after Task 1.9)

- [ ] 6.1 Event create form + inline edit: render "Nabídnout společnou dopravu" and "Nabídnout společné ubytování" checkboxes from the `createEvent` / `updateEvent` template metadata; wire to the request payload.
- [ ] 6.2 Registration form: render "Chci využít společnou dopravu" / "Chci využít společné ubytování" checkboxes **only when** present in the `registerForEvent` template; default unchecked.
- [ ] 6.3 Edit-own-registration form: same conditional checkboxes from the `updateRegistration` template, prefilled from `RegistrationDto`.
- [ ] 6.4 Event detail page: render a "Společná doprava: N členů" / "Společné ubytování: M členů" summary section iff `sharedServicesSummary` (resp. each sub-object) is present in the response; show `0 členů` when count is 0.
- [ ] 6.5 Event detail page: show the "Seznam pro ubytování" action only when the `accommodation-list` link is present (already link-driven — verify it now also disappears when the offer is off).
- [ ] 6.6 `npm run build` (tsc + bundle) passes; component tests for the conditional checkbox rendering and the summary section.

## 7. Joint verification

- [ ] 7.1 Backend: full `events` + `event-registrations` test suites green; domain-layer coverage 100%, module ≥80%.
- [ ] 7.2 `frontend-qa-testing` (Playwright) against `http://localhost:3000`: manager enables both offers on an event → member registers choosing accommodation → coordinator sees "Společné ubytování: 1 člen" and an accommodation list containing only that member → manager disables the offer → summary line and list action disappear, member's stored choice untouched → manager re-enables → line and list reappear with the earlier choice.
- [ ] 7.3 Release note: the "Seznam pro ubytování" behavioural change (now filtered to members who chose shared accommodation; hidden when the event does not offer it).
