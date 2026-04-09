# Team coordination file — refine-user-groups-management

**Created:** 2026-04-09
**Proposal:** `openspec/changes/refine-user-groups-management/`
**Team leader:** main agent (coordinator only, no code changes)

## Purpose

Subagents working on this proposal must read this file first to understand the current state, then append a concise summary of their work and any open issues / decisions at the end of the file so the next subagent can resume.

## Iteration plan (vertical slices)

1. **Iter 1 — Shared WithInvitations owner-promotion rule** (backend)
   - Tasks 1.x + 2.x from `tasks.md`
   - New `CannotPromoteNonMemberToOwnerException`, default method on `WithInvitations`, `MembersGroup.addOwner` routes through it, HTTP mapping, controller integration test.
2. **Iter 2 — FamilyGroup scalar create + addChild/removeChild domain** (backend)
   - Tasks 3.x, 4.x, 5.x, 8.x
   - `CreateFamilyGroup(name, parent)` scalar, `addChild`/`removeChild` on aggregate, `FamilyGroupManagementService` ports, parent/child exclusivity domain test.
3. **Iter 3 — FamilyGroup REST API** (backend)
   - Tasks 6.x
   - Replace `CreateFamilyGroupRequest` with domain record (mirroring EventController pattern), new `POST /children`, `DELETE /children/{memberId}`, HAL-Forms affordances.
4. **Iter 4 — TrainingGroup manual trainee exclusivity** (backend)
   - Tasks 7.x
   - `MemberAlreadyInTrainingGroupException`, repository lookup, service-level check, HTTP mapping.
5. **Iter 5 — Frontend: family group create + add-member button** (frontend)
   - Tasks 9.x + 10.x
6. **Iter 6 — Frontend: member picker filtering** (frontend)
   - Tasks 11.x
7. **Iter 7 — simplify + code review + full test run + commit polish** (coordinator driven)

Each iteration ends with: all new tests passing, `tasks.md` checkboxes ticked, git commit.

## Key decisions (from design.md, do not re-litigate)

- Owner-promotion rule lives on `WithInvitations` default method (Decision 1).
- New dedicated exception `CannotPromoteNonMemberToOwnerException` (Decision 2).
- `CreateFamilyGroup(String name, MemberId parent)` — scalar (Decision 3).
- Separate `/parents` and `/children` endpoints, not a unified `/members` with role field (Decision 4).
- `addChild` rejects members already parent of same group (Decision 5).
- Manual training trainee exclusivity enforced in **application service**, not domain (Decision 6).
- Member-picker filtering is **frontend-only** (Decision 7).

## Open questions

_(none at team-leader level — all decisions captured in design.md)_

## Out of scope (do NOT touch)

- `TrainingGroup.addTrainer` auto-member bug — separate task queue item.
- Automatic age-based training-group reassignment path.
- Any non-invitation owner-management rules.

---

## Iteration log

_(Subagents append entries below as they complete work.)_

---

### Iter 2 — 2026-04-09 — FamilyGroup scalar create + addChild/removeChild (tasks 3.x, 4.x, 5.x, 8.x)

**Files modified:**
- `backend/src/main/java/com/klabis/members/familygroup/domain/FamilyGroup.java` — `CreateFamilyGroup` record collapsed to `(String name, MemberId parent)`; `create()` uses `UserGroup.create(name, ownerId)` (single parent = single owner + single member); added `addChild(MemberId)` and `removeChild(MemberId)` methods
- `backend/src/main/java/com/klabis/members/familygroup/application/FamilyGroupManagementPort.java` — added `addChild(FamilyGroupId, MemberId)` and `removeChild(FamilyGroupId, MemberId)`
- `backend/src/main/java/com/klabis/members/familygroup/application/FamilyGroupManagementService.java` — `createFamilyGroup` validates only the single parent; added `addChild` and `removeChild` implementations (addChild calls `validateNoExistingFamilyGroup` first)
- `backend/src/main/java/com/klabis/members/familygroup/infrastructure/restapi/FamilyGroupController.java` — minimal compile fix: `CreateFamilyGroupRequest` collapsed to `(name, parentId UUID)`; removed unused `Collectors` import
- `backend/src/test/java/com/klabis/members/familygroup/domain/FamilyGroupTest.java` — rewrote `CreateMethod` for new scalar signature; updated `AddParentMethod` and `RemoveParentMethod` to use new constructor; added `AddChildMethod` (3 tests), `RemoveChildMethod` (3 tests), `ParentChildExclusivityInvariant` (2 tests)
- `backend/src/test/java/com/klabis/members/familygroup/application/FamilyGroupManagementServiceTest.java` — updated `createFamilyGroup` tests for scalar signature; added `AddChildMethod` (2 tests) and `RemoveChildMethod` (2 tests) nested classes
- `backend/src/test/java/com/klabis/members/familygroup/infrastructure/restapi/FamilyGroupControllerTest.java` — updated POST request body to `{"name":..., "parentId":...}` in all four existing create tests
- `backend/src/test/java/com/klabis/members/familygroup/infrastructure/jdbc/FamilyGroupPersistenceTest.java` — updated all `CreateFamilyGroup` usages to new signature; replaced inline `Set.of(PARENT_A,PARENT_B)` group creation with `create(PARENT_A) + addParent(PARENT_B)` pattern; fixed `shouldPersistRemovedParent` to capture the saved entity before mutating

