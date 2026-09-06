## 1. Reproduce the defect first

- [x] 1.1 Add a `SyncRecordJdbcRepositoryTest` case: enrol a record, drive it to `FAILED`, set a non-null `dirty_since`, then assert it is **absent** from `findDueForScan`. Confirm this test **fails** against the current query — if it passes before the fix, the fixture does not actually reproduce the condition and the test is worthless (see the project's negative-test rule).
- [x] 1.2 Reach `FAILED` the way production does (`recordTerminalFailure` via the domain), not by writing the status column directly — the point is that the real transition leaves `dirty_since` set.

## 2. Fix the query

- [x] 2.1 Add `AND status <> 'FAILED'` to `findDueForScan` in `SyncRecordJdbcRepository`.
- [x] 2.2 Confirm the test from 1.1 now passes, and that removing the new predicate makes it fail again.
- [x] 2.3 Rewrite the `findDueForScan` javadoc: `CONFLICT` self-excludes because `recordConflict` clears both scheduling fields; `FAILED` does **not** self-exclude (`recordTerminalFailure` clears only `nextAttemptDueAt`) and is excluded by the predicate. Do not repeat the current claim that both clear both fields — that is what made the gap invisible.
- [x] 2.4 Leave `markDirty` and `SyncRecord#markDirty` unchanged (proposal, "What Changes") — the flag is legitimate on a failed record.

## 3. Verification

- [x] 3.1 Check whether any existing test asserted the old behaviour — a scheduler test expecting an exception or an ERROR log for a failed record would now be asserting something that cannot happen. Update or remove it deliberately, do not let it silently keep passing for a new reason.
- [x] 3.2 Run the full backend test suite; all tests compile and pass.
- [x] 3.3 Code review, focused on task 1.1: the new test must fail without the predicate.
- [x] 3.4 Commit.
