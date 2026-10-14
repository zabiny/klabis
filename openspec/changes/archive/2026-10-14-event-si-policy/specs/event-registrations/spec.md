## ADDED Requirements

### Requirement: Registration Follows The Event's SI Policy

The registration form and the edit form SHALL follow the event's SI policy. When the event does not use SI cards, no SI card field is shown. When SI is optional, the SI card field is shown but may be left empty. When SI is required, the member MUST enter an SI card number, unless the event allows rental and the member asks to rent one. Where rental is allowed, the form offers a "Chci si půjčit čip" tick box, unticked by default.

#### Scenario: Registration form for an event that does not use SI

- **GIVEN** an ACTIVE event with open registrations that does not use SI cards
- **WHEN** a member opens the registration form
- **THEN** no SI card field and no rental tick box is shown
- **AND** the member can register without an SI card number

#### Scenario: Member registers without an SI card for an event where SI is optional

- **GIVEN** an ACTIVE event where SI cards are optional
- **WHEN** the member leaves the SI card field empty and submits the registration form
- **THEN** the registration is created without an SI card number

#### Scenario: Member rents an SI card instead of entering a number

- **GIVEN** an ACTIVE event that requires SI cards and allows rental
- **WHEN** the member ticks "Chci si půjčit čip", leaves the SI card field empty and submits
- **THEN** the registration is created recording that the member wants to rent an SI card

#### Scenario: Member enters an SI card number and asks for rental

- **GIVEN** an ACTIVE event that allows SI card rental
- **WHEN** the member enters an SI card number and also ticks "Chci si půjčit čip"
- **THEN** the registration is created with the SI card number and the rental request

#### Scenario: Required SI card without rental cannot be skipped

- **GIVEN** an ACTIVE event that requires SI cards and does not allow rental
- **WHEN** the member submits the registration form with an empty SI card field
- **THEN** the form shows an error that the SI card number is required

#### Scenario: Required SI card with rental allowed but not asked for

- **GIVEN** an ACTIVE event that requires SI cards and allows rental
- **WHEN** the member submits the registration form with an empty SI card field and without ticking "Chci si půjčit čip"
- **THEN** the form shows an error that an SI card number is required unless the member rents one

#### Scenario: Rental tick box is absent when rental is not allowed

- **GIVEN** an ACTIVE event that uses SI cards and does not allow rental
- **WHEN** a member opens the registration form or the edit form
- **THEN** no "Chci si půjčit čip" tick box is shown

#### Scenario: Member changes their rental request

- **GIVEN** a member is registered with a rental request for an event that allows rental, with an open registration window
- **WHEN** the member edits the registration, unticks "Chci si půjčit čip", enters their SI card number and saves
- **THEN** the registration records the SI card number and no rental request

#### Scenario: Editing a registration that does not fit the SI policy requires fixing the SI values

- **GIVEN** a member is registered without an SI card number for an event that has since been changed to require SI cards without rental
- **WHEN** the member edits the registration, changes only the category and saves
- **THEN** the form shows an error that the SI card number is required
- **AND** the registration is not changed
- **WHEN** the member enters a valid SI card number and saves
- **THEN** the registration is updated with the new category and the SI card number

#### Scenario: Registration with a rental request after rental was disallowed

- **GIVEN** a member is registered without an SI card number and with a rental request, and the event has since been changed to require SI cards without rental
- **WHEN** the member edits the registration, changes only the category and saves
- **THEN** the form shows an error that the SI card number is required
- **WHEN** the member enters a valid SI card number and saves
- **THEN** the registration is updated with the new category and the SI card number
- **AND** the recorded rental request is kept

#### Scenario: Changing the SI values of a registration must fit the policy

- **GIVEN** a member is registered with an SI card number for an event that requires SI cards without rental
- **WHEN** the member edits the registration, clears the SI card field and saves
- **THEN** the form shows an error that the SI card number is required
- **AND** the registration keeps its SI card number

#### Scenario: Values the event does not offer are kept on edit

- **GIVEN** a member registered with an SI card number and a rental request, and the event has since been changed to not use SI cards
- **WHEN** the member edits the registration and saves
- **THEN** the SI card number and the rental request recorded on the registration are kept

#### Scenario: Registration detail shows the rental request

- **GIVEN** a member's registration records a rental request
- **WHEN** the member, or a user with EVENTS:REGISTRATIONS, opens the registration detail
- **THEN** the detail shows that the member wants to rent an SI card

## MODIFIED Requirements

### Requirement: Register for Event

The system SHALL allow authenticated members to register themselves for ACTIVE events with open registrations (future event date AND registration deadline not passed). Only users with an associated member profile can register. When the event has categories defined, the member MUST select a category. After a successful registration the member remains on the event detail page and the registration list refreshes to include their new registration.

When the registration form opens for an event that uses SI cards, the SI card number field SHALL be prefilled with the member's SI card number from their profile if one is set. The prefilled value MAY be overwritten by the member for this registration without affecting the profile. Whether the SI card number is required follows the event's SI policy.

#### Scenario: Member registers for an open event

