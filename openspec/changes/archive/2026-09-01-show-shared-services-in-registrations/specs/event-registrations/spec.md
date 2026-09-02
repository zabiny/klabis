## MODIFIED Requirements

### Requirement: List Event Registrations

The system SHALL display the list of members registered for an event. Each registration shows the member's first name, last name, and selected category (if applicable). SI card numbers are not shown to other members.

The registration timestamp SHALL be visible only to the event coordinator (the member assigned as the event's coordinator) and to users with the EVENTS:REGISTRATIONS authority. Other members do not see the registration timestamp column.

When the event offers shared transport, each registration additionally shows whether the member asked to use shared transport. When the event offers shared accommodation, each registration additionally shows whether the member asked to use shared accommodation. These choices are displayed as "Ano"/"Ne" and SHALL be visible to every authenticated member who can view the registration list. When the event does not offer a service, the corresponding choice is not displayed for any registration; when the event offers neither service, no shared-service choice is displayed at all.

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
- **WHEN** an authenticated club member without EVENTS:REGISTRATIONS authority who is not the coordinator views the registration list
- **THEN** the member sees for every registration whether that member asked for shared accommodation
