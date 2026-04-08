## Context

The `user-groups` capability is implemented across three aggregate roots (`FamilyGroup`, `TrainingGroup`, `MembersGroup`) that all embed a shared `common.usergroup.UserGroup` building block. `UserGroup` owns the raw set of owners and `GroupMembership` and exposes `addOwner` / `addMember` / `removeOwner` / `removeMember`. A second shared building block, `WithInvitations`, is an interface implemented by `MembersGroup` to model the invitation-based membership flow.

Today the three aggregates embed `UserGroup` directly and delegate owner/member operations to it. `UserGroup.addOwner` has a side effect: if the candidate is not already a member, it silently adds them. That side effect is correct for family groups (where parent = owner + member by definition) and irrelevant for training groups (trainers are not members), but **wrong for invitation-based groups**, where it bypasses the whole invitation flow.

A related design tension shows up on the family group side: the `CreateFamilyGroup` command accepts sets of parents and initial members, but in practice users always want "name + one parent" and then manage the rest incrementally from the detail page. The detail page is currently a dead end for non-parent members because the backend has no `addChild` / `removeChild` endpoints at all — children can only ever be set at creation time.

## Goals / Non-Goals

**Goals:**
- Make owner promotion on invitation-based groups honor the invitation rule, without copy-pasting the check into every aggregate that uses invitations.
- Fill the missing `addChild` / `removeChild` endpoints for family groups so the detail page is actually manageable.
- Simplify family group creation to "name + one parent" with a break-compatible API change (acceptable because the system has no production deployment).
- Close the training-group manual-add loophole so the "one trainee per training group" rule from the spec is actually enforced.
- Document the parent/child exclusivity invariant that the data model accidentally enforces today.

**Non-Goals:**
- Any rework of the automatic age-based training-group assignment logic.
- Fixing the `addTrainer` auto-member side effect (tracked as a separate task queue item because it is a different bug with its own implications).
- Introducing new group types or new membership states.
- Any server-side filtering for member-picker dropdowns — filtering is purely a frontend concern using data already returned by existing endpoints.

## Decisions

### Decision 1: Owner-promotion rule lives on `WithInvitations`, not on each aggregate

**Choice:** Move the "owner must already be a member" check into the `WithInvitations` interface (as a default method or via a small internal helper) so any aggregate that opts into invitation-based membership automatically inherits the rule. `MembersGroup.addOwner` routes through this shared path; other aggregates keep their existing `UserGroup.addOwner` behavior.

**Why:** The rule is conceptually tied to the presence of an invitation flow: groups that use invitations cannot have people appear out of thin air, whether as members or as owners. Tying it to `WithInvitations` means:
- Future group types that implement `WithInvitations` get the rule for free.
- Family groups and training groups, which legitimately grant ownership to non-members (parent creation, trainer assignment), are not accidentally constrained.
- The intent ("owners can only come from the membership of an invitation-based group") is documented by the type hierarchy instead of scattered across aggregates.

**Alternatives considered:**
- **Check inside `MembersGroup.addOwner` directly.** Simpler, but leaks the rule into one aggregate and leaves the next `WithInvitations` implementation to forget it.
- **Check inside `UserGroup.addOwner`.** Wrong layer — `UserGroup` has no knowledge of invitations and is shared with group types where the rule must not apply.
- **Replace `UserGroup.addOwner`'s silent auto-add entirely.** Would break family groups, where parent = owner + member is the point.

### Decision 2: New dedicated exception, not a reused one

**Choice:** Introduce `CannotPromoteNonMemberToOwnerException` in `common.usergroup`. It carries the offending `UserId` / `MemberId`.

**Why:** The existing exceptions (`NotInvitedMemberException`, `DirectMemberAdditionNotAllowedException`, `MemberAlreadyInGroupException`) all describe different failure modes on the invitation/membership path. Reusing them would muddy the semantics and make controller-level error handling ambiguous.

**Alternatives considered:**
- **Reuse `NotInvitedMemberException`** — semantically about accepting invitations, not about promotion.
- **Throw `IllegalStateException`** — untyped, cannot be mapped cleanly to an HTTP status by the exception handler.

### Decision 3: Scalar `parent` in `CreateFamilyGroup`, not a size-1 collection

**Choice:** The domain command record becomes `CreateFamilyGroup(String name, MemberId parent)`. The request DTO on the controller collapses to a single `parentId` field of type `MemberId` (deserialized via the existing `MemberIdMixin`). The controller forwards the record directly to the application service as `@RequestBody`, following the pattern already used by `EventController`, `CalendarController`, and `CategoryPresetController`.

**Why:** A scalar makes the invariant "exactly one parent at creation time" impossible to violate at compile time and removes any ambiguity for clients. The existing pattern of binding `@RequestBody` directly onto the domain command record eliminates a translation layer and keeps the validation annotations in one place.

**Alternatives considered:**
- **Keep `Set<MemberId> parents` with `@Size(min = 1, max = 1)`** — runtime-only, allows clients to submit empty or multi-element sets and get a validation error rather than a type error.
- **Keep a separate request DTO** — adds a pointless second type when the domain command can be deserialized directly. The other controllers in the project prove this works.

