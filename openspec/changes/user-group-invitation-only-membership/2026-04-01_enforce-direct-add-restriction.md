# Team Coordination File — Enforce Direct Member Add Restriction

## Goal

Implement slice 3 of the user-group-invitation-only-membership proposal:
1. Domain layer: `DirectMemberAdditionNotAllowedException` + `UserGroup.addMember()` override
2. API layer: remove `addGroupMember` affordance for `WithInvitations` groups + map exception to HTTP 422

## Proposal Tasks

See `tasks.md` for full task list. All tasks (1.1–2.5) are pending.

## Key Design Decisions

- Restriction is based on `WithInvitations` interface, NOT `FreeGroup` type
- Two-layer protection: HATEOAS (no affordance) + Domain (exception)
- Exception: `DirectMemberAdditionNotAllowedException` → HTTP 422
- No frontend changes needed (button disappears via missing HAL affordance)

## Progress Log

### 2026-04-02 — backend-developer agent

All tasks 1.1–2.5 implemented and verified. 134/134 tests pass.

**Domain layer changes:**
- Created `DirectMemberAdditionNotAllowedException` (public, in `usergroups.domain`) — thrown when `addMember()` is called on a `WithInvitations` group
- `UserGroup.addMember()` now checks `this instanceof WithInvitations` and throws the exception; extracted `addMemberInternal()` as a protected bypass used by `FreeGroup.acceptInvitation()`
- `FreeGroup.acceptInvitation()` updated to call `addMemberInternal()` — invitation flow remains functional

**API layer changes:**
- `GroupController`: `addGroupMember` affordance is now only added for groups that do NOT implement `WithInvitations`; `WithInvitations` groups get the `inviteMember` affordance only
- `GroupsExceptionHandler` created in `usergroups.infrastructure.restapi` — maps `DirectMemberAdditionNotAllowedException` → HTTP 422 Unprocessable Entity

**Test updates:**
- `FreeGroupTest.AddMemberMethod`: replaced existing tests with single test asserting `DirectMemberAdditionNotAllowedException` is thrown
- All tests using `addMember()` directly on `FreeGroup` (in `FreeGroupTest`, `UserGroupPersistenceTest`) migrated to invitation flow via `addMemberViaInvitation()` helper
- `GroupControllerTest`: added `GetGroupAffordanceTests` (task 2.1) verifying affordance absence for FreeGroup and presence for TrainingGroup; added 422 test for `POST /api/groups/{id}/members` (task 2.4)

**No issues encountered.** Two-layer protection is in place: HATEOAS (no affordance) + domain exception (HTTP 422).

### 2026-04-01 — backend-developer agent (code review fixes)

Addressed two blocking code review findings. 46/46 tests pass.

**Finding 1 — Missing `inviteMember` affordance presence test:**
- Added `shouldIncludeInviteMemberAffordanceForFreeGroup` to `GetGroupAffordanceTests` in `GroupControllerTest`
- Test verifies that when an authenticated owner GETs a FreeGroup, `$._templates.inviteMember` is present in the HAL response

**Finding 2 — `GroupsExceptionHandler` scope:**
- Removed `basePackageClasses = GroupController.class` restriction from `@RestControllerAdvice`
- The annotation now applies globally, making the intent explicit (was already covering `TrainingGroupController` via package derivation, but was misleading)
- Added `AddTrainingGroupMemberTests` nested class to `TrainingGroupControllerTest` with a test verifying that `POST /api/training-groups/{id}/members` returns 422 when `DirectMemberAdditionNotAllowedException` is thrown — proving handler coverage from `TrainingGroupController`

---
