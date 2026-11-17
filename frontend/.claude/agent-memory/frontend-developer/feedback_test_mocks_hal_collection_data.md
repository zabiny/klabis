---
name: test-mocks-hal-collection-data
description: When mocking useAuthorizedQuery for anything backed by useHalCollection, return undefined (never null) and an envelope with exactly one _embedded key
metadata:
  type: feedback
---

When a test mocks the data boundary (`useAuthorizedQuery`) for a hook or component that reads through `useHalCollection`, the mocked envelope must satisfy the collection contract:

- `data` must be `undefined` (not `null`) when there is nothing to render. `embeddedItems` only treats `undefined`/absent `_embedded` as "empty"; a literal `null` throws and the table renders its error state ("Data se nepodařilo načíst.").
- The envelope must have at most one `_embedded` key. Several keys make `embeddedItems` throw `HalCollectionError`, so component tests that used to pass a `collectionName` override now fail unless the fixture really has a single-key `_embedded`.

**Why:** TanStack Query never yields `null` data, so `{data: null}` stubs were harmless before the unwrap moved into the hook body — after that move they surface as a guard error. This bit several page/component test files at once (MembersPage, GroupsPage, TierCatalogSection).

**How to apply:** Also mock the boundary, never `useHalCollection` itself — the hook is inside the module under test. See [[hal-collection-inference]] for the production contract.
