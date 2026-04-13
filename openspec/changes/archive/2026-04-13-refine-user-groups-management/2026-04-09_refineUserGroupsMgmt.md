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

### Iter 6 — 2026-04-09 — Frontend: member picker filtering (tasks 11.x)

**Component identified (11.1):**
`HalFormsMemberId` (`frontend/src/components/HalNavigator2/halforms/fields/HalFormsMemberId.tsx`) is the single member-picker component used across all group dialogs. It is rendered generically by `KlabisFieldsFactory` for fields of type `MemberId` or `UUID`. No standalone member picker outside HAL-Forms was found.

**How filtering was threaded through HAL-Forms (11.2):**
- Added `excludeIds?: string[]` and `includeIds?: string[]` props directly to `HalFormsMemberId`. These filter `rawOptions` after the `useHalFormOptions` fetch returns. `includeIds` takes precedence if both are provided (whitelist semantics).
- `KlabisFieldsFactory` was refactored: extracted `memberIdFieldRenderer(conf, extraProps?)` to avoid code duplication, and added `createMemberFilteredFactory(excludeIds?, includeIds?): HalFormFieldFactory` — a factory creator that, when called for `MemberId`/`UUID` fields, passes the filter props to `HalFormsMemberId`. For all other field types it falls through to the standard `klabisFieldsFactory`.
- `HalFormDisplay` gained two optional props: `excludeMemberIds?: string[]` and `includeOnlyMemberIds?: string[]`. When either is set, it replaces the default `klabisFieldsFactory` with the output of `createMemberFilteredFactory(...)` via a `useMemo`. The caller-provided `fieldsFactory` prop still takes full precedence over both filter props.

**Wiring from each caller (11.3):**
- `FamilyGroupDetailPage` — computes `allCurrentMemberIds = [...parents.map(p=>p.memberId), ...children.map(c=>c.memberId)]`; passes `excludeMemberIds={allCurrentMemberIds}` to both add-parent and add-child `HalFormDisplay` instances.
- `TrainingGroupDetailPage` — computes `trainerIds` and `memberIds` from the response arrays; passes `excludeMemberIds={memberIds}` to the add-trainee form and `excludeMemberIds={trainerIds}` to the add-trainer form.
- `GroupDetailPage` — computes `currentMemberIds` from `resourceData.members`; passes `excludeMemberIds={currentMemberIds}` to the add-member form.

**Promote-to-owner picker restriction (11.4):**
The `addGroupOwner` form on `GroupDetailPage` now receives `includeOnlyMemberIds={currentMemberIds}`. This causes `HalFormsMemberId` to show only current members in the picker — consistent with the backend rule (iteration 1) that rejects promoting non-members. The `includeIds` whitelist approach was chosen over `excludeIds` because the complement (everyone-minus-members) would require fetching the full member list, which is not available on the detail page. Whitelisting the members already in hand is the correct approach here.

**Files modified:**
- `frontend/src/components/HalNavigator2/halforms/fields/HalFormsMemberId.tsx` — added `HalFormsMemberIdProps` interface with `excludeIds`/`includeIds`; filter applied after `useHalFormOptions`
- `frontend/src/components/HalNavigator2/halforms/fields/HalFormsMemberId.test.tsx` — fixed pre-existing wrong mock path (`../../../` → `../../../../`); added 2 new tests for exclude/include filtering
- `frontend/src/components/KlabisFieldsFactory.tsx` — extracted `memberIdFieldRenderer`; added `createMemberFilteredFactory` export
- `frontend/src/components/HalNavigator2/HalFormDisplay.tsx` — added `excludeMemberIds`/`includeOnlyMemberIds` props; `useMemo` selects effective factory
- `frontend/src/pages/family-groups/FamilyGroupDetailPage.tsx` — computed `allCurrentMemberIds`; wired `excludeMemberIds`
- `frontend/src/pages/training-groups/TrainingGroupDetailPage.tsx` — computed `trainerIds`/`memberIds`; wired `excludeMemberIds` on both add-trainee and add-trainer forms
- `frontend/src/pages/groups/GroupDetailPage.tsx` — computed `currentMemberIds`; wired `excludeMemberIds` on add-member, `includeOnlyMemberIds` on add-owner

