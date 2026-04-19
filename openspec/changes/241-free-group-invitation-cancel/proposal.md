## Why

GitHub issue #241 ("Zruseni pozvanky do skupiny", milestone `MVP`, labels `clen klubu`, `vedoucí skupiny`) asks for the ability to cancel a pending free-group invitation before the invitee has responded. Acceptance criteria from the issue:

1. Only the group owner or the invitation's author can cancel.
2. Cancellation is reachable from the invited-members list with search.
3. The canceller can optionally provide a reason.
4. The invitee is notified (with the reason).
5. All group owners and the invitation's author are notified too (except the person performing the action), with the reason.

Today the `user-groups` spec `Free Group Invitation System` covers: invite, accept, reject, re-invite after rejection, owner-only invite, "no pending duplicate" guard, and deletion of a free group (which implicitly cancels pending invitations per `Owner deletes a free group`). What is missing:

- **No explicit cancel-pending-invitation requirement.** The only way to remove a pending invitation today is to delete the whole group.
- **No notifications.** The notifications topic is broadly TODO across the project (see related TODO issues #28, #89). Issue #241 asks explicitly for email/system notifications on cancellation.
- **The invited-members list does not even have a "cancel" affordance** specified — the `Free Group Detail View` requirement says "pending invitations" are displayed but lists no action affordances beyond invite.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `user-groups`: extend `Free Group Invitation System` with cancellation of pending invitations (authorization, reason, side effects). Extend `Free Group Detail View` with the cancel affordance on each pending invitation row.
- Possibly new notification coupling (see Open Questions).

## Impact

**Affected specs:**
- `openspec/specs/user-groups/spec.md` — `Free Group Invitation System` gains a "cancel pending invitation" requirement covering: who can cancel (owner or inviter), what happens (invitation transitions from pending to cancelled), what happens on re-invite (allowed — mirrors existing re-invite after rejection), and optionally an in-domain "reason" field. `Free Group Detail View` gains an affordance scenario.
- Possibly `openspec/specs/email-service/spec.md` (or a new notifications spec): invitation-cancelled email templates for invitee and for other owners.

**Affected code (backend, members / user-groups module):** the `UserGroup` invitation-related domain gains a `cancelInvitation(invitationId, actor, reason)` operation with the new authorization check. The REST representation of a free-group detail gains a HAL-Forms affordance on each pending invitation row.

**Affected code (frontend):** the invited-members list on the free-group detail page gains a "Zrušit pozvánku" button; clicking opens a modal that asks for the optional reason and confirms.

**APIs (REST):** additive — new endpoint / action on each pending invitation (e.g., `DELETE /api/groups/{groupId}/invitations/{invitationId}` with an optional `reason` body).

**Dependencies:** depends on the notifications capability once that is implemented. For this proposal, notifications are stubbed (the domain event is emitted; email wiring is a separate change).

**Data:** invitation status enum grows (`PENDING`, `ACCEPTED`, `REJECTED`, plus `CANCELLED`). Optional `cancellation_reason` column. Optional audit columns (`cancelled_at`, `cancelled_by`).

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Exact authorization rule.** Acceptance criterion 1 says "group owner or invitation author". The invitation author is already a group owner (only owners can invite, per `Only group owner can invite members`), so "group owner" subsumes "invitation author". However, if ownership changed between the invite and the cancel (co-owner added/removed), the author may no longer be an owner. Recommend: authorization is "any current group owner", and the "author" clause is redundant. Confirm or keep the "author" exception.

2. **Is the reason required or optional?** The issue says "*mám možnost zadat*" — implies optional. Recommend: optional. Confirm.

3. **What happens to a cancelled invitation record?**
   - Option A: soft-status transition (invitation row kept with CANCELLED status, audit trail preserved).
   - Option B: hard delete (row gone, future re-invite creates a fresh pending invitation without trace).

   Recommend: Option A (parallel to REJECTED status).

4. **Can an invitation that is already ACCEPTED / REJECTED be "cancelled"?** No — cancellation applies only to PENDING. Confirm that attempting to cancel a non-pending invitation returns an error.

5. **Re-invite after cancellation.** Same rule as re-invite after rejection (existing scenario): new pending invitation is allowed. Confirm.

6. **Notifications scope.**
   - Option A: emit a domain event (`FreeGroupInvitationCancelled`) with invitee, group, reason, actor — downstream notification system handles delivery (future work).
   - Option B: synchronously send email as part of the cancel operation (couples domain to email-service).
   - Option C: no notifications in this change; defer to a dedicated notifications proposal.

   Recommend: Option A (emit event now, deliver later).

7. **Who exactly gets notified?** Acceptance criterion 5 says "všem vedoucím skupiny a autorovi pozvanky … mimo toho, kdo prováděl akci". The inviter is already an owner → the set reduces to "all owners except the actor". Confirm.

8. **What if the invitee user has already been deactivated / suspended between invite and cancel?** Invitation should still be cancellable (it becomes CANCELLED). No notification to the suspended user. Confirm.

9. **UI: a list with search.** Acceptance criterion 2 mentions search capability for the list of invited members. Is the existing `Free Group Detail View` invited-members section already searchable, or does this change also add search? Currently the spec is silent — likely the invited list is short enough that search is overkill. Recommend: list only, no dedicated search. Confirm.

10. **Bulk cancellation.** Can an owner cancel multiple pending invitations at once (checkbox + bulk action)? Not mentioned in the issue. Recommend: single-item cancel only for MVP. Confirm.
