## 1. Core module + first consumer

- [x] 1.1 Red: test `embeddedItems` against every row of design D3 — undefined envelope and missing `_embedded` yield `[]`, single key is inferred, explicit `collection` override wins, and the guards (two keys without override, override key absent, non-array value) throw `HalCollectionError` naming the self href and the keys found
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

- [ ] 3.1 Migrate the fetching sites `CategoryPresetPickerButton`, `PastCampaignsSection`, `CampaignDetail`, `GroupsPage`, `AccommodationListPage` to `useHalCollection`; remove their local `_embedded` envelope types
- [ ] 3.2 Migrate the page-data readers `RulesTable` and `CalendarPage` to `embeddedItems(resourceData)`, and `MembershipFeeGroupDetailPage` to `embeddedItems(resourceData, 'members')`
- [ ] 3.3 Delete `hasCalendarItems` (and its `CalendarItemEmbedded` type if nothing else uses it) from `src/api/types.ts`
- [ ] 3.4 Verify: affected component tests green

## 4. Table adapter

- [ ] 4.1 Red: test `useHalTableQuery` — returns `items`, `pageMetadata`, `currentPage`/`setPage`/`rowsPerPage`/`setRowsPerPage`; no `rows`, `pageData` or `page` attributes; a response without `_embedded` (including a former `content`/`data` shape) yields empty `items`; a `collection` override is honoured
- [ ] 4.2 Green: rename `useHalCollectionQuery` → `useHalTableQuery`, compose `useHalCollection`, delete the `content`/`data` fallback and the explicit `retry`/`gcTime`
- [ ] 4.3 Migrate `KlabisTableWithQuery` and `FinancesPage` to the new return shape; pass `collectionName` through as `collection` and make it optional on `HalEmbeddedTable` (existing call sites unchanged)
- [ ] 4.4 Delete the old hook and its test file
- [ ] 4.5 Verify: table slice tests green

## 5. Verification

- [ ] 5.1 Full frontend unit suite green
- [ ] 5.2 `npm run build` (type check) and `npm run lint` green
- [ ] 5.3 E2E smoke of an affected journey (member list, fee campaign detail, finances, calendar, dashboard deadlines) against the local environment, including one empty list
- [ ] 5.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA
