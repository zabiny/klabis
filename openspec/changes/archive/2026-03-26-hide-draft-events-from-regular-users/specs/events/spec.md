## MODIFIED Requirements

### Requirement: List Events

The system SHALL provide an API endpoint to retrieve a paginated list of events with filtering capabilities. DRAFT events SHALL only be included in results for users with `EVENTS:MANAGE` permission.

#### Scenario: List all events with default pagination

- **WHEN** authenticated user makes GET request to /api/events without parameters
- **THEN** the system returns HTTP 200 OK
- **AND** response contains first page of events (page 0, size 10)
- **AND** response includes page metadata (size, totalElements, totalPages, number)
- **AND** response includes HATEOAS pagination links (self, first, last, next if applicable)

#### Scenario: List events excludes DRAFT for regular users

- **WHEN** authenticated user without `EVENTS:MANAGE` permission makes GET request to /api/events
- **THEN** the response SHALL NOT contain events in DRAFT status
- **AND** page metadata reflects the filtered count (excluding DRAFT events)

#### Scenario: List events includes DRAFT for managers

- **WHEN** authenticated user with `EVENTS:MANAGE` permission makes GET request to /api/events
- **THEN** the response SHALL include events in all statuses including DRAFT

#### Scenario: Filter events by DRAFT status requires EVENTS:MANAGE

- **WHEN** authenticated user without `EVENTS:MANAGE` permission makes GET request to /api/events?status=DRAFT
- **THEN** the system returns HTTP 200 OK with empty results
- **AND** no DRAFT events are disclosed

#### Scenario: Filter events by DRAFT status with permission

- **WHEN** authenticated user with `EVENTS:MANAGE` permission makes GET request to /api/events?status=DRAFT
- **THEN** the system returns events in DRAFT status

#### Scenario: Filter events by status

- **WHEN** authenticated user makes GET request to /api/events?status=ACTIVE
- **THEN** the system returns only events with ACTIVE status

#### Scenario: Filter events by organizer

- **WHEN** authenticated user makes GET request to /api/events?organizer=ZBM
- **THEN** the system returns only events organized by "ZBM"

#### Scenario: Filter events by date range

- **WHEN** authenticated user makes GET request to /api/events?from=2026-03-01&to=2026-03-31
- **THEN** the system returns only events with eventDate within the specified range

#### Scenario: Filter events by coordinator

- **WHEN** authenticated user makes GET request to /api/events?eventCoordinator={memberId}
- **THEN** the system returns only events where the specified member is coordinator

#### Scenario: Unauthenticated access to event list

- **WHEN** unauthenticated user attempts to access /api/events
- **THEN** HTTP 401 Unauthorized is returned

### Requirement: Get Event Detail

The system SHALL provide an API endpoint to retrieve detailed information about a specific event. DRAFT events SHALL only be accessible to users with `EVENTS:MANAGE` permission.

#### Scenario: Get existing event

- **WHEN** authenticated user makes GET request to /api/events/{id}
- **THEN** the system returns HTTP 200 OK
- **AND** response includes all event fields (name, eventDate, location, organizer, status, websiteUrl, eventCoordinator)
- **AND** response includes HAL+FORMS links based on event status and user permissions

#### Scenario: Get DRAFT event without permission

- **WHEN** authenticated user without `EVENTS:MANAGE` permission makes GET request to /api/events/{id} for an event in DRAFT status
- **THEN** HTTP 404 Not Found is returned
- **AND** the system SHALL NOT disclose that the event exists

#### Scenario: Get DRAFT event with permission

- **WHEN** authenticated user with `EVENTS:MANAGE` permission makes GET request to /api/events/{id} for an event in DRAFT status
- **THEN** the system returns HTTP 200 OK
- **AND** response includes all event fields and HAL+FORMS links

#### Scenario: Get event includes appropriate links for DRAFT

- **WHEN** user with EVENTS:MANAGE permission views event in DRAFT status
- **THEN** response includes links: self, edit, publish, cancel, registrations

#### Scenario: Get event includes appropriate links for ACTIVE

- **WHEN** user with EVENTS:MANAGE permission views event in ACTIVE status
- **THEN** response includes links: self, edit, cancel, finish, registrations

#### Scenario: Get event includes appropriate links for FINISHED/CANCELLED

- **WHEN** user views event in FINISHED or CANCELLED status
- **THEN** response includes links: self, registrations
- **AND** edit, publish, cancel, finish links are NOT included

#### Scenario: Get non-existent event

- **WHEN** authenticated user makes GET request to /api/events/{id} with non-existent ID
- **THEN** HTTP 404 Not Found is returned
- **AND** response includes error details with problem+json media type
