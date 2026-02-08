## Why

The club needs to manage events (races, training sessions, meetings) that it organizes or participates in. Members need
to register for events, and coordinators need visibility into who is attending. Currently there is no system support for
event management - everything is handled manually or via external tools.

## What Changes

- New `events` module for managing club events
- Event CRUD operations with lifecycle states (DRAFT → ACTIVE → FINISHED/CANCELLED)
- Member self-registration for events with SI card number
- Automatic event completion after event date passes
- New permission `EVENTS:MANAGE` for event administration
- API endpoints for event management and registration

## Capabilities

### New Capabilities

- `events`: Core event management - create, update, publish, cancel, and finish events. Events have name, date,
  location, organizer (club code), optional website URL, and optional coordinator (club member). Supports lifecycle
  states and automatic completion.
- `event-registrations`: Member self-registration for events. Members can register with SI card number (pre-filled from
  profile but editable). Supports unregistration before event date. Prevents duplicate registrations.

### Modified Capabilities

- `users`: Add new permission `EVENTS:MANAGE` to authorization system

## Impact

- **New module**: `com.klabis.events` Spring Modulith module
- **API**: New endpoints under `/api/events` and `/api/events/{id}/registrations`
- **Database**: New tables for events and event registrations
- **Dependencies**: Events module depends on Users module (for UserId, permissions)
- **Scheduled jobs**: Background task to auto-finish events after date passes
