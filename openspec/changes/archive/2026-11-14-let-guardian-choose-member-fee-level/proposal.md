# Proposal

## Why

A legal guardian manages a minor's club membership, but on the "Členské příspěvky" page of their minor they see only the fee history. The minor's current level and the choice controls stay hidden, because those operations are owner-only. A minor (and a non-member guardian) therefore cannot choose a fee level at all before the voting deadline, and a missed choice triggers the sanction for a missing choice.

## What Changes

- A legal guardian sees the minor's current level section on the minor's "Členské příspěvky" page and can choose, change and withdraw the minor's level before the voting deadline.
- A membership administrator sees the member's current level section read-only (no choice controls). Choosing and withdrawing stays with the member and the member's legal guardian only; administrators keep using emergency assignment.
- Choosing and withdrawing no longer require the acting user to be a club member, so a non-member guardian can do it.
- Spec sentence "The current level section of the page is shown only to the member themselves" is replaced.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `membership-fees`: who may see the current level section and who may choose or withdraw a level.

## Impact

- API spec `docs/openapi/spec/membershipfees.yaml`: authorization of `getFeeSummary`, `getChoice`, `chooseTier`, `removeChoice`; regenerate backend bundle and frontend types.
- Backend `membershipfees` module: controllers, link processors, tests.
- Frontend: `MemberFeeSection`, `MemberFeeChoicePage`, tests.
- E2E: `membership-fee-history.feature`, `membership-fee-choice.feature`.
- Supersedes the non-goal of `add-member-fee-history-page` (archived 2026-11-14).
