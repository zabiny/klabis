## 1. Core module + first consumer

- [x] 1.1 Red: test `embeddedItems` against every row of design D3 — undefined envelope and missing `_embedded` yield `[]`, the single key is inferred, and the guards (two keys, non-array value) throw `HalCollectionError` naming the self href and the keys found
- [x] 1.2 Red: test `useHalCollection` — returns typed `items` and `pageMetadata`, `select` receives typed rows, `undefined` href yields `items: []` and `isLoading: false`, a guard failure arrives as `error` (no throw), `refetch` is exposed
- [x] 1.3 Green: implement `src/hooks/useHalCollection.ts` (`embeddedItems` + `useHalCollection`) with the interface from design D2
- [x] 1.4 Refactor: make `useEventTypes` an adapter over the module; keep `getById` and its mapping test, drop the envelope-fetch assertions the module now owns
- [x] 1.5 Verify: module slice tests green (`useHalCollection`, `useEventTypes`)

## 2. Remaining adapters

- [x] 2.1 Migrate `useMembershipFeeTierOptions` to `select` value/prompt mapping
- [x] 2.2 Migrate `useLegalGuardians`; delete its hand-written `undefined` href loading rule
- [x] 2.3 Migrate `useUpcomingDeadlines` → `{items, totalElements, refetch}` (`totalElements` from `pageMetadata`) and `useMyUpcomingRegistrations` → `{items, refetch}`; projections stay in the adapters, passed through `select`
- [x] 2.4 Migrate `UpcomingDeadlinesWidget` and `HomePage` to the flattened return shape
- [x] 2.5 Trim the adapter tests to their own mapping only
- [x] 2.6 Verify: adapter and consumer slice tests green

## 3. Component call sites

- [x] 3.1 Migrate the fetching sites `CategoryPresetPickerButton`, `PastCampaignsSection`, `CampaignDetail`, `GroupsPage`, `AccommodationListPage` to `useHalCollection`; remove their local `_embedded` envelope types
- [x] 3.2 Migrate the page-data readers `RulesTable` and `CalendarPage` to `embeddedItems(resourceData)`, and `MembershipFeeGroupDetailPage` to `embeddedItems(resourceData)` (inference finds `members`)
- [x] 3.3 Delete `hasCalendarItems` (and its `CalendarItemEmbedded` type if nothing else uses it) from `src/api/types.ts`
- [x] 3.4 Verify: affected component tests green

## 4. Table adapter

- [x] 4.1 Red: test `useHalTableQuery` — returns `items`, `pageMetadata`, `currentPage`/`setPage`/`rowsPerPage`/`setRowsPerPage`; no `rows`, `pageData` or `page` attributes; a response without `_embedded` (including a former `content`/`data` shape) yields empty `items`
- [x] 4.2 Green: rename `useHalCollectionQuery` → `useHalTableQuery`, compose `useHalCollection`, delete the `content`/`data` fallback and the explicit `retry`/`gcTime`
- [x] 4.3 Migrate `KlabisTableWithQuery` and `FinancesPage` to the new return shape (later committing to inference only, see group 6)
- [x] 4.4 Delete the old hook and its test file
- [x] 4.5 Verify: table slice tests green

## 5. Verification

- [x] 5.1 Full frontend unit suite green
- [x] 5.2 `npm run build` (type check) and `npm run lint` green
- [x] 5.3 E2E smoke of an affected journey (member list, fee campaign detail, finances, calendar, dashboard deadlines) against the local environment, including one empty list
- [x] 5.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA

## 6. Review fixes

- [x] 6.1 Move the unwrap out of TanStack `select` into the hook body (memoised), so consumer tests mock the data boundary again; guard errors return as `error`
- [x] 6.2 Remove the `collection`/`collectionName` override from the module, `useHalTableQuery`, `KlabisTableWithQuery`, `HalEmbeddedTable` and the 13 table call sites
- [x] 6.3 Drop the unused `enabled` option and the redundant `href !== undefined &&` loading guard
- [x] 6.4 Replace the identity cast in `CampaignDetail` with the view type as the row type
- [x] 6.5 Dedupe the self-link extraction and `EventSummaryRow` alias across the two event adapters
- [x] 6.6 Make `useEventTypes` test mock the data boundary instead of `useHalCollection`
- [x] 6.7 Delete the now-obsolete `select`-aware `useAuthorizedQuery` shims from the affected test files
- [x] 6.8 Verify: full frontend unit suite, `npm run build`, `npm run lint`, E2E smoke green
