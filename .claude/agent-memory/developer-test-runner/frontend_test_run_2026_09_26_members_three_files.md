---
name: frontend_test_run_2026_09_26_members_three_files
description: Frontend test run on 2026-09-26 for three members page test files — 96/97 passed, one failure in MemberDetailPage.groups.test.tsx
metadata:
  type: project
---

# Frontend Test Run (2026-09-26) — Members Page Test Files

**Scope:** Three specific test files:
- src/pages/members/MembersPage.test.tsx
- src/pages/members/MemberDetailPage.test.tsx
- src/pages/members/MemberDetailPage.groups.test.tsx

**Results:** 96/97 tests passed (1 failed)

## Failed Test

**Test Name:** MemberDetailPage sync status indicator (4.4) renders SyncStatusIndicator when member._links.sync is present

**File:** src/pages/members/MemberDetailPage.groups.test.tsx

**Error:** 
```
No "toHref" export is defined on the "../../api/hateoas" mock. 
Did you forget to return it from "vi.mock"?
```

The test mock for the hateoas module is missing the `toHref` export. The HalRouteContext (at src/contexts/HalRouteContext.tsx:18) is trying to use `toHref` from the mocked hateoas module but it's not defined in the mock setup.

## Test Count Breakdown

- MembersPage.test.tsx: 36 tests, all passed
- MemberDetailPage.test.tsx: 61 tests (need to verify exact split)
- MemberDetailPage.groups.test.tsx: 1 test, failed

**Duration:** Roughly 10 seconds for three files (much faster than full suite)
