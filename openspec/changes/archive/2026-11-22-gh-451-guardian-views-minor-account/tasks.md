# Tasks

Each task follows Red-Green-Refactor: write the failing test first, implement, refactor. Run tests through the test-runner; write E2E scenarios with the e2e-tester agent.

## 1. `FINANCE:VIEW` authority and explicit group delegation sets (design D1, D3)

- [x] 1.1 Add `Authority.FINANCE_VIEW("FINANCE:VIEW", TargetType.MEMBER, GrantForm.SPECIFIC)` with a unit test asserting it is in `holdableOverTargets()` and not in `grantableOverAll()`. Verify: `Authority` unit tests pass.
- [x] 1.2 Define the explicit delegation sets in the group aggregates: `FreeGroup.delegatableAuthorities()` → `{MEMBERS_EDIT_PROFILE}` and `LegalGuardianGroup.delegatedAuthorities()` → `{MEMBERS_EDIT_PROFILE, FINANCE_VIEW}`, with unit tests for both sets. Verify: group domain unit tests pass.
- [x] 1.3 Switch `FreeGroup.delegatableAuthorities()` to the explicit set and keep its callers (`FreeGroupController` create-form options) on it. Test that creating a free group delegating `FINANCE:VIEW` throws `InvalidDelegatedAuthorityException` and that the "Create group" form options contain only "Úprava údajů člena". Verify: FreeGroup domain and controller tests pass.
- [x] 1.4 Make `LegalGuardianGroup.delegatedAuthorities()` include `FINANCE_VIEW`. Test that `LegalGuardianGroupRelationshipSource` grants a guardian `FINANCE:VIEW` and `MEMBERS:EDIT_PROFILE` over each minor of the group and nothing over the minor once removed from the group. Verify: legal guardian group tests pass.
- [x] 1.5 Add the `'FINANCE:VIEW'` authority label "Zobrazení financí člena" (with description) to `frontend/src/localization/labels.ts`. Verify: frontend label tests and `npm run build` pass.

## 2. Account owner sees own account through `FINANCE:VIEW` (design D2, D4, D5)

- [x] 2.1 Write integration tests first: a member without FINANCE_MANAGE reads their own account, transaction list and single transaction; a member without FINANCE_MANAGE is denied another member's account; a FINANCE_MANAGE user reads any account. Verify: tests fail only on the parts not implemented yet (the own-account cases must keep passing throughout).
- [x] 2.2 Add a finance-module `RelationshipSource` granting the current user `FINANCE:VIEW` over their own member id when `Members` finds a member for the user. Unit-test that a non-member user (EXT guardian, bootstrap admin) gets no grant. Verify: unit tests pass.
- [x] 2.3 In `docs/openapi/spec/finance.yaml`, change `getAccount`, `listTransactions` and `getTransaction` to `x-klabis-authority: [FINANCE_MANAGE, FINANCE_VIEW]` and remove `x-klabis-owner-visible`; update the file's header comment. Regenerate the OpenAPI bundle and the frontend types. Verify: bundle validation passes and the 2.1 tests pass.
- [x] 2.4 Make `AccountMemberDetailLinkProcessor` add the `account` link for FINANCE_MANAGE, or for `FINANCE:VIEW` over the displayed member when it is not the user's own profile. Test: the link is absent on the user's own profile and on an unrelated member's profile without FINANCE_MANAGE, and present for a finance manager. Verify: link processor tests pass.

## 3. Legal guardian opens their child's account from the child's detail (spec: member-accounts, legal-guardians)

- [x] 3.1 Backend integration tests: a member guardian and a non-member (EXT) guardian read their minor's account, transactions and a single transaction; the member detail of their minor carries the `account` link; no deposit/charge templates and no reverse affordance are present; an unrelated minor's account is denied and their detail has no `account` link. Verify: tests pass.
- [x] 3.2 Backend integration tests for loss of access: after the minor is removed from the legal guardian group (turning 18 / guardian removed), the former guardian is denied the account and the detail has no `account` link. Verify: tests pass.
- [x] 3.3 Frontend: confirm `MemberDetailPage` shows the account action from the `account` link and `MemberAccountManagePage` renders read-only (no transaction dialog, no reverse action) for a guardian response; add or extend component tests with a guardian HAL fixture. Verify: vitest for both pages passes.
- [x] 3.4 E2E: add scenarios to `frontend/e2e/features/member-accounts.feature` (or `legal-guardians.feature`) for "guardian opens their child's account from the child's detail page" and "guardian cannot record any transaction on the child's account", extending the `example-data` profile with a guardian–minor pair if missing. Verify: `npm run test:e2e` passes the new scenarios locally.

## 4. Account switcher on the "Finance" page (design D6, D8, D9; spec: Finance Page Switches Between Accounts the User May View)

- [x] 4.1 Check that `x-hal-links` with a repeated rel is accepted by `validate.mjs` and rendered by `haltypes.mjs` as `HalResourceLinks`; extend the generator with tests if not. Verify: `tools/openapi-bundle` tests pass.
- [x] 4.2 Declare the `viewableAccount` rel on `getAccount` in `finance.yaml` and add it in `MemberAccountPostprocessor`: one link per `FINANCE:VIEW` target with `title` = owner name, own account first, children by last name and first name; only when the displayed account is among them and there are at least two. Tests: member guardian with two minors gets three links in that order on own and child account; finance manager on a stranger's account gets none; member without minors gets none; EXT guardian of one minor gets none. Regenerate bundle and frontend types. Verify: tests pass.
- [x] 4.3 Frontend: render an account switcher on `MemberAccountManagePage` when `viewableAccount` links are present, with the current account selected; selecting another option navigates to that account's page. Add the switcher label. Component tests cover rendering, selection and absence without links. Verify: vitest passes and `npm run build` passes.
- [x] 4.4 E2E: scenarios "member guardian switches to a child's account" and "member without minor children sees no switcher". Verify: `npm run test:e2e` passes the new scenarios.

## 5. "Finance" menu entry for non-member guardians (design D7; spec: application-navigation)

- [x] 5.1 Change `AccountRootLinkProcessor` to link to the first viewable account (own account for a member, first child for a non-member guardian) and add no link without a viewable account. Tests: member → own account; EXT guardian of two minors → first minor by last name and first name; EXT guardian whose only child left the group → no link. Verify: tests pass.
- [x] 5.2 Frontend: confirm the "Finance" menu entry follows the root `account` link for an EXT guardian (no member profile); add a navigation test if the menu assumes a member profile. Verify: vitest passes.
- [x] 5.3 E2E: scenario "non-member guardian opens Finance and sees their child's account with the switcher". Verify: `npm run test:e2e` passes the new scenario.

## 6. Integration checks

- [x] 6.1 Run the full backend suite (with `SKIP_OPTIMIZATIONS` and `--rerun-tasks`) and the full frontend suite including `npm run build`. Verify: all green except the known pre-existing failures.
- [ ] 6.2 QA the guardian, EXT guardian and finance manager flows in the browser on `http://localhost:3000` against the delta specs. Verify: every delta scenario is checked off in the QA report.
- [x] 6.3 Code review of all backend and frontend changes before committing; fix the findings. Verify: the review reports no open important findings.
- [ ] 6.4 Add the label `BackendCompleted` to GitHub issue #451 once the backend work is merged. Verify: the label is visible on the issue.

## Workflow follow-up

- Tell the `introduce-notification-feed` change (PR #450) that the negative-balance notification can now link to the child's account.
- Archive the change after review and verify the main specs were updated.
