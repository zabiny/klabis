---
name: backend-test-run-2026-11-16-fee-year-lifecycle-red
description: Intentional RED runs (2026-11-16) of FeeYearLifecycleServiceTest — now 0/21 passed (18 tests earlier), wall clock ~33s (incl. compile)
metadata:
  type: project
---

Backend single-class run on branch `deepen-fee-year-lifecycle`:
`run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend "FeeYearLifecycleServiceTest"`.

- Earlier run (2026-11-16): 0/18 passed, ~56s.
- Latest run (2026-11-16): 0/21 passed, ~33s (3 newly added tests).

All fail with `UnsupportedOperationException: closeCampaign is not implemented yet`
(`FeeYearLifecycleService.java:29`) or `processCampaignEnd is not implemented yet`
(`FeeYearLifecycleService.java:35`). Three tests wrap it in an AssertJ `AssertionError`
expecting a domain exception: `FeeSelectionCampaignNotFoundException`,
`CampaignAlreadyProcessedException`, and `IllegalStateException` (BatchFailure).

**Why:** the test class was committed first; `FeeYearLifecycleService` / `FeeYearLifecyclePort`
still hold stubs.

**How to apply:** for a single backend test class, budget is dominated by compile; observed
wall clock 33–56s. A single-class run needs nowhere near the 30-min ceiling. In the GREEN phase,
watch the three AssertionError-style failures: the UOE masks a type mismatch, so they will only
start proving the intended exception type once the stubs are implemented. Related:
[[feedback-modulith-test-skip]].
