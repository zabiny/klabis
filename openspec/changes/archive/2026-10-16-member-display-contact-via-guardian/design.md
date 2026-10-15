# Design

## Context

Member detail is assembled in `MemberController.getMember` (already loads the guardian group for `missingData`); the list in `listMembers` via `MemberSummaryConverter`, with a per-page batch postprocessor for links. Guardian contacts are available through `LegalGuardianGroupPort` / `GuardianContactResolver` (members and non-member guardians alike). `email` and `phone` on the responses are raw own values and feed the HAL-FORMS edit template prefill. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One shared rule for the effective contact of a member, used by detail and list.
- Guardian marker visible only to MEMBERS:MANAGE.

**Non-Goals:**
- Changing completeness rules, notifications or any other consumer of member contacts (events do not show member contacts today).
- Caching or batch optimisation of guardian lookups (see CLAUDE.md design principles).

## Decisions

### D1: Separate derived field `displayContact`, raw `email`/`phone` unchanged

Substituting into `email`/`phone` would prefill the edit form with a guardian's contact and an admin saving it would copy that contact into the minor's own record. A separate field keeps edit semantics intact.

### D2: Resolution rule

For a minor, each of e-mail and telephone independently: own value, else the value of the first guardian (ordered by last name, then first name) who has it. Adults and members with their own value never consult guardians. Guardian lookup happens only for minors with at least one missing own value.

### D3: Marker visibility via field-level authority

`fromGuardian` and `guardianName` carry `x-klabis-authority: MEMBERS_MANAGE`, so existing field-security hides them from other users; `value` is visible to everyone who can see the member detail. In the list the whole contact column is MEMBERS:MANAGE-only as before (`displayContact` carries the authority on the summary schema).

### D4: Resolution lives in the members application layer

A small application-level component (taking a member, returning the effective contact) reuses `guardiansOf` + `GuardianContactResolver`; the controller calls it for detail, and the list does the same per row. N+1 for the list is accepted (200-300 members, page-sized, admin-only column).

## API

`MemberDetailsResponse` and `MemberSummaryResponse` gain:

```
displayContact:
  email: { value: string|null, fromGuardian: boolean, guardianName: string|null }
  phone: { value: string|null, fromGuardian: boolean, guardianName: string|null }
```

`fromGuardian`/`guardianName` are present only for MEMBERS:MANAGE. `email`, `phone` unchanged. No new links or affordances.

## Risks / Trade-offs

- [Guardian contact exposed to all users who see the minor] → intended by the requirement; the guardian's name is not exposed to non-admins.
- [List shows contact of guardian whose data changed after] → computed at request time, no staleness.
- [Per-row guardian lookups in list] → accepted per load assumptions; revisit only if proven slow.
