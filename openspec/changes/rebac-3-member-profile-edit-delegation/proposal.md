# Proposal

## Why

Only administrators can change a member's data, plus adult members their own. A legal guardian cannot correct their child's phone number or dietary restrictions, and there is no way for people to entrust someone else (a parent, a team leader) with maintaining their profile. Administrators end up doing routine data maintenance on behalf of families.

With the mechanism from `rebac-2-target-authorization` in place, this change introduces the first permission that is held over specific members and derived from relationships: **"Úprava údajů člena"** (`MEMBERS:EDIT_PROFILE`). It flows from being oneself (adults only), from guardianship, and from owning a free group whose members agreed to it.

This is the third of three ordered changes (`rebac-1-…` → `rebac-2-…` → **`rebac-3-…`**).

## What Changes

- New permission "Úprava údajů člena" (`MEMBERS:EDIT_PROFILE`), held only over specific members and only through relationships; it cannot be granted in the permissions dialog. Holding `MEMBERS:MANAGE` does not imply it — the two are separate ways to edit.
- Every adult member holds it over themself. A minor does not: a minor sees their own data but cannot edit it (today's rule, now expressed through the permission).
- Member data can be edited by a member administrator or by a holder of "Úprava údajů člena" over that member.
- A holder sees all of the member's data, including birth number, dietary restrictions and the minor's legal guardians, and gets a pre-filled edit form. The administrator-reserved fields (first name, last name, date of birth, gender, birth number) are shown read-only; only a member administrator can change them.
- A holder can see and edit a suspended member they hold the permission over. Viewing a birth number is audited for holders as it is for administrators.
- **Group delegation**: a group can define a set of permissions its owners gain over all of its members (owners only — never members, see `rebac-1`). An owner loses the permission over a member as soon as the member leaves, and all delegated permissions when they stop being an owner. Only per-member permissions can be delegated; administrator permissions never.
  - **Free group**: the founder chooses the delegated permissions when creating the group (opt-in, nothing preselected; today the only choice is "Úprava údajů člena"); they cannot be changed afterwards. Invitations and the group detail show what the owners will hold over members.
  - **Training group**: delegates nothing; not configurable.
  - **Legal guardian group**: always delegates "Úprava údajů člena"; not configurable. A guardian therefore edits their minor children's data and loses that when the child turns 18 and leaves the group, or when they stop being the child's guardian.
- Change history keeps "who and when" only; the relationship a permission came from is not recorded.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `members`: who may edit member data (administrator or holder of "Úprava údajů člena"), what a holder sees, read-only reserved fields in the edit form, holder access to suspended members, edit action visibility.
- `user-groups`: delegated permissions of groups — free group founder's opt-in choice at creation, immutability, display in invitations and group detail; training groups delegate nothing.
- `legal-guardians`: legal guardians edit and see their minors' data through their group; non-member guardians gain access to their minors' data.
- `non-functional-requirements`: the member detail edit template reflects the holder of "Úprava údajů člena" and shows reserved fields read-only.

## Impact

- **Backend `common`**: `Authority.MEMBERS_EDIT_PROFILE` (target MEMBER, grant form SPECIFIC only).
- **Backend `members`**: self relationship source (adults), legal guardian group relationship source, `updateMember` / `getMember` authorization and field rules, removal of `OwnProfileEditRule`, `ManagementService.getMemberAndRecordView` (suspended-member access and birth-number audit through the evaluator), member detail affordances.
- **Backend `groups`**: free group stores its delegated permissions (chosen at creation), free group relationship source, delegated permissions in group detail and invitation responses; training group delegates nothing.
- **OpenAPI**: `members.yaml` (`updateMember` and member detail field rules, `x-klabis-read-authority` on reserved request fields), `groups.yaml` (create free group with delegated permissions, delegated permissions in detail and invitations).
- **Frontend**: member detail/edit for holders (full layout, read-only reserved fields, no reserved fields in the submitted change), free group create dialog (opt-in checkbox), invitation list and group detail showing delegated permissions, permission label "Úprava údajů člena".
- **Depends on** `rebac-1-groups-owners-not-members` and `rebac-2-target-authorization`.
