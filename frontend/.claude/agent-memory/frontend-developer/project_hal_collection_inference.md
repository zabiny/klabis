---
name: hal-collection-inference
description: useHalCollection infers the collection from the single _embedded key and decodes in the hook body, so no collection override and no select passthrough to TanStack
metadata:
  type: project
---

The HAL collection seam lives entirely in `src/hooks/useHalCollection.ts`:

- `embeddedItems<Row>(envelope)` takes no collection name — it reads the single `_embedded` key. Several keys or a non-array value throw `HalCollectionError`; absent/empty `_embedded` means an empty collection.
- `useHalCollection` calls `useAuthorizedQuery<HalEnvelope>(href, {staleTime})` with no `select` and decodes in a `useMemo`, turning a guard throw into the returned `error` (never an exception out of the hook).
- `useHalTableQuery` / `KlabisTableWithQuery` / `HalEmbeddedTable` carry no `collectionName` prop any more.

**Why:** the generated API has exactly one `_embedded` block per response, so naming it at call sites was duplicated knowledge; a guard failure must be visible instead of silently rendering an empty table.

**How to apply:** new table/collection consumers just pass an href; if a page resource ever embeds two collections, the fix is in that response's design (or a new guard), not a reintroduced override. Test fixtures must follow the contract — see [[test-mocks-hal-collection-data]].
