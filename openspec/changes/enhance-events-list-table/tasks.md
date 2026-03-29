## 1. Registration Deadline — Backend Domain + Persistence

- [ ] 1.1 Add `registrationDeadline` (optional LocalDate) field to Event aggregate, update `EventCommand` record, update `areRegistrationsOpen()` to check deadline
- [ ] 1.2 Add `registration_deadline` column to V001 migration script (nullable DATE)
- [ ] 1.3 Add validation: registrationDeadline must be on or before eventDate (domain invariant)
- [ ] 1.4 Write unit tests for `areRegistrationsOpen()` with deadline scenarios (before deadline, after deadline, no deadline)
- [ ] 1.5 Write unit tests for deadline validation (deadline after eventDate rejected)

## 2. Registration Deadline — Backend API

- [ ] 2.1 Add `registrationDeadline` to `EventDto` and `EventSummaryDto`
- [ ] 2.2 Update `EventDtoMapper` to map registrationDeadline
- [ ] 2.3 Update EventController form affordance (create/update) to include registrationDeadline field
- [ ] 2.4 Write controller integration tests for create/update with registrationDeadline

## 3. List API Enhancements — Backend

- [ ] 3.1 Add `websiteUrl` to `EventSummaryDto`, update mapper
- [ ] 3.2 Add `@HasAuthority(EVENTS_MANAGE)` on `status` field in `EventSummaryDto` with `@JsonInclude(NON_NULL)` and `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)`
- [ ] 3.3 Refactor list endpoint to load full Event aggregates (needed for affordances and coordinator link)
- [ ] 3.4 Add `_links.coordinator` to each list item (reuse pattern from `addLinksForEvent`)
- [ ] 3.5 Add registration/unregistration affordance on list item self link (reuse `areRegistrationsOpen()` + currentUser check)
- [ ] 3.6 Write integration tests: list response includes websiteUrl, registrationDeadline, coordinator link, registration affordance
- [ ] 3.7 Write integration tests: status field hidden for regular users, visible for managers

## 4. Registration Deadline — Rejection on expired deadline

- [ ] 4.1 Verify that `areRegistrationsOpen()` change (task 1.1) correctly prevents registration after deadline — affordance not shown, and direct POST returns 400
- [ ] 4.2 Verify unregistration is also blocked after deadline passes
- [ ] 4.3 Write integration tests for registration/unregistration rejection when deadline passed

## 5. Frontend — Events Table Columns

- [ ] 5.1 Update `EventListData` interface to include `websiteUrl`, `registrationDeadline`
- [ ] 5.2 Add "Web" column with ExternalLink icon (conditional on websiteUrl presence)
- [ ] 5.3 Add "Uzávěrka" column with formatted date
- [ ] 5.4 Add "Koordinátor" column — resolve name from `_links.coordinator` HAL link, render as clickable link to member detail
- [ ] 5.5 Add "Status" column conditionally (show only when field present in API response — field-level security)
- [ ] 5.6 Add "Akce" column with HalFormButton for registerForEvent/unregisterFromEvent affordance
- [ ] 5.7 Add localization labels for new columns

## 6. ORIS Import — Registration Deadline

- [ ] 6.1 Update `Event.createFromOris()` factory method to accept optional `registrationDeadline` parameter
- [ ] 6.2 Map `EventDetails.entryDate1` (ZonedDateTime) to `LocalDate` in `EventManagementServiceImpl.importEventFromOris()`
- [ ] 6.3 Write unit test: import with EntryDate1 set → registrationDeadline mapped
- [ ] 6.4 Write unit test: import with EntryDate1 null → registrationDeadline is null

## 7. Frontend — Event Detail Page

- [ ] 7.1 Add `registrationDeadline` to `EventDetail` interface in `EventDetailPage.tsx`
- [ ] 7.2 Add DetailRow for registrationDeadline in the event info card (formatted date, shown when present or editing)
- [ ] 7.3 Support inline editing of registrationDeadline via `ri('registrationDeadline')` (same pattern as websiteUrl)
- [ ] 7.4 Add localization label for registrationDeadline field

## 8. Frontend — Create/Edit Event Form

- [ ] 8.1 Update event create/edit form to include registrationDeadline date picker field
- [ ] 8.2 Verify HAL+FORMS template includes registrationDeadline property
