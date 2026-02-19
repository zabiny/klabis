# events Specification

## Purpose

This specification defines requirements for club event management. It encompasses creating, updating, and managing
events through their lifecycle (draft, active, cancelled, finished), including automatic completion after the event date
passes.

## Requirements

### Requirement: Create Event

The system SHALL allow authorized users to create new events with required and optional information.

**Required fields:**

- Event name (max 200 characters)
- Event date (date format YYYY-MM-DD)
- Location description (max 200 characters)
- Organizer code (max 10 characters, e.g., "ZBM", "ABS")

**Optional fields:**

- Website URL (validated http/https URL)
- Event coordinator (reference to a club member)

#### Scenario: Create event with all required fields

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events with name, eventDate, location, and
  organizer
- **THEN** the system creates an event in DRAFT status
- **AND** returns HTTP 201 Created with Location header
- **AND** response includes HAL+FORMS links (self, publish, edit)

#### Scenario: Create event with optional fields

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events with all required fields plus
  websiteUrl and eventCoordinator
- **THEN** the system creates an event with all provided data
- **AND** eventCoordinator references an existing club member

#### Scenario: Create event without permission

- **WHEN** authenticated user without EVENTS:MANAGE permission attempts to create an event
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes error details with problem+json media type

#### Scenario: Create event with invalid data

- **WHEN** user submits event with missing required fields or invalid format
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field

#### Scenario: Create event with invalid website URL

- **WHEN** user submits event with websiteUrl that is not a valid http/https URL
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates websiteUrl must be a valid URL

#### Scenario: Create event with non-existent coordinator

- **WHEN** user submits event with eventCoordinator that does not reference an existing member
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates the coordinator member was not found

### Requirement: Update Event

The system SHALL allow authorized users to update event information while the event is in DRAFT or ACTIVE status.

#### Scenario: Update event in DRAFT status

- **WHEN** authenticated user with EVENTS:MANAGE permission submits PATCH /api/events/{id} for an event in DRAFT status
- **THEN** the system updates the provided fields
- **AND** returns HTTP 200 OK with updated event representation
- **AND** response includes HAL+FORMS links
- **AND** EventUpdatedEvent is published with full Event data (eventId, name, eventDate, location, organizer, websiteUrl)

#### Scenario: Update event in ACTIVE status

- **WHEN** authenticated user with EVENTS:MANAGE permission submits PATCH /api/events/{id} for an event in ACTIVE status
- **THEN** the system updates the provided fields
- **AND** returns HTTP 200 OK with updated event representation
- **AND** response includes HAL+FORMS links
- **AND** EventUpdatedEvent is published with full Event data (eventId, name, eventDate, location, organizer, websiteUrl)

#### Scenario: Update event in FINISHED status

- **WHEN** authenticated user attempts to update an event in FINISHED status
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates finished events cannot be modified

#### Scenario: Update event in CANCELLED status

- **WHEN** authenticated user attempts to update an event in CANCELLED status
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates cancelled events cannot be modified

#### Scenario: Update event without permission

- **WHEN** authenticated user without EVENTS:MANAGE permission attempts to update an event
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: EventUpdatedEvent includes all updatable fields

- **WHEN** Event is updated with name, eventDate, location, organizer, or websiteUrl
- **THEN** EventUpdatedEvent payload includes:
  - eventId: Event identifier
  - name: Updated event name
  - eventDate: Updated event date
  - location: Updated event location
  - organizer: Updated event organizer
  - websiteUrl: Updated website URL (may be null)
- **AND** Calendar module can use this event to update linked CalendarItem without querying Events repository

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions according to the defined lifecycle: DRAFT → ACTIVE → FINISHED or
CANCELLED.

**Allowed transitions:**

- DRAFT → ACTIVE (publish)
- DRAFT → CANCELLED (cancel)
- ACTIVE → CANCELLED (cancel)
- ACTIVE → FINISHED (manual finish or automatic after event date)

#### Scenario: Publish event (DRAFT to ACTIVE)

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events/{id}/publish for an event in DRAFT
  status
- **THEN** the system changes status to ACTIVE
- **AND** returns HTTP 200 OK with updated event representation
- **AND** members can now register for the event

#### Scenario: Cancel draft event

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events/{id}/cancel for an event in DRAFT
  status
- **THEN** the system changes status to CANCELLED
- **AND** returns HTTP 200 OK with updated event representation