**Pre-existing bug fixed (incidental):**
`HalFormsMemberId.test.tsx` had the wrong relative path for its `vi.mock('useHalFormOptions')` — three levels up instead of four. This meant the mock was silently not applied, so options were never rendered in those tests. All 5 original tests happened to pass because they tested Formik value state and label presence, not options rendering. The fix was part of this iteration since the new tests require the mock to work correctly.

**Tests:**
- 2 new tests in `HalFormsMemberId.test.tsx` (exclude filtering, include filtering)
- 1106/1106 frontend tests pass; TypeScript build clean

**Result:** All 11.x checkboxes ticked.

---

_(Subagents append entries below as they complete work.)_

---

### Backend simplify polish — 2026-04-09 — 5 code review fixes (backend only)

**Fix 1 — `MemberAlreadyInTrainingGroupException` extends `BusinessRuleViolationException`**
Changed from `RuntimeException` to `BusinessRuleViolationException`. Added `memberId` and `conflictingGroupId` accessors to match convention. The per-controller `TrainingGroupExceptionHandler` still maps it to 409, overriding the global 400 handler — same pattern as existing per-module handlers.

**Fix 2 — `CannotPromoteNonMemberToOwnerException` handler moved to global `MvcExceptionHandler`**
Removed `@ExceptionHandler(CannotPromoteNonMemberToOwnerException.class)` from `MembersGroupExceptionHandler` (members-module scoped). Added equivalent 409 mapping to `MvcExceptionHandler` (global). The exception lives in `common.usergroup` and can be thrown by any group type; the global handler is the correct home. Spring picks the most-specific subclass, so the new handler takes precedence over the `BusinessRuleViolationException` → 400 handler.

**Fix 3 — Removed WHAT-comments from `FamilyGroup.addChild` and `removeChild`**
Two comments describing what the code does (not why) were removed per project convention in CLAUDE.md.

**Fix 4 — Filter same-group case in `TrainingGroupManagementService.addMemberToTrainingGroup`**
Added `.filter(existing -> !existing.getId().equals(id))` to the `findGroupForMember` call. Without this, a member already in the target group would trigger `MemberAlreadyInTrainingGroupException` with `conflictingGroupId == id` — a confusing error. The aggregate's own duplicate check handles the same-group case correctly.

**Fix 5 — Renamed `AddParentRequest` to `AddMemberRequest`**
The record was reused for both `POST /parents` and `POST /children` endpoints. The name `AddParentRequest` was misleading on the child path. Renamed via JetBrains refactoring — 2 usages updated in `FamilyGroupController.java`; test files build raw JSON bodies and were not affected.

**Result:** 2081/2081 backend tests pass. No new tests added.

---

### Simplify fixes — 2026-04-09 — useMemo for id-list derivations + guard verification

**Fix 1 — Memoize id-list derivations (4 sites):**
All three group detail pages were computing id arrays unconditionally on every render, passing unstable array references as `excludeMemberIds`/`includeOnlyMemberIds` to `HalFormDisplay`. Because `HalFormDisplay` has an internal `useMemo` on `effectiveFieldsFactory` that depends on those props, the new reference on every parent render defeated that memo and caused the filtered factory to be recreated unnecessarily.

- `FamilyGroupDetailPage`: `allCurrentMemberIds` wrapped in `useMemo([parents, children])`
- `GroupDetailPage`: `currentMemberIds` wrapped in `useMemo([resourceData.members])`
- `TrainingGroupDetailPage`: `trainerIds` wrapped in `useMemo([trainers])`, `memberIds` wrapped in `useMemo([members])`

`useMemo` imported from React in all three files (was not previously imported).

**Fix 2 — Guard in `createMemberFilteredFactory` verified correct:**
The existing guard `(excludeIds && excludeIds.length > 0) || includeIds !== undefined` correctly uses strict `=== undefined` for the whitelist check. Passing `includeIds = []` (empty whitelist) correctly falls through to the filtered factory path. No change required.

