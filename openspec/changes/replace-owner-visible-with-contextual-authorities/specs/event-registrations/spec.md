# Spec Delta

## MODIFIED Requirements

### Requirement: Unregister from Event

The system SHALL allow members to cancel their event registration before the event date and before the registration deadline (if set). A legal guardian SHALL additionally be able to cancel the registration of their minor, and users with the EVENTS:REGISTER_MEMBER authority the registration of any member, under the same timing rules. A single unregister operation serves both cases: it names the member whose registration is cancelled, and the unregister action is offered wherever the user is allowed to use it. The unregister action is reached from the registration edit dialog, where it asks for confirmation; the event detail page does not offer a separate unregister button.

#### Scenario: Member unregisters before event date

- **WHEN** authenticated member chooses "Odhlásit" in the registration edit dialog for an event before the event date and confirms
- **AND** the registration deadline has not passed
- **THEN** the registration is cancelled
- **AND** the event row shows a register button again

#### Scenario: User with EVENTS:REGISTRATIONS unregisters another member

- **GIVEN** a member is registered for an event that has not started and whose registration deadline has not passed
- **WHEN** a user with EVENTS:REGISTER_MEMBER opens that member's registration and chooses "Odhlásit" and confirms
- **THEN** the member's registration is cancelled
- **AND** the registrations list no longer shows the member

#### Scenario: Unregister action is offered to a user with EVENTS:REGISTRATIONS on another member's registration

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** a user with EVENTS:REGISTER_MEMBER opens that member's registration
- **THEN** the registration shows the unregister action

#### Scenario: Member cannot unregister another member

- **GIVEN** a member without EVENTS:REGISTER_MEMBER
- **WHEN** they attempt to unregister a different member who is not their minor from an event
- **THEN** the system refuses the request as forbidden
- **AND** the other member's registration remains

#### Scenario: Event coordinator without EVENTS:REGISTRATIONS cannot unregister members

- **GIVEN** an event coordinator who does not hold EVENTS:REGISTER_MEMBER
- **WHEN** they open a member's registration for their event
- **THEN** no unregister action is offered

#### Scenario: Unregistration on or after event date not allowed

- **WHEN** member attempts to unregister on or after the event date
- **THEN** the system shows an error that unregistration is only allowed before the event date

#### Scenario: Unregistration after deadline not allowed

- **WHEN** member attempts to unregister from an event whose registration deadline has passed
- **THEN** the system shows an error that the registration deadline has passed

#### Scenario: Unregistration when not registered shows error

- **WHEN** member attempts to unregister from an event they are not registered for
- **THEN** the system shows an error that no registration was found

### Requirement: View Own Registration

The system SHALL allow members to view their own registration details including their SI card number. A legal guardian SHALL be able to view the registration details of their minor, and users with the EVENTS:REGISTER_MEMBER authority those of any member, including the SI card number. The coordinator of an event SHALL be able to open the registration details of any registration for that event in read-only form, including the member's SI card number (needed to register the member at the event presentation); the coordinator is not offered any action to change the registration.

#### Scenario: Member views their own registration

- **WHEN** authenticated member views their own registration for an event
- **THEN** the registration details are shown including SI card number and registration time
- **AND** unregister action is shown if unregistration is still allowed
- **AND** edit registration action is shown if registrations are still open

#### Scenario: User with EVENTS:REGISTRATIONS views a specific member's registration

- **WHEN** authenticated user with EVENTS:REGISTER_MEMBER views a member's registration for an event
- **THEN** the registration details are shown including the member's SI card number and registration time

#### Scenario: Event coordinator views a member's registration

- **GIVEN** a member is registered for an event coordinated by another member
- **WHEN** the coordinator opens that member's registration from the event's registrations list
- **THEN** the registration details are shown including the member's SI card number, category and registration time
- **AND** no edit or unregister action is offered

#### Scenario: User without EVENTS:REGISTRATIONS cannot view another member's registration

- **WHEN** authenticated user without EVENTS:REGISTER_MEMBER who is neither the event coordinator nor the member's legal guardian attempts to view another member's registration details
- **THEN** access is refused

#### Scenario: Own registration page shows not found when not registered

- **WHEN** authenticated member navigates to their own registration for an event they are not registered for
- **THEN** the system shows a not-found message

#### Scenario: User with EVENTS:REGISTRATIONS views registration for member who is not registered

- **WHEN** user with EVENTS:REGISTER_MEMBER navigates to a registration for a member who is not registered for the event
- **THEN** the system shows a not-found message