- **WHEN** authenticated member clicks the register button for an active event with open registrations
- **AND** provides their SI card number where the event requires one
- **THEN** the registration is created
- **AND** the member stays on the event detail page
- **AND** the registration list refreshes and shows the new registration
- **AND** the event row shows an unregister button

#### Scenario: Registration form prefills SI card number from profile

- **GIVEN** an authenticated member has an SI card number recorded in their profile
- **WHEN** the member opens the registration form for an active event that uses SI cards
- **THEN** the SI card number field is prefilled with the value from the profile
- **AND** the member can submit the form immediately without retyping the SI card number

#### Scenario: Member overwrites the prefilled SI card number

- **GIVEN** the registration form is prefilled with the member's profile SI card number
- **WHEN** the member overwrites the SI card number field with a different value and submits
- **THEN** the registration is created with the overwritten value
- **AND** the member's profile SI card number remains unchanged

#### Scenario: Registration form has empty SI card number when not in profile

- **GIVEN** an authenticated member has no SI card number recorded in their profile
- **WHEN** the member opens the registration form for an active event that requires SI cards
- **THEN** the SI card number field is empty
- **AND** the member must enter the SI card number manually, or ask to rent one where rental is allowed, before submitting

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
- **THEN** no category selection is offered
- **AND** the SI card field is shown according to the event's SI policy

#### Scenario: Edit form for an event with categories requires a category

- **GIVEN** the event defines categories
- **WHEN** the member submits the edit form without a selected category
- **THEN** the form shows an error that a category is required

#### Scenario: Submitting a category not offered by the event is rejected

- **WHEN** the member submits the edit form with a category that is not in the event's category list
- **THEN** the form shows an error that the chosen category is not available for the event

#### Scenario: Submitting an invalid SI card number is rejected

- **WHEN** the member submits the edit form with an SI card number containing non-digits, or shorter than 4 digits, or longer than 8 digits, or blank where the event's SI policy requires one
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

### Requirement: SI Card Number Validation

The system SHALL validate SI card numbers whenever one is entered. A valid SI card number contains only digits and is 4-8 characters long. A blank SI card number is rejected only when the event's SI policy requires one and the member does not ask to rent a card.

#### Scenario: Valid SI card number accepted

- **WHEN** member enters a valid SI card number (4-8 digits)
- **THEN** the form accepts the value

#### Scenario: SI card number with letters rejected

- **WHEN** member enters an SI card number containing non-digit characters
- **THEN** the form shows an error that the SI card number must contain only digits

#### Scenario: SI card number too short rejected

- **WHEN** member enters an SI card number with fewer than 4 digits
- **THEN** the form shows an error that the SI card number must be 4-8 digits

#### Scenario: SI card number too long rejected

- **WHEN** member enters an SI card number with more than 8 digits
- **THEN** the form shows an error that the SI card number must be 4-8 digits

#### Scenario: Blank SI card number rejected

- **WHEN** member submits registration without entering an SI card number for an event that requires one, without asking to rent a card
- **THEN** the form shows an error that the SI card number is required

#### Scenario: Blank SI card number accepted where not required

- **WHEN** member submits registration without entering an SI card number for an event where SI is optional or not used
- **THEN** the registration is created without an SI card number

### Requirement: Registration form driven by the registration representation

The registration form (new registration and registration edit) SHALL be built from the affordances and data carried by the registration representation addressed by the link the member follows — the `newRegistration` link of an event for a new registration, the registration's own link for editing. The form SHALL work identically from every place that offers such a link (dashboard, events list, event detail). When a new-registration representation carries no register affordance, the form SHALL show an error and MUST NOT offer a submit action. When the representation of an existing registration carries no edit affordance — the window for editing has closed, or the user may only read it — the registration SHALL be shown read-only as the registration detail, without inputs or a submit action.

#### Scenario: Member opens a new registration form from any entry point

- **WHEN** a member follows the new-registration link of an event with open registrations — from the dashboard widget, the events list, or the event detail page
- **THEN** the same registration form opens with the member's name context, the event context, and — where the event uses SI cards — an SI card number prefilled from their profile
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
- **THEN** the form opens in edit mode showing the member chip with their name, the category and the current SI card number and rental request as the event's SI policy offers them
- **AND** the form submits as an update of the existing registration

#### Scenario: Registration that can no longer be edited opens as a read-only detail

- **GIVEN** a member is registered for an event whose registration deadline has passed
- **WHEN** the member clicks "Detail" on their row in the registrations list
- **THEN** the registration is shown with its category, SI card number, rental request and notes
- **AND** no input and no submit action is offered

#### Scenario: Event coordinator opens a registration as a read-only detail

- **WHEN** the coordinator of an event clicks "Detail" on a member's row in the registrations list
- **THEN** the member's registration is shown read-only
- **AND** no edit or unregister action is offered

#### Scenario: Registration form for an event without SI does not send the profile SI card number

- **GIVEN** a member has an SI card number in their profile
- **WHEN** the member registers for an event that does not use SI cards
- **THEN** the registration is created without an SI card number