#### Scenario: Cancel active event

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events/{id}/cancel for an event in ACTIVE
  status
- **THEN** the system changes status to CANCELLED
- **AND** returns HTTP 200 OK with updated event representation
- **AND** existing registrations are preserved for record-keeping

#### Scenario: Manually finish active event

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events/{id}/finish for an event in ACTIVE
  status
- **THEN** the system changes status to FINISHED
- **AND** returns HTTP 200 OK with updated event representation
- **AND** existing registrations are preserved for record-keeping

#### Scenario: Invalid status transition

- **WHEN** user attempts an invalid status transition (e.g., FINISHED → ACTIVE)
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates the transition is not allowed

### Requirement: Automatic Event Completion

The system SHALL automatically transition ACTIVE events to FINISHED status after the event date has passed.

#### Scenario: Automatic transition to FINISHED

- **GIVEN** an event is in ACTIVE status
- **AND** the event date has passed (current date > eventDate)
- **WHEN** the automatic completion process runs
- **THEN** the event status changes to FINISHED
- **AND** existing registrations are preserved

#### Scenario: DRAFT events not auto-finished

- **GIVEN** an event is in DRAFT status
- **AND** the event date has passed
- **WHEN** the automatic completion process runs
- **THEN** the event status remains DRAFT (not automatically finished)

### Requirement: List Events

The system SHALL provide an API endpoint to retrieve a paginated list of events with filtering capabilities.

#### Scenario: List all events with default pagination

- **WHEN** authenticated user makes GET request to /api/events without parameters
- **THEN** the system returns HTTP 200 OK
- **AND** response contains first page of events (page 0, size 10)
- **AND** response includes page metadata (size, totalElements, totalPages, number)
- **AND** response includes HATEOAS pagination links (self, first, last, next if applicable)

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

The system SHALL provide an API endpoint to retrieve detailed information about a specific event.

#### Scenario: Get existing event

- **WHEN** authenticated user makes GET request to /api/events/{id}
- **THEN** the system returns HTTP 200 OK
- **AND** response includes all event fields (name, eventDate, location, organizer, status, websiteUrl,
  eventCoordinator)
- **AND** response includes HAL+FORMS links based on event status and user permissions

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

### Requirement: EventId Value Object

The system SHALL use EventId as a unique identifier for events. The identifier wraps a UUID and provides type safety.

#### Scenario: EventId wraps UUID value

- **WHEN** an EventId is created with a valid UUID
- **THEN** the identifier is successfully created
- **AND** the identifier is immutable
- **AND** the identifier provides equality based on the wrapped UUID

#### Scenario: EventId prevents null UUID

- **WHEN** an EventId is created with null UUID
- **THEN** validation fails with error indicating UUID cannot be null

### Requirement: WebsiteUrl Value Object

The system SHALL validate website URLs to ensure they are valid http or https URLs.

#### Scenario: Valid https URL accepted

- **WHEN** websiteUrl "https://example.com/event" is provided
- **THEN** the WebsiteUrl is created successfully

#### Scenario: Valid http URL accepted

- **WHEN** websiteUrl "http://example.com/event" is provided
- **THEN** the WebsiteUrl is created successfully

#### Scenario: Invalid URL rejected

- **WHEN** websiteUrl "not-a-url" is provided
- **THEN** validation fails with error indicating URL format is invalid

#### Scenario: Non-http/https URL rejected

- **WHEN** websiteUrl "ftp://example.com/file" is provided
- **THEN** validation fails with error indicating only http/https URLs are allowed

#### Scenario: Blank URL rejected

- **WHEN** websiteUrl is blank or empty
- **THEN** validation fails with error indicating URL is required (when field is provided)

### Requirement: Event Response Format

Event API responses SHALL follow HAL+FORMS specification with proper data serialization.

#### Scenario: Event dates serialized as ISO-8601

- **WHEN** an event with eventDate 2026-03-15 is returned
- **THEN** the eventDate field is serialized as "2026-03-15" (ISO-8601 format)

#### Scenario: Event response includes coordinator details

- **WHEN** an event with eventCoordinator is returned
- **THEN** response includes coordinator's name (firstName, lastName)
- **AND** response includes link to coordinator's member resource

#### Scenario: Event response uses HAL+FORMS media type

- **WHEN** any event endpoint returns a response
- **THEN** Content-Type is application/prs.hal-forms+json
- **AND** response includes _links object for hypermedia navigation
