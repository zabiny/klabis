## Context

`HalRouteProvider` already owns the fetch for the routed HAL resource and exposes `resourceData`, `isLoading`, `error`, `queryState`, `refetch`, `navigateToResource` and `getResourceLink` through `HalRouteContext`. `useHalPageData` is a 19-line `Pick` of that context used by 40 call sites: pages gate on it, components read data or links from it.

The gate itself never moved. 17 pages write:

```tsx
if (isLoading) return <Skeleton/>;
if (error) return <ErrorPage error={error}/>;      // 6 pages
if (!resourceData) return <Skeleton/>;             // or null
return <PageContent resourceData={resourceData}/>;
```

Five more write only `if (isLoading) return <Skeleton/>` and render with `resourceData?.x`, so a failed route shows a half-empty page. Five pages render `<Alert severity="error">{describeError(error)}</Alert>` where six render `<ErrorPage>` — two policies for the same situation.

`queryState` was meant to carry four states but its derivation is:

```ts
if (status === 'pending') 'pending'; else if (status === 'error') 'error'; else if (status === 'success') 'success'; else 'idle';
```

`status` is always one of `pending | error | success`, so `idle` is unreachable. With `enabled: false` (auth not ready, `/login`) TanStack reports `status: 'pending'` and `isLoading: false`, so the page sees "no data, no error, not loading" — exactly the state the gate must not render as an empty page.

## Goals / Non-Goals

**Goals:**
- One module answers "render this routed HAL page": loading, error, idle, data.
- One error policy across gated pages.
- Pages stop writing the three branches; they receive a non-null typed resource.
- `idle` becomes a real, consumed state.

**Non-Goals:**
- Changing `HalRouteProvider`'s fetch policy (staleTime, gcTime, retry) or `HalRouteContext`'s other fields.
- Touching inline error presentation in tables, forms and sub-resource sections (`ErrorDisplay`, `HalEmbeddedTable`, `HalSubresourceProvider`).
- Migrating components that read page data without gating (`RulesTable`, `CampaignDetail`, `HalFormsSection`, `HalLinksSection`, `TransactionsTable`, `AccountOwnerHeader`, `SyncStatusIndicator`, `MemberName*`).
- Changing `ErrorPage`, `NotFoundPage` or `ForbiddenPage`.
- Any backend, API or spec change.

## Decisions

**D1 — A render-prop component, not a hook returning a union.** `<HalPageGate>{(member) => …}</HalPageGate>` removes the three branches from the page; a hook returning a discriminated union would only rename them. The component is the page boundary the pages already implement by hand.

**D2 — The child receives the typed resource as its only argument.** No wrapper object: the page needs the resource, and `refetch`/`navigateToResource`/`getResourceLink` stay available through `useHalPageData` where a body genuinely needs them. Typing by `<HalPageGate<MemberDetail>>{(member) => …}</HalPageGate>`.

**D3 — Loading is the default slot; `loading` overrides it.** Default `<Skeleton/>` matches the majority. `CalendarPage`, `GenericHalPage` and the account pages pass `loading={<Spinner/>}` (or the existing "Načítání transakcí…" line) so no page changes its loading visual.

**D4 — A missing resource without an error is loading.** After `isLoading` and `error`, an absent resource means the query has not produced data yet; rendering an empty page would be indistinguishable from a bug. A genuine missing record is a 404 and arrives as `error`.

**D5 — `idle` renders nothing.** `HalRouteProvider` gains a correct derivation: `idle` when `!shouldFetch` (auth not ready, `/login`, `/auth/callback`), otherwise from the query status. The value reaches the gate through `useHalPageData` (D8) and the gate returns `null` in `idle`, so the pre-auth frame is blank instead of a skeleton that never resolves. This fixes the unreachable branch and gives `queryState` its first consumer.

**D6 — One error policy: `ErrorPage`.** 404 → `NotFoundPage`, 401/403 → `ForbiddenPage`, otherwise the Czech error card. Inline `<Alert severity="error">` inside a page's error branch disappears; section-level `ErrorDisplay` stays. The gate passes a non-null error, which `ErrorPage` already requires.

