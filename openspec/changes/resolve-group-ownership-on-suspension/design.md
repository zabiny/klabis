# Design

## Context

See proposal.md, section Why, for the motivation. Current state (verified in code):

- `ManagementService.suspendMember` asks every `MemberOwnedGroupsPort` (one adapter per group module: training, free, legal guardian) for groups where the member is the last owner/trainer/guardian, checks the finance snapshot, and throws `SuspensionBlockedException`. `MembersExceptionHandler` turns it into a plain-JSON 409 (`SuspensionBlockedWarning` with `debt` and `groups.affectedGroups[{groupId, groupName, groupType}]`) - not HAL, no links.
- `suspendMember` requires `MEMBERS_MANAGE` (`x-klabis-authority`). `MEMBERS:UPDATE`, used by the members spec, is not in the `Authority` enum - `MEMBERS:MANAGE` is the only matching authority. The user-stated "MEMBERS:MANAGE" is therefore right and the spec text was stale.
- Training groups: `addTrainer`, `deleteTrainingGroup` carry `x-klabis-authority: GROUPS_TRAINING`; `getTrainingGroup` is allowed for GROUPS:TRAINING holders and group members (imperative), and its self link already offers `addTrainer` (remote member options) and `deleteTrainingGroup`. A trainer need not be a trainee.
- Free groups: `FreeGroupController` operations have no `x-klabis-authority`; ownership is enforced inside `FreeGroup` (`requireOwner`) from `@ActingMember`. `getGroup` is allowed for owners and members only. The self link offers `addGroupOwner`/`deleteGroup` only to owners. Owner promotion requires the promoted person to be a current member (`CannotPromoteNonMemberToOwnerException`). The existing group detail page already renders the `addGroupOwner` template (member picker). Gap: `FreeGroup.invite` has no owner check (no test covers it), so a non-owner member can invite through the API, contrary to the user-groups scenario "Only group owner can invite members".
- Authorization building blocks (ADR-010): `x-klabis-authority` lists are OR-ed; an authority held over everything passes regardless of the target, a SPECIFIC one only over the `x-klabis-target-id` target. Precedent for a relationship-only authority: `EVENTS_EDIT` from `EventCoordinatorRelationshipSource`. The authorization snapshot is built per request.
- Legal guardian groups ("family groups" in older specs/the old spec text; ADR-008): cannot be created or deleted by hand (legal-guardians spec). Guardians are replaced as a whole through `setLegalGuardianGroupGuardians` (`MEMBERS_MANAGE`), UI on the "Zákonní zástupci" page. The current dialog and its unit test offer "Rozpustit skupinu" for them - contradicts the specs.
- Dependency direction is groups -> members (ports in members, adapters in groups); the members module must not reference group APIs.
- Dialog consumers: `MemberDetailPage` and `MembersPage` via `useSuspendMemberAction`; the 409 is delivered through `HalFormDisplay.onSubmitError`.

## Goals / Non-Goals

**Goals:**
- Make "Určit nástupce" and "Rozpustit skupinu" work from the dialog, offered only where the caller is allowed to act.
- Keep the members module free of knowledge about group-module APIs.
- Reuse existing group templates, forms and pickers.

**Non-Goals:**
- Dissolving training groups or legal guardian groups.
- Changing who may perform the other free-group operations (rename, invite, remove owner/member, cancel invitation) - they stay owner-only; only the mechanism that enforces it moves (D1).
- Notifying members of a dissolved group (existing deletion behavior is unchanged).
- Per-group audit of who dissolved what (no such audit exists for owner deletions either).

## Decisions

### D1: Free group authorization - relationship authority GROUPS:OWNER

Today the owner check lives inside `FreeGroup` (`requireOwner(actingMember)`), invisible to the method guard. `x-klabis-authority: MEMBERS_MANAGE` alone on `addGroupOwner`/`deleteGroup` would lock out ordinary owners, and `x-klabis-owner-visible` compares a path *member* id with the caller, not a group-owner set.

Options:

