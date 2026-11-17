---
name: mocking-useauthorizedquery-select
description: Tests that mock useAuthorizedQuery must apply its select option, or hooks that unwrap inside select (useHalCollection) silently return empty items
metadata:
  type: feedback
---

When a test mocks `useAuthorizedQuery` (instead of `authorizedFetch`), the mock must honour the
`select` option it receives — e.g.
`data: envelope != null && options?.select ? options.select(envelope) : envelope`.

The `envelope != null` guard matters: TanStack Query never calls `select` for `undefined` data,
and `useHalCollection`'s select reads `envelope.page`, so an ungated mock throws
`TypeError: Cannot read properties of undefined (reading 'page')` in any test whose stub data is
nullish (`data: undefined` / `data: null`).

**Why:** `useHalCollection` unwraps `_embedded` inside `select` (design D4 of
`deepen-hal-collection`, so a guard failure surfaces as `error` instead of crashing render).
A mock that returns the raw envelope regardless of options makes `useHalCollection` read
`data.items` off the envelope → `items: []`. Symptoms: page looks loaded, only the event-type
badges/pills are missing. This already bit `EventsPage.test.tsx` and `EventDetailPage.test.tsx`
after `useEventTypes` became an adapter over `useHalCollection`; their non-empty event-type stubs
now call `options.select(envelope)`.

**How to apply:** any hook that composes `useAuthorizedQuery`/`useHalTableQuery` through `select`
inherits this. When migrating more adapters (group 2+) or components (group 3), expect tests that
stub `useAuthorizedQuery` at the boundary to need the select-aware mock, or to mock the adapter hook
itself. Note `vi.clearAllMocks()` keeps implementations — a `mockImplementation` set by one test
leaks into later tests in the same file, which is fine when the stub data only affects the render
being asserted but can silently change what an untouched sibling renders.

Once `KlabisTableWithQuery`/`HalEmbeddedTable` route through `useHalTableQuery`, every page test
fixture with `_embedded` rows needs the select-aware stub, or its table renders "empty". Two extra
gotchas surfaced in `deepen-hal-collection` group 4:

- a blanket "return the events list envelope for every URL" stub also feeds the event-type filter
  (`useEventTypes` → `useHalCollection`); honouring `select` then turns event rows into filter
  pills and tests fail on duplicate text. Exclude `/api/event-types` from blanket list stubs.
- leaked implementations make tests order-dependent once select is honoured; reset the mock in
  `beforeEach` (`mockReset()` + a null-data implementation) instead of relying on `clearAllMocks`.

See [[feedback_test_setup_hal_route_provider]] for the other boundary-mock convention.
