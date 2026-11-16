## 1. Core module + first consumer

- [ ] 1.1 Red: test `useHalCollection` — returns rows from the single `_embedded` key, `undefined` href yields `items: []` and `isLoading: false`, explicit `collection` override wins, and the three guards (zero keys, two keys, non-array value) fail loudly with the href and keys in the message
- [ ] 1.2 Green: implement `src/hooks/useHalCollection.ts` with the interface from design D2
- [ ] 1.3 Refactor: make `useEventTypes` an adapter over the module; keep `getById` and its mapping test, drop the envelope-fetch assertions the module now owns
- [ ] 1.4 Verify: module slice tests green (`useHalCollection`, `useEventTypes`)

## 2. Remaining adapters

- [ ] 2.1 Migrate `useMembershipFeeTierOptions` to `select` value/prompt mapping
- [ ] 2.2 Migrate `useLegalGuardians`; delete its hand-written `undefined` href loading rule
- [ ] 2.3 Migrate `useUpcomingDeadlines` and `useMyUpcomingRegistrations` — projections stay in the adapters, passed through `select`
- [ ] 2.4 Trim the three adapter tests to their own mapping only
- [ ] 2.5 Verify: adapter slice tests green

## 3. Component call sites

- [ ] 3.1 Migrate `CategoryPresetPickerButton`, `PastCampaignsSection`, `CampaignDetail`, `RulesTable` to `useHalCollection`
- [ ] 3.2 Migrate `GroupsPage`, `AccommodationListPage`, `MembershipFeeGroupDetailPage`; remove their local `_embedded` envelope types
- [ ] 3.3 Verify: affected component tests green

## 4. Table adapter

- [ ] 4.1 Red: test `useHalTableQuery` — returns `items` for rows, `page` metadata and paging state; no `pageData` rows attribute; `content`/`data` responses are no longer accepted
- [ ] 4.2 Green: rename `useHalCollectionQuery` → `useHalTableQuery`, compose `useHalCollection`, delete the `content`/`data` fallback
- [ ] 4.3 Migrate `KlabisTableWithQuery` and `FinancesPage` to the new return shape
- [ ] 4.4 Delete the old hook and its test file
- [ ] 4.5 Verify: table slice tests green

## 5. Verification

- [ ] 5.1 Full frontend unit suite green
- [ ] 5.2 `npm run build` (type check) and `npm run lint` green
- [ ] 5.3 E2E smoke of an affected journey (member list, fee campaign detail, finances) against the local environment
- [ ] 5.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA
