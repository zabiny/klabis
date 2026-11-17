---
name: backend-test-run-2026-11-16-member-fee-placement-red
description: MemberFeePlacementTest single-class run — 0/9 passed, all UnsupportedOperationException from unimplemented MemberFeePlacement, ~50s
metadata:
  type: project
---

Branch `deepen-fee-year-lifecycle`, single class `com.klabis.membershipfees.application.MemberFeePlacementTest`: 0/9 passed.

All 9 tests fail with `java.lang.UnsupportedOperationException` thrown from `MemberFeePlacement.placeByMemberChoice` (line 32) / `placeByAdminAssignment` (line 36) — the class is a stub. One test (`PlaceByMemberChoice.shouldRejectRemovalWhenVotingIsClosed`) fails on an assertion expecting `VotingClosedException` but getting `UnsupportedOperationException`.

**Why:** TDD red phase for the placement refactor — the test was written before the implementation.

**How to apply:** Any future run of this class before the implementation lands will show the same signal; do not treat it as an environment problem. Wall clock ~50s including compilation.

Related: [[backend-test-run-2026-11-16-fee-year-lifecycle-red]]
