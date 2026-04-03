## MODIFIED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for ACTIVE events with open registrations (future event date AND registration deadline not passed). Only users with an associated member profile can register.

#### Scenario: Member registers for an open event

- **WHEN** authenticated member clicks the register button for an active event with open registrations
- **AND** provides their SI card number
- **THEN** the registration is created
- **AND** the event row shows an unregister button

#### Scenario: Member registers with a different SI card than their profile

- **WHEN** member provides an SI card number that differs from the one stored in their profile
- **THEN** the registration is created with the provided SI card number

#### Scenario: Duplicate registration prevented

- **WHEN** member attempts to register for an event they are already registered for
- **THEN** the system shows an error that the member is already registered

#### Scenario: Registration for a non-active event not allowed

- **WHEN** member attempts to register for an event that is not in ACTIVE status (DRAFT, FINISHED, or CANCELLED)
- **THEN** the system shows an error that registration is only allowed for active events

#### Scenario: Registration after deadline not allowed

- **WHEN** member attempts to register for an active event whose registration deadline has passed
- **THEN** the system shows an error that the registration deadline has passed

#### Scenario: User without member profile cannot register

- **WHEN** authenticated user without a member profile attempts to register for an event
- **THEN** the system shows an error that a member profile is required to register for events

### Requirement: Unregister from Event

The system SHALL allow members to cancel their event registration before the event date and before the registration deadline (if set).

#### Scenario: Member unregisters before event date

- **WHEN** authenticated member clicks the unregister button for an event before the event date
- **AND** the registration deadline has not passed
- **THEN** the registration is cancelled
- **AND** the event row shows a register button again

#### Scenario: Unregistration on or after event date not allowed

- **WHEN** member attempts to unregister on or after the event date
- **THEN** the system shows an error that unregistration is only allowed before the event date

#### Scenario: Unregistration after deadline not allowed

- **WHEN** member attempts to unregister from an event whose registration deadline has passed
- **THEN** the system shows an error that the registration deadline has passed

#### Scenario: Unregistration when not registered shows error

- **WHEN** member attempts to unregister from an event they are not registered for
- **THEN** the system shows an error that no registration was found

### Requirement: List Event Registrations

The system SHALL display the list of members registered for an event. Each registration shows the member's first name, last name, and registration timestamp. SI card numbers are not shown to other members.

#### Scenario: User views registration list for an event

- **WHEN** authenticated user views the registration list for an event
- **THEN** the list shows each registered member's first name, last name, and registration time
- **AND** SI card numbers are not shown

### Requirement: View Own Registration

The system SHALL allow members to view their own registration details including their SI card number.

#### Scenario: Member views their own registration

- **WHEN** authenticated member views their own registration for an event
- **THEN** the registration details are shown including SI card number and registration time
- **AND** unregister action is shown if unregistration is still allowed

#### Scenario: Own registration page shows not found when not registered

- **WHEN** authenticated member navigates to their own registration for an event they are not registered for
- **THEN** the system shows a not-found message

### Requirement: SI Card Number Validation

The system SHALL validate SI card numbers. A valid SI card number contains only digits and is 4–8 characters long.

#### Scenario: Valid SI card number accepted

- **WHEN** member enters a valid SI card number (4–8 digits)
- **THEN** the form accepts the value

#### Scenario: SI card number with letters rejected

- **WHEN** member enters an SI card number containing non-digit characters
- **THEN** the form shows an error that the SI card number must contain only digits

#### Scenario: SI card number too short rejected

- **WHEN** member enters an SI card number with fewer than 4 digits
- **THEN** the form shows an error that the SI card number must be 4–8 digits

#### Scenario: SI card number too long rejected

- **WHEN** member enters an SI card number with more than 8 digits
- **THEN** the form shows an error that the SI card number must be 4–8 digits

#### Scenario: Blank SI card number rejected

- **WHEN** member submits registration without entering an SI card number
- **THEN** the form shows an error that the SI card number is required
