## 1. Confirm the premise before changing anything

- [x] 1.1 Re-read `design.md` D9, D12 and D15 in `openspec/changes/archive/2026-09-04-add-bidirectional-sync-engine/` and confirm the dirty marker is genuinely a scheduling hint, not a domain change, and that D12's claim guarantee rests on `claimed_at` rather than on any scheduling column.
- [x] 1.2 Establish that the race is real and the retry is load-bearing: write a test that reproduces the `markDirty` / `SyncOutcomeWriter#persist` collision and fails when the retry in `withOptimisticLockRetry` is removed. Without this the later removal proves nothing.
- [x] 1.3 Transcribe the scheduling behaviour of all ten methods listed in the proposal's table directly from their current bodies in `SyncRecord`, and record any row where the code disagrees with the table. The table is a starting point, not the source of truth.
- [x] 1.4 If the premise does not hold — the marker turns out to carry correctness, or the claim depends on a scheduling column — stop and report rather than proceeding.

## 2. Introduce the schedule as its own concept

- [ ] 2.1 Add `SyncSchedule` (value object: `dirtySince`, `nextAttemptDueAt`) and `ScheduleEffect` (`clear()`, `clearDueAt()`, `dueAt(Instant)`, `dirtySince(Instant)`, and a no-change variant) to `com.klabis.sync.domain`.
- [ ] 2.2 Add the `SyncScheduleRepository` secondary port: load a schedule for a record, and apply a `ScheduleEffect` to it.
- [ ] 2.3 Add the `sync.sync_schedule` table to `V001__initial_schema.sql` (`sync_record_id` PK/FK, `dirty_since`, `next_attempt_due_at`). Deliberately **no** version column — a scheduling write must never contend for one. Per the project rule, update `V001` rather than adding a new migration.
- [ ] 2.4 Move `idx_sync_record_due_scan` onto the new table, indexing `(dirty_since, next_attempt_due_at)`.
- [ ] 2.5 Add the JDBC memento and adapter for the schedule, following the existing `SyncRecordMemento` / `SyncRecordRepositoryAdapter` shape.
- [ ] 2.6 Ensure a record always has a schedule row — created at enrolment — so no read path has to handle a missing one. Decide and document what happens for a record enrolled before this change (no production data exists, so a coded default is acceptable; state it).

## 3. Move scheduling out of the aggregate

- [ ] 3.1 Remove the `dirtySince` and `nextAttemptDueAt` fields from `SyncRecord`, along with `markDirty`.
- [ ] 3.2 Change the return type of the ten methods identified in 1.3 to return a `ScheduleEffect`, preserving each one's current behaviour exactly as transcribed — not as summarised in the proposal.
- [ ] 3.2a `resolveWithDirection` is an eleventh call site found during 1.3: it delegates to `recordSuccess` and so inherits `clear()`. It must return that effect onward, and `SynchronizationService.resolveConflict` must apply it — easy to miss because the proposal's table does not list it separately.
- [ ] 3.3 Keep `SyncRecord.getDirtySince()` / `getNextAttemptDueAt()` reading from a schedule loaded alongside the record, so existing readers keep working. The aggregate reads the schedule; it no longer owns its mutation.
- [ ] 3.4 Verify `recordTerminalFailure` and `reset` still leave `dirtySince` standing (they clear only the due-at today) — a record that was dirty when it failed must still be dirty after a reset.
- [ ] 3.5 Update `SyncRecordMemento` and the record's JDBC adapter to load the schedule with the record, never lazily.
- [ ] 3.6 Confirm `SyncStateResponseConverter` still reports the same `nextAttemptDueAt` value to the API.

## 4. Apply effects from the application layer

- [ ] 4.1 In `SynchronizationService`, capture the `ScheduleEffect` returned by each domain call and apply it through `SyncScheduleRepository`.
- [ ] 4.2 Apply the schedule write **inside the same transaction** as the record and attempt row (D15). A schedule written outside it can survive a rolled-back outcome.
- [ ] 4.3 Rewrite `SynchronizationService.markDirty` to write only through the schedule repository — no aggregate load, no aggregate save.
- [ ] 4.4 Verify the version-token short-circuit at `SynchronizationService.java:338` still reads a populated `dirtySince`. This is the change's quietest failure mode: a null schedule makes the condition true when it should be false, the short-circuit stops firing, and the engine silently does a full read on every pass with no test failing.
- [ ] 4.5 Update `findDueForScan` and `findAllActive` to join `sync_schedule` instead of filtering columns on `sync_record`, preserving the exact predicates (including the `status NOT IN ('FAILED', 'CONFLICT')` and claim-staleness clauses).

## 5. Remove the layers the race forced

- [ ] 5.1 Remove `withOptimisticLockRetry` and the `AuditMetadata` version-stamp overwrite from `SyncOutcomeWriter`.
- [ ] 5.2 Remove `Propagation.REQUIRES_NEW` from `doPersist` and `doPersistResolution`, and fold them back into `persist` / `persistResolution` as plain `@Transactional` methods.
- [ ] 5.3 Remove the `@Lazy` self-proxy from the constructor.
- [ ] 5.4 Remove the swallowed `OptimisticLockingFailureException` from `SynchronizationService.markDirty`.
- [ ] 5.5 Keep `SyncOutcomeWriter` a separate bean — the cross-bean proxy boundary is still what makes `@Transactional` apply — and update its javadoc, which currently explains all four removed layers in detail.
- [ ] 5.6 Update `SyncRecord`, `SyncRecordRepository` and `SyncRecordJdbcRepository` javadocs that describe scheduling fields as living on the record.

## 6. Verification

- [ ] 6.1 The test from 1.2 must now pass with the retry gone — that is the proof the race is actually eliminated rather than merely hidden.
- [ ] 6.2 Verify atomicity directly: a failure between the record save, the attempt append and the schedule write must roll back all three.
- [ ] 6.3 Assert the short-circuit from 4.4 still fires — a record with a baseline and no dirty marker must skip the full read. Verify this test fails if the schedule is not loaded with the record.
- [ ] 6.4 Verify a record marked dirty is still returned by the due scan, and that a record whose claim is fresh is still excluded.
- [ ] 6.5 Confirm a claim no longer fails with `OptimisticLockingFailureException` because of a concurrent dirty marker.
- [ ] 6.6 Strengthen `SynchronizationServiceMarkDirtyIntegrationTest` and `SynchronizationServiceFailureHandlingIntegrationTest` rather than merely keeping them passing.
- [ ] 6.7 Per the project's negative-test rule, verify each new test above fails when its mechanism is removed. A test that passes for the wrong reason is worse than no test.
- [ ] 6.8 Review the test code itself, not just its green result.
- [ ] 6.9 Run the full backend test suite; all tests compile and pass.
- [ ] 6.10 Code review, focused on transaction boundaries, the concurrent paths, and the ten transcribed `ScheduleEffect` rows.
- [ ] 6.11 Commit.
