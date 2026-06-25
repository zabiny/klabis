# Tasks: Event Coordinator Collection

Each numbered group is an independently committable vertical slice. Slices are ordered so that the domain model lands first, then read paths, then write paths, then authority, then frontend.

## 1. Domain: coordinators collection on Event aggregate

- [x] 1.1 Write failing test: `Event.create(CreateEvent)` with a set of coordinator IDs stores them in insertion order
- [x] 1.2 Write failing test: `Event.create` / `Event.update` deduplicates coordinator IDs (the `LinkedHashSet` collection type enforces uniqueness; duplicates are silently merged, first-occurrence order preserved)
- [x] 1.3 Write failing test: `event.isCoordinator(memberId)` returns true for a member in the collection, false otherwise
- [x] 1.4 Replace `eventCoordinatorId: MemberId` with `coordinators: LinkedHashSet<MemberId>` on the `Event` aggregate
- [x] 1.5 Add `coordinators` to `CreateEvent` and `UpdateEvent` commands (remove `eventCoordinatorId`); uniqueness enforced structurally by the `LinkedHashSet` type
- [x] 1.6 Add `isCoordinator(MemberId): boolean` to `Event`
- [x] 1.7 Update `CreateEventFromOris` to initialize empty coordinators; ensure `SyncFromOris` does not touch coordinators
- [x] 1.8 Refactor: ensure domain logic has 100% test coverage; green build

## 2. Persistence: event_coordinators join table

- [x] 2.1 Add `events.event_coordinators(event_id, member_id, position)` table to the schema; remove `event_coordinator_id` column from `events.events`
- [x] 2.2 Write failing repository test: save an event with multiple coordinators and reload it preserving order
- [x] 2.3 Update the Event memento / JDBC mapping to persist and load the coordinators collection
- [x] 2.4 Add index on `event_coordinators(member_id)` for the coordinator filter
- [x] 2.5 Verify round-trip test green; refactor mapping

## 3. Read path: event detail exposes coordinators

- [x] 3.1 Write failing controller test: `GET /api/events/{id}` returns `coordinators: MemberId[]` and one `coordinator` HAL link per coordinator
- [x] 3.2 Replace `eventCoordinatorId` with `coordinators` in `EventDto` and the detail mapper
- [x] 3.3 Add one `coordinator` HAL link per coordinator in the event detail postprocessor (default Spring HATEOAS rendering: object for a single coordinator, array for multiple — see design decision)
- [x] 3.4 Green build; refactor mapper

## 4. Read path: list display and coordinator filter

- [x] 4.1 Write failing test: `GET /api/events` summary includes `coordinators: MemberId[]`
- [x] 4.2 Replace `eventCoordinatorId` with `coordinators` in `EventSummaryDto` and the list mapper
- [x] 4.3 Write failing test: coordinator filter matches an event where the member is at any position in the collection
- [x] 4.4 Update the list filter (`EventFilter` + query) to JOIN `event_coordinators` and match any coordinator
- [x] 4.5 Green build; refactor query

## 5. Write path: create and update with coordinators collection

- [x] 5.1 Write failing test: `POST /api/events` with multiple coordinators creates the event with all of them
- [x] 5.2 Write failing test: `POST /api/events` with a duplicate coordinator creates the event with the coordinator listed once (duplicates silently deduplicated by the `LinkedHashSet` collection)
- [x] 5.3 Update `CreateEventRequest` and `UpdateEventRequest` to accept `coordinators: MemberId[]`; map to commands
- [x] 5.4 Write failing test: `POST` / `PATCH` with a non-existent coordinator member returns a not-found error
- [x] 5.5 Green build; refactor request mapping

## 6. Authority: coordinators can edit their event

- [x] 6.1 Write failing test: a member listed as coordinator can `PATCH /api/events/{id}` without EVENTS:MANAGE
- [x] 6.2 Write failing test: a coordinator of event A cannot edit event B; a non-coordinator without EVENTS:MANAGE is denied
- [x] 6.3 Move the edit endpoint authorization from `@HasAuthority(EVENTS_MANAGE)` to an in-method guard allowing EVENTS:MANAGE OR `event.isCoordinator(actingMemberId)`
- [x] 6.4 Update `EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority` to use `event.isCoordinator(...)`
- [x] 6.5 Verify `@OwnerId` on `RegistrationSummaryDto` works against the coordinators collection; implement a custom resolver if the annotation does not support collections (DefaultOwnershipResolver extended additively to support Collection owner values)
- [x] 6.6 Write failing test: edit affordance link is present on the event detail for a coordinator, absent for an unrelated member
- [x] 6.7 Green build; refactor authority checks

## 7. Frontend: coordinators in detail, list, and edit form

- [ ] 7.1 Grep frontend for `eventCoordinatorId`; replace all references with the `coordinators` collection
- [ ] 7.2 Event detail page: render the full coordinator list, each name linking to the member detail page, under the "Vedoucí" heading
- [ ] 7.3 Events list table: show the first coordinator name as a link plus a "+N" badge when more coordinators exist; empty when none
- [ ] 7.4 Create/update form: render the "Vedoucí" field as a multi-value member dropdown
- [ ] 7.5 Manual QA via Playwright: create an event with two coordinators, verify list badge, detail list, and coordinator-without-EVENTS:MANAGE edit access

## 8. Finalize

- [ ] 8.1 Run full backend and frontend test suites; confirm green
- [ ] 8.2 Code review (code-reviewer agent) before commit
- [ ] 8.3 Add label `BackendCompleted` to GitHub issue #83
- [ ] 8.4 Assign the `review-3` milestone to GitHub issue #83
