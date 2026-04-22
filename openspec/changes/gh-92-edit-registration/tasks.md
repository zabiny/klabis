## 1. Domain: edit SI card number (vertical slice)

- [ ] 1.1 Write failing test on `Event` aggregate: `editRegistration` updates SI card number, preserves `id` and `registeredAt`, removes old value from registrations collection, adds updated one
- [ ] 1.2 Write failing test: `editRegistration` refuses with `RegistrationNotFoundException` when member is not registered
- [ ] 1.3 Write failing test: `editRegistration` refuses when event is not `ACTIVE` (same rule as register/unregister)
- [ ] 1.4 Write failing test: `editRegistration` refuses when `registrationDeadline` has passed
- [ ] 1.5 Write failing test: `editRegistration` refuses on or after `eventDate`
- [ ] 1.6 Write failing test: `editRegistration` emits `RegistrationEditedEvent` with event id, member id, old snapshot, new snapshot
- [ ] 1.7 Add `EventRegistration.withChanges(SiCardNumber newSiCard, String newCategory)` returning a new value object copying `id` and `registeredAt`
- [ ] 1.8 Add `Event.EditRegistrationCommand(SiCardNumber siCardNumber, String category)` record
- [ ] 1.9 Add `Event.editRegistration(MemberId memberId, EditRegistrationCommand command)` using `assertRegistrationsOpen()` + `resolveCategory()`
- [ ] 1.10 Add `RegistrationEditedEvent(eventId, memberId, oldSiCardNumber, oldCategory, newSiCardNumber, newCategory)` domain event
- [ ] 1.11 Run domain tests green, refactor

## 2. Domain: edit category (extend slice 1)

- [ ] 2.1 Write failing test: `editRegistration` updates category for event with categories
- [ ] 2.2 Write failing test: `editRegistration` refuses when category is null/blank for event with categories
- [ ] 2.3 Write failing test: `editRegistration` refuses when category is not in the event's category list
- [ ] 2.4 Write failing test: `editRegistration` ignores provided category for event without categories (mirrors register behavior)
- [ ] 2.5 Verify `resolveCategory()` already covers the cases; run tests green, refactor

## 3. Application service

- [ ] 3.1 Write failing test on `EventRegistrationServiceTest`: `editRegistration(eventId, memberId, command)` loads event, calls aggregate, saves
- [ ] 3.2 Write failing test: `editRegistration` propagates `EventNotFoundException` when event missing
- [ ] 3.3 Write failing test: `editRegistration` propagates `RegistrationNotFoundException` when member not registered
- [ ] 3.4 Add `EventRegistrationPort.editRegistration(EventId, MemberId, Event.EditRegistrationCommand)` signature
- [ ] 3.5 Implement `EventRegistrationService.editRegistration(...)`
- [ ] 3.6 Run tests green, refactor

## 4. REST endpoint + HAL-FORMS affordance

- [ ] 4.1 Write failing `@WebMvcTest` slice for `EventRegistrationController`: `PUT /api/events/{eventId}/registrations/{memberId}` with acting user equal to `{memberId}` updates registration and returns 204
- [ ] 4.2 Write failing slice test: `PUT` by a different member returns 403 (owner check)
- [ ] 4.3 Write failing slice test: `PUT` with invalid SI card number returns 400 with field feedback
- [ ] 4.4 Write failing slice test: `PUT` with category not in event's categories returns 400
- [ ] 4.5 Write failing slice test: `GET /api/events/{eventId}/registrations/{memberId}` (own registration) exposes `edit` affordance when window is open AND viewer is the registered member
- [ ] 4.6 Write failing slice test: `edit` affordance is absent after deadline
- [ ] 4.7 Write failing slice test: `edit` affordance is absent when viewer is not the registered member
- [ ] 4.8 Write failing slice test: registrations list (`GET /api/events/{eventId}/registrations`) carries `edit` affordance on the acting member's row and on no other row
- [ ] 4.9 Add `EditRegistrationRequest` DTO (siCardNumber, category) with bean validation mirroring the register DTO
- [ ] 4.10 Add controller method `editRegistration` with `@OwnerVisible` + `@OwnerId @PathVariable MemberId memberId`
- [ ] 4.11 Add HAL-FORMS `edit` affordance to own-registration representation when `assertRegistrationsOpen()` allows it, using `klabisAfford`
- [ ] 4.12 Add HAL-FORMS `edit` affordance to the acting member's row in the registrations list
- [ ] 4.13 Run slice tests green, refactor

## 5. Backend E2E

- [ ] 5.1 Extend `EventRegistrationE2ETest`: member edits SI card number → GET returns updated value, `registeredAt` unchanged
- [ ] 5.2 Extend E2E: member edits category → subsequent list shows new category on their row
- [ ] 5.3 Extend E2E: other member attempting to edit someone else's registration gets 403
- [ ] 5.4 Extend E2E: guest member (`hostující`) edits own registration successfully
- [ ] 5.5 Extend E2E: edit after deadline passes returns error and registration unchanged
- [ ] 5.6 Run full backend test suite via test-runner agent

## 6. Frontend: edit on "Moje přihláška"

- [ ] 6.1 Add "Upravit" button to own registration page, driven by presence of `edit` affordance in HAL response (no static condition)
- [ ] 6.2 Open edit modal pre-filled with current SI card number and category (when event has categories)
- [ ] 6.3 Submit via HAL-FORMS template; surface field-level validation errors inline
- [ ] 6.4 On success, refresh the own-registration query and close the modal

## 7. Frontend: edit on registrations list row

- [ ] 7.1 Show "Upravit" row-action only on the row whose `edit` affordance is present (i.e., the acting member's row)
- [ ] 7.2 Open the same edit modal used by "Moje přihláška"
- [ ] 7.3 On success, refresh the registrations list and close the modal

## 8. Finalization

- [ ] 8.1 Code review (developer:code-reviewer)
- [ ] 8.2 Full test suite green (backend + frontend)
- [ ] 8.3 Manual QA via Playwright: club member edits SI + category, guest member edits, non-owner cannot edit, edit hidden after deadline
- [ ] 8.4 Add label `BackendCompleted` to GitHub issue #92 after backend portion is merged
