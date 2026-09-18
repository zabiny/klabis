## Why

Every inward synchronisation wakes a writer that immediately contends with it.

When a pass decides to adopt ORIS's values, it calls
`OrisEventSyncAdapter.applyToLocal` → `Event.syncFromOris(...)`, which registers an
`EventUpdatedEvent`. That event is
consumed asynchronously by `EventsSyncListener` (`@ApplicationModuleListener`), which
calls `SynchronizationPort.markDirty` on the very record the pass is about to finish
writing. The engine marks its own write as an unsynchronised local change.

Two costs follow:

- **A pointless extra pass.** The record is left dirty by the write that just brought
  it into step, so the next due scan picks it up, re-reads both sides, finds nothing
  changed and writes a `NOTHING_TO_DO` attempt. For an active club calendar this is a
  steady trickle of work and history rows produced entirely by the engine reacting to
  itself.
- **A self-inflicted write race.** The archived change
  `2026-09-10-sync-followup-outcome-writer-transactions` records this exactly: *"The
  two writers are not two steps of one operation — they are two threads. A pass writes
  the local entity, that write raises `EventUpdatedEvent`, and the asynchronous
  `EventsSyncListener` calls `markDirty` on the very `sync_record` row the pass is
  about to save. The pass wakes the writer that then races it."* That race is the
  reason scheduling was moved out of the aggregate into `sync.sync_schedule`.

The signal is already distinguishable at the source: `Event` has a dedicated
`syncFromOris(SyncFromOris)` method, separate from the ordinary update path, and only
the synchronisation engine ever calls it. Nothing about that origin reaches the
listener today, so the listener cannot tell a manager's edit from the engine's own
write.

**What this does not fix.** A manager editing the event *while* a pass is in flight is
a genuine concurrent write — the external HTTP call runs with no transaction open
(design.md D12), so the window is real and `markDirty` must still be able to land
during it. This change removes only the engine's self-inflicted half. In particular it
does **not** make it safe to move `dirty_since` back onto `SyncRecord` or to delete
`sync.sync_schedule`; see the "No" note in Impact.

## What Changes

- **`EventUpdatedEvent` carries the origin of the update** — whether the change came
  from a synchronisation write or from an ordinary edit. `Event.syncFromOris` and the
  ordinary update path already register the event from two distinct methods, so the
  distinction is available where the event is constructed and needs no ambient or
  thread-local state.
- **`EventsSyncListener` ignores an update that originated in synchronisation.** It
  keeps handling every other update exactly as now.
- **No other consumer changes behaviour.** `calendar`'s `EventsEventListener` also
  consumes `EventUpdatedEvent` and must keep reacting to *all* updates — a
  synchronisation write changes what the calendar shows just as much as a manual edit
  does.

Explicitly **not** in this change:

- Removing `sync.sync_schedule`, `ScheduleEffect` or the `SyncScheduleRepository` port
  (see Impact).
- Changing when a pass runs, what it compares, or how conflicts and retries behave.
- Suppressing the `EventUpdatedEvent` itself. It is a true statement about the
  aggregate and other modules depend on it; only the sync listener's reaction changes.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — unaffected.
  - *"Records Are Kept In Step Automatically"* requires that the system detect which
    side changed and *"SHALL NOT write anything when neither side has changed"*. The
    suppressed marker refers to a write the engine performed itself, immediately after
    which both sides agree. Today that marker triggers a pass which correctly
    concludes nothing changed and writes nothing; afterwards the pass does not happen
    at all. The observable outcome — nothing written — is identical.
  - *"A Local Change Is Never Silently Overwritten"* is the requirement to be careful
    about here, and it is preserved: only an update *originating in synchronisation*
    is ignored. Every genuine local edit, including one made during a pass, still
    marks the record dirty.
  - *"Every Synchronisation Attempt Is Recorded"* — attempts are still recorded in
    full. Fewer attempts *occur*, but no attempt that occurs goes unrecorded. The spec
    requires that each attempt be recorded, not that a particular number of attempts
    happen.
- `openspec/specs/events/spec.md` — unaffected. The ORIS sync write itself, the fields
  it touches and the event's resulting state are unchanged.
- `openspec/specs/calendar-items/spec.md` — unaffected, and deliberately so: the
  calendar listener keeps receiving every `EventUpdatedEvent`, synchronisation-sourced
  ones included.

**Why no spec update is needed:**

