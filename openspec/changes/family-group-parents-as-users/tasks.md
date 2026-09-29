## 1. Domain and persistence on UserId (refactor slice, existing behavior preserved)

- [ ] 1.1 Update `FamilyGroupTest` to `UserId` parents / `MemberId` children API (red), incl. role exclusivity, last parent, duplicate membership
- [ ] 1.2 Change `FamilyGroup` to `MemberGroup<FamilyGroup, FamilyGroupId, UserId>`: `create`/`addParent`/`removeParent`/`isLastParent` take `UserId`, `addChild`/`removeChild` take `MemberId` (converted via `toUserId()`), `getParents(): Set<UserId>`, `getChildren()` exposes `MemberId`
- [ ] 1.3 Rename `user_group_owners.member_id` to `owner_id` directly in `V001__initial_schema.sql` (column, PK, index); update `GroupOwnerMemento` and `GroupJdbcRepository` queries; verify `GroupsCoexistenceTest` for Free/Training groups
- [ ] 1.4 Switch `FamilyGroupRepositoryAdapter` to `UserId::uuid` / `UserId::new`; `FamilyGroupFilter.memberOrParentIs` to `UserId`; `MemberAlreadyInFamilyGroupException` to `UserId`; update `FamilyGroupPersistenceTest`
- [ ] 1.5 Adapt `FamilyGroupManagementPort`/`FamilyGroupManagementService` (`addParent`/`removeParent` with `UserId`, `validateNoExistingFamilyGroup` over `UserId`) and `FamilyGroupManagementServiceTest`
- [ ] 1.6 Adapt `FamilyGroupSuspensionBlockersAdapter` and `MemberFamilyGroupLinkProcessor` to map `MemberId.toUserId()`; update their tests

## 2. Create family group with a non-member parent (vertical slice)

- [ ] 2.1 Update `docs/openapi/spec/groups.yaml`: `CreateFamilyGroupRequest.parent` with `x-hal-input-type: UserId`; regenerate bundle (`klabis-full.json`) and frontend types
- [ ] 2.2 Write failing `FamilyGroupControllerTest` cases: create with member parent, create with user without member profile, create with parent already in a family group (409)
- [ ] 2.3 Update `FamilyGroupController.createFamilyGroup` to use `UserId`; verify tests green

## 3. Add and remove parent by userId (vertical slice)

- [ ] 3.1 Spec: add `AddParentRequest{userId}` (`AddMemberRequest` stays for children), remove-parent path `{userId}`; regenerate bundle and frontend types
- [ ] 3.2 Write failing controller tests: add non-member parent, remove parent, reject removing last parent, promote existing child to parent keeps single entry
- [ ] 3.3 Update `addFamilyGroupParent` / `removeFamilyGroupParent` in `FamilyGroupController`, affordances for `addFamilyGroupParent` (field `userId`, options `listMemberOptions`)

## 4. Group detail: parents without member link, access by userId (vertical slice)

- [ ] 4.1 Spec: `ParentResponse{userId}`; regenerate bundle and types
- [ ] 4.2 Write failing tests: parent without member profile views group detail (200), non-participant denied (403), `MEMBERS:MANAGE` allowed, parents listed as `userId` without `member` link, children keep `memberId`
- [ ] 4.3 Update `getFamilyGroup` authorization to `MEMBERS:MANAGE` or `group.hasMember(currentUser.userId())`; remove `member` link from parent items in `toFamilyGroupResponse`

## 5. Frontend

- [ ] 5.1 Adapt `FamilyGroupDetailPage` and `FamilyGroupsPage` (parents list by `userId`, no member link for parents, add-parent form field `userId`); update `FamilyGroupDetailPage.parents.test.tsx`, `FamilyGroupDetailPage.addMember.test.tsx`, related tests and labels
- [ ] 5.2 Run frontend tests and `npm run build`

## 6. Children and member profile integration (vertical slice)

- [ ] 6.1 Verify child add/remove flow (`MemberId` API) and family group link on member detail (`MemberFamilyGroupLinkProcessor`) by tests, incl. child already in another group and parent-vs-child role conflict
- [ ] 6.2 Verify member suspension blockers for family group with a member parent (last parent) and that a non-member parent does not affect suspension

## 7. Specification and finalization

- [ ] 7.1 Update `docs/openapi` bundle consistency and run full backend suite sequentially with `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true` (test-runner agent)
- [ ] 7.2 Run code review agent, address findings
- [ ] 7.3 Sync delta spec into `openspec/specs/user-groups/spec.md` and archive the change
