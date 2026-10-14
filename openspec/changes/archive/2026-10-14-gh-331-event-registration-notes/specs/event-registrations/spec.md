## ADDED Requirements

### Requirement: Member Adds Notes To Their Registration

When registering for an event and when editing a registration, a member SHALL be able to fill in two optional free-text notes: "Interní poznámka", meant only for the club, and "Poznámka do ORIS", meant for the event organiser's registration in ORIS. Both notes are empty by default. The notes follow the same timing rules as the rest of the registration. A user with EVENTS:REGISTRATIONS SHALL be able to fill in and change both notes on any member's registration.

#### Scenario: Registration form offers both notes

- **WHEN** an authenticated member opens the registration form for an ACTIVE event with open registrations
- **THEN** the form shows an empty "Interní poznámka" field and an empty "Poznámka do ORIS" field
- **AND** the member can submit the form without filling in either note

#### Scenario: Member registers with notes

- **WHEN** the member fills in "Interní poznámka" and "Poznámka do ORIS" on the registration form and submits it
- **THEN** the registration is created with both notes
- **AND** the member sees both notes when they open their registration

#### Scenario: Member changes a note on their registration

- **GIVEN** a member is registered for an event with an open registration window
- **WHEN** the member clicks "Upravit", changes the "Poznámka do ORIS" text and saves
- **THEN** the registration shows the new ORIS note
- **AND** the internal note and the registration time are unchanged

#### Scenario: Member clears a note

- **GIVEN** a member's registration has an internal note
- **WHEN** the member edits the registration, empties the "Interní poznámka" field and saves
- **THEN** the registration has no internal note

#### Scenario: Notes cannot be changed after the registration deadline

- **GIVEN** a member is registered for an event whose registration deadline has passed
- **WHEN** the member opens the detail of their registration from the registrations list
- **THEN** both notes are shown read-only
- **AND** no edit action is available to change them

#### Scenario: User with EVENTS:REGISTRATIONS edits another member's notes

- **GIVEN** a user with EVENTS:REGISTRATIONS and an ACTIVE event with an open registration window
- **WHEN** the user opens another member's registration, clicks "Upravit", changes both notes and saves
- **THEN** the other member's registration shows the new notes
- **AND** the member sees the new notes when they next open their registration

### Requirement: Registration Notes Are Shown Only On The Registration Detail

Both notes SHALL be shown only on the registration detail and in the registration edit form, never in the registrations list. The notes SHALL be visible to the registered member, to users with EVENTS:REGISTRATIONS and to the event's coordinator. The coordinator sees them read-only. No other member SHALL see another member's notes.

#### Scenario: Registrations list does not show notes

- **GIVEN** registrations of an event carry notes
- **WHEN** any user, including the event coordinator and a user with EVENTS:REGISTRATIONS, views the registrations list of the event
- **THEN** no note is shown in the list

#### Scenario: Event coordinator reads the notes of a registration

- **GIVEN** a member's registration for an event carries both notes
- **WHEN** the coordinator of that event opens the member's registration from the registrations list
- **THEN** the registration detail shows the internal note and the ORIS note
- **AND** no action to edit the notes or the registration is offered to the coordinator

#### Scenario: Other members do not see notes

- **WHEN** a member without EVENTS:REGISTRATIONS who is not the event coordinator views the registrations of an event
- **THEN** they cannot open another member's registration detail
- **AND** they see no note of any other member

### Requirement: Registration Note Validation

Each note SHALL be plain text of at most 255 characters. A note containing only whitespace SHALL be treated as no note.

#### Scenario: Note input is limited to 255 characters

- **WHEN** a member types or pastes a note into the registration form or the edit form
- **THEN** the form accepts at most 255 characters in each note and shows how many characters are used out of 255

#### Scenario: Note that is too long is rejected

- **WHEN** a registration or registration change with a note longer than 255 characters is submitted
- **THEN** the registration is not created or changed

#### Scenario: Note with only whitespace is stored as no note

- **WHEN** a member submits the registration form with "Interní poznámka" containing only spaces
- **THEN** the registration is created without an internal note

## MODIFIED Requirements

