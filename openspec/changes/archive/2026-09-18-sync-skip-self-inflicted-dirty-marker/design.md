## Context

`EventUpdatedEvent` is registered from exactly two places in `Event`
(`Event.java:714`, the ordinary update path, and `Event.java:748`,
`syncFromOris`). Both are consumed by `EventsSyncListener.handle(EventUpdatedEvent)`,
which unconditionally calls `SynchronizationPort.markDirty` on the paired
`SyncRecord`. When the update came from `syncFromOris` itself, this marks the
record dirty immediately after the synchronisation engine finished
reconciling it — a self-inflicted signal that later causes a pointless extra
pass and, per the archived
`2026-09-10-sync-followup-outcome-writer-transactions` change, a write race
between the pass and the async listener.

`calendar.infrastructure.listeners.EventsEventListener` also consumes
`EventUpdatedEvent` and must keep reacting to every update, synchronisation
writes included — the calendar reflects both origins equally.

## Goals / Non-Goals

**Goals:**
- Stop `EventsSyncListener` from marking a record dirty when the triggering
  `EventUpdatedEvent` originated from the synchronisation engine's own write
  (`Event.syncFromOris`).
- Keep every other consumer of `EventUpdatedEvent` (`calendar`'s listener)
  behaving exactly as today, for both origins.
- Make the classification derive from the code path that performed the
  write (which of the two registration sites fired), never from inferred
  data — a misclassified genuine edit must never be silently dropped.

**Non-Goals:**
- Changing pass scheduling, comparison logic, or conflict/retry behaviour.
- Removing `sync.sync_schedule`, `ScheduleEffect`, or `SyncScheduleRepository`.
- Suppressing `EventUpdatedEvent` itself, or changing its consumption by
  `calendar`.
- Applying the same treatment to `EventFinishedEvent` / `EventCancelledEvent`
  — out of scope per the resolved open question; their handlers call `retire`,
  not `markDirty`, and the sync adapter is `pullOnly` so it never triggers
  them.

## Decisions

**D1 — Origin representation: `UpdateOrigin` enum.**
`EventUpdatedEvent` gains a new component `UpdateOrigin origin`, an enum with
two values: `MANUAL` and `SYNCHRONISATION`. Chosen over a boolean because it
reads better at the call site and at the listener (`origin ==
UpdateOrigin.SYNCHRONISATION` vs. a bare `fromSynchronization` flag), and
because a third origin (e.g. `IMPORT`) is a plausible future addition that
the enum absorbs without a signature change. The type lives in
`com.klabis.events` next to `EventUpdatedEvent`, alongside the event's other
top-level types — `calendar` already depends on this package for the event
itself and gains no new module coupling.

```java
public enum UpdateOrigin {
    MANUAL,
    SYNCHRONISATION
}
```

**D2 — Classification is derived at the two registration sites, not inferred.**
`Event.registerUpdate()` (the ordinary update path, `Event.java:714`) calls
`EventUpdatedEvent.fromAggregate(this, UpdateOrigin.MANUAL)`.
`Event.syncFromOris(...)` (`Event.java:748`) calls
`EventUpdatedEvent.fromAggregate(this, UpdateOrigin.SYNCHRONISATION)`. These
are the only two call sites today, which is what makes the classification
exhaustive and reliable — no other code path can produce an
`EventUpdatedEvent` with the wrong origin by construction.

**D3 — `EventsSyncListener` skips `SYNCHRONISATION`-origin updates.**
`handle(EventUpdatedEvent event)` returns immediately when
`event.origin() == UpdateOrigin.SYNCHRONISATION`, without calling
`markDirty`. Every `MANUAL`-origin update is handled exactly as today.

**D4 — `EventsEventListener` (calendar) is unchanged.**
It keeps reacting to every `EventUpdatedEvent` regardless of `origin()` — a
synchronisation write changes what the calendar displays just as much as a
manual edit.

**D5 — `RecordBuilder` default for existing construction sites.**
`EventUpdatedEvent` is `@RecordBuilder`. Since `fromAggregate` is the only
factory used in production code and both call sites are updated to pass an
explicit origin, no default value is needed on the record itself; the
canonical constructor keeps requiring a non-null `origin` like its other
required fields. Test code constructing this event directly (if any) must
also supply an explicit origin — this is a compile-time change that surfaces
every affected test.

## Risks / Trade-offs

- **Risk:** a future third call site for `EventUpdatedEvent` forgets to pass
  the correct origin, silently reintroducing the swallowed-edit failure mode
  (a genuine local change classified as `SYNCHRONISATION` would never reach
  `markDirty`, violating "A Local Change Is Never Silently Overwritten").
  **Mitigation:** the constructor requires `origin` as a non-null component
  (no default), so a new call site fails to compile without deciding it
  explicitly; the class Javadoc on `EventUpdatedEvent` states that `origin`
  must reflect the actual code path, never be inferred from data.
- **Trade-off:** the enum introduces a small new public type that
  `calendar` (and any other future consumer) technically depends on, even
  though `calendar` ignores its value. Accepted per D1 — the type carries no
  behaviour and the alternative (boolean) would need renaming if a third
  origin appears later.
