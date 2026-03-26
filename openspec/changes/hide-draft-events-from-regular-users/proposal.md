## Why

DRAFT events are currently visible to all authenticated users with `EVENTS:READ` permission (which is granted to every standard member). This leaks event planning details before they are ready to be published — regular members should only see events in `ACTIVE`, `FINISHED`, or `CANCELLED` status. Only users with `EVENTS:MANAGE` can view DRAFT events.

## What Changes

- Filter DRAFT events from list endpoint (`GET /api/events`) for users without `EVENTS:MANAGE` permission
- Filter DRAFT events from status-filtered list (`GET /api/events?status=DRAFT`) for users without `EVENTS:MANAGE`
- Return 404 on detail endpoint (`GET /api/events/{id}`) when a DRAFT event is requested by a user without `EVENTS:MANAGE`
- Frontend event list should not display DRAFT events for regular users (backend enforces this, frontend follows)

## Capabilities

### New Capabilities

_(none — this change modifies existing event visibility behavior)_

### Modified Capabilities

- `events`: Add visibility rules — DRAFT events are only visible to users with `EVENTS:MANAGE` permission. Affects list and detail retrieval scenarios.

## Impact

- **Backend**: `EventController` — authority check and DRAFT filtering decision; `EventManagementServiceImpl` — extended with status exclusion filter parameter
- **API**: No new endpoints; existing endpoints return filtered results based on caller's authority
- **Frontend**: No code changes needed if frontend already handles missing events gracefully (events simply won't appear in API responses)
- **Tests**: New test scenarios for draft visibility filtering
