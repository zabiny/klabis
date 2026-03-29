## ADDED Requirements

### Requirement: Registration Deadline

The system SHALL support an optional registration deadline for events. When set, the registration deadline determines the last date when members can register for or unregister from the event.

#### Scenario: Create event with registration deadline

- **WHEN** authorized user creates an event with registrationDeadline set to a future date
- **THEN** the event is created with the specified registration deadline

#### Scenario: Create event without registration deadline

- **WHEN** authorized user creates an event without registrationDeadline
- **THEN** the event is created without a registration deadline
- **AND** registration availability is determined solely by event status and event date

#### Scenario: Update event registration deadline

- **WHEN** authorized user updates registrationDeadline on a DRAFT or ACTIVE event
- **THEN** the registration deadline is updated

#### Scenario: Registration deadline must be before or on event date

- **WHEN** authorized user sets registrationDeadline to a date after the event date
- **THEN** the system rejects the request with a validation error

#### Scenario: Registrations closed after deadline passes

- **WHEN** an event has registrationDeadline set
- **AND** the current date is after the registration deadline
- **THEN** registrations are closed regardless of event status or event date

#### Scenario: Registrations open before deadline

- **WHEN** an event is ACTIVE with a future event date
- **AND** the event has registrationDeadline set to a future date
- **THEN** registrations are open

#### Scenario: No deadline means original behavior

- **WHEN** an event has no registrationDeadline set
- **THEN** registrations are open when the event is ACTIVE and the event date is in the future

### Requirement: ORIS Import Includes Registration Deadline

The system SHALL import the registration deadline from ORIS when importing an event. The primary entry deadline from ORIS (EntryDate1) SHALL be mapped to the event's registrationDeadline.

#### Scenario: Import event with registration deadline from ORIS

- **WHEN** authorized user imports an event from ORIS
- **AND** the ORIS event has a primary entry deadline set
- **THEN** the imported event includes registrationDeadline derived from the ORIS entry deadline date

#### Scenario: Import event without registration deadline from ORIS

- **WHEN** authorized user imports an event from ORIS
- **AND** the ORIS event has no primary entry deadline
- **THEN** the imported event has no registrationDeadline set

### Requirement: Events Table Display

The application SHALL display the events list as a table with columns providing key information at a glance, enabling users to see event details, navigate to external resources, and register for events without opening the detail page.

#### Scenario: Events table columns for regular user

- **WHEN** a club member views the events list page
- **THEN** the table displays columns: date, name, location, organizer, website link, registration deadline, coordinator name, and registration action

#### Scenario: Events table columns for manager

- **WHEN** a user with EVENTS:MANAGE permission views the events list page
- **THEN** the table displays all columns visible to regular users plus an additional status column

#### Scenario: Website link column displays clickable icon

- **WHEN** an event in the table has an external website URL
- **THEN** the website column displays a clickable icon that opens the URL in a new tab

#### Scenario: Website link column empty when no URL

- **WHEN** an event in the table has no external website URL
- **THEN** the website column is empty for that row

#### Scenario: Registration deadline column displays formatted date

- **WHEN** an event in the table has a registration deadline set
- **THEN** the registration deadline column displays the deadline as a formatted date

#### Scenario: Registration deadline column empty when not set

- **WHEN** an event in the table has no registration deadline
- **THEN** the registration deadline column is empty for that row

#### Scenario: Coordinator column displays name with link

- **WHEN** an event in the table has a coordinator assigned
- **THEN** the coordinator column displays the coordinator's full name as a clickable link to the member detail page

#### Scenario: Coordinator column empty when not assigned

- **WHEN** an event in the table has no coordinator assigned
- **THEN** the coordinator column is empty for that row

#### Scenario: Action column shows register button

- **WHEN** an event in the table has open registrations
- **AND** the current user is not registered for the event
- **THEN** the action column displays a register button

#### Scenario: Action column shows unregister button

- **WHEN** an event in the table has open registrations
- **AND** the current user is already registered for the event
- **THEN** the action column displays an unregister button

#### Scenario: Action column empty for closed registrations

- **WHEN** an event in the table does not have open registrations
- **THEN** the action column is empty for that row

#### Scenario: Status column conditionally visible

- **WHEN** the API response does not include the status field (field-level security)
- **THEN** the status column is not displayed in the table

### Requirement: Event Detail Page Displays Registration Deadline

The application SHALL display the registration deadline on the event detail page when it is set, and allow managers to edit it inline.

#### Scenario: Event detail shows registration deadline

- **WHEN** a user views the detail page for an event with a registration deadline set
- **THEN** the event information section displays the registration deadline as a formatted date

#### Scenario: Event detail hides registration deadline when not set

- **WHEN** a user views the detail page for an event without a registration deadline
- **THEN** no registration deadline row is shown in the event information section

#### Scenario: Event detail inline edit includes registration deadline

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the registration deadline field is editable as a date picker

#### Scenario: Event create/edit form includes registration deadline

- **WHEN** a manager creates or edits an event via the form
- **THEN** the form includes a registration deadline date picker field

## MODIFIED Requirements

### Requirement: List Events

The system SHALL provide an API endpoint to retrieve a paginated list of events with filtering capabilities. DRAFT events SHALL only be included in results for users with `EVENTS:MANAGE` permission.

