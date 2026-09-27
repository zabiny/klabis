---
name: backend_test_run_2026_09_26_reviewchanges
description: Events module test run after removing syncEventFromOris endpoint and adding sync HAL link to event list rows
metadata:
  type: project
---

## Test Execution

**Date:** 2026-09-26  
**Branch:** feature/member-synchronization-reviewchanges  
**Configuration:**
- `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`
- `--rerun-tasks` (forced re-execution)
- Backend root: `/home/davca/Documents/Devel/klabis/backend`

## Scope

Filtered test run for events module and related sync tests:
- `com.klabis.events.infrastructure.restapi.EventControllerTest`
- `com.klabis.events.infrastructure.restapi.OrisEventControllerTest`
- `com.klabis.events.application.OrisEventImportServiceTest`
- `com.klabis.sync.infrastructure.restapi.SynchronizationControllerTest`

## Results

**GRAND TOTAL: 202 executed, 202 passed, 0 failed, 0 skipped** ✓

### Per-Class Breakdown

#### OrisEventImportServiceTest
- **Total:** 4/4 passed
- ImportEventFromOrisMethod: 4/4 ✓

#### EventControllerTest
- **Total:** 142/142 passed
- 17 nested test classes, all passing
- **NEW TESTS PASSING:**
  - ListRowSyncLinkTests: 2/2 ✓
    - "ORIS-enrolled DRAFT/ACTIVE event row carries a sync link" (0.03s)
    - "non-ORIS event row does NOT carry a sync link" (0.028s)

#### OrisEventControllerTest
- **Total:** 22/22 passed
- 5 nested test classes, all passing

#### SynchronizationControllerTest
- **Total:** 34/34 passed
- 7 nested test classes, all passing

## Status

✓ All tests passing. New tests for sync HAL link on event list rows are working correctly. No regressions detected in events module or related sync tests.
