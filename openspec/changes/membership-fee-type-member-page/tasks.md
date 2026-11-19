# Tasks

Prerequisite: change `rename-fee-tier-to-fee-type` is implemented and merged (post-rename names are used below). Follow Red-Green-Refactor in every group; run tests via the test-runner skill, sequentially.

## 1. History shows the price per year (vertical slice)

- [x] 1.1 Write failing tests (history service returns `yearlyFee` from the published snapshot; `@WebMvcTest` on `fee-history` asserts `assignments[].yearlyFee`); verify they fail for the right reason
- [x] 1.2 Add `yearlyFee` to `FeeAssignmentResponse` in `docs/openapi/spec/membershipfees.yaml` (use `klabis-api-spec` skill), run `./gradlew openapiBundle` and `npm run openapi`; verify the generated types contain the field
- [x] 1.3 Add `yearlyFee` to the history read model and mapper; verify the backend tests from 1.1 pass
- [x] 1.4 Write failing frontend test, then add the "Cena" column to the history table in `MemberFeeHistoryPage` with label in `labels.memberFee`; verify the vitest file passes and `npm run build` succeeds
- [x] 1.5 Add a test that a later catalog edit does not change the price of a past year's entry; verify it passes

## 2. Section only for those who may choose, only during an active campaign (vertical slice)

- [x] 2.1 Write failing `@WebMvcTest` cases for `fee-history`: `feeSummary` link present for member and guardian while a campaign is active; absent for an administrator without the choice right, for another member, and when no campaign is active; `fee-summary` answers for the active campaign's year without a `{year}` path variable and returns 404 when no campaign is active
- [x] 2.2 Change `MemberFeeHistoryDetailsPostprocessor` to add the `feeSummary` link per D1 (active campaign + caller may choose); drop `{year}` from `getFeeSummary` in the OpenAPI spec and controller (year from the active campaign, `year` in the response, 404 otherwise), regenerate bundle and frontend types; verify 2.1 passes
- [x] 2.3 Write failing frontend tests for `MemberFeeSection` / `MemberFeeHistoryPage`: section rendered only when the `feeSummary` link is present, fetches via the link; locked state after deadline; "voting closed, nothing chosen" message; hidden otherwise
- [x] 2.4 Update `MemberFeeSection` to gate on the link and drop hand-built URLs; verify the vitest files pass

## 3. Choose the type directly in the section, with prices (vertical slice)

- [x] 3.1 Write failing tests: `chooseFeeType` template exposes `membershipFeeGroupId` with `options.link` to the published-groups endpoint of the campaign year (no inline options) and type `MembershipFeeGroupChoice`, only while voting is open; verify they fail
- [x] 3.2 Rename `/api/fee-selection-campaigns/{year}/levels` to `/groups` in the OpenAPI spec; set `x-hal-input-type: MembershipFeeGroupChoice` on the property; replace inline options with `HalFormsOptionsDef.Remote` in the summary postprocessor; regenerate bundle and frontend types; verify 3.1 passes
- [x] 3.3 Write failing frontend tests: section shows each type from the options link with name and yearly price, pre-selects the recommended type, choose / change / withdraw submit the HAL template targets and refresh the page data, locked card after deadline
- [x] 3.4 Add the `MembershipFeeGroupChoice` field (fetches `options.link.href`, shows name and `yearlyFeeAmount`); move the radio-card selection, withdraw action and locked card from `MemberFeeChoicePage` into `MemberFeeSection`; verify the vitest files pass
- [x] 3.5 Delete `MemberFeeChoicePage` (+ test) and the `/members/:memberId/fee-choice[/:year]` routes in `App.tsx`; verify `npm run build` and no remaining references (`grep MemberFeeChoicePage`)

## 4. Terminology: "typ členského příspěvku" in UI texts

- [x] 4.1 Replace "tier"/"úroveň"/"Tier poplatku"/"Tiery" in `frontend/src/localization/labels.ts` (catalog title, create/edit/delete actions, choose/change, current type, field labels, error type labels, empty messages) and in components with inline Czech texts (e.g. `TierCatalogSection`); update `labels.test.ts`; verify vitest passes
- [x] 4.2 Replace Czech messages in backend (exception handler/problem details, validation messages) that mention tier/level; verify the related backend tests pass
- [x] 4.3 Verify no user-visible leftovers: `grep -rniE "tier|úrove" frontend/src/localization frontend/src/components frontend/src/pages backend/src/main` shows only code identifiers outside UI texts (none expected after the rename change)

## 5. E2E and final verification

- [ ] 5.1 Update E2E features and steps (`membership-fee-choice`, `membership-fee-catalog`, `membership-fee-campaigns`, `users`) for the section, prices, history price column and new wording (use `gherkin-bdd` skill); run `npm run test:e2e` for the membership-fee scenarios and verify they pass
- [ ] 5.2 Run the full backend and frontend suites (`SKIP_OPTIMIZATIONS` + `--rerun-tasks` for the final backend run) and `npm run build`; verify all green apart from the known pre-existing failures
- [ ] 5.3 Manual check on http://localhost:3000 as `ZBM9500` (member), a guardian, and `ZBM9000` (admin): section visible only for the first two during an active campaign, hidden otherwise, history shows prices
- [ ] 5.4 Add the label `BackendCompleted` to GitHub issue #439 (`gh issue edit 439 --add-label BackendCompleted`; if `gh` is blocked by the hook, give the user the command)

## Workflow follow-up

- Archive the change after code review and merge; the delta specs update `membership-fees`, `application-navigation` and `users`.
