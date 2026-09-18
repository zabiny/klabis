## Why

`SyncOutcomeWriter` defends against one race with four mechanisms stacked on top of each
other:

1. a `@Lazy` self-injected proxy, so `@Transactional` applies to an internally-invoked
   method,
2. `Propagation.REQUIRES_NEW`, because a retry inside the failed transaction would trade
   `OptimisticLockingFailureException` for `UnexpectedRollbackException`,
3. a manual retry that re-reads the stored version and overwrites the record's
   `AuditMetadata` version stamp — which effectively disables optimistic locking for that
   write,
4. and, in `SynchronizationService.markDirty`, a swallowed `OptimisticLockingFailureException`.

Each layer is individually justified, and the javadoc explains all four honestly — this
is not careless code. But the stack exists because two independent things write to the
same `sync_record` row: the outcome of a pass, and the dirty marker raised when an inward
write triggers the module's own `EventUpdatedEvent` listener.

The two writers are not two steps of one operation — they are two threads. A pass writes
the local entity, that write raises `EventUpdatedEvent`, and the asynchronous
`EventsSyncListener` calls `markDirty` on the very `sync_record` row the pass is about to
save. The pass wakes the writer that then races it.

The engine's own design already says what the fix is. `design.md` D9 records that
`dirtySince` is a scheduling hint that must never affect correctness. If scheduling is not
domain state, it does not belong in an aggregate that carries a version — and once it does
not take the version, there is nothing left to race and all four layers become
unnecessary.

**A second race, not previously recorded, disappears with it.** `SyncRecordClaimer.claim`
is a read-check-write: `isClaimAvailable` reads possibly-stale state, and the actual
mutual exclusion between two concurrent passes rests entirely on the optimistic lock in
`save`. Because `markDirty` contends for that same version today, a dirty marker can make
a claim fail with `OptimisticLockingFailureException` — not `SyncRecordClaimedException` —
when no competing pass exists at all. Taking `markDirty` off the version removes that
spurious failure. The claim's own guarantee is untouched by this change (see Impact).

Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`) and deferred from it: this is a change to how
the module persists, not cleanup.

## What Changes

**Scheduling moves out of the aggregate into its own table and its own port.**

- A new `sync.sync_schedule` table holds `dirty_since` and `next_attempt_due_at`, keyed by
  `sync_record_id`. It carries no version column: a scheduling write is not a domain
  change and must never contend for one.
- `claimed_at` deliberately **stays** on `sync_record`, under optimistic locking. It is a
  correctness mechanism (D12 — one pass at a time per record), not a scheduling hint, and
  moving it is a separate change to D12 with a different risk profile. See Impact.
- A new `SyncScheduleRepository` secondary port persists the schedule. `markDirty` writes
  through it alone and no longer touches the aggregate at all.

**Domain methods stop mutating scheduling state and start returning it.**

`SyncRecord` no longer holds `dirtySince` or `nextAttemptDueAt`. The methods that
currently clear or set them return a `ScheduleEffect` describing what should happen to the
schedule, which `SynchronizationService` applies:

```java
ScheduleEffect effect = record.recordSuccess(direction, local, external, now);
// ... later, in the same transaction as the record and the attempt row:
scheduleRepository.apply(record.getId(), effect);
```

The affected methods and their effects, matching today's behaviour exactly:

| Method | Effect today | `ScheduleEffect` |
|---|---|---|
| `recordSuccess` | clears both | `clear()` |
| `recordConverged` | clears both | `clear()` |
| `recordConflict` | clears both | `clear()` |
| `acceptDivergence` | clears both | `clear()` |
| `recordTerminalFailure` | clears `nextAttemptDueAt` only | `clearDueAt()` |
| `reset` | clears `nextAttemptDueAt` only | `clearDueAt()` |
| `recordOutage` | sets `nextAttemptDueAt` | `dueAt(instant)` |
| `recordRetryableFailure` | sets `nextAttemptDueAt` | `dueAt(instant)` |
| `recordOutwardWriteWithSkippedAdvance` | sets `dirtySince` | `dirtySince(instant)` |
| `markDirty` | sets `dirtySince` | `dirtySince(instant)` |

Note that `recordTerminalFailure` and `reset` clear only `nextAttemptDueAt` and leave
`dirtySince` standing — deliberately preserved as-is rather than tidied into `clear()`,
because a record that was dirty when it failed is still dirty after a reset and must be
picked up. Each row above is transcribed from the current method body and must be
re-verified against it during implementation, not trusted from this table.

**The read side keeps the schedule attached to the record.**

`SynchronizationService#runPass` consults `record.getDirtySince()` in the version-token
short-circuit (`SynchronizationService.java:338`). The schedule is therefore loaded
together with the record — never lazily — so that short-circuit keeps working. `SyncRecord`
exposes the loaded schedule for that read; it just no longer owns the mutation.

