## 1. `Event` records the write's origin on `EventUpdatedEvent`

- [x] 1.1 Write a failing unit test on `Event`'s ordinary update method
      asserting the registered `EventUpdatedEvent.origin()` equals
      `UpdateOrigin.MANUAL` (reference the not-yet-existing `UpdateOrigin`
      type and `origin()` accessor — this drives their creation).
- [x] 1.2 Write a failing unit test on `Event.syncFromOris` asserting the
      registered `EventUpdatedEvent.origin()` equals `UpdateOrigin.SYNCHRONISATION`.
- [x] 1.3 Add `com.klabis.events.UpdateOrigin` enum with values `MANUAL`,
      `SYNCHRONISATION`, documenting that it must reflect the code path that
      performed the write and must never be inferred from data (design.md D1, D2).
- [x] 1.4 Add `origin` component (`UpdateOrigin`, non-null, no default) to
      `EventUpdatedEvent`; update the compact constructor's `Objects.requireNonNull`
      checks accordingly; change `EventUpdatedEvent.fromAggregate` to accept
      an `UpdateOrigin` parameter and pass it through.
- [x] 1.5 Update the ordinary update path (`Event.java:714`) to call
      `fromAggregate` with `UpdateOrigin.MANUAL`, and `Event.syncFromOris`
      (`Event.java:748`) with `UpdateOrigin.SYNCHRONISATION` — the minimal
      change to make both tests from 1.1/1.2 pass.
- [x] 1.6 Fix all remaining compile errors from the new required
      constructor/factory parameter across production and test code
      (`EventUpdatedEventBuilder` usages, direct `new EventUpdatedEvent(...)`
      calls, unrelated test fixtures) without changing their assertions.
- [x] 1.7 Refactor: review naming/placement of `UpdateOrigin` and the two
      call sites for clarity; re-run tests from 1.1/1.2 to confirm still green.

## 2. `EventsSyncListener` ignores self-inflicted updates

- [x] 2.1 Write a failing unit test asserting `SynchronizationPort.markDirty`
      is NOT called when `EventsSyncListener.handle(EventUpdatedEvent)`
      receives an event with `UpdateOrigin.SYNCHRONISATION` (must fail against
      the current, unconditional implementation).
- [x] 2.2 Write (or confirm existing coverage for) a test asserting `markDirty`
      IS still called for `UpdateOrigin.MANUAL`, so the fix can't overshoot
      into ignoring everything.
- [x] 2.3 Update `EventsSyncListener.handle(EventUpdatedEvent)` with the
      minimal change to make 2.1 pass while keeping 2.2 green: return without
      calling `markDirty` when `event.origin() == UpdateOrigin.SYNCHRONISATION`.
- [x] 2.4 Refactor if needed; confirm `handle(EventFinishedEvent)` /
      `handle(EventCancelledEvent)` are untouched and their existing tests
      still pass unmodified.

## 3. Verify no regression in other consumers

- [x] 3.1 Confirm `calendar.infrastructure.listeners.EventsEventListener` needs
      no code change and its existing tests pass unmodified (it must keep
      reacting to `EventUpdatedEvent` regardless of `origin()`).
- [x] 3.2 Grep for any other `EventUpdatedEvent` consumers or constructors
      project-wide to confirm the two registration sites in `Event` remain the
      only producers.

## 4. Integration coverage for the real self-inflicted sequence

- [x] 4.1 Write an integration test covering the actual asynchronous sequence:
      an inward sync pass → `OrisEventSyncAdapter.applyToLocal` →
      `Event.syncFromOris` → async `EventsSyncListener` handling → assert the
      paired `SyncRecord` is NOT left dirty afterwards. Confirm it would have
      failed before section 2's change (run against a stash/revert of 2.3, or
      reason from the unit test in 2.1 that it exercises the same branch).
- [x] 4.2 Add/keep a companion integration test (or reuse an existing one)
      showing an ordinary manual update DOES leave the record dirty, so a
      listener broken outright (e.g. always skipping) would fail this test.
- [x] 4.3 Confirm both integration tests pass against the current implementation.

## 5. Full verification

- [x] 5.1 Run the full `events` and `sync` module test suites; confirm no
      existing test needed a non-mechanical change (only the required-origin
      compile fixes from task 1.6).
- [x] 5.2 Update `tasks.md` checkboxes and confirm `openspec status` shows the
      change ready to archive.
