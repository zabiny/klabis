## Why

`SyncRecordJdbcRepository#findDueForScan` selects scan candidates on three conditions —
not retired, dirty or retry-due, claim not fresh — and excludes terminally failed records
only indirectly. Its javadoc states the reasoning:

> `CONFLICT` and `FAILED` exclude themselves naturally, since both clear `dirty_since` and
> `next_attempt_due_at` when entered

**That statement is half wrong, and the wrong half is load-bearing.** `recordConflict`
clears both fields (`SyncRecord.java:274-275`). `recordTerminalFailure` clears only
`nextAttemptDueAt` (`SyncRecord.java:392`) — it leaves `dirtySince` untouched. A `FAILED`
record with a non-null `dirty_since` therefore matches the due-scan predicate, and
`SyncScheduler#runDueScan` hands it to `runScheduledPass`, whose first act is an
`Assert.state` that rejects `FAILED` (`SynchronizationService.java:158`).

This is reachable with the code as it stands, not only after some future change:

1. A record fails its last permitted attempt → `FAILED`, `dirty_since` whatever it was.
2. Any ordinary user edit of that event publishes `EventUpdatedEvent` (`Event.java:671` —
   the plain update path, unrelated to any sync pass).
3. `EventsSyncListener` calls `markDirty` unconditionally; `SynchronizationService#markDirty`
   (line 102) looks the record up and calls `record.markDirty()` with **no status check** —
   `SyncRecord#markDirty` (line 107) sets `dirtySince` unconditionally.
4. The next due scan selects it and throws.

The scheduler catches `RuntimeException` per record and logs at ERROR
(`SyncScheduler.java:77-79`), so the scan continues and no data is harmed. The cost is a
recurring ERROR-level stack trace every scan interval, for every terminally failed record
that anyone edits — noise that looks like a defect in the scheduler and points nowhere near
`markDirty`.

The underlying problem is structural: the safety of a query in the persistence layer
depends on a state invariant maintained in the domain layer, and the query does not say so.
Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`).

## What Changes

- Add `AND status <> 'FAILED'` to `findDueForScan`. The exclusion the query depends on
  becomes local to the query, matching what `findAllActive` already does in SQL for the
  nightly full pass.
- Correct the `findDueForScan` javadoc: `CONFLICT` self-excludes (it clears both scheduling
  fields), `FAILED` does not and is excluded explicitly. The current text asserts something
  untrue about `FAILED` and would keep misleading the next reader.
- Do **not** change `markDirty` to skip `FAILED` records. Dirty-since is a scheduling
  signal, never consulted for correctness (design.md D9, D11), and a record that is edited
  while failed genuinely is dirty — that flag is what makes it re-evaluate correctly once a
  manager resets it. Suppressing the flag would lose that. The scan is the right place to
  decide what not to attempt.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — unaffected. "Failed Synchronisation Is
  Retried, Then Stops And Waits" requires that a terminally failed record stops being
  attempted and waits for a manager. Today it is not attempted because a guard throws
  after it is selected; afterwards it is not selected at all. The requirement's observable
  promise — no attempt happens — holds identically before and after.
- `openspec/specs/non-functional-requirements/spec.md` — unaffected. Scan cadence,
  retry limits and lease durations are untouched.

**Why no spec update is needed:**

No record changes status, no attempt is recorded that was not recorded before, and no
record becomes eligible that was not eligible before. The only difference is *where* a
`FAILED` record is rejected: in the SQL predicate instead of in an application-layer
assertion one call later. The user-visible effect is the disappearance of ERROR log entries
for work that was never performed anyway.

Strictly speaking this removes a thrown exception, which is why the reasoning is spelled
out rather than asserted: the exception was caught and swallowed by the scheduler's
per-record handler, so it never propagated to a caller, an API response, or a failed scan.

## Impact

- **Modules:** `sync` only, persistence layer.
- **Code:** one SQL predicate and one javadoc block in
  `SyncRecordJdbcRepository`. No signature, no domain, no application-layer change.
- **Tests:** `SyncRecordJdbcRepositoryTest` gains a case for a `FAILED` record carrying a
  non-null `dirty_since` — it must be absent from `findDueForScan`, and that test must fail
  if the new predicate is removed. This case does not exist today, which is why the gap
  survived review.
- **Performance:** the schema's index is on `(dirty_since, next_attempt_due_at)`; adding a
  status predicate narrows the result set and does not change index usage.
- **Risk:** low. The change is additive to a `WHERE` clause and removes candidates that the
  next layer rejects outright.
