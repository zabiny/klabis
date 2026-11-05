## Why

GitHub issue #89 ("Chci nepropasnout zadny deadline prihlasek", milestone `MVP`, labels `clen klubu`, `hostujici clen`, `question`) asks that members don't miss any registration deadline. The acceptance criteria are open (`question`), so the proposal covers the plausible mechanisms.

Today the relevant specs give members a **passive** way to spot deadlines:
- `calendar-items` `Automatic Synchronization from Events` creates a "Přihlášky - {event name}" calendar item on the deadline day itself (one per deadline when the event has several), so a member who happens to open the calendar on that day will see it.
- `events` list shows a deadline column (`Events Table Display`).
- `dashboard` `Upcoming Deadlines Widget` ("Končící přihlášky tento týden") lists the active events closing within seven days that the member is not yet registered to, with an in-place "Přihlásit se" action. It is only seen when the member opens the dashboard.

There is **no active reminder**. A member who forgets to open the application simply misses the deadline.

Two building blocks that did not exist when the issue was filed are now in place: the `push-notifications` capability (web push to the devices a member enabled, with privacy-safe content and a click-through to the related page; backend entry point `PushNotifications.send(UserId, PushMessage)`), and the `email-service` capability (templates, event-driven sending). The reminder is therefore a scheduled producer on top of existing delivery channels, not a new notification framework.

## Capabilities

### New Capabilities

<!-- None: delivery channels already exist (`push-notifications`, `email-service`). The reminder job is a new producer, specified as a requirement of an existing capability — see Modified Capabilities. -->

### Modified Capabilities

- `push-notifications`: a new notification kind "registration deadline approaching" (message content, click-through to the event detail, privacy-safe wording as required by `Notification Content Is Privacy-Safe`).
- `events` / `event-registrations`: the reminder is sent only to members NOT yet registered, so the system needs to query registrations to decide recipients, and honour `RegistrationDeadlines.nextRelevant` for events with several deadlines.
- `email-service`: new template for the reminder e-mail, only if e-mail is chosen as a second channel (see Open Questions).

## Impact

**Affected specs:**
- `openspec/specs/push-notifications/spec.md` — a requirement for the deadline reminder as a notification kind.
- `openspec/specs/event-registrations/spec.md` or `events/spec.md` — recipient rules (not registered, event ACTIVE, deadline still ahead).
- `openspec/specs/email-service/spec.md` — new template "Připomenutí termínu přihlášek", only if e-mail is in scope.
- The dashboard widget already exists (`dashboard` spec) and is not part of this change.

**Affected code (backend):**
- A scheduled job scans ACTIVE events whose nearest relevant registration deadline (`RegistrationDeadlines.nextRelevant`) falls inside the reminder window. It follows the `@Scheduled`-with-configurable-cron pattern of the existing discovery jobs; if `introduce-quartz-scheduler` lands first, it becomes a Quartz job (that change names this proposal as a motivation, including a one-shot trigger as an alternative to polling).
- For each event, compute recipients (members not yet registered; see Open Questions).
- Dispatch through `PushNotifications.send` (and `email-service` if chosen).
- Record "sent reminder" state to avoid duplicates.

**Affected code (frontend):**
- Click-through target of the notification (existing mechanism of `Clicking A Notification Opens The Related Page`).
- Notification preferences beyond the per-device enable/disable are future work (#28).

**APIs (REST):** minimal — the reminder flow is backend-scheduled + email; any user-facing API is for the dashboard widget / preferences page.

**Dependencies:**
- `push-notifications` capability (exists; a member receives a notification only on devices where they enabled it, which is also the opt-out).
- `email-service` capability (exists), only if e-mail is in scope.
- Ideally a notification-preferences surface (#28), but this proposal can ship without — "all members who can still register" is an acceptable initial recipient set.
- Optionally `gh-113-event-training-group-tagging` for a group-relevant recipient set.

**Data:** new table/state to record that a reminder was sent for a given (event, deadline-kind) pair to avoid duplicates.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Scope.** The notification framework question is settled by what shipped: web push exists. What remains is a tightly-scoped deadline reminder as a producer on top of it, with per-user preferences deferred to #28. Confirm.

2. **Delivery channel.**
   - Web push only (existing, per-device opt-in, no new infrastructure).
   - Web push plus e-mail (reaches members who never enabled push, needs a template and an opt-out link).
   - E-mail only.

   Recommend: web push only for MVP, because it needs no new delivery code; add e-mail if push adoption turns out to be low. Confirm.

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

8. **Opt-out path.** For push, a member opts out by disabling notifications on the device (`Disabling Notifications On The Current Device`). For e-mail (if in scope) every message needs an unsubscribe route; until #28 ships a placeholder "kontaktujte administrátora" link is acceptable. Confirm.

9. **Dashboard widget.** Already delivered (`Upcoming Deadlines Widget`, "Končící přihlášky tento týden"). Removed from this proposal. The reminder window should be chosen consistently with the widget's seven days.

10. **Guest members.** Do guests get reminders? Recommend: yes, same rule set. Confirm.

11. **Timezone.** All times interpreted in club-local (Europe/Prague) — consistent with existing time handling. Confirm.

12. **ORIS / supplementary-service deadlines.** Some supplementary services (T-shirt orders, accommodation) can have their own deadlines earlier than the event registration deadline (see `event-registration-pricing`). Out of scope here. Confirm.

13. **Internal deadline offset.** If `gh-113-oris-internal-deadline-offset` lands, the deadline members are bound by is the internal one. The reminder must be computed from the same effective deadline the member sees, not from the stored ORIS one. Confirm.
