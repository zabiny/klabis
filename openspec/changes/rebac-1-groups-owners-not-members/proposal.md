# Proposal

## Why

Groups are about to become a source of delegated permissions: the owners of a group will gain permissions over its members (see `rebac-2-target-authorization` and `rebac-3-member-profile-edit-delegation`). Today an owner of a free group is also one of its members, and a trainer may at the same time be a trainee of their own training group. Once owners gain permissions "over all members", that overlap means co-owners would hold permissions over each other — including over a member who was promoted to owner without ever agreeing to it. The two roles have to be separated before any permission is attached to them.

At the same time, a member who joined a free group by accepting an invitation has no way to leave it. When membership becomes consent to give the owners permissions over you, that consent has to be revocable.

This is the first of three ordered changes (`rebac-1-…`, `rebac-2-…`, `rebac-3-…`).

## What Changes

- **BREAKING (behavior)**: An owner of a group is never at the same time a member of that group. Applies to free groups and training groups; legal guardian groups already satisfy it (guardians own, minors are members).
- Creating a free group makes the creator its owner only; the group starts with no members.
- Promoting a free group member to co-owner moves them from the member list to the owner list.
- Removing an owner (or trainer) from a group removes them from the group entirely — they do not fall back to being a member/trainee.
- Making a trainee of a training group its trainer removes them from the trainee list; automatic age-based assignment never adds a group's trainer as its trainee.
- New: a member can leave a free group at any time. Members of training groups and legal guardian groups cannot leave on their own.
- No data migration is needed (the application has no production data yet).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `user-groups`: owners and members become disjoint roles (creation, owner promotion, owner removal, trainer assignment, automatic trainee assignment); a new requirement lets a member leave a free group.

## Impact

- **Backend, `common.groups`**: group base aggregate gains the "owner is not a member" invariant and the move-on-promotion / leave-on-demotion behavior.
- **Backend, `groups.freegroup`**: creation, owner promotion and removal, new "leave group" use case and endpoint with its HAL-FORMS affordance.
- **Backend, `groups.traininggroup`**: trainer assignment and automatic age-based assignment respect the invariant.
- **Backend, `members.legalguardiangroup`**: no behavior change; invariant is already true and is now enforced by the shared base.
- **Frontend**: free group detail page — "Opustit skupinu" action for non-owner members; member/owner lists no longer show owners twice.
- **OpenAPI** (`docs/openapi/spec/groups.yaml`): new leave operation.
- **Prerequisite for** `rebac-2-target-authorization` and `rebac-3-member-profile-edit-delegation`.
