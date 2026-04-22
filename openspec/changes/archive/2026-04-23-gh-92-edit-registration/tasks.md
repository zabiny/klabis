## 1. Domain: edit SI card number (vertical slice)

- [x] 1.1 Write failing test on `Event` aggregate: `editRegistration` updates SI card number, preserves `id` and `registeredAt`, removes old value from registrations collection, adds updated one
- [x] 1.2 Write failing test: `editRegistration` refuses with `RegistrationNotFoundException` when member is not registered
- [x] 1.3 Write failing test: `editRegistration` refuses when event is not `ACTIVE` (same rule as register/unregister)
- [x] 1.4 Write failing test: `editRegistration` refuses when `registrationDeadline` has passed
- [x] 1.5 Write failing test: `editRegistration` refuses on or after `eventDate`
- [x] 1.6 Write failing test: `editRegistration` emits `RegistrationEditedEvent` with event id, member id, old snapshot, new snapshot
- [x] 1.7 Add `EventRegistration.withChanges(SiCardNumber newSiCard, String newCategory)` returning a new value object copying `id` and `registeredAt`
- [x] 1.8 Add `Event.EditRegistrationCommand(SiCardNumber siCardNumber, String category)` record
- [x] 1.9 Add `Event.editRegistration(MemberId memberId, EditRegistrationCommand command)` using `assertRegistrationsOpen()` + `resolveCategory()`
- [x] 1.10 Add `RegistrationEditedEvent(eventId, memberId, oldSiCardNumber, oldCategory, newSiCardNumber, newCategory)` domain event
- [x] 1.11 Run domain tests green, refactor

## 2. Domain: edit category (extend slice 1)

- [x] 2.1 Write failing test: `editRegistration` updates category for event with categories
- [x] 2.2 Write failing test: `editRegistration` refuses when category is null/blank for event with categories
- [x] 2.3 Write failing test: `editRegistration` refuses when category is not in the event's category list
- [x] 2.4 Write failing test: `editRegistration` ignores provided category for event without categories (mirrors register behavior)
- [x] 2.5 Verify `resolveCategory()` already covers the cases; run tests green, refactor

## 3. Application service

- [x] 3.1 Write failing test on `EventRegistrationServiceTest`: `editRegistration(eventId, memberId, command)` loads event, calls aggregate, saves
- [x] 3.2 Write failing test: `editRegistration` propagates `EventNotFoundException` when event missing
- [x] 3.3 Write failing test: `editRegistration` propagates `RegistrationNotFoundException` when member not registered
- [x] 3.4 Add `EventRegistrationPort.editRegistration(EventId, MemberId, Event.EditRegistrationCommand)` signature
- [x] 3.5 Implement `EventRegistrationService.editRegistration(...)`
- [x] 3.6 Run tests green, refactor

## 4. REST endpoint + HAL-FORMS affordance

- [x] 4.1 Write failing `@WebMvcTest` slice for `EventRegistrationController`: `PUT /api/events/{eventId}/registrations/{memberId}` with acting user equal to `{memberId}` updates registration and returns 204
- [x] 4.2 Write failing slice test: `PUT` by a different member returns 403 (owner check)
- [x] 4.3 Write failing slice test: `PUT` with invalid SI card number returns 400 with field feedback
- [x] 4.4 Write failing slice test: `PUT` with category not in event's categories returns 400
- [x] 4.5 Write failing slice test: `GET /api/events/{eventId}/registrations/{memberId}` (own registration) exposes `edit` affordance when window is open AND viewer is the registered member
- [x] 4.6 Write failing slice test: `edit` affordance is absent after deadline
- [x] 4.7 Write failing slice test: `edit` affordance is absent when viewer is not the registered member
- [x] 4.8 Write failing slice test: registrations list (`GET /api/events/{eventId}/registrations`) carries `edit` affordance on the acting member's row and on no other row
- [x] 4.9 Add `EditRegistrationRequest` DTO (siCardNumber, category) with bean validation mirroring the register DTO
- [x] 4.10 Add controller method `editRegistration` with `@OwnerVisible` + `@OwnerId @PathVariable MemberId memberId`
- [x] 4.11 Add HAL-FORMS `edit` affordance to own-registration representation when `assertRegistrationsOpen()` allows it, using `klabisAfford`
- [x] 4.12 Add HAL-FORMS `edit` affordance to the acting member's row in the registrations list
- [x] 4.13 Run slice tests green, refactor

## 5. Backend E2E

- [x] 5.1 Extend `EventRegistrationE2ETest`: member edits SI card number → GET returns updated value, `registeredAt` unchanged
- [x] 5.2 Extend E2E: member edits category → subsequent list shows new category on their row
- [x] 5.3 Extend E2E: other member attempting to edit someone else's registration gets 403
- [x] 5.4 Extend E2E: guest member (`hostující`) edits own registration successfully
- [x] 5.5 Extend E2E: edit after deadline passes returns error and registration unchanged
- [x] 5.6 Run full backend test suite via test-runner agent

## 6. Frontend: edit on "Moje přihláška"

- [x] 6.1 Add "Upravit" button to own registration page, driven by presence of `edit` affordance in HAL response (no static condition)
- [x] 6.2 Open edit modal pre-filled with current SI card number and category (when event has categories)
- [x] 6.3 Submit via HAL-FORMS template; surface field-level validation errors inline
- [x] 6.4 On success, refresh the own-registration query and close the modal

## 7. Frontend: edit on registrations list row

- [x] 7.1 Show "Upravit" row-action only on the row whose `edit` affordance is present (i.e., the acting member's row)
- [x] 7.2 Open the same edit modal used by "Moje přihláška"
- [x] 7.3 On success, refresh the registrations list and close the modal

## 8. Finalization

- [x] 8.1 Code review (developer:code-reviewer)
- [x] 8.2 Full test suite green (backend + frontend)
- [ ] 8.3 Manual QA via Playwright: club member edits SI + category, guest member edits, non-owner cannot edit, edit hidden after deadline
- [x] 8.4 Add label `BackendCompleted` to GitHub issue #92 after backend portion is merged
