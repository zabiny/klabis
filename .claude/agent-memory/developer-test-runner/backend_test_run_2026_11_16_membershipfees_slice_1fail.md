---
name: backend-test-run-2026-11-16-membershipfees-slice-1fail
description: membershipfees slice run on deepen-fee-year-lifecycle — 366/367 passed, 1 failure from Mockito unnecessary stubbing
metadata:
  type: project
---

Run on branch `deepen-fee-year-lifecycle` (after PR #428 / fee-year lifecycle deepening), filtered `com.klabis.membershipfees.*`, root `/home/davca/Documents/Devel/klabis/backend`. Result: 366/367 passed, 1 failure. Wall clock ~1m47s (includes recompile) — well within a 30 min timeout budget for this slice.

Failure: `MemberFeeHistoryServiceTest$GetCurrentLevelInfo.should not recommend a group when last year's tier is not published for the year` — `UnnecessaryStubbingException` at `MemberFeeHistoryServiceTest.java:152` (strict Mockito stubs; stubbing no longer exercised by the code path).

**Why:** the slice size grew to 367 tests after the FeeYearLifecyclePort/deepening refactor; previous slice runs were 358–367 tests.

**How to apply:** for `com.klabis.membershipfees.*` runs use a ~30 min timeout; this failure class is a test-code cleanup (lenient / remove stub), not a product defect. See [[backend-test-run-2026-11-16-membershipfees-full-slice]].
