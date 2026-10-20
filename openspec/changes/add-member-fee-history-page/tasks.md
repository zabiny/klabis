# Tasks

## 1. Slice: member opens their own fee level history from the profile

- [ ] 1.1 API spec: add `yearlyFee` (number) to `FeeAssignmentResponse` and a `member` link (`operation: getMember`) to the `getFeeHistory` response `x-hal-links` in `docs/openapi/spec/membershipfees.yaml`. Document the `feeHistory` link (`operation: getFeeHistory`, "present for the member themselves or MEMBERS:MANAGE") in `members.yaml` `getMember` `x-hal-links`. Run `./gradlew openapiBundle` in `backend/` and `npm run openapi` in `frontend/`. Verify that both succeed and that `GetFeeHistoryResource` / `GetMemberRels` in `halTypes.ts` contain `member` / `feeHistory`.
- [ ] 1.2 Red-Green: in `MemberFeeHistoryServiceTest`, add a test that each history entry carries its fee group's yearly fee snapshot. Then extend `MemberFeeHistoryPort.LevelAssignment` and `MemberFeeHistoryService.toAssignment` and map the field in `MembershipFeesResponseMapper`. Verify that `MemberFeeHistoryServiceTest` passes.
- [ ] 1.3 Red-Green: in `MemberFeeSummaryControllerTest` (`GetFeeHistoryTests`, with `.accept(MediaTypes.HAL_FORMS_JSON)`), assert `assignments[0].yearlyFee` and the `_links.member.href` of the member. Then add the `member` link in `MemberFeeHistoryDetailsPostprocessor`. Verify that the test passes.
- [ ] 1.4 Red-Green: add a `@WebMvcTest` slice test for a new `MemberFeeHistoryLinkProcessor` (membershipfees `infrastructure/restapi`) using `@WithKlabisMockUser`. It checks that `feeHistory` is present on the member detail when the caller views their own profile and absent when an ordinary member views someone else. Then implement the processor with `klabisLinkTo(methodOn(MemberFeeSummaryApi.class).getFeeHistory(id, null))`. If it injects a new port, add it to `MembershipFeesWebMvcMockitoBeans`. Verify that the test passes.
- [ ] 1.5 Red-Green (frontend): write `MemberFeeHistoryPage.test.tsx` and implement `frontend/src/pages/membership-fees/MemberFeeHistoryPage.tsx` using `useHalPageData<GetFeeHistoryResource>()` and `KlabisTable` fed through `data`. The tests check:
  - rows newest first, with year, tier name, yearly fee in Kč and source label ("Vlastní volba" / "Přiřazeno adminem");
  - the empty-state message when there are no assignments;
  - a back link whose target is taken from `_links.member`.

  Add the labels to `labels.memberFee`. Verify that the Vitest file passes.
- [ ] 1.6 Register the route `/members/:memberId/fee-history` → `MemberFeeHistoryPage` in `frontend/src/App.tsx`, next to the `fee-choice` routes. Verify that a test or a manual check on `http://localhost:3000` renders the page instead of `GenericHalPage`.
- [ ] 1.7 Red-Green (frontend): in `MemberFeeSection.test.tsx`, replace the dormant `summary._links.feeHistory` tests with tests that the "Zobrazit historii příspěvků" link is driven by a `feeHistoryHref` prop and navigates to `extractNavigationPath(href)`. Update `MemberFeeSection` and `MemberDetailPage` to pass `_links.feeHistory` from the member resource. Remove `feeHistory` from `FeeSummaryLinks` in `memberFeeTypes.ts`. Verify that the `MemberFeeSection` and `MemberDetailPage` tests pass and `npm run build` succeeds.

## 2. Slice: membership administrator opens any member's fee level history

- [ ] 2.1 Red: in `MemberFeeSummaryControllerTest`, add tests that a `MEMBERS:MANAGE` holder can read another member's fee history, and that an ordinary member still gets forbidden. Keep the existing tests that the summary stays forbidden for a manager. Verify that the new manager test fails.
- [ ] 2.2 Green: add `x-klabis-authority: MEMBERS_MANAGE` to `getFeeHistory` in `membershipfees.yaml`. Update the module header comment: 4 owner-only operations, plus `getFeeHistory` with OR semantics. Update the operation's `403` description. Regenerate the bundle and the frontend types. Verify that the tests from 2.1 pass.
- [ ] 2.3 Red-Green: extend the `MemberFeeHistoryLinkProcessor` slice test so that a `MEMBERS:MANAGE` holder sees `feeHistory` on another member's detail. Verify that the test passes without an imperative check in the processor (authorization comes from `klabisLinkTo`).
- [ ] 2.4 Integration test: add a `@SpringBootTest` + `@AutoConfigureMockMvc` test in `backend/src/test/java/com/klabis/membershipfees/` (style of `MembershipFeeTierWithRulesIntegrationTest`). Through the real HTTP stack, it checks that:
  - the member detail of a member with a past assignment offers `feeHistory` to an admin;
  - following it returns the assignment with group name, yearly fee and source;
  - an ordinary member gets forbidden for another member's history.

  Verify that the test passes.
- [ ] 2.5 Red-Green (frontend): rewrite the `MemberDetailPage.test.tsx` case "does NOT show MemberFeeSection when feeSummary link is absent (admin viewing another member)". With only a `feeHistory` link, the fee section shows only the history link and no summary. With neither link, there is no section. Adjust the `MemberDetailPage` render condition. Verify that the tests pass.
- [ ] 2.6 Update `.claude/skills/klabis-api-spec/references/field-security.md`, which says "MemberFeeChoice's and MemberFeeSummary's 5 operations" are owner-only, to reflect the `getFeeHistory` exception. Verify with a grep that no other doc claims 5 owner-only operations.

