## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Generate Accommodation List for Event Registrations

The event coordinator and users with the EVENTS:REGISTRATIONS authority SHALL be able to generate a print-friendly accommodation list for an event. The list contains, for every member registered for the event, the member's first name, last name, identity card number, identity card validity date, date of birth, and full address.

The list SHALL be available as a separate page reached from the event detail or from the registration list. The page SHALL be designed for printing (legible black-on-white layout, repeating header on each page) so the coordinator can hand the printout to the accommodation provider.

If a registered member has no identity card recorded in their profile, the corresponding cells SHALL display the literal text "neuvedeno" so the coordinator can quickly spot incomplete records before printing.

Members without EVENTS:REGISTRATIONS authority who are not the event coordinator SHALL NOT have access to this list — neither through the user interface (the "Seznam pro ubytování" action is not exposed in the event detail page) nor through the API (request is rejected).

#### Scenario: Event coordinator generates the accommodation list

- **GIVEN** an event has registered members and the user is the coordinator of that event
- **WHEN** the coordinator selects the "Seznam pro ubytování" action on the event detail page
- **THEN** the system displays a printable list of all registered members
- **AND** each row shows first name, last name, identity card number, identity card validity date, date of birth, and address
- **AND** the page exposes a "Tisknout" action that opens the browser print dialog

#### Scenario: User with EVENTS:REGISTRATIONS authority generates the accommodation list for any event

- **GIVEN** the user has the EVENTS:REGISTRATIONS authority
- **WHEN** the user opens the accommodation list for any event
- **THEN** the list is rendered with full details for every registered member

#### Scenario: Accommodation list shows placeholder for members without identity card data

- **GIVEN** a member registered for the event has no identity card number in their profile
- **WHEN** the accommodation list is generated
- **THEN** the identity card number cell and the identity card validity date cell display "neuvedeno"
- **AND** other columns display the available data

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
- **AND** direct attempts to load the accommodation list URL return an authorization error
