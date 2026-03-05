# event-registrations Specification

## Purpose

This specification defines requirements for member registration to club events. Members can register themselves for
active events with their SI card number, view registrations, and unregister before the event date.

## ADDED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for events that are in ACTIVE status.

**IMPORTANT CONSTRAINTS:**

- UserName in authentication is `User.userName` (string username), NOT User ID (UUID)
- Not every user has a member record - only users with associated members can register for events
- The system must resolve username to member ID to validate membership before allowing registration

#### Scenario: Successful self-registration

- **WHEN** authenticated member submits POST /api/events/{eventId}/registrations with siCardNumber
- **AND** the event is in ACTIVE status
- **AND** the member is not already registered
- **THEN** the system creates a registration for the member
- **AND** records the provided SI card number
- **AND** records the registration timestamp
- **AND** returns HTTP 201 Created
- **AND** response includes HAL+FORMS links (self, event, unregister)

#### Scenario: Registration with SI card from profile

- **WHEN** member's profile has chipNumber "123456"
- **AND** member submits registration with siCardNumber "123456"
- **THEN** registration is created with SI card number "123456"

#### Scenario: Registration with different SI card than profile

- **WHEN** member's profile has chipNumber "123456"
- **AND** member submits registration with siCardNumber "789012"
- **THEN** registration is created with SI card number "789012" (member can use different card)

#### Scenario: Duplicate registration prevented

- **WHEN** authenticated member attempts to register for an event
- **AND** the member is already registered for that event
- **THEN** HTTP 409 Conflict is returned
- **AND** response includes details about existing registration (siCardNumber, registeredAt)

#### Scenario: Registration for non-ACTIVE event rejected

- **WHEN** authenticated member attempts to register for an event in DRAFT status
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates registration is only allowed for active events

#### Scenario: Registration for FINISHED event rejected

- **WHEN** authenticated member attempts to register for an event in FINISHED status
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates registration is only allowed for active events

#### Scenario: Registration for CANCELLED event rejected

- **WHEN** authenticated member attempts to register for an event in CANCELLED status
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates registration is only allowed for active events

#### Scenario: Registration for non-existent event

- **WHEN** authenticated member attempts to register for a non-existent event
- **THEN** HTTP 404 Not Found is returned

#### Scenario: User without member record cannot register

- **WHEN** authenticated user (with username but no member record) attempts to register for an ACTIVE event
- **THEN** HTTP 403 Forbidden is returned
- **AND** error message indicates user must have a member profile to register for events

#### Scenario: Username to member ID resolution

- **WHEN** authenticated user "john.doe" registers for event
- **AND** user has a member record with UUID "12345678-1234-1234-1234-123456789012"
- **THEN** the system resolves username to member ID
- **AND** creates registration associated with member ID "12345678-1234-1234-1234-123456789012"

### Requirement: Unregister from Event

The system SHALL allow members to unregister themselves from events before the event date.

#### Scenario: Successful unregistration before event date

- **WHEN** authenticated member submits DELETE /api/events/{eventId}/registrations
- **AND** the member is registered for the event
- **AND** the event date is in the future
- **THEN** the system removes the member's registration
- **AND** returns HTTP 204 No Content

#### Scenario: Unregistration on event date rejected

- **WHEN** authenticated member attempts to unregister on the event date
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates unregistration is only allowed before the event date

#### Scenario: Unregistration after event date rejected

- **WHEN** authenticated member attempts to unregister after the event date has passed
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates unregistration is only allowed before the event date

#### Scenario: Unregistration when not registered

- **WHEN** authenticated member attempts to unregister from an event
- **AND** the member is not registered for that event
- **THEN** HTTP 404 Not Found is returned
- **AND** error message indicates no registration found

### Requirement: List Event Registrations

The system SHALL provide an API endpoint to retrieve the list of members registered for an event.

#### Scenario: List registrations for event

- **WHEN** authenticated user makes GET request to /api/events/{eventId}/registrations
- **THEN** the system returns HTTP 200 OK
- **AND** response contains list of registrations
- **AND** each registration includes member's firstName and lastName only (privacy)
- **AND** response includes HATEOAS links

#### Scenario: List registrations includes registration details

- **WHEN** authenticated user lists registrations
- **THEN** each registration shows:
    - Member's first name
    - Member's last name
    - Registration timestamp
- **AND** SI card number is NOT included (privacy - only visible to member themselves)

#### Scenario: List registrations for non-existent event

- **WHEN** authenticated user makes GET request to /api/events/{id}/registrations with non-existent event ID
- **THEN** HTTP 404 Not Found is returned

#### Scenario: Unauthenticated access to registrations

- **WHEN** unauthenticated user attempts to access /api/events/{id}/registrations
- **THEN** HTTP 401 Unauthorized is returned

### Requirement: View Own Registration

The system SHALL allow members to view their own registration details including SI card number.

#### Scenario: Get own registration

- **WHEN** authenticated member makes GET request to /api/events/{eventId}/registrations/me
- **AND** the member is registered for the event
- **THEN** the system returns HTTP 200 OK
- **AND** response includes full registration details (siCardNumber, registeredAt)
- **AND** response includes HAL+FORMS links (self, event, unregister if allowed)

#### Scenario: Get own registration when not registered

- **WHEN** authenticated member makes GET request to /api/events/{eventId}/registrations/me
- **AND** the member is NOT registered for the event
- **THEN** HTTP 404 Not Found is returned
- **AND** response indicates the member is not registered

### Requirement: SiCardNumber Value Object

The system SHALL validate SI card numbers to ensure they contain only digits and are 4-8 characters long.

#### Scenario: Valid 6-digit SI card number accepted

- **WHEN** siCardNumber "123456" is provided
- **THEN** the SiCardNumber is created successfully

#### Scenario: Valid 4-digit SI card number accepted

- **WHEN** siCardNumber "1234" is provided
- **THEN** the SiCardNumber is created successfully

#### Scenario: Valid 8-digit SI card number accepted

- **WHEN** siCardNumber "12345678" is provided
- **THEN** the SiCardNumber is created successfully

#### Scenario: SI card number with letters rejected

- **WHEN** siCardNumber "ABC123" is provided
- **THEN** validation fails with error indicating SI card number must contain only digits

#### Scenario: SI card number too short rejected

- **WHEN** siCardNumber "123" is provided (3 digits)
- **THEN** validation fails with error indicating SI card number must be 4-8 digits

#### Scenario: SI card number too long rejected

- **WHEN** siCardNumber "123456789" is provided (9 digits)
- **THEN** validation fails with error indicating SI card number must be 4-8 digits

#### Scenario: Blank SI card number rejected

- **WHEN** siCardNumber is blank or empty
- **THEN** validation fails with error indicating SI card number is required

### Requirement: Registration Response Format

Registration API responses SHALL follow HAL+FORMS specification.

#### Scenario: Registration timestamps serialized as ISO-8601

- **WHEN** a registration with timestamp is returned
- **THEN** the registeredAt field is serialized as ISO-8601 datetime (e.g., "2026-03-01T10:30:00Z")

#### Scenario: Registration response uses HAL+FORMS media type

- **WHEN** any registration endpoint returns a response
- **THEN** Content-Type is application/prs.hal-forms+json
- **AND** response includes _links object for hypermedia navigation

#### Scenario: Registration list includes event link

- **WHEN** registrations list is returned
- **THEN** response includes link to parent event resource