| Option | Description | Verdict |
|---|---|---|
| A | Separate admin-only operations (new endpoints with `x-klabis-authority: MEMBERS_MANAGE`) | Two endpoints and two templates for one action; frontend must choose between them. Rejected. |
| B | Keep the owner check in the aggregate; the service additionally allows `has(MEMBERS_MANAGE)`; the postprocessor gates affordances with the same predicate | Rule duplicated in service and postprocessor, actor-free aggregate variants just to bypass `requireOwner`, `@ActingUser` switch for callers without a profile. Rejected (user decision 17. 11. 2026). |
| C | Group ownership is a relationship grant: SPECIFIC-only authority `GROUPS:OWNER` over target type GROUP, derived by `FreeGroupRelationshipSource`; owner-only operations declare `x-klabis-authority`, the admin override adds `MEMBERS_MANAGE` to the list | **Chosen** (user decision 17. 11. 2026) - declarative, affordances filtered by `klabisAfford`, authorization leaves the aggregate. All owner-only operations migrate at once so `FreeGroup` does not mix two mechanisms. |

Consequences of C:
- `Authority.GROUPS_OWNER("GROUPS:OWNER", TargetType.GROUP, GrantForm.SPECIFIC)`, new `TargetType.GROUP` and `TargetRef.group(uuid)`. It is never granted by hand (not in `grantableOverAll`, so the permissions UI does not offer it) and never delegated (`delegatedAuthorities` unchanged).
- `FreeGroupRelationshipSource` adds `GROUPS_OWNER` over every group the user owns, beside the existing delegated grants. The snapshot is per request, so promotion or removal of an owner takes effect on the next request.
- Operations (all on the `id` path parameter, `x-klabis-target-id: GROUP`):

  | Operation | Authorization |
  |---|---|
  | `updateGroup`, `removeGroupOwner`, `inviteMember`, `cancelInvitation` | `x-klabis-authority: GROUPS_OWNER` |
  | `addGroupOwner`, `deleteGroup` | `x-klabis-authority: [MEMBERS_MANAGE, GROUPS_OWNER]` |
  | `removeGroupMember` | Imperative in the service: allowed when the member removes themselves or the caller `has(GROUPS_OWNER, group)`. Not declarative: owner-visible needs a MEMBER target and an operation has a single target parameter. |
  | `getGroup` | Imperative: owner (`has(GROUPS_OWNER, group)`), member, or `has(MEMBERS_MANAGE)`. Switches to `@ActingUser CurrentUserData` (`memberId` nullable) so the dialog can load it for an admin without a member profile. |
  | `acceptInvitation`, `rejectInvitation` | Unchanged - the invitee check is an identity rule, stays in the aggregate. |

- Domain: authorization leaves `FreeGroup`. `requireOwner` and the actor parameters of `rename`, `addOwner`, `removeOwner`, `removeMember` and `cancelInvitation` go away. `FreeGroup` overrides `addOwner(memberId)` with the "must be a current member" rule. `invite(invitedBy, target)` keeps `invitedBy` as recorded data. `cancelInvitation(invitationId, reason)` loses the optional actor; the system cancellation in `MemberSuspendedListener` calls the same method. `GroupOwnershipRequiredException` is removed if unused; a 403 then comes from the method guard.
- Controller: `@ActingMember` is dropped where it only fed the owner check (`updateGroup`, `deleteGroup`, `addGroupOwner`, `removeGroupOwner`, `cancelInvitation`); it stays for `inviteMember` (`invitedBy`), `removeGroupMember` (self rule) and accept/reject.
- Affordances: owner templates are filtered by `klabisAfford` with the group target; the manual `isOwner` branch in `FreeGroupDetailsPostprocessor` and the cancel-invitation gating in `FreeGroupItemPostprocessors` go away. The "leave group" affordance for plain members stays imperative. A manager automatically gets exactly `addGroupOwner` and `deleteGroup`.
- `getGroup` for a manager: name, owners, members; no pending invitations, no invitation management - needed so the dialog can discover templates and the member picker. MEMBERS:MANAGE already reads every member's data, so no new data class is exposed.
- Side effect, accepted by the user decision: a MEMBERS:MANAGE holder may add an owner to / dissolve any free group at any time, not only inside a suspension flow. It is the same trust level as managing all members.
- The `invite` gap (Context) is closed by the guard.

### D2: What happens on dissolution