## 3. Slice: legal guardian opens the fee level history of their minor

- [ ] 3.1 Red: in `MemberFeeSummaryControllerTest`, add tests using `@WithKlabisMockUser` with a `MEMBERS:EDIT_PROFILE` grant targeted at a minor. They check that:
  - a guardian can read the history of their minor;
  - a guardian with a grant over a different minor gets forbidden;
  - a `MEMBERS:READ`-only holder and a `FINANCE:MANAGE`-only holder get forbidden;
  - a user without a member profile (a non-member guardian) can read their minor's history.

  Express the targeted grant with `@WithKlabisMockUser(targetGrants = ...)`. Express a non-member guardian with `userId` set and `memberId` empty. Verify that the guardian and non-member tests fail.
- [ ] 3.2 Green: change `getFeeHistory` to `x-klabis-authority: [MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` and remove its `x-spring-provide-args` `@ActingMember` entry (design D2, D2a). Update the module header comment and the `403` description. Regenerate the bundle and the frontend types. Update `MemberFeeSummaryController.getFeeHistory` and all `methodOn(...).getFeeHistory(...)` calls to the new signature. Verify that the tests from 2.1 and 3.1 pass.
- [ ] 3.3 Red-Green: extend the `MemberFeeHistoryLinkProcessor` slice test. A guardian sees `feeHistory` on their minor's detail and does not see it on another member's detail. A `MEMBERS:READ`-only user does not see it. Verify that the test passes with no guardian-specific code in the processor.
- [ ] 3.4 Integration test: extend the `@SpringBootTest` test from 2.4. Set up a real legal guardian group (minor plus guardian) and an assignment for the minor in a past year. Through the real HTTP stack and the real `LegalGuardianGroupRelationshipSource`, check that:
  - the guardian sees `feeHistory` on the minor's detail and can follow it;
  - the same guardian gets forbidden for a different member's history.

  Cover both a member guardian and a non-member guardian if test data setup allows. Verify that the test passes.
- [ ] 3.5 Red-Green (frontend): add a `MemberDetailPage.test.tsx` case. On a minor's detail with only a `feeHistory` link (the guardian case), the fee section shows only the history link. This is the same rendering as in 2.5, so verify that the test passes without further production changes.
- [ ] 3.6 Update `.claude/skills/klabis-api-spec/references/field-security.md` (task 2.6) to name the guardian alternative (`MEMBERS_EDIT_PROFILE`) of `getFeeHistory` as well. Verify with a grep.

## 4. Slice: E2E scenarios use the history page

- [ ] 4.1 Rewrite the scenario "Past assignments remain available" in `frontend/e2e/features/membership-fees.feature`: admin, closed campaign with the member assigned, then open the member list, open member "ZBM9500", click "Zobrazit historii příspěvků", and see the tier name and the scenario year. Reuse existing steps (`I click`, `I open the list row`, `I see`). Add a step to `steps/*.ts` only if one is missing, and document any new step in the step vocabulary of `frontend/e2e/README.md`. Verify with `npm run test:e2e -- --grep "Past assignments remain available"` against `./runLocalEnvironment.sh`.
- [ ] 4.2 Add E2E scenarios with spec-verbatim titles:
  - "Member opens their fee level history from the profile" (member chooses a tier, opens their profile, opens the history and sees the tier and "Vlastní volba");
  - "Member cannot open another member's fee level history" (member opens another member's detail; assert a positive anchor first, then "I do not see" the link).

  Verify that both pass in an E2E run.
- [ ] 4.3 Add guardian E2E scenarios. In the `example-data` profile, the `member` role (`ZBM9500`, Eva Svobodová) is the member legal guardian of the minor Sofie Svobodová (`MembersDataBootstrap.createLegalGuardianScenarios`). The non-member guardians `EXT0001`/`EXT0002` are pending activation and have no password, so they are not usable. The scenarios:
  - "Legal guardian sees the fee level history of their minor": an active campaign; a new `Given` step "the administrator has assigned member {text} to tier {text}", which resolves the member by name through the admin API; the campaign is closed. Then, as `member`, open the member list, open the row "Sofie", click "Zobrazit historii příspěvků", and see the tier.
  - "Legal guardian cannot open the history of a member who is not their minor": as `member`, open the detail of a non-ward adult (e.g. "Petra Nováčková"); a positive anchor; then `I do not see "Zobrazit historii příspěvků"`.

  Document the new step and the guardian relationship of the `member` role in `frontend/e2e/README.md`. Verify that both scenarios pass.

## 5. Final verification

- [ ] 5.1 Run the backend `membershipfees` and `members` module tests and the frontend unit tests through the test-runner, then the full suite once (sequentially, not in parallel). Verify that there are no new failures apart from the known pre-existing ones.
- [ ] 5.2 Manual QA on `http://localhost:3000`:
  - as `ZBM9500`: own history, and the history of Sofie Svobodová as guardian; no link on a non-ward;
  - as `ZBM9000`: another member's history; an inactive member shows no link.

  Verify the behavior against the spec scenarios.
- [ ] 5.3 Run `openspec validate add-member-fee-history-page --strict` and verify that it passes.
