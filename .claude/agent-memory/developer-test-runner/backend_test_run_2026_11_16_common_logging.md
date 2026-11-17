---
name: backend-test-run-2026-11-16-common-logging
description: common.logging package run 2026-11-16 — 5/5 passed (MdcFilterIntegrationTest 1, RequestLoggingFilterTest 4), fast (~1 min)
metadata:
  type: project
---

`--tests "com.klabis.common.logging.*"` on branch `deepen-fee-year-lifecycle` (2026-11-16): 5/5 passed.
Only two classes exist in that package: `MdcFilterIntegrationTest` (1 test) and `RequestLoggingFilterTest` (4 tests).
`MdcFilterIntegrationTest` passes when run in isolation, confirming the known full-suite flake (see [[backend-test-run-2026-11-16-full-final-modulith-skip]]).

**Why:** used as a fast verification slice after editing `RequestLoggingFilter`.
**How to apply:** expect ~1 min wall clock; 1800000 ms timeout is more than sufficient.
