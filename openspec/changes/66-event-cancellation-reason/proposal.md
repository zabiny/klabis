## Why

GitHub issue #66 ("Chci mít možnost viditelně škrtnout akci se zveřejněním důvodu zrušení", milestone `core`, labels `Events`, `přihlašovatel`, `question`) asks for the ability to cancel an event *with a visible reason* — so that members who were about to register, or who are already registered, understand why the event is no longer happening.

Today the `events` spec supports cancellation as a status transition (`Event Status Lifecycle`: DRAFT → CANCELLED and ACTIVE → CANCELLED) and preserves existing registrations for records (`Manager cancels an ACTIVE event` scenario). The list and detail pages show the CANCELLED status (via the status column visible to managers, and presumably a visual marker for regular members — currently underspecified). However:

- **No cancellation reason is captured.** There is no text field on the cancel command. Members see "zrušeno" with no explanation.
- **Visibility is not specified.** The `Events Table Display` spec explicitly states "Status column visible only to manager in list". Regular members may not see that the event is cancelled at all unless the visual treatment in the list/detail is already implemented (not in the spec today).
- **Members with existing registrations are not explicitly informed** beyond whatever implicit UI signal the cancelled status gives them.

The feature request has two parts: (1) capture a cancellation reason, and (2) make sure cancelled events — and the reason — are visibly communicated to all relevant personas.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: extend `Event Status Lifecycle` (capturing the reason at cancellation time), `Get Event Detail` and `Event Detail Page` (showing the reason to all viewers), `Events Table Display` and `List Events` (making cancellation visible to regular members too).

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — `Event Status Lifecycle` gains a "cancel with reason" scenario where the manager must (or may) provide a text reason. `Event Detail Page` and `Get Event Detail` gain scenarios where the cancellation reason is displayed for cancelled events. `Events Table Display` gains a scenario for how cancelled events are rendered to regular members (e.g., strikethrough / disabled row / dedicated badge) — this is the "viditelně škrtnout" part of the title.
- Possibly `openspec/specs/event-registrations/spec.md` — registered members whose event is cancelled should see the cancellation reason on their own registration view too.
- Possibly `openspec/specs/calendar-items/spec.md` — today calendar items are *deleted* on cancellation (per the archived calendar sync change). We should consider whether to keep them and mark them cancelled instead, so that the cancellation is visible from the calendar.

**Affected code (backend, events module):** the cancel command gains a `reason` parameter (string, potentially with a minimum length). The `Event` aggregate stores the reason on the cancelled status. The HAL representation exposes the reason for cancelled events.

**Affected code (frontend):** the "Zrušit akci" action opens a modal that asks for the reason. The detail page displays the reason in a prominent banner for cancelled events. The list table strikes through / visually demotes cancelled rows (today: unclear, may already be partially there). Registered members' own registration view surfaces the cancellation reason.

**APIs (REST):** additive — new field on the cancel command body, new field on the event detail response for cancelled events.

**Dependencies:** none.

**Data:** new nullable column on the events table (`cancellation_reason`).

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Is the reason required or optional?**
   - Option A: required on cancel (min length 1, max e.g. 500). Forces managers to explain. Simpler for members — they always see *something*.
   - Option B: optional. Quick cancellations stay quick; reason is exposed only when provided.

2. **Who sees the reason?** Recommend: everyone who can see the event (so every authenticated member, guest, etc.). Confirm.

3. **"Viditelně škrtnout" — what does that mean visually?**
   - Option A: strikethrough on the event name + a "Zrušeno" badge (row still in the list).
   - Option B: hidden from the default list and shown only when the user filters for CANCELLED status (already possible for managers today).
   - Option C: shown on a separate "Zrušené akce" section at the bottom of the list.

   Recommend: Option A (row with strikethrough + badge + cancellation reason tooltip or inline).

4. **Are cancelled events still visible to regular members in the events list at all today?** The spec `Regular user does not see DRAFT events` is explicit. For CANCELLED there is no equivalent. This proposal needs to commit: "Regular members see cancelled events with visible marking" — yes/no.

5. **Calendar interaction.** Currently, cancellation deletes all event-linked calendar items (per the archived change). Options:
   - Keep the current behavior — calendar items disappear on cancellation; members see only the strikethrough event in the events list.
   - Change to: calendar items are marked cancelled (kept but with a "zrušeno" label and struck through). More intrusive change — affects `calendar-items` spec.

   Recommend: keep current behavior for this proposal; a follow-up may revisit calendar cancellation visibility.

6. **Registration holders:** should the system actively notify members whose registration was affected (email notification)? That is a notifications topic (currently TODO / #28). Recommend: out of scope here — but capture that the cancellation reason is stored so a future email template can use it.

7. **Is the reason editable after cancellation?** E.g., the manager initially writes "rušíme" and later expands to include alternatives ("přesunuto na 10.5."). Recommend: yes — managers with `EVENTS:MANAGE` can edit the cancellation reason on a cancelled event. Otherwise, cancelled events are otherwise immutable per `Cancelled event cannot be edited`. Confirm.

8. **Character limit and formatting.** Plain text? Markdown? URLs auto-linked? Recommend: plain text, max 500 characters, no markdown.

9. **What about events already cancelled before this change ships (migration)?** Project uses H2 in-memory — no data migration needed. Confirm assumption.
