# Tasks: Unified Finance Dialog (gh-273)

Vertical slices — every group is independently committable, testable, and adds end-to-end visible value.

## 1. Backend — `accountOwner` HAL link on member-account

- [x] 1.1 Add failing test in `MemberAccountControllerTest` (or equivalent slice test) asserting the GET member-account response contains a HAL link with rel `accountOwner` whose href resolves to the corresponding `/members/{id}` resource
- [x] 1.2 Extend `MemberAccountPostprocessor` to add the `accountOwner` link via `klabisLinkTo(methodOn(MembersController.class).getMember(...))` (or the existing equivalent endpoint)
- [x] 1.3 Verify test passes; run the full finance module test suite
- [x] 1.4 Regenerate OpenAPI spec (`./gradlew generateOpenApi` or equivalent) — confirm `accountOwner` link rel appears in `MemberAccountResponse`

## 2. Frontend — regenerate API types & shared building blocks

- [ ] 2.1 Run `npm run openapi` in `frontend/` to refresh `klabisApi.d.ts` from the updated backend spec
- [ ] 2.2 Add localization strings to `src/localization/labels.ts`: dialog title ("Vložit / Vybrat"), tab labels ("Připsání vkladu", "Stržení částky"), Banknote action aria-labels for member list and member account page

## 3. Frontend — `FinanceTransactionDialog` standalone component (TDD)

- [ ] 3.1 Write component test for `FinanceTransactionDialog` covering: opens with skeleton until both fetches resolve, renders header (name + registration number + balance) after both resolve, shows both tabs when both templates are present, hides tabs when only one template is present, does not render action at all when no template is present
- [ ] 3.2 Add test: switching tabs preserves amount and note values
- [ ] 3.3 Add test: last selected tab is persisted in `localStorage` and restored on next open
- [ ] 3.4 Add test: submit posts to the correct template endpoint for the active tab; on success invokes `onClose` and invalidates relevant queries
- [ ] 3.5 Implement `FinanceTransactionDialog` accepting `{ accountLink: Link; isOpen: boolean; onClose: () => void }` props; internally fetch account → follow `accountOwner` link → wait for both → render
- [ ] 3.6 Implement tab UI matching Pencil mockup (underline-style, primary color for active); CTA button styling per active tab (green for deposit, destructive/red for charge)
- [ ] 3.7 Verify all tests pass; verify visually in dev environment

## 4. Frontend — integrate dialog into `MemberAccountManagePage`

- [ ] 4.1 Update existing `MemberAccountManagePage.test.tsx`: replace assertions for two `HalFormButton`s with assertions for a single Banknote action button that opens `FinanceTransactionDialog`
- [ ] 4.2 In `MemberAccountManagePage.tsx`, replace the two `HalFormButton` instances (deposit, charge) with one button (Banknote icon, label "Vložit / Vybrat") that opens `FinanceTransactionDialog` passing the current account's self link
- [ ] 4.3 Remove imports of `ArrowUpCircle`, `ArrowDownCircle` from this page if no longer used
- [ ] 4.4 Manual verification: full deposit and charge flows still work end-to-end; transaction history refreshes after submit

## 5. Frontend — integrate dialog into `MembersPage`

- [ ] 5.1 Update `MembersPage.test.tsx` (or equivalent): assert Banknote action appears in member row when `_links.account` exists, opens `FinanceTransactionDialog` on click, does not navigate
- [ ] 5.2 In `MembersPage.tsx`, remove the `PiggyBank` button branch from `renderActionsCell` (lines around 115–128)
- [ ] 5.3 Add a `Banknote` button to `renderActionsCell` that opens `FinanceTransactionDialog` for the row's member account link (replaces the removed PiggyBank action)
- [ ] 5.4 Remove `openAccountPage` helper function — no longer used
- [ ] 5.5 Manual verification: clicking Banknote opens dialog with correct member context; row click still navigates to member detail (unchanged)

## 6. Frontend — icon swap on `MemberDetailPage`

- [ ] 6.1 In `MemberDetailPage.tsx` (around line 286), change `<PiggyBank ... />` to `<Banknote ... />` on the "open member account" button; keep label and navigation behavior unchanged
- [ ] 6.2 Update import on line 14 — replace `PiggyBank` with `Banknote` if `PiggyBank` is no longer used elsewhere in the file
- [ ] 6.3 Existing `MemberDetailPage.test.tsx` should still pass (label-based assertions, not icon-based)

## 7. Frontend — cleanup

- [ ] 7.1 Search the codebase for any remaining usages of `PiggyBank` related to finance; remove or replace if found
- [ ] 7.2 Run `npm run lint` and `npm run test` — all clean
- [ ] 7.3 Run `npm run refresh-backend-server-resources` to publish frontend to backend static resources

## 8. Verification & wrap-up

- [ ] 8.1 Manual smoke test: start `runLocalEnvironment.sh`, log in as admin (ZBM9000), exercise: open dialog from member list, switch tabs, verify values preserved, submit deposit, reopen dialog (verify last tab restored), open dialog from account page, navigate to account from member detail (verify Banknote icon)
- [ ] 8.2 Manual smoke test as regular member (ZBM9500): verify no Banknote action appears in any page (no FINANCE:MANAGE)
- [ ] 8.3 Update GitHub issue #273 with a brief summary and add label `BackendCompleted` (per project convention)
- [ ] 8.4 Run `openspec validate gh-273-unified-finance-dialog --strict` — confirm clean