**Verification:** `npx tsc --noEmit` → exit 0. `run-frontend-tests.sh` → 1106/1106 passed.

---

### Iter 6 correction — 2026-04-09 — TypeScript compiler verification

**What was reported:** The previous Iter 6 agent declared "build clean, 1106/1106 tests pass" but a subsequent IDE diagnostic scan showed a large number of TypeScript errors (unused imports, missing exports, unused variables in caller pages).

**What was found:** Running `npx tsc --noEmit` in `frontend/` reported **zero errors** (exit code 0). The code as committed is TypeScript-clean. Specifically:
- `createMemberFilteredFactory` IS exported from `KlabisFieldsFactory.tsx` (line 207) and IS imported in `HalFormDisplay.tsx` (line 8) — the "no exported member" IDE diagnostic was stale.
- `effectiveFieldsFactory` IS consumed — it is passed to `HalFormsForm` at line 158 — the "declared but never read" IDE diagnostic was stale.
- All caller pages (`FamilyGroupDetailPage`, `TrainingGroupDetailPage`, `GroupDetailPage`) DO pass `excludeMemberIds` / `includeOnlyMemberIds` to their respective `HalFormDisplay` usages — the "unused variable" IDE diagnostics were stale.
- The `HalFormsMemberId.tsx` line 26 diagnostic ("Argument of type 'string | number' is not assignable to 'string'") was also stale — `String(opt.value)` always produces `string`, and the array parameter to `.includes()` is `string[]`.

**Root cause of false alarms:** JetBrains IDE caches diagnostics between file saves and can show errors for code that no longer exists or has already been corrected. The IDE was showing an earlier broken state of the files.

**Actual verification result:** `npx tsc --noEmit` → exit code 0, no errors. `run-frontend-tests.sh` → 1106/1106 passed.

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

### Iter 3 — 2026-04-09 — FamilyGroup REST API update (tasks 6.x)

**Files modified:**
- `backend/src/main/java/com/klabis/members/familygroup/infrastructure/restapi/FamilyGroupController.java` — dropped `CreateFamilyGroupRequest` DTO; `@RequestBody` now binds directly onto `FamilyGroup.CreateFamilyGroup` (mirrors `EventController` pattern); added `addFamilyGroupChild` (`POST /{id}/children`) and `removeFamilyGroupChild` (`DELETE /{id}/children/{memberId}`) handlers; `getFamilyGroup` now emits `addFamilyGroupChild` affordance alongside `addFamilyGroupParent` when user has `MEMBERS:MANAGE`; `toFamilyGroupResponse` split into parent and child model builders — children filtered by excluding parent `MemberId`s; `buildChildModel` emits `removeChild` affordance per child when user has `MEMBERS:MANAGE`
- `backend/src/test/java/com/klabis/members/familygroup/infrastructure/restapi/FamilyGroupControllerTest.java` — updated all existing POST body JSON from `parentId` to `parent` field name (domain record field); added `AddFamilyGroupChildTests` (3 tests), `RemoveFamilyGroupChildTests` (2 tests), `FamilyGroupDetailAffordancesTests` (2 tests); added `shouldReturn400WhenParentIdIsMissing` and `shouldCreateFamilyGroupWithOneParentAndNoChildren` tests

**Tests added (new):**
- `CreateFamilyGroupTests.shouldReturn400WhenParentIdIsMissing` (6.2)
- `CreateFamilyGroupTests.shouldCreateFamilyGroupWithOneParentAndNoChildren` (6.1)
- `AddFamilyGroupChildTests.shouldReturn204WhenAddingChild` (6.3)
- `AddFamilyGroupChildTests.shouldReturn400WhenChildIsAlreadyParent` (6.4)
- `AddFamilyGroupChildTests.shouldReturn403WhenMissingAuthority` (6.3)
- `RemoveFamilyGroupChildTests.shouldReturn204WhenRemovingChild` (6.5)
- `RemoveFamilyGroupChildTests.shouldReturn403WhenMissingAuthority` (6.5)
- `FamilyGroupDetailAffordancesTests.shouldIncludeAddParentAndAddChildAffordancesWhenAuthorized` (6.6)
- `FamilyGroupDetailAffordancesTests.shouldOmitAffordancesWhenNotAuthorized` (6.6)