#### Scenario: List all events with default pagination

- **WHEN** authenticated user makes GET request to /api/events without parameters
- **THEN** the system returns HTTP 200 OK
- **AND** response contains first page of events (page 0, size 10)
- **AND** response includes page metadata (size, totalElements, totalPages, number)
- **AND** response includes HATEOAS pagination links (self, first, last, next if applicable)

#### Scenario: Event summary includes extended fields

- **WHEN** authenticated user lists events
- **THEN** each event summary includes: name, eventDate, location, organizer, websiteUrl, registrationDeadline
- **AND** each event summary includes status only for users with EVENTS:MANAGE permission

#### Scenario: Event summary includes coordinator link

- **WHEN** an event in the list has an event coordinator assigned
- **THEN** the event summary includes a `coordinator` HATEOAS link pointing to the coordinator's member resource

#### Scenario: Event summary includes registration affordance

- **WHEN** an event in the list has open registrations
- **AND** the current user is a member who is not registered for the event
- **THEN** the event summary self link includes a `registerForEvent` affordance

#### Scenario: Event summary includes unregistration affordance

- **WHEN** an event in the list has open registrations
- **AND** the current user is a member who is already registered for the event
- **THEN** the event summary self link includes an `unregisterFromEvent` affordance

#### Scenario: Event summary without registration affordance for closed registrations

- **WHEN** an event in the list does not have open registrations (past date, past deadline, or non-ACTIVE status)
- **THEN** the event summary self link does NOT include registration or unregistration affordances

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
- Registration deadline (date format YYYY-MM-DD, must be on or before event date)

#### Scenario: Create event with all required fields

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events with name, eventDate, location, and organizer
- **THEN** the system creates an event in DRAFT status
- **AND** returns HTTP 201 Created with Location header
- **AND** response includes HAL+FORMS links (self, publish, edit)

#### Scenario: Create event with optional fields

- **WHEN** authenticated user with EVENTS:MANAGE permission submits POST /api/events with all required fields plus websiteUrl, eventCoordinator, and registrationDeadline
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

#### Scenario: Create event with registration deadline after event date

- **WHEN** user submits event with registrationDeadline after eventDate
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates registration deadline must be on or before event date

### Requirement: Update Event

The system SHALL allow authorized users to update event information while the event is in DRAFT or ACTIVE status.

#### Scenario: Update event in DRAFT status

- **WHEN** authenticated user with EVENTS:MANAGE permission submits PATCH /api/events/{id} for an event in DRAFT status
- **THEN** the system updates the provided fields (including registrationDeadline)
- **AND** returns HTTP 200 OK with updated event representation
- **AND** response includes HAL+FORMS links
- **AND** EventUpdatedEvent is published with full Event data (eventId, name, eventDate, location, organizer, websiteUrl)

#### Scenario: Update event in ACTIVE status

- **WHEN** authenticated user with EVENTS:MANAGE permission submits PATCH /api/events/{id} for an event in ACTIVE status
- **THEN** the system updates the provided fields (including registrationDeadline)
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

### Requirement: Get Event Detail

The system SHALL provide an API endpoint to retrieve detailed information about a specific event. DRAFT events SHALL only be accessible to users with `EVENTS:MANAGE` permission.

#### Scenario: Get existing event

- **WHEN** authenticated user makes GET request to /api/events/{id}
- **THEN** the system returns HTTP 200 OK
- **AND** response includes all event fields (name, eventDate, location, organizer, status, websiteUrl, eventCoordinator, registrationDeadline)
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

#### Scenario: Event detail displays registration deadline

- **WHEN** authenticated user views event detail for an event with registrationDeadline set
- **THEN** the response includes registrationDeadline field with the deadline date

#### Scenario: Event detail without registration deadline

- **WHEN** authenticated user views event detail for an event without registrationDeadline
- **THEN** the registrationDeadline field is null

## MODIFIED Requirements

### Requirement: Event Response Format

Event API responses SHALL follow HAL+FORMS specification with proper data serialization.

#### Scenario: Event dates serialized as ISO-8601

- **WHEN** an event with eventDate 2026-03-15 is returned
- **THEN** the eventDate field is serialized as "2026-03-15" (ISO-8601 format)

#### Scenario: Registration deadline serialized as ISO-8601

- **WHEN** an event with registrationDeadline 2026-03-10 is returned
- **THEN** the registrationDeadline field is serialized as "2026-03-10" (ISO-8601 format)

#### Scenario: Event response includes coordinator details

- **WHEN** an event with eventCoordinator is returned
- **THEN** response includes coordinator's name (firstName, lastName)
- **AND** response includes link to coordinator's member resource

#### Scenario: Event response uses HAL+FORMS media type

- **WHEN** any event endpoint returns a response
- **THEN** Content-Type is application/prs.hal-forms+json
- **AND** response includes _links object for hypermedia navigation

#### Scenario: Status field visibility based on permission

- **WHEN** an event is returned to a user without EVENTS:MANAGE permission
- **THEN** the status field is NOT included in the response

#### Scenario: Status field visible to managers

- **WHEN** an event is returned to a user with EVENTS:MANAGE permission
- **THEN** the status field IS included in the response
