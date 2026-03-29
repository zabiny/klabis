## MODIFIED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for events that are in ACTIVE status and have open registrations (event date in the future AND registration deadline not passed).

**IMPORTANT CONSTRAINTS:**

- UserName in authentication is `User.userName` (string username), NOT User ID (UUID)
- Not every user has a member record - only users with associated members can register for events
- The system must resolve username to member ID to validate membership before allowing registration

#### Scenario: Successful self-registration

- **WHEN** authenticated member submits POST /api/events/{eventId}/registrations with siCardNumber
- **AND** the event is in ACTIVE status
- **AND** the event date is in the future
- **AND** the registration deadline has not passed (if set)
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

#### Scenario: Registration after deadline rejected

- **WHEN** authenticated member attempts to register for an ACTIVE event
- **AND** the event has a registration deadline that has passed
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates the registration deadline has passed

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

The system SHALL allow members to unregister themselves from events before the event date and before the registration deadline (if set).

#### Scenario: Successful unregistration before event date

- **WHEN** authenticated member submits DELETE /api/events/{eventId}/registrations
- **AND** the member is registered for the event
- **AND** the event date is in the future
- **AND** the registration deadline has not passed (if set)
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

#### Scenario: Unregistration after deadline rejected

- **WHEN** authenticated member attempts to unregister from an event
- **AND** the event has a registration deadline that has passed
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates the registration deadline has passed

#### Scenario: Unregistration when not registered

- **WHEN** authenticated member attempts to unregister from an event
- **AND** the member is not registered for that event
- **THEN** HTTP 404 Not Found is returned
- **AND** error message indicates no registration found
