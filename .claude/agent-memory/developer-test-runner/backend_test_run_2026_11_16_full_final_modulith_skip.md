---
name: backend-test-run-2026-11-16-full-final-modulith-skip
description: Final full backend suite on deepen-fee-year-lifecycle (4893 tests, 1 unrelated MDC test-isolation failure, 6m09s)
metadata:
  type: project
---

Final full-suite verification for the `deepen-fee-year-lifecycle` branch (all 3 iterations committed).

**Command:** `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend --rerun-tasks`
**Duration:** 6m 09s wall clock.
**Result:** 4893 tests completed, 1 failed, 14 skipped (script reports 4892/4893 passed).

**Only failure:** `com.klabis.common.logging.MdcFilterIntegrationTest.shouldCleanUpMDCAfterRequest`
`AssertionFailedError: expected: null but was: {"userId"="admin"}` — the test asserts
`MDC.getCopyOfContextMap()` is null before/after a MockMvc request. Passes 1/1 in isolation
(`run-backend-tests.sh ... "MdcFilterIntegrationTest"`). Branch diff touches only the
membershipfees package plus openspec/GLOSSARY, so this looks like a pre-existing
thread-local/MDC test-isolation leak that surfaces depending on which test classes share a
JVM fork (class distribution changed because this branch deleted 3 test classes and added
FeeYearLifecycleServiceTest + MemberFeePlacementTest).

**How to apply:** when this test fails in a full run but passes alone, treat it as
order-dependent test isolation, not a regression in the change under test. Confirm by rerunning
the single class before blaming the branch. Related: [[project-preexisting-failing-tests]].
