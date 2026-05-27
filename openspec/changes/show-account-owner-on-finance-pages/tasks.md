## 1. Slice — MemberAccountManagePage shows account owner

- [x] 1.1 Write a failing test in `MemberAccountManagePage.test.tsx`: when the account HAL response contains an `accountOwner` link pointing to a member resource with `firstName`, `lastName`, `registrationNumber`, the page header shows the owner's full name and registration number
- [x] 1.2 Implement the header — follow `accountOwner` link, render `Jan Novák (ZBM1234)` above the existing balance card; show a header skeleton while the owner is loading; render nothing (graceful) if the link is missing
- [x] 1.3 Add new localization entries to `frontend/src/localization/labels.ts` (e.g. `finance.accountOwnerHeading`); reuse existing name/registration formatting helpers if available
- [x] 1.4 Refactor — extract `AccountOwnerHeader` component (or hook + presentational piece) if both finance pages duplicate the logic
- [x] 1.5 Test passes; ESLint clean; `npm run build` succeeds

## 2. Slice — FinancesPage shows account owner

- [x] 2.1 Write a failing test in `FinancesPage.test.tsx`: when the logged-in member opens "Finance", the page header shows their first name, last name, and registration number obtained via the `accountOwner` link
- [x] 2.2 Reuse the component/hook introduced in slice 1; wire it into `FinancesPage.tsx`
- [x] 2.3 Test passes; ESLint clean; `npm run build` succeeds

## 3. Manual verification

- [ ] 3.1 Run `./runLocalEnvironment.sh`, log in as admin (`ZBM9000`), navigate to a member detail and open the account — header shows that member's name and registration number
- [ ] 3.2 Log in as club member (`ZBM9500`), open "Finance" — header shows the logged-in member's name and registration number
- [ ] 3.3 Navigate between two different members' accounts as admin — header updates correctly for each

## 4. Spec sync and publishing

- [ ] 4.1 Run `npm run publish-frontend-resources` in `frontend/` so the change is reflected in the backend static bundle
- [ ] 4.2 After review and merge, archive the change via `/openspec-archive-change` so the delta lands in `openspec/specs/member-accounts/spec.md`
