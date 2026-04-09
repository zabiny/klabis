## 1. Shared building block — owner promotion rule on `WithInvitations` (TDD)

- [x] 1.1 Add failing unit test (new test class in `common/usergroup`): a `WithInvitations`-backed group rejects promoting a non-member to owner and throws `CannotPromoteNonMemberToOwnerException`
- [x] 1.2 Add failing unit test: the same path succeeds when the candidate is already a current member and does not re-add them to the members set
- [x] 1.3 Create new exception class `common/usergroup/CannotPromoteNonMemberToOwnerException.java` carrying the offending `UserId`
- [x] 1.4 Add owner-promotion validation helper to `common/usergroup/WithInvitations.java` (default method `promoteOwner(UserId)` or a shared static guard) that enforces the "must already be a member" rule and delegates to the underlying `UserGroup.addOwner` without its auto-add side effect
- [x] 1.5 Map `CannotPromoteNonMemberToOwnerException` to HTTP 409 (or 400) in the shared `MvcExceptionHandler` / `MembersGroupExceptionHandler`
- [x] 1.6 Verify tests 1.1 and 1.2 pass

## 2. MembersGroup uses the shared rule (TDD)

- [x] 2.1 Add failing `MembersGroupTest` unit test: `addOwner(memberId)` on a non-member throws `CannotPromoteNonMemberToOwnerException` and the member count is unchanged
- [x] 2.2 Add failing unit test: `addOwner(memberId)` on an existing member succeeds and the owner count increases without duplicating the member
- [x] 2.3 Update `MembersGroup.addOwner(MemberId)` to route through the new `WithInvitations` promotion path instead of calling `userGroup.addOwner` directly
- [x] 2.4 Verify tests 2.1 and 2.2 pass
- [x] 2.5 Extend the `MembersGroupController` integration test to cover the HTTP error response for "promote non-member" (status code + problem detail shape)

## 3. Family group domain — scalar `CreateFamilyGroup` (TDD)

- [x] 3.1 Add failing `FamilyGroupTest`: `FamilyGroup.create(new CreateFamilyGroup(name, parentId))` produces a group with the single parent listed as owner and member
- [x] 3.2 Add failing test: `CreateFamilyGroup` with `null` or missing parent fails fast with `IllegalArgumentException`
- [x] 3.3 Change the `CreateFamilyGroup` record signature to `(String name, MemberId parent)`; drop `initialMembers`
- [x] 3.4 Update `FamilyGroup.create(...)` to build the `UserGroup` from a single parent owner and a single-element membership set
- [x] 3.5 Update any in-project callers of the old signature (persistence, tests, memento) to compile against the new record
- [x] 3.6 Verify tests 3.1 and 3.2 pass

## 4. Family group domain — `addChild` / `removeChild` (TDD)

- [x] 4.1 Add failing `FamilyGroupTest`: `addChild(childId)` on a fresh group (one parent) adds the child as a non-owner member
- [x] 4.2 Add failing test: `addChild` rejects a member who is already a parent of the same group (throws `MemberAlreadyInGroupException` or equivalent typed exception with a clear message)
- [x] 4.3 Add failing test: `addChild` rejects a member who is already a child of the same group (duplicate membership)
- [x] 4.4 Add failing test: `removeChild(childId)` removes a current child; `removeChild` on a parent throws `OwnerCannotBeRemovedFromGroupException` (reuses existing behavior)
- [x] 4.5 Add failing test: `removeChild` on a member who is not in the group throws `MemberNotInGroupException`
- [x] 4.6 Implement `FamilyGroup.addChild(MemberId)` — calls `userGroup.addMember(...)` after an explicit "not an owner of this group" check
- [x] 4.7 Implement `FamilyGroup.removeChild(MemberId)` — calls `userGroup.removeMember(...)` and relies on the built-in guard against removing owners
- [x] 4.8 Verify tests 4.1–4.5 pass

## 5. Family group application service — new ports (TDD)

- [x] 5.1 Add failing `FamilyGroupManagementServiceTest`: `addChild(groupId, memberId)` persists the new child; `removeChild` removes it
- [x] 5.2 Add failing test: `addChild` for a member who is already in another family group throws `MemberAlreadyInFamilyGroupException` (uses existing `validateNoExistingFamilyGroup` helper)
- [x] 5.3 Add `addChild(FamilyGroupId, MemberId)` and `removeChild(FamilyGroupId, MemberId)` to `FamilyGroupManagementPort`
- [x] 5.4 Implement both methods in `FamilyGroupManagementService` — call `validateNoExistingFamilyGroup(memberId)` on add, then delegate to the aggregate and save
- [x] 5.5 Verify tests 5.1 and 5.2 pass

## 6. Family group REST API — simplified create + new child endpoints (TDD)

- [ ] 6.1 Add failing `FamilyGroupControllerTest`: `POST /api/family-groups` with body `{"name":"...","parentId":"<uuid>"}` returns 201 and the new group has exactly one parent and no children
- [ ] 6.2 Add failing test: `POST /api/family-groups` with missing `parentId` returns 400 with a validation error
- [ ] 6.3 Add failing test: `POST /api/family-groups/{id}/children` with `{"memberId":"<uuid>"}` adds the child; response is 201 or 204 (match existing `/parents` endpoint style)
- [ ] 6.4 Add failing test: `POST /api/family-groups/{id}/children` for a member who is already a parent of the same group returns a 4xx with a parent/child conflict error
- [ ] 6.5 Add failing test: `DELETE /api/family-groups/{id}/children/{memberId}` removes the child
- [ ] 6.6 Add failing test: HAL-Forms template on the family group detail response includes both `addParent` and `addChild` affordances when the current user has `MEMBERS:MANAGE`, and omits them otherwise
- [ ] 6.7 Replace `CreateFamilyGroupRequest` with the domain `FamilyGroup.CreateFamilyGroup` record bound via `@RequestBody`; reuse the existing `MemberIdMixin` so `MemberId` deserializes from a UUID string (mirror the `EventController` / `CalendarController` pattern)
- [ ] 6.8 Add `POST /api/family-groups/{id}/children` handler and `DELETE /api/family-groups/{id}/children/{memberId}` handler; both require `MEMBERS:MANAGE`
- [ ] 6.9 Update the family group detail HAL-Forms builder to emit `addChild` and `removeChild` affordances (add to the per-child representation; remove goes on each child)
- [ ] 6.10 Verify tests 6.1–6.6 pass

