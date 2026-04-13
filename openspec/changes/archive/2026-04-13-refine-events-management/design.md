## Context

The `events` capability is implemented by a single `Event` aggregate that carries name, date, location, organizer, optional website URL, optional coordinator, optional registration deadline, status, and categories. Location is currently required at three gates: a `@NotBlank` on each command record, a private `validateLocation(...)` method called from both factory paths, and a database `NOT NULL` column. Event publish / cancel / finish / sync-from-ORIS are exposed as dedicated POST endpoints on `EventController`, and the events list response attaches HAL affordances to each row through `addLinksForListItem`.

Two neighboring subsystems observe changes to `Event`:

- `CalendarEventSyncService` listens for event lifecycle events and calls `CalendarItem.createForEvent(...)` / `synchronizeFromEvent(...)`. Both paths feed `location` and `organizer` into `CalendarItem.buildEventDescription(...)`, which concatenates them with `" - "`.
- The `EventCompletionScheduler` calls `EventManagementService.finishExpiredActiveEvents(...)`, which iterates ACTIVE events past their event date and invokes `Event.finish()` on each.

See `proposal.md` for motivation and `specs/events/spec.md` + `specs/calendar-items/spec.md` in this change for the updated requirements.

## Goals / Non-Goals

**Goals:**
- Let ORIS imports succeed when the upstream event has no location.
- Remove the manual "finish event" action from both the UI and the API surface, relying exclusively on the scheduled completion path.
- Put row-level management actions on the events list where managers actually need them, driven by HAL affordances the same way the members table already works.
- Swap the exclusive region picker in the ORIS import dialog from checkboxes to radio buttons without requiring a backend change.
- Keep the calendar item description meaningful when an event has no location.

**Non-Goals:**
- Reworking the background completion scheduler or introducing a separate lifecycle port.
- Changing how `status` field-level security is applied to the list response.
- Making organizer optional (organizer remains required on `Event`).
- Tightening the backend `GET /api/oris/events` signature — leaving it as `List<String>` keeps the frontend change trivial.
- Re-opening the recently accepted "calendar item description is optional" change — the description field stays nullable; this proposal only changes how event sync populates it.

## Decisions

### Decision 1: Relax location to nullable everywhere in one pass

**Choice:** Remove `@NotBlank` from every `Event` command record that carries location (`CreateEvent`, `UpdateEvent`, `CreateEventFromOris`, `SyncFromOris`), delete `validateLocation(...)` and its call sites, relax the database column to `NULL` in `V001__initial_schema.sql`, and let the field be a plain nullable `String` in the aggregate. `@Size(max = 200)` stays so overly long values are still rejected when provided.

**Why:** Location was required in four places that all enforced the same rule. Relaxing any subset would leave the others as silent traps — a null from ORIS would pass the database layer but fail on `validateLocation`, and so on. The cleanest fix is to make the field consistently nullable from top to bottom in a single change, with `@Size` retained as the one constraint that still makes sense.

**Alternatives considered:**
- **Default missing ORIS location to an empty string or `"Neznámé"`.** Rejected: the downstream UI would then render a meaningless placeholder. Users want to know the location is unknown, not see misleading filler text.
- **Keep location required on manual create/update, relax only on ORIS import.** Rejected: two different validation rules for the same field produce the worst of both worlds — the domain would have to route via different commands, and the list/detail views would still have to cope with the rare legitimate null.

### Decision 2: `buildEventDescription` joins only what it has, collapses empty to null

**Choice:** Replace the current `location + " - " + organizer` concatenation with a null-safe join:

- Collect the non-null, non-blank values from `location` and `organizer` in order, join them with `" - "`.
- If a website URL is provided, append it on a new line.
- If the resulting string is empty, return `null` (not `""`).

**Why:** The previous `calendar-items` proposal already made the description field nullable and normalizes blank inputs to `null`. This change keeps that invariant intact: a synchronized calendar item for an event with no location, organizer, or URL has `description = null`, not `description = " - "` or `""`. Returning `null` also lets the detail view naturally hide the description row using the existing "no description" rendering path from the calendar-items proposal.

**Alternatives considered:**
- **Always return a string, even if empty.** Rejected: re-introduces the "two representations of empty" problem that the calendar-items proposal explicitly ruled out.
- **Move the null-handling to `CalendarEventSyncService`.** Rejected: the description-assembly rule belongs to the `CalendarItem` aggregate, not to the sync service. The service's job is to shuttle data; the aggregate's job is to know what a valid calendar item looks like.
- **Introduce a dedicated `EventDescriptionBuilder` helper.** Rejected: over-engineering for a 5-line method that lives on the aggregate and has exactly one caller.

### Decision 3: Remove the manual finish endpoint, keep the domain method

**Choice:** Delete `EventController.finishEvent(UUID)`, `EventManagementPort.finishEvent(EventId)`, `EventManagementService.finishEvent(EventId)`, the `finishEvent` affordance in `addLinksForEvent`, and the frontend "Ukončit akci" button. Keep `Event.finish()` and its `status.validateTransition(EventStatus.FINISHED)` guard — the scheduler calls `finishExpiredActiveEvents(...)` → `Event.finish()`, and that path continues to need both.

**Why:** The scheduler already automates the `ACTIVE → FINISHED` transition once the event date has passed (see the existing `Automatic Event Completion` requirement). Keeping a second, manually-triggered route to the same transition doubles the UX surface and the spec surface for no business reason. By removing only the manual entry points and leaving the aggregate method, the scheduler keeps working unchanged and the status-transition guard still catches any accidental re-entry from future code paths.

**Alternatives considered:**
- **Also delete `Event.finish()`** and have the scheduler set the status directly. Rejected: bypasses the status-transition guard and the `EventFinishedEvent` registration inside the aggregate, violating the aggregate-root contract.
- **Keep the manual endpoint but hide it behind a feature flag.** Rejected: the system has no production deployment, no operator wants a "click to override the scheduler" hatch, and dead code behind flags rots.

### Decision 4: Share row-level affordance-building between list and detail

**Choice:** Extract the status-driven affordance switch out of `addLinksForEvent` into a private helper (`addManagementAffordances(LinkBuilder, Event, CurrentUserData)` or similar). Both `addLinksForListItem` and `addLinksForEvent` call the helper so the list rows and the detail page attach the same edit / cancel / publish / sync-from-ORIS affordances. The existing register/unregister logic stays where it is — it is already duplicated between the two methods for a reason (detail page has slightly different context) but it is small and the refactor can leave it alone.

**Why:** Duplicating a 20-line switch across two methods would be an invitation to drift. The helper keeps the rule "what a manager can do with this event" in exactly one place, which makes the spec scenarios easier to verify and the frontend easier to reason about.

**Alternatives considered:**
- **Inline the same switch into `addLinksForListItem`.** Rejected: straight duplication.
- **Move the whole logic onto the `Event` aggregate as a `getAvailableActions()` method that returns an enum set.** Rejected: mixes REST-layer affordances into the domain layer. The domain already exposes `status` and `orisId`; that is enough for the controller to decide affordances.

### Decision 5: ORIS dialog region picker — frontend-only switch

**Choice:** Replace the checkbox group in `ImportOrisEventModal.tsx` with a `<input type="radio">` group of the same three options. State type moves from `string[]` to `string`. The fetch call continues to build `?region=<value>` with a single value. Backend `OrisController.listOrisEvents` stays as `List<String>`.

**Why:** The change is cosmetic to the backend — a list of one element is still a list. Touching the backend signature would ripple into the API docs, OpenAPI regeneration, and potentially any external consumers. The cost of "tolerate a size-1 list" on the backend is zero; the benefit of leaving it alone is a much smaller change.

**Alternatives considered:**
- **Tighten `OrisController` to a scalar `region`.** Rejected: more work, no meaningful win.
- **Remove the region selector altogether and default to "all three".** Rejected: the whole point of the selector is to avoid loading all ORIS events into the dropdown.

## Risks / Trade-offs

- **Risk:** Making location nullable could break reports or integrations that assume "every event has a location". **Mitigation:** None currently exist — the codebase is single-module, the only reader besides the events API is `CalendarEventSyncService`, which is updated in the same change. If such a consumer appears later it needs to decide its own fallback.

- **Risk:** Removing the manual finish endpoint is a breaking change for any API client that calls it directly. **Mitigation:** The project has no external API consumers; the frontend is the only client and is updated in the same change.

- **Risk:** The refactored affordance helper could change which affordances the detail page emits if it is not careful. **Mitigation:** The helper's tests cover every combination of status × permissions × `orisId`, and the existing `EventControllerTest` fixtures are extended to assert the full affordance set on both list items and detail responses.

- **Trade-off:** Tolerant backend (`List<String>` region param) keeps the code slightly less precise about "exactly one region at a time". The trade is accepted because tightening would bleed into OpenAPI regeneration and does not improve user-visible behavior.

- **Trade-off:** `buildEventDescription` returning `null` means "empty" and "no description" become indistinguishable in storage. That is deliberate — it was the explicit outcome of the calendar-items proposal — and this change simply honors it.
