## Why

A review of the user groups area surfaced a mix of implementation bugs, UX inconsistencies, and missing spec coverage:

- **Bug — free group owner promotion bypasses the invitation flow.** Promoting a non-member directly to owner silently adds them as a member, defeating the invitation rule that is the entire point of free groups.
- **Bug — a member can become a trainee in multiple training groups manually.** The spec already limits trainees to one training group, but manual "add member" in the UI does not enforce it.
- **UX — creating a family group is heavier than it needs to be.** The current flow asks for multiple parents and optional initial members in one dialog; in practice users want to name the family and pick one parent, then add the rest from the detail page.
- **UX — family group detail has a single "Add parent" button but no way to add a child after creation.** The backend has no endpoint for adding or removing children in an existing family group; once created, the child roster is frozen.
- **UX — member-picker dialogs still offer people who are already in the group,** leading to confusing errors.
- **Gap — the spec never spells out that a member cannot be both a parent and a child in the same family group.** Today the data model accidentally prevents it, but there is no test and no documentation that this is an intentional invariant.

This proposal tightens the rules, fills the missing endpoints, and updates the spec to reflect the real desired behavior.

## What Changes

- **BREAKING — Family group creation is simplified.** `CreateFamilyGroup` now takes a single parent, no additional members. API and domain command both move from collection-valued `parents`/`initialMembers` to a scalar `parent`.
- **Family group detail gains "add/remove child" endpoints.** The frontend consolidates the two actions (add parent, add child) behind a single "Add member" button that asks the admin to choose parent vs. child and routes to the correct endpoint.
- **Adding a child validates that the member is not already a parent of the same family group.** This is the only way the data model could drift into a parent+child conflict, and the new `addChild` path explicitly rejects it.
- **Promoting an owner on any invitation-based group requires the candidate to already be a member.** The rule is enforced at the `WithInvitations` layer so it applies uniformly to every group type that uses invitations (today: members groups / free groups). A dedicated `CannotPromoteNonMemberToOwnerException` replaces the silent "auto-add as member" side effect.
- **Manually adding a trainee to a training group validates that the member is not already a trainee of another training group.** On conflict the operation is rejected with a clear error. The existing automatic-assignment "move" behavior is unchanged — this rule only applies to the manual add path. Trainers are explicitly exempt: a club member can be a trainer of multiple training groups.
- **Parent/child exclusivity within a family group becomes an explicit spec requirement and is covered by a domain test.** No code change is required — the existing `UserGroup` data model already enforces uniqueness — but the invariant is now documented and locked in by a test.
- **Member-picker dialogs for adding members or owners hide people who are already in the group.** The filter is applied uniformly across family, training, and free groups. Backend API surface is unchanged; the frontend filters locally using data it already has.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `user-groups`: simplified family group creation, new family group child-membership scenarios, explicit parent/child exclusivity invariant, free group owner-promotion rule, manual training group trainee exclusivity, and member-picker filtering scenarios.

## Impact

**Backend:**
- `common/usergroup/WithInvitations.java` — promote `addOwner` logic to the interface so the invitation-based owner-promotion rule lives with invitations.
- `common/usergroup/CannotPromoteNonMemberToOwnerException.java` — new exception in the shared user-group building block.
- `members/membersgroup/domain/MembersGroup.java` — `addOwner(MemberId)` validates that the target is already a member; delegates through the new `WithInvitations` contract.
- `members/familygroup/domain/FamilyGroup.java` — drop `initialMembers` from `CreateFamilyGroup`, collapse `parents` to a single `parent`, add `addChild(MemberId)` / `removeChild(MemberId)`, `addChild` rejects members who are already a parent of the same group.
- `members/familygroup/application/FamilyGroupManagementService.java` — new `addChild` / `removeChild` ports.
- `members/familygroup/infrastructure/restapi/FamilyGroupController.java` — request DTO collapses to scalar `parentId`; new `POST /api/family-groups/{id}/children` and `DELETE /api/family-groups/{id}/children/{memberId}` endpoints; HAL-Forms affordances on the detail representation expose both add-parent and add-child actions (subject to `MEMBERS:MANAGE`).
- `members/traininggroup/application/TrainingGroupManagementService.java` — manual `addMember(TrainingGroupId, MemberId)` path loads the repository to verify the member is not a trainee of another training group and throws a new `MemberAlreadyInTrainingGroupException` on conflict. Automatic assignment paths are unchanged.
- `members/familygroup/application/MemberAlreadyInFamilyGroupException` — reused; no change.
- Domain/integration tests — cover owner promotion failure on non-member, simplified create, new add-child / remove-child paths, parent/child exclusivity, manual training-group exclusivity, and verify existing automatic-assign "move" path still works.

**Frontend:**
- Family group detail page — replace "Add parent" button with "Add member" button that opens a dialog asking for role (parent / child) and routes to the matching endpoint based on HAL link visibility.
- Family group create dialog — remove the multi-parent picker and the "initial members" picker; keep name + single parent picker.
- Member-picker components used by family, training, and free group dialogs — filter out members that are already in the target group before rendering the dropdown.
- HAL-Forms wiring — frontend already renders actions based on the presence of HAL links; the backend will emit `addChild` / `removeChild` links only when the current user has `MEMBERS:MANAGE`, so no client-side role checks are required.

**Specs:**
- `openspec/specs/user-groups/spec.md` — delta updates to `Create Family Group`, `Group Owner Management`, `Training Group Member Management`, and a new explicit invariant about parent/child exclusivity. A new requirement about member-picker filtering captures the UX rule for all group types.

**Out of scope (tracked separately):**
- `TrainingGroup.addTrainer` currently calls `UserGroup.addOwner`, which auto-adds the trainer as a regular member. This contradicts the comment "Trainers are owners but not automatically members" in `TrainingGroup.create()`. A follow-up task queue item will address this trainer-vs-trainee coupling bug without touching this proposal.
- Any change to the automatic age-based training-group reassignment path.
- Any change to non-invitation-based owner management (family groups, training groups).
