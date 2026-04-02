## 1. Free Group CRUD (Slice 1a)

- [x] 1.1 Create `user-groups` module structure: `package-info.java` (@ApplicationModule), domain/, application/, infrastructure/ packages
- [x] 1.2 Domain model: `UserGroupId` value object, `GroupMembership` value object, `UserGroup` abstract aggregate root (id, name, owners, members, addMember, removeMember)
- [x] 1.3 Domain model: `FreeGroup` concrete aggregate extending `UserGroup` (no additional fields yet, invitation support added in Slice 1b)
- [x] 1.4 Domain repository interface: `UserGroupRepository` (save, findById, findAll, delete)
- [x] 1.5 DB migration: `user_groups` table (single table inheritance with type discriminator), `user_group_owners` table, `user_group_members` table
- [x] 1.6 JDBC persistence: `UserGroupMemento` (maps to user_groups table), `UserGroupJdbcRepository`, `UserGroupRepositoryAdapter`
- [x] 1.7 Application service: `GroupManagementService` — create free group, edit name, delete group, add/remove members
- [x] 1.8 REST API: `GroupController` — POST create free group, GET list free groups, GET group detail, PATCH edit name, DELETE group, POST add member, DELETE remove member (HAL+FORMS)
- [x] 1.9 Root API link: add `groups` link to root API response for navigation
- [x] 1.10 Frontend: add "Skupiny" navigation item, create groups list page (free groups where user is member), group detail page (members, edit name, delete), create group form
- [x] 1.11 Frontend: add/remove member actions on group detail page
- [x] 1.12 Tests: domain unit tests (FreeGroup creation, add/remove member, invariants), integration tests (repository, controller), Spring Modulith module test

## 2. Free Group Invitations (Slice 1b)

- [x] 2.1 Domain model: `Invitation` entity, `InvitationId` value object, `InvitationStatus` enum (PENDING, ACCEPTED, REJECTED)
- [x] 2.2 Extend `FreeGroup`: `invite(MemberId invitedBy, MemberId target)`, `acceptInvitation(InvitationId)`, `rejectInvitation(InvitationId)`, pending invitations collection
- [x] 2.3 DB migration: `invitations` table (id, group_id, invited_member_id, invited_by_member_id, status, created_at)
- [x] 2.4 Extend JDBC persistence: invitation mapping in `UserGroupMemento`, query for pending invitations by member
- [x] 2.5 Application service: `InvitationService` — invite member, accept invitation, reject invitation
- [x] 2.6 REST API: `InvitationController` — POST invite member, POST accept invitation, POST reject invitation, GET my pending invitations (HAL+FORMS)
- [x] 2.7 Frontend: pending invitations section on groups list page (accept/reject actions), invitation management on group detail page (owner view)
- [x] 2.8 Tests: domain unit tests (invitation lifecycle, duplicate invite, accept/reject), integration tests (controller, repository)

## 3. Training Groups (Slice 2)

- [x] 3.1 Add `GROUPS_TRAINING` authority to `Authority` enum (scope GLOBAL)
- [x] 3.2 Domain model: `TrainingGroup` concrete aggregate, `AgeRange` value object (minAge, maxAge, includes(age), overlaps(AgeRange))
- [x] 3.3 Application service: create training group with disjunktnost validation, edit age range with overlap check, delete training group, manual add/remove member with exclusive membership enforcement
- [x] 3.4 DB migration: add age_range_min, age_range_max columns (already in initial migration, verify nullable for non-TRAINING types)
- [x] 3.5 REST API: training group endpoints on `GroupController` — POST create, GET list, GET detail, PATCH edit, DELETE (require GROUPS:TRAINING authority)
- [x] 3.6 Frontend: dedicated training groups page (accessible only with GROUPS:TRAINING), list with age ranges and member counts, create/edit/delete forms
- [x] 3.7 Frontend: training group detail — member list, manual add/remove member
- [x] 3.8 Tests: domain unit tests (AgeRange overlap, exclusive membership), integration tests (disjunktnost validation, authorization), Spring Modulith module test

## 4. Training Group Automations (Slice 3)

- [x] 4.1 Event listener: `MemberCreatedListener` — listen to `MemberCreatedEvent`, auto-assign to matching training group by age
- [x] 4.2 Domain events: publish events on member reassignment (for future notifications)
- [x] 4.3 Backend: add training group info to member detail response (group name, owner name + contact)
- [x] 4.4 Frontend: training group section on member profile page (group name, owner contact info; hidden if not assigned)
- [x] 4.5 Tests: listener integration test (MemberCreatedEvent → auto-assignment), member detail response test

## 5. Family Groups (Slice 4)

- [x] 5.1 Domain model: `FamilyGroup` concrete aggregate extending `UserGroup`
- [x] 5.2 Application service: create family group (requires MEMBERS:MANAGE), exclusive membership enforcement (max 1 family group per member), delete family group
- [x] 5.3 REST API: family group endpoints — POST create (from member selection), GET list, GET detail, DELETE (require MEMBERS:MANAGE)
- [x] 5.4 Frontend: create family group from members list page (select members, designate owner), family group info section on member profile page
- [x] 5.5 Tests: domain unit tests (exclusive membership), integration tests (authorization, creation from member list)

## 6. Owner Management + Members Integration (Slice 5)

- [x] 6.1 Domain logic: `addOwner(MemberId)`, `removeOwner(MemberId)` on `UserGroup` with last-owner validation (reject removal of sole owner)
- [x] 6.2 REST API: owner management endpoints — POST add owner, DELETE remove owner (HAL+FORMS affordances on group detail)
- [x] 6.3 Frontend: owner management UI on group detail pages (add/remove owner, last-owner error display)
- [x] 6.4 Public query API: `UserGroups` interface at module root — `findGroupsWhereLastOwner(MemberId)` returning group type + group name
- [x] 6.5 Members integration: modify `ManagementService.suspendMember()` to call `UserGroups.findGroupsWhereLastOwner()` before suspension
- [x] 6.6 Backend: return warning response when suspending last owner (training: require successor, family/free: successor or dissolve)
- [x] 6.7 Frontend: suspension warning dialog showing affected groups with resolution options (designate successor / dissolve group)
- [x] 6.8 Tests: domain unit tests (last-owner invariant), integration tests (suspension with group ownership, query API), cross-module Spring Modulith test
