## 1. Confirm the premise before changing anything

- [x] 1.1 Re-read `design.md` D9, D12 and D15 in `openspec/changes/archive/2026-09-04-add-bidirectional-sync-engine/` and confirm the dirty marker is genuinely a scheduling hint, not a domain change, and that D12's claim guarantee rests on `claimed_at` rather than on any scheduling column.
- [x] 1.2 Establish that the race is real and the retry is load-bearing: write a test that reproduces the `markDirty` / `SyncOutcomeWriter#persist` collision and fails when the retry in `withOptimisticLockRetry` is removed. Without this the later removal proves nothing.
- [x] 1.3 Transcribe the scheduling behaviour of all ten methods listed in the proposal's table directly from their current bodies in `SyncRecord`, and record any row where the code disagrees with the table. The table is a starting point, not the source of truth.
- [x] 1.4 If the premise does not hold — the marker turns out to carry correctness, or the claim depends on a scheduling column — stop and report rather than proceeding.

## 2. Introduce the schedule as its own concept

- [x] 2.1 Add `SyncSchedule` (value object: `dirtySince`, `nextAttemptDueAt`) and `ScheduleEffect` (`clear()`, `clearDueAt()`, `dueAt(Instant)`, `dirtySince(Instant)`, and a no-change variant) to `com.klabis.sync.domain`.
- [x] 2.2 Add the `SyncScheduleRepository` secondary port: load a schedule for a record, and apply a `ScheduleEffect` to it.
- [x] 2.3 Add the `sync.sync_schedule` table to `V001__initial_schema.sql` (`sync_record_id` PK/FK, `dirty_since`, `next_attempt_due_at`). Deliberately **no** version column — a scheduling write must never contend for one. Per the project rule, update `V001` rather than adding a new migration.
- [x] 2.4 Move `idx_sync_record_due_scan` onto the new table, indexing `(dirty_since, next_attempt_due_at)`. **Half done deliberately:** `idx_sync_schedule_due_scan` is added, but `idx_sync_record_due_scan` is left in place because `findDueForScan` still queries `sync_record` this iteration — dropping it now would silently deoptimise a live query. Dropping it moves to task 4.5, where the query actually switches tables.
- [x] 2.5 Add the JDBC memento and adapter for the schedule, following the existing `SyncRecordMemento` / `SyncRecordRepositoryAdapter` shape.
- [x] 2.6 Ensure a record always has a schedule row — created at enrolment — so no read path has to handle a missing one. Decide and document what happens for a record enrolled before this change (no production data exists, so a coded default is acceptable; state it).

## 3. Move scheduling out of the aggregate

- [x] 3.1 Remove the `dirtySince` and `nextAttemptDueAt` fields from `SyncRecord`, along with `markDirty`.
- [x] 3.2 Change the return type of the ten methods identified in 1.3 to return a `ScheduleEffect`, preserving each one's current behaviour exactly as transcribed — not as summarised in the proposal.
- [x] 3.2a `resolveWithDirection` is an eleventh call site found during 1.3: it delegates to `recordSuccess` and so inherits `clear()`. It must return that effect onward, and `SynchronizationService.resolveConflict` must apply it — easy to miss because the proposal's table does not list it separately.
- [x] 3.3 Keep `SyncRecord.getDirtySince()` / `getNextAttemptDueAt()` reading from a schedule loaded alongside the record, so existing readers keep working. The aggregate reads the schedule; it no longer owns its mutation.
- [x] 3.4 Verify `recordTerminalFailure` and `reset` still leave `dirtySince` standing (they clear only the due-at today) — a record that was dirty when it failed must still be dirty after a reset.
- [x] 3.5 Update `SyncRecordMemento` and the record's JDBC adapter to load the schedule with the record, never lazily.
- [x] 3.6 Confirm `SyncStateResponseConverter` still reports the same `nextAttemptDueAt` value to the API.

## 4a. Route effects through the schedule repository

Split from the original section 4 so the change lands in two independently testable
halves. This half makes `sync_schedule` the authority for *writes* while `sync_record`'s
old columns still back every *read*, so the system stays consistent at the end of it.

