# Team coordination file — refine-user-groups-management

**Created:** 2026-04-09
**Proposal:** `openspec/changes/refine-user-groups-management/`
**Team leader:** main agent (coordinator only, no code changes)

## Purpose

Subagents working on this proposal must read this file first to understand the current state, then append a concise summary of their work and any open issues / decisions at the end of the file so the next subagent can resume.

## Iteration plan (vertical slices)

1. **Iter 1 — Shared WithInvitations owner-promotion rule** (backend)
   - Tasks 1.x + 2.x from `tasks.md`
   - New `CannotPromoteNonMemberToOwnerException`, default method on `WithInvitations`, `MembersGroup.addOwner` routes through it, HTTP mapping, controller integration test.
2. **Iter 2 — FamilyGroup scalar create + addChild/removeChild domain** (backend)
   - Tasks 3.x, 4.x, 5.x, 8.x
   - `CreateFamilyGroup(name, parent)` scalar, `addChild`/`removeChild` on aggregate, `FamilyGroupManagementService` ports, parent/child exclusivity domain test.
3. **Iter 3 — FamilyGroup REST API** (backend)
   - Tasks 6.x
   - Replace `CreateFamilyGroupRequest` with domain record (mirroring EventController pattern), new `POST /children`, `DELETE /children/{memberId}`, HAL-Forms affordances.
4. **Iter 4 — TrainingGroup manual trainee exclusivity** (backend)
   - Tasks 7.x
   - `MemberAlreadyInTrainingGroupException`, repository lookup, service-level check, HTTP mapping.
5. **Iter 5 — Frontend: family group create + add-member button** (frontend)
   - Tasks 9.x + 10.x
6. **Iter 6 — Frontend: member picker filtering** (frontend)
   - Tasks 11.x
7. **Iter 7 — simplify + code review + full test run + commit polish** (coordinator driven)

Each iteration ends with: all new tests passing, `tasks.md` checkboxes ticked, git commit.

## Key decisions (from design.md, do not re-litigate)

- Owner-promotion rule lives on `WithInvitations` default method (Decision 1).
- New dedicated exception `CannotPromoteNonMemberToOwnerException` (Decision 2).
- `CreateFamilyGroup(String name, MemberId parent)` — scalar (Decision 3).
- Separate `/parents` and `/children` endpoints, not a unified `/members` with role field (Decision 4).
- `addChild` rejects members already parent of same group (Decision 5).
- Manual training trainee exclusivity enforced in **application service**, not domain (Decision 6).
- Member-picker filtering is **frontend-only** (Decision 7).

## Open questions

_(none at team-leader level — all decisions captured in design.md)_

## Out of scope (do NOT touch)

- `TrainingGroup.addTrainer` auto-member bug — separate task queue item.
- Automatic age-based training-group reassignment path.
- Any non-invitation owner-management rules.

---

## Iteration log

_(Subagents append entries below as they complete work.)_

---

### Iter 1 — 2026-04-09 — WithInvitations owner promotion rule (tasks 1.x + 2.x)

**Files created:**
- `backend/src/main/java/com/klabis/common/usergroup/CannotPromoteNonMemberToOwnerException.java` — new exception carrying `UserId`

**Files modified:**
- `backend/src/main/java/com/klabis/common/usergroup/WithInvitations.java` — added `hasMember(UserId)` and `addOwner(UserId)` abstract methods + `promoteOwner(UserId)` default method
- `backend/src/main/java/com/klabis/members/membersgroup/domain/MembersGroup.java` — implemented `hasMember(UserId)` and `addOwner(UserId)` overrides (delegate to composed `userGroup`); `addOwner(MemberId)` now calls `promoteOwner()` instead of `userGroup.addOwner()` directly
- `backend/src/main/java/com/klabis/members/membersgroup/infrastructure/restapi/MembersGroupExceptionHandler.java` — added `@ExceptionHandler` for `CannotPromoteNonMemberToOwnerException` → HTTP 409
- `backend/src/test/java/com/klabis/common/usergroup/UserGroupWithInvitationsTest.java` — added `promoteOwner()` nested test class (2 tests: reject non-member, accept existing member without duplicating membership)
- `backend/src/test/java/com/klabis/members/membersgroup/domain/MembersGroupTest.java` — added 2 tests to `OwnerManagement`: non-member throws, existing member succeeds without duplicate
- `backend/src/test/java/com/klabis/members/membersgroup/infrastructure/restapi/MembersGroupControllerTest.java` — added `shouldReturn409WhenPromotingNonMemberToOwner` inside `AddOwnerTests`

**Tests added:**
- `UserGroupWithInvitationsTest.PromoteOwnerMethod.shouldThrowWhenPromotingNonMember`
- `UserGroupWithInvitationsTest.PromoteOwnerMethod.shouldPromoteExistingMemberWithoutDuplicatingMembership`
- `MembersGroupTest.OwnerManagement.shouldThrowWhenPromotingNonMemberToOwner`
- `MembersGroupTest.OwnerManagement.shouldPromoteExistingMemberToOwnerWithoutDuplicatingMembership`
- `MembersGroupControllerTest.AddOwnerTests.shouldReturn409WhenPromotingNonMemberToOwner`

**Design note:** `WithInvitations` now declares `hasMember(UserId)` and `addOwner(UserId)` as abstract methods so the `promoteOwner` default can call them without casting. `MembersGroup` implements them by delegating to its composed `userGroup`. `InvitationGroup` test double in `UserGroupWithInvitationsTest` inherits them from `UserGroup` (it extends it directly).

**Surprises / deviations:**
- `MembersGroup` composes `UserGroup` rather than extending it, so the two new abstract methods needed explicit `@Override` implementations — not immediately obvious from reading the interface alone.
- Existing `MembersGroupTest.shouldAddOwner` test was already correct (it invites the member first before promoting), so no changes needed to existing tests.

**Result:** 2061/2061 tests pass. All 1.x and 2.x checkboxes ticked.
