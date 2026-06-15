## Why

The term "fixed surcharge" is misleading — it implies an extra charge on top of event price, but semantically this is simply a fixed CZK contribution amount, parallel to the `Percentage` variant. The word "surcharge" also collides with its other meaning in the spec (the calculated event charge result), creating ambiguity.

## What Changes

- Rename "fixed surcharge rule" → "fixed amount rule" in the membership-fees spec (rule-type occurrences only; occurrences where "surcharge" means the calculated event charge result remain unchanged)
- Rename `RuleValue.FixedSurcharge` → `RuleValue.FixedAmount` in backend domain and infrastructure
- Rename factory method `fixedSurcharge(...)` → `fixedAmount(...)` 
- Update DB discriminator `"FIXED_SURCHARGE"` → `"FIXED_AMOUNT"`
- Update frontend labels: `"Fixní příplatek"` → `"Fixní částka"`

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `membership-fees`: Terminology cleanup — "fixed surcharge rule" renamed to "fixed amount rule" in requirements. No behavioral change.

## Impact

- `membershipfees` backend module (domain + infrastructure + tests)
- Frontend membership-fees pages and shared labels
- `openspec/specs/membership-fees/spec.md`
