## Why

GitHub issue #240 ("Pri zalození noveho clena klubu chci zvolit skupiny do kterych bude prirazen", milestone `MVP`, labels `question`, `vedení klubu`) asks for the admin to be able to, at member-registration time:
- pick a specific training group (rather than relying on automatic age-based assignment), and
- pick an existing family group OR create a new family group in-line.

### The family-group half is superseded

Family groups no longer exist. They were replaced by **legal guardian groups** (`refactor-family-group-parents`, `family-group-parents-as-users`, `legal-guardians-via-groups`; see the `user-groups` requirement `Group Types` and the `legal-guardians` capability). The registration flow already covers what the family-group part of the issue asked for, in a different shape:

- `Member Registration Flow` registers a minor together with at least one legal guardian, each guardian either chosen from the legal guardian candidates or entered as a new non-member guardian in the same step (scenarios "Admin registers a minor with an existing guardian" and "…with a new non-member guardian"). An adult is registered without guardians.
- The guardian group is created or reused as a consequence of setting the guardians (`Setting The Legal Guardians Of A Minor`, `Legal Guardian Groups`); it is not something the admin creates or picks separately.

The "register parent and child in one transaction" super-flow mentioned in the original text is therefore also covered by the guardian step (a new non-member guardian is created together with the minor).

### The training-group half is still open

`Training Group Membership Assignment` assigns a new member to the training group whose age range fits (`New member is automatically assigned to a training group`); a member belongs to at most one training group, and a manual "add member" from the training group side is rejected while the member is a trainee of another one (`Manual Training Group Trainee Exclusivity`), so correcting the automatic choice means removing the member from one group and adding them to another. The registration form (`Member Registration Page Layout`) collects personal data and, for minors, guardians — and does not offer a training group. A child who trains with an older group, or an adult who should go to a specific group, must be moved in a second step after registration. That is the gap that remains of #240.

## What Changes

- The registration form gains an optional "Tréninková skupina" choice. Empty keeps today's behaviour (age-based automatic assignment); a chosen group takes precedence over the automatic assignment.
- No change to guardians, guardian groups or any other group type.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `members`: `Member Registration Flow` and `Member Registration Page Layout` — optional training group choice in the registration form.
- `user-groups`: `Training Group Membership Assignment` — an explicit choice at registration takes precedence over the age-based assignment.

## Impact

**Affected specs:**
- `openspec/specs/members/spec.md` — registration scenarios for choosing a training group, for an empty choice (automatic assignment) and for an age-mismatching choice.
- `openspec/specs/user-groups/spec.md` — scenario "explicit training group at registration takes precedence over age-based auto-assignment".

**Affected code (backend, members + groups):** the registration command (`RegistrationService.RegisterNewMember`) gains an optional training group id. The members module already announces a registered member and the groups module assigns on it; the assignment must use the explicit group when present. The `groups.traininggroup` module is reached only through its existing event/port, not directly.

**Affected code (frontend):** the registration page (`MemberRegistrationPage`, with `RegistrationGuardianSections` for the guardian part) gains a training group dropdown, after the personal data and guardian sections.

**APIs (REST):** additive — the registration request accepts an optional training group.

**Dependencies:** none.

**Data:** none.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **MVP confirmation.** The issue notes "overit zda má patřit do MVP". With the family-group half gone, is the remaining training-group choice still wanted for the MVP, or is moving the member after registration acceptable? If the latter, close the proposal.

2. **Validation rules.** Can the admin pick any training group, or only one matching the new member's age? If the group does not match the age, is it an error, a warning, or silent acceptance? Recommend: any group allowed; warning on age mismatch; admin confirms to proceed.

3. **Empty choice.** Empty → auto-assign by age (current behaviour), not "no group". Recommend, to preserve today's default.

4. **Interaction with `Age-Based Automatic Reassignment`.** If the admin manually placed a member into a non-age-matching group, should the periodic reassignment leave them alone or move them? Recommend: keep today's semantics (the periodic reassignment applies), the choice is only initial. Note that this means a deliberate "older group" placement of a child may be undone at the next reassignment; if that is not acceptable the change needs a "manually placed" marker (a new concept). `Age-Based Automatic Reassignment` makes no distinction between manual and automatic placement: it moves (or removes) every member whose age does not fit their current group's range, so a deliberately mismatching placement is undone by the next periodic check.

5. **Permissions.** Training group manual management requires `GROUPS:TRAINING`; registration requires `MEMBERS:CREATE`. Does choosing a group at registration need `GROUPS:TRAINING` as well, or is `MEMBERS:CREATE` enough ("completing the same task")? Recommend: `MEMBERS:CREATE` only.

6. **Members brought in from ORIS.** `Import ORIS members` (`import-oris-members`) creates members through the registration code path without an admin form. Should the training group choice apply there? Recommend: no, they keep the automatic assignment.

7. **Out of scope confirmation.** Not covered: free-group assignment (invitation-based by spec), bulk registration, any guardian-group changes.
