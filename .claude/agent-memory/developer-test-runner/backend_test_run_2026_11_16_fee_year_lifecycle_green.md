---
name: backend-test-run-2026-11-16-fee-year-lifecycle-green
description: GREEN run (2026-11-16) of FeeYearLifecycleServiceTest — 21/21 passed after implementing the service; wall clock ~35s
metadata:
  type: project
---

Backend single-class run on branch `deepen-fee-year-lifecycle`:
`run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend "com.klabis.membershipfees.application.FeeYearLifecycleServiceTest"`.

- GREEN run (2026-11-16): 21/21 passed, ~35s (compile dominated).
- Previous RED runs: see [[backend-test-run-2026-11-16-fee-year-lifecycle-red]] (0/21, stubs throwing UnsupportedOperationException).

**Why:** `FeeYearLifecycleService` / `FeeYearLifecyclePort` were implemented after the test-first commits.

**How to apply:** single backend test class still costs only tens of seconds; 30-min ceiling is never needed unless the whole suite runs. The three AssertionError-type tests from the RED phase now genuinely assert the intended exception types.