**D7 — Scope: 21 pages that own a routed HAL resource.** Full gate today: `MemberDetailPage`, `EventDetailPage`, `GroupDetailPage`, `TrainingGroupDetailPage`, `LegalGuardianDetailPage`, `LegalGuardianGroupDetailPage`, `MembershipFeeTierDetailPage`, `FeeSelectionCampaignDetailPage`, `MembershipFeeGroupDetailPage`, `MemberFeeHistoryPage`, `SyncOverviewPage`, `CalendarPage`, `GenericHalPage`, `GroupsPage`, `TrainingGroupsPage`, `LegalGuardianGroupsPage`, `MembershipFeesAdminPage`. Loading-only today: `MembersPage`, `EventsPage`, `NotificationSettingsPage`, `MemberAccountManagePage`.

**D8 — `useHalPageData` stays and gains `queryState`.** It is already the boundary that 23 page tests mock, so the gate consumes it instead of `useHalRoute`: `useHalPageData` returns `resourceData`, `isLoading`, `error`, `queryState`, `refetch`, `navigateToResource` and `getResourceLink`. Extending it by one field keeps one accessor and one mock boundary; routing the gate through `useHalRoute` would have forced all 23 page tests to mock a second module. Components that only read data or links are unaffected.

**D9 — The module lives in `src/pages/HalPageGate.tsx`.** It is a page boundary and imports `pages/ErrorPage`; putting it under `components/` would create a components → pages dependency.

**D10 — `Pages/finances/FinancesPage.tsx` is dissolved.** It exports no page: `BalanceCard`, `TransactionsTable` and `TransactionReverseRequest` move to `components/finance/` (which already holds `FinanceTransactionDialog`); `MemberAccountManagePage` and `ReverseConfirmModal` update imports; `FinancesPage.test.tsx` moves with the components it tests.

**D11 — Tests concentrate, they are not duplicated.** One module test covers the four states (`isLoading`, `error`, absent data, idle) and the `loading` slot. Page tests keep the happy path and the page-specific error cases that assert page behaviour, not the gate's branches.

### Alternatives considered for D1

| Shape | What a page writes | Non-null guarantee | Test surface | Cost |
|---|---|---|---|---|
| **Render-prop component (chosen)** | `<HalPageGate>{(r) => <Content r/>}</HalPageGate>` | type | gate test + page happy path | callback is not a component, so hooks live in `*Content` — already the case on every page |
| Flat children + resource context | `<HalPageGate><Content/></HalPageGate>` | runtime throw outside the gate | body only testable inside the gate | a second context; the guarantee moves from types to a throw |
| HOC / route wrapper | page receives a `resource` prop | type | page rendered directly with a prop | router indirection, HOC typing, every page test's render changes |
| Hook returning a union | `if (page.state !== 'ready') return …` | type | as today | the branch stays; the gate is only renamed |
| Suspense + error boundary | nothing | type | every page test needs a `Suspense` wrapper | a real render bug would surface as `ForbiddenPage`/`NotFoundPage`; shared `useAuthorizedQuery` gains suspension for non-page consumers; `enabled: false` does not compose |
| Plain function (not a component) | same call shape as the chosen one | type | no component identity for tests or DevTools | hooks in the callback are illegal; strictly worse than the chosen shape |

The render-prop shape wins because pages already split into a thin page and a `*Content` component, so the migration is mechanical and the callback never needs hooks.

## Risks / Trade-offs

- **Five pages change the container of an error.** `ForbiddenPage`/`NotFoundPage` instead of an inline alert; the Czech text and the authorization outcome are the same. Documented in the proposal's No Behavior Change Justification.
- **Four pages gain a page-level error state.** A failed route now replaces the page instead of rendering empty sections. That is the point, but it is visible.
- **`idle` blanks one frame.** Before the first data arrives and while auth settles, the gate renders nothing. The app shell is already a spinner in that window, so nothing flickers.
- **Pages that legitimately render without the route resource** would now be gated behind it. Audited: all 21 use the route resource as their primary data; the excluded ones (`TransactionsTable`, `AccountOwnerHeader`, `HalEmbeddedTable`, `SyncStatusIndicator`, `MemberName*`) are sections that keep their own state.

## Glossary

| Term | Meaning |
|---|---|
| HAL page | A page whose content is the HAL resource the route fetches: one fetch, one resource, one rendering. |
| Page gate | The module that turns the route query's state into the page's rendering: loading, error, idle, data. |
| Idle | The route query has not started because the app is not authenticated yet; nothing should be rendered. |
| Section state | Loading/error handling inside a part of a page (a table, a form, a sub-resource) — outside the gate. |

## Domain and API impact

None. No domain concept, REST resource, request/response shape, link, template or authority changes; the SPA consumes and renders the same payloads.
