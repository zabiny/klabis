# Team Coordination File: fix-user-groups-access

**Date:** 2026-04-13  
**Proposal:** fix-user-groups-access  
**Summary:** Fix 403 errors for group members viewing group detail, hide Family Groups nav for non-MEMBERS:MANAGE users

## Proposal Overview

Two bug fixes:
1. Members of any group type (training, family, free) currently get HTTP 403 when trying to view the group detail page they are members of. Backend authorization must be extended to allow group members to read their group detail.
2. The "Rodinné skupiny" navigation item in Administration section is shown to all users but should only appear for users with MEMBERS:MANAGE permission. Fix: condition the HAL link in RootController on MEMBERS:MANAGE role.

## Tasks

From tasks.md:
- Task 1: Backend - Training Group Member Access (1.1-1.4)
- Task 2: Backend - Family Group Member Access (2.1-2.4)
- Task 3: Backend - Free Group Member Access (3.1-3.3)
- Task 4: Backend - Family Groups Navigation HAL Link (4.1-4.3)
- Task 5: Frontend - Family Groups Navigation Visibility (5.1)

## Implementation Plan

**Iteration 1:** Backend - all three group types member access + navigation HAL link (Tasks 1-4)
- Small scope, all in backend, can be done in one iteration

**Iteration 2:** Frontend - verify navigation visibility (Task 5)
- Likely already works via existing HAL-link-driven pattern; verify and add test if needed

## Key Findings

### Current Authorization Issues

**TrainingGroupController.getTrainingGroup:**
- Has `@HasAuthority(Authority.MEMBERS_READ)` → any authenticated member with MEMBERS_READ can call it
- But then inside it calls `requireTrainingAuthority` implicitly only for write ops — actually reads are already allowed with MEMBERS_READ
- Wait: the issue is different. The controller has MEMBERS_READ guard but the actual 403 may come from somewhere else

**FamilyGroupController.getFamilyGroup:**
- Calls `requireMembersManageAuthority` immediately → ALWAYS returns 403 for users without MEMBERS:MANAGE. This is the confirmed bug.

**MembersGroupController.getGroup (free groups):**
- Does NOT call any authority check, checks `isOwner` for affordances only
- But calls `requireMemberProfile` which only checks `isMember()` — should be OK for members

Need to verify: TrainingGroup `getTrainingGroup` — has `@HasAuthority(Authority.MEMBERS_READ)` and calls no authority check inside → should work already? Need to check what MEMBERS_READ means in terms of who has it.

### Changes Needed

1. **FamilyGroupController.getFamilyGroup**: Remove `requireMembersManageAuthority()` check; instead allow if user hasAuthority(MEMBERS_MANAGE) OR user is a member of the group. Need to load the group first, then check.

2. **TrainingGroupController.getTrainingGroup**: Check if group members (non-GROUPS:TRAINING) can already access it. The `@HasAuthority(Authority.MEMBERS_READ)` annotation + no hard authority check inside may already work — confirm by looking at what authority club members have.

3. **FamilyGroupsRootPostprocessor**: Add MEMBERS:MANAGE condition to `klabisLinkTo` — need to check how klabisLinkTo works with authority.

4. **MembersGroupController.getGroup** (free groups): May already work since it only checks `isMember()` — confirm.

### Authority Structure
- `Authority.MEMBERS_READ` — standard user authority (ALL club members have it)
- `Authority.MEMBERS_MANAGE` — admin only
- `Authority.GROUPS_TRAINING` — training group managers (global scope)
- Standard user authorities: `MEMBERS_READ` + `EVENTS_READ`

### Confirmed Required Changes

**1. TrainingGroupController.getTrainingGroup:**
- Currently: `@HasAuthority(Authority.MEMBERS_READ)` → ANY member can read ANY training group detail
- Required: Allow GROUPS:TRAINING holders OR training group members (hasMember check). Deny non-members.
- Need to add membership check + deny non-members without GROUPS_TRAINING

**2. FamilyGroupController.getFamilyGroup:**
- Currently: calls `requireMembersManageAuthority()` immediately → always 403 for non-admins
- Required: Allow MEMBERS:MANAGE OR family group members (hasMember check)
- Need to load group first, then check: if not admin AND not member → 403

**3. FamilyGroupsRootPostprocessor:**
- Currently: always adds "family-groups" HAL link to root
- Required: Only add when user has MEMBERS:MANAGE
- `klabisLinkTo()` respects `@HasAuthority` on the target method — the listFamilyGroups already has `@HasAuthority(Authority.MEMBERS_MANAGE)` so klabisLinkTo should NOT include it for non-admin. Verify this is working via test.

**4. MembersGroupController.getGroup (free groups):**
- Currently: Only checks `isMember()` (requireMemberProfile). No ownership/membership check for reading.
- Appears to already work for members — they can view any group. The bug mentioned in proposal may only be about the navigation button test.
- Verify this works and add test confirming it.

## Agent Progress

### Team Leader Assessment (2026-04-13)
Status: IMPLEMENTATION ALREADY COMPLETE

Reviewed code state:
- FamilyGroupController.getFamilyGroup: Already has membership check (hasMembersManage OR isMember, else 403)
- TrainingGroupController.getTrainingGroup: Already has membership check (hasTrainingAuthority OR isMember OR isTrainer, else 403)
- FamilyGroupsNavigationTest: Exists with tests for MEMBERS:MANAGE visibility of collection link
- FamilyGroupControllerTest: Has shouldReturn403WhenNonMemberLacksMembersManageAuthority test
- TrainingGroupControllerTest: Has shouldReturn403WhenNonMemberLacksTrainingAuthority test
- All tasks.md items are checked

Next step: Run tests to confirm all pass, then code review, then commit.
