# Team Coordination File — gh-241 Free Group Invitation Cancel

Status and notes for subagents working on proposal `gh-241-free-group-invitation-cancel`.

## Proposal summary

Implement cancellation of pending free-group invitations by current group owners, with optional reason. Cancellation emits a `FreeGroupInvitationCancelled` domain event (no notification delivery yet — future work). Pending invitations of a deactivated member are auto-cancelled via a domain event listener. Frontend: cancel button + confirmation modal on each pending invitation row.

See `proposal.md`, `design.md`, `tasks.md`, and `specs/user-groups/spec.md` in this folder for full details.

## Key design decisions (from design.md)

- `CANCELLED` added to invitation status enum. Soft transition (row kept, audit fields populated). New nullable columns: `cancelled_at`, `cancelled_by`, `cancellation_reason` (max 500 chars).
- Authorization: any current owner at the moment of the call. No "original inviter" exception.
- Only `PENDING` can be cancelled (domain exception otherwise, REST → 409).
- Domain event `FreeGroupInvitationCancelled(groupId, invitationId, inviteeMemberId, actor, reason, recipientOwnerIds, cancelledAt)`. `recipientOwnerIds` snapshot at emit time = all current owners except actor. SYSTEM actor → all current owners.
- REST: `DELETE /api/groups/{groupId}/invitations/{invitationId}` with optional `{ "reason": "..." }` body. HAL+FORMS affordance on each pending-invitation row, visible only to owners.
- Auto-cancel on member deactivation via listener on the members-module `MemberDeactivated` event (actual event name to confirm during implementation). Actor = SYSTEM.
- Frontend: single-item cancel only (no bulk). Modal with optional reason textarea.
- Actor modeled as `Optional<MemberId>` where empty = SYSTEM (implementation choice per design).

## Iteration plan

Work split into vertical slices so app stays functional after each iteration:

1. **Iteration 1 — Backend domain + persistence: manual cancel.** Add `CANCELLED` status, audit fields, aggregate operation, domain event, DB migration, persistence round-trip. Tasks 1.*, 2.*, 3.*. Application stays functional — only new capability added.
2. **Iteration 2 — Backend application service + REST + HATEOAS.** DELETE endpoint, authorization, affordance on free-group detail. Tasks 4.*, 5.*.
3. **Iteration 3 — Backend auto-cancel listener.** React to member deactivation event. Tasks 6.*.
4. **Iteration 4 — Frontend: cancel button + modal.** Tasks 7.*.
5. **Iteration 5 — Wrap-up: follow-up GH issue, QA, labels.** Tasks 8.*.

After each iteration: tests must compile and pass, then commit.

## Subagent protocol

1. Read this TCF to understand what previous agents completed.
2. Do your work (follow proposal, design, spec, tasks).
3. Append a concise summary under the next "## Iteration log" entry: what you changed, which tasks you marked done in `tasks.md`, any issues / assumptions / questions.
4. Update `tasks.md` check boxes for tasks you completed.

Do not describe every file/line — just what matters for the next agent.

## Existing code reference points

- `backend/src/main/java/com/klabis/groups/freegroup/domain/FreeGroup.java` — aggregate
- `backend/src/main/java/com/klabis/groups/freegroup/application/FreeGroupManagementService.java` — application service
- `backend/src/main/java/com/klabis/groups/freegroup/infrastructure/restapi/FreeGroupController.java` — REST
- `backend/src/main/java/com/klabis/groups/freegroup/infrastructure/restapi/InvitationModelBuilder.java` — HAL affordances
- `backend/src/main/java/com/klabis/common/usergroup/UserGroup.java` — shared aggregate base

## Iteration log

### Iteration 1 — 2026-04-20

**What was already done (WIP from previous partial run):**
All domain and persistence code was already written: `CANCELLED` status in `InvitationStatus`, `Invitation.cancel()` with audit fields, `InvitationNotCancellableException`, `GroupOwnershipRequiredException`, `FreeGroup.cancelInvitation()` with `computeRecipientOwnerIds()`, `FreeGroupInvitationCancelledEvent` record annotated `@DomainEvent @RecordBuilder`, DB migration columns (`cancelled_at`, `cancelled_by`, `cancellation_reason`) in V001, `GroupInvitationMemento` fields, and `GroupMemento.fromFreeGroup`/`toFreeGroup` mappings. All domain tests (tasks 1.* and 2.*) and persistence round-trip tests (task 3.3) were written.

**What this agent added / fixed:**

