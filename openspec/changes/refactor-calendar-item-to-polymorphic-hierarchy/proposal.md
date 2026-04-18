## Why

The `CalendarItem` aggregate currently uses a nullable `eventId` field as an implicit discriminator between **manual** and **event-linked** items. Read-only rules, factory methods, and synchronization logic are guarded by runtime `isEventLinked()` checks scattered across domain, application, and infrastructure layers. This is primitive obsession: the *kind* of item drives behavior, but the code expresses it through a flag.

A follow-up change will introduce a second event-linked kind (registration deadline calendar items). Before adding a second variant, the domain should express kinds as types, so that:

- read-only vs. editable is a compile-time guarantee (different subtypes), not a runtime `throw`
- each variant owns its own factory and synchronization methods (no methods that apply only to one kind living on the base)
- adding a third variant (or future kinds) becomes mechanical and risk-free

This change is a pure refactor: no user-visible behavior changes, no API changes.

## What Changes

- **BREAKING (internal only)**: `CalendarItem` becomes an abstract base class. Direct instantiation is no longer possible; callers must use subtype factories. This affects only in-repo code (domain, application, infrastructure, tests) — no public API.
- Introduce `ManualCalendarItem` subtype — carries `update()` and `assertCanBeDeleted()` (returns normally).
- Introduce `EventLinkedCalendarItem` intermediate abstract subtype — holds `eventId` association and enforces read-only by throwing `CalendarItemReadOnlyException` from `assertCanBeDeleted()`.
- Introduce `EventDateCalendarItem` concrete subtype under `EventLinkedCalendarItem` — carries `createForEvent()` and `synchronizeFromEvent()`.
- Introduce `CalendarItemKind` enum (`MANUAL`, `EVENT_DATE`) used only as persistence discriminator; the enum is internal to the persistence layer and MAY be referenced by application code when needed.
- `CalendarMemento` gains a `kind` field (NOT NULL, default `EVENT_DATE`) — single-table inheritance mapping. `V001` schema script updated in place.
- `CalendarRepositoryAdapter` performs polymorphic dispatch: memento→domain switches on `kind`, domain→memento pattern-matches on concrete subtype.
- `CalendarRepository.findByEventId(EventId)` return type changes from `Optional<CalendarItem>` to `List<CalendarItem>` — preparation for multiple event-linked items per event (a future change will add deadline items; today the list contains 0..1).
- `CalendarEventSyncService` works against `EventDateCalendarItem` directly; filters by `instanceof EventDateCalendarItem` after `findByEventId`.
- `CalendarManagementService` works against `ManualCalendarItem` directly.
- `CalendarController` replaces `isEventLinked()` branching with `instanceof ManualCalendarItem` for affordance rendering.
- `CalendarItemDto` remains unchanged (flat, no new `kind` field).
- `CalendarItemReadOnlyException` semantics unchanged; throw site moves from `CalendarItem` into `EventLinkedCalendarItem.assertCanBeDeleted()` and equivalent guards.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calendar-items`: two requirements are re-worded to no longer assume "at most one event-linked calendar item per event". The refactor itself does not introduce a second event-linked item (that is the follow-up change), but it is what makes such an extension possible, and the spec should stop asserting a one-to-one invariant the codebase is about to outgrow. All scenarios are preserved; only the normative wording is adjusted.

## Impact

**Affected code (backend only):**

- `backend/src/main/java/com/klabis/calendar/domain/CalendarItem.java` — split into base class + subtypes
- `backend/src/main/java/com/klabis/calendar/domain/CalendarRepository.java` — `findByEventId` signature change
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarMemento.java` — add `kind` field
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarRepositoryAdapter.java` — polymorphic dispatch
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarJdbcRepository.java` — adjust queries (list return, `kind` column)
- `backend/src/main/java/com/klabis/calendar/application/CalendarEventSyncService.java` — work with `EventDateCalendarItem`
- `backend/src/main/java/com/klabis/calendar/application/CalendarManagementService.java` — work with `ManualCalendarItem`
- `backend/src/main/java/com/klabis/calendar/infrastructure/restapi/CalendarController.java` — replace `isEventLinked()` branching
- `backend/src/main/resources/db/migration/V001*.sql` — add `kind` column (update in place, not a new migration)
- All calendar unit & integration tests — update to new factory API; behavior expectations unchanged

**Frontend:** None. `CalendarItemDto` shape is unchanged.

**APIs (REST):** None. All endpoints, status codes, request/response shapes, HAL links and affordances are unchanged.

**Dependencies:** None added or removed.

**Data:** H2 in-memory, no production deployment — no data migration needed.

**Specs:** `calendar-items/spec.md` — two requirements re-worded (no behavioral change, just opens wording for multiple event-linked items per event).
