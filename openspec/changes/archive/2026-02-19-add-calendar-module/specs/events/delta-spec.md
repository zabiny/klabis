# events Specification (Delta)

## MODIFIED Requirements

### Requirement: Update Event

The system SHALL allow authorized users to update event information while event is in DRAFT or ACTIVE status.

#### Scenario: Update event in DRAFT status

- **WHEN** authenticated user with EVENTS:MANAGE permission submits PATCH /api/events/{id} for an event in DRAFT status
- **THEN** system updates the provided fields
- **AND** returns HTTP 200 OK with updated event representation
- **AND** response includes HAL+FORMS links
- **AND** EventUpdatedEvent is published with full Event data (eventId, name, eventDate, location, organizer, websiteUrl)

#### Scenario: Update event in ACTIVE status

- **WHEN** authenticated user with EVENTS:MANAGE permission submits PATCH /api/events/{id} for an event in ACTIVE status
- **THEN** system updates the provided fields
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
