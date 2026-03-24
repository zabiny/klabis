# Birth Number Required for Czech Nationals

## Problem

Birth number (rodne cislo) is currently optional for Czech nationals. It should be mandatory — every Czech member must have a birth number on file. Non-Czech members must not have one (this is already enforced).

## Proposed Change

Make birth number a required field when nationality is CZ:

| Nationality | Current | Target |
|---|---|---|
| CZ | birthNumber optional | birthNumber **required** |
| non-CZ | birthNumber forbidden | birthNumber forbidden (no change) |

### Backend

- `Member.validateBirthNumberNationality()` — add validation: if nationality is CZ and birthNumber is null, throw error
- `MembersDataBootstrap` — add birth numbers to bootstrap CZ members (currently null)

### Frontend

- Member form: show birth number field only when nationality is CZ, mark as required
- Hide birth number field entirely for non-CZ nationalities

### Spec Update

- Update `openspec/specs/members/spec.md` — change birth number from optional to required for CZ nationals
- Update relevant scenarios to reflect mandatory status

## Scope

- Backend: `Member.java` validation, `MembersDataBootstrap.java`
- Frontend: member registration/edit form (birth number field visibility and required state)
- Specs: `members/spec.md`

## Out of Scope

- Birth number format validation (already implemented, no changes)
- Encryption, audit trail (already implemented, no changes)
- Consistency warnings for date/gender mismatch (unchanged)
