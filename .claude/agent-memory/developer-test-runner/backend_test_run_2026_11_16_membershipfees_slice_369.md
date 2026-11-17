---
name: backend-test-run-2026-11-16-membershipfees-slice-369
description: membershipfees slice green 369/369 on branch deepen-fee-year-lifecycle after feasibility check changes; single filtered Gradle invocation
metadata:
  type: project
---

Backend `com.klabis.membershipfees.*` slice on branch `deepen-fee-year-lifecycle`: 369/369 passed, 0 failures.

**Why:** Verification run requested after modifying `FeeYearLifecycleService`, `MemberFeePlacement`, `YearlyFeeChargeMarkerRepository(+Adapter)`, `FeeSelectionDeadlineScheduler` and three test classes. Test count grew from 367 (earlier runs seen in memory) to 369 — new tests added since [[backend-test-run-2026-11-16-membershipfees-verification]].

**How to apply:** Command used (one invocation, foreground, no `--rerun-tasks`, no `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS`):

```
run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend "com.klabis.membershipfees.*"
```

Wall clock roughly 2 minutes; a 1800000 ms timeout is comfortable. Expect ~369 tests for this slice going forward.
