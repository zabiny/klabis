# Team Coordination File: refactor-training-group-trainers

## Proposal Summary
Rename owners→trainers in TrainingGroup API, explicit trainer assignment at creation, merged PATCH endpoint, permission-based auth (GROUPS:TRAINING), domain event moved into aggregate.

## Task File
`./openspec/changes/refactor-training-group-trainers/tasks.md`

## Iteration Plan

### Iteration 1: Domain Events Infrastructure + TrainingGroup Domain
- Tasks 1.1, 1.2, 2.1, 2.2, 2.3, 2.4

### Iteration 2: TrainingGroupManagementService + Listener Simplification  
- Tasks 3.1, 3.2, 3.3, 3.4, 4.1, 4.2

### Iteration 3: API Layer — TrainingGroupController
- Tasks 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8

### Iteration 4: Frontend
- Tasks 6.1, 6.2, 6.3, 6.4

## Progress Log

### Iteration 1 — 2026-04-04

**Tasks completed:** 1.1, 1.2, 2.1, 2.2, 2.3, 2.4

**Changes made:**

- `UserGroupMemento`: Added `@Transient UserGroup userGroup` field, set in `from()`, and added `@DomainEvents` / `@AfterDomainEventPublication` delegation methods — same pattern as `MemberMemento`.
- `TrainingGroup.CreateTrainingGroup`: Renamed `owner` record component to `trainer` (validation message updated to "Trainer is required"). `create()` factory updated to use `command.trainer()`.
- `TrainingGroup`: Added `getTrainers()`, `addTrainer()`, `removeTrainer()`, `replaceTrainers(Set<MemberId>)` methods delegating to package-private owner operations. `replaceTrainers` adds new trainers before removing old ones to avoid triggering the last-owner constraint.
- `TrainingGroup.addMember()`: Overrides `UserGroup.addMember(AddMember)` — calls `addMemberInternal()` then registers `MemberAssignedToTrainingGroupEvent`.
- `TrainingGroup.assignEligibleMember()`: Also registers `MemberAssignedToTrainingGroupEvent` (used by `MemberCreatedListener`).

**Tests added:**
- `UserGroupPersistenceTest`: New `DomainEventPublication` nested class with `@EventListener`-based event capture verifying that saving a `TrainingGroup` after `assignEligibleMember()` publishes `MemberAssignedToTrainingGroupEvent`.
- `TrainingGroupTest`: New `TrainerMethods` nested class (7 tests) and `AddMemberEvent` nested class (3 tests). Updated `CreateMethod` to verify `getTrainers()` and rename null-trainer test.

**All 90 tests pass.**

### Iteration 2 — 2026-04-04

**Tasks completed:** 3.1, 3.2, 3.3, 3.4, 4.1, 4.2

**Changes made:**

- `UpdateTrainingGroupCommand`: New record with `PatchField<String> name`, `PatchField<Integer> minAge`, `PatchField<Integer> maxAge`, `PatchField<Set<MemberId>> trainers`. Compact constructor validates that `minAge` and `maxAge` must both be provided or both absent.
- `TrainingGroupManagementPort`: New `@PrimaryPort` interface with `createTrainingGroup`, `updateTrainingGroup`, `deleteTrainingGroup` methods.
- `TrainingGroupManagementService`: New `@Service` implementing the port. No owner-based auth checks. `createTrainingGroup` validates age range overlap, creates group, runs age-based auto-assignment. `updateTrainingGroup` applies `PatchField` changes atomically (rename, age range with overlap validation, trainer replacement). `deleteTrainingGroup` loads and deletes.
- `UserGroup.rename(String)`: Changed visibility from package-private to public — needed by service layer to bypass owner check (authorization enforced at controller level per design D2).
- `TrainingGroup.updateAgeRange(AgeRange)`: Changed visibility from package-private to public — same reason.
- `MemberCreatedListener`: Removed `ApplicationEventPublisher` dependency and manual `MemberAssignedToTrainingGroupEvent` publishing. Event is now published automatically via `@DomainEvents` delegation in `UserGroupMemento` when `assignEligibleMember()` registers the event in the aggregate.

**Tests added/updated:**
- `TrainingGroupManagementServiceTest`: Fully rewritten to test `TrainingGroupManagementService` directly (was testing `GroupManagementService`). 15 tests covering create (5 scenarios), update (7 scenarios), delete (3 scenarios).
- `MemberCreatedListenerIntegrationTest`: No changes needed — tests verified the listener still correctly auto-assigns members (2 tests pass).

**All 1132 tests pass.**

### Fix: JVM heap exhaustion in full test run — 2026-04-04

**Symptom:** Up to 94 tests failing with `ApplicationContext failure threshold (1) exceeded` cascade when running the full test suite. Root cause was `java.lang.OutOfMemoryError: Java heap space` during parallel Spring context loading — not a code defect in Iteration 2.

**Fix:** Added `jvmArgs("-Xmx2g")` to `tasks.test` in `/backend/build.gradle.kts`. No code changes to application or test sources.

**Verification:** Full suite runs clean — 1865 tests pass, 15 skipped.
