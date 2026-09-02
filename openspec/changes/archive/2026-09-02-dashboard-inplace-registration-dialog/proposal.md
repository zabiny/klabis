## Why

The "Končící přihlášky tento týden" widget on the home dashboard opens a generic HAL-FORMS modal for registration instead of the customized registration dialog used everywhere else (event detail page, events list). Members registering from the dashboard get an inconsistent form without the SI-chip prefill UX, event/deadline context header, and the offer-aware shared-service checkboxes introduced by `event-registrations` requirements. The dashboard spec is also self-contradictory: the requirement mandates the in-place registration form while one scenario still describes navigation to the event detail page.

## What Changes

- The widget's "Přihlásit se" action opens the customized in-place registration dialog (the same `EventRegistrationDialog` flow as the events list and event detail page) instead of the generic HAL-FORMS modal.
- The events list/summary representation carries the shared-offer flags (`sharedTransportEnabled`, `sharedAccommodationEnabled`) so the registration dialog can show/hide the shared-service checkboxes correctly wherever it is opened from a list row (dashboard widget and events list).
- Dashboard spec delta: the contradictory "opens the event detail" scenario is replaced by the in-place dialog behavior already mandated by the requirement text.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `dashboard`: "Přihlásit se" in the Upcoming Deadlines widget opens the customized in-place registration dialog (same flow as the events list) instead of navigating to the event detail page; the contradictory navigation scenario is removed.
- `events`: the event list representation (`EventSummaryDto`) exposes the shared-offer flags so the registration form opened from a list row can honor the "choice offered only when the event has the offer turned on" rule from `event-registrations`.

## Impact

- **Backend**: `docs/openapi/spec/events.yaml` (`EventSummaryDto` + offer flags), `EventSummaryDtoConverter` mapping, regenerated codegen DTO.
- **Frontend**: `UpcomingDeadlinesWidget.tsx` (dialog swap), `useUpcomingDeadlines.ts` (item fields), regenerated API types; no changes needed in `EventsPage` (flags flow through automatically).
- **Specs**: `openspec/specs/dashboard/spec.md` scenario alignment; `event-registrations` requirements remain unchanged — this change makes them hold on list surfaces.
