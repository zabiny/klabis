# Design

## Context

This change only updates the spec. The behaviour in it is already implemented and was accepted during E2E work on branch `test/e2e-membership-fees`. See proposal.md (Why) for the motivation. The spec deltas are in `specs/membership-fees/spec.md`.

## Goals / Non-Goals

**Goals:**
- Make `membership-fees` describe the observed, accepted behaviour, using user-facing language.

**Non-Goals:**
- Any change to backend, frontend or REST API code.
- Renaming existing scenarios: the E2E feature file copies their titles verbatim.

## Decisions

1. **Withdrawing a level assigned by an administrator is allowed.** Before the deadline, a member can withdraw any current assignment for the upcoming year, whoever made it. Alternative: allow withdrawing only choices the member made themselves. The user rejected this, because before the deadline the member owns the choice.
2. **"Locked" covers both the passed deadline and a closed campaign.** Withdrawing and changing a choice are refused after the voting deadline or once the campaign is closed. A campaign can be closed manually before its deadline. Alternative: tie locking to the deadline only. That would not match the implementation, which also locks every level of a closed campaign.
3. **Relevant year is defined in the requirement text, not as a separate requirement.** The fallback order is: active campaign, then the latest campaign year that is not in the past, then the current year. One scenario covers the visible consequence, which is a locked level that stays visible after its campaign closes. Alternative: a scenario for each fallback branch. That adds little user-facing value for the expected load.

## Domain and API

No domain or API changes. The existing withdraw-choice action on the member's fee choice is now described in the spec.

## Glossary

- **Withdrawing a choice**: a member removes their current fee level assignment for the upcoming year before the voting deadline. After that, the member has no level for that year.
- **Relevant year**: the year whose fee level the member's profile shows.

## Risks / Trade-offs

- [Withdrawal before the deadline can leave a member without a level, so the sanction applies] → This is stated in the withdrawal scenario. The profile prompts the member to choose again.