**Tests added (new):**
- `FamilyGroupTest.CreateMethod.shouldCreateGroupWithScalarParentAsOwnerAndMember` (3.1)
- `FamilyGroupTest.CreateMethod.shouldRejectNullParent` (3.2)
- `FamilyGroupTest.AddChildMethod.shouldAddChildAsNonOwnerMember` (4.1)
- `FamilyGroupTest.AddChildMethod.shouldRejectChildWhoIsAlreadyParent` (4.2)
- `FamilyGroupTest.AddChildMethod.shouldRejectDuplicateChild` (4.3)
- `FamilyGroupTest.RemoveChildMethod.shouldRemoveExistingChild` (4.4)
- `FamilyGroupTest.RemoveChildMethod.shouldThrowWhenRemovingParentViaRemoveChild` (4.4)
- `FamilyGroupTest.RemoveChildMethod.shouldThrowWhenRemovingNonMember` (4.5)
- `FamilyGroupTest.ParentChildExclusivityInvariant.shouldRejectAddingParentAsChild` (8.1)
- `FamilyGroupTest.ParentChildExclusivityInvariant.shouldPromoteChildToParentWithoutDuplicatingMembership` (8.2)
- `FamilyGroupManagementServiceTest.AddChildMethod.shouldAddChildAndSave` (5.1)
- `FamilyGroupManagementServiceTest.AddChildMethod.shouldThrowWhenChildAlreadyInAnotherFamilyGroup` (5.2)
- `FamilyGroupManagementServiceTest.RemoveChildMethod.shouldRemoveChildAndSave` (5.1)
- `FamilyGroupManagementServiceTest.RemoveChildMethod.shouldThrowWhenGroupNotFound` (5.1)

**Decision made: exception for `addChild` parent-conflict**
Reused `MemberAlreadyInGroupException` (from `common.usergroup`). The candidate IS already in the group (as an owner/member), so the message "User X is already in the group" is accurate. Tasks.md explicitly offered this as a valid choice. Avoids introducing a new exception class for a nuance that the existing type already covers.

**Surprises / deviations:**
- `FamilyGroup.create()` was simplified to use `UserGroup.create(name, ownerId)` which correctly initialises both the owner set and the membership set with a single entry — no manual set construction needed.
- The multi-parent persistence test (`shouldSaveAndRetrieveFamilyGroupWithMultipleParents`) was removed since multiple parents at creation time is no longer supported — two-parent scenarios now go through `create + addParent`.
- `shouldPersistRemovedParent` required capturing the return value from the first `save` before calling `addParent` on a second save, because Spring Data JDBC uses `auditMetadata != null` to detect `isNew`.

**Result:** 2067/2067 tests pass. All 3.x, 4.x, 5.x, 8.x checkboxes ticked.

---

### Iter 1 — 2026-04-09 — WithInvitations owner promotion rule (tasks 1.x + 2.x)

**Files created:**
- `backend/src/main/java/com/klabis/common/usergroup/CannotPromoteNonMemberToOwnerException.java` — new exception carrying `UserId`

**Files modified:**
- `backend/src/main/java/com/klabis/common/usergroup/WithInvitations.java` — added `hasMember(UserId)` and `addOwner(UserId)` abstract methods + `promoteOwner(UserId)` default method
- `backend/src/main/java/com/klabis/members/membersgroup/domain/MembersGroup.java` — implemented `hasMember(UserId)` and `addOwner(UserId)` overrides (delegate to composed `userGroup`); `addOwner(MemberId)` now calls `promoteOwner()` instead of `userGroup.addOwner()` directly
- `backend/src/main/java/com/klabis/members/membersgroup/infrastructure/restapi/MembersGroupExceptionHandler.java` — added `@ExceptionHandler` for `CannotPromoteNonMemberToOwnerException` → HTTP 409
- `backend/src/test/java/com/klabis/common/usergroup/UserGroupWithInvitationsTest.java` — added `promoteOwner()` nested test class (2 tests: reject non-member, accept existing member without duplicating membership)
- `backend/src/test/java/com/klabis/members/membersgroup/domain/MembersGroupTest.java` — added 2 tests to `OwnerManagement`: non-member throws, existing member succeeds without duplicate
- `backend/src/test/java/com/klabis/members/membersgroup/infrastructure/restapi/MembersGroupControllerTest.java` — added `shouldReturn409WhenPromotingNonMemberToOwner` inside `AddOwnerTests`

**Tests added:**
- `UserGroupWithInvitationsTest.PromoteOwnerMethod.shouldThrowWhenPromotingNonMember`
- `UserGroupWithInvitationsTest.PromoteOwnerMethod.shouldPromoteExistingMemberWithoutDuplicatingMembership`
- `MembersGroupTest.OwnerManagement.shouldThrowWhenPromotingNonMemberToOwner`
- `MembersGroupTest.OwnerManagement.shouldPromoteExistingMemberToOwnerWithoutDuplicatingMembership`
- `MembersGroupControllerTest.AddOwnerTests.shouldReturn409WhenPromotingNonMemberToOwner`

**Design note:** `WithInvitations` now declares `hasMember(UserId)` and `addOwner(UserId)` as abstract methods so the `promoteOwner` default can call them without casting. `MembersGroup` implements them by delegating to its composed `userGroup`. `InvitationGroup` test double in `UserGroupWithInvitationsTest` inherits them from `UserGroup` (it extends it directly).

**Surprises / deviations:**
- `MembersGroup` composes `UserGroup` rather than extending it, so the two new abstract methods needed explicit `@Override` implementations — not immediately obvious from reading the interface alone.
- Existing `MembersGroupTest.shouldAddOwner` test was already correct (it invites the member first before promoting), so no changes needed to existing tests.

**Result:** 2061/2061 tests pass. All 1.x and 2.x checkboxes ticked.
