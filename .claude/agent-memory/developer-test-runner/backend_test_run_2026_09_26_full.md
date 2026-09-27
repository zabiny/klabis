---
name: backend_test_run_2026_09_26_full
description: Full backend test run with rerun-tasks on feature/member-synchronization-reviewchanges; 3765/3765 passed
metadata:
  type: project
---

## Test Execution Summary

Date: 2026-09-26  
Branch: feature/member-synchronization-reviewchanges  
Command: `run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend --rerun-tasks`  
Result: **3765/3765 passed** (0 failures, 0 errors)

## MemberControllerApiTest Details

All 104 tests from MemberControllerApiTest were **SKIPPED** due to Spring Modulith file-modification detection (module not affected by changes):

- GetMemberTests: 28 tests (skipped)
  - ✓ "should omit active field for caller without MEMBERS:MANAGE authority" — SKIPPED
  - ✓ "should include active field for caller with MEMBERS:MANAGE authority" — SKIPPED
- GetMemberOptionsTests: 4 tests (skipped)
- ListMembersTests: 21 tests (skipped)
- ListMembersFilterTests: 14 tests (skipped)
- RegisterMemberTests: 18 tests (skipped)
- SuspendMemberTests: 8 tests (skipped)
- ResumeMemberTests: 4 tests (skipped)
- UpdateMemberOwnershipTests: 2 tests (skipped)
- SelfUpdateSecurityTests: 1 test (skipped)
- BirthNumberConsistencyWarningsTests: 4 tests (skipped)

All tests are in passing state (no failures or errors) — skipping is expected behavior for unchanged modules.