**Pattern mirrored for @RequestBody + MemberIdMixin:** `EventController` — `@Valid @RequestBody Event.CreateEvent command` binds the domain record directly; `MemberIdMixin` (`@JacksonMixin(MemberId.class)`) is auto-discovered by Spring Boot's Jackson auto-configuration and applies in `@WebMvcTest` without any extra `@Import`.

**HAL-Forms affordance template names:** Spring HATEOAS derives template names from the controller method name with first letter lower-cased. `addFamilyGroupParent` → `addFamilyGroupParent`, `addFamilyGroupChild` → `addFamilyGroupChild`. Initial test used `addParent`/`addChild` which failed; corrected to use full method names.

**JSON field name change:** `CreateFamilyGroup` record has field `parent` (not `parentId`). Existing controller tests that sent `"parentId"` in the JSON body were updated to `"parent"` to match the domain record field name. This is the only breaking API change from the old `CreateFamilyGroupRequest`.

**Child filtering in detail response:** `toFamilyGroupResponse` now filters the members list to exclude parents (owners), so the `members` array in the API response contains only children, not parents who appear in the `parents` array too.

**Result:** 2076/2076 tests pass. All 6.x checkboxes ticked.

---

### Iter 4 — 2026-04-09 — TrainingGroup manual trainee exclusivity (tasks 7.x)

**Files created:**
- `backend/src/main/java/com/klabis/members/traininggroup/application/MemberAlreadyInTrainingGroupException.java` — new exception carrying `MemberId` and conflicting `TrainingGroupId`
- `backend/src/main/java/com/klabis/members/traininggroup/infrastructure/restapi/TrainingGroupExceptionHandler.java` — `@RestControllerAdvice` scoped to `TrainingGroupController`, maps new exception to HTTP 409

**Files modified:**
- `backend/src/main/java/com/klabis/members/traininggroup/application/TrainingGroupManagementService.java` — added `findGroupForMember` call at the top of `addMemberToTrainingGroup` (manual path only); throws before `loadTrainingGroup` on conflict
- `backend/src/test/java/com/klabis/members/traininggroup/application/TrainingGroupManagementServiceTest.java` — added `AddMemberToTrainingGroupMethod` nested class (4 tests, tasks 7.1–7.4)
- `backend/src/test/java/com/klabis/members/traininggroup/infrastructure/restapi/TrainingGroupControllerTest.java` — added `shouldReturn409WhenMemberAlreadyInAnotherTrainingGroup` test inside `AddTrainingGroupMemberTests` (task 7.8)

**Tests added (new):**
- `TrainingGroupManagementServiceTest.AddMemberToTrainingGroupMethod.shouldThrowWhenMemberIsAlreadyTraineeOfAnotherGroup` (7.1)
- `TrainingGroupManagementServiceTest.AddMemberToTrainingGroupMethod.shouldSucceedWhenMemberIsNotTraineeAnywhere` (7.2)
- `TrainingGroupManagementServiceTest.AddMemberToTrainingGroupMethod.shouldSucceedWhenMemberIsOnlyTrainerElsewhere` (7.3)
- `TrainingGroupManagementServiceTest.AddMemberToTrainingGroupMethod.shouldNotApplyExclusivityCheckOnAutoAssignPath` (7.4)
- `TrainingGroupControllerTest.AddTrainingGroupMemberTests.shouldReturn409WhenMemberAlreadyInAnotherTrainingGroup` (7.8)

**How trainer vs trainee was distinguished:**
No new repository method was needed. `TrainingGroupRepository.findGroupForMember(MemberId)` already existed and queries `training_group_members` (the trainee/member table), not `training_group_trainers`. Trainers live only in `training_group_trainers`. So the existing method returns `Optional.empty()` for a member who is only a trainer of another group — the trainer exemption is structural, not conditional logic.

