# Tasks

## 1. Invariant in the shared group aggregate

- [x] 1.1 Write failing unit tests for `MemberGroup` (via a minimal test subtype): promoting a member moves them to owners, removing an owner does not leave them as a member, adding an owner as member is rejected; verify they fail
- [x] 1.2 Implement the invariant in `MemberGroup` (D1), add `OwnerCannotBeMemberException`, remove `OwnerCannotBeRemovedFromGroupException` and its handler mapping in `GroupsExceptionHandler`; verify 1.1 passes and `LegalGuardianGroup` tests stay green

## 2. Free group: creation, promotion, owner removal (vertical slice)

- [x] 2.1 Write failing tests: `FreeGroup.create` has the creator only among owners; promotion moves the member; removing a co-owner removes them from the group; verify they fail
- [x] 2.2 Implement in `FreeGroup` and verify domain tests pass; update `FreeGroupController` detail/list tests so owners are not expected in the member list and verify `@WebMvcTest`s pass
- [x] 2.3 Frontend: free group detail page shows owners and members without duplicates; the remove-owner confirmation says the person will leave the group — component tests green
      - Verified at the API + component level. The manual `http://localhost:3000` pass is still outstanding: no browser was attached to this session, so the rendered UI was not eyeballed.

## 3. Free group: member leaves (vertical slice)

- [x] 3.1 Update `docs/openapi/spec/groups.yaml` — `removeGroupMember` allowed for an owner or the member themselves; document the `leaveGroup` template on the free group detail; verify the bundle validates
- [x] 3.2 Write failing domain and `@WebMvcTest` tests: member removes themselves → leaves; non-owner removing someone else → rejected; owner cannot use the leave path; verify they fail
- [x] 3.3 Implement `FreeGroup.removeMember` self path, service and controller authorization, and the `leaveGroup` affordance shown only to non-owner members; verify 3.2 passes
- [x] 3.4 Frontend: "Opustit skupinu" action with confirmation on the free group detail page; after leaving the user returns to the group list — component tests green
      - Manual `http://localhost:3000` pass outstanding, as in 2.3.
- [x] 3.5 Verify a member who left can be re-invited (domain test)

## 4. Training groups (vertical slice)

- [x] 4.1 Write failing tests: adding a trainee as trainer removes them from trainees; `assignEligibleMember` skips the group's trainer and publishes no event; removed trainer does not become a trainee; verify they fail
- [x] 4.2 Implement in `TrainingGroup` (D3) and verify tests pass, including `TrainingGroupManagementService` and `MemberCreatedListener` tests
- [x] 4.3 Exclude the group's trainers from the "add member" picker options and remove the obsolete "trainer cannot be removed from members" UI guard; verify with `@WebMvcTest` and frontend component tests

## 5. Data and integration

- [x] 5.1 Review `example-data` bootstrap (`TrainingGroupDataBootstrap`, free group bootstrap) so no person is owner and member of the same group; verify the application starts with the `example-data` profile
  - No free-group bootstrap exists — free groups are only created through the API, which now uses the owner-only creator path. `TrainingGroupDataBootstrap` uses `TrainingGroup.create`, already owner-only, and the age-based listener now skips trainers.
- [x] 5.2 Run the full backend test suite and frontend tests; verify all pass — backend 3998 run / 0 failures / 14 pre-existing skips; frontend 2133 run / 0 failures; eslint 0 errors; tsc clean
- [x] 5.3 E2E check: create a free group (creator not listed as member), invite and accept, promote to owner (moved), remove co-owner (gone), member leaves (gone)
  - Run against a live backend via the HAL+FORMS API rather than a browser click-through — no browser was attached to this session. All 22 checks pass, covering the listed flow plus the authorization boundaries: a member is offered `leaveGroup` targeting their own id, an owner is not, an owner cannot use the leave path (400 "not in the group"), and a member removing someone else is rejected (403).
  - The rendered UI on `http://localhost:3000` still deserves one human pass for tasks 2.3 and 3.4.
