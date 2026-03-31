# TCF: Free Group Invitations (Slice 1b)

**Datum:** 2026-03-31
**Scope:** Tasks 2.1 - 2.8

## Tasks

- [x] 2.1 Domain model: Invitation entity, InvitationId value object, InvitationStatus enum (PENDING, ACCEPTED, REJECTED)
- [x] 2.2 Extend FreeGroup: invite(), acceptInvitation(), rejectInvitation(), pending invitations collection
- [x] 2.3 DB migration: invitations table
- [x] 2.4 Extend JDBC persistence: invitation mapping in UserGroupMemento, query for pending invitations by member
- [x] 2.5 Application service: InvitationService — invite member, accept invitation, reject invitation
- [x] 2.6 REST API: InvitationController — POST invite, POST accept, POST reject, GET my pending invitations (HAL+FORMS)
- [x] 2.7 Frontend: pending invitations section on groups list page, invitation management on group detail page
- [x] 2.8 Tests: domain unit tests, integration tests

## Iteration Log

### 2026-03-31 — Tasks 2.1–2.5 (backend-developer agent)

**Domain model (2.1):**
- `InvitationId` — value object record (`UUID value`), `@ValueObject`, implements `Identifier`, has `newId()` factory
- `InvitationStatus` — enum: `PENDING`, `ACCEPTED`, `REJECTED`
- `Invitation` — `@Entity`, public class, package `domain`, identity-based equality on `InvitationId`; package-private mutation methods (`accept()`, `reject()`); public getters for infrastructure layer

**FreeGroup extensions (2.2):**
- Added `Set<Invitation> invitations` field (initialized empty on create)
- `invite(invitedBy, target)` — validates no duplicate pending invite for same target member; throws `DuplicatePendingInvitationException`
- `acceptInvitation(InvitationId)` — finds pending invitation, marks ACCEPTED, calls `addMember()`; throws `InvitationNotFoundException` if not found
- `rejectInvitation(InvitationId)` — finds pending invitation, marks REJECTED
- `getPendingInvitations()` — returns filtered list of pending invitations
- `getInvitations()` — returns unmodifiable set of all invitations
- `isInvitedMember(InvitationId, MemberId)` — query helper for application layer
- `reconstruct()` overloaded: backward-compatible overload (no invitations = empty set) + full overload with invitations

**DB migration (2.3):**
- Added `invitations` table to `V001__initial_schema.sql` (section 13): `id UUID PK`, `group_id UUID FK → user_groups`, `invited_member_id UUID`, `invited_by_member_id UUID`, `status VARCHAR(20)`, `created_at TIMESTAMP`
- Indexes on `group_id`, `invited_member_id`, `status`

**Persistence (2.4):**
- `InvitationMemento` — `@Table("invitations")`, `from(Invitation)` / `toInvitation()` mapping
- `UserGroupMemento` — added `@MappedCollection(idColumn = "group_id") Set<InvitationMemento> invitations`; `from()` populates invitations for `FreeGroup` instances; `toUserGroup()` reconstructs invitations in `FREE` branch
- `UserGroupRepository` — added `findAllWithPendingInvitationForMember(MemberId)`
- `UserGroupJdbcRepository` — added `@Query` joining `invitations` on `status = 'PENDING'`
- `UserGroupRepositoryAdapter` — delegates to new JDBC query

**Application service (2.5):**
- `InvitationPort` — `@PrimaryPort` interface: `inviteMember`, `acceptInvitation`, `rejectInvitation`, `getGroupsWithPendingInvitations`
- `InvitationService` — `@Service` implementation; owner guard on `inviteMember`; invited-member guard on accept/reject via `FreeGroup.isInvitedMember()`; throws `NotGroupOwnerException`, `NotInvitedMemberException`, `GroupNotFoundException`
- `NotInvitedMemberException` — application-layer exception for wrong member attempting to act on invitation

### 2026-03-31 — Compilation verification (backend-developer agent)

Investigated reported compilation errors in `InvitationMemento`, `UserGroupMemento`, `InvitationService`, and `UserGroupRepositoryAdapter`. Found that all reported issues were already resolved in the implementation:

- `Invitation` class is `public` (line 12) — accessible from `infrastructure.jdbc` package
- All getters (`getId`, `getInvitedMember`, `getInvitedBy`, `getStatus`, `getCreatedAt`) are `public` — no access issues
- `UserGroupRepositoryAdapter.findAllWithPendingInvitationForMember(MemberId)` is fully implemented, delegates to `UserGroupJdbcRepository`
- Both `./gradlew compileJava` and `./gradlew compileTestJava` complete with `BUILD SUCCESSFUL`

