## MODIFIED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for ACTIVE events with open registrations (future event date AND registration deadline not passed). Only users with an associated member profile can register. When the event has categories defined, the member MUST select a category.

#### Scenario: Member registers for an open event

- **WHEN** authenticated member clicks the register button for an active event with open registrations
- **AND** provides their SI card number
- **THEN** the registration is created
- **AND** the event row shows an unregister button

#### Scenario: Member registers for an event with categories

- **WHEN** authenticated member registers for an event that has categories defined
- **THEN** the registration form includes a category selection field with the event's available categories
- **AND** the member MUST select one category before submitting

#### Scenario: Member registers for an event without categories

- **WHEN** authenticated member registers for an event that has no categories defined
- **THEN** the registration form does not include a category selection field
- **AND** the registration is created without a category

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

### Requirement: List Event Registrations

The system SHALL display the list of members registered for an event. Each registration shows the member's first name, last name, selected category (if applicable), and registration timestamp. SI card numbers are not shown to other members.

#### Scenario: User views registration list for an event

- **WHEN** authenticated user views the registration list for an event
- **THEN** the list shows each registered member's first name, last name, category (if selected), and registration time
- **AND** SI card numbers are not shown

#### Scenario: Registration list shows category column only when event has categories

- **WHEN** authenticated user views the registration list for an event without categories
- **THEN** no category column is displayed in the registration list
