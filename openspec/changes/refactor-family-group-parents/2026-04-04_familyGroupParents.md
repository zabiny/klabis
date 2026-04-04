# Team Coordination File: refactor-family-group-parents

## Proposal Summary
Rename owners→parents in FamilyGroup API, explicit parent assignment at creation (min 1), addParent=addOwner+addMember, removeParent=removeOwner+removeMember, MEMBERS:MANAGE auth for all family group management.

## Task File
`./openspec/changes/refactor-family-group-parents/tasks.md`

## Iteration Plan

### Iteration 1: Domain + Application Service
- Tasks 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5

### Iteration 2: REST API
- Tasks 3.1, 3.2, 3.3, 3.4, 3.5, 3.6

### Iteration 3: Frontend
- Tasks 4.1, 4.2, 4.3

## Progress Log

### Iteration 1 — 2026-04-04 — Domain + Application Service (Tasks 1.1–2.5) DONE

**Domain changes (`FamilyGroup`):**
- `CreateFamilyGroup` command: changed `owner: MemberId` → `parents: Set<MemberId>` with `@Assert.notEmpty` validation (min 1)
- `FamilyGroup.create()`: sets all parents as owners and adds them as members via `addMemberInternal()`
- Added `getParents()` delegating to `getOwners()`
- Added `addParent(MemberId)`: calls `addOwner()` + `addMemberInternal()`
- Added `removeParent(MemberId)`: calls `removeOwner()` (enforces last-parent invariant) then `removeMember()` (bypasses owner guard since ownership already removed)
- `FamilyGroupTest`: fully rewritten with new command shape and parent method coverage

**Application service changes:**
- `GroupManagementService.createFamilyGroup()`: validates exclusive membership for all parents (not just single owner)
- Added `addParentToFamilyGroup(UserGroupId, MemberId)` with `loadFamilyGroup()` type guard
- Added `removeParentFromFamilyGroup(UserGroupId, MemberId)`
- `GroupManagementPort`: extended with two new family group parent methods
- New test class `GroupManagementServiceFamilyGroupTest` covers create, addParent, removeParent scenarios

**Collateral fixes (to keep build green):**
- `FamilyGroupController.CreateFamilyGroupRequest`: added `parentIds` field, removed automatic owner from authenticated user
- `FamilyGroupControllerTest`: updated create request bodies to include `parentIds`
- `UserGroupPersistenceTest`: updated 3 `CreateFamilyGroup` instantiations to use `Set.of(OWNER)` shape

All 1892 tests pass.

### Iteration 2 — 2026-04-04 — REST API (Tasks 3.1–3.6) DONE

**Controller changes (`FamilyGroupController`):**
- Task 3.1 was already complete from Iteration 1 (`parentIds` field, no automatic owner)
- `POST /{id}/owners` renamed to `POST /{id}/parents` → delegates to `addParentToFamilyGroup()`
- `DELETE /{id}/owners/{memberId}` renamed to `DELETE /{id}/parents/{memberId}` → delegates to `removeParentFromFamilyGroup()`
- `FamilyGroupResponse`: `owners` field renamed to `parents`, `OwnerResponse` replaced by new `ParentResponse` record
- Authorization: add/remove parent endpoints use `MEMBERS:MANAGE` only — no owner-based check
- HAL affordance `addFamilyGroupParent` shown only for `hasMembersManage` (was `requestingUserIsOwner`)
- Per-parent remove affordance shown only when `hasMembersManage && parentIds.size() > 1`

**Test changes (`FamilyGroupControllerTest`):**
- `AddFamilyGroupOwnerTests` → `AddFamilyGroupParentTests`, endpoint path updated
- `RemoveFamilyGroupOwnerTests` → `RemoveFamilyGroupParentTests`, endpoint path updated, removed `NotGroupOwnerException` test (no longer applies)
- Added happy-path 204 test for remove parent
- `GetFamilyGroupTests`: response field assertion `$.owners` → `$.parents`
- Mock calls updated: `addOwnerToGroup`/`removeOwnerFromGroup` → `addParentToFamilyGroup`/`removeParentFromFamilyGroup`

All 1893 tests pass.
