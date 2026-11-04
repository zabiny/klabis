## 1. The `apiPath` module

- [x] 1.1 Write `frontend/src/api/apiPath.test.ts`, table-driven, for `toApiHref` and `toRoutePath`. Cover:
  - absolute URL, `/api/x`, `/x` and bare `x`;
  - URI template variables in path and query position;
  - empty input and `/`;
  - a query string that is kept and a hash that is dropped;
  - the prefix edge cases from design D2 (`/api`, `/api?x`, `/apiary`).

  Move the `normalizeKlabisApiPath` cases from `utils/halFormsUtils.test.ts` into it, adapting the expected values to `toRoutePath`. Confirm the tests fail.
- [x] 1.2 Implement `frontend/src/api/apiPath.ts` with no imports (D1, D2). 1.1 passes.

## 2. Fetch layer

- [x] 2.1 Add a `useAuthorizedQuery` test. Two consumers request `/members/1` and `https://host/api/members/1`, and the test asserts a single `fetch` call. Add `authorizedFetch` cases for a raw HAL href, a templated href and a bare route path. Confirm the new cases fail.
- [x] 2.2 `authorizedFetch` resolves string URLs through `toApiHref`. `useAuthorizedQuery` keys by `toApiHref(url)`. Delete `toPath` and the prefix branch of `resolveUrl` (D3). Check that nothing reads individual query-key parts. Run the tests of `api/` and `hooks/`.

## 3. Callers

- [x] 3.1 Migrate the navigation callers of `extractNavigationPath` to `toRoutePath`:
  - `HalFormDisplay`, `HalRouteContext`, `HalLinksSection`;
  - `useRootNavigation`, `useHalActions`;
  - `CalendarPage`, `UpcomingDeadlinesWidget`, `FeeGroupsTable`, `GroupMembersTable`, `PastCampaignsSection` (links), `HomePage`, `NegativeBalanceSuspensionDialog`, `SyncOverviewPage`.
- [x] 3.2 Make the fetch callers pass raw hrefs (D4):
  - `PastCampaignsSection` and `CampaignDetail` (fetch URLs);
  - `HalRouteContext`, `useLegalGuardians`, `useHalFormOptions`, `useHalFormGetAvailability` and `useHalFormData`;
  - `EventRegistrationDialog` and `toRegistrationLink`;
  - `SyncAttemptHistory`, which uses `toApiHref` before `parseHalHref`.

  `shouldFetchTargetData` compares both sides through `toApiHref`.
- [x] 3.3 Remove the hand-built prefixes in `HalFormDisplay`, `HalFormsPageLayout` and `Layout` (`'/api' + …`, `` `/api${…}` ``).
- [x] 3.4 Update the tests that import or mock the old helpers, including `useHalActions.test.tsx`, keeping their assertions. Run the frontend test suite.

## 4. Cleanup and verification

- [x] 4.1 Delete `frontend/src/utils/navigationPath.ts` and remove `normalizeKlabisApiPath` and `stripUriTemplate` from `utils/halFormsUtils.ts`. Confirm with grep that nothing outside `api/apiPath.ts` handles the `/api` prefix, apart from the literal strings and `normalizeUrl` listed as out of scope.
- [x] 4.2 Update `.claude/skills/hal-navigator-patterns/references/component-api.md` ("HATEOAS Utility Functions") to describe `toApiHref` / `toRoutePath`, and say that fetch callers pass raw hrefs.
- [x] 4.3 Run `npm run build`, eslint and the full frontend test suite.
- [x] 4.4 Run the E2E suite against a local instance. Only the 2 pre-existing unregister failures in `event-registrations.feature` are allowed. Result: 79/80; the only failure is the pre-existing "User with EVENTS:REGISTRATIONS unregisters another member" scenario.

## 5. "No URL" as `undefined` (D6, added after simplify review)

- [ ] 5.1 Add `useAuthorizedQuery` tests:
  - `useAuthorizedQuery(undefined)` never calls `fetch` and does not receive data cached for `/api`;
  - a defined URL with `enabled: false` still does not fetch.

  Change the signature to `url: string | undefined`, use `skipToken` for `undefined`, and delete the `url ? toApiHref(url) : ''` key special case. Run the tests of `src/hooks/`.
- [ ] 5.2 Migrate every caller that passes `''` (or `x ?? ''`) as a "no URL" sentinel to pass `undefined`, and drop the guards that only existed for the sentinel (`enabled: !!x`, `href !== ''`). Apply the same rule to `useHalFormGetAvailability`. Update tests mechanically.
- [ ] 5.3 Run `npm run build`, eslint and the full frontend test suite.
