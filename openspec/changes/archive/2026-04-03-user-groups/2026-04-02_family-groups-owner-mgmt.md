# Team Coordination File — Family Groups + Owner Management

## Goal

Implement Slice 4 (Family Groups, tasks 5.1–5.5) and Slice 5 (Owner Management + Members Integration, tasks 6.1–6.8) from the user-groups proposal.

## Scope

### Slice 4 — Family Groups (tasks 5.1–5.5)
- Domain: `FamilyGroup` concrete aggregate
- Application service: create family group (MEMBERS:MANAGE), exclusive membership (max 1 family group per member), delete
- REST API: family group endpoints (create, list, detail, delete)
- Frontend: create family group from members list page, family group info on member profile
- Tests: domain unit, integration (authorization, creation)

### Slice 5 — Owner Management + Members Integration (tasks 6.1–6.8)
- Domain: `addOwner(MemberId)`, `removeOwner(MemberId)` on `UserGroup` with last-owner validation
- REST API: owner management endpoints (add owner, remove owner) with HAL affordances
- Frontend: owner management UI on group detail pages
- Public query API: `UserGroups` interface at module root — `findGroupsWhereLastOwner(MemberId)`
- Members integration: modify `ManagementService.suspendMember()` to call `UserGroups.findGroupsWhereLastOwner()`
- Backend: warning response when suspending last owner
- Frontend: suspension warning dialog
- Tests: domain, integration, cross-module Spring Modulith test

## Key Design Decisions

- Exclusive family group membership enforced at application service level + unique DB index safety net
- `FamilyGroup` has no extra fields beyond `UserGroup` (type discriminator only)
- Family groups created from members list page, require MEMBERS:MANAGE permission
- Owner management: `addOwner`/`removeOwner` on abstract `UserGroup`, last-owner invariant rejects removal of sole owner
- Public `UserGroups` interface (module root) — synchronous query API for cross-module use
- Training group last-owner suspension → requires successor; family/free → successor OR dissolve

## Current Architecture

- Module: `com.klabis.usergroups` (Spring Modulith)
- Single-table inheritance: `user_groups` table (type: TRAINING, FAMILY, FREE)
- `UserGroup` — abstract aggregate root
- `FreeGroup extends UserGroup implements WithInvitations`
- `TrainingGroup extends UserGroup` (age range)
- `FamilyGroup extends UserGroup` — NOT YET IMPLEMENTED
- `UserGroups.java` interface at module root — NOT YET IMPLEMENTED
- `GroupManagementService`, `InvitationService`

## Progress Log

<!-- Agents append their summaries here -->

### Slice 4 — Family Groups (2026-04-02, backend-developer agent)

Implemented tasks 5.1–5.3 + 5.5 (5.4 frontend skipped per instructions).

**Changes:**

- `FamilyGroup.java` — new domain class extending `UserGroup` with `TYPE_DISCRIMINATOR = "FAMILY"`, `CreateFamilyGroup` command record, `create()` and `reconstruct()` factory methods
- `UserGroupRepository` — added `findAllFamilyGroups()` and `findFamilyGroupByMember(MemberId)`
- `UserGroupJdbcRepository` — added two SQL queries for FAMILY type
- `UserGroupMemento` — added `FamilyGroup` case in `toUserGroup()` switch and `discriminatorFor()`
- `UserGroupRepositoryAdapter` — implemented two new repository methods
- `GroupManagementPort` + `GroupManagementService` — added `createFamilyGroup()` (with exclusive membership check), `listFamilyGroups()`, `getFamilyGroup()`, `deleteFamilyGroup()`
- `MemberAlreadyInFamilyGroupException` — public exception (extends `BusinessRuleViolationException`, mapped to HTTP 400 by global handler)
- `FamilyGroupController` — REST endpoints: POST `/api/family-groups`, GET list, GET detail, DELETE; requires `MEMBERS:MANAGE`; HAL+FORMS links
- `FamilyGroupsRootPostprocessor` — adds `family-groups` link to root API
- `FamilyGroupTest` — 8 domain unit tests
- `FamilyGroupControllerTest` — 13 controller integration tests (auth, CRUD, exclusivity)

**Notes:**
- Exclusive membership validated in `GroupManagementService.validateNoExistingFamilyGroup()` — checks both the creating user and all initial members
- `MemberAlreadyInFamilyGroupException` returns HTTP 400 (not 422) because it extends `BusinessRuleViolationException` which is globally mapped to 400 by `MvcExceptionHandler`
- DB schema already had `FAMILY` as valid discriminator value in comments — no migration needed
- All 1784 tests pass

### Slice 5 — Owner Management + Members Integration (2026-04-02, backend-developer agent)

Implemented tasks 6.1, 6.2, 6.4, 6.5, 6.6, 6.8 (6.3 and 6.7 are frontend tasks).

**Changes:**

- `UserGroup.java` — added `addOwner(MemberId)`, `removeOwner(MemberId)`, `isLastOwner(MemberId)` methods; added `CannotRemoveLastOwnerException` (extends `BusinessRuleViolationException`, mapped to HTTP 422)
- `UserGroupOwnershipInfo.java` (new) — public record at module root: `(UUID groupId, String groupName, String groupType)`, used in the public `UserGroups` API
- `UserGroups.java` (new) — public interface at module root: `findGroupsWhereLastOwner(MemberId)` — cross-module query API
- `UserGroupRepository` — added `findAllByOwner(MemberId)`
- `UserGroupJdbcRepository` — added SQL query joining `user_group_owners`
- `UserGroupRepositoryAdapter` — implemented `findAllByOwner`
- `GroupManagementPort` + `GroupManagementService` — added `addOwnerToGroup()`, `removeOwnerFromGroup()`
- `UserGroupsImpl.java` (new) — implements both `UserGroups` and `LastOwnershipChecker`; public class so Spring can autowire the `LastOwnershipChecker` across module boundary
- `GroupsExceptionHandler` — added `@Order(1)` to take priority over global handler; added HTTP 422 handler for `CannotRemoveLastOwnerException`
- `AddOwnerRequest.java` (new) — request record with `toMemberId()` helper
- `GroupController`, `TrainingGroupController`, `FamilyGroupController` — added `addOwner`/`removeOwner` endpoints; updated HATEOAS affordances (addOwner on group detail for owners; removeOwner on each owner entity when multiple owners exist)
- `LastOwnershipChecker.java` (new) — port interface in `com.klabis.members` root package (NOT application package) — enables `usergroups` to implement it without violating Spring Modulith's cross-module type exposure rules
- `MemberIsLastGroupOwnerException.java` (new) — in `members.application`; carries `List<OwnedGroupInfo>`
- `ManagementService` — added `LastOwnershipChecker` dependency; checks before suspension, throws `MemberIsLastGroupOwnerException` if member is last owner of any group
- `MembersExceptionHandler.java` (new) — maps `MemberIsLastGroupOwnerException` → HTTP 409 with `affectedGroups` array
- Module-isolation tests patched — added `@MockitoBean LastOwnershipChecker` to 5 tests: `RegisterMemberAutoProvisioningTest`, `UpdateMemberPersistenceTest`, `MemberControllerSecurityTest`, `MemberLifecycleE2ETest`, `EventManagementE2ETest`, `EventRegistrationE2ETest`

**Key design decision: dependency inversion to break cycle**

Direct injection of `UserGroups` into `ManagementService` (members → usergroups → members) caused a cycle rejected by Spring Modulith even with `allowedDependencies`. The fix: `LastOwnershipChecker` interface defined in `members` root package, implemented in `usergroups.application.UserGroupsImpl`. The `members` module depends only on its own interface; `usergroups` depends on `members.LastOwnershipChecker` (an exposed type).

