## ADDED Requirements

### Requirement: ORIS Import Tolerates Missing Location

The system SHALL import events from ORIS even when the upstream ORIS event has no location. The imported event is created with an empty location and the import flow does not reject the event on account of the missing field.

#### Scenario: Manager imports an ORIS event without a location

- **WHEN** event manager imports an event from ORIS that has no location
- **THEN** the event is created successfully in DRAFT status with no location
- **AND** the imported event appears in the events list with an empty location cell

#### Scenario: Manager imports an ORIS event with a location

- **WHEN** event manager imports an event from ORIS that has a location
- **THEN** the event is created with the location from ORIS

### Requirement: Row-Level Management Actions in Events Table

The system SHALL expose row-level management actions for events directly in the events list for users with EVENTS:MANAGE permission. The available actions for each row depend on the event status and whether the event was imported from ORIS. The actions are driven by HAL-Forms affordances attached to each row and render only when the current user is authorized.

#### Scenario: Manager sees Upravit and Zrušit actions for a DRAFT event in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **THEN** each DRAFT event row shows "Upravit" and "Zrušit" actions

#### Scenario: Manager sees Upravit and Zrušit actions for an ACTIVE event in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **THEN** each ACTIVE event row shows "Upravit" and "Zrušit" actions

#### Scenario: Manager sees Synchronizovat action for an ORIS-imported DRAFT or ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **AND** an event row is in DRAFT or ACTIVE status and was imported from ORIS
- **THEN** the row additionally shows a "Synchronizovat" action

#### Scenario: Non-ORIS event does not show Synchronizovat action in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **AND** an event row was not imported from ORIS
- **THEN** the row does NOT show a "Synchronizovat" action

#### Scenario: FINISHED or CANCELLED event has no management actions in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **AND** an event row is in FINISHED or CANCELLED status
- **THEN** the row shows no management actions (no edit, cancel, or sync)

#### Scenario: Regular member sees only the register action in the list

- **WHEN** user without EVENTS:MANAGE permission views the events list
- **THEN** event rows show only the register or unregister action (when applicable)
- **AND** no management actions are shown

#### Scenario: Register action in the list is preserved

- **WHEN** user views the events list
- **AND** an event has open registrations
- **THEN** the row shows the register or unregister action as described by the existing events table scenarios

## MODIFIED Requirements

### Requirement: Create Event

The system SHALL allow users with EVENTS:MANAGE permission to create events. Required fields: name, event date, organizer code. Optional: location, website URL, coordinator, registration deadline, categories.

#### Scenario: Manager creates an event with all required fields

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form with name, event date, and organizer code
- **THEN** the event is created in DRAFT status
- **AND** appears in the event list

#### Scenario: Manager creates an event with optional fields

- **WHEN** user with EVENTS:MANAGE permission fills in optional fields (location, website URL, coordinator, registration deadline, categories) and submits
- **THEN** the event is created with all provided data

#### Scenario: Manager creates an event without a location

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form without a location
- **THEN** the event is created successfully with no location

#### Scenario: Create event button not shown without permission

- **WHEN** user without EVENTS:MANAGE permission views the events list
- **THEN** no create event button is shown

#### Scenario: Form shows validation errors for invalid data

- **WHEN** user submits the event form with missing required fields or invalid formats
- **THEN** the form shows inline validation errors for each issue

#### Scenario: Invalid website URL shows error

- **WHEN** user enters a website URL that is not a valid http/https URL
- **THEN** the form shows an error that the URL must be a valid web address

#### Scenario: Non-existent coordinator shows error

- **WHEN** user references a coordinator member that does not exist
- **THEN** the form shows an error that the coordinator was not found

#### Scenario: Registration deadline after event date shows error

- **WHEN** user sets a registration deadline after the event date
- **THEN** the form shows an error that the deadline must be on or before the event date

### Requirement: Events Table Display

The system SHALL display the events list as a table with key columns. The status column is only shown to users with EVENTS:MANAGE permission.

#### Scenario: Regular member views events table

- **WHEN** a club member views the events list page
- **THEN** the table shows columns: date, name, location, organizer, website link, registration deadline, coordinator name, and registration action

#### Scenario: Manager views events table with status column

- **WHEN** user with EVENTS:MANAGE permission views the events list page
- **THEN** the table additionally shows the event status column

#### Scenario: Website link shown as clickable icon

- **WHEN** an event in the table has an external website URL
- **THEN** the website column shows a clickable icon that opens the URL in a new tab

#### Scenario: Website link column empty when no URL set

- **WHEN** an event in the table has no external website URL
- **THEN** the website column is empty for that row

#### Scenario: Location column empty when not set

- **WHEN** an event in the table has no location
- **THEN** the location column is empty for that row

#### Scenario: Registration deadline shown as formatted date

- **WHEN** an event in the table has a registration deadline set
- **THEN** the deadline column displays the date in readable format

#### Scenario: Registration deadline column empty when not set