No code changes were needed — the implementation is correct as written.

### 2026-03-31 — Task 2.6 REST API (backend-developer agent)

**InvitationController (2.6):**
- `InvitationController` — `@PrimaryAdapter @RestController`, package-private class in `infrastructure/restapi`
- `POST /api/groups/{groupId}/invitations` — owner invites member; delegates to `InvitationPort.inviteMember()`; returns 204
- `POST /api/groups/{groupId}/invitations/{invitationId}/accept` — invited member accepts; returns 204
- `POST /api/groups/{groupId}/invitations/{invitationId}/reject` — invited member rejects; returns 204
- `GET /api/invitations/pending` — returns `CollectionModel<EntityModel<PendingInvitationResponse>>` with accept/reject affordance links per invitation
- All endpoints require member profile (`isMember()` check), throw `MemberProfileRequiredException` → 403 for non-members
- Authorization enforced by existing `InvitationPort` (owner guard on invite, invited-member guard on accept/reject)

**GroupController extensions (2.6):**
- `getGroup()` now adds `inviteMember` affordance to self link for FreeGroup owners
- `toGroupResponse()` now includes `pendingInvitations` list for FreeGroup owners (empty list for non-owners/non-FreeGroups)
- `buildPendingInvitationModel()` — shared helper used by both `GroupController` and `InvitationController`
- `GroupResponse` — added `pendingInvitations` field

**Request/Response DTOs (2.6):**
- `InviteMemberRequest` — `@NotNull UUID memberId`
- `PendingInvitationResponse` — `groupId`, `groupName`, `invitationId`, `invitedBy`; `@Relation(collectionRelation = "pendingInvitationResponseList")`

**Visibility changes (2.6):**
- `NotInvitedMemberException` — made `public class` with `public` constructor (needed by tests in different package)

**Tests (2.6):**
- `InvitationControllerTest` — 19 tests covering all 4 endpoints: happy path, 400 (business rule), 401 (unauthenticated), 403 (no member profile), 404 (group not found)
- All 51 tests pass (32 existing GroupControllerTest + 19 new)

### 2026-03-31 — Compilation error investigation (backend-developer agent)

Investigated reported compilation errors in `InvitationController`, `GroupController`, and `InvitationControllerTest`. All reported errors were already resolved in the implementation:

- `@MvcComponent` annotation is correctly present on `GroupsRootPostprocessor` (not on `InvitationController` which uses `@RestController` correctly — only postprocessors use `@MvcComponent`)
- `andAffordances()` calls are correct — the existing code uses the singular chained `.andAffordances()` pattern with `klabisAfford()` helpers, which is the correct Klabis pattern
- `GroupResponse` constructor already accepts `pendingInvitations` as 5th parameter (added in task 2.6)
- `buildPendingInvitationModel()` is correctly defined in `GroupController` (private method, lines 241–262)
- `NotInvitedMemberException` is `public class` with `public` constructor — visible from test package
- Both `./gradlew classes` and `./gradlew compileTestJava` complete with `BUILD SUCCESSFUL`
- All 51 tests pass: 32 GroupControllerTest + 19 InvitationControllerTest

### 2026-03-31 — Task 2.7 Frontend (frontend-developer agent)

**Groups list page — pending invitations section (2.7):**
- `PendingInvitationsSection` — component fetches `GET /api/invitations/pending` via `useAuthorizedQuery`; renders only when `pendingInvitationResponseList` is non-empty
- Shows group name per invitation; "Přijmout" (accept) and "Odmítnout" (reject) buttons calling the respective HAL `accept`/`reject` link URLs via `useAuthorizedMutation` with POST
- On success: invalidates all queries and refetches the groups resource

**Group detail page — invitation management (2.7):**
- `GroupDetail` type extended with `pendingInvitations?: PendingInvitation[]` field (returned from HAL response for group owners)
- Pending invitations section rendered as a `Card` when `resourceData.pendingInvitations` is non-empty; shows section heading "ČEKAJÍCÍ POZVÁNKY"
- "Pozvat člena" button rendered when `inviteMember` template exists in `_templates`; opens modal with `HalFormDisplay` pointing at the template

