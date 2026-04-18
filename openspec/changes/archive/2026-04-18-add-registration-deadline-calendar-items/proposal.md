## Why

When members browse the club calendar, today they see one entry per event — the event date itself. They do not see when registrations for that event close. Registration deadlines are published on the event's detail page and in external systems (ORIS), but members who only scan the calendar miss the deadline and can show up the day after registrations closed.

The calendar already synchronizes event-date items automatically from published events (change #1 refactored the domain so this is cleanly expressed as `EventCalendarItem`). We can extend that synchronization to also show a second calendar entry for the event's registration deadline, when one is set. Members will see, on the deadline day: "Přihlášky - {event name}", and clicking it takes them to the event detail.

This is a pure extension of the automatic event→calendar synchronization. No REST API shape change, no frontend code change, no new aggregate.

## What Changes

- Calendar synchronization produces a **second** automatically-managed item per event when the event has a registration deadline set: a **registration-deadline item** labelled `Přihlášky - {event name}`, dated on the deadline day, read-only, and linked to the source event.
- The registration-deadline item appears, disappears, and updates automatically as the event's `registrationDeadline` field is set, cleared, or changed between event revisions.
- When the event's registration deadline equals the event date, **both** items appear on that day — the event-date item and the deadline item. This is intentional: they carry different information.
- Clicking the registration-deadline item navigates to the event detail page, just like the event-date item (both carry the `event` HAL link today).
- The description of the registration-deadline item is empty. The label and the date are sufficient; clicking opens the event detail for everything else.
- **Unified reconcile semantics** in calendar synchronization: when an event is published or updated, the calendar recomputes *which* event-linked items should exist for that event (always one event-date item; additionally one deadline item if `registrationDeadline != null`) and syncs the calendar state to match. Previously, an update to an event whose event-date calendar item somehow did not exist was skipped with a warning; now the calendar self-heals. This keeps the two kinds consistent and removes a silent-failure mode.
- When an event is cancelled, **all** event-linked calendar items for that event are deleted (not just the event-date item as before).
- The `EventCalendarItem` domain class represents both kinds. It gains a `kind` field (`EVENT_DATE` or `EVENT_REGISTRATION_DATE`) that already exists as a persistence discriminator, now promoted to the domain so the class knows which event property drives its date and label. No new subtype, no new aggregate.
- The single existing `EventsEventListener` continues to be the only entry point from the events module into the calendar module. It delegates to the reworked `CalendarEventSyncService`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calendar-items`: extend the `Automatic Synchronization from Events` requirement with scenarios for the registration-deadline item (creation when deadline is present, absence when deadline is null, appearance / disappearance / update as the deadline field changes between event revisions, and coexistence with the event-date item when the two fall on the same day). The existing event-date synchronization scenarios tighten to express the new self-healing reconcile semantics.

## Impact

**Affected code (backend only):**

- `backend/src/main/java/com/klabis/calendar/domain/EventCalendarItem.java` — add `kind` field and getter; add factory for the deadline kind; add a synchronization method that accepts an updated event and recomputes its own name/date according to its kind.
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarItemKind.java` — add `EVENT_REGISTRATION_DATE` value.
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarMemento.java` — extend memento↔domain mapping to round-trip the new kind.
- `backend/src/main/java/com/klabis/calendar/application/CalendarEventSyncService.java` — replace the three independent handle methods with a unified reconcile that computes expected items from the event and drives create/update/delete. `handleEventCancelled` becomes "delete all event-linked items for this event".
- `backend/src/main/java/com/klabis/calendar/infrastructure/listeners/EventsEventListener.java` — delegates all three event types to the service (minor wiring change; no new listener).
- `backend/src/main/java/com/klabis/events/EventData.java` — add `registrationDeadline` field.
- `backend/src/main/java/com/klabis/events/infrastructure/jdbc/EventDataProviderImpl.java` — populate the new field from the event aggregate.
- Calendar module tests updated to cover the new kind and the reconcile semantics.

**Frontend:** None. `CalendarItemDto` is unchanged. The calendar UI already renders every item in the returned list with `_links.event`-based navigation on click; the deadline items appear and behave identically to event-date items.

**APIs (REST):** None. Endpoints, status codes, request/response shapes, HAL links, and affordances are unchanged. The collection endpoint may return one additional item per event that has a deadline.

**Dependencies:** None added or removed.

**Data:** H2 in-memory, no production deployment. The `kind` column already exists; we only add a new allowed value. No migration script change.

**Specs:** `calendar-items/spec.md` — `Automatic Synchronization from Events` requirement extended (new scenarios + unified reconcile wording).
