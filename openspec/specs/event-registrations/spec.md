# Event Registrations Specification

## Purpose

Covers how members register for and unregister from club events. Defines the registration workflow with SI card numbers, viewing registration lists, and the rules for when registrations are open or closed.

## Requirements

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

The system SHALL display the list of members registered for an event. Each registration shows the member's first name, last name, and selected category (if applicable). SI card numbers are not shown to other members.

The registration timestamp SHALL be visible only to the event coordinator (the member assigned as the event's coordinator) and to users with the EVENTS:REGISTRATIONS authority. Other members do not see the registration timestamp column.

The list SHALL be sorted by registration time ascending by default (first-come-first-served order). Users SHALL be able to sort the list by first name, last name, and category by clicking the corresponding column headers. The event coordinator and users with EVENTS:REGISTRATIONS authority SHALL additionally be able to sort by registration time.

#### Scenario: Member views registration list for an event

- **WHEN** an authenticated club member without EVENTS:REGISTRATIONS authority views the registration list for an event they did not coordinate
- **THEN** the list shows each registered member's first name, last name, and category (if the event has categories)
- **AND** SI card numbers are not shown
- **AND** the registration timestamp column is not shown
- **AND** the rows are ordered by registration time ascending (members registered earlier appear higher)

#### Scenario: Event coordinator views registration list

- **GIVEN** an event has been assigned a coordinator
- **WHEN** the event coordinator views the registration list for that event
- **THEN** the list additionally shows the registration timestamp column for each row
- **AND** the registration timestamp column is sortable

#### Scenario: User with EVENTS:REGISTRATIONS authority views registration list

- **WHEN** an authenticated user with EVENTS:REGISTRATIONS authority views the registration list for any event
- **THEN** the list additionally shows the registration timestamp column for each row
- **AND** the registration timestamp column is sortable

#### Scenario: Member sorts the registration list by last name

- **WHEN** an authenticated member clicks the last name column header in the registration list
- **THEN** the list reorders by last name ascending
- **AND** clicking the header again toggles to last name descending

#### Scenario: Member attempts to sort by registration time without authorization

- **GIVEN** an authenticated member without EVENTS:REGISTRATIONS authority is not the event coordinator
- **WHEN** the member views the registration list
- **THEN** the registration time column header is not displayed
- **AND** the member cannot sort by registration time

#### Scenario: Registration list shows category column only when event has categories

- **WHEN** authenticated user views the registration list for an event without categories
- **THEN** no category column is displayed in the registration list

### Requirement: Generate Accommodation List for Event Registrations

The event coordinator and users with the EVENTS:REGISTRATIONS authority SHALL be able to generate an accommodation list for an event. The list contains, for every member registered for the event, the member's first name, last name, identity card number, identity card validity date, date of birth, and full address.

The list SHALL be available as a separate page reached from the event detail or from the registration list. The page SHALL be designed for printing (legible black-on-white layout, repeating header on each page) so the coordinator can hand the printout to the accommodation provider.

The page SHALL additionally offer a "Stáhnout CSV" action that downloads the same list as a CSV file, so the coordinator can hand the list to the accommodation provider electronically (by e-mail or as input to the provider's system). The downloaded file SHALL contain the same rows and the same columns as the printable list (first name, last name, identity card number, identity card validity date, date of birth, address), with the address rendered as a single combined column. The CSV SHALL open directly in the Czech locale of MS Excel and SHALL begin with a header row labelling the columns in Czech. The downloaded file SHALL be named so the coordinator can recognise which event it belongs to.

If a registered member has no identity card recorded in their profile, the corresponding cells of the printable list SHALL display the literal text "neuvedeno" so the coordinator can quickly spot incomplete records before printing. In the CSV download the corresponding cells SHALL instead be left empty, so the file is easier to process in the accommodation provider's system.

Members without EVENTS:REGISTRATIONS authority who are not the event coordinator SHALL NOT have access to this list — neither through the user interface (the "Seznam pro ubytování" action is not exposed in the event detail page) nor through the API (any request, including the CSV download, is rejected).

#### Scenario: Event coordinator generates the accommodation list

- **GIVEN** an event has registered members and the user is the coordinator of that event
- **WHEN** the coordinator selects the "Seznam pro ubytování" action on the event detail page
- **THEN** the system displays a printable list of all registered members
- **AND** each row shows first name, last name, identity card number, identity card validity date, date of birth, and address
- **AND** the page exposes a "Tisknout" action that opens the browser print dialog
- **AND** the page exposes a "Stáhnout CSV" action

#### Scenario: User with EVENTS:REGISTRATIONS authority generates the accommodation list for any event

- **GIVEN** the user has the EVENTS:REGISTRATIONS authority
- **WHEN** the user opens the accommodation list for any event
- **THEN** the list is rendered with full details for every registered member

#### Scenario: Coordinator downloads the accommodation list as CSV

- **GIVEN** an event has registered members and the user is the coordinator or has the EVENTS:REGISTRATIONS authority
- **WHEN** the user selects the "Stáhnout CSV" action on the accommodation list page
- **THEN** the browser downloads a CSV file
- **AND** the file is named after the event so the coordinator can recognise which event it belongs to
- **AND** the file opens in the Czech locale of MS Excel with correctly displayed Czech characters
- **AND** the first row contains Czech column labels
- **AND** each following row contains one registered member with first name, last name, identity card number, identity card validity date, date of birth, and the combined address

#### Scenario: Accommodation list shows placeholder for members without identity card data

- **GIVEN** a member registered for the event has no identity card number in their profile
- **WHEN** the printable accommodation list is generated
- **THEN** the identity card number cell and the identity card validity date cell display "neuvedeno"
- **AND** other columns display the available data

#### Scenario: CSV download leaves missing values empty

- **GIVEN** a member registered for the event has no identity card number in their profile
- **WHEN** the accommodation list is downloaded as CSV
- **THEN** the identity card number cell and the identity card validity date cell are empty
- **AND** other columns contain the available data

#### Scenario: Event coordinator sees the accommodation list action in event detail

- **GIVEN** an event has been assigned a coordinator
- **WHEN** the event coordinator opens the event detail page
- **THEN** the "Seznam pro ubytování" action is displayed

#### Scenario: User with EVENTS:REGISTRATIONS authority sees the accommodation list action in event detail

- **WHEN** a user with EVENTS:REGISTRATIONS authority opens any event detail page
- **THEN** the "Seznam pro ubytování" action is displayed

#### Scenario: Unauthorized user cannot access the accommodation list

- **GIVEN** an authenticated member without EVENTS:REGISTRATIONS authority and not the event coordinator
- **WHEN** the member opens the event detail page
- **THEN** the "Seznam pro ubytování" action is NOT displayed
- **AND** direct attempts to load the accommodation list, including the CSV download, return an authorization error

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

### Requirement: Registrations List Not Accessible for DRAFT Events

The system SHALL NOT provide access to the registrations list for events in DRAFT status. The registrations list link SHALL only be available for events in ACTIVE or FINISHED status.

#### Scenario: Registrations list link is absent for DRAFT event

- **WHEN** a user views the detail page of an event in DRAFT status
- **THEN** no link to the registrations list is present in the page
- **AND** the registrations section is not shown

#### Scenario: Registrations list is accessible for ACTIVE event

- **WHEN** a user navigates to the registrations list of an event in ACTIVE status
- **THEN** the registrations list is displayed normally

### Requirement: SI Card Number Validation

The system SHALL validate SI card numbers. A valid SI card number contains only digits and is 4-8 characters long.

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

- **WHEN** member submits registration without entering an SI card number
- **THEN** the form shows an error that the SI card number is required
### Requirement: Finance Manager Records Transaction From Registrations List

The system SHALL allow a user with FINANCE:MANAGE authority to record a deposit or a charge on the financial account of any member listed in an event's registrations, directly from that registrations list, without navigating away from the event. The action SHALL open the same unified transaction dialog used elsewhere for recording deposits and charges on a member account, and SHALL prefill the transaction note with the event name so the resulting account history entry identifies the source event. After a successful submission, the user SHALL remain on the registrations list and the dialog SHALL close.

Users without FINANCE:MANAGE authority SHALL NOT see the action in any registration row.

#### Scenario: Finance manager sees the transaction action on every registration row

- **GIVEN** an authenticated user holds FINANCE:MANAGE authority
- **WHEN** the user views the registrations list of an event with at least one registration
- **THEN** every registration row offers an action that opens the unified transaction dialog for that registered member's account

#### Scenario: Member without FINANCE:MANAGE does not see the action

- **GIVEN** an authenticated member without FINANCE:MANAGE authority
- **WHEN** the member views the registrations list of any event
- **THEN** no row offers an action to open the unified transaction dialog

#### Scenario: Dialog opens with the registered member's identity and balance

- **GIVEN** a finance manager views the registrations list of an event
- **WHEN** the finance manager triggers the transaction action on a row
- **THEN** the unified transaction dialog opens
- **AND** the dialog header shows that member's first name, last name, registration number, and current account balance
- **AND** the dialog does not appear before the identity and balance are loaded

#### Scenario: Note is prefilled with the event name

- **GIVEN** a finance manager opens the unified transaction dialog from an event's registrations list
- **WHEN** the dialog is shown
- **THEN** the note field is prefilled with text identifying the event (for example, the event name)
- **AND** the finance manager can edit or clear the note before submitting

#### Scenario: Finance manager records a charge for the event entry fee

- **GIVEN** the unified transaction dialog has been opened from an event's registrations list for a member with sufficient balance
- **WHEN** the finance manager selects the charge tab, enters a positive amount and submits
- **THEN** the charge is recorded on that member's account with the event-derived note
- **AND** the dialog closes
- **AND** the user remains on the registrations list of the same event

#### Scenario: Finance manager records a deposit (refund) from the registrations list

- **GIVEN** the unified transaction dialog has been opened from an event's registrations list
- **WHEN** the finance manager selects the deposit tab, enters a positive amount and submits
- **THEN** the deposit is recorded on that member's account with the event-derived note
- **AND** the dialog closes
- **AND** the user remains on the registrations list of the same event

#### Scenario: Direct API attempt without FINANCE:MANAGE is refused

- **GIVEN** an authenticated user without FINANCE:MANAGE authority
- **WHEN** the user attempts a direct API request to record a transaction on a registered member's account via the registrations list resource
- **THEN** the request is refused with an authorization error
- **AND** no transaction is recorded
