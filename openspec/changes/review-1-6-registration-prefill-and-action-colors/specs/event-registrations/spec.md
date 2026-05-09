## MODIFIED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for ACTIVE events with open registrations (future event date AND registration deadline not passed). Only users with an associated member profile can register. When the event has categories defined, the member MUST select a category. After a successful registration the member remains on the event detail page and the registration list refreshes to include their new registration.

When the registration form opens, the SI card number field SHALL be prefilled with the member's SI card number from their profile if one is set. The prefilled value MAY be overwritten by the member for this registration without affecting the profile.

#### Scenario: Member registers for an open event

- **WHEN** authenticated member clicks the register button for an active event with open registrations
- **AND** provides their SI card number
- **THEN** the registration is created
- **AND** the member stays on the event detail page
- **AND** the registration list refreshes and shows the new registration
- **AND** the event row shows an unregister button

#### Scenario: Registration form prefills SI card number from profile

- **GIVEN** an authenticated member has an SI card number recorded in their profile
- **WHEN** the member opens the registration form for an active event
- **THEN** the SI card number field is prefilled with the value from the profile
- **AND** the member can submit the form immediately without retyping the SI card number

#### Scenario: Member overwrites the prefilled SI card number

- **GIVEN** the registration form is prefilled with the member's profile SI card number
- **WHEN** the member overwrites the SI card number field with a different value and submits
- **THEN** the registration is created with the overwritten value
- **AND** the member's profile SI card number remains unchanged

#### Scenario: Registration form has empty SI card number when not in profile

- **GIVEN** an authenticated member has no SI card number recorded in their profile
- **WHEN** the member opens the registration form for an active event
- **THEN** the SI card number field is empty
- **AND** the member must enter the SI card number manually before submitting

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
