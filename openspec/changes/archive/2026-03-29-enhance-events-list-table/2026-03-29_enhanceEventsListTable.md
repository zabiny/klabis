# Team Coordination: Enhance Events List Table

## Change
Add registrationDeadline to Event domain, extend list API with new fields/links/affordances, update frontend table and detail page.

## Iterations Plan
1. **Backend Domain + API + ORIS** (tasks 1.1-1.5, 2.1-2.4, 6.1-6.4): registrationDeadline field, DTOs, ORIS mapping
2. **Backend List Enhancements** (tasks 3.1-3.7, 4.1-4.3): websiteUrl in summary, field-level security on status, coordinator link, registration affordance on list
3. **Frontend** (tasks 5.1-5.7, 7.1-7.4, 8.1-8.2): events table columns, detail page, create/edit form

## Key Design Files
- design.md: technical decisions
- specs/events/spec.md: event specs (backend + frontend)
- specs/event-registrations/spec.md: registration deadline impact

## Progress Log

### 2026-03-29 — Iteration 1 complete (backend-developer)

Tasks 1.1–1.5, 2.1–2.4 (except 2.3), 3.1, and 6.1–6.4 implemented and tested.

**Domain changes:**
- `registrationDeadline` (optional `LocalDate`) added to `Event` aggregate, `EventCommand`, `create()`, `reconstruct()`, `createFromOris()`, `update()` factory/command methods
- `areRegistrationsOpen()` updated: returns false when deadline is not null and deadline is not after today
- Validation in `create()` and `update()`: deadline must be on or before eventDate (throws `BusinessRuleViolationException`)
- `registration_deadline DATE NULL` column added to V001 migration script

**API changes:**
- `EventDto` and `EventSummaryDto` both include `registrationDeadline`
- `EventSummaryDto` also includes `websiteUrl` (task 3.1)
- `EventDtoMapper` maps both fields
- `EventTestDataBuilder.withRegistrationDeadline()` builder method added

**ORIS import:**
- `Event.createFromOris()` accepts `registrationDeadline`
- `importEventFromOris()` maps `EventDetails.entryDate1()` (ZonedDateTime) → `LocalDate` via `.toLocalDate()`

**Tests added:**
- `EventTest.areRegistrationsOpen`: 3 new deadline scenarios (future deadline, past deadline, today deadline)
- `EventTest.create`: deadline-after-eventDate rejected, deadline-equal-to-eventDate allowed
- `EventControllerTest.CreateEventTests`: registrationDeadline in request → 201 Created
- `EventControllerTest.UpdateEventTests`: registrationDeadline in PATCH request → 204 No Content
- `EventControllerTest.GetEventTests`: registrationDeadline present in GET response
- `EventManagementServiceTest.ImportEventFromOrisMethod`: entryDate1 mapped to registrationDeadline; null entryDate1 → null deadline

**Not done (task 2.3):** HAL+FORMS affordance template property for registrationDeadline — depends on EventCommand being the affordance target; no code change needed as Jackson serializes the field automatically from the record component.

### 2026-03-29 — Iteration 2 complete (backend-developer)

Tasks 3.2–3.7 and 4.1–4.3 implemented and tested.

**Group 3 — List API Enhancements:**
- `EventSummaryDto` annotated with `@JsonInclude(NON_NULL)` + `@HandleAuthorizationDenied(NullDeniedHandler)` at class level; `status` field annotated with `@HasAuthority(EVENTS_MANAGE)` — field omitted from JSON for regular users
- `listEvents()` refactored: `PagedResourcesAssembler<EventSummaryDto>` changed to `PagedResourcesAssembler<Event>` so full Event objects are available for link building; `EventDtoMapper.toSummaryDto()` called inside the assembler's `RepresentationModelAssembler` lambda
- `listEvents()` gains `@CurrentUser CurrentUserData currentUser` parameter for affordance logic
- New private `addLinksForListItem()` method: adds self link (GET detail), coordinator link if `getEventCoordinatorId() != null`, and register/unregister affordance based on `areRegistrationsOpen()` + current user registration check
- `validateSortFields` updated to allow `registrationDeadline` as sort field
- `EventsRootPostprocessor` and `addLinksForEvent` updated to pass `null` as third argument to `listEvents()`
- Existing tests adjusted: two tests that asserted `status` field for non-manager users updated to assert `doesNotExist()`

**Group 4 — Deadline rejection:**
- `EventRegistrationServiceImpl` adds `rejectIfDeadlinePassed()` private method: checks `registrationDeadline != null && !deadline.isAfter(LocalDate.now())`, throws `BusinessRuleViolationException("Registration deadline has passed")`
- Called in `registerMember()` before `event.registerMember()` and in `unregisterMember()` before `event.unregisterMember()`
- Unregister order: not-registered check first, then deadline check — correct precedence

**Tests added:**
- `EventControllerTest.ListEventsExtendedFieldsTests` (7 tests): websiteUrl, registrationDeadline, coordinator link, register affordance, no affordance for past events, no affordance when deadline passed, status visibility for regular user/manager
- `EventRegistrationControllerTest.RegistrationDeadlineEnforcementTests` (2 tests): POST /registrations returns 400, DELETE /registrations returns 400 when `BusinessRuleViolationException` thrown
- `EventRegistrationServiceTest.RegisterMemberMethod` (2 new tests): deadline passed rejected, future deadline allowed
- `EventRegistrationServiceTest.UnregisterMemberMethod` (2 new tests): deadline passed rejected, future deadline allowed

### 2026-03-29 — Compilation verification (backend-developer)

