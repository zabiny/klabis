## 1. Reproduce the defect first

- [x] 1.1 Add a `SyncRecordJdbcRepositoryTest` case: enrol a record, drive it to `CONFLICT` via the domain (`recordConflict`), then `markDirty()`, persist, assert the row has `status = 'CONFLICT'` AND `dirty_since IS NOT NULL`, then assert it is **absent** from `findDueForScan`. Confirm this test **fails** against the post-`sync-followup-due-scan-status-filter` query (`status <> 'FAILED'` only).
- [x] 1.2 Reach `CONFLICT` the way production does (`recordConflict` via the domain), not by writing the status column.

## 2. Fix the query

- [x] 2.1 Change `findDueForScan` in `SyncRecordJdbcRepository` from `AND status <> 'FAILED'` to `AND status NOT IN ('FAILED', 'CONFLICT')`.
- [x] 2.2 Confirm the test from 1.1 now passes, and that dropping `'CONFLICT'` from the `IN` list makes it fail again.
- [x] 2.3 Rewrite the `findDueForScan` javadoc: `CONFLICT` is excluded by the predicate, not by construction — `markDirty` has no status guard and re-sets `dirty_since` on a standing conflict when the underlying entity is edited. Do not repeat the "`CONFLICT` self-excludes" claim.
- [x] 2.4 Leave `markDirty` and `SyncRecord#markDirty` unchanged.

## 3. Verification

- [x] 3.1 Check whether any existing test asserted a `CONFLICT` record being handed to a pass (scheduler test expecting an exception / ERROR log). Update or remove deliberately.
- [x] 3.2 Run the full backend test suite; all tests compile and pass.
- [x] 3.3 Code review, focused on task 1.1: the new test must fail without the `'CONFLICT'` predicate.
- [x] 3.4 Commit.