`findDueForScan` and `findAllActive` join `sync_schedule` instead of filtering columns on
`sync_record`. The `idx_sync_record_due_scan` index moves to the new table.

**With the write conflict gone, the layers it forced are removed:**

- the manual optimistic-lock retry and the version-stamp overwrite in `SyncOutcomeWriter`,
- `Propagation.REQUIRES_NEW`, which existed only to give the retry a fresh transaction,
- the `@Lazy` self-proxy, which existed only to make that retry transactional,
- the swallowed `OptimisticLockingFailureException` in `SynchronizationService.markDirty`.

**Atomicity is preserved.** `design.md` D15 requires that a crash never leave an attempt
unrecorded. The record, its attempt row and its schedule are written in one transaction.
`SyncOutcomeWriter` stays a separate bean so the call from `SynchronizationService` still
crosses a proxy boundary — that reason survives even though the retry it also served does
not.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — unaffected. "Every Synchronisation
  Attempt Is Recorded" still holds: record, attempt and schedule are written in one
  transaction. "Records Are Kept In Step Automatically" is unchanged; a record marked
  dirty is still returned by the due scan, now via a join rather than a column on the same
  row.
- `openspec/specs/non-functional-requirements/spec.md` — reviewed for "One
  Synchronisation At A Time Per Record". `claimed_at` stays on the aggregate under
  optimistic locking, so the claim mechanism is untouched. What changes is that a dirty
  marker can no longer make a claim fail spuriously — a strict reduction in false
  failures, never a relaxation of the guarantee.
- `openspec/specs/events/spec.md` — unaffected.

**Why no spec update is needed:**

The outcomes persisted, their contents and their atomicity are identical, and the due scan
returns the same records for the same reasons. What changes is which writes contend for
the aggregate's version and which table the two scheduling columns live in — internal
persistence concerns with no scenario describing them. Where today a losing write retries
and succeeds, afterwards it does not have to retry; the committed result is the same
either way. `nextAttemptDueAt` remains visible through
`SyncStateResponseConverter` with the same value.

## Impact

- **Modules:** `sync` only — `SyncRecord`, `SynchronizationService`, `SyncOutcomeWriter`,
  `SyncRecordRepository` and its JDBC adapter, `SyncRecordMemento`, plus new
  `SyncSchedule`, `ScheduleEffect` and `SyncScheduleRepository`.
- **Schema:** a new `sync.sync_schedule` table and the due-scan index moved onto it. Per
  the project's migration rule, this updates `V001__initial_schema.sql` rather than adding
  a migration. Existing rows' `dirty_since` / `next_attempt_due_at` have no production
  data to preserve (no production environment yet), so no backfill is written.
- **Code:** the removal is a net reduction — four defensive layers replaced by a table
  that does not version. The `ScheduleEffect` plumbing adds back roughly what it removes,
  so the change is close to size-neutral overall, and buys a domain model where scheduling
  and domain state are no longer confusable.
- **Deliberately out of scope:** moving `claimed_at`. Doing so would replace the claim's
  read-check-write with an atomic conditional `UPDATE`, which is genuinely stronger than
  today and would return `SyncRecordClaimedException` where an
  `OptimisticLockingFailureException` leaks out now. But it changes D12's mechanism, and
  it interacts with `SyncOutcomeWriter.persist` releasing the claim in the same
  transaction as the outcome — get that wrong and a record keeps a claim it has finished
  with until the lease expires. That belongs in its own proposal.

### Risk

This is the highest-risk of the sync follow-ups. It touches transaction boundaries and
concurrency on the engine's central write path, and the failure modes are silent:

- **The short-circuit read breaks quietly.** If the schedule is loaded lazily or left
  null, `record.getDirtySince() == null` becomes true when it should not be, and the
  version-token short-circuit stops short-circuiting. Nothing fails — the engine just does
  a full read every pass. No existing test would notice. A test must assert the
  short-circuit still fires for a record with a baseline and no dirty marker.
- **A `ScheduleEffect` row is transcribed wrongly.** Each of the ten methods above must be
  checked against its current body, not against this table. The consequence of an error is
  a record that is never rescheduled, or one rescheduled when it should have settled —
  both invisible until the timing is observed.
- **The schedule write escapes the outcome transaction.** D15's atomicity covers the
  schedule too; a schedule written outside the transaction can survive a rolled-back
  outcome.

`SynchronizationServiceMarkDirtyIntegrationTest` and
`SynchronizationServiceFailureHandlingIntegrationTest` cover this area and must be
strengthened, not merely kept passing. Per the project's negative-test rule, each new test
is verified to fail when its mechanism is removed.

- **Prerequisite:** confirm against `design.md` D9, D12 and D15 in the archived change
  before starting, since the argument for the whole proposal rests on them.
