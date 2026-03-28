# oris — delta spec: Seznam závodů pro import

## Requirement: List Upcoming ORIS Events

The system SHALL provide an endpoint listing upcoming ORIS events available for import.

### Scenario: List upcoming events

- **WHEN** authenticated user with `EVENTS:MANAGE` permission makes `GET /api/oris/events`
- **THEN** HTTP 200 OK is returned
- **AND** response contains events from ORIS with `eventDate` between today and today + 1 year
- **AND** each item contains: `id` (ORIS integer ID), `name`, `date`

### Scenario: Endpoint unavailable when ORIS inactive

- **WHEN** ORIS integration profile is not active
- **THEN** the endpoint does not exist (no bean registered)

### Scenario: Access without permission

- **WHEN** authenticated user without `EVENTS:MANAGE` permission calls `GET /api/oris/events`
- **THEN** HTTP 403 Forbidden is returned