**Notes:**
- `LastOwnershipChecker` MUST be in the `members` root package (not `members.application`) — Spring Modulith only considers root-package types as "exposed" public API of a module
- `GroupsExceptionHandler` requires `@Order(1)` — without it, the global `MvcExceptionHandler` catches `BusinessRuleViolationException` (parent of `CannotRemoveLastOwnerException`) first and returns HTTP 400
- All 1805 tests pass

### Compilation Error Verification (2026-04-02, backend-developer agent)

Task: Verify and fix compilation errors listed in the task description.

**Outcome: No changes needed.** All reported compilation errors were already resolved by the Slice 5 implementation agent. Full compilation (`compileJava`, `compileTestJava`) and all 1805 tests pass successfully.

**Verification findings:**
- `CannotRemoveLastOwnerException` — defined as nested static class inside `UserGroup` (line 108), resolves correctly
- `UserGroupRepositoryAdapter.findAllByOwner` — implemented, delegates to `UserGroupJdbcRepository.findAllByOwnerId`
- `GroupManagementService.addOwnerToGroup` / `removeOwnerFromGroup` — both implemented
- `GroupController.buildOwnerModel` — correct signature and call site
- `ManagementService` — imports `LastOwnershipChecker` from `com.klabis.members` (correct)
- `MembersExceptionHandler` — uses `info.groupId()` (String), no UUID confusion
- `ManagementServiceTest` — has `@Mock LastOwnershipChecker lastOwnershipChecker` and correct 4-arg constructor call

### Code Review Fixes (2026-04-02, backend-developer agent)

Task: Fix all BLOCKING and WARNING findings from code review of Slice 4 + Slice 5.

**All 6 findings fixed (all 71 affected tests pass):**

**BLOCKING:**

1. **`UserGroupsImpl` — duplicate logic eliminated** — extracted `findSolelyOwnedGroups(MemberId)` private helper used by both `findGroupsWhereLastOwner()` and `findGroupsOwnedSolely()`. Both methods now call the shared helper. Single DB query, single filter predicate.

2. **`FamilyGroupController.getFamilyGroup` — `addOwner` affordance fixed** — affordance now only added when `requestingUserIsOwner == true`. Passed `requestingUserIsOwner` flag into `toFamilyGroupResponse()`.

3. **`FamilyGroupController.toFamilyGroupResponse` — `removeOwner` affordance fixed** — condition changed from `ownerIds.size() > 1` to `requestingUserIsOwner && ownerIds.size() > 1`, matching the pattern from `GroupController.buildOwnerModel()`.

**WARNING:**

4. **`UserGroupsImpl` — type discriminator** — replaced `group.getClass().getSimpleName()` with `typeDiscriminatorFor(group)` private method using `TYPE_DISCRIMINATOR` constants (`"FAMILY"`, `"TRAINING"`, `"FREE"`). Consistent with what the DB and 409 response schema expect.

5. **`GroupManagementService` — unchecked casts** — added `@SuppressWarnings("unchecked")` to `createFamilyGroup`, `createTrainingGroup`, and `updateTrainingGroupAgeRange`. Casts are safe by construction (the concrete type is known at the call site).

6. **`UserGroupJdbcRepository.findFamilyGroupsByMemberId` — owners included** — rewritten from `JOIN user_group_members` to `EXISTS` subqueries covering both `user_group_members` AND `user_group_owners`, preventing bypass of exclusive-membership constraint via ownership.

7. **`FamilyGroupControllerTest` — non-owner remove test added** — new test `shouldReturn400WhenNonOwnerAttemptsRemoveOwner`: MEMBERS_MANAGE user who is not the group owner attempts `DELETE /api/family-groups/{id}/owners/{memberId}` → service throws `NotGroupOwnerException` → global handler maps to HTTP 400.

**Ignored per instructions:** Nález 7 (addOwner without member check), Nález 8 (TrainingGroupController double-check), Nález 10.
