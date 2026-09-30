---
name: frontend_test_hang_2026_09_30
description: MemberRegistration.integration.test.tsx hangs (infinite loop or await)
metadata:
  type: project
---

## Hanging Test File

**File:** `src/pages/members/MemberRegistration.integration.test.tsx`

**Symptom:** Test run hangs immediately when vitest attempts to execute this file. Timeout occurs at 60 seconds with no test names printed to console before the hang.

**Discovered:** 2026-09-30 23:01 UTC

All other test files in the frontend run successfully within 1-6 seconds per file.

## Test Files Confirmed Passing
- src/api/authorizedFetch.test.ts — 7 tests in 1.74s
- src/api/hateoas.test.ts — 19 tests in 1.97s
- src/api/klabisUserManager.test.ts — 6 tests in 1.88s
- src/pages/members/isMinorOnDate.test.ts — 3 tests in 1.70s
- src/pages/members/MemberDetailPage.groups.test.tsx — 6 tests in 3.83s
- src/pages/members/MemberDetailPage.test.tsx — 62 tests in 5.92s
- src/pages/members/MemberFeeSection.test.tsx — 15 tests in 2.36s
- src/pages/members/MemberRegistration.integration.test.tsx — **HANGS** (timeout at 60s, no tests printed)
