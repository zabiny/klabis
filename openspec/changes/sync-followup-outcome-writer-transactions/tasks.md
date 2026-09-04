## 1. Confirm the premise before changing anything

- [ ] 1.1 Re-read `design.md` D9 and D15 in `openspec/changes/archive/2026-09-04-add-bidirectional-sync-engine/` and confirm the dirty marker is genuinely a scheduling hint, not a domain change.
- [ ] 1.2 Establish exactly which writes contend today: reproduce the `markDirty` race in a test that fails without the retry, so there is proof the retry is load-bearing before it is removed.
- [ ] 1.3 If the premise does not hold, stop and report rather than proceeding.

## 2. Make the dirty marker stop taking the version

- [ ] 2.1 Persist `dirtySince` through a targeted update that leaves the aggregate version untouched.
- [ ] 2.2 Confirm a record marked dirty is still selected by the next scan — the scheduling behaviour must not regress.
- [ ] 2.3 Confirm the marker survives concurrently with an outcome write, in both orders.

## 3. Remove the layers the race forced

- [ ] 3.1 Remove the manual optimistic-lock retry and the version-stamp overwrite from `SyncOutcomeWriter`.
- [ ] 3.2 Remove `Propagation.REQUIRES_NEW` once the retry no longer needs a fresh transaction.
- [ ] 3.3 Remove the `@Lazy` self-proxy and call the persist method directly.
- [ ] 3.4 Remove the swallowed `OptimisticLockingFailureException` from `SynchronizationService.markDirty`.
- [ ] 3.5 Keep `SyncOutcomeWriter` a separate bean and verify the record plus its attempt row are still written in one transaction.

## 4. Verification

- [ ] 4.1 Verify atomicity directly: a failure between the record save and the attempt append must roll back both.
- [ ] 4.2 Strengthen `SynchronizationServiceMarkDirtyIntegrationTest` and `SynchronizationServiceFailureHandlingIntegrationTest` — the test from 1.2 must still pass without the retry.
- [ ] 4.3 Run the full backend test suite; all tests compile and pass.
- [ ] 4.4 Code review, focused on transaction boundaries and the concurrent paths.
- [ ] 4.5 Commit.
