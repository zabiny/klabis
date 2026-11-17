---
name: backend-test-run-2026-11-16-fee-year-lifecycle-red-2
description: FeeYearLifecycleServiceTest run 2026-11-16 on deepen-fee-year-lifecycle — 21/47 passed, 26 failed with UnsupportedOperationException stubs, 51s
metadata:
  type: project
---

FeeYearLifecycleServiceTest (com.klabis.membershipfees.application) on branch `deepen-fee-year-lifecycle`, root `/home/davca/Documents/Devel/klabis/backend`, filter "FeeYearLifecycleServiceTest".

Result: 21/47 passed, 26 failed, wall clock 51s (build FAILED).

All 26 failures are `UnsupportedOperationException` thrown from still-stubbed `FeeYearLifecycleService` methods:
- `assignLevel` (line 97) — 11 failures
- `chooseFeeLevel` (line 103) — 5 failures
- `removeFeeChoice` (line 109) — 3 failures
- `getCurrentChoice` (line 115) — 2 failures
- `getRecommendedLevelForYear` (line 121) — 3 failures

Plus 2 sanitized to AssertionError where `catchThrowable` expected a domain exception (VotingClosedException / MembershipFeeGroupNotFoundException / FeeSelectionCampaignNotFoundException) but got UnsupportedOperationException.

**Why:** intentional RED — test class written before service implementation in the deepening refactor; class grew from 21 tests to 47.
**How to apply:** expect the same scoped run (~1 min) to go green once those five methods are implemented; see [[backend-test-run-2026-11-16-fee-year-lifecycle-green]] for the earlier 21-test green state.
