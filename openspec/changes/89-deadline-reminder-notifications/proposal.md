## Why

GitHub issue #89 ("Chci nepropasnout zadny deadline prihlasek", milestone `MVP`, labels `clen klubu`, `hostujici clen`, `question`) asks that members don't miss any registration deadline. The acceptance criteria are open (`question`), so the proposal covers the plausible mechanisms.

Today the relevant specs give members a **passive** way to spot deadlines:
- `calendar-items` `Automatic Synchronization from Events` creates a "Přihlášky - {event name}" calendar item on the deadline day itself, so a member who happens to open the calendar on that day will see it.
- `events` list shows a deadline column (`Events Table Display`).

There is **no active reminder**. A member who forgets to open the calendar on the deadline day simply misses the deadline. For members who rely on email, there is nothing.

Issue #89 asks for a reliable "don't miss the deadline" experience. The natural answers: email reminders, in-app notifications, or a prominent dashboard widget.

## Capabilities

### New Capabilities

- Probably **`notifications`** (new capability): a cross-cutting notification framework has been referenced by multiple TODO issues (#28 "chci si vybrat jake notifikace", #27 "Chci notifikaci pri zapornem zustatku", etc.). Issue #89 is a natural first consumer. Whether this proposal introduces the full notification capability or just a tightly-scoped deadline-reminder flow is one of the open questions.

### Modified Capabilities

- `events` / `event-registrations`: if the reminder is sent only to members who are NOT yet registered (the natural audience), the system needs to query registrations to decide recipients.
- `calendar-items`: if the dashboard widget is added, the "upcoming deadlines" list may mirror calendar-item queries.
- `email-service`: new templates for the reminder emails.

## Impact

**Affected specs:**
- New `openspec/specs/notifications/spec.md` (if the full capability is introduced), OR localized scenarios in `events` / `event-registrations` if only deadline-reminders are added.
- `openspec/specs/email-service/spec.md` — new email template "Připomenutí termínu přihlášek" with expected content + recipient filtering.
- Possibly a dashboard widget spec addition (`application-navigation` or a new dashboard-items spec).

**Affected code (backend):**
- A scheduled job scans events whose `registrationDeadline` is within the next N days.
- For each event, compute recipients (all club members not yet registered, or everyone in relevant groups, or only members with opted-in preference — see Open Questions).
- Dispatch via `email-service`.
- Record "sent reminder" state to avoid duplicates.

**Affected code (frontend):**
- Possibly a dashboard widget "Nejbližší deadliny přihlášek" showing the N nearest deadlines.
- Possibly a notifications panel (future work).
- Notification preferences page (future work, #28).

**APIs (REST):** minimal — the reminder flow is backend-scheduled + email; any user-facing API is for the dashboard widget / preferences page.

**Dependencies:**
- `email-service` capability (exists, per `members` `Welcome Email on Registration`).
- Ideally a notification-preferences surface (#28), but this proposal can ship without — "all members who can still register" is an acceptable initial recipient set.

**Data:** new table/state to record that a reminder was sent for a given (event, deadline-kind) pair to avoid duplicates.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Scope — notifications capability now, or deadline-only reminder?**
   - Option A: introduce a full `notifications` capability (channels, preferences, templates, delivery history). Large scope.
   - Option B: ship a tightly-scoped "deadline reminder email" feature and defer the notifications capability to a bigger proposal triggered by #28.

   Recommend: Option B. Confirm.

2. **Delivery channel.**
   - Email only (leverages existing `email-service`).
   - In-app feed / badge (new infrastructure).
   - Both (email + in-app).

   Recommend: email only for MVP.

3. **Timing / cadence.** When is the reminder sent?
   - 24 hours before the deadline (matches the informal rule "termín prihlasek -1D" from issue #113).
   - Three days before + one day before (two reminders).
   - Configurable per user preference (pending #28).
   - At a fixed wall-clock time (e.g., 18:00) on the day before.

   Recommend: 24 hours before, single reminder, at a fixed time (default 18:00 club-local). Configurable centrally (admin settings). Per-user preferences deferred.

4. **Recipient set.**
   - Option A: all active members who are NOT currently registered and the event is relevant to their groups (intersection with #110/#113 work).
   - Option B: all active members who are not yet registered.
   - Option C: everyone in the club.

   Recommend: Option A ideally (needs event-to-group tagging from #113), falling back to Option B until #113 lands.

5. **What if the member is already registered?** No reminder — they are fine. Confirm.

6. **Idempotency.** Rerunning the scheduler must not produce duplicate emails. State needed — what's the storage? Recommend: a `sent_deadline_reminders` table keyed by (event_id, deadline, kind). Confirm.

7. **Event lifecycle edge cases.**
   - Event cancelled after reminder sent: no action.
   - Event deadline updated after reminder sent: send again. Recommend this behavior.
   - Event cancelled before reminder sent: skip (no reminder).

8. **Opt-out path.** Every email includes an "Odhlásit z notifikací" link. Lands in a preferences page once #28 ships; until then, a placeholder "kontaktujte administrátora" link is acceptable. Confirm.

9. **Dashboard widget scope.** Include in this proposal, or defer?
   - In-scope: "Nejbližší deadliny" widget showing 3 upcoming events with deadlines in the next 14 days, with a register button per row.
   - Deferred: requires dashboard infrastructure decisions (role-based dashboard is already shipped per memory — widgets plug into it).

   Recommend: include as part of this proposal — the widget reinforces the email reminder.

10. **Guest members.** Do guests get reminders? Recommend: yes, same rule set. Confirm.

11. **Timezone.** All times interpreted in club-local (Europe/Prague) — consistent with existing time handling. Confirm.

12. **ORIS / supplementary-service deadlines.** Some supplementary services (T-shirt orders, accommodation) can have their own deadlines earlier than the event registration deadline (see #112). Out of scope here. Confirm.
