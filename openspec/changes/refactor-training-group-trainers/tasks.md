## 1. Domain Events Infrastructure

- [ ] 1.1 Add `@DomainEvents` and `clearDomainEvents` delegation to `UserGroupMemento` (same pattern as `MemberMemento`)
- [ ] 1.2 Add test verifying domain events registered in UserGroup aggregate are published on save

## 2. TrainingGroup Domain — Trainer Alias & Event

- [ ] 2.1 Add `getTrainers()`, `addTrainer()`, `removeTrainer()`, `replaceTrainers(Set<MemberId>)` methods to TrainingGroup delegating to owner operations. `replaceTrainers` validates minimum 1 trainer.
- [ ] 2.2 Override `addMember()` in TrainingGroup to call `addMemberInternal()` + `registerEvent(MemberAssignedToTrainingGroupEvent)`
- [ ] 2.3 Update `CreateTrainingGroup` command — rename `owner` parameter to `trainer`
- [ ] 2.4 Add/update domain tests for trainer methods, addMember event registration, and create command

## 3. TrainingGroupManagementService

- [ ] 3.1 Create `TrainingGroupManagementPort` interface with methods: `createTrainingGroup`, `updateTrainingGroup`, `deleteTrainingGroup`
- [ ] 3.2 Create `TrainingGroupManagementService` implementing the port — no owner-based auth checks, uses domain operations directly
- [ ] 3.3 Create `UpdateTrainingGroupCommand` with `PatchField` fields (name, minAge, maxAge, trainers). Validate that if minAge or maxAge is provided, both must be provided.
- [ ] 3.4 Add service tests for create (with trainer), update (all PatchField combinations), and delete

## 4. MemberCreatedListener Simplification

- [ ] 4.1 Remove manual `MemberAssignedToTrainingGroupEvent` publishing from `MemberCreatedListener` — event is now registered by aggregate on `addMember()`
- [ ] 4.2 Update listener tests

## 5. API Layer — TrainingGroupController

- [ ] 5.1 Update `CreateTrainingGroupRequest` — add `trainerId` (UUID), remove auto-assignment of current user as owner
- [ ] 5.2 Create `UpdateTrainingGroupRequest` with `PatchField<String> name`, `PatchField<Integer> minAge`, `PatchField<Integer> maxAge`, `PatchField<List<UUID>> trainers`
- [ ] 5.3 Merge rename and age range update into single `PATCH /api/training-groups/{id}` endpoint using `UpdateTrainingGroupRequest`. Remove `PATCH /api/training-groups/{id}/age-range` endpoint.
- [ ] 5.4 Rename owner endpoints to trainer URLs: `POST /api/training-groups/{id}/trainers`, `DELETE /api/training-groups/{id}/trainers/{memberId}`
- [ ] 5.5 Update `TrainingGroupResponse` and `TrainingGroupSummaryResponse` — rename `owners` field to `trainers`, rename `OwnerResponse` to `TrainerResponse`
- [ ] 5.6 Update HATEOAS links and affordances — replace owner-based visibility (`isOwner`) with `GROUPS:TRAINING` permission check for all edit/delete/trainer affordances
- [ ] 5.7 Remove `requireOwner` calls from TrainingGroup operations in controller — all operations use `requireTrainingAuthority` only
- [ ] 5.8 Add/update controller tests for all changed endpoints

## 6. Frontend — Training Groups

- [ ] 6.1 Update training group creation form — add trainer selection (member picker), remove auto-assignment of current user
- [ ] 6.2 Update training group edit form — single form for name, age range, and trainers with optional fields
- [ ] 6.3 Update training group detail/list views — rename "owners" labels to "trainers" (trenéři)
- [ ] 6.4 Update API client calls to match new endpoint URLs and request/response shapes
