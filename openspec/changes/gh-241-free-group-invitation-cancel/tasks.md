## 1. Backend — Domain: CANCELLED invitation state and manual cancel

- [x] 1.1 Add `CANCELLED` value to the invitation status enum in the `user-groups` domain
- [x] 1.2 Extend `FreeGroupInvitation` with optional `cancelledAt`, `cancelledBy` (MemberId or SYSTEM), `cancellationReason` (free text, max 500 chars)
- [x] 1.3 Write failing aggregate test: current owner cancels a PENDING invitation with reason → status becomes CANCELLED, audit fields populated
- [x] 1.4 Write failing aggregate test: current owner cancels a PENDING invitation without reason → status becomes CANCELLED, reason is null
- [x] 1.5 Write failing aggregate test: former owner (no longer in owners set) attempts to cancel → domain exception
- [x] 1.6 Write failing aggregate test: non-owner member attempts to cancel → domain exception
- [x] 1.7 Write failing aggregate test: cancel on ACCEPTED / REJECTED / CANCELLED invitation → state-conflict domain exception
- [x] 1.8 Implement `UserGroup.cancelInvitation(invitationId, actor, reason)` on the aggregate; make tests pass
- [x] 1.9 Write failing aggregate test: member can be re-invited after their previous invitation was cancelled
- [x] 1.10 Ensure re-invite path permits a new pending invitation when the most recent one is CANCELLED; update duplicate-pending guard if needed

## 2. Backend — Domain event

- [x] 2.1 Define `FreeGroupInvitationCancelled` domain event (groupId, invitationId, inviteeMemberId, actor, reason, recipientOwnerIds, cancelledAt)
- [x] 2.2 Write failing aggregate test: cancel emits the event with `recipientOwnerIds = all current owners except the actor`
- [x] 2.3 Write failing aggregate test: cancel by SYSTEM actor emits the event with `recipientOwnerIds = all current owners` (no exclusion)
- [x] 2.4 Implement event emission from the aggregate; make tests pass

## 3. Backend — Persistence

- [x] 3.1 Add DB migration for the three new nullable columns (`cancelled_at`, `cancelled_by`, `cancellation_reason`) and the CANCELLED status value
- [x] 3.2 Update the invitation memento / repository mapping to serialize and restore the new fields
- [x] 3.3 Write repository integration test: round-trip a CANCELLED invitation through save + load and assert all fields match

## 4. Backend — Application service and REST

- [x] 4.1 Add application command `CancelFreeGroupInvitation(groupId, invitationId, actor, reason)`
- [x] 4.2 Write failing controller test: `DELETE /api/groups/{groupId}/invitations/{invitationId}` with empty body → 204, invitation becomes CANCELLED
- [x] 4.3 Write failing controller test: `DELETE /api/groups/{groupId}/invitations/{invitationId}` with `{"reason": "..."}` → 204, reason persisted
- [x] 4.4 Write failing controller test: cancel on non-PENDING invitation → 409 Conflict
- [x] 4.5 Write failing controller test: caller is not a current owner → 403 Forbidden
- [x] 4.6 Implement the REST endpoint and wire authorization; make tests pass

## 5. Backend — HATEOAS affordance on free-group detail

- [x] 5.1 Write failing representation test: free-group detail for an owner includes a "cancel" affordance on each pending-invitation row, with an optional `reason` field
- [x] 5.2 Write failing representation test: free-group detail for a non-owner does NOT include the cancel affordance
- [x] 5.3 Implement the affordance in the detail representation processor; make tests pass

## 6. Backend — Auto-cancel on invitee deactivation

- [ ] 6.1 Identify or confirm the exact domain event emitted by the members module on deactivation / termination
- [ ] 6.2 Write failing listener test: on `MemberDeactivated`, all PENDING invitations for that invitee across all free groups transition to CANCELLED with SYSTEM actor
- [ ] 6.3 Write failing listener test: non-PENDING invitations for the deactivated member are NOT modified
- [ ] 6.4 Write failing listener test: one invitation's cancel failure does not block the others (partial success logged)
- [ ] 6.5 Implement the listener in the `user-groups` module; make tests pass

## 7. Frontend — Cancel action on pending invitation row

- [ ] 7.1 Render the "Zrušit pozvánku" button on each pending invitation row when the HAL affordance is present
- [ ] 7.2 Hide the button when the affordance is absent (non-owner view)
- [ ] 7.3 Clicking the button opens a confirmation modal with an optional `reason` textarea
- [ ] 7.4 Confirm submits the DELETE with the reason; cancel closes the modal without calling the API
- [ ] 7.5 On success, remove the invitation row from the pending list and refresh the representation
- [ ] 7.6 Write component test covering: affordance present → button visible, affordance absent → button hidden, confirm → DELETE call with body, cancel → no call

## 8. Follow-up and wrap-up

- [ ] 8.1 Create a follow-up GitHub issue referencing issue #241 — scope is to wire the `FreeGroupInvitationCancelled` event to the notification-service once that capability is added
- [ ] 8.2 Manual QA on local environment: invite → cancel → re-invite; deactivate member with pending invitations → verify auto-cancel
- [ ] 8.3 Add label `BackendCompleted` to GitHub issue #241 once backend tasks (sections 1-6) are merged