### Requirement: Edit Own Registration

Members SHALL be able to change their existing event registration (SI card number and category) before the event starts, without having to unregister and register again. A legal guardian SHALL additionally be able to edit the registration of their minor, and users with the EVENTS:REGISTER_MEMBER authority the registration of any member, under the same timing rules. Editing preserves the original registration time so the member does not lose their place in the first-come-first-served order.

#### Scenario: Edit button is visible on own registration view when editing is still allowed

- **GIVEN** a member is registered for an active event with an open registration window
- **WHEN** the member opens "Moje přihláška" for that event
- **THEN** the page shows an "Upravit" button alongside the existing "Odhlásit" button

#### Scenario: Edit button is visible on the member's own row in the registrations list

- **GIVEN** a member without EVENTS:REGISTER_MEMBER who is not a legal guardian is registered for an active event with an open registration window
- **WHEN** the member views the registrations list on the event detail page
- **THEN** the row representing the member shows an "Upravit" action
- **AND** no other row shows an "Upravit" action

#### Scenario: Edit button is visible on every row for a user with EVENTS:REGISTRATIONS

- **GIVEN** an active event with an open registration window and several registrations
- **WHEN** a user with EVENTS:REGISTER_MEMBER views the registrations list on the event detail page
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

- **GIVEN** a user with EVENTS:REGISTER_MEMBER views the registrations list for an active event with an open registration window
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

- **WHEN** a member without EVENTS:REGISTER_MEMBER attempts to open or submit the edit form for the registration of another member who is not their minor
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

### Requirement: List Event Registrations

The system SHALL display the list of members registered for an event. Each registration shows the member's first name, last name, and selected category (if applicable). SI card numbers are not shown to other members.

