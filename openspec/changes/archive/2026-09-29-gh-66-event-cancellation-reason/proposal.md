## Why

GitHub issue #66 ("Chci mít možnost viditelně škrtnout akci se zveřejněním důvodu zrušení", milestone `core`, labels `Events`, `přihlašovatel`, `question`) asks for the ability to cancel an event *with a visible reason* — so that members who were about to register, or who are already registered, understand why the event is no longer happening.

The **cancellation reason capture** part of this issue is already delivered: the archived change `2026-05-10-review-1-4-event-cancellation-and-multiple-deadlines` added the optional `cancellationReason` (max 500 chars) to the cancel command, persisted it (`events.cancellation_reason`), exposed it on `EventDto`/`EventSummaryDto`, and the `events` spec (`Event Status Lifecycle`) already records it. The detail page shows the reason in a bottom "Akce byla zrušena" banner; the list shows it only as a tooltip on the (manager-only) status cell.

Two pieces of the original request remain **unimplemented**, and this reworked proposal covers exactly those:

- **A — Visibility.** Today every authenticated user sees CANCELLED events in the events list (only DRAFT is hidden for non-managers), but with no marking, because the `status` column is gated to `EVENTS:MANAGE`. The issue's intent ("viditelně škrtnout") is that a cancelled event is clearly visible to the right audience and hidden from everyone else: managers and the members who actually registered for it.

- **B — Visual marking.** There is no strikethrough/badge treatment. The list shows the reason only as a tooltip on the manager-only status cell; the detail shows it in a bottom banner rather than next to the name. The issue title literally asks to "škrtnout" (strike through) the event.

### Target behaviour

- A cancelled event is visible in the events list to users with `EVENTS:MANAGE` and to members registered for that event. A regular user who is **not** registered does not see it in the list at all.
- In the events list, a cancelled event's **name is struck through** (name only).
- In the event detail, the **name is struck through**, a prominent **"Zrušeno" badge** is shown, and the **cancellation reason is displayed directly under the name**.

## What Changes