**Labels added (localization/labels.ts):**
- `templates.inviteMember` → 'Pozvat člena'
- `sections.pendingInvitations` → 'ČEKAJÍCÍ POZVÁNKY'
- `buttons.accept` → 'Přijmout'
- `buttons.reject` → 'Odmítnout'

**Tests (TDD red-green cycle):**
- `GroupsPage.test.tsx` — 7 new tests covering: section heading visibility, group name display, accept/reject buttons, mutate call verification with correct URLs
- `GroupDetailPage.test.tsx` — 6 new tests covering: pending invitations section visibility (with/without data), invite member button visibility and modal trigger
- All 946 frontend tests pass; `npm run build` succeeds

### 2026-03-31 — Task 2.8 Domain unit tests (backend-developer agent)

**Domain unit tests (2.8):**
- Extended `FreeGroupTest.java` with 10 new tests in 5 nested `@DisplayName` groups covering all invitation behaviors
- `invite()` — happy path (pending invitation created with correct member and status), duplicate throws `DuplicatePendingInvitationException`, re-invite after rejection succeeds
- `acceptInvitation()` — happy path (member added, status ACCEPTED, no longer in pending list), not-found throws `InvitationNotFoundException` with ID in message
- `rejectInvitation()` — happy path (member NOT added, status REJECTED, no longer in pending list), not-found throws `InvitationNotFoundException`
- `getPendingInvitations()` — returns only PENDING invitations, excluding ACCEPTED
- `isInvitedMember()` — true for correct member+invitation, false for wrong member, false for unknown invitation ID
- All 26 `FreeGroupTest` tests pass (16 pre-existing + 10 new)

### 2026-03-31 — Code review fixes (frontend-developer agent)

**[HIGH] Duplicate `onSuccess` removed (`GroupsPage.tsx`):**
- Removed global `onSuccess` from `useAuthorizedMutation` options; the per-call callback in `mutate()` is the correct pattern per TanStack Query + StrictMode guidance (fires exactly once)

**[HIGH] Shared `PendingInvitation` type (`src/pages/groups/types.ts`):**
- Extracted to `frontend/src/pages/groups/types.ts`; includes `_links.member?: HalResourceLinks` for invited member resolution
- Both `GroupsPage.tsx` and `GroupDetailPage.tsx` now import from the shared type; the local duplicate definitions are removed

**[HIGH] Pending invitations display in `GroupDetailPage.tsx`:**
- Backend `InvitationModelBuilder` updated to add `_links.member` pointing to `/api/members/{invitedMemberUuid}` for each pending invitation
- Frontend displays invited member name using `HalRouteProvider` + `MemberNameWithRegNumber`; falls back to `invitationId` when member link is absent
- 2 new tests: member name resolved via HAL link; fallback to `invitationId` when link absent

**[MEDIUM] Targeted cache invalidation (`GroupsPage.tsx`):**
- Replaced `queryClient.invalidateQueries()` (all queries) with targeted invalidation by query key prefix: `['authorized', '/api/invitations/pending']` and `['authorized', '/api/groups']`

**Verification:** TypeScript `--noEmit` clean; all 948 frontend tests pass (+2 new tests)

### 2026-03-31 — Code review fixes (backend-developer agent)

**[HIGH] Invitation state transition validation:**
- `Invitation.accept()` and `reject()` now validate `status == PENDING` before transitioning; throw `IllegalStateException` if not PENDING
- The `FreeGroup.findPendingInvitation()` guard already prevented this in practice, but the `Invitation` entity now enforces its own invariant directly

**[HIGH] Duplicate buildPendingInvitationModel logic:**
- Extracted shared logic into a new package-private `InvitationModelBuilder` class in `infrastructure/restapi`
- Both `GroupController` and `InvitationController` now delegate to `InvitationModelBuilder.buildPendingInvitationModel(UserGroup, Invitation)`
- `GroupController`'s private wrapper method kept as a trivial delegate; `InvitationController`'s local method removed entirely

**[MEDIUM] InvitationMemento status as enum:**
- Changed `String status` field → `InvitationStatus status` in `InvitationMemento`; Spring Data JDBC maps enums by name natively
- Removed manual `.name()` in `from()` and `InvitationStatus.valueOf()` in `toInvitation()`

**[LOW] Unused import in FreeGroupTest.java:**
- `import java.util.List` is actively used (e.g. `List<Invitation> pending = group.getPendingInvitations()`) — no removal needed

**Verification:** All 1714 backend tests pass.
