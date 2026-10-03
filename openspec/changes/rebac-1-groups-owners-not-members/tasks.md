# Tasks

## 1. Invariant in the shared group aggregate

- [ ] 1.1 Write failing unit tests for `MemberGroup` (via a minimal test subtype): promoting a member moves them to owners, removing an owner does not leave them as a member, adding an owner as member is rejected; verify they fail
- [ ] 1.2 Implement the invariant in `MemberGroup` (D1), add `OwnerCannotBeMemberException`, remove `OwnerCannotBeRemovedFromGroupException` and its handler mapping in `GroupsExceptionHandler`; verify 1.1 passes and `LegalGuardianGroup` tests stay green

## 2. Free group: creation, promotion, owner removal (vertical slice)

- [ ] 2.1 Write failing tests: `FreeGroup.create` has the creator only among owners; promotion moves the member; removing a co-owner removes them from the group; verify they fail
- [ ] 2.2 Implement in `FreeGroup` and verify domain tests pass; update `FreeGroupController` detail/list tests so owners are not expected in the member list and verify `@WebMvcTest`s pass
- [ ] 2.3 Frontend: free group detail page shows owners and members without duplicates; the remove-owner confirmation says the person will leave the group; verify with component tests and manually on http://localhost:3000

## 3. Free group: member leaves (vertical slice)

- [ ] 3.1 Update `docs/openapi/spec/groups.yaml` — `removeGroupMember` allowed for an owner or the member themselves; document the `leaveGroup` template on the free group detail; verify the bundle validates
- [ ] 3.2 Write failing domain and `@WebMvcTest` tests: member removes themselves → leaves; non-owner removing someone else → rejected; owner cannot use the leave path; verify they fail
- [ ] 3.3 Implement `FreeGroup.removeMember` self path, service and controller authorization, and the `leaveGroup` affordance shown only to non-owner members; verify 3.2 passes
- [ ] 3.4 Frontend: "Opustit skupinu" action with confirmation on the free group detail page; after leaving the user returns to the group list; verify with component tests and manually on http://localhost:3000
- [ ] 3.5 Verify a member who left can be re-invited (domain test)

## 4. Training groups (vertical slice)

- [ ] 4.1 Write failing tests: adding a trainee as trainer removes them from trainees; `assignEligibleMember` skips the group's trainer and publishes no event; removed trainer does not become a trainee; verify they fail
- [ ] 4.2 Implement in `TrainingGroup` (D3) and verify tests pass, including `TrainingGroupManagementService` and `MemberCreatedListener` tests
- [ ] 4.3 Exclude the group's trainers from the "add member" picker options and remove the obsolete "trainer cannot be removed from members" UI guard; verify with `@WebMvcTest` and frontend component tests

## 5. Data and integration

- [ ] 5.1 Review `example-data` bootstrap (`TrainingGroupDataBootstrap`, free group bootstrap) so no person is owner and member of the same group; verify the application starts with the `example-data` profile
- [ ] 5.2 Run the full backend test suite and frontend tests; verify all pass
- [ ] 5.3 E2E check on http://localhost:3000: create a free group (creator not listed as member), invite and accept, promote to owner (moved), remove co-owner (gone), member leaves (gone)