- **List visibility (backend).** For callers without `EVENTS:MANAGE`, CANCELLED events are excluded from the list result **unless the caller is registered for that event**. This is a per-row condition (status = CANCELLED AND a registration exists for the caller's member id), not the simple status exclusion used today for DRAFT. The existing `"Moje přihlášky"` behaviour (registered members see their cancelled events) is preserved and becomes the default for registered members regardless of that toggle.
- **Status field on the list payload (backend).** `EventSummaryDto.status` loses its `EVENTS:MANAGE` gate, so the status is returned to every authenticated caller. This gives every viewer of a row a reliable "is cancelled" signal without introducing a derived duplicate field (review decision: status is not secret — users know it from other displayed information anyway). Visibility of the status **column** is unchanged (manager-only); the frontend gates it explicitly because the implicit payload-driven hiding no longer applies.
- **List rendering (frontend).** The event **name** is struck through when the row is a cancelled event (name only — the rest of the row stays normal and its actions remain usable), driven by `status === 'CANCELLED'`. Because a non-manager sees no status column, the struck name also repeats the state as visually hidden text for screen readers — the state, never the reason. The status column is hidden explicitly for users without `EVENTS:MANAGE`.
- **Summary payload cleanup (backend).** `EventSummaryDto.cancellationReason` is removed. The list tooltip it fed is gone, so the field would ship on every row with no consumer — and it is the one place a cancelled event's reason would reach viewers who have no business seeing it. The reason remains on `EventDto`, where the detail page shows it under the name.
- **Detail rendering (frontend).** The event name is struck through, a prominent "Zrušeno" badge is shown in the header, and the cancellation reason moves to directly under the name. This supersedes the current bottom "Akce byla zrušena" banner.
- **Detail access (backend).** Unchanged: a cancelled event's detail **stays open** — reachable by any authenticated user who has the link (only DRAFT detail is blocked for non-managers today). The rendering changes above apply whenever the detail is shown.
- **Spec.** The `events` capability is updated to describe the new cancelled-event visibility and the strikethrough/badge/reason-under-name treatment.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`:
  - `List Events` — cancelled events are shown only to `EVENTS:MANAGE` holders and to members registered for the event; hidden from all other users.
  - `Events Table Display` — a cancelled event's name is rendered struck through. The status column stays manager-only (unchanged); the "status column hidden when the API omits the field" scenario is dropped because the field is now always returned. The summary payload no longer carries the cancellation reason, so no requirement in the list mentions it.
  - `Event Detail Page` — a cancelled event shows a struck-through name, a "Zrušeno" badge, and the cancellation reason under the name (replacing the bottom banner).
  - `Event Status Lifecycle` — the "cancellation reason is shown on the cancelled event row in the list" scenario is restated in terms of the strikethrough name (the reason itself is no longer surfaced as a list tooltip; it is shown in the detail under the name).
  - `Get Event Detail` — cancelled event detail stays accessible to every authenticated user (locks Resolved Decision 1: the list-level hiding does not extend to the detail page).
- `dashboard`:
  - `Upcoming Registrations Widget` — lists only ACTIVE events, so a cancelled event the member is registered to no longer appears in "Moje nadcházející akce" (the "Končící přihlášky" widget already requires ACTIVE).

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — `List Events`, `Events Table Display`, `Event Detail Page`, `Event Status Lifecycle`, `Get Event Detail`.
- `openspec/specs/dashboard/spec.md` — `Upcoming Registrations Widget`.

**Affected code (backend, events module):**
- `EventManagementService.listEvents` / `EventFilter` — add the "exclude CANCELLED for non-managers unless registered by the caller" rule. The caller's member id must reach the filter; this cannot be expressed as a plain `withExcludedStatus(CANCELLED)` because registered members must still see their cancelled events.
- `docs/openapi/spec/events.yaml` — remove `x-klabis-authority: EVENTS_MANAGE` from `EventSummaryDto.status` (review decision: no derived `cancelled` duplicate; status is not secret), and remove `cancellationReason` from `EventSummaryDto` (no consumer once the tooltip is gone). `EventDto` is untouched. No new response fields.

**Affected code (frontend):**
- `frontend/src/pages/events/EventsPage.tsx` — strike through the name cell for cancelled rows (replacing the current status-cell tooltip); hide the status column explicitly for users without `EVENTS:MANAGE`.
- `frontend/src/pages/events/EventDetailPage.tsx` — strike through the name, render a prominent "Zrušeno" badge, show the reason under the name; remove/replace the bottom "Akce byla zrušena" banner.
- `frontend/src/localization/labels.ts` — labels for the badge/reason placement if new strings are needed.

**APIs (REST):** behavioural — the list result set changes for non-registered users (cancelled events no longer returned). Field-security change: `EventSummaryDto.status` is returned to all authenticated callers. Field removal: `EventSummaryDto.cancellationReason` is no longer part of the list response. No new fields, no new endpoints, no changed HAL links or affordances. Detail endpoint access and `EventDto` are unchanged.

**Data:** none — `cancellation_reason` column already exists.

**Dependencies:** none.

**Out of scope (explicitly not part of this rework):**
- E-mail/notification of registered members on cancellation (still TODO / #28). The reason is stored so a future template can use it.
- Editing the cancellation reason after the event is cancelled (cancelled events remain immutable).
- Calendar-item treatment on cancellation (remains as-is: linked calendar items are deleted).

## Resolved Decisions

1. **Detail access stays open.** A cancelled event's detail remains reachable by any authenticated user with the link (only DRAFT detail is blocked for non-managers). The list visibility rule does not extend to the detail endpoint.

2. **Cancelled signal in the list = the struck-through name.** The row communicates cancellation through the struck-through event name. Per review, the signal is the summary's `status` field with its `EVENTS:MANAGE` gate removed — no derived `cancelled` duplicate is introduced. Visibility of the status column itself is unchanged (manager-only), gated explicitly by the frontend.

3. **Strikethrough scope = name only.** Only the event name is struck through in the list; the rest of the row and its actions stay normal.
