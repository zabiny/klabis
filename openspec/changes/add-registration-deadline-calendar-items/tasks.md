## 1. Event module: surface registrationDeadline

- [x] 1.1 Add `registrationDeadline` (`LocalDate`) to `com.klabis.events.EventData`.
- [x] 1.2 Update `EventDataProviderImpl` to populate `registrationDeadline` from the event aggregate (`event.getRegistrationDeadline()`).
- [x] 1.3 Update any existing test setup that constructs `EventData` directly to include the new field (default to `null` where not relevant).

## 2. Persistence enum

- [x] 2.1 Add `EVENT_REGISTRATION_DATE` to `CalendarItemKind`. Keep the enum package-private; expand its access if necessary for step 3.1.
- [x] 2.2 Move `CalendarItemKind` from `com.klabis.calendar.infrastructure.jdbc` to `com.klabis.calendar` (package root) so that the domain can reference it. Visibility stays package-private.
- [x] 2.3 Update imports anywhere the enum was referenced (`CalendarMemento`, any tests).

## 3. Domain: EventCalendarItem carries a kind

- [x] 3.1 Write failing domain tests for the new factory `EventCalendarItem.createForRegistrationDeadline(...)`: builds an item with `kind = EVENT_REGISTRATION_DATE`, `name = "Přihlášky - {event name}"`, `description = null`, `startDate = endDate = deadlineDate`, `eventId` set. Rejects null event name, null eventId, and null deadline.
- [x] 3.2 Write failing tests for `EventCalendarItem.synchronizeFromEvent(EventData)`: for a `EVENT_DATE` kind item, updates name/description/startDate/endDate from event fields the same way `SynchronizeFromEvent` did. For a `EVENT_REGISTRATION_DATE` kind item, updates the label to `"Přihlášky - " + event.name()` and moves startDate/endDate to `event.registrationDeadline()`.
- [x] 3.3 Add `private final CalendarItemKind kind` field to `EventCalendarItem`. Expose via `getKind()`. Set from constructor.
- [x] 3.4 Rename existing `createForEvent(CreateCalendarItemForEvent)` factory to `createForEventDate(...)` and have it set `kind = EVENT_DATE`. Update callers.
- [x] 3.5 Add `createForRegistrationDeadline(String eventName, EventId eventId, LocalDate deadlineDate)` factory. Unconditionally sets `kind = EVENT_REGISTRATION_DATE`, `name = "Přihlášky - " + eventName`, `description = null`, `startDate = endDate = deadlineDate`. Rejects null eventName / eventId / deadlineDate.
- [x] 3.6 Replace the existing `synchronizeFromEvent(SynchronizeFromEvent)` method with `synchronizeFromEvent(EventData event)`. The method reads `this.kind` and updates fields accordingly (see design.md Decision 4 for per-kind behavior). Delete the `SynchronizeFromEvent` record — it is no longer needed; the caller passes `EventData` directly.
- [x] 3.7 Extend `reconstruct(...)` factory to accept and apply `kind`.
- [x] 3.8 Verify all domain tests from 3.1 and 3.2 now pass.

## 4. Persistence: round-trip the new kind

- [x] 4.1 Update `CalendarMemento.from()`: pattern-match branch for `EventCalendarItem` reads `eventItem.getKind()` and sets `memento.kind` accordingly (replacing the hard-coded `CalendarItemKind.EVENT_DATE`).
- [x] 4.2 Update `CalendarMemento.toCalendarItem()`: the existing `switch` on `this.kind` handles `EVENT_DATE`; add a case for `EVENT_REGISTRATION_DATE` that calls `EventCalendarItem.reconstruct(..., CalendarItemKind.EVENT_REGISTRATION_DATE, ...)`.
- [x] 4.3 Update `CalendarJdbcRepositoryTest` and `CalendarRepositoryAdapterTest`: cover both directions for a `EVENT_REGISTRATION_DATE` item. Assert the `kind` column value persists and round-trips.

## 5. Application: unified reconcile

**Prerequisite:** sections 1–4 must be complete (enum value, domain `kind` field, factories, and memento round-trip) before these tests will compile.

- [x] 5.1 Write failing unit test(s) for `CalendarEventSyncService`:
  - Publishing an event without a deadline creates exactly one `EVENT_DATE` item.
  - Publishing an event with a deadline creates both `EVENT_DATE` and `EVENT_REGISTRATION_DATE` items.
  - Updating an event that had a deadline to clear the deadline removes the deadline item (and keeps the event-date item).
  - Updating an event to add a deadline creates the deadline item.
  - Updating an event's name updates both items' labels (event-date name, deadline "Přihlášky - {name}").
  - Updating an event whose event-date item is missing (simulate by deleting) recreates it (self-heal).
  - Cancelling an event removes both items when both exist; removes only the event-date item if that was the only one.
- [x] 5.2 Refactor `CalendarEventSyncService` into a unified reconcile path. Concretely:
  - `handleEventPublished(eventId)` and `handleEventUpdated(eventId)` both call a shared private `reconcile(eventId)` method.
  - `reconcile` loads event data via `eventDataProvider`, loads existing event-linked items via `findEventCalendarItems(eventId)`, groups them by `kind`, computes expected kinds from the event, and creates / syncs / deletes accordingly.
  - `handleEventCancelled(eventId)` deletes every event-linked item returned by `findEventCalendarItems(eventId)` (both kinds).
- [x] 5.3 Confirm that the warn-and-skip code paths in the previous `handleEventUpdated` and `handleEventCancelled` are gone (no surviving branch from the pre-reconcile code). They are subsumed by the reconcile loop (self-heal when missing) and by the cancel-delete-all behavior respectively.
- [x] 5.4 Verify tests from 5.1 now pass.

## 6. Listener wiring

- [x] 6.1 Verify `EventsEventListener` continues to delegate Published / Updated / Cancelled to the service's (now reworked) port methods. No new listener. No change to the port interface signatures beyond the internal behavior.

## 7. Integration

- [ ] 7.1 Extend `CalendarEventSyncIntegrationTest` with end-to-end scenarios that publish and update events with and without registration deadlines, asserting the expected set of calendar items after each transaction.
- [ ] 7.2 Adjust any existing assertions in the calendar or events modules that counted items per event and implicitly assumed "one event = one calendar item".

## 8. Controller & DTO verification

- [ ] 8.1 Verify `CalendarController` and `CalendarItemDto` unchanged — the deadline item is returned by the list endpoint identically to an event-date item (HAL self + event link, no edit/delete affordance). Add a MockMvc assertion that a list containing both kinds returns both with the correct self + event links.

## 9. Verification

- [ ] 9.1 Run the full backend test suite via `test-runner` agent; confirm all tests pass.
- [ ] 9.2 Start the backend locally and verify: publish an event with a deadline; confirm two items appear in the month view; update the event to clear the deadline; confirm only one remains. (Smoke test.)
- [ ] 9.3 Run `openspec validate add-registration-deadline-calendar-items --strict`.
