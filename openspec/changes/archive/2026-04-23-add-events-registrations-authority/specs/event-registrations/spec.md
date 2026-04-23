## MODIFIED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for ACTIVE events with open registrations (future event date AND registration deadline not passed). Only users with an associated member profile can register. When the event has categories defined, the member MUST select a category. After a successful registration the member remains on the event detail page and the registration list refreshes to include their new registration.

#### Scenario: Member registers for an open event

- **WHEN** authenticated member clicks the register button for an active event with open registrations
- **AND** provides their SI card number
- **THEN** the registration is created
- **AND** the member stays on the event detail page
- **AND** the registration list refreshes and shows the new registration
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

### Requirement: View Own Registration

The system SHALL allow members to view their own registration details including their SI card number. Users with the EVENTS:REGISTRATIONS authority SHALL be able to view any member's registration details including their SI card number.

#### Scenario: Member views their own registration

- **WHEN** authenticated member views their own registration for an event
- **THEN** the registration details are shown including SI card number and registration time
- **AND** unregister action is shown if unregistration is still allowed
- **AND** edit registration action is shown if registrations are still open

#### Scenario: User with EVENTS:REGISTRATIONS views a specific member's registration

- **WHEN** authenticated user with EVENTS:REGISTRATIONS views a member's registration for an event
- **THEN** the registration details are shown including the member's SI card number and registration time

#### Scenario: User without EVENTS:REGISTRATIONS cannot view another member's registration

- **WHEN** authenticated user without EVENTS:REGISTRATIONS attempts to view another member's registration details
- **THEN** access is refused

#### Scenario: Own registration page shows not found when not registered

- **WHEN** authenticated member navigates to their own registration for an event they are not registered for
- **THEN** the system shows a not-found message

#### Scenario: User with EVENTS:REGISTRATIONS views registration for member who is not registered

- **WHEN** user with EVENTS:REGISTRATIONS navigates to a registration for a member who is not registered for the event
- **THEN** the system shows a not-found message

### Requirement: Edit Own Registration

Members SHALL be able to change their existing event registration (SI card number and category) before the event starts, without having to unregister and register again. Users with the EVENTS:REGISTRATIONS authority SHALL additionally be able to edit any member's registration under the same timing rules. Editing preserves the original registration time so the member does not lose their place in the first-come-first-served order.

#### Scenario: Edit button is visible on own registration view when editing is still allowed

- **GIVEN** a member is registered for an active event with an open registration window
- **WHEN** the member opens "Moje přihláška" for that event
- **THEN** the page shows an "Upravit" button alongside the existing "Odhlásit" button

#### Scenario: Edit button is visible on the member's own row in the registrations list

- **GIVEN** a member without EVENTS:REGISTRATIONS is registered for an active event with an open registration window
- **WHEN** the member views the registrations list on the event detail page
- **THEN** the row representing the member shows an "Upravit" action
- **AND** no other row shows an "Upravit" action

#### Scenario: Edit button is visible on every row for a user with EVENTS:REGISTRATIONS

- **GIVEN** an active event with an open registration window and several registrations
- **WHEN** a user with EVENTS:REGISTRATIONS views the registrations list on the event detail page
- **THEN** every registration row shows an "Upravit" action

#### Scenario: Edit button is hidden after the registration deadline

- **GIVEN** a member is registered for an event whose registration deadline has passed
- **WHEN** the member opens "Moje přihláška" or the registrations list for that event
- **THEN** no "Upravit" button is shown

#### Scenario: Edit button is hidden on or after the event date

- **GIVEN** a member is registered for an event whose event date is today or in the past
- **WHEN** the member opens "Moje přihláška" or the registrations list for that event
- **THEN** no "Upravit" button is shown

#### Scenario: Edit button is hidden for events that are not ACTIVE

- **GIVEN** a member is registered for an event in DRAFT, FINISHED, or CANCELLED state
- **WHEN** the member opens "Moje přihláška" or the registrations list for that event
- **THEN** no "Upravit" button is shown

#### Scenario: Member changes their SI card number

- **GIVEN** a member is registered for an event with an open registration window
- **WHEN** the member clicks "Upravit", enters a different valid SI card number, and submits the form
- **THEN** the registration is updated with the new SI card number
- **AND** the registration time shown on the page is unchanged
- **AND** other members' positions in the registrations list are unchanged

#### Scenario: Member changes their category

- **GIVEN** a member is registered for an event that defines categories, with an open registration window
- **WHEN** the member clicks "Upravit", selects a different category from the event's available categories, and submits the form
- **THEN** the registration is updated with the new category
- **AND** the registration time shown on the page is unchanged

#### Scenario: User with EVENTS:REGISTRATIONS edits another member's registration

- **GIVEN** a user with EVENTS:REGISTRATIONS views the registrations list for an active event with an open registration window
- **WHEN** the user clicks "Upravit" on another member's row, changes the SI card number or category, and submits the form
- **THEN** the registration is updated with the new values
- **AND** the registration time shown on that row is unchanged
- **AND** the edited member sees the updated values when they next view their registration

#### Scenario: Edit form for an event without categories hides the category field

- **WHEN** the member opens the edit form for an event that defines no categories
- **THEN** the form shows only the SI card number field
- **AND** no category selection is offered

#### Scenario: Edit form for an event with categories requires a category

- **GIVEN** the event defines categories
- **WHEN** the member submits the edit form without a selected category
- **THEN** the form shows an error that a category is required

#### Scenario: Submitting a category not offered by the event is rejected

- **WHEN** the member submits the edit form with a category that is not in the event's category list
- **THEN** the form shows an error that the chosen category is not available for the event

#### Scenario: Submitting an invalid SI card number is rejected

- **WHEN** the member submits the edit form with an SI card number containing non-digits, or shorter than 4 digits, or longer than 8 digits, or blank
- **THEN** the form shows the same validation feedback shown on the registration form (digits only, 4–8 characters, required)

#### Scenario: Editing another member's registration without EVENTS:REGISTRATIONS is not possible

- **WHEN** a member without EVENTS:REGISTRATIONS attempts to open or submit the edit form for another member's registration
- **THEN** the edit action is not offered in the UI
- **AND** a direct attempt to submit an edit for another member is refused

#### Scenario: Editing without being registered shows not found

- **WHEN** a member attempts to edit their registration for an event they are not registered for
- **THEN** the system shows a not-found message

#### Scenario: Edit submitted after the deadline is refused

- **GIVEN** the member opened the edit form while the window was open
- **WHEN** the registration deadline passes before the member submits the form
- **AND** the member submits the form
- **THEN** the system shows an error that the registration deadline has passed
- **AND** the registration is not changed

#### Scenario: Guest member edits their own registration

- **GIVEN** a guest member (hostující) is registered for an active event with an open registration window
- **WHEN** the guest member opens "Moje přihláška" and edits their SI card number or category
- **THEN** the same rules and feedback apply as for club members

#### Scenario: Other members see the updated values after an edit

- **GIVEN** a member has edited their SI card number and category
- **WHEN** another user views the registrations list for the event
- **THEN** the edited member's row shows the new category
- **AND** the registration time shown for that row is the original registration time
- **AND** SI card numbers are not shown to other members