This suppresses an internal signal the engine sends to itself. No requirement states
how many passes run, and none is written in terms of the dirty marker — `design.md` D9
is explicit that it is a scheduling hint that must never affect correctness. The
user-visible result of a synchronisation is the same before and after; what changes is
that the engine stops re-examining a record it has just reconciled.

**The boundary to watch:** if the origin flag were ever wrong in the "came from sync"
direction — a genuine local edit misclassified as a synchronisation write — that edit
would never be pushed and *"A Local Change Is Never Silently Overwritten"* would be
violated silently. The classification must therefore be derived from the code path
that performed the write, never inferred from the data. This is the main implementation
risk of the change.

## Impact

**Affected code (backend):**

- `com.klabis.events.EventUpdatedEvent` — a field naming the origin of the update.
  Note it is `@RecordBuilder` and consumed by two modules, so the addition needs a
  sensible default for existing construction sites.
- `com.klabis.events.domain.Event` — the two registration sites (`Event.java:745`,
  the ordinary update, and `Event.java:782`, `syncFromOris`) pass their respective
  origins. These are the *only* two places that register this event, which is what
  makes the classification reliable.
- `com.klabis.events.infrastructure.listeners.EventsSyncListener` — skips
  synchronisation-originated updates in `handle(EventUpdatedEvent)`. The
  `EventFinishedEvent` / `EventCancelledEvent` handlers are untouched.
- `com.klabis.calendar.infrastructure.listeners.EventsEventListener` — unchanged, by
  design.

**Data:** none. `EventUpdatedEvent` is a domain event; if Spring Modulith's event
publication registry persists it, the serialised shape gains a field — worth checking
against any stored, not-yet-completed publications at deploy time.

**APIs (REST):** none. **Frontend:** none. **Dependencies:** none.

**What this explicitly does NOT enable.** It is tempting to conclude that with the
self-inflicted race gone, `dirty_since` could return to `SyncRecord` and
`sync.sync_schedule` could be deleted. It could not:

- A manager's edit arriving during the external call (D12 leaves no transaction open
  across it) is a real concurrent write, not an artefact. On the aggregate, that
  `markDirty` would take the optimistic lock and contend with
  `SyncOutcomeWriter.persist` exactly as before.
- Losing that contention is worse than losing the self-inflicted one: swallowing the
  `OptimisticLockingFailureException` would drop a *genuine* local change, so it would
  never be synchronised.
- The same archived change also records that `markDirty` contending for the version
  makes `SyncRecordClaimer.claim` fail with `OptimisticLockingFailureException` rather
  than `SyncRecordClaimedException` when no competing pass exists. That would return
  too.

So this change stands on its own merits — fewer useless passes, less history noise,
one less way for two writers to meet — and does not reopen the scheduling design. See
`consider-migration-of-sync-to-quartz` for why that stays as it is.

**Testing:**

- The behaviour is a *non*-event (no marker raised), which is easy to assert for the
  wrong reason. A test must first show the record becomes dirty on an ordinary update,
  then show it does not on a synchronisation write — otherwise a listener that is
  broken outright would pass.
- An integration test covering the real sequence (inward pass → `applyToLocal` →
  async listener → no dirty marker) is worth having, since the asynchrony is the whole
  point and a unit test of the listener cannot show it.

## Open Questions

1. **How is the origin represented on the event?** A boolean (`fromSynchronization`)
   is the smallest thing that works. An enum (`MANUAL` / `SYNCHRONISATION`, perhaps
   later `IMPORT`) says more and reads better at the listener, at the cost of a type
   that other modules must then understand. Recommend the enum only if a third origin
   is genuinely foreseeable; otherwise the boolean.

2. **Does the same treatment belong on `EventFinishedEvent` / `EventCancelledEvent`?**
   Those handlers call `retire`, not `markDirty`, and the sync adapter is `pullOnly`
   so it never causes them. Probably out of scope — to confirm rather than assume.

3. **Are there other self-inflicted markers?** `EVENT` is the only entity type
   currently enrolled, so `EventsSyncListener` is the only place this pattern exists
   today. Any future entity type with a local write path will need the same care —
   is that worth stating somewhere the next implementer will see it, and where?

4. **Does the stored-event-publication shape matter here?** If Spring Modulith is
   persisting event publications, a publication serialised before the deploy and
   completed after it must still deserialise. To check against the actual
   configuration.
