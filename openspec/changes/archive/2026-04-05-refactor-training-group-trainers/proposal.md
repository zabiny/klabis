## Why

Training groups use the generic "owner" concept inherited from UserGroup, but the domain language for training groups is "trainer" (trenér). The current model also couples group creation to the creating user (auto-assigned as owner), forces two separate edit operations for name and age range, and publishes domain events from infrastructure listeners instead of the aggregate — violating DDD principles.

## What Changes

- **BREAKING**: Rename `owners` to `trainers` in TrainingGroup API (URLs, JSON fields). Internal domain model keeps `owners` in UserGroup.
- **BREAKING**: Training group creation requires explicit `trainerId` (MemberId) — creator is no longer auto-assigned as trainer.
- **BREAKING**: Replace two separate edit endpoints (rename + update age range) with a single PATCH endpoint using `PatchField` for optional fields: `name`, `minAge`, `maxAge`, `trainers`.
- **BREAKING**: Replace owner-based authorization for TrainingGroup operations with permission-based authorization (`GROUPS:TRAINING`). Owner-based auth remains unchanged for FreeGroup/FamilyGroup.
- Move `MemberAssignedToTrainingGroupEvent` creation from `MemberCreatedListener` into `TrainingGroup.addMember()` override. Requires adding `@DomainEvents` delegation to `UserGroupMemento`.
- When `trainers` field is provided in PATCH, it replaces the entire trainer list (minimum 1 trainer required).
- Trainer cannot be removed from group members (existing constraint, carries over from owner behavior).

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `user-groups`: Training group API changes — trainers terminology, explicit trainer assignment at creation, merged edit endpoint, permission-based authorization replacing owner-based auth for training groups, domain event moved into aggregate.

## Impact

- **Backend API**: Breaking changes to TrainingGroup endpoints (URLs, request/response shapes, authorization model)
- **Frontend**: Training group create/edit forms need updating for new API contract
- **Domain**: TrainingGroup aggregate gets `addMember()` override; `UserGroupMemento` needs `@DomainEvents` delegation
- **Infrastructure**: `MemberCreatedListener` simplified (event publishing removed, only member assignment remains)
- **Authorization**: `GroupManagementService` split — TrainingGroup operations move to separate service with `GROUPS:TRAINING` permission checks
- **Tests**: Controller tests, service tests, and domain tests for TrainingGroup need updating
