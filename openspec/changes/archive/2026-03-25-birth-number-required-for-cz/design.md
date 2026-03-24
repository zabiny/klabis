## Context

Birth number (rodne cislo) is currently optional for Czech nationals. The validation in `Member.validateBirthNumberNationality()` only checks the negative case: birth number is forbidden for non-CZ nationality. The positive case — birth number is required for CZ nationality — is not enforced.

Bootstrap data creates two CZ members without birth numbers, which would violate the new constraint.

Frontend currently shows birth number field for CZ nationals but does not enforce it as required.

## Goals / Non-Goals

**Goals:**
- Enforce birth number as mandatory for Czech nationals in backend validation
- Frontend shows birth number field only for CZ nationality, marked as required
- Bootstrap data includes valid birth numbers for CZ members

**Non-Goals:**
- Changes to birth number format validation (already correct)
- Changes to encryption or audit trail
- Changes to consistency warnings (date/gender mismatch)

## Decisions

**Validation location: extend existing `validateBirthNumberNationality` method**

The method already handles the "non-CZ + birthNumber = error" case. Adding the symmetric "CZ + no birthNumber = error" case keeps all nationality-birth-number coupling in one place.

**Bootstrap birth numbers: use valid, consistent values**

Jan Novak (male, born 1990-03-15): `900315/1234`
Eva Svobodova (female, born 1995-07-22): `955722/1234`

These follow the format rules and are consistent with the members' date of birth and gender, avoiding consistency warnings.

**Frontend: conditional visibility, not just disabled state**

Birth number field is hidden entirely for non-CZ nationalities (not just disabled). When nationality changes to CZ, field appears as required. When nationality changes from CZ, field is hidden and value is cleared.

## Risks / Trade-offs

**Register vs Update validation symmetry** — The same validation runs on both register and update paths. Since only H2 in-memory database is used (no existing production data), there are no migration concerns. [Risk: none] → No mitigation needed.