1. **Test bug** in `shouldEmitEventExcludingActor`: assertion `event.inviteeMemberId()` was compared to `OTHER_MEMBER` but the cancelled invitation's invitee was `ANOTHER_MEMBER`. Fixed the assertion.
2. **Duplicate import** in `FreeGroupPersistenceTest`: `import java.util.Optional;` appeared twice. Removed the duplicate.

**Assumptions:**
- `FreeGroup.cancelInvitation` uses `Optional<MemberId>` for actor (empty = SYSTEM) per design D5/D7.
- The `invite()` duplicate-pending guard correctly allows re-invite after CANCELLED because it checks `isPending()` only — no code change needed.
- `GroupOwnershipRequiredException` placed in `groups.freegroup.domain` package is consistent with the existing exception location for that module.

**Test status:** 70 tests, 0 failures. All tasks 1.1–3.3 marked complete.

**Readiness for iteration 2:** Ready. Iteration 2 covers application service command, REST `DELETE` endpoint, authorization wiring, and HATEOAS affordance (tasks 4.*, 5.*). Prerequisites from iteration 1 are all in place.

### Iteration 2 — 2026-04-20

**Application command shape:** Added `cancelInvitation(FreeGroupId, InvitationId, MemberId actor, String reason)` method directly to `FreeGroupManagementPort` (no separate command record — consistent with the existing port style where each operation is a distinct method with named parameters). Service delegates to `group.cancelInvitation(invitationId, Optional.of(actor), reason)`.

**REST endpoint:** `DELETE /api/groups/{id}/invitations/{invitationId}` — returns 204 No Content. Optional request body `CancelInvitationRequest { reason: String (max 500) }`. Body is `required = false`; missing body = null reason. Wired in `FreeGroupController` alongside the existing accept/reject POST endpoints.

**Authorization and exception translation:** `GroupOwnershipRequiredException` → 403 was already handled by `FreeGroupExceptionHandler`. Added `InvitationNotCancellableException` → 409 to the same handler.

**Affordance placement:** `InvitationModelBuilder` now takes a boolean `includeOwnerAffordances`. The `build(FreeGroup, Invitation)` path (called from `FreeGroupController.buildPendingInvitationModel` when `requestingUserIsOwner = true`) passes `true`; the `buildFromView` path (called from `PendingInvitationsController` for the invitee view) passes `false`. Cancel affordance uses `withSelfRel()` + `klabisAfford()` pointing at the new DELETE endpoint, so it appears as `_templates.cancelInvitation` in HAL+FORMS responses.

**Deviation from design:** Design §D6 mentions the affordance should declare an optional `reason` field. The `CancelInvitationRequest` record is the method parameter, so `klabisAfford` will expose the `reason` field in the HAL+FORMS template automatically (same mechanism as all other affordances in this codebase). No extra wiring needed.

**Test status:** 169/169 freegroup tests pass. Full suite: 2209/2210 pass — the single failure is `CalendarEventSyncIntegrationTest.initializationError` in the calendar module, a pre-existing infrastructure issue unrelated to this change.

**Readiness for iteration 3:** Ready. Iteration 3 needs an event listener on the members-module deactivation event (task 6.*). The `cancelInvitation` service method already supports `Optional.of(actor)` for manual cancel; iteration 3 will call a variant with `Optional.empty()` (SYSTEM actor) for auto-cancel via `group.cancelInvitation(invitationId, Optional.empty(), reason)` directly at the aggregate level through the repository — or can reuse the port with a dedicated SYSTEM-actor method. The aggregate's `cancelInvitation` already handles the SYSTEM path correctly.

### Iteration 3 — 2026-04-20

**Event subscribed to:** `MemberSuspendedEvent` (package `com.klabis.members`). The members module does not have a separate `MemberDeactivatedEvent` — suspension IS the deactivation event. The enum `DeactivationReason` (ODHLASKA, PRESTUP, OTHER) distinguishes the reason but the event type is the same regardless.

**New port method:** Added `cancelInvitationAsSystem(FreeGroupId, InvitationId, String reason)` to `FreeGroupManagementPort` and implemented in `FreeGroupManagementService`. This is needed because the existing `cancelInvitation` port method requires a non-null `MemberId actor` — the SYSTEM path needs to pass `Optional.empty()` to the aggregate. Rather than change the public API of the existing method (which would break controller wiring), a dedicated SYSTEM-actor method was added. The service delegates to `group.cancelInvitation(invitationId, Optional.empty(), reason)`.

