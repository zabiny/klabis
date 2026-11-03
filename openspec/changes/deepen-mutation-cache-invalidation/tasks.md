## 1. Invalidation inside the mutation module

- [x] 1.1 In `hooks/useAuthorizedFetch.test.ts`, with a real `QueryClient` and a stubbed `authorizedFetch` (as in `useLegalGuardians.test.tsx`), write tests showing that:
  - after a successful `mutate`, an active `authorized` query is refetched before the per-call `onSuccess` runs;
  - `isPending` stays `true` until that refetch has finished;
  - a failed mutation triggers no refetch;
  - a successful mutation still resolves (and calls `onSuccess`) when the refetch fails;
  - `invalidateAuthorizedQueries(queryClient)` refetches the active `authorized` queries.

  Confirm that the tests fail.
- [x] 1.2 Implement `invalidateAuthorizedQueries(queryClient)` in `useAuthorizedFetch.ts`, using the same `['authorized']` key as `useAuthorizedQuery`. Make the `mutationFn` of `useAuthorizedMutation` await it after a successful response. Narrow `UseAuthorizedMutationOptions` to `{method}` (drop `onSuccess`, `onError`, `headers`) and update the JSDoc example. 1.1 passes.
- [x] 1.3 Rewrite `useLegalGuardians.test.tsx` so that the guardians refetch is triggered by a successful `useAuthorizedMutation` call instead of `useFormCacheInvalidation`. Keep the assertion unchanged.
- [x] 1.4 In `useAuthorizedQuery`, pass `retry` to `useQuery` only when `options.retry` is defined, so the `QueryClient` default (`retry: 1` in `main.tsx`) is no longer overridden by an explicit `undefined`. Add a test in `useAuthorizedFetch.test.ts` showing that a query without `retry` option uses the `QueryClient` default, then drop the `retry: false` workaround from the 1.1 refetch-failure test if it is no longer needed.

## 2. Callers

- [x] 2.1 Remove `useFormCacheInvalidation` and the `invalidateAllCaches()` calls from:
  - `HalFormDisplay`
  - `ReverseConfirmModal`
  - `FinanceTransactionDialog`
  - `usePermissionsEditor`
  - `BulkSyncOrisModal`
  - `SyncMembersFromOrisModal`
  - `useOrisEventImport`

  In `useOrisEventImport`, the ref wrapper `invalidateAllCachesRef` goes away too. Toasts, `Location` navigation, closing and error handling stay as they are.
- [x] 2.2 Remove the redundant refetches after mutations:
  - `route.refetch()` in `HalFormDisplay` and `ReverseConfirmModal`;
  - in `GroupsPage`, the two hand-built `invalidateQueries` keys, `route.refetch()` and the `useQueryClient` import;
  - `route.refetch()` in `NotificationSettingsPage`;
  - both `refetch()` calls in `MemberFeeChoicePage`.
- [x] 2.3 Switch `pwa/usePushReregistration` from its hand-built key to `invalidateAuthorizedQueries(queryClient)`.
- [x] 2.4 Delete `hooks/useFormCacheInvalidation.ts`. Remove the mocks of it and the assertions on `invalidateAllCaches` from the caller tests (`HalFormDisplay`, `useOrisEventImport`, `MemberDetailPage`, `MemberDetailPage.groups`, `EventDetailPage`). Remove assertions that `route.refetch` was called after a mutation (`HalFormDisplay`, `NotificationSettingsPage`). Leave every other assertion and the `useAuthorizedMutation` mocks unchanged.

## 3. Verification

- [ ] 3.1 Confirm with `grep` in `frontend/src`:
  - no `useFormCacheInvalidation` and no `invalidateAllCaches` remain;
  - no `queryKey: ['authorized'` remains outside `hooks/useAuthorizedFetch.ts`.
- [ ] 3.2 Run the frontend unit tests (test-runner agent) and `npm run build`. Both must pass.
- [ ] 3.3 In the browser at `http://localhost:3000`, submit a HAL form (for example, editing a member) and reverse a finance transaction. Confirm in the network tab that the route resource is fetched once after the mutation, not twice, and that the page shows the updated data.
- [ ] 3.4 Run `npm run refresh-backend-server-resources` before the commit.
