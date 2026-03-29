## 1. Registration Deadline — Backend Domain + Persistence

- [x] 1.1 Add `registrationDeadline` (optional LocalDate) field to Event aggregate, update `EventCommand` record, update `areRegistrationsOpen()` to check deadline
- [x] 1.2 Add `registration_deadline` column to V001 migration script (nullable DATE)
- [x] 1.3 Add validation: registrationDeadline must be on or before eventDate (domain invariant)
- [x] 1.4 Write unit tests for `areRegistrationsOpen()` with deadline scenarios (before deadline, after deadline, no deadline)
- [x] 1.5 Write unit tests for deadline validation (deadline after eventDate rejected)

## 2. Registration Deadline — Backend API

- [x] 2.1 Add `registrationDeadline` to `EventDto` and `EventSummaryDto`
- [x] 2.2 Update `EventDtoMapper` to map registrationDeadline
- [x] 2.3 Update EventController form affordance (create/update) to include registrationDeadline field
- [x] 2.4 Write controller integration tests for create/update with registrationDeadline

## 3. List API Enhancements — Backend

- [x] 3.1 Add `websiteUrl` to `EventSummaryDto`, update mapper
- [x] 3.2 Add `@HasAuthority(EVENTS_MANAGE)` on `status` field in `EventSummaryDto` with `@JsonInclude(NON_NULL)` and `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)`
- [x] 3.3 Refactor list endpoint to load full Event aggregates (needed for affordances and coordinator link)
- [x] 3.4 Add `_links.coordinator` to each list item (reuse pattern from `addLinksForEvent`)
- [x] 3.5 Add registration/unregistration affordance on list item self link (reuse `areRegistrationsOpen()` + currentUser check)
- [x] 3.6 Write integration tests: list response includes websiteUrl, registrationDeadline, coordinator link, registration affordance
- [x] 3.7 Write integration tests: status field hidden for regular users, visible for managers

## 4. Registration Deadline — Rejection on expired deadline

- [x] 4.1 Verify that `areRegistrationsOpen()` change (task 1.1) correctly prevents registration after deadline — affordance not shown, and direct POST returns 400
- [x] 4.2 Verify unregistration is also blocked after deadline passes
- [x] 4.3 Write integration tests for registration/unregistration rejection when deadline passed

## 5. Frontend — Events Table Columns

- [x] 5.1 Update `EventListData` interface to include `websiteUrl`, `registrationDeadline`
- [x] 5.2 Add "Web" column with ExternalLink icon (conditional on websiteUrl presence)
- [x] 5.3 Add "Uzávěrka" column with formatted date
- [x] 5.4 Add "Koordinátor" column — resolve name from `_links.coordinator` HAL link, render as clickable link to member detail
- [x] 5.5 Add "Status" column conditionally (show only when field present in API response — field-level security)
- [x] 5.6 Add "Akce" column with HalFormButton for registerForEvent/unregisterFromEvent affordance
- [x] 5.7 Add localization labels for new columns

## 6. ORIS Import — Registration Deadline

- [x] 6.1 Update `Event.createFromOris()` factory method to accept optional `registrationDeadline` parameter
- [x] 6.2 Map `EventDetails.entryDate1` (ZonedDateTime) to `LocalDate` in `EventManagementServiceImpl.importEventFromOris()`
- [x] 6.3 Write unit test: import with EntryDate1 set → registrationDeadline mapped
- [x] 6.4 Write unit test: import with EntryDate1 null → registrationDeadline is null

## 7. Frontend — Event Detail Page

- [x] 7.1 Add `registrationDeadline` to `EventDetail` interface in `EventDetailPage.tsx`
- [x] 7.2 Add DetailRow for registrationDeadline in the event info card (formatted date, shown when present or editing)
- [x] 7.3 Support inline editing of registrationDeadline via `ri('registrationDeadline')` (same pattern as websiteUrl)
- [x] 7.4 Add localization label for registrationDeadline field

## 8. Frontend — Create/Edit Event Form

- [x] 8.1 Update event create/edit form to include registrationDeadline date picker field
- [x] 8.2 Verify HAL+FORMS template includes registrationDeadline property
