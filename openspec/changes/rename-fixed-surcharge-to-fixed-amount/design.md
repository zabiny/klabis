## Context

`MembershipPaymentRule.RuleValue` is a sealed interface with two variants: `Percentage` and `FixedSurcharge`. The name `FixedSurcharge` is misleading and collides with another use of "surcharge" in the spec (the calculated event charge result). This design renames the variant to `FixedAmount`.

## Goals / Non-Goals

**Goals:**
- Consistent terminology across spec, domain, infrastructure, and frontend
- Eliminate ambiguity between "fixed amount rule type" and "calculated event charge"

**Non-Goals:**
- No behavioral change
- No API contract change (discriminator string updated in H2 only; production database does not exist yet)

## Decisions

**Rename `FixedSurcharge` → `FixedAmount` everywhere** — single mechanical rename across all layers. No intermediate adapter or alias needed since this is a development-stage project with no production data.

## Risks / Trade-offs

- DB discriminator string changes (`FIXED_SURCHARGE` → `FIXED_AMOUNT`) — safe because H2 resets on restart; no migration script needed.
