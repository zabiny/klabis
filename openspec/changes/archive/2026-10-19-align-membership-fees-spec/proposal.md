# Proposal

## Why

The `membership-fees` spec has drifted from the behaviour the application implements and the user has accepted as correct. Writing E2E scenarios on branch `test/e2e-membership-fees` exposed three gaps. The spec should describe the accepted behaviour so that future work and E2E coverage rely on one source of truth.

## What Changes

- **Creating a fee level**: a catalog level is created with a name and a yearly fee only. Membership payment rules are added afterwards on the level's detail page, one rule at a time. The spec currently says the level is created together with its set of rules.
- **Withdrawing a choice**: before the voting deadline, a member can withdraw their current fee level for the upcoming year. This includes a level assigned by an administrator. After the deadline, or once the campaign is closed, withdrawal is not possible. The spec currently does not mention withdrawal.
- **Relevant year on the profile**: the profile shows the level for the year of the active campaign. If no campaign is active, it shows the latest campaign year that is not in the past, and otherwise the current year. As a result, a member still sees their locked level after a campaign for the upcoming year has been closed. The spec currently leaves "the relevant year" undefined.

These are spec-only changes. The behaviour is already implemented.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `membership-fees`: the requirements "Membership Fee Level Catalog", "Member Chooses a Fee Level for the Upcoming Year" and "Member Sees Their Current Fee Level" are updated to match the implemented behaviour.

## Impact

- Spec only: `openspec/specs/membership-fees/spec.md`.
- No changes to backend, frontend or API code.
- E2E: `frontend/e2e/features/membership-fees.feature` needs new scenarios for the added spec scenarios. Existing scenario titles stay unchanged.
