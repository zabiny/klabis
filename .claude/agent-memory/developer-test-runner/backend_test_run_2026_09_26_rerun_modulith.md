---
name: backend_test_run_2026_09_26_rerun_modulith
description: Full backend test run with --rerun-tasks and SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true
metadata:
  type: reference
---

**Date:** 2026-09-26  
**Branch:** feature/member-synchronization-reviewchanges  
**Command:** `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend --rerun-tasks`

## Results

- **Tests:** 3778
- **Skipped:** 14 (expected test-class level skips, not cache-related)
- **Failures:** 0
- **Errors:** 0
- **Status:** ✓ All tests passed

This run forced fresh execution with `--rerun-tasks` and disabled Spring Modulith test optimizations via environment variable. No cache-related UP-TO-DATE skips occurred. All 3778 tests executed fresh with zero failures.
