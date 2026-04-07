# Events Specification

## Purpose

Covers the creation, management, and lifecycle of club events. Defines how event managers create and update events, how events transition through statuses (draft, active, finished, cancelled), how members discover events in the list, and how registration deadlines control when members can sign up.

## Requirements

### Requirement: Registration Deadline

The system SHALL support an optional registration deadline for events. When set, it determines the last date when members can register or unregister.

#### Scenario: Event created with a registration deadline

- **WHEN** event manager creates an event and sets a registration deadline
- **THEN** the event is saved with the specified deadline

#### Scenario: Event created without a registration deadline

- **WHEN** event manager creates an event without setting a registration deadline
- **THEN** registration availability is determined solely by the event status and event date

#### Scenario: Registration deadline must be before or on event date

- **WHEN** event manager sets a registration deadline to a date after the event date
- **THEN** the form shows an error that the deadline must be on or before the event date

#### Scenario: Registrations are closed after deadline passes

- **WHEN** the current date is past the registration deadline
- **THEN** the registration action is no longer available for that event

#### Scenario: Registrations open before deadline

- **WHEN** an event is active with a future event date and a future registration deadline
- **THEN** the registration action is available for members

#### Scenario: No deadline means default open/close behavior

- **WHEN** an event has no registration deadline
- **THEN** registrations are open when the event is active and the event date is in the future

### Requirement: ORIS Import Includes Registration Deadline

The system SHALL import the registration deadline from ORIS (using EntryDate1 as the primary entry deadline).

#### Scenario: Import event with a registration deadline from ORIS

- **WHEN** event manager imports an event from ORIS that has a primary entry deadline
- **THEN** the imported event shows the registration deadline derived from the ORIS entry deadline

#### Scenario: Import event without a registration deadline from ORIS

- **WHEN** event manager imports an event from ORIS that has no primary entry deadline
- **THEN** the imported event has no registration deadline set

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

#### Scenario: Action column empty for closed registrations

- **WHEN** an event does not have open registrations
- **THEN** the action column is empty for that row

#### Scenario: Status column hidden when not returned by API

- **WHEN** the API response does not include the status field (field-level security)
- **THEN** the status column is not displayed in the table

### Requirement: Event Detail Page

The application SHALL display the event detail page with registration deadline (when set) and categories (when defined), and allow managers to edit them inline.

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

### Requirement: Create Event

The system SHALL allow users with EVENTS:MANAGE permission to create events. Required fields: name, event date, location, organizer code. Optional: website URL, coordinator, registration deadline, categories.

#### Scenario: Manager creates an event with all required fields

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form with all required fields
- **THEN** the event is created in DRAFT status
- **AND** appears in the event list

#### Scenario: Manager creates an event with optional fields

- **WHEN** user with EVENTS:MANAGE permission fills in optional fields (website URL, coordinator, registration deadline, categories) and submits
- **THEN** the event is created with all provided data

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

### Requirement: Update Event

The system SHALL allow users with EVENTS:MANAGE permission to update events in DRAFT or ACTIVE status. Editable fields include categories.

#### Scenario: Manager updates a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission edits and saves a DRAFT event
- **THEN** the event is updated with the new values

#### Scenario: Manager updates an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission edits and saves an ACTIVE event
- **THEN** the event is updated with the new values

#### Scenario: Finished event cannot be edited

- **WHEN** user attempts to edit a FINISHED event
- **THEN** the system shows an error that finished events cannot be modified

#### Scenario: Cancelled event cannot be edited

- **WHEN** user attempts to edit a CANCELLED event
- **THEN** the system shows an error that cancelled events cannot be modified

#### Scenario: Update action not shown without permission

- **WHEN** user without EVENTS:MANAGE permission views an event
- **THEN** no edit action is available

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions: DRAFT → ACTIVE → FINISHED or CANCELLED.

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

#### Scenario: Manager manually finishes an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission marks an ACTIVE event as finished
- **THEN** the event becomes FINISHED
- **AND** existing registrations are preserved for records

#### Scenario: Invalid status transition shows error

- **WHEN** user attempts an invalid status transition (e.g., FINISHED → ACTIVE)
- **THEN** the system shows an error that the transition is not allowed

### Requirement: Automatic Event Completion

The system SHALL automatically transition ACTIVE events to FINISHED status after the event date has passed. DRAFT events are not automatically finished.

#### Scenario: Past active event is automatically finished

- **WHEN** the event date has passed for an ACTIVE event
- **THEN** the system automatically changes the event status to FINISHED
- **AND** existing registrations are preserved

#### Scenario: DRAFT event past event date is not auto-finished

- **WHEN** the event date has passed for a DRAFT event
- **THEN** the event remains in DRAFT status

### Requirement: List Events

The system SHALL show a paginated event list. DRAFT events are only visible to users with EVENTS:MANAGE permission. The list can be filtered by status, organizer, date range, and coordinator.

#### Scenario: Regular user does not see DRAFT events

- **WHEN** user without EVENTS:MANAGE permission views the event list
- **THEN** no DRAFT events are shown

#### Scenario: Manager sees all events including DRAFT

- **WHEN** user with EVENTS:MANAGE permission views the event list
- **THEN** events in all statuses including DRAFT are shown

#### Scenario: User can filter events by status

- **WHEN** user applies a status filter (e.g., ACTIVE)
- **THEN** only events with that status are shown

#### Scenario: Regular user filtering by DRAFT sees no results

- **WHEN** user without EVENTS:MANAGE permission filters events by DRAFT status
- **THEN** the list is empty and no DRAFT events are disclosed

#### Scenario: User can filter events by organizer

- **WHEN** user filters the event list by organizer code
- **THEN** only events from that organizer are shown

#### Scenario: User can filter events by date range

- **WHEN** user filters the event list by a date range
- **THEN** only events within that date range are shown

#### Scenario: User can filter events by coordinator

- **WHEN** user filters the event list by a coordinator member
- **THEN** only events where that member is coordinator are shown

#### Scenario: Event status visible only to manager in list

- **WHEN** user without EVENTS:MANAGE permission views the event list
- **THEN** the event status is not shown

### Requirement: Get Event Detail

The system SHALL display complete event detail including categories. DRAFT events are only visible to users with EVENTS:MANAGE permission.

#### Scenario: User views event detail

- **WHEN** authenticated user navigates to an event detail page
- **THEN** all event information is displayed (name, date, location, organizer, website, coordinator, registration deadline, categories)

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
- **THEN** available actions are: edit, cancel, finish, sync from ORIS (if ORIS-imported)

#### Scenario: FINISHED or CANCELLED event has no management actions

- **WHEN** user views a FINISHED or CANCELLED event
- **THEN** no edit, publish, cancel, finish, or sync actions are available

#### Scenario: Event detail shows registration deadline

- **WHEN** user views event detail for an event with a registration deadline
- **THEN** the registration deadline is displayed

#### Scenario: Event detail without registration deadline

- **WHEN** user views event detail for an event without a registration deadline
- **THEN** no registration deadline row is shown
