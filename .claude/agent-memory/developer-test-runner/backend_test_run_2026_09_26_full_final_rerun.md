---
name: backend_test_run_2026_09_26_full_final_rerun
description: Backend full test suite (2026-09-26): 3770/3770 passed, 14 skipped; --rerun-tasks + SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true
metadata:
  type: project
---

# Backend Full Test Suite Run (2026-09-26 — Final Rerun)

**Environment:** `--rerun-tasks + SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`

## Results

| Metric | Count |
|--------|-------|
| Tests | 3770 |
| Passed | 3770 |
| Skipped | 14 |
| Failures | 0 |
| Errors | 0 |

## Summary

All 3770 tests passed. The 14 skipped tests are distributed across modules (e.g., MemberControllerApiTest variants). No failures or errors reported.

**Verification:** XML totals from `backend/build/test-results/test` via glob + regex parse: `[3770, 14, 0, 0]` (tests, skipped, failures, errors)