The registration timestamp SHALL be visible only to the event coordinator (the member assigned as the event's coordinator) and to users with the EVENTS:REGISTER_MEMBER authority. Other members do not see the registration timestamp column.

When the event offers shared transport, each registration additionally shows whether the member asked to use shared transport. When the event offers shared accommodation, each registration additionally shows whether the member asked to use shared accommodation. These choices are displayed as "Ano"/"Ne" and SHALL be visible to every authenticated member who can view the registration list. When the event does not offer a service, the corresponding choice is not displayed for any registration; when the event offers neither service, no shared-service choice is displayed at all.

The list SHALL be sorted by registration time ascending by default (first-come-first-served order). Users SHALL be able to sort the list by first name, last name, and category by clicking the corresponding column headers. The event coordinator and users with EVENTS:REGISTER_MEMBER authority SHALL additionally be able to sort by registration time.

#### Scenario: Member views registration list for an event

- **WHEN** an authenticated club member without EVENTS:REGISTER_MEMBER authority views the registration list for an event they did not coordinate
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

- **WHEN** an authenticated user with EVENTS:REGISTER_MEMBER authority views the registration list for any event
- **THEN** the list additionally shows the registration timestamp column for each row
- **AND** the registration timestamp column is sortable

#### Scenario: Member sorts the registration list by last name

- **WHEN** an authenticated member clicks the last name column header in the registration list
- **THEN** the list reorders by last name ascending
- **AND** clicking the header again toggles to last name descending

#### Scenario: Member attempts to sort by registration time without authorization

- **GIVEN** an authenticated member without EVENTS:REGISTER_MEMBER authority is not the event coordinator
- **WHEN** the member views the registration list
- **THEN** the registration time column header is not displayed
- **AND** the member cannot sort by registration time

#### Scenario: Registration list shows category column only when event has categories

- **WHEN** authenticated user views the registration list for an event without categories
- **THEN** no category column is displayed in the registration list

#### Scenario: Member sees shared service choices for an event offering both services

- **GIVEN** an event with both the shared transport offer and the shared accommodation offer turned on, where some registered members asked for the services and others did not
- **WHEN** an authenticated club member views the registration list for the event
- **THEN** every registration row shows "Ano" or "Ne" for shared transport according to that member's choice
- **AND** every registration row shows "Ano" or "Ne" for shared accommodation according to that member's choice

#### Scenario: Registration list shows shared transport choice only when the event offers shared transport

- **GIVEN** an event with the shared transport offer turned on and the shared accommodation offer turned off
- **WHEN** an authenticated club member views the registration list for the event
- **THEN** each registration row shows the member's shared transport choice
- **AND** no shared accommodation choice is displayed for any registration

#### Scenario: Registration list shows no shared service choices when the event offers none

- **GIVEN** an event with both the shared transport offer and the shared accommodation offer turned off
- **WHEN** an authenticated club member views the registration list for the event
- **THEN** no shared transport or shared accommodation choice is displayed for any registration

#### Scenario: Shared service choices are visible regardless of authority

- **GIVEN** an event with the shared accommodation offer turned on where some registered members asked for shared accommodation
- **WHEN** an authenticated club member without EVENTS:REGISTER_MEMBER authority who is not the coordinator views the registration list
- **THEN** the member sees for every registration whether that member asked for shared accommodation

### Requirement: Generate Accommodation List for Event Registrations

The event coordinator and users with the EVENTS:REGISTER_MEMBER authority SHALL be able to generate an accommodation list for an event that has the shared accommodation offer turned on. The list contains, for every member registered for the event who asked to use shared accommodation, the member's first name, last name, identity card number, identity card validity date, date of birth, and full address. Members who are registered but did not ask for shared accommodation are not on the list.

The "Seznam pro ubytování" action SHALL be offered only when the event has the shared accommodation offer turned on. When the offer is off, the action is not exposed in the event detail page and any direct attempt to open the accommodation list, including the CSV download, is rejected.

The list SHALL be available as a separate page reached from the event detail or from the registration list. The page SHALL be designed for printing (legible black-on-white layout, repeating header on each page) so the coordinator can hand the printout to the accommodation provider.

The page SHALL additionally offer a "Stáhnout CSV" action that downloads the same list as a CSV file, so the coordinator can hand the list to the accommodation provider electronically (by e-mail or as input to the provider's system). The downloaded file SHALL contain the same rows and the same columns as the printable list (first name, last name, identity card number, identity card validity date, date of birth, address), with the address rendered as a single combined column. The CSV SHALL open directly in the Czech locale of MS Excel and SHALL begin with a header row labelling the columns in Czech. The downloaded file SHALL be named so the coordinator can recognise which event it belongs to.

If a listed member has no identity card recorded in their profile, the corresponding cells of the printable list SHALL display the literal text "neuvedeno" so the coordinator can quickly spot incomplete records before printing. In the CSV download the corresponding cells SHALL instead be left empty, so the file is easier to process in the accommodation provider's system.

Members without EVENTS:REGISTER_MEMBER authority who are not the event coordinator SHALL NOT have access to this list — neither through the user interface (the "Seznam pro ubytování" action is not exposed in the event detail page) nor through the API (any request, including the CSV download, is rejected).

#### Scenario: Event coordinator generates the accommodation list

- **GIVEN** an event has the shared accommodation offer turned on and members who asked for shared accommodation, and the user is the coordinator of that event
- **WHEN** the coordinator selects the "Seznam pro ubytování" action on the event detail page
- **THEN** the system displays a printable list of the members who asked for shared accommodation
- **AND** each row shows first name, last name, identity card number, identity card validity date, date of birth, and address
- **AND** the page exposes a "Tisknout" action that opens the browser print dialog
- **AND** the page exposes a "Stáhnout CSV" action

#### Scenario: Accommodation list excludes members who did not choose shared accommodation

- **GIVEN** an event with the shared accommodation offer turned on where some registered members asked for shared accommodation and others did not
- **WHEN** the coordinator opens the accommodation list
- **THEN** only the members who asked for shared accommodation appear on the list

#### Scenario: Accommodation list action is hidden when the event does not offer shared accommodation

- **GIVEN** an event with the shared accommodation offer turned off
- **WHEN** the event coordinator or a user with EVENTS:REGISTER_MEMBER opens the event detail page
- **THEN** the "Seznam pro ubytování" action is not displayed
- **AND** a direct attempt to open the accommodation list or download its CSV is rejected

#### Scenario: User with EVENTS:REGISTRATIONS authority generates the accommodation list for any event

- **GIVEN** the user has the EVENTS:REGISTER_MEMBER authority
- **AND** the event has the shared accommodation offer turned on
- **WHEN** the user opens the accommodation list for that event
- **THEN** the list is rendered with full details for every member who asked for shared accommodation

#### Scenario: Coordinator downloads the accommodation list as CSV

- **GIVEN** an event with the shared accommodation offer turned on has members who asked for shared accommodation and the user is the coordinator or has the EVENTS:REGISTER_MEMBER authority
- **WHEN** the user selects the "Stáhnout CSV" action on the accommodation list page
- **THEN** the browser downloads a CSV file
- **AND** the file is named after the event so the coordinator can recognise which event it belongs to
- **AND** the file opens in the Czech locale of MS Excel with correctly displayed Czech characters
- **AND** the first row contains Czech column labels
- **AND** each following row contains one member who asked for shared accommodation with first name, last name, identity card number, identity card validity date, date of birth, and the combined address

#### Scenario: Accommodation list shows placeholder for members without identity card data

- **GIVEN** a member on the accommodation list has no identity card number in their profile
- **WHEN** the printable accommodation list is generated
- **THEN** the identity card number cell and the identity card validity date cell display "neuvedeno"
- **AND** other columns display the available data

#### Scenario: CSV download leaves missing values empty

- **GIVEN** a member on the accommodation list has no identity card number in their profile
- **WHEN** the accommodation list is downloaded as CSV
- **THEN** the identity card number cell and the identity card validity date cell are empty
- **AND** other columns contain the available data

#### Scenario: Event coordinator sees the accommodation list action in event detail

- **GIVEN** an event has been assigned a coordinator and has the shared accommodation offer turned on
- **WHEN** the event coordinator opens the event detail page
- **THEN** the "Seznam pro ubytování" action is displayed

#### Scenario: User with EVENTS:REGISTRATIONS authority sees the accommodation list action in event detail

- **GIVEN** an event has the shared accommodation offer turned on
- **WHEN** a user with EVENTS:REGISTER_MEMBER authority opens the event detail page
- **THEN** the "Seznam pro ubytování" action is displayed

#### Scenario: Unauthorized user cannot access the accommodation list

- **GIVEN** an authenticated member without EVENTS:REGISTER_MEMBER authority and not the event coordinator
- **WHEN** the member opens the event detail page
- **THEN** the "Seznam pro ubytování" action is NOT displayed
- **AND** direct attempts to load the accommodation list, including the CSV download, return an authorization error

### Requirement: Member Edits Their Shared Transport And Shared Accommodation Choices

A member SHALL be able to change their shared transport and shared accommodation choices for an event they are registered for, under the same conditions that allow editing the registration (before the registration deadline and before the event date, for an ACTIVE event). Each choice is editable only when the event has the matching offer turned on. A user with the EVENTS:REGISTER_MEMBER authority SHALL be able to change these choices on another member's registration under the same conditions.

#### Scenario: Member turns on shared accommodation after registering

- **GIVEN** a member is registered for an ACTIVE event that has the shared accommodation offer turned on
- **AND** the registration deadline and the event date are still in the future
- **WHEN** the member edits their registration, ticks "Chci využít společné ubytování" and saves
- **THEN** the registration now records that the member wants shared accommodation
- **AND** the event's shared accommodation count on the detail page goes up by one

#### Scenario: Member removes their shared transport choice

- **GIVEN** a member is registered for an ACTIVE event with the shared transport offer on and has previously asked for shared transport
- **WHEN** the member edits their registration, unticks "Chci využít společnou dopravu" and saves
- **THEN** the registration no longer records a shared transport request

#### Scenario: Edit form hides a choice the event does not offer

- **GIVEN** a member is registered for an ACTIVE event with the shared accommodation offer off
- **WHEN** the member opens the edit form for their registration
- **THEN** no shared accommodation tick box is shown

#### Scenario: Choices cannot be edited after the registration deadline

- **GIVEN** a member is registered for an event whose registration deadline has passed
- **WHEN** the member views their registration
- **THEN** no edit action is available and the shared transport and shared accommodation choices cannot be changed

#### Scenario: User with EVENTS:REGISTRATIONS edits another member's choices

- **GIVEN** a user with the EVENTS:REGISTER_MEMBER authority
- **AND** an ACTIVE event with the shared transport offer turned on and open editing
- **WHEN** the user edits another member's registration and changes the shared transport choice
- **THEN** the other member's registration reflects the new choice

### Requirement: Member Adds Notes To Their Registration

When registering for an event and when editing a registration, a member SHALL be able to fill in two optional free-text notes: "Interní poznámka", meant only for the club, and "Poznámka do ORIS", meant for the event organiser's registration in ORIS. Both notes are empty by default. The notes follow the same timing rules as the rest of the registration. A user with EVENTS:REGISTER_MEMBER SHALL be able to fill in and change both notes on any member's registration.

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

- **GIVEN** a user with EVENTS:REGISTER_MEMBER and an ACTIVE event with an open registration window
- **WHEN** the user opens another member's registration, clicks "Upravit", changes both notes and saves
- **THEN** the other member's registration shows the new notes
- **AND** the member sees the new notes when they next open their registration

### Requirement: Registration Notes Are Shown Only On The Registration Detail

Both notes SHALL be shown only on the registration detail and in the registration edit form, never in the registrations list. The notes SHALL be visible to the registered member, to users with EVENTS:REGISTER_MEMBER and to the event's coordinator. The coordinator sees them read-only. No other member SHALL see another member's notes.

#### Scenario: Registrations list does not show notes

- **GIVEN** registrations of an event carry notes
- **WHEN** any user, including the event coordinator and a user with EVENTS:REGISTER_MEMBER, views the registrations list of the event
- **THEN** no note is shown in the list

#### Scenario: Event coordinator reads the notes of a registration

- **GIVEN** a member's registration for an event carries both notes
- **WHEN** the coordinator of that event opens the member's registration from the registrations list
- **THEN** the registration detail shows the internal note and the ORIS note
- **AND** no action to edit the notes or the registration is offered to the coordinator

#### Scenario: Other members do not see notes

- **WHEN** a member without EVENTS:REGISTER_MEMBER who is not the event coordinator views the registrations of an event
- **THEN** they cannot open another member's registration detail
- **AND** they see no note of any other member

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
- **WHEN** the member, or a user with EVENTS:REGISTER_MEMBER, opens the registration detail
- **THEN** the detail shows that the member wants to rent an SI card

### Requirement: A Registration Removed From ORIS Is Kept And Marked

When the ORIS entry matched with a registration disappears from ORIS, the system SHALL NOT delete the registration in Klabis. It SHALL mark the registration as "V ORISu nenalezena". The mark SHALL be shown on the registration detail and, to the event coordinator and users with EVENTS:REGISTER_MEMBER, in the registrations list. When the member is entered into the event in ORIS again, the mark SHALL disappear and the registration SHALL take the ORIS values.

#### Scenario: Entry deleted in ORIS stays in Klabis with a mark

- **GIVEN** a registration matched with an ORIS entry
- **WHEN** the registrar deletes the entry in ORIS
- **THEN** after the next synchronisation run the registration is still listed in Klabis
- **AND** the coordinator and users with EVENTS:REGISTER_MEMBER see it marked "V ORISu nenalezena"
- **AND** the member sees the mark on the detail of their registration

#### Scenario: Other members do not see the mark in the list

- **WHEN** a member without EVENTS:REGISTER_MEMBER who is not the coordinator views the registrations list
- **THEN** no "V ORISu nenalezena" mark is shown on other members' rows

#### Scenario: Entry re-created in ORIS removes the mark

- **GIVEN** a registration marked "V ORISu nenalezena"
- **WHEN** the registrar enters the member into the event in ORIS again
- **THEN** after the next synchronisation run the mark is gone
- **AND** the registration shows the values entered in ORIS

### Requirement: Registration Edit Dialog Offers Unregistering

When a user edits a registration that they are allowed to cancel — their own, or any member's for a user with EVENTS:REGISTER_MEMBER — the edit dialog SHALL offer an "Odhlásit" action. Choosing it SHALL lead to the same confirmation of unregistering as elsewhere in the application; the registration is cancelled only after confirmation. The action is not offered when unregistering is not allowed.

#### Scenario: Member unregisters from the edit dialog

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** the member opens the edit dialog of their registration and chooses "Odhlásit"
- **THEN** the edit dialog closes and the member is asked to confirm unregistering
- **AND** after confirming, the registration is cancelled and the list they came from shows the event as not registered

#### Scenario: Registration manager unregisters another member from the edit dialog

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** a user with EVENTS:REGISTER_MEMBER opens the edit dialog of that member's registration from the registrations list and chooses "Odhlásit"
- **THEN** they are asked to confirm unregistering
- **AND** after confirming, the registration is cancelled and the registrations list no longer shows the member

#### Scenario: Member declines to unregister

- **WHEN** the member chooses "Odhlásit" in the edit dialog and then dismisses the confirmation
- **THEN** the registration remains unchanged

#### Scenario: Unregister action is hidden when unregistering is not allowed

- **GIVEN** the registration deadline of the event has passed
- **WHEN** the member opens the edit dialog of their registration
- **THEN** no "Odhlásit" action is shown