- **WHEN** an event in the table has no registration deadline
- **THEN** the deadline column is empty for that row

#### Scenario: Coordinator shown as clickable link

- **WHEN** an event in the table has a coordinator assigned
- **THEN** the coordinator column shows the coordinator's full name as a link to their member detail page

#### Scenario: Coordinator column empty when not assigned

- **WHEN** an event in the table has no coordinator assigned
- **THEN** the coordinator column is empty for that row

#### Scenario: Register button shown for open unregistered event

- **WHEN** an event has open registrations
- **AND** the current user is not yet registered
- **THEN** the action column shows a register button

#### Scenario: Unregister button shown for registered event

- **WHEN** an event has open registrations
- **AND** the current user is already registered
- **THEN** the action column shows an unregister button

#### Scenario: Action column empty for closed registrations and no management actions

- **WHEN** an event does not have open registrations
- **AND** the current user has no management actions available for that row
- **THEN** the action column is empty for that row

#### Scenario: Status column hidden when not returned by API

- **WHEN** the API response does not include the status field (field-level security)
- **THEN** the status column is not displayed in the table

### Requirement: Event Detail Page

The application SHALL display the event detail page with location and registration deadline (when set) and categories (when defined), and allow managers to edit them inline.

#### Scenario: Event detail shows location when set

- **WHEN** user views the detail page for an event with a location
- **THEN** the event information section shows the location

#### Scenario: Event detail hides location when not set

- **WHEN** user views the detail page for an event without a location
- **THEN** no location row is shown in the event information section

#### Scenario: Event detail shows registration deadline

- **WHEN** user views the detail page for an event with a registration deadline set
- **THEN** the event information section shows the registration deadline as a formatted date

#### Scenario: Event detail hides registration deadline when not set

- **WHEN** user views the detail page for an event without a registration deadline
- **THEN** no registration deadline row is shown in the event information section

#### Scenario: Inline edit includes registration deadline field

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the registration deadline field is editable as a date picker

#### Scenario: Event create/edit form includes registration deadline

- **WHEN** a manager creates or edits an event via the form
- **THEN** the form includes a registration deadline date picker field

#### Scenario: Event detail shows categories

- **WHEN** user views the detail page for an event with categories defined
- **THEN** the categories are displayed as individual pills/tags

#### Scenario: Event detail hides categories when not set

- **WHEN** user views the detail page for an event without categories
- **THEN** no categories row is shown

#### Scenario: Inline edit includes categories field

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the categories field is editable

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions: DRAFT → ACTIVE → FINISHED or CANCELLED. The transition from ACTIVE to FINISHED is performed exclusively by the automatic completion process; there is no manual "finish" action available to managers.

#### Scenario: Manager publishes a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission publishes an event in DRAFT status
- **THEN** the event becomes ACTIVE
- **AND** members can now register for it

#### Scenario: Manager cancels a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission cancels a DRAFT event
- **THEN** the event becomes CANCELLED

#### Scenario: Manager cancels an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission cancels an ACTIVE event
- **THEN** the event becomes CANCELLED
- **AND** existing registrations are preserved for records

#### Scenario: Invalid status transition shows error

- **WHEN** user attempts an invalid status transition (e.g., FINISHED → ACTIVE)
- **THEN** the system shows an error that the transition is not allowed

### Requirement: Get Event Detail

The system SHALL display complete event detail including categories. DRAFT events are only visible to users with EVENTS:MANAGE permission.

#### Scenario: User views event detail

- **WHEN** authenticated user navigates to an event detail page
- **THEN** all available event information is displayed (name, date, location when set, organizer, website, coordinator, registration deadline, categories)

#### Scenario: Regular user cannot access DRAFT event detail

- **WHEN** user without EVENTS:MANAGE permission navigates to a DRAFT event's detail page
- **THEN** the page shows not found

#### Scenario: Manager views DRAFT event detail

- **WHEN** user with EVENTS:MANAGE permission navigates to a DRAFT event's detail page
- **THEN** the full event detail is displayed

#### Scenario: DRAFT event actions available to manager

- **WHEN** user with EVENTS:MANAGE permission views a DRAFT event
- **THEN** available actions are: edit, publish, cancel, sync from ORIS (if ORIS-imported)

#### Scenario: ACTIVE event actions available to manager

- **WHEN** user with EVENTS:MANAGE permission views an ACTIVE event
- **THEN** available actions are: edit, cancel, sync from ORIS (if ORIS-imported)

#### Scenario: FINISHED or CANCELLED event has no management actions

- **WHEN** user views a FINISHED or CANCELLED event
- **THEN** no edit, publish, cancel, or sync actions are available

#### Scenario: Event detail shows registration deadline

- **WHEN** user views event detail for an event with a registration deadline
- **THEN** the registration deadline is displayed

#### Scenario: Event detail without registration deadline

- **WHEN** user views event detail for an event without a registration deadline
- **THEN** no registration deadline row is shown
