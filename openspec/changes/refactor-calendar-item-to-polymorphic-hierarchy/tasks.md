## 1. Persistence scaffolding

- [ ] 1.1 Introduce `CalendarItemKind` enum in the persistence package (`com.klabis.calendar.infrastructure.jdbc`) with values `MANUAL` and `EVENT_DATE`.
- [ ] 1.2 Add `kind` field (non-null) to `CalendarMemento` record.
- [ ] 1.3 Update `V001` Flyway migration: add `kind VARCHAR(32) NOT NULL DEFAULT 'EVENT_DATE'` column to the calendar items table (do not add a new migration script).
- [ ] 1.4 Update `CalendarJdbcRepository` queries: include `kind` in SELECT / INSERT / UPDATE; change any method signature that currently returns a single event-linked item (by event id) to return a list.

## 2. Domain hierarchy

- [ ] 2.1 Write failing domain unit tests for `ManualCalendarItem`: `create(...)` succeeds, `update(...)` mutates fields, `assertCanBeDeleted()` returns normally. Cover validation rules currently on `CalendarItem.create` / `CalendarItem.update`.
- [ ] 2.2 Write failing domain unit tests for `EventDateCalendarItem`: `createForEvent(...)` succeeds, `synchronizeFromEvent(...)` mutates fields, `assertCanBeDeleted()` throws `CalendarItemReadOnlyException`. Cover validation rules currently on `CalendarItem.createForEvent` / `CalendarItem.synchronizeFromEvent`.
- [ ] 2.3 Refactor `CalendarItem` to an abstract base class holding only the fields and behavior common to all kinds (`id`, `name`, `description`, `startDate`, `endDate`, `auditMetadata`, getters, `equals`/`hashCode`). Declare `assertCanBeDeleted()` as abstract. Remove `eventId`, `isEventLinked()`, and all event-linked factories/methods from the base.
- [ ] 2.4 Create `ManualCalendarItem` extending `CalendarItem`: package-private constructor, `reconstruct(...)` factory, `create(CreateCalendarItem)` factory, `update(UpdateCalendarItem)`, `assertCanBeDeleted()` returns normally.
- [ ] 2.5 Create `EventLinkedCalendarItem` abstract class extending `CalendarItem`: holds `@Association EventId eventId`, package-private constructor, `assertCanBeDeleted()` throws `CalendarItemReadOnlyException`, exposes `getEventId()`.
- [ ] 2.6 Create `EventDateCalendarItem` extending `EventLinkedCalendarItem`: package-private constructor, `reconstruct(...)` factory, `createForEvent(CreateCalendarItemForEvent)` factory, `synchronizeFromEvent(SynchronizeFromEvent)`.
- [ ] 2.7 Verify all domain unit tests from 2.1 and 2.2 now pass (green).

## 3. Repository adapter

- [ ] 3.1 Update `CalendarRepository.findByEventId(EventId)` return type from `Optional<CalendarItem>` to `List<CalendarItem>`.
- [ ] 3.2 Rewrite `CalendarRepositoryAdapter` memento→domain mapping: `switch` on `memento.kind()` dispatches to the correct subtype's `reconstruct`.
- [ ] 3.3 Rewrite `CalendarRepositoryAdapter` domain→memento mapping: pattern-match on concrete subtype (`ManualCalendarItem`, `EventDateCalendarItem`) to derive `kind` and the optional `eventId`.
- [ ] 3.4 Update `CalendarRepositoryAdapter`'s `findByEventId` to return a list.
- [ ] 3.5 Update `CalendarRepositoryAdapterTest` and `CalendarJdbcRepositoryTest`: cover both round-trip directions for `ManualCalendarItem` and `EventDateCalendarItem`; assert the `kind` column is set correctly; assert `findByEventId` returns a list containing the expected item.

## 4. Application services

- [ ] 4.1 Update `CalendarEventSyncService.handleEventPublished` to build an `EventDateCalendarItem` via its factory.
- [ ] 4.2 Update `CalendarEventSyncService.handleEventUpdated` to fetch the list via `findByEventId`, filter to `EventDateCalendarItem.class`, and call `synchronizeFromEvent` on the result. Preserve existing "item missing → warn + skip" behavior (full reconcile will be introduced in the follow-up change).
- [ ] 4.3 Update `CalendarEventSyncService.handleEventCancelled` to fetch the list via `findByEventId`, filter to `EventDateCalendarItem.class`, and delete.
- [ ] 4.4 Update `CalendarManagementService` CRUD paths to operate on `ManualCalendarItem` (create via `ManualCalendarItem.create`, delete via `assertCanBeDeleted` + `repository.delete`).
- [ ] 4.5 Adjust `CalendarEventSyncServiceTest` and `CalendarManagementServiceTest` to the new types; behavior-level assertions (what happens to the repository on each event / each command) remain unchanged.

## 5. REST controller

- [ ] 5.1 Replace `isEventLinked()` checks in `CalendarController.addLinksForCalendarItem` with `instanceof ManualCalendarItem` (or the inverse): manual items get update/delete affordances on self; event-linked items get a plain self link + `event` link.
- [ ] 5.2 Verify `CalendarItemDto` remains unchanged (flat, no `kind` field).
- [ ] 5.3 Update `CalendarControllerTest` and any MockMvc test: assertions about which affordances appear remain unchanged; only the way the test constructs the underlying aggregate changes.

## 6. Test infrastructure

- [ ] 6.1 Split `CalendarItemTestDataBuilder` into `ManualCalendarItemTestDataBuilder` and `EventDateCalendarItemTestDataBuilder` (or introduce typed builder methods on a single builder class).
- [ ] 6.2 Update `CalendarItemAssert` if it inspects `eventId` directly; prefer assertions based on the concrete subtype (`extracting(CalendarItem::getClass)` or `instanceof`).
- [ ] 6.3 Update `CalendarEventSyncIntegrationTest` to use the new factories / builders.

## 7. Verification

- [ ] 7.1 Run the full backend test suite; confirm all tests pass.
- [ ] 7.2 Start the backend locally, verify the calendar list and calendar item detail endpoints return the same HAL representations as before for manual and event-linked items (smoke test).
- [ ] 7.3 Run `openspec validate refactor-calendar-item-to-polymorphic-hierarchy --strict`.
