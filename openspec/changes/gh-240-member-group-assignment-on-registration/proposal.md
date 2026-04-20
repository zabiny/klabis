## Why

GitHub issue #240 ("Pri zalození noveho clena klubu chci zvolit skupiny do kterych bude prirazen", milestone `MVP`, labels `question`, `vedení klubu`) asks for the admin to be able to, at member-registration time:
- pick a specific training group (rather than relying on automatic age-based assignment), and
- pick an existing family group OR create a new family group in-line.

The issue references #119 (group-permissions topic) and notes the feature extends #3 (Member Registration Flow, COMPLETED) and #5 (Guest Member Creation from ORIS, TODO).

Today the relevant specs behave as follows:

- `members` spec `Member Registration Flow` collects personal info only (name, birthdate, nationality, gender, address). It does not collect group assignments. On registration completion, the `user-groups` spec `Training Group Membership Assignment` automatically assigns the new member to the training group whose age range matches (*"New member is automatically assigned to a training group"*).
- There is no way to assign a family group at registration time. Family groups are created from the members list page (`Create Family Group` in the `user-groups` spec) by a separate action, after the member already exists.
- The automatic training-group assignment cannot be overridden at registration time.

This creates friction for common cases:
- A 12-year-old registering should usually land in the under-13 training group. The auto-assignment works. But a child who trains with the older group (e.g., talented, or an older sibling to accompany) has to be manually moved after registration — a separate action.
- A new child member who belongs to an existing family (parent is already a member) should be added to that family group during registration — currently requires: register child, navigate to family groups, add child to parent's family group.
- A new child member whose parent is ALSO newly registering requires: register parent, register child, create family group with parent, add child. Three steps that could be one.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `members`: extend `Member Registration Flow` with optional training-group and family-group selection at registration time.
- `user-groups`: extend `Training Group Membership Assignment` to cover the manual override at registration. Extend `Create Family Group` / `Add and Remove Child Members of a Family Group` for the in-line "pick existing or create new family group" flow during registration.

## Impact

**Affected specs:**
- `openspec/specs/members/spec.md` — `Member Registration Flow` gains scenarios for optional training-group selection, optional family-group selection (existing group pick), and optional new-family-group creation with the new member as child (and a choice of existing member as parent, or the creating admin picks the parent).
- `openspec/specs/user-groups/spec.md` — `Training Group Membership Assignment` gains a scenario "when admin provides explicit training group at registration, it takes precedence over age-based auto-assignment". `Create Family Group` / `Add and Remove Child Members of a Family Group` gain scenarios for the registration-time variant.

**Affected code (backend, members + user-groups modules):** the registration command gains optional `trainingGroupId` and either `familyGroupId` OR `createFamilyGroupWithParent` parameters. Cross-module orchestration: the members module emits a "member registered" event that the user-groups module already listens to for auto-assignment; the flow becomes "if explicit training group provided, use it; else auto-assign".

**Affected code (frontend):** the registration form gains two new optional sections:
- "Tréninková skupina (volitelné)" — a dropdown of existing training groups. Empty = auto-assign by age.
- "Rodinná skupina (volitelné)" — a combined picker (existing family group) + a "Založit novou" option that reveals additional fields to pick a parent member.

**APIs (REST):** additive — the registration request body accepts new optional fields.

**Dependencies:** none.

**Data:** none.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **MVP confirmation.** The issue notes "overit zda má patřit do MVP". If it belongs to a later milestone, we should still land the proposal but defer tasks. Confirm milestone.

2. **Training-group override: validation rules.**
   - Can the admin pick *any* training group, or only one matching the new member's age?
   - If the admin picks a group that doesn't match the member's age, is it an error, a warning, or silent acceptance?

   Recommend: any group allowed; warning if age-mismatch; admin can confirm to proceed. Confirm.

3. **Training group = empty means what?**
   - Option A: empty → auto-assign by age (current behavior).
   - Option B: empty → no training group assigned (member sits outside any training group until manually moved).

   Recommend: Option A, to preserve today's behavior as default.

4. **Family group creation during registration: who is the parent?**
   - Option A: parent must be an existing member (picked from a member-picker).
   - Option B: can be either an existing member OR "register this new member as the parent" (requires a role flag on the registering member — child vs. adult).
   - Option C: the admin is the parent (rarely correct).

   Recommend: Option A, with a follow-up proposal to cover "register parent + create family + register children" as a single super-flow if needed.

5. **Family group: existing vs. new.** If the admin picks an existing family group, the new member is added as a CHILD (not parent). This follows the rule that children are added to existing groups, parents are designated at creation. Confirm.

6. **ORIS-sourced registrations (#5).** This proposal refers to `Member Registration Flow`, which currently is admin-driven. When a hostujici / prestupujici member is imported from ORIS (#5, TODO), should group selection apply there too? Recommend: yes, same fields on the ORIS-import registration command. Confirm.

7. **Interaction with Training Group automatic reassignment.** `Age-Based Automatic Reassignment` periodically moves members across training groups as they age. If the admin manually placed a member into a non-age-matching group, should the periodic reassignment leave them alone or move them?
   - Option A: never automatically move a manually-placed member (requires a new "manually placed" flag).
   - Option B: always run the periodic reassignment — the manual choice is only initial.

   Recommend: Option B, to keep today's semantics. The admin can re-intervene if needed.

8. **Permissions.** Who can specify group assignments at registration? Is it gated by `MEMBERS:CREATE` alone, or does it additionally require `GROUPS:TRAINING` (for training-group override) and `MEMBERS:MANAGE` (for family-group creation)? Today:
   - training-group manual management requires `GROUPS:TRAINING`.
   - family-group creation requires `MEMBERS:MANAGE`.

   Recommend: at registration, allow any admin with `MEMBERS:CREATE` to specify both, with the rationale that they are completing the same task. Confirm.

9. **UI ordering.** Should the group section appear before or after the personal-info section in the registration form? Recommend: after, so the required fields come first.

10. **Out of scope confirmation.** This proposal does NOT cover:
    - free-group assignment at registration (free groups are invitation-based by spec),
    - bulk member registration,
    - a new family-group creation flow where *two* new members are created as parent + child in a single transaction.

    Confirm.
