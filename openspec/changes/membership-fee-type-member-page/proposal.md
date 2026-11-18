# Proposal

## Why

Club feedback (issue #439): the word "tier" is not club vocabulary, and the member's "Členské příspěvky" page is clumsy — choosing a membership fee type needs a detour to a separate page, prices are not visible when choosing, and the history does not show what the member paid. The page should show the choice only when it is relevant and let the member decide in one place.

## What Changes

- **Terminology:** "tier", "úroveň" and "fee level" become "typ členského příspěvku" (membership fee type) in specs and in all Czech UI texts: catalog, actions Přidat/Upravit/Smazat, Zvolit/Změnit, current type, error messages, menu/sections.
- **Member's "Členské příspěvky" page:**
  - Section "Členský příspěvek" above the history is shown only while a fee selection campaign is active; outside it the chosen type is visible only in the history table.
  - The type is chosen directly in that section (no separate choice page); every offered type shows its yearly price.
  - The section is visible and editable only to the member and to a legal guardian with the right to choose for their minor. An administrator without that right does not see it.
- **History table** gains the price the member pays for the chosen type in that year (snapshot of the published type).
- **BREAKING (UI only):** the separate fee choice page `/members/:memberId/fee-choice/:year` is removed; old links stop working.
- Backend: the fee page offers the section (`feeSummary` link) only to those who may choose, while a campaign is active; history entries carry the yearly price.
- Not in scope: detailed display of the current type outside the history table (deferred); renaming `MembershipFeeGroup`.

**Depends on** change `rename-fee-tier-to-fee-type`, which must be implemented first (names `MembershipFeeType`, `chooseFeeType`, `/api/membership-fee-types`).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `membership-fees`: terminology across all requirements (renamed headers where they contain "Level"); behavior changes in choosing a type, the section shown on the member's page, and the history (price per year).
- `application-navigation`: terminology in the "Členské příspěvky" menu entry requirement.
- `users`: terminology in the sign-in return-to-page scenario.

## Impact

- Spec: `docs/openapi/spec/membershipfees.yaml` (history entry gets `yearlyFee`; summary gets the offered types with prices), regenerated bundle, backend API and frontend types.
- Backend `membershipfees`: summary/history controllers and postprocessors, history service; tests.
- Frontend: `MemberFeeSection` merges the choice UI, `MemberFeeChoicePage` and its route removed, history table column, `labels.ts` texts; tests and E2E scenarios for membership fees.
