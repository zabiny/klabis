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

### Requirement: Events Table Display

The system SHALL display the events list as a table with key columns and a filter bar. The status column is only shown to users with EVENTS:MANAGE permission. The filter bar exposes fulltext search, a time window selector (Budoucí / Proběhlé / Vše), and — for users with a member profile — a "Moje přihlášky" toggle. The default sort order depends on the active time window: upcoming events are sorted by event date ascending (nearest first), past and all events are sorted by event date descending (most recent first).

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

#### Scenario: Filter bar is visible on the events list

- **WHEN** a user opens the events list page
- **THEN** a filter bar is visible above the table with a fulltext search field, a time window selector, and — if the user has a member profile — a "Moje přihlášky" toggle

#### Scenario: User without a member profile does not see the "Moje přihlášky" toggle

- **WHEN** a user who has no member profile opens the events list page
- **THEN** the filter bar does NOT show the "Moje přihlášky" toggle

#### Scenario: Time window selector defaults to "Budoucí"

- **WHEN** a user opens the events list page without any explicit filter
- **THEN** the time window selector shows "Budoucí" as the active option

#### Scenario: Filter state is preserved in the page URL

- **WHEN** a user sets filters on the events list
- **AND** the user reloads the page or shares the URL
- **THEN** the same filters are active after reload or when the shared URL is opened

#### Scenario: Default sort is ascending by date in the upcoming view

- **WHEN** a user views the events list with time window "Budoucí"
- **AND** no manual sort has been applied
- **THEN** events are ordered by event date ascending (nearest date first)

#### Scenario: Default sort is descending by date in past and all views

- **WHEN** a user views the events list with time window "Proběhlé" or "Vše"
- **AND** no manual sort has been applied
- **THEN** events are ordered by event date descending (most recent date first)

#### Scenario: User can override the default sort by clicking a column header

- **WHEN** a user clicks a column header on the events list
- **THEN** the list is sorted by that column and the widget-default sort no longer applies

### Requirement: Event Detail Page

The application SHALL display the event detail page with location and registration deadline (when set) and categories (when defined), and allow managers to edit them inline. The registrations section and the link to the registrations list SHALL only be shown for events that are not in DRAFT status.

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

#### Scenario: Event detail hides registrations section for DRAFT event

- **WHEN** user views the detail page for an event in DRAFT status
- **THEN** the registrations section is NOT shown
- **AND** no link to the registrations list is displayed

#### Scenario: Event detail shows registrations section for ACTIVE event

- **WHEN** user views the detail page for an event in ACTIVE status
- **THEN** the registrations section is shown with the link to the registrations list

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

The system SHALL show a paginated event list. DRAFT events are only visible to users with EVENTS:MANAGE permission. By default the list shows only upcoming events (events whose date is today or later). The list can be filtered by status, organizer, date range, coordinator, fulltext (event name and location), and registered-by-me. Multiple filters combine with AND semantics.

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

#### Scenario: Default view shows only upcoming events

- **WHEN** a user opens the events list without any explicit time filter
- **THEN** only events whose event date is today or later are shown

#### Scenario: Today's events count as upcoming

- **WHEN** the events list is shown with the default upcoming view
- **AND** an event's event date is today
- **THEN** that event appears in the list

#### Scenario: User can switch to past events

- **WHEN** a user switches the time window to "Proběhlé"
- **THEN** only events whose event date is before today are shown

#### Scenario: User can show all events regardless of date

- **WHEN** a user switches the time window to "Vše"
- **THEN** events from any date are shown, limited only by other active filters

#### Scenario: User can search events by name

- **WHEN** a user types part of an event name into the search field
- **THEN** only events whose name contains the typed text are shown

#### Scenario: User can search events by location

- **WHEN** a user types part of a location name into the search field
- **THEN** only events whose location contains the typed text are shown

#### Scenario: Search is case-insensitive

- **WHEN** a user types "JIHLAVA" into the search field
- **AND** there is an event with location "Jihlava"
- **THEN** that event appears in the results

#### Scenario: Search ignores diacritics

- **WHEN** a user types "cernav" (without diacritics) into the search field
- **AND** there is an event with location "Černava"
- **THEN** that event appears in the results

#### Scenario: Multi-word search matches events containing all words

- **WHEN** a user types "podzim kolo" into the search field
- **AND** there is an event whose name or location contains both "podzim" and "kolo" in any order
- **THEN** that event appears in the results

#### Scenario: Multi-word search excludes events missing any word

- **WHEN** a user types "podzim kolo" into the search field
- **AND** there is an event whose name or location contains "podzim" but not "kolo"
- **THEN** that event does NOT appear in the results

#### Scenario: Member can filter to only events they are registered to

- **WHEN** a member with a member profile enables the "Moje přihlášky" filter
- **THEN** only events where that member has a registration are shown
- **AND** the filter respects the current time window and other active filters

#### Scenario: "Moje přihlášky" filter includes cancelled events with existing registration

- **WHEN** a member enables "Moje přihlášky" with time window "Vše"
- **AND** the member is registered to an event that has since been cancelled
- **THEN** that cancelled event appears in the results

#### Scenario: "Moje přihlášky" filter includes finished events with past registration

- **WHEN** a member enables "Moje přihlášky" with time window "Proběhlé"
- **AND** the member was registered to an event that has finished
- **THEN** that finished event appears in the results

#### Scenario: "Moje přihlášky" with no matching registrations returns empty list

- **WHEN** a member enables "Moje přihlášky"
- **AND** the member has no registrations matching the current filters
- **THEN** the list is empty

#### Scenario: User without a member profile sees no results when filtering by own registrations

- **WHEN** a user who has no member profile requests the events list filtered to their own registrations
- **THEN** the list is empty and no error is shown

#### Scenario: Filters combine with AND semantics

- **WHEN** a user combines multiple filters (e.g., fulltext "jihlava" + time window "Proběhlé" + "Moje přihlášky")
- **THEN** only events matching every active filter are shown

#### Scenario: Empty result shows a message

- **WHEN** the combination of active filters matches no events
- **THEN** the list displays a message indicating no events match the current filters

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

### Requirement: Apply Category Preset in Event Form

The system SHALL allow event managers to populate the categories field in the event create/edit form by selecting an existing Category Preset. Selecting a preset replaces the current categories field value with the preset's categories. The manager can further edit the categories manually after applying a preset.

#### Scenario: Manager applies a category preset in the event form

- **WHEN** a manager opens the event create or edit form
- **AND** at least one Category Preset exists
- **AND** the manager clicks the "Select from templates" button next to the categories field
- **AND** selects a preset from the list
- **THEN** the categories field is populated with the categories from the selected preset
- **AND** the manager can still edit the categories manually before saving

#### Scenario: Category preset picker is not shown when no presets exist

- **WHEN** a manager opens the event create or edit form
- **AND** no Category Presets exist in the system
- **THEN** the "Select from templates" button is NOT shown next to the categories field

