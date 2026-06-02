## MODIFIED Requirements

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
