## 1. Open the read endpoint and split the response in the spec

- [ ] 1.1 In `docs/openapi/spec/sync.yaml`, remove `x-klabis-authority: SYNC_MANAGE` from the `getSyncState` operation only; leave it on `synchronizeNow`, `acknowledgeSyncConflict`, `resolveSyncConflict` and `resetSyncRecord`.
- [ ] 1.2 Add `x-klabis-authority: SYNC_MANAGE` to eleven `SyncStateResponse` properties: `externalId`, `lastDirection`, `nextAttemptDueAt`, `failedAttemptsSinceLastSuccess`, `acceptedDivergence`, `divergedFields`, `changedSides`, `local`, `external`, `baseline`, `baselineExternal`. Do **not** add `x-klabis-owner-visible` — a sync record has no owner.
- [ ] 1.3 Leave `entityType`, `status`, `externalSystem` and `lastSuccessfulSyncAt` unannotated.
- [ ] 1.4 Shrink `required` to `[entityType, status, externalSystem]`.
- [ ] 1.5 Update the `getSyncState` operation description and the `SyncStateResponse` schema description to say which fields depend on the permission, following the wording style of `getMember` in `members.yaml`.

## 2. Regenerate and make it compile

- [ ] 2.1 Regenerate the backend model and the bundled `klabis-full.json`.
- [ ] 2.2 Regenerate the frontend types from the same spec — both derive from it and must move together.
- [ ] 2.3 Fix `SyncStateResponseConverter`: dropping `required` changes the generated component types for `failedAttemptsSinceLastSuccess` and `acceptedDivergence`, so the builder calls need adjusting. Keep the converter unconditional — it still builds every field; the split happens at serialization (design.md D4).
- [ ] 2.4 Adjust `SynchronizationController#getSyncState` if the generated interface's authority annotation changed shape for that one method; the other four keep theirs.
- [ ] 2.5 Confirm the whole backend compiles, including tests.

## 3. Tests

- [ ] 3.1 Replace `SynchronizationControllerTest.GetSyncState.requiresAuthority()` — it asserts 403 without the authority, which is now wrong. It becomes the positive case: a caller with no authorities gets 200.
- [ ] 3.2 Add a test asserting that a caller **without** `SYNC_MANAGE` receives exactly `entityType`, `status`, `externalSystem` and `lastSuccessfulSyncAt`, and that each of the eleven managed fields is absent (`jsonPath(...).doesNotExist()`) — absent, not null.
- [ ] 3.3 Add the counterpart asserting a caller **with** `SYNC_MANAGE` still receives every field, on a record in `CONFLICT` so `divergedFields`, `changedSides` and both projections are populated.
- [ ] 3.4 Keep both assertions in the `@WebMvcTest` layer with a real authenticated principal. Field-level authorization is invisible to a `@JsonTest` or a direct converter call — such a test passes while the endpoint leaks (design.md, Risks).
- [ ] 3.5 Verify the four state-changing operations still refuse a caller without `SYNC_MANAGE`.
- [ ] 3.6 Verify the 404 for a non-enrolled entity now also reaches a caller without the authority.

## 4. Frontend

- [ ] 4.1 Find the consumers of the sync detail response and make them tolerate the eleven now-hidden fields.
- [ ] 4.2 Confirm the managing view is unchanged for a user who holds the permission.
- [ ] 4.3 Verify with `npm run build`, not only `tsc --noEmit`.

## 5. Verification

- [ ] 5.1 Run the full backend test suite; all tests compile and pass.
- [ ] 5.2 Code review, with attention to the field-level annotations matching the table in design.md D2 exactly — a missing annotation is a silent leak.
- [ ] 5.3 Commit.
