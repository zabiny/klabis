---
name: backend-test-run-2026-11-16-full-after-review-fixes
description: Second full backend suite on deepen-fee-year-lifecycle after review fixes (4895 tests, same MdcFilter failure, 5m40s)
metadata:
  type: project
---

Full-suite verification for `deepen-fee-year-lifecycle` after applying code-review fixes.

**Command:** `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend --rerun-tasks`
**Duration:** 5m 40s Gradle wall clock (script auto-promoted to background at 180s).
**Result:** 4895 tests completed, 1 failed, 14 skipped (script reports 4894/4895 passed).
Test count grew by 2 vs the earlier full run (4893) — review-fix tests added.

**Only failure:** `com.klabis.common.logging.MdcFilterIntegrationTest.shouldCleanUpMDCAfterRequest`
(first cause: `org.opentest4j.AssertionFailedError: expected: null but was: {"userId"="admin"}`).
Identical to the failure recorded in [[backend-test-run-2026-11-16-full-final-modulith-skip]]; it
passes in isolation and the branch diff does not touch `common.logging`.

**How to apply:** treat this as the known MDC thread-local test-isolation leak. Suite is otherwise
green. Full backend suite with `--rerun-tasks` takes ~6 minutes — 30 min timeout is ample.