## 7. Training group — manual trainee exclusivity (TDD)

- [ ] 7.1 Add failing `TrainingGroupManagementServiceTest`: manually adding a member who is already a trainee of another training group throws `MemberAlreadyInTrainingGroupException`
- [ ] 7.2 Add failing test: manually adding a member who is not a trainee of any other training group succeeds
- [ ] 7.3 Add failing test: manually adding a member who is a **trainer** of another training group (but not a trainee anywhere) succeeds — trainer role is exempt
- [ ] 7.4 Add failing test: the existing automatic `assignEligibleMember(...)` path is NOT affected — auto-assign of an already-assigned member still follows the move-on-conflict behavior described in the existing spec
- [ ] 7.5 Create new exception `members/traininggroup/application/MemberAlreadyInTrainingGroupException.java` (or reuse the pattern from `MemberAlreadyInFamilyGroupException`)
- [ ] 7.6 Add a repository method on `TrainingGroupRepository` to find a training group by trainee member id (if not already present)
- [ ] 7.7 In `TrainingGroupManagementService.addMember(groupId, memberId)`, query the repository and throw the new exception on conflict before calling the aggregate
- [ ] 7.8 Map the new exception to a 409 (or 400) via the relevant exception handler
- [ ] 7.9 Verify tests 7.1–7.4 pass

## 8. Parent/child exclusivity — document and test the invariant

- [x] 8.1 Add failing `FamilyGroupTest` that explicitly asserts the invariant: "a single `FamilyGroup` aggregate cannot end up with the same `MemberId` present both as a parent and as a non-parent member through any legal sequence of domain operations"
- [x] 8.2 The test should cover: `create` → `addChild(parent)` rejection; `create(child)` → `addParent(child)` legitimately promotes in place without duplicating the member
- [x] 8.3 Verify the test passes (this should already be the case given the existing `UserGroup` data model, plus the `addChild` check from section 4)

## 9. Frontend — simplified family group create dialog

- [ ] 9.1 Locate the family group create dialog component in `frontend/src`
- [ ] 9.2 Remove the "initial members" picker and the multi-parent selector
- [ ] 9.3 Replace with a single-parent picker plus the group name field
- [ ] 9.4 Wire the dialog to the new `POST /api/family-groups` request shape (`{ name, parentId }`)
- [ ] 9.5 After successful create, navigate to the family group detail page (behavior exists elsewhere; confirm it also applies here)

## 10. Frontend — unified "Add member" button on family group detail

- [ ] 10.1 Replace the existing "Add parent" button on the family group detail page with a single "Add member" button
- [ ] 10.2 On click, open a dialog with name field + role picker (parent / child) + member picker
- [ ] 10.3 On submit, route to `POST /api/family-groups/{id}/parents` or `POST /api/family-groups/{id}/children` based on the selected role
- [ ] 10.4 Render the button only when the corresponding HAL-Forms affordance is present in the group detail response (i.e. the backend exposed `addParent` or `addChild`)
- [ ] 10.5 Add a "Remove" action next to each child row, wired to `DELETE /api/family-groups/{id}/children/{memberId}` (shown only if the backend emitted the `removeChild` affordance for that child)

## 11. Frontend — filter existing members out of member pickers

- [ ] 11.1 Identify the member-picker component used across family, training, and free group dialogs
- [ ] 11.2 Add a prop for "exclude these member ids" that filters the rendered options
- [ ] 11.3 Pass the current group's member and owner ids from each caller (family group detail, training group detail, free group detail)
- [ ] 11.4 For the "promote to owner" picker on free groups, additionally restrict to current members (consistent with the new spec rule)

## 12. Frontend QA walkthrough

- [ ] 12.1 Log in as admin (`ZBM9000`). Go to Členové, create a new family group with only a name and one parent → detail page opens automatically
- [ ] 12.2 On the family group detail page, click "Přidat člena", pick "Dítě", select a candidate → child appears in the list
- [ ] 12.3 Open the picker again — the already-added child and the parent do NOT appear in the candidate list
- [ ] 12.4 Click "Přidat člena", pick "Rodič", select another candidate → new parent is added
- [ ] 12.5 Remove the child via its row action → child disappears
- [ ] 12.6 Go to Skupiny (free groups), create a group, try to promote a non-member to owner → error is shown and no membership is created
- [ ] 12.7 Promote an existing member to owner → succeeds
- [ ] 12.8 Go to Tréninkové skupiny, create a training group, manually add a member → success. Try to add the same member to another training group manually → error "already trainee of another training group"
- [ ] 12.9 Assign the same member as trainer to a second training group → succeeds (trainer exemption)
- [ ] 12.10 Confirm existing backend and frontend test suites still pass via the `test-runner` agent

## 13. Follow-up task queue item (out of scope for this change)

- [x] 13.1 Queue a task for the separate `TrainingGroup.addTrainer` auto-member coupling bug. Already created: see `tasks/training-group-add-trainer-auto-member-bug.md`. The task is independently implementable before or after this proposal is archived.