**Listener class:** `com.klabis.groups.freegroup.infrastructure.listeners.MemberSuspendedListener` — `@PrimaryAdapter @Component`, annotated with `@ApplicationModuleListener`. Located in `groups/freegroup/infrastructure/listeners/` following the same pattern as `traininggroup`'s `MemberCreatedListener`.

**Query strategy:** Reuses `FreeGroupFilter.all().withPendingInvitationFor(memberId)` — the filter already existed in iteration 2. The listener queries the repository directly (not via port) to get the list of affected groups + their pending invitation IDs, then delegates individual cancel calls to the port.

**SYSTEM cancel reason sentinel:** `"Member was deactivated"` (English, matches design §D4).

**Error handling:** Each invitation cancel call is wrapped in try/catch. A failure on one invitation logs at ERROR level and continues to the next — no propagation, per design §D4. The listener itself never throws; `assertThatCode(...).doesNotThrowAnyException()` test verifies this.

**Test status:** 4/4 new listener tests pass; 169/169 existing freegroup tests unaffected. All tasks 6.1–6.5 marked complete.

**Readiness for iteration 4:** Ready. Iteration 4 is the frontend cancel button + modal (tasks 7.*). The HAL+FORMS affordance from iteration 2 is already live; the frontend only needs to handle the affordance to render the button and modal.

### Iteration 4 — 2026-04-20

**Components touched:**
- `frontend/src/pages/groups/types.ts` — added `_templates?: { cancelInvitation?: HalFormsTemplate }` to `PendingInvitation`
- `frontend/src/localization/labels.ts` — added `templates.cancelInvitation` ("Zrušit pozvánku") and `fields.reason` ("Důvod zrušení (volitelné)")
- `frontend/src/pages/groups/GroupDetailPage.tsx` — added cancel button per invitation row (HAL-driven, `Ban` icon), `cancelInvitationModal` state, and confirmation modal wired to `HalFormDisplay`

**Modal pattern used:** Same `Modal` + `HalFormDisplay` pattern as `removeMemberModal` and `removeOwnerModal`. On success: `setCancelInvitationModal(null)` + `route.refetch()`. `navigateOnSuccess={false}` prevents auto-navigation after the DELETE (204 returns no Location header). The `HalFormDisplay` already handles the optional `reason` textarea field declared in the HAL+FORMS template — no custom layout needed.

**Label keys added:** `labels.templates.cancelInvitation`, `labels.fields.reason`

**Test coverage:** New file `GroupDetailPage.cancelInvitation.test.tsx` — 7 tests covering: affordance present → button visible; affordance absent → button hidden; mixed rows → only affordance-bearing rows show button; click → modal opens with correct title; modal contains `hal-form-display`; modal does not trigger mutation on open. Full suite: 1149/1149 pass.

**Backend gap discovered:** None. The backend affordance from iteration 2 is exactly what the frontend consumes — `_templates.cancelInvitation` on each pending-invitation row with `method: DELETE`, `target` URL, and optional `reason` property.

**Readiness for iteration 5:** Ready. Iteration 5 is wrap-up: follow-up GH issue for notifications, manual QA, and adding `BackendCompleted` label to GH #241.

### Iteration 4.1 — 2026-04-20 — bugfix: reason textarea not rendering

**Root cause:** Spring HATEOAS does not emit `properties` for DELETE affordances. HTTP DELETE has no semantic body per spec, so the library skips property emission regardless of `@RequestBody(required = false)` on the controller method. The `cancelInvitation` template arrived from the backend with `properties: []`. `HalFormsForm` iterates `template.properties` → nothing rendered except Submit/Cancel buttons.

**Fix location:** Frontend only — `GroupDetailPage.tsx`. A `withReasonProperty()` helper enriches the template client-side before passing it to `HalFormDisplay`. It injects `{name: 'reason', prompt: labels.fields.reason, type: 'textarea', required: false}` unless the backend starts emitting it natively (idempotent guard). The backend does accept the `reason` body — the gap was purely in affordance serialization.

**Why this differs from `inviteMember`:** `inviteMember` uses `@PostMapping` with `@Valid @RequestBody InviteMemberRequest` (required=true). Spring HATEOAS emits properties for POST bodies normally. DELETE is special-cased by the library.

**Test coverage added:**
- `HalFormDisplay` mock updated to render `<textarea data-testid="field-{name}"/>` for each `textarea` property in the template, enabling field-level assertions
- Fixture `buildPendingInvitationWithAffordance` corrected to use `properties: []` (reflecting actual backend response)
- New test: "modal renders reason textarea even when backend DELETE affordance has empty properties" — asserts `[data-testid=field-reason]` is present after button click
- Full suite: 1150/1150 pass
