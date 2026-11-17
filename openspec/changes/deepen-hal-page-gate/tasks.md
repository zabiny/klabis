## 1. Page gate module + idle state

- [x] 1.1 Red: test `HalPageGate` — loading renders the default `Skeleton`; a `loading` prop replaces it; an error renders `ErrorPage` (404 → not-found page, 401/403 → forbidden page, other → error card); a missing resource without an error renders the loading view; `queryState: 'idle'` renders nothing; the child receives the non-null resource
- [x] 1.2 Red: test `HalRouteProvider`'s `queryState` — `idle` before auth is ready and on `/login`, `pending`/`success`/`error` from the query afterwards — and that `useHalPageData` exposes it together with `resourceData`, `isLoading` and `error`
- [x] 1.3 Green: fix the `queryState` derivation in `contexts/HalRouteContext.tsx` (`idle` when `!shouldFetch`) and return it from `hooks/useHalPageData.ts`
- [x] 1.4 Green: implement `src/pages/HalPageGate.tsx` with the interface from design D1–D6
- [x] 1.5 Verify: module tests green

## 2. Detail pages

- [x] 2.1 Migrate the eleven detail pages to the gate: `MemberDetailPage`, `EventDetailPage`, `GroupDetailPage`, `TrainingGroupDetailPage`, `LegalGuardianDetailPage`, `LegalGuardianGroupDetailPage`, `MembershipFeeTierDetailPage`, `FeeSelectionCampaignDetailPage`, `MembershipFeeGroupDetailPage`, `MemberFeeHistoryPage`, `SyncOverviewPage`
- [x] 2.2 Keep page-specific error assertions (e.g. a page-level `ErrorPage` case) and remove the per-page duplicated loading/absent assertions
- [x] 2.3 Verify: detail-page slice tests green

## 3. List, admin, calendar and newly gated pages

- [ ] 3.1 `GroupsPage`, `TrainingGroupsPage`, `LegalGuardianGroupsPage`, `MembershipFeesAdminPage`: replace the inline `<Alert severity="error">` branch with the gate (error policy now `ErrorPage`)
- [ ] 3.2 `CalendarPage`, `GenericHalPage`: adopt the gate with `loading={<Spinner/>}`
- [ ] 3.3 `MembersPage`, `EventsPage`: adopt the gate, introducing the page-level error state they lack today
- [ ] 3.4 `NotificationSettingsPage`, `MemberAccountManagePage`: adopt the gate; keep their existing loading visuals via the `loading` prop
- [ ] 3.5 Trim the migrated page tests to happy path plus page-specific behaviour; verify the slice green

## 4. Dissolve `FinancesPage.tsx`

- [ ] 4.1 Move `BalanceCard`, `TransactionsTable` and `TransactionReverseRequest` to `components/finance/`; move `FinancesPage.test.tsx` with them
- [ ] 4.2 Update imports in `pages/finances/MemberAccountManagePage.tsx` and `pages/finances/ReverseConfirmModal.tsx`; delete `pages/finances/FinancesPage.tsx` and confirm no references remain
- [ ] 4.3 Verify: finance slice tests green

## 5. Verification

- [ ] 5.1 Full frontend unit suite green
- [ ] 5.2 `npm run build` (type check) and `npm run lint` green
- [ ] 5.3 E2E smoke of affected journeys (member list and detail, event list and detail, membership fees admin, calendar, account with transactions, notification settings) against the local environment
- [ ] 5.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA
