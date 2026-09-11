## ADDED Requirements

### Requirement: Member Chooses Shared Transport And Shared Accommodation When Registering

When registering for an event, a member SHALL be able to say whether they want to use the event's shared transport and whether they want to use the event's shared accommodation. Each choice is offered only when the event has the matching offer turned on; when an offer is off, the corresponding field is absent from the registration form. Both choices default to "no".

#### Scenario: Registration form offers both choices when the event has both offers on

- **GIVEN** an ACTIVE event with open registrations that has both the shared transport and shared accommodation offers turned on
- **WHEN** an authenticated member opens the registration form
- **THEN** the form shows a "Chci využít společnou dopravu" tick box and a "Chci využít společné ubytování" tick box
- **AND** both are unticked by default

#### Scenario: Member registers and asks for shared accommodation

- **GIVEN** an ACTIVE event with the shared accommodation offer turned on
- **WHEN** the member ticks "Chci využít společné ubytování" and submits the registration
- **THEN** the registration is created recording that the member wants shared accommodation

#### Scenario: Registration form hides a choice when the event does not offer it

- **GIVEN** an ACTIVE event with the shared transport offer on and the shared accommodation offer off
- **WHEN** an authenticated member opens the registration form
- **THEN** the form shows the "Chci využít společnou dopravu" tick box
- **AND** no shared accommodation tick box is shown

#### Scenario: Registration form shows neither choice when the event offers nothing

- **GIVEN** an ACTIVE event with both offers off
- **WHEN** an authenticated member opens the registration form
- **THEN** the form shows neither a shared transport nor a shared accommodation tick box

#### Scenario: Member registers without ticking either choice

- **GIVEN** an ACTIVE event with both offers turned on
- **WHEN** the member submits the registration without ticking either box
- **THEN** the registration is created recording that the member wants neither shared transport nor shared accommodation

### Requirement: Member Edits Their Shared Transport And Shared Accommodation Choices

A member SHALL be able to change their shared transport and shared accommodation choices for an event they are registered for, under the same conditions that allow editing the registration (before the registration deadline and before the event date, for an ACTIVE event). Each choice is editable only when the event has the matching offer turned on. A user with the EVENTS:REGISTRATIONS authority SHALL be able to change these choices on another member's registration under the same conditions.

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

- **GIVEN** a user with the EVENTS:REGISTRATIONS authority
- **AND** an ACTIVE event with the shared transport offer turned on and open editing
- **WHEN** the user edits another member's registration and changes the shared transport choice
- **THEN** the other member's registration reflects the new choice

## MODIFIED Requirements

### Requirement: Generate Accommodation List for Event Registrations

The event coordinator and users with the EVENTS:REGISTRATIONS authority SHALL be able to generate an accommodation list for an event that has the shared accommodation offer turned on. The list contains, for every member registered for the event who asked to use shared accommodation, the member's first name, last name, identity card number, identity card validity date, date of birth, and full address. Members who are registered but did not ask for shared accommodation are not on the list.

The "Seznam pro ubytování" action SHALL be offered only when the event has the shared accommodation offer turned on. When the offer is off, the action is not exposed in the event detail page and any direct attempt to open the accommodation list, including the CSV download, is rejected.

The list SHALL be available as a separate page reached from the event detail or from the registration list. The page SHALL be designed for printing (legible black-on-white layout, repeating header on each page) so the coordinator can hand the printout to the accommodation provider.

The page SHALL additionally offer a "Stáhnout CSV" action that downloads the same list as a CSV file, so the coordinator can hand the list to the accommodation provider electronically (by e-mail or as input to the provider's system). The downloaded file SHALL contain the same rows and the same columns as the printable list (first name, last name, identity card number, identity card validity date, date of birth, address), with the address rendered as a single combined column. The CSV SHALL open directly in the Czech locale of MS Excel and SHALL begin with a header row labelling the columns in Czech. The downloaded file SHALL be named so the coordinator can recognise which event it belongs to.

If a listed member has no identity card recorded in their profile, the corresponding cells of the printable list SHALL display the literal text "neuvedeno" so the coordinator can quickly spot incomplete records before printing. In the CSV download the corresponding cells SHALL instead be left empty, so the file is easier to process in the accommodation provider's system.

Members without EVENTS:REGISTRATIONS authority who are not the event coordinator SHALL NOT have access to this list — neither through the user interface (the "Seznam pro ubytování" action is not exposed in the event detail page) nor through the API (any request, including the CSV download, is rejected).

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
- **WHEN** the event coordinator or a user with EVENTS:REGISTRATIONS opens the event detail page
- **THEN** the "Seznam pro ubytování" action is not displayed
- **AND** a direct attempt to open the accommodation list or download its CSV is rejected

#### Scenario: User with EVENTS:REGISTRATIONS authority generates the accommodation list for any event

- **GIVEN** the user has the EVENTS:REGISTRATIONS authority
- **AND** the event has the shared accommodation offer turned on
- **WHEN** the user opens the accommodation list for that event
- **THEN** the list is rendered with full details for every member who asked for shared accommodation

#### Scenario: Coordinator downloads the accommodation list as CSV

- **GIVEN** an event with the shared accommodation offer turned on has members who asked for shared accommodation and the user is the coordinator or has the EVENTS:REGISTRATIONS authority
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
- **WHEN** a user with EVENTS:REGISTRATIONS authority opens the event detail page
- **THEN** the "Seznam pro ubytování" action is displayed

#### Scenario: Unauthorized user cannot access the accommodation list

- **GIVEN** an authenticated member without EVENTS:REGISTRATIONS authority and not the event coordinator
- **WHEN** the member opens the event detail page
- **THEN** the "Seznam pro ubytování" action is NOT displayed
- **AND** direct attempts to load the accommodation list, including the CSV download, return an authorization error
