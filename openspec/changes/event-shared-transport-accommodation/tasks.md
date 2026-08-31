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

- [x] 4.1 Add `sharedTransportCount()` / `sharedAccommodationCount()` query methods to `Event` (plain filter over `registrations`, return `long`); unit test with a mix of choices.
- [x] 4.2 In `EventController.getEvent`, after conversion set `EventDto.sharedServicesSummary` to non-null only when ACTIVE + coordinator/EVENTS:REGISTRATIONS + ≥1 offer enabled; each sub-object present only for its enabled offer; null otherwise (dropped by record-level `@JsonInclude(NON_NULL)`). `EventDtoConverter` gets `@Mapping(target="sharedServicesSummary", ignore=true)`. It is a plain DTO field, not `_embedded`.
- [x] 4.3 Controller/API test matrix (a)–(f) in `EventControllerTest`.

## 5. Backend — accommodation list filter + gate (vertical slice: filtered list)

- [x] 5.1 Controller/API test (red): `getAccommodationList` (HAL and `text/csv`) on an event with `sharedAccommodationEnabled = true` returns only registrations with `wantsSharedAccommodation = true`. _(CSV variant verified via ArgumentCaptor on `csvRenderer.renderToBytes`.)_
- [x] 5.2 Filter `assembleAccommodationItems` in `EventController` to `wantsSharedAccommodation` (new `registrationsWantingSharedAccommodation(Event)` helper); the same filtered list feeds `HalResponseContext.setDomainList`.
- [x] 5.3 Flag precondition in `loadAuthorizedEventForAccommodation` → `AccessDeniedException` (403, Open Question 2) for both HAL and CSV variants, after the existing auth check.
- [x] 5.4 `accommodation-list` HAL link (in `EventDetailsPostprocessor`) gated on `isSharedAccommodationEnabled() && isCoordinatorOrHasRegistrationsAuthority`; presence/absence tests added.
- [x] 5.5 Existing `AccommodationListTests` / `AccommodationListCsvTests` / link-presence fixtures updated (`withSharedAccommodationEnabled(true)` + `wantsSharedAccommodation = true` on their registrations); "unauthorized user" tests still assert 403 (auth check precedes flag check).

## 6. Frontend (parallel with Sections 2–5 after Task 1.9)

- [x] 6.1 Event create form (EventsPage custom layout) + inline edit (EventDetailPage `DetailRow`s): checkboxes for `sharedTransportEnabled` / `sharedAccommodationEnabled` from the `createEvent` / `updateEvent` template metadata; wired to payload.
- [x] 6.2 Registration form: `wantsSharedTransport` / `wantsSharedAccommodation` checkboxes render from the `registerForEvent` template `properties[]` (present only when the event enables the offer). _No page change needed — fixed the generic `HalFormsFieldFactory` to route HAL-FORMS `type: "Boolean"` → `HalFormsCheckbox` (renderer previously only handled lowercase `boolean`, which the backend never emits)._
- [x] 6.3 Edit-own-registration form: same conditional checkboxes from the `editRegistration` / `updateRegistration` template, prefilled from `RegistrationDto.wantsShared*`.
- [x] 6.4 EventDetailPage: count-summary `Card` near the registrations section — renders iff `sharedServicesSummary` present; each line iff its sub-object present; `count ?? 0` (so "0 členů" shows).
- [x] 6.5 EventDetailPage: "Seznam pro ubytování" action already purely `_links['accommodation-list']`-driven; verified it disappears when the backend omits the link.
- [x] 6.6 `npm run build` (`tsc -b` + vite) passes; `npm run lint` clean; component tests added for boolean-checkbox dispatch, conditional checkbox rendering, and the summary section.

## 7. Joint verification

- [ ] 7.1 Backend: full `events` + `event-registrations` test suites green; domain-layer coverage 100%, module ≥80%.
- [ ] 7.2 `frontend-qa-testing` (Playwright) against `http://localhost:3000`: manager enables both offers on an event → member registers choosing accommodation → coordinator sees "Společné ubytování: 1 člen" and an accommodation list containing only that member → manager disables the offer → summary line and list action disappear, member's stored choice untouched → manager re-enables → line and list reappear with the earlier choice.
- [ ] 7.3 Release note: the "Seznam pro ubytování" behavioural change (now filtered to members who chose shared accommodation; hidden when the event does not offer it).