**Automatic path (untouched):**
`createTrainingGroup` calls `assignEligibleMember` in a loop. The guard (`findGroupForMember`) is only in `addMemberToTrainingGroup` (manual path). Test 7.4 verifies `findGroupForMember` is never called during `createTrainingGroup`.

**Surprises / deviations:**
- Test 7.1 initially included an unnecessary stub for `findById(GROUP_ID)` — removed since the service throws before reaching `loadTrainingGroup`. Mockito strict stubbing detected this immediately.
- `MemberAlreadyInTrainingGroupException` extends `RuntimeException` directly (not `BusinessRuleViolationException`) because it is mapped to 409 via a dedicated handler. Extending `BusinessRuleViolationException` would have mapped it to 400 via `MvcExceptionHandler` unless overridden, creating ambiguity.

**Result:** 2081/2081 tests pass. All 7.x checkboxes ticked.

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

---

### Iter 5 — 2026-04-09 — Frontend: family group create + add-member button (tasks 9.x + 10.x)

**Files modified:**
- `frontend/src/localization/labels.ts` — added `templates.addFamilyGroupChild`, `templates.removeFamilyGroupChild`, `templates.addFamilyGroupMember`; added `sections.familyGroupChildren`; added `familyGroupRoles` section (`parent`, `child`)
- `frontend/src/pages/family-groups/FamilyGroupDetailPage.tsx` — replaced standalone "Add parent" button with unified "Přidat člena" button; replaced single `addParentModal` state with two-step role-picker flow (`addMemberRolePicker` + `addMemberModal`); renamed `FamilyGroupMember` to `FamilyGroupChild` to clarify the backend's members array now contains only non-parent members (children); added children table with per-row "Odebrat" action when `removeFamilyGroupChild` affordance present on the child item; removed the separate parents card shown conditionally on `addFamilyGroupParent` presence — parents section now shows whenever there are parents
- `frontend/src/pages/family-groups/FamilyGroupDetailPage.parents.test.tsx` — updated tests to reflect unified button: "Přidat rodiče" standalone button tests replaced with role-picker flow assertions

**Tests added (new file):**
- `FamilyGroupDetailPage.addMember.test.tsx` — 14 tests covering all 10.x tasks:
  - button visibility based on `addFamilyGroupParent` and/or `addFamilyGroupChild` affordance presence
  - absence of old standalone "Přidat rodiče" button
  - role picker opens on click, shows only available role options
  - selecting a role transitions to HalFormDisplay
  - children section with "DĚTI" label
  - "Odebrat" button per child (affordance-gated), opens modal on click

**Block A (9.x) — create dialog:**
The `FamilyGroupsPage` already delegates entirely to `HalFormButton name="createFamilyGroup"`, which renders the HAL-Forms template returned by the backend. No frontend code change was required because:
- The form fields come from the backend template (the backend now returns only `name` + `parent` properties in `createFamilyGroup`)
- The navigate-on-create behavior (POST + Location → navigate) is already implemented generically in `HalFormDisplay`
- No bespoke "initial members" or "multi-parent" picker ever existed in `FamilyGroupsPage` — the old form was entirely template-driven

**Role picker UX decision:**
The unified dialog is implemented as a two-step flow: (1) a role picker modal with buttons "Rodič" / "Dítě", each only rendered when the corresponding HAL-Forms affordance is present; (2) a second modal with the HalFormDisplay for the chosen template. This keeps the add-parent and add-child paths entirely template-driven without any client-side routing logic beyond reading the affordance map.

**Rough edges / notes for iter 6:**
- The member picker within both add-parent and add-child forms currently shows all club members — iteration 6 will add filtering to exclude members already in the group.
- The `removeFamilyGroupChild` template target URL is used as-is from the child item's `_templates` — no path construction needed since the backend embeds the full target URL in the affordance.

**Result:** 1104/1104 frontend tests pass. TypeScript build clean. All 9.x and 10.x checkboxes ticked.
