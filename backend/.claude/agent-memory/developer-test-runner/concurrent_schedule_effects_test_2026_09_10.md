---
name: concurrent_schedule_effects_test_2026_09_10
description: Test result for concurrentEffectsOnDifferentColumnsBothSurvive against current HEAD
metadata:
  type: reference
---

## Test Execution Results

**Test:** `com.klabis.sync.infrastructure.jdbc.SyncScheduleRepositoryAdapterTest$Apply#concurrentEffectsOnDifferentColumnsBothSurvive`

**Display name:** "a DIRTY_SINCE effect and a concurrent DUE_AT effect on different columns both survive"

**Current HEAD:** 8b5466df (refactor(sync): remove the defences the scheduling race forced)

**Test Result:** **PASSED** (7/7 tests in nested class Apply)

### Execution Details

- Ran against current production code (commit 8b5466df with defences removed)
- Used test-runner-skill with filter: `SyncScheduleRepositoryAdapterTest`
- Full output enabled via `--full` flag
- Test class executed successfully with no failures

### Test Purpose

The test verifies that when two concurrent writers apply effects to different columns of the same schedule row:
- Thread A applies a DIRTY_SINCE effect
- Thread B applies a DUE_AT effect
- Both effects must survive the concurrent execution

The test uses explicit latching to force the interleaving:
1. Thread A reads and signals it has started
2. Thread B reads and writes while A waits at latch
3. Thread A is released and performs its write after B

### Test Code Path

File: `/home/davca/Documents/Devel/klabis/backend/src/test/java/com/klabis/sync/infrastructure/jdbc/SyncScheduleRepositoryAdapterTest.java`

Lines 187-232 contain the test implementation. It uses:
- CountDownLatch for synchronization
- ExecutorService with 2 threads
- Transaction boundary commits/starts to ensure cross-connection visibility
- Assertions verifying both dirtySince and nextAttemptDueAt survive

### Important Note

The test **passed** against code with read-modify-write implementation. This suggests either:
1. H2's transaction isolation level masks the race condition
2. The test execution timing doesn't create the exact interleaving that would expose the race
3. Spring Data JDBC's persistence mechanism handles concurrent updates in a way that prevents the specific loss pattern

The test comment (lines 171-185) documents that this test was run against pre-fix code and failed with `dirtySince` null, but in current execution it passes in all observed runs.
