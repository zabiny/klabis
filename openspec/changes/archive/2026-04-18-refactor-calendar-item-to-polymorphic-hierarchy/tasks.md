## 1. Persistence scaffolding

- [x] 1.1 Introduce `CalendarItemKind` enum in the persistence package (`com.klabis.calendar.infrastructure.jdbc`) with values `MANUAL` and `EVENT_DATE`.
- [x] 1.2 Add `kind` field (non-null) to `CalendarMemento` record.
- [x] 1.3 Update `V001` Flyway migration: add `kind VARCHAR(32) NOT NULL DEFAULT 'EVENT_DATE'` column to the calendar items table (do not add a new migration script).
- [x] 1.4 Update `CalendarJdbcRepository` queries: include `kind` in SELECT / INSERT / UPDATE; change any method signature that currently returns a single event-linked item (by event id) to return a list.

## 2. Domain hierarchy

- [x] 2.1 Write failing domain unit tests for `ManualCalendarItem`: `create(...)` succeeds, `update(...)` mutates fields, `assertCanBeDeleted()` returns normally. Cover validation rules currently on `CalendarItem.create` / `CalendarItem.update`.
- [x] 2.2 Write failing domain unit tests for `EventCalendarItem`: `createForEvent(...)` succeeds, `synchronizeFromEvent(...)` mutates fields, `assertCanBeDeleted()` throws `CalendarItemReadOnlyException`. Cover validation rules currently on `CalendarItem.createForEvent` / `CalendarItem.synchronizeFromEvent`.
- [x] 2.3 Refactor `CalendarItem` to an abstract base class holding only the fields and behavior common to all kinds (`id`, `name`, `description`, `startDate`, `endDate`, `auditMetadata`, getters, `equals`/`hashCode`). Declare `assertCanBeDeleted()` as abstract. Remove `eventId`, `isEventLinked()`, and all event-linked factories/methods from the base.
- [x] 2.4 Create `ManualCalendarItem` extending `CalendarItem`: package-private constructor, `reconstruct(...)` factory, `create(CreateCalendarItem)` factory, `update(UpdateCalendarItem)`, `assertCanBeDeleted()` returns normally.
- [x] 2.5 ~~Create `EventLinkedCalendarItem` abstract class~~ — collapsed into 2.6; see section 8.
- [x] 2.6 Create `EventCalendarItem` extending `CalendarItem` directly: package-private constructor, `@Association EventId eventId`, `assertCanBeDeleted()` throws `CalendarItemReadOnlyException`, `getEventId()`, `reconstruct(...)` factory, `createForEvent(CreateCalendarItemForEvent)` factory, `synchronizeFromEvent(SynchronizeFromEvent)`.
- [x] 2.7 Verify all domain unit tests from 2.1 and 2.2 now pass (green).

## 3. Repository adapter

- [x] 3.1 Update `CalendarRepository.findByEventId(EventId)` return type from `Optional<CalendarItem>` to `List<CalendarItem>`.
- [x] 3.2 Rewrite `CalendarRepositoryAdapter` memento→domain mapping: `switch` on `memento.kind()` dispatches to the correct subtype's `reconstruct`.
- [x] 3.3 Rewrite `CalendarRepositoryAdapter` domain→memento mapping: pattern-match on concrete subtype (`ManualCalendarItem`, `EventCalendarItem`) to derive `kind` and the optional `eventId`.
- [x] 3.4 Update `CalendarRepositoryAdapter`'s `findByEventId` to return a list.
- [x] 3.5 Update `CalendarRepositoryAdapterTest` and `CalendarJdbcRepositoryTest`: cover both round-trip directions for `ManualCalendarItem` and `EventCalendarItem`; assert the `kind` column is set correctly; assert `findByEventId` returns a list containing the expected item.

## 4. Application services

- [x] 4.1 Update `CalendarEventSyncService.handleEventPublished` to build an `EventCalendarItem` via its factory.
- [x] 4.2 Update `CalendarEventSyncService.handleEventUpdated` to fetch the list via `findByEventId`, filter to `EventCalendarItem.class`, and call `synchronizeFromEvent` on the result. Preserve existing "item missing → warn + skip" behavior (full reconcile will be introduced in the follow-up change).
- [x] 4.3 Update `CalendarEventSyncService.handleEventCancelled` to fetch the list via `findByEventId`, filter to `EventCalendarItem.class`, and delete.
- [x] 4.4 Update `CalendarManagementService` CRUD paths to operate on `ManualCalendarItem` (create via `ManualCalendarItem.create`, delete via `assertCanBeDeleted` + `repository.delete`).
- [x] 4.5 Adjust `CalendarEventSyncServiceTest` and `CalendarManagementServiceTest` to the new types; behavior-level assertions (what happens to the repository on each event / each command) remain unchanged.

## 5. REST controller

- [x] 5.1 Replace `isEventLinked()` checks in `CalendarController.addLinksForCalendarItem` with `instanceof ManualCalendarItem` (or the inverse): manual items get update/delete affordances on self; event-linked items get a plain self link + `event` link.
- [x] 5.2 Verify `CalendarItemDto` remains unchanged (flat, no `kind` field).
- [x] 5.3 Update `CalendarControllerTest` and any MockMvc test: assertions about which affordances appear remain unchanged; only the way the test constructs the underlying aggregate changes.

## 6. Test infrastructure

- [x] 6.1 Introduce typed builder methods on `CalendarItemTestDataBuilder` (`buildManual()` / `buildEventLinked(UUID)`) rather than splitting into separate builder classes.
- [x] 6.2 Update `CalendarItemAssert` if it inspects `eventId` directly; prefer assertions based on the concrete subtype (`extracting(CalendarItem::getClass)` or `instanceof`).
- [x] 6.3 Update `CalendarEventSyncIntegrationTest` to use the new factories / builders.

## 7. Verification

- [x] 7.1 Run the full backend test suite; confirm all tests pass.
- [ ] 7.2 Start the backend locally, verify the calendar list and calendar item detail endpoints return the same HAL representations as before for manual and event-linked items (smoke test).
- [x] 7.3 Run `openspec validate refactor-calendar-item-to-polymorphic-hierarchy --strict`.

## 8. Follow-up: collapse intermediate abstract class

Design was revised: the follow-up change will add registration deadline items via a `kind` discriminator field on `EventCalendarItem` (single class, multi-kind) rather than a new subtype. The `EventLinkedCalendarItem` intermediate abstract class is therefore unnecessary.

- [x] 8.1 Delete `EventLinkedCalendarItem.java`.
- [x] 8.2 Rename `EventDateCalendarItem` → `EventCalendarItem` (JetBrains rename refactoring — updates file, class, constructors, and all 109 references in one shot).
- [x] 8.3 `EventCalendarItem` extended to inherit from `CalendarItem` directly; absorbs `@Association EventId eventId`, `assertCanBeDeleted()`, and `getEventId()` from the deleted intermediate class.
- [x] 8.4 Update all call sites: `CalendarController`, `CalendarItemAssert`, `CalendarEventSyncServiceTest`, `CalendarEventSyncIntegrationTest` — replace `EventLinkedCalendarItem` references with `EventCalendarItem`.
- [x] 8.5 Update test method names that referenced the old class names for consistency.
- [x] 8.6 Update OpenSpec artifacts (`proposal.md`, `design.md`, `tasks.md`) to reflect the 2-level hierarchy.