- [x] 4.1 In `SynchronizationService`, capture the `ScheduleEffect` returned by each domain call and apply it through `SyncScheduleRepository`. **Every one of these call sites currently discards the returned value** — harmless while `applyToSchedule` still mutates the aggregate in memory, but the moment 4.3 moves the write off the aggregate, any site left discarding it loses its scheduling write silently, with no compile error and no failing test. Work through the list rather than trusting a green suite: `recordConflict` (×2, in `resolveConflict` and `runPass`), `resolveWithDirection` (×2, INWARD and OUTWARD), `acceptDivergence`, `reset`, `recordConverged`, `recordOutage`, `recordTerminalFailure` (×2), `recordRetryableFailure`, `recordSuccess` (×2, in `writeInward` and `writeOutward`), `recordOutwardWriteWithSkippedAdvance`, and the `applyToSchedule` call in `markDirty`. Fourteen sites in total; re-derive the list from the code, since line numbers shift as you edit.
- [x] 4.2 Apply the schedule write **inside the same transaction** as the record and attempt row (D15). A schedule written outside it can survive a rolled-back outcome.
- [x] 4.3 Rewrite `SynchronizationService.markDirty` to write only through the schedule repository — no aggregate load, no aggregate save.
- [x] 4.4 Verify the version-token short-circuit at `SynchronizationService.java:338` still reads a populated `dirtySince`. This is the change's quietest failure mode: a null schedule makes the condition true when it should be false, the short-circuit stops firing, and the engine silently does a full read on every pass with no test failing.
- [x] 4.7 Call `SyncScheduleRepository.createFor` from `SynchronizationService.enroll`, in the same transaction as the record save, so task 2.6's "every record has a schedule row" invariant actually holds. The method exists but has no caller yet.
- [x] 4.8 *(moved to 4b.5 — done there)* — `applyToSchedule` must stay public through this half, because 4.10's double-write depends on it.
- [x] 4.9 Make the dropped-effect trap impossible to reintroduce rather than relying on 4.1's checklist: once `applyToSchedule` no longer mutates the aggregate, a discarded `ScheduleEffect` is always a bug. Prefer a mechanism that fails loudly — annotate the domain methods so an ignored result is a compile-time warning/error, or have the effect be applied only by a helper the call site must pass it to. If no such mechanism fits the codebase, say so and instead add a test that a pass which schedules a retry actually leaves a persisted `next_attempt_due_at`, and verify it fails when one call site is left discarding.
- [x] 4.10 **The bridge that makes this half independently shippable:** every effect applied through `SyncScheduleRepository` must ALSO keep `sync_record`'s own `dirty_since`/`next_attempt_due_at` columns up to date, because until 4b lands those columns still back `findDueForScan`, `findAllActive` and `SyncRecordMemento`'s read path. Keeping `applyToSchedule`'s in-memory mutation (and therefore what `SyncRecordMemento.from` persists) achieves this for free — so do NOT remove it in this half; 4.8 moves to 4b. State explicitly in your report that both stores are written and that the two cannot disagree.
- [x] 4.11 Verify the double-write directly: after a pass that schedules a retry, assert `sync_schedule` and `sync_record` hold the same `next_attempt_due_at`. This test is what proves 4a is safe to ship on its own, and it is deleted in 4b once `sync_record`'s columns are gone.

## 4b. Switch reads onto the schedule table and drop the old columns

Second half. Only start once 4a is committed with a green suite. At the end of this half
`sync_record` no longer carries scheduling at all, so the double-write from 4.10 stops
being needed and the transitional hatch closes.

- [x] 4b.1 Update `findDueForScan` and `findAllActive` to join `sync_schedule` instead of filtering columns on `sync_record`, preserving the exact predicates (including the `status NOT IN ('FAILED', 'CONFLICT')` and claim-staleness clauses). A record with no schedule row must still behave exactly as one with an empty schedule does today — check what the join does to such a record, since an INNER JOIN would silently drop it from the scan.
- [x] 4b.2 Move `SyncRecordMemento`'s read path off `sync_record`'s scheduling columns and onto the schedule loaded via `SyncScheduleRepository`, keeping task 3.5's "never lazily" property.
- [x] 4b.3 Drop `idx_sync_record_due_scan` and the `dirty_since` / `next_attempt_due_at` columns from `sync_record` in `V001`. Deferred from task 2.4 so the live query was never left without its index.
- [x] 4b.4 Remove the 4.10 double-write and the 4.11 test that guarded it — both exist only to make 4a shippable on its own.
- [x] 4b.5 Close the iteration-3 transitional hatch: reduce `SyncRecord.applyToSchedule` to private, or drop it entirely (moved here from 4.8 — it must stay public while 4.10's double-write depends on it).
- [x] 4b.6 Re-verify the version-token short-circuit (4.4) now that `dirtySince` arrives from the new table rather than the aggregate's own columns. This is the second and last chance for the change's quietest failure mode to appear.

## 5. Remove the layers the race forced

- [x] 5.1 Remove `withOptimisticLockRetry` and the `AuditMetadata` version-stamp overwrite from `SyncOutcomeWriter`.
- [x] 5.2 Remove `Propagation.REQUIRES_NEW` from `doPersist` and `doPersistResolution`, and fold them back into `persist` / `persistResolution` as plain `@Transactional` methods.
- [x] 5.3 Remove the `@Lazy` self-proxy from the constructor.
- [x] 5.4 Remove the swallowed `OptimisticLockingFailureException` from `SynchronizationService.markDirty`.
- [x] 5.5 Keep `SyncOutcomeWriter` a separate bean — the cross-bean proxy boundary is still what makes `@Transactional` apply — and update its javadoc, which currently explains all four removed layers in detail.
- [x] 5.6 Update `SyncRecord`, `SyncRecordRepository` and `SyncRecordJdbcRepository` javadocs that describe scheduling fields as living on the record.

## 6. Verification

- [x] 6.1 The test from 1.2 must now pass with the retry gone — that is the proof the race is actually eliminated rather than merely hidden.
- [x] 6.2 Verify atomicity directly: a failure between the record save, the attempt append and the schedule write must roll back all three.
- [x] 6.3 Assert the short-circuit from 4.4 still fires — a record with a baseline and no dirty marker must skip the full read. Verify this test fails if the schedule is not loaded with the record.
- [x] 6.4 Verify a record marked dirty is still returned by the due scan, and that a record whose claim is fresh is still excluded.
- [x] 6.5 Confirm a claim no longer fails with `OptimisticLockingFailureException` because of a concurrent dirty marker.
- [x] 6.6 Strengthen `SynchronizationServiceMarkDirtyIntegrationTest` and `SynchronizationServiceFailureHandlingIntegrationTest` rather than merely keeping them passing.
- [x] 6.7 Per the project's negative-test rule, verify each new test above fails when its mechanism is removed. A test that passes for the wrong reason is worse than no test.
- [x] 6.8 Review the test code itself, not just its green result.
- [x] 6.9 Run the full backend test suite; all tests compile and pass.
- [x] 6.10 Code review, focused on transaction boundaries, the concurrent paths, and the ten transcribed `ScheduleEffect` rows.
- [x] 6.11 Commit.