Investigated reported compilation errors for `registrationDeadline` call sites. All files were already correctly updated as part of the Iteration 1 implementation:

- `EventTestDataBuilder.build()` — `registrationDeadline` present at position 8 in `Event.reconstruct()` call
- `EventJdbcRepositoryTest` — all `Event.create()` calls already pass `null` as the 7th argument (registrationDeadline)
- `EventCreatedEvent`, `EventUpdatedEvent`, `EventAssert` — no unused `WebsiteUrl` imports; all imports are in use
- `EventsEventListenerTest`, `CalendarEventSyncIntegrationTest` — `Event.reconstruct()` calls already include `registrationDeadline` at position 8

Both `compileJava` and `compileTestJava` succeed. Test run: 1586/1593 passing. The 7 failing tests are pre-existing issues unrelated to this change (OIDC E2E, module boundary violation for coordinator link, permission authority ordering).

### 2026-03-29 — Iteration 2 compilation fix verification (backend-developer)

Investigated 4 reported compilation errors after Iteration 2. All were already resolved in the current codebase:

1. `PagedResourcesAssembler<Event>` vs `Page<EventSummaryDto>` — `PagedResourcesAssembler<Event>` is correct; `page` is `Page<Event>` and DTO mapping happens inside the assembler lambda. No change needed.
2. `listEvents(null, null)` missing `currentUser` at line 357 — already passes 3 arguments: `listEvents(null, null, null)`. No change needed.
3. `EventsRootPostprocessor.listEvents(null, Pageable.unpaged())` missing `currentUser` — already has `null` as third argument. No change needed.
4. Unused import `BusinessRuleViolationException` in `EventRegistrationServiceImpl` — import IS used in `rejectIfDeadlinePassed()` (line 56 anonymous subclass). No change needed.

`compileJava` and `compileTestJava` both pass. `EventControllerTest` and `EventRegistrationServiceImpl` tests: 44/44 passing.

### 2026-03-29 — Iteration 3 complete (frontend-developer)

Tasks 5.1–5.7, 7.1–7.4, 8.1–8.2 implemented. All 887 frontend tests pass.

**Group 5 — Events Table Columns:**
- `EventListData` interface extended with `websiteUrl?: string` and `registrationDeadline?: string`
- "Web" column: `ExternalLink` icon wrapped in `<a target="_blank">`, only renders when `websiteUrl` is truthy; `e.stopPropagation()` prevents row navigation
- "Uzávěrka" column: formatted date via `formatDate`, sortable
- "Koordinátor" column: `CoordinatorCellContent` component uses `HalRouteProvider` with `_links.coordinator` from row item; `CoordinatorName` inside fetches member via HAL context and renders clickable button navigating to member detail; `e.stopPropagation()` prevents row navigation
- "Status" column: `hideEmptyColumns={true}` on `HalEmbeddedTable` hides column when all rows have null/undefined status (field-level security for regular users)
- "Akce" column: `EventRowActions` wraps per-row `HalRouteProvider` with row's self link; `EventRowActionButtons` inside uses `HalFormButton` for `registerForEvent`/`unregisterFromEvent` which reads templates from per-row HAL context
- Localization labels added: `fields.registrationDeadline`, `tables.web`, `tables.registrationDeadline`, `tables.coordinator`

**Group 7 — Event Detail Page:**
- `registrationDeadline?: string` added to `EventDetail` interface
- `DetailRow` for `registrationDeadline` added after `websiteUrl` row; follows same conditional pattern `(isEditing || event.registrationDeadline)`; displays `formatDate(event.registrationDeadline)` in read mode, `ri('registrationDeadline')` in edit mode
- Inline editing works automatically via `enrichTemplateWithReadOnlyFields` + `ri()` pattern already in place

**Group 8 — Create/Edit Event Form:**
- No frontend changes needed: HAL+FORMS templates include `registrationDeadline` automatically because the backend `EventCommand` record has the field and Jackson serializes it; `HalFormsForm` renders all template properties including the new field

### 2026-03-29 — Code review findings fixed (backend-developer)

**Finding 1 — Remove duplicate deadline check from service:**
- `rejectIfDeadlinePassed()` removed from `EventRegistrationServiceImpl` along with its two call sites in `registerMember` and `unregisterMember`
- Deadline check moved into `Event.registerMember()`: throws `BusinessRuleViolationException("Registration deadline has passed")` when `registrationDeadline != null && !registrationDeadline.isAfter(LocalDate.now())`
- Deadline check added to `Event.unregisterMember()`: same guard using the passed-in `currentDate`, checked before the event-date guard
- `EventRegistrationServiceTest.shouldRejectUnregistrationWhenDeadlinePassed` updated: setup now uses `Event.reconstruct()` to build the test fixture with a past deadline and a pre-existing registration (bypasses domain validation, which is correct for test setup)

**Finding 2 — Validate registrationDeadline in createFromOris:**
- `validateRegistrationDeadline(registrationDeadline, eventDate)` call added to `createFromOris()`, matching the validation already present in `create()` and `update()`

All 85 affected tests pass (78 unit + 7 controller/service integration).

### 2026-03-29 — Missing test added (backend-developer)

Added `shouldRejectImportWhenEntryDate1IsAfterEventDate` to `EventManagementServiceTest.ImportEventFromOrisMethod`: verifies that importing an ORIS event where `entryDate1` is after the event date is rejected with `BusinessRuleViolationException`. This covers the validation path in `createFromOris()` added during the code review fix (Finding 2). All 27 tests in `EventManagementServiceTest` pass.