### Requirement: View Own Registration

The system SHALL allow members to view their own registration details including their SI card number. Users with the EVENTS:REGISTRATIONS authority SHALL be able to view any member's registration details including their SI card number. The coordinator of an event SHALL be able to open the registration details of any registration for that event in read-only form, including the member's SI card number (needed to register the member at the event presentation); the coordinator is not offered any action to change the registration.

#### Scenario: Member views their own registration

- **WHEN** authenticated member views their own registration for an event
- **THEN** the registration details are shown including SI card number and registration time
- **AND** unregister action is shown if unregistration is still allowed
- **AND** edit registration action is shown if registrations are still open

#### Scenario: User with EVENTS:REGISTRATIONS views a specific member's registration

- **WHEN** authenticated user with EVENTS:REGISTRATIONS views a member's registration for an event
- **THEN** the registration details are shown including the member's SI card number and registration time

#### Scenario: Event coordinator views a member's registration

- **GIVEN** a member is registered for an event coordinated by another member
- **WHEN** the coordinator opens that member's registration from the event's registrations list
- **THEN** the registration details are shown including the member's SI card number, category and registration time
- **AND** no edit or unregister action is offered

#### Scenario: User without EVENTS:REGISTRATIONS cannot view another member's registration

- **WHEN** authenticated user without EVENTS:REGISTRATIONS who is not the event coordinator attempts to view another member's registration details
- **THEN** access is refused

#### Scenario: Own registration page shows not found when not registered

- **WHEN** authenticated member navigates to their own registration for an event they are not registered for
- **THEN** the system shows a not-found message

#### Scenario: User with EVENTS:REGISTRATIONS views registration for member who is not registered

- **WHEN** user with EVENTS:REGISTRATIONS navigates to a registration for a member who is not registered for the event
- **THEN** the system shows a not-found message

### Requirement: Registration form driven by the registration representation

The registration form (new registration and registration edit) SHALL be built from the affordances and data carried by the registration representation addressed by the link the member follows — the `newRegistration` link of an event for a new registration, the registration's own link for editing. The form SHALL work identically from every place that offers such a link (dashboard, events list, event detail). When a new-registration representation carries no register affordance, the form SHALL show an error and MUST NOT offer a submit action. When the representation of an existing registration carries no edit affordance — the window for editing has closed, or the user may only read it — the registration SHALL be shown read-only as the registration detail, without inputs or a submit action.

#### Scenario: Member opens a new registration form from any entry point

- **WHEN** a member follows the new-registration link of an event with open registrations — from the dashboard widget, the events list, or the event detail page
- **THEN** the same registration form opens with the member's name context, the event context, and an SI card number prefilled from their profile
- **AND** the form submits as a new registration

#### Scenario: New registration representation does not offer editing

- **WHEN** a member opens the new registration form via the event's new-registration link
- **THEN** the form is driven by the register affordance of the returned registration defaults
- **AND** no edit or unregister affordance is offered by that response

#### Scenario: Registration form without an available register action shows an error

- **WHEN** a member opens the registration form for an event whose registrations are closed
- **THEN** the form shows an error instead of the submit action

#### Scenario: Blocked member does not receive the register action

- **GIVEN** a member is blocked from registering by a sanction
- **WHEN** the member opens the new registration form via the event's new-registration link
- **THEN** the form shows an error instead of the submit action

#### Scenario: Member edits their registration from the registration link

- **WHEN** a member follows the link of their existing registration to edit it
- **THEN** the form opens in edit mode showing the member chip with their name, the current SI card number and category
- **AND** the form submits as an update of the existing registration

#### Scenario: Registration that can no longer be edited opens as a read-only detail

- **GIVEN** a member is registered for an event whose registration deadline has passed
- **WHEN** the member clicks "Detail" on their row in the registrations list
- **THEN** the registration is shown with its category, SI card number and notes
- **AND** no input and no submit action is offered

#### Scenario: Event coordinator opens a registration as a read-only detail

- **WHEN** the coordinator of an event clicks "Detail" on a member's row in the registrations list
- **THEN** the member's registration is shown read-only
- **AND** no edit or unregister action is offered

