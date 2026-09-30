---
name: test_run_2026_09_30_legal_guardians
description: Full test suite run on legal-guardians-refactor branch with SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true
metadata:
  type: project
---

# Test Run (2026-09-30) — Legal Guardians Refactor

**Date:** 2026-09-30  
**Branch:** legal-guardians-refactor  
**Flags:** `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`

## Filtered Test Run (2026-09-30)

### Backend Tests
```
BACKEND TEST RESULTS: 1337/1337 passed
```

- **Total:** 1337
- **Passed:** 1337
- **Failed:** 0
- **Status:** ✓ All tests passed

### Test Filters Applied
- `com.klabis.members.*`
- `com.klabis.common.users.*`
- `com.klabis.ModuleStructureVerificationTest`

## Full Test Run (2026-09-30, --rerun-tasks)

### Backend Tests
```
BACKEND TEST RESULTS: 3914/3914 passed
```

- **Total:** 3914
- **Passed:** 3914
- **Failed:** 0
- **Skipped:** 0 (reported; 14 typically masked by modulith skip flag)
- **Status:** ✓ All tests passed
- **Command:** `run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend --rerun-tasks`
- **Environment:** `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`

### Frontend Test Results
```
FRONTEND TEST RESULTS: 2113/2113 passed
```

- **Total:** 2113
- **Passed:** 2113
- **Failed:** 0
- **Skipped:** 0
- **Status:** ✓ All tests passed
- **Command:** `run-frontend-tests.sh --root /home/davca/Documents/Devel/klabis/frontend`

## Build Results
- **TypeScript compilation:** Clean (tsc -b)
- **Vite build:** Succeeded in 8.68s
- **Modules transformed:** 2494
- **Bundle size:** index-B-4yOFe3.js 763.66 kB (gzip: 219.70 kB)
- **PWA entries:** 19

## Summary
All tests passing on feature branch with --rerun-tasks. No failures or errors detected. 40 additional tests since last filtered run.
