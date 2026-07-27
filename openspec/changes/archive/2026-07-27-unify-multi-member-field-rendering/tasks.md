## 1. Backend: use MemberId directly in UpdateTrainingGroupRequest.trainers

- [x] 1.1 Change `UpdateTrainingGroupRequest.trainers` from `PatchField<List<String>>` to `PatchField<List<MemberId>>`
- [x] 1.2 Remove the now-unnecessary `trainerUuids()` conversion method on `UpdateTrainingGroupRequest`
- [x] 1.3 Simplify `TrainingGroupController.updateTrainingGroup` to build `UpdateTrainingGroupCommand` directly from `request.trainers()` (mapping `List<MemberId>` to `Set<MemberId>`), removing the manual UUID string parsing
- [x] 1.4 Update/add unit tests in `UpdateTrainingGroupRequestTest` (or equivalent) covering the new `MemberId`-typed `trainers` field, including the existing "trainers explicitly null" case
- [x] 1.5 Update `TrainingGroupControllerTest` request bodies/assertions if the JSON shape assumptions change (wire format for `trainers` stays `["uuid", ...]` — only Java type changes), run this test class and confirm all cases pass unchanged in behavior

## 2. Frontend: verify trainers renders as member picker + remove dead code

- [x] 2.1 Remove the dead `case "List"` branch from `frontend/src/components/KlabisFieldsFactory.tsx` (unreachable — no backend field emits `type: "List"`)
- [x] 2.2 Update `KlabisFieldsFactory.test.tsx` to remove any test coverage written specifically for the deleted `case "List"` branch, confirming no other test relies on it
- [x] 2.3 Regenerate OpenAPI types if the backend field type change affects `klabisApi.d.ts` (`npm run openapi`)
- [x] 2.4 Manually verify in the browser (`http://localhost:3000/training-groups/{id}`, edit form) that the "trainers" field now renders as a member picker per row (name + registration number) instead of `[object Object]` text inputs, for a group with an existing trainer

## 3. Full verification

- [x] 3.1 Run backend test suite for the `groups.traininggroup` module (`TrainingGroupControllerTest`, `TrainingGroupManagementServiceTest`, `TrainingGroupTest`, `TrainingGroupPersistenceTest`) — confirm all pass
- [x] 3.2 Run frontend test suite for `KlabisFieldsFactory.test.tsx` and any training-group page tests — confirm all pass
- [x] 3.3 Run `tsc --noEmit` to confirm no type errors after the OpenAPI regeneration and field factory cleanup
- [x] 3.4 Confirm no other `List<String>`/`List<UUID>` HAL-FORMS properties semantically represent member IDs (re-check performed during proposal: only `PublishYearRequest.levelIds` is a non-member multi-select, correctly typed as `MembershipFeeTierMultiSelect`) — no further action expected, just a final sanity re-grep before closing the change
- [x] 3.5 Run `npm run refresh-backend-server-resources` to rebuild the frontend bundle and copy it into `backend/src/main/resources/static/`, so the fix is reflected when serving the frontend from the backend on `:8443` (not just the Vite dev server on `:3000`)
