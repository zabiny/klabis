# Design

## Context

Builds on:
- `rebac-1-groups-owners-not-members` — owners and members of a group are disjoint; a free group member can leave.
- `rebac-2-target-authorization` — `Authority` with `targetType`/`grantForms`, per-request `AuthorizationSnapshot`, `RelationshipSource` SPI, single `AuthorizationEvaluator`, `x-klabis-authority` lists, `x-klabis-target-id`, `x-klabis-read-authority`.

Today in `members`:
- `updateMember` is `MEMBERS_MANAGE` ∨ owner-visible; `OwnProfileEditRule` additionally forbids a minor's self-edit (affordance and update share the rule).
- `MemberDetailResponse` fields carry `MEMBERS_MANAGE` + owner-visible; reserved request fields carry `MEMBERS_MANAGE`.
- `ManagementService.getMemberAndRecordView` hides suspended members from non-managers and audits birth-number access only for managers and the owner.
- The frontend already renders template-absent fields as read-only in self-edit and supports `readOnly` HAL-FORMS properties for simple field types.

Legal guardian groups (`members.legalguardiangroup`) hold guardians as `UserId` owners and minors as members; minors leave the group the day after they turn 18 (`MinorAgedOutListener`). Free and training groups live in `groups`.

## Goals / Non-Goals

**Goals:**
- `MEMBERS:EDIT_PROFILE` with three relationship sources: self (adults), legal guardian group, free group delegation.
- Member read/edit rules expressed only through grants and owner-visible — no special-case code (`OwnProfileEditRule` disappears).
- Free group delegated permissions chosen once at creation and shown in invitations and group detail.

**Non-Goals:**
- "Who holds permissions over me" on the profile (follow-up).
- Delegating other permissions (event registrations, fee choice) — follow-ups once those authorities get `SPECIFIC`.
- Recording which relationship authorized a change (decided: who and when only).
- Exact-day revocation at the 18th birthday: the daily removal from the guardian group is accepted.

## Decisions

### D1. The authority

`MEMBERS_EDIT_PROFILE("MEMBERS:EDIT_PROFILE", targetType = MEMBER, grantForms = {SPECIFIC})`. Not assignable in the dialog (no `ALL`), delegatable (`SPECIFIC`). `MEMBERS_MANAGE` does not imply it; rules that accept either list both.

### D2. Relationship sources

| Source | Module | Grants |
|---|---|---|
| `SelfProfileRelationshipSource` | `members` | `EDIT_PROFILE` over own `MemberId` when the member is an adult today |
| `LegalGuardianGroupRelationshipSource` | `members.legalguardiangroup` | for each group where the user (`UserId`) is a guardian: `EDIT_PROFILE` over each minor member |
| `FreeGroupRelationshipSource` | `groups.freegroup` | for each free group the user owns: its delegated permissions over each member |

Each source issues one query for "all groups where user U is an owner" (plus members). Training groups have no source — delegating nothing is the absence of a source, not a stored empty set.

Self-edit by adults is now a relationship grant, which replaces `OwnProfileEditRule`. The minor rule is evaluated against today's date in the self source. The guardian source does not check age: a member who turned 18 stays in the group until the daily run, an accepted delay of at most one day (user decision; the self grant starts on the birthday, so both may briefly overlap).

*Alternative considered:* keep `OwnProfileEditRule` and owner-visible for self-edit. Rejected — the requirement "every user holds the permission over themself" is a relationship like any other, and keeping a parallel code path is exactly what `rebac-2` D5 forbids.

### D3. Group delegation model

```mermaid
classDiagram
    class MemberGroup~M~ {
        owners
        members
        delegatedAuthorities() Set~Authority~
    }
    class FreeGroup {
        delegatedAuthorities : Set~Authority~ "fixed at creation"
        create(name, creator, delegated)
    }
    class TrainingGroup {
        delegatedAuthorities() = ∅
    }
    class LegalGuardianGroup {
        delegatedAuthorities() = {MEMBERS_EDIT_PROFILE}
    }
    MemberGroup <|-- FreeGroup
    MemberGroup <|-- TrainingGroup
    MemberGroup <|-- LegalGuardianGroup
```