### Decision 4: `addChild` as a separate endpoint, not an `/members` router with a role field

**Choice:** The controller exposes `POST /api/family-groups/{id}/parents` (existing), `POST /api/family-groups/{id}/children` (new), and `DELETE /api/family-groups/{id}/children/{memberId}` (new). The frontend's sole "Add member" button reads the HAL-Forms templates for both affordances and shows a role picker that routes to the right URL.

**Why:** Symmetry with the existing `/parents` endpoint, and HAL-Forms can describe each affordance independently including its own authorization constraints. A single `/members` endpoint with a `role` body field would require custom discriminator logic in the DTO and couple two different operations that may grow apart over time (for example, adding a child might one day require different validation than promoting a parent).

**Alternatives considered:**
- **Unified `POST /members` with `{memberId, role}`** — rejected for the reasons above.
- **PATCH-style partial updates on the group resource** — too heavy for a simple "add one member" operation and does not fit HAL-Forms affordances.

### Decision 5: `addChild` rejects members who are already a parent of the same family group

**Choice:** `FamilyGroup.addChild(MemberId)` checks that the candidate is not already an owner of the same group. If they are, it throws (new typed exception or reused `MemberAlreadyInGroupException` — implementation decision made in tasks.md based on what reads best). The reverse direction — `addParent` on an existing child — is already covered by `UserGroup.addOwner` turning the child into an owner while preserving membership.

**Why:** The entire parent/child exclusivity invariant relies on never calling `addMember` on someone who is already an owner. `UserGroup.addOwner` handles the "promote member to owner" direction correctly; `addChild` (a plain `addMember` semantically) is the only path where a stray "demote owner to child" could happen. An explicit check there closes the loop.

### Decision 6: Manual training group trainee exclusivity is enforced in the application service, not the domain

**Choice:** `TrainingGroupManagementService.addMember(groupId, memberId)` queries the repository for "any training group containing this member as a trainee" before calling `TrainingGroup.addMember`. If another group is found, a new `MemberAlreadyInTrainingGroupException` is thrown and the add is rejected. The aggregate itself stays oblivious to the rule.

**Why:** The "one trainee per training group" invariant is a cross-aggregate constraint — the aggregate root for a single training group cannot see the others. The correct home is the application service, which already owns coordination between aggregates and the repository. This also keeps the existing `assignEligibleMember` (auto-assignment) path untouched, preserving the "move on conflict" behavior described in the current spec.

**Alternatives considered:**
- **Encode the rule in the domain via a domain service.** Over-engineering for a single use case.
- **Check in a pre-persist listener / database constraint.** Loses the clean error path and makes the rejection less explicit at the application boundary.

### Decision 7: Member-picker filtering is frontend-only

**Choice:** The frontend member-picker components (used by family group, training group, and free group dialogs) load the full club member list from the existing `/api/members` endpoint and filter out members that already appear in the target group's detail response before rendering the dropdown. No new backend endpoint is introduced.

**Why:** The detail responses already carry the full membership list for any group where the operation is possible. Reusing that data keeps the backend surface small and lets the frontend render instant updates without extra round-trips.

**Alternatives considered:**
- **New `GET /api/.../candidates` endpoint per group type.** Explicit but duplicates work that can be done client-side with data already in hand.
- **Server-side filter on `/api/members`.** Couples the member listing to group management concerns and produces a less reusable endpoint.

## Risks / Trade-offs

- **Risk:** Moving owner-promotion logic onto `WithInvitations` requires adding a default method or helper to an interface. If `WithInvitations` is ever consumed from outside the module, the added behavior is observable.
  **Mitigation:** The interface lives in `common.usergroup` and is only implemented by `MembersGroup` today. The default method is a pure validation check that throws on bad input — no state mutation, no side effect.

- **Risk:** The `CreateFamilyGroup` API change is a breaking change for any frontend that still posts `parentIds` / `memberIds`.
  **Mitigation:** The system has no production deployment and the frontend is updated as part of this proposal. The break is confined to a single request handler and catches at compile time in TypeScript (if the client is regenerated) or at runtime with a clear 400 otherwise.

- **Risk:** The manual training-group exclusivity check in the application service adds a repository lookup on every manual add. For an orienteering club with tens of training groups, this is negligible, but the pattern is worth noting.
  **Mitigation:** Accepted — the call is at most a `findByMemberId` against a small table and fits the project's performance budget.

- **Trade-off:** Frontend-side filtering of member pickers means the frontend needs to know which members belong to the target group. This is already available via the detail response, but it couples the picker component to the group's response shape.
  **Mitigation:** Filtering is a small pure function that takes the group's membership list and the full candidate list — easy to unit test and localize.

- **Trade-off:** Separate `/parents` and `/children` endpoints mean the "Add member" dialog in the UI routes to different URLs based on the role picker. One slightly more complex button in exchange for cleaner server-side semantics and HAL affordances.
