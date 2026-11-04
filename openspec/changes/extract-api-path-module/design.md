## Context

Backend HAL links come in several forms:
- absolute: `https://localhost:8443/api/members/1`, because the Vite proxy uses `changeOrigin`;
- origin-relative: `/api/members/1`;
- templated: `/api/members{?q}`.

The router uses the same path without `/api`. `HalRouteContext` fetches `/api` plus the router path.

All authenticated requests go through `authorizedFetch`. Its `resolveUrl` passes `http(s)://…` and `/api…` through unchanged and prefixes everything else with `/api`. `useAuthorizedQuery` keys the cache by `toPath(url)`, which is the caller's path verbatim. Callers pre-normalise with whichever helper they know:

| Helper | Callers (non-test) | Does |
|---|---|---|
| `extractNavigationPath` (`utils/navigationPath.ts`) | 16 files | strips URI template, origin and `/api` |
| `normalizeKlabisApiPath` (`utils/halFormsUtils.ts`) | 9 files | strips origin and `/api`, adds a leading `/`, `''` → `/` |
| `stripUriTemplate` (`utils/halFormsUtils.ts`) | 4 files | strips URI template |
| `toPath` (`hooks/useAuthorizedFetch.ts`) | 1 | splits into pathname / search / hash for the query key |
| `'/api' + …` | `HalFormDisplay`, `HalFormsPageLayout`, `Layout` | re-adds the prefix |

The two big helpers differ only in edge cases (template stripping, empty input). Both match `/api` with `startsWith`, so `/apiary` would lose its first four characters.

## Goals / Non-Goals

**Goals:**
- One module owns the `/api` rule and URI-template stripping.
- Callers pass raw HAL hrefs to the fetch layer and use one function for router paths.
- One cache entry per resource, whatever form its href had.

**Non-Goals:**
- `normalizeUrl` in `api/url.ts`. It builds absolute URLs for the OIDC client and must stay import-free (`klabisUserManager` evaluates it at module load).
- `parseHalHref` / `serializeHalHref` (query-parameter editing) and `toHref` / `asLinkArray` (the `Link | Link[]` shape; see `narrow-hal-page-data`).
- Literal `/api/...` strings built from route params (`AccommodationListPage`, `MemberFeeChoicePage`) and the unauthenticated `passwordSetup` calls.
- `klabisOpenapiQueryClient` (openapi-fetch with its own `baseUrl`).

## Decisions

### D1: `api/apiPath.ts` with two pure functions

```ts
toApiHref(href: string): string    // → '/api/members/1?q=x'
toRoutePath(href: string): string  // → '/members/1?q=x'
```

Both functions accept an absolute URL, `/api/…`, `/…` or a bare `members/1`, each with or without URI-template variables (`{?q}`, `{id}`). Both:
- drop the origin and the hash;
- remove template variables;
- keep the query string.

Empty input or `/` gives `/api` and `/` respectively. The module has no imports, so it can be used from `api/` and `hooks/` without a cycle.

Alternative: an `ApiPath` object or class. Rejected because two free functions are the whole interface.

### D2: Exact prefix match

`/api` counts as the prefix only when it is followed by `/`, `?`, `#` or the end of the string. `toApiHref('/apiary')` gives `/api/apiary`, and `toRoutePath('/api')` gives `/`.

### D3: Normalisation lives in the fetch layer

- `authorizedFetch` resolves string URLs with `toApiHref`. A `URL` instance is passed through unchanged.
- `useAuthorizedQuery` builds its key as `[...AUTHORIZED_QUERY_KEY, toApiHref(url)]`, so the path and the search string are one key part. Invalidation by the `AUTHORIZED_QUERY_KEY` prefix keeps working.
- `toPath` and the prefix branch of `resolveUrl` are deleted.

Callers pass raw hrefs. They need no knowledge of `/api` for fetching.

Alternative: keep the key `[…, pathname, search]`. Rejected because it would mean a second parse for no gain. No code reads the key parts individually; verify this during implementation.

### D4: Caller migration

| Today | After |
|---|---|
| `extractNavigationPath(x)` for `navigate` / `<Link to>` | `toRoutePath(x)` |
| `extractNavigationPath(x)` used as a fetch URL (`PastCampaignsSection`, `CampaignDetail`) | raw `x` |
| `normalizeKlabisApiPath(x)` before `useAuthorizedQuery` / `authorizedFetch` (`HalRouteContext`, `useLegalGuardians`, `useHalFormOptions`, `useHalFormGetAvailability`, `useHalFormData`) | raw `x` |
| `normalizeKlabisApiPath` used for comparison (`shouldFetchTargetData`) | compare `toApiHref` of both sides |
| `normalizeKlabisApiPath(template.target)` in `EventRegistrationDialog`, `toRegistrationLink` | raw target |
| `stripUriTemplate(x)` (`useLegalGuardians`, `useHalFormOptions`, `SyncAttemptHistory`) | raw `x` for fetches; `toApiHref(x)` before `parseHalHref` |
| `'/api' + normalizeKlabisApiPath(pathname)` (`HalFormDisplay`, `HalFormsPageLayout`) | `pathname` |
| `` `/api${notificationsHref}` `` (`Layout`) | `notificationsHref` |

`HalRouteContext` keeps building the resource URL from the router location, so its query key becomes `/api/<route>`. That is the same key that a component gets when it fetches the resource's own HAL href.

### D5: Tests

- `api/apiPath.test.ts` is table-driven. It covers every input form from D1, the prefix edge cases from D2, and templates in both path and query position. The existing `normalizeKlabisApiPath` cases in `halFormsUtils.test.ts` move there.
- A `useAuthorizedQuery` test renders two consumers, one with `/members/1` and one with `https://host/api/members/1`. It asserts one `fetch` call.
- Tests that `vi.mock` `navigationPath` or the removed helpers are updated to the new import.

## Risks / Trade-offs

- **Absolute hrefs go through the dev proxy.** In `npm run dev`, absolute backend hrefs (`https://localhost:8443/api/…`) were fetched cross-origin. Now they become origin-relative and go through the Vite `/api` proxy. Production serves frontend and API from one origin, so nothing changes there. This is acceptable, and it removes a dev-only CORS dependency.
- **A caller relied on pre-normalisation for something other than fetching.** Mitigation: every call site is listed in D4, and the full test suite plus an E2E smoke run gate the change.
- **Query-key shape change.** Persisted query caches do not exist in the app, so there is no stale-key migration.