| Element | Change |
|---|---|
| `MemberGroup.delegatedAuthorities()` | **Added** — abstract; what owners hold over members |
| `FreeGroup.delegatedAuthorities` | **Added** — chosen at creation, immutable; validated: every authority must have `SPECIFIC` and `targetType = MEMBER` |
| `TrainingGroup.delegatedAuthorities()` | **Added** — constant ∅ |
| `LegalGuardianGroup.delegatedAuthorities()` | **Added** — constant `{MEMBERS_EDIT_PROFILE}` |
| `FreeGroup.create` | **Changed** — takes the delegated set (may be empty) |
| `FreeGroupMemento` / group table | **Changed** — stores the delegated set for free groups (no migration needed) |

Validation lives in the aggregate (`InvalidDelegatedAuthorityException` when an `{ALL}`-only authority is supplied), so an API client cannot delegate `MEMBERS_MANAGE` by bypassing the form.

### D4. Member API rules

| Element | Rule |
|---|---|
| `getMember` | `x-klabis-authority: MEMBERS_READ` unchanged; suspended member visible with `[MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` on the target |
| `updateMember` | `x-klabis-authority: [MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]`, `x-klabis-target-id: MEMBER`; **no** `x-klabis-owner-visible` |
| Detail fields today `MEMBERS_MANAGE` + owner-visible | `[MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` + owner-visible (a minor still reads their own data) |
| Reserved request fields (firstName, lastName, dateOfBirth, gender, birthNumber) | write `MEMBERS_MANAGE`; `x-klabis-read-authority: [MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` → read-only in the template |
| Legal guardians section of a minor | visible with `[MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` + owner-visible; "Upravit zástupce" stays `MEMBERS_MANAGE` |

`ManagementService.getMemberAndRecordView` receives the decision from the evaluator instead of a `canManageMembers` flag: "may see suspended" = `has(MANAGE) ∨ has(EDIT_PROFILE, target)`; "birth number visible (audit)" = the evaluator's field rule for `birthNumber` on this target, so the audit can never disagree with what is serialized.

### D5. Frontend

- Member edit form: reserved fields come as `readOnly` template properties; `HalFormsForm` already renders `ReadOnlyField` for simple types — `gender` (select) and `dateOfBirth` (date) are verified and extended if needed. Read-only fields are excluded from the PATCH body (the backend rejects unauthorized fields).
- Member detail: no role detection; the layout follows the template as today, and the "Členské příspěvky" button stays bound to its own link (owner-visible), so a guardian does not see it.
- Free group create dialog: checkbox list built from the HAL-FORMS options of the delegated-permissions property (today one item, "Úprava údajů člena"), unchecked by default.
- Pending invitations and free group detail: a line listing the delegated permissions ("Vlastníci skupiny (i budoucí) mohou: Úprava údajů člena" / "Vlastníci skupiny nad členy nezískávají žádná oprávnění").
- Labels: `MEMBERS:EDIT_PROFILE` → "Úprava údajů člena".

### D6. API changes in `groups.yaml`

- `createGroup` request: optional `delegatedAuthorities` (array of authority, options = authorities with `SPECIFIC` and `targetType = MEMBER`), default empty.
- Free group detail and pending invitation responses: `delegatedAuthorities` (read-only).
- `updateGroup` does not accept it.

## Glossary

| Term | Meaning |
|---|---|
| Úprava údajů člena (`MEMBERS:EDIT_PROFILE`) | Permission to see all data of a specific member and change the non-reserved ones |
| Administrator-reserved fields | First name, last name, date of birth, gender, birth number — changeable only with `MEMBERS:MANAGE` |
| Delegated permissions | The permissions a group gives its owners over its members |
| Holder | A user who holds a permission over a given member |

## Risks / Trade-offs

- [Free group owners gain access to sensitive data (birth number, dietary restrictions) of consenting members] → Opt-in at creation, explicit text in the invitation including "future owners", the member can leave at any time (`rebac-1`), birth-number access is audited.
- [Adding a co-owner extends access without asking members] → Accepted (user decision S2a); the invitation text states that future owners hold the permission.
- [Pre-existing: a non-admin who changes nationality away from or to Czech touches the reserved birth number] → Already true for adult self-edit today; this change does not alter it. Tracked as an open question below.
- [Self and guardian grants overlap for up to a day after the 18th birthday] → Accepted (user decision G5).

## Open Questions

- The nationality ↔ birth number interaction for non-admin editors (clearing or requiring a birth number the editor may not change) predates this change; resolve in a separate fix without changing this design.