- Free group: existing deletion semantics (group, memberships removed; pending invitations cancelled). Confirmation dialog in the UI before the call.
- Legal guardian group: never dissolved (minors would be left without a guardian group). Two resolutions: set another guardian ("Určit nástupce" navigates to the group's page in "Zákonní zástupci", existing guardian form, no second form embedded) or suspend the whole group (D6).
- Training group: never dissolved from here (spec: successor required).

### D3: Who can be the successor, how it is picked

| Group type | Successor | Picker |
|---|---|---|
| Training | any active member (trainers need not be trainees); a trainee promoted to trainer leaves the trainee list (existing rule) | `addTrainer` template, existing remote member options |
| Free | a current member of that group (existing rule) | `addGroupOwner` template, same picker as the existing group detail page |
| Legal guardian | another guardian (member or new non-member), or no successor at all when the whole group is suspended (D6) | existing guardian form on the group page |

"Group without other members" (free): `addGroupOwner` can never succeed, so the dialog hides "Určit nástupce" and offers only dissolve. The dialog learns it from the group resource (`members` empty), not from a separate flag. Picking the member being suspended again is harmless: the group stays blocked and the final resubmission reports it.

### D4: Dialog behavior after resolution

- Each row has local state `unresolved | resolved`; an action's success marks the row resolved (checkmark, buttons hidden). No refetch of the blockers - cheap and the final resubmission is authoritative.
- A row resolved by "Pozastavit celou skupinu" differs from the other resolutions: nothing is executed when it is chosen; it only adds the group to the stored request (D6) and is executed atomically by the final resubmission. It can be undone ("Zrušit") until then.
- Multiple groups: rows are independent.
- One dialog for all blockers (#418): the warning dialog lists every blocking reason of the 409 at once - the negative balance (amount and "Otevřít účet"), the affected groups and the dependents' blockers (D6). `NegativeBalanceSuspensionDialog` is merged into `SuspensionWarningDialog`; a debt-only 409 opens the same dialog with only the balance section. The suspension form is closed whenever the dialog opens (the hook keeps the request). Today both dialogs open at once and the debt dialog covers the group warning. Showing them one after another was rejected: the admin would not see the full picture before acting on the groups.
- A negative balance cannot be resolved in the dialog, so while it is reported "Ukončit členství" is absent even when all rows are resolved; the dialog says the balance has to be settled first. The group actions stay available - their changes persist and the admin finishes the suspension after the deposit.
- When all rows are resolved and no negative balance is reported the dialog shows a primary "Ukončit členství" button that resubmits the original suspension request (reason, note kept by `useSuspendMemberAction`); while any row is unresolved it is absent.
- Resubmission is explicit, not automatic: it is a destructive step and the user should see the group changes happened first. Automatic resubmit rejected (surprising side effect, harder to test). On a repeated 409 the dialog is rebuilt from the fresh response; on success the same refresh as the normal suspension happens (detail page / list).
- `HalFormDisplay.onSubmitError` currently gets only the error; it must also pass the submitted values so the hook can keep them (small change, no behavior change for other users).

### D5: Discovery - HAL affordances instead of buttons hardcoded by type

The 409 body (plain JSON, not HAL) cannot carry templates, and assembling group affordances in the members module would invert the module dependency. Instead each affected group carries a link to its own resource:

```mermaid
sequenceDiagram
    participant UI as Dialog
    participant M as Members API
    participant G as Group resource
    UI->>M: suspend member
    M-->>UI: 409 affected groups with groupLink
    UI->>G: GET groupLink as HAL-FORMS
    alt allowed
        G-->>UI: templates addTrainer / addGroupOwner / deleteGroup
        UI->>G: submit chosen template
        G-->>UI: 204
    else forbidden
        G-->>UI: 403
        UI->>UI: show contact info
    end
```

- The group-module adapters implementing `MemberOwnedGroupsPort` build the link with their own `linkTo(methodOn(...))` and return it in `OwnedGroup` (new field); `MembersExceptionHandler` copies it to `AffectedGroup.groupLink`.
- The UI renders per row: "Určit nástupce" iff the group resource offers `addTrainer` or `addGroupOwner` (free group: and has other members); "Rozpustit skupinu" iff it offers `deleteGroup` (only free groups ever do, since the training `deleteTrainingGroup` template is deliberately ignored by the dialog); 403/missing templates -> the row shows the contact text. For `LEGAL_GUARDIAN` rows the UI shows, based on the type, the navigation action and "Pozastavit celou skupinu"; the group resource (`getLegalGuardianGroup`, MEMBERS:MANAGE) supplies the minors' names for the confirmation.
- A GROUPS:TRAINING-only rule needs no code of its own: `getTrainingGroup` already requires GROUPS:TRAINING or membership and `addTrainer` is guarded by `x-klabis-authority: GROUPS_TRAINING`. A MEMBERS:MANAGE-only admin gets 403 -> contact text. "Whom to contact" is a fixed text ("správce tréninkových skupin"), not a list of people (see Open Questions).

### D6: Suspending the whole legal guardian group

**What it means.** A legal guardian group has two kinds of people: guardians (owners) and minors (members, only minors). The blocker fires only when the suspended member is the *sole* guardian of a group that has minors - a group with another guardian never blocks. So "whole group" = the suspended guardian + all minors of the group; there are no other guardians to include. The group (and its guardian link) is not deleted; it simply consists of suspended people.

**One operation, not one by one.** The cascade is a single transactional operation inside `ManagementService.suspendMember`: all-or-nothing. One-by-one (admin suspends each minor separately) was rejected: it leaves the guardian suspended with active minors without a guardian between steps, and each minor would first hit its own checks in an arbitrary order.

**Atomicity and other blockers.** The service collects the member set (guardian + minors of the requested groups) and runs the existing checks for *each* member before changing anything: negative balance, and last-owner status of other groups (excluding the cascaded legal guardian groups themselves). If any member is blocked nothing is suspended and the 409 lists every blocker, attributed to its member. The same transaction suspends all members, so a failure midway rolls back everyone. Events (`MemberSuspendedEvent`) and user-account suspension are executed per member as today.

**Reason and note.** The same reason and note apply to all suspended members (one form, one decision); `suspendedBy` is the acting admin. Alternative of a different reason per minor rejected as needless.

**Blockers of dependents are not resolvable in the dialog.** They concern other members (a minor's debt, a minor owning a free group). The dialog only reports them; the admin undoes the cascade for that group or fixes the cause elsewhere, then retries. A minor who is the last owner of a free group could technically be handled by the same row actions, but this is rare and deferred (see Open Questions).

**Confirmation.** Choosing "Pozastavit celou skupinu" first shows a confirmation listing the guardian and the minors (names from the group resource). Confirming marks the row; the final "Ukončit členství" executes. No second confirmation at execution.

**Authorization.** The existing `suspendMember` authority `MEMBERS_MANAGE` (held over everything) covers all members, so no new authority is needed. The service still asks `has(MEMBERS_MANAGE, member(minorId))` per cascaded member (ADR-010: use target form, a future targeted grant keeps working).

**API shape.**

| Option | Verdict |
|---|---|
| Optional list `cascadeGroupIds` (legal guardian group ids) on `SuspendMembershipRequest` of the existing `suspendMember` | **Chosen**. The dialog's final resubmission already replays this request; per-group choice is exact; server validates that each id is a legal guardian group the member is sole guardian of (else 400). Absent/empty = today's behavior. |
| Boolean flag | Coarser (all groups or none); the list costs nothing extra. |
| New endpoint (suspend legal guardian group) | A second route for the same business operation with its own blockers/409 body; rejected. |

**Invariant (alternative handling).** Today nothing prevents a suspended member from staying a guardian: the guardian picker excludes suspended members, but existing guardian links are untouched, and no listener reacts to `MemberSuspendedEvent` in the legal guardian module. For a group with other guardians the suspended person would stay listed as guardian (and, via `LegalGuardianGroupRelationshipSource`, keep delegated edit rights over the minors, if the account were still usable) - this exists today independently of this change. Recommendation: a listener in `members.legalguardiangroup` reacts to `MemberSuspendedEvent` and removes the member from the guardians of every group that still has other guardians (the existing `changeGuardians` handles renaming and merging). A group whose sole guardian is suspended is left alone (its minors are suspended together, D6, or a successor was set before). Resume does not restore the link. If the user does not choose the cascade and sets a successor via the group page, the old guardian is typically replaced there; if they keep both, the listener removes the suspended one. Alternative - a domain invariant refusing to save a group with a suspended guardian - was rejected: it would break resumption and past groups.

## Domain model changes

```mermaid
classDiagram
    class FreeGroup {
        +rename(name)
        +addOwner(memberId)
        +removeOwner(memberId)
        +removeMember(memberId)
        +invite(invitedBy, target)
        +cancelInvitation(invitationId, reason)
    }
    class FreeGroupRelationshipSource {
        +grantsOf(userId)
    }
    class OwnedGroup {
        +groupId
        +groupName
        +groupType
        +groupLink
    }
    FreeGroupRelationshipSource ..> Authority : GROUPS_OWNER over GROUP
    FreeGroupManagementService ..> FreeGroup
    FreeGroupManagementService ..> AuthorizationEvaluator : removeMember only
```

| Element | Change |
|---|---|
| `TargetType`, `TargetRef`, `Authority` | New `TargetType.GROUP`, `TargetRef.group(uuid)`, `Authority.GROUPS_OWNER` (SPECIFIC only). |
| `FreeGroupRelationshipSource` | Also returns `GROUPS_OWNER` over each owned group. |
| `FreeGroup` | No authorization: `requireOwner` and actor parameters removed; `addOwner(memberId)` override rejects a non-member (`CannotPromoteNonMemberToOwnerException`). |
| `FreeGroupManagementService` / `FreeGroupManagementPort` | Actor parameters removed except `inviteMember` (`invitedBy`) and `removeMember` (self-or-owner check via `AuthorizationEvaluator`). |
| `OwnedGroup` (members root, public) | Add `groupLink` (URI of the group resource). |
| `ManagementService.suspendMember` | Accepts the requested legal guardian group ids; builds the member set; checks every member; suspends all in one transaction. |
| `SuspensionBlockedException` | Carries blockers per member (the requested member plus dependents). |
| Legal guardian module listener | On `MemberSuspendedEvent` removes the member from guardians of groups with other guardians. |
| Aggregates `TrainingGroup`, `LegalGuardianGroup` | No new rules; `LegalGuardianGroup.changeGuardians` reused. |

## REST API (spec-first: `docs/openapi/spec`, then `openapiBundle` + `npm run openapi`)

**Members (`members.yaml`)**

- `suspendMember` request `SuspendMembershipRequest`: new optional `cascadeGroupIds` (array of uuid; ids of legal guardian groups whose minors are suspended together with the member). The HAL-FORMS template for `suspendMember` stays as is for the form (the property is set by the dialog, hidden in the form via `x-klabis-halforms-access: NONE`); 400 when an id is not a group of which the member is the sole guardian. Description of `suspendMember` updated (atomic, same reason/note).
- 409 `SuspensionBlockedWarning`: new optional `dependentMembers` array of `{memberId, memberName, debt, groups}` - blockers of the cascaded members; `debt`/`groups` keep their shape. Nothing was suspended when it is present.
- `suspendMember` 409 body `SuspensionBlockedWarning.groups.affectedGroups[]`: new required property `groupLink` (string, uri; absolute URL of `GET /api/groups/{id}`, `/api/training-groups/{id}` or `/api/legal-guardian-groups/{id}` by `groupType`). No new endpoints, no HAL link/template names; update the `AffectedGroup`/`LastOwnerWarning` descriptions (legal guardian groups are never dissolved).

**Groups (`groups.yaml`)**, all existing endpoints:

| Operation | Change |
|---|---|
| `GET /api/groups/{id}` (`getGroup`) | Also allowed for callers with MEMBERS:MANAGE (documented in description; no `x-klabis-authority`, imperative). `x-spring-provide-args` `@ActingUser CurrentUserData` replaces `@ActingMember`. Self-link templates for a manager: `addGroupOwner`, `deleteGroup` only. |
| `PATCH /api/groups/{id}` (`updateGroup`), `DELETE /api/groups/{id}/owners/{memberId}` (`removeGroupOwner`), `DELETE /api/groups/{id}/invitations/{invitationId}` (`cancelInvitation`) | `x-klabis-authority: GROUPS_OWNER`; `x-spring-provide-args` acting member removed. |
| `POST /api/groups/{id}/invitations` (`inviteMember`) | `x-klabis-authority: GROUPS_OWNER`; acting member kept (`invitedBy`). |
| `POST /api/groups/{id}/owners` (`addGroupOwner`), `DELETE /api/groups/{id}` (`deleteGroup`) | `x-klabis-authority: [MEMBERS_MANAGE, GROUPS_OWNER]`; acting member removed. Request `{memberId}` unchanged; 400 when not a current member. |
| `DELETE /api/groups/{id}/members/{memberId}` (`removeGroupMember`), accept/reject invitation | Unchanged contract (imperative check, acting member kept). |
| Training group operations | Unchanged (`addTrainer`, `getTrainingGroup`). |
| `setLegalGuardianGroupGuardians` | Unchanged. |

The `id` path parameter of the owner-only operations gets `x-klabis-target-id: GROUP` (a shared parameter component). Rewrite the module header comment of `groups.yaml`: pattern b (authorization inside the aggregate) is replaced by the GROUPS:OWNER relationship authority; `removeGroupMember` and `getGroup` stay imperative and say why.

## Risks / Trade-offs

- [Authorization of the existing free-group operations moves out of the aggregate] -> controller regression tests per operation (owner, member, outsider, manager) are written before the change and must stay green.
- [GROUPS:OWNER must never be grantable by hand] -> SPECIFIC-only; a test asserts it is not in the grantable set.
- [MEMBERS:MANAGE gains standing power over free groups] -> explicit user decision; documented in ADR.
- [Group changed by someone else between GET and submit] -> the action's normal error is shown in the row (toast); final resubmission is authoritative.
- [The dialog needs one extra request per row] -> at most a handful of groups; no batching (design principles).
- [Suspension rows for legal guardian groups send the user away from the dialog when choosing a successor] -> the dialog stays reachable by repeating the suspension; acceptable for a rare case.
- [Cascade suspends minors, which is hard to reverse in bulk] -> explicit confirmation listing every person; resume is per member.
- [Long transaction for a large family] -> a group has a handful of minors; no concern at club size.
- [Removing a suspended guardian changes group names and may merge groups] -> reuses the existing guardian-change behavior; resume does not restore (documented).

## Migration Plan

No data migration (GROUPS:OWNER is derived, never stored). API change is additive (`groupLink`, wider access) except that a non-owner can no longer invite (closes the gap, matches the spec). Deploy backend and frontend together; the old frontend ignores the new field. Rollback = redeploy previous version.

## Open Questions

1. **Authority name.** Recommendation (and applied): MEMBERS:MANAGE. MEMBERS:UPDATE does not exist (`Authority` enum, `suspendMember`); the members spec is corrected. Two scenarios in `non-functional-requirements` still say MEMBERS:UPDATE - clean up separately?
2. **Legal guardian groups.** ANSWERED by the user: never dissolved; offer "Pozastavit celou skupinu" (D6). "Určit nástupce" (link to the group page) is kept beside it - confirm that both are wanted.
3. **Contact info for training groups.** Recommendation: fixed text naming the role (správce tréninkových skupin). Alternative: list the users holding GROUPS:TRAINING (needs a new query across modules; not recommended at club size).
4. **Manager view of free groups.** Recommendation: MEMBERS:MANAGE may open any free group's detail (read-only plus add-owner/delete). Alternative: keep the detail closed and give the dialog the member list in the 409 (needs member names in the groups adapter and a new owner-candidates option source). Confirm the broader read access is acceptable.
5. **Free group picker.** The spec says only current group members are candidates (and owners hidden); today's picker is the generic member list and a wrong choice is rejected by the server. Reuse as is (recommended) or restrict options on the server in this change?
6. **Resolving a dependent minor's own blocker in the dialog** (e.g. minor is sole owner of a free group): recommendation - report only, not resolvable from the dialog (rare).
7. **Invariant for suspended guardians.** Recommendation (planned in slice 3): on suspension remove the member from guardian lists of groups with other guardians; resume does not restore. Alternatives: do nothing in this change (pre-existing gap), or restore on resume. Confirm scope.
8. **Cascade to groups where the guardian is not the sole guardian.** Recommendation: not offered (such groups never block; the minors keep their other guardian). Confirm.
9. **Cascade when the minor already is suspended.** Recommendation: skipped silently (already suspended minors are not an error).
