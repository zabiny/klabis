## 1. Collection query owns the page (table slice)

- [x] 1.1 Write `hooks/useHalCollectionQuery.test.tsx` with a real `QueryClient` and a stubbed `authorizedFetch` (as in `useLegalGuardians.test.tsx`). It covers:
  - the request URL carries `page`, `size` and `sort` on top of `link.href`;
  - `rows` come from `_embedded[collectionName]`;
  - after `setPage(2)`, a new `link.href` produces a request with `page=0` and no request for page 2;
  - a page-size change resets the page.

  Confirm that the tests fail.
- [x] 1.2 Implement `useHalCollectionQuery(link, sort, collectionName)` by extracting page, persisted page size, request URL and `useAuthorizedQuery` from `KlabisTableCore`. Reset `page` to 0 during render when `link.href` changes (store the previous `href` in state). 1.1 passes.
- [x] 1.3 Make `KlabisTableCore` use the hook. Existing `KlabisTableWithQuery.test.tsx` passes unchanged.
- [x] 1.4 Make `TransactionsTableContent` (`FinancesPage`) use the hook and derive `reversedIds` from `rows`. Delete its own page, page-size and URL code. `FinancesPage.test.tsx` and `MemberAccountManagePage.test.tsx` pass.

## 2. Sort follows the table id

- [x] 2.1 In `hooks/useTableSort.test.tsx`, add a test that a new `tableId` re-resolves the sort (URL → localStorage → default). Confirm that it fails.
- [x] 2.2 Re-resolve the sort in `useTableSort` during render when `tableId` changes. 2.1 passes.

## 3. URL filters, members slice

- [x] 3.1 Write `hooks/useUrlFilters.test.tsx` with `MemoryRouter`. It covers:
  - reading `string`, `boolean` (including `trueValue`) and `string[]`, with `param` renames;
  - a missing parameter gives the default and writes nothing to the URL;
  - `setValues` writes a key with a default even when empty (`''`), deletes an empty key without a default, appends `string[]` values, and keeps parameters outside the schema;
  - navigation to a bare URL returns the defaults;
  - `values` keeps its identity when unrelated parameters change.

  Confirm that the tests fail.
- [x] 3.2 Implement `useUrlFilters(schema)` → `{values, setValues}` (design decision 1). 3.1 passes.
- [x] 3.3 Add a members `toQuery(values)` (`status` always, `q` only with ≥ 2 characters, `incomplete`) with pure tests.
- [x] 3.4 Replace the inline filter code in `MembersPage` with `useUrlFilters` and `toQuery`. Remove `useDefaultSearchParam` from it.
- [x] 3.5 Add a `MembersPage` test: on page 2, a status change requests `page=0`. All `MembersPage` tests pass, including "keeps the default active-only filter … after returning via the menu link".
- [x] 3.6 Delete `hooks/useDefaultSearchParam.ts` and `hooks/useDefaultSearchParam.test.tsx`.

## 4. Account transactions slice

- [ ] 4.1 Replace `useTransactionFilters` in `MemberAccountManagePage` with `useUrlFilters` (`type`, `dateFrom`→`occurredAtFrom`, `dateTo`→`occurredAtTo`). The `extraParams` become the values under their URL names.
- [ ] 4.2 Rewrite the `useTransactionFilters` block of `TransactionHistory.test.tsx` as page-level assertions on the request URL, keeping its three cases. Delete `pages/finances/useTransactionFilters.ts`. Tests pass.

## 5. Sync attempts slice

- [ ] 5.1 Add a sync attempts `toQuery(values)` (dates → instants `from`, `to`) with pure tests.
- [ ] 5.2 Make the attempts tab of `SyncOverviewPage` use `useUrlFilters` and `toQuery`. Remove `key={JSON.stringify(extraParams)}` and delete `pages/sync/useSyncAttemptFilters.ts`. `SyncOverviewPage.test.tsx` passes.

## 6. Events slice

- [ ] 6.1 Reimplement `useEventsFilterState` over `useUrlFilters` (`year` with the current year as default, `when` with `DEFAULT_TIME_WINDOW`, `q`, `registeredByMe` with `trueValue: 'me'`, `eventTypeIds`). Keep its interface and event rules. Remove the write-once default effect, the `dateFrom`/`dateTo` cleanup, the `eslint-disable` lines and the `join(',')` dependencies.
- [ ] 6.2 In `useEventsFilterState.test.ts`, drop only the "URL normalised" assertion for a coerced time window. Every other assertion stays and passes.
- [ ] 6.3 Remove `key={selectedYear ?? timeWindow}` from `EventsPage`. Add `EventsPage` tests showing that:
  - with a year selected, switching the time window applies that window's default sort;
  - a search change on page 2 requests `page=0`.

  All `EventsPage` tests pass.

## 7. Calendar slice

- [ ] 7.1 Make `CalendarPage` read and toggle `mySchedule` through `useUrlFilters` (`{mySchedule: {type: 'boolean'}}`). `CalendarPage.test.tsx` passes.

## 8. Verification

- [ ] 8.1 Confirm with `grep` in `frontend/src`:
  - no `useDefaultSearchParam`, `useTransactionFilters` or `useSyncAttemptFilters` remain;
  - `setSearchParams` appears only in `useUrlFilters`, `useTableSort` and the `tab` switch of `SyncOverviewPage`;
  - no table `key` built from filter values remains.
- [ ] 8.2 Run the frontend unit tests (test-runner agent) and `npm run build`. Both must pass.
- [ ] 8.3 In the browser at `http://localhost:3000`, check the following:
  - members: go to page 2, type a search, and see the first page of results;
  - events: select a past year, switch time windows, and check that the sort follows the window;
  - account transactions: filter by type from page 2;
  - sync attempts and calendar "Můj rozvrh": the filters still work;
  - a bare `/members` stays bare and shows active members;
  - reload keeps the filters.
- [ ] 8.4 Run the E2E features `members`, `events` and `member-accounts` against `./runLocalEnvironment.sh`.
- [ ] 8.5 Run `npm run refresh-backend-server-resources` before the commit.
