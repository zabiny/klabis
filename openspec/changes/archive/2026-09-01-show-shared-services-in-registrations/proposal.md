## Why

Events can offer shared transport and shared accommodation, and members declare their choices during registration — but the registration list on the event detail page does not show them. Members cannot see who signed up for which service (e.g. to arrange carpooling), and coordinators must cross-check the accommodation list to learn the same. The data already exists on each registration; only the list wire contract and the table are missing it.

## What Changes

- The registration summary returned in the event detail's `registrationDtoList` embedded collection gains `wantsSharedTransport` and `wantsSharedAccommodation` properties.
- Each property is included only when the event has the matching offer enabled (mirroring the existing contract of the `registerForEvent` / `editRegistration` templates); the property is omitted when the offer is off.
- The shared-service choices in the registration list are visible to every authenticated member viewing the list — they carry no field-level protection, unlike the registration timestamp.
- The registrations table on the event detail page shows a shared transport column and a shared accommodation column, each rendered as "Ano"/"Ne"; a column appears only when the event has that offer enabled. When no offer is enabled, no shared-service column is displayed.
- The event detail's existing "enabled for event" Ano/Ne rows and the shared-services counts are unchanged.

## Capabilities

### New Capabilities

<!-- none -->

### Modified Capabilities

- `event-registrations`: The "List Event Registrations" requirement is extended — each registration row additionally shows the member's shared transport and shared accommodation choice, whenever the event offers that service. The choices are visible to all members viewing the list (no authority or owner restriction).

## Impact

- **API spec** (`docs/openapi/spec/events.yaml`): `RegistrationSummaryDto` gains the two boolean properties with per-offer inclusion documented.
- **Backend**: `RegistrationDtoMapper` fills the new fields from the registration and the event's offer flags; codegen-regenerated `RegistrationSummaryDto`.
- **Frontend**: `RegistrationsTable` in `EventDetailPage.tsx` conditionally renders the two columns from `event.sharedTransportEnabled` / `event.sharedAccommodationEnabled`; reuses existing labels.
- **Tests**: backend controller/mapper tests for per-offer inclusion and values; frontend table tests for conditional columns.
