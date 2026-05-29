# Events Specification

## Purpose

Covers the creation, management, and lifecycle of club events. Defines how event managers create and update events, how events transition through statuses (draft, active, finished, cancelled), how members discover events in the list, and how registration deadlines control when members can sign up.
## Requirements
### Requirement: Registration Deadline

The system SHALL support up to three optional registration deadlines per event. When set, deadlines determine the last dates when members can register or unregister; the registration window remains open until the latest set deadline passes.

The deadlines are sequential. A second deadline may be set only if a first deadline is set; a third deadline only if both first and second are set. Each deadline must be on or before the event date and on or after the previous deadline.

When the event has at least one deadline, the system SHALL surface the deadline that is currently relevant — the earliest deadline still in the future, or the last deadline if all have passed — in summary views (e.g. event list); detailed views SHALL list all set deadlines chronologically.

#### Scenario: Event created with a single registration deadline

- **WHEN** event manager creates an event and sets only the first deadline
- **THEN** the event is saved with the first deadline; the second and third deadlines are not set
- **AND** registrations remain open until the first deadline passes

#### Scenario: Event created with three registration deadlines

- **WHEN** event manager creates an event and sets first, second, and third deadlines (each on or before the event date and chronologically increasing)
- **THEN** the event is saved with all three deadlines
- **AND** registrations remain open until the third deadline passes

#### Scenario: Event created without a registration deadline

- **WHEN** event manager creates an event without setting any deadline
- **THEN** registration availability is determined solely by the event status and event date

#### Scenario: Second deadline cannot be set without a first deadline

- **WHEN** event manager attempts to set the second deadline while leaving the first deadline empty
- **THEN** the form shows an error that a previous deadline must be set first

#### Scenario: Third deadline cannot be set without a second deadline

- **WHEN** event manager attempts to set the third deadline while leaving the second deadline empty
- **THEN** the form shows an error that a previous deadline must be set first

#### Scenario: Deadlines must be chronologically ordered

- **WHEN** event manager sets a later deadline to a date earlier than a previous deadline
- **THEN** the form shows an error that deadlines must be in chronological order

#### Scenario: Each deadline must be on or before event date

- **WHEN** event manager sets any deadline to a date after the event date
- **THEN** the form shows an error that the deadline must be on or before the event date

#### Scenario: Registrations are closed after the last deadline passes

- **WHEN** the current date is past the latest set deadline
- **THEN** the registration action is no longer available for that event

#### Scenario: Registrations remain open while at least one deadline is in the future

- **WHEN** the first deadline has passed but a later deadline is still in the future
- **THEN** the registration action is available for members

#### Scenario: No deadline means default open/close behaviour

- **WHEN** an event has no registration deadline set
- **THEN** registrations are open when the event is active and the event date is in the future

#### Scenario: Event list shows the currently relevant deadline

- **GIVEN** an event has multiple deadlines and the first has already passed
- **WHEN** a user views the event list
- **THEN** the deadline column shows the next future deadline
- **AND** the row indicates that additional deadlines exist (e.g. through an icon or badge)

#### Scenario: Event detail lists all set deadlines

- **GIVEN** an event has multiple deadlines
- **WHEN** a user views the event detail
- **THEN** all set deadlines are listed chronologically
- **AND** the deadline that is currently relevant is visually highlighted

### Requirement: ORIS Import Includes Registration Deadlines

The system SHALL import all available registration deadlines from ORIS (mapping `EntryDate1`, `EntryDate2`, `EntryDate3` onto the first, second, and third deadlines of the imported event).

#### Scenario: Import event with one registration deadline from ORIS

- **WHEN** event manager imports an event from ORIS that has only `EntryDate1`
- **THEN** the imported event has the first deadline set to `EntryDate1`
- **AND** the second and third deadlines are not set

#### Scenario: Import event with multiple registration deadlines from ORIS

- **WHEN** event manager imports an event from ORIS that has `EntryDate1`, `EntryDate2`, and `EntryDate3`
- **THEN** the imported event has all three deadlines set, mapped to the corresponding ORIS entry dates

#### Scenario: Import event without a registration deadline from ORIS

- **WHEN** event manager imports an event from ORIS that has no entry deadline
- **THEN** the imported event has no deadlines set

### Requirement: ORIS Import Tolerates Missing Location

The system SHALL import events from ORIS even when the upstream ORIS event has no location. The imported event is created with an empty location and the import flow does not reject the event on account of the missing field.

#### Scenario: Manager imports an ORIS event without a location

- **WHEN** event manager imports an event from ORIS that has no location
- **THEN** the event is created successfully in DRAFT status with no location
- **AND** the imported event appears in the events list with an empty location cell

#### Scenario: Manager imports an ORIS event with a location

- **WHEN** event manager imports an event from ORIS that has a location
- **THEN** the event is created with the location from ORIS

### Requirement: Multi-Event ORIS Import

The system SHALL allow an event manager to select multiple ORIS events at once in the "Import from ORIS" dialog and import them in a single batch. The dialog SHALL only offer ORIS events that are not yet present in the application, so the manager never selects an event that is already imported. The import processes each selected event independently: a failure to import one event SHALL NOT prevent the others from being imported (no all-or-nothing). The dialog SHALL allow at most a fixed maximum number of events to be selected in a single batch and SHALL make this limit clear to the manager before submitting. After the batch completes, the manager SHALL see a per-event result indicating success or failure for each selected event, where successfully imported events are clearly marked as successful and failed events as failed. The number of currently selected events SHALL be shown to the manager without redundant duplication. Selecting a single event remains a valid case of this batch flow.

#### Scenario: Manager selects multiple events to import

- **WHEN** an event manager opens the "Import from ORIS" dialog
- **THEN** each available ORIS event is presented with a selection control allowing more than one event to be selected at the same time
- **AND** the manager can select several events before confirming

#### Scenario: Already imported events are not offered

- **WHEN** an event manager opens the "Import from ORIS" dialog
- **THEN** ORIS events that have already been imported into the application are not shown in the list of events available to import
- **AND** only events that can still be imported are presented for selection

#### Scenario: No events remain to import

- **WHEN** every ORIS event for the selected region has already been imported
- **THEN** the dialog shows an empty state indicating there are no events left to import

#### Scenario: Manager imports the selected events in one action

- **WHEN** an event manager has selected several ORIS events and confirms the import
- **THEN** all selected events are imported in a single operation
- **AND** the manager is not required to repeat the import for each event individually

#### Scenario: Selection is limited to the maximum batch size

- **WHEN** an event manager has selected the maximum allowed number of ORIS events in the dialog
- **THEN** the manager cannot add further events to the selection
- **AND** the dialog makes the maximum limit clear to the manager

#### Scenario: One event fails but the rest are imported

- **WHEN** an event manager imports several ORIS events and one of them cannot be imported
- **THEN** the remaining selected events are still imported successfully
- **AND** the failing event does not cancel the whole batch

#### Scenario: Manager sees a per-event result summary

- **WHEN** a batch import finishes
- **THEN** the manager sees a summary listing each selected event with its date and name
- **AND** each successfully imported event is clearly marked as imported successfully
- **AND** each failed event is clearly marked as failed and shows the reason it could not be imported

#### Scenario: Selected count is shown once

- **WHEN** an event manager has selected one or more ORIS events in the dialog
- **THEN** the number of selected events is presented to the manager a single time, without being repeated elsewhere in the same dialog area

#### Scenario: Importing a single selected event

- **WHEN** an event manager selects exactly one ORIS event and confirms the import
- **THEN** that event is imported
- **AND** the result summary lists the single event with its outcome

#### Scenario: Confirm action is unavailable when nothing is selected

- **WHEN** an event manager has not selected any ORIS event in the dialog
- **THEN** the import cannot be confirmed

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

When cancelling an event, the manager MAY provide an optional cancellation reason (free text, up to 500 characters). The reason SHALL be stored with the event and SHALL be displayed to viewers of the cancelled event detail; if a reason is set, summary views (e.g. event list) SHALL surface it as supplementary text on the cancelled row.

#### Scenario: Manager publishes a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission publishes an event in DRAFT status
- **THEN** the event becomes ACTIVE
- **AND** members can now register for it

#### Scenario: Manager cancels a DRAFT event without a reason

- **WHEN** user with EVENTS:MANAGE permission cancels a DRAFT event and leaves the cancellation reason empty
- **THEN** the event becomes CANCELLED with no reason recorded

#### Scenario: Manager cancels a DRAFT event with a reason

- **WHEN** user with EVENTS:MANAGE permission cancels a DRAFT event and provides a cancellation reason
- **THEN** the event becomes CANCELLED with the reason recorded
- **AND** the cancellation reason is shown on the event detail page

#### Scenario: Manager cancels an ACTIVE event with a reason

- **WHEN** user with EVENTS:MANAGE permission cancels an ACTIVE event and provides a cancellation reason
- **THEN** the event becomes CANCELLED with the reason recorded
- **AND** existing registrations are preserved for records
- **AND** the cancellation reason is shown on the event detail page

#### Scenario: Cancellation reason is shown on the cancelled event row in the list

- **GIVEN** a cancelled event with a recorded cancellation reason
- **WHEN** a user views the event list
- **THEN** the cancelled status indicator on that row exposes the reason as a tooltip or supplementary text

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

### Requirement: Action Buttons Visually Indicate Their Effect

The system SHALL render action buttons in the events list with a visual treatment (color / variant) that indicates the nature of the action, so users can scan the table quickly:

- Actions that create or publish content (e.g. registering for an event, publishing a draft event) SHALL use a primary / success treatment.
- Actions that destructively cancel or remove (e.g. cancelling an event) SHALL use a destructive treatment.
- Actions that reverse a previous user action (e.g. unregistering from an event) SHALL use a warning treatment.
- Actions that change content without destruction (e.g. editing, syncing from ORIS) SHALL use a neutral treatment.

The exact colors are determined by the application theme; the requirement is that the four categories are visually distinguishable.

#### Scenario: Register button stands out as the primary action

- **WHEN** a member views the events list and an active event with open registration is shown
- **THEN** the "Přihlásit se" button on that row uses the primary / success visual treatment

#### Scenario: Cancel event button is visually destructive

- **WHEN** a manager views the events list
- **THEN** the "Zrušit akci" button on each manageable row uses the destructive visual treatment

#### Scenario: Unregister button is visually warning

- **WHEN** a member views the events list and an event they are registered for is shown
- **THEN** the "Odhlásit se z akce" button on that row uses the warning visual treatment

#### Scenario: Edit and sync buttons are visually neutral

- **WHEN** a manager views the events list
- **THEN** the "Upravit" and "Synchronizovat" buttons use the neutral visual treatment

#### Scenario: Publish draft event button is visually primary

- **WHEN** a manager views a DRAFT event row
- **THEN** the "Publikovat" button uses the primary / success visual treatment

### Requirement: Bulk Synchronize ORIS-Imported Upcoming Events

The system SHALL provide a single action that synchronizes every ORIS-imported event whose status is DRAFT or ACTIVE and whose event date is today or in the future. The action SHALL be available only to users with the EVENTS:MANAGE authority and SHALL be exposed as a global toolbar action above the events list (not as a per-row action).

The action SHALL process matching events sequentially. A failure on one event SHALL NOT abort the operation; the system SHALL continue processing the remaining events and report a summary at the end with the number of successful syncs, the number of failures, and per-event details for failures (event id, name, error description).

The action SHALL be idempotent — running it twice in a row with no upstream changes SHALL produce the same result as running it once.

#### Scenario: Manager triggers bulk sync with all events in good state

- **GIVEN** the events list contains five ORIS-imported events with status DRAFT or ACTIVE and event dates in the future
- **WHEN** an authenticated manager triggers the "Synchronizovat všechny budoucí z ORIS" toolbar action
- **THEN** the system synchronizes all five events from ORIS
- **AND** the result dialog shows "5 úspěšně synchronizováno, 0 chyb"

#### Scenario: Manager triggers bulk sync, one event fails

- **GIVEN** the events list contains five ORIS-imported upcoming events and ORIS returns an error for one of them
- **WHEN** the manager triggers the bulk sync
- **THEN** the system synchronizes the four successful events and skips the failed one
- **AND** the result dialog shows "4 úspěšně synchronizováno, 1 chyba"
- **AND** the dialog lists the failed event's name and the error reason

#### Scenario: Bulk sync excludes finished, cancelled, and past events

- **GIVEN** the events list contains both ORIS-imported upcoming events and ORIS-imported FINISHED / CANCELLED / past-date events
- **WHEN** the manager triggers the bulk sync
- **THEN** only events that are DRAFT or ACTIVE with event date today or later are processed
- **AND** the FINISHED / CANCELLED / past events are not touched

#### Scenario: Bulk sync excludes events not imported from ORIS

- **GIVEN** the events list contains a mix of ORIS-imported and manually created events
- **WHEN** the manager triggers the bulk sync
- **THEN** only ORIS-imported events are processed
- **AND** manually created events are not touched

#### Scenario: Member without EVENTS:MANAGE does not see the bulk sync action

- **WHEN** a member without EVENTS:MANAGE views the events list
- **THEN** the "Synchronizovat všechny budoucí z ORIS" toolbar action is not displayed

### Requirement: Event Type Assignment

The system SHALL allow every club event to optionally have an event type assigned from the event types catalog. Events without a type continue to display normally. When a type is set, the event detail page and the events list SHALL display the event type as a colored badge with the type name.

Users with EVENTS:MANAGE authority SHALL be able to set, change, or clear the event type in the event create form and the event update form.

#### Scenario: Manager creates an event with a type

- **GIVEN** the event types catalog contains "Trénink" and "Pohárový závod"
- **WHEN** a manager fills in the event create form, selects type "Pohárový závod", and submits
- **THEN** the event is created with that type
- **AND** the event detail and event list display "Pohárový závod" as a colored badge

#### Scenario: Manager creates an event without a type

- **WHEN** a manager fills in the event create form, leaves the type dropdown empty, and submits
- **THEN** the event is created without a type
- **AND** the event detail and event list show no type badge for the event

#### Scenario: Manager changes the type of an existing event

- **GIVEN** an event has type "Trénink"
- **WHEN** the manager opens the event update form, selects type "Pohárový závod", and submits
- **THEN** the event's type changes to "Pohárový závod"

#### Scenario: Manager clears the type of an existing event

- **GIVEN** an event has a type assigned
- **WHEN** the manager opens the update form, clears the type dropdown, and submits
- **THEN** the event has no type

### Requirement: Filter Events by Type

The events list filter bar SHALL include a multi-select filter by event type. When one or more types are selected, the list SHALL show only events whose type matches one of the selected values. Events without an assigned type SHALL be excluded from filtered results unless the user explicitly includes "no type" in the selection.

#### Scenario: User filters events by a single type

- **GIVEN** the events list contains 3 events of type "Pohárový závod" and 7 events of other types or no type
- **WHEN** the user selects "Pohárový závod" in the type filter
- **THEN** the list shows only the 3 matching events

#### Scenario: User filters events by multiple types

- **GIVEN** the events list contains events of various types
- **WHEN** the user selects "Pohárový závod" and "Trénink" in the type filter
- **THEN** the list shows events that have either of those two types

#### Scenario: User clears the type filter

- **GIVEN** the user has applied a type filter
- **WHEN** the user clears the type filter selection
- **THEN** the list shows events of all types again

### Requirement: ORIS Import Auto-Maps Event Type by Name

The ORIS import SHALL look up an event type from the catalog using a case-insensitive name match against the type identifier provided by the ORIS payload. If a matching type is found, it is assigned to the imported event. If no match is found, the imported event is created without a type — the manager assigns it manually later. The import SHALL NOT create new entries in the event types catalog automatically.

#### Scenario: ORIS event maps to an existing catalog type

- **GIVEN** the catalog contains a type "Pohárový závod" and the imported ORIS event has type identifier "Pohárový závod"
- **WHEN** the manager imports the event
- **THEN** the imported event has type "Pohárový závod" assigned

#### Scenario: ORIS event has no matching catalog type

- **GIVEN** the catalog does not contain a type matching the ORIS payload type identifier
- **WHEN** the manager imports the event
- **THEN** the imported event is created with no type
- **AND** no new type is added to the catalog

#### Scenario: ORIS type identifier matches case-insensitively

- **GIVEN** the catalog contains a type "Trénink" and the imported ORIS event has type identifier "trénink" or "TRÉNINK"
- **WHEN** the manager imports the event
- **THEN** the imported event has type "Trénink" assigned

### Requirement: Filter Events by Year

The events list filter bar SHALL include a year selector that allows the user to restrict the list to a single calendar year. The selector exposes the past ten years and the next two years relative to the current year, plus an explicit "no year" option. The default value of the year selector is the **current year**.

The year selector and the time-window selector (Budoucí / Proběhlé / Vše) SHALL combine with AND semantics. When both are set, the list shows only events whose date falls within the selected year AND matches the selected time window. Selecting or changing one selector SHALL NOT modify the other.

The availability of the time-window options SHALL depend on the selected year:

- When **"no year"** or the **current year** is selected, all three time-window options (Budoucí, Proběhlé, Vše) are available.
- When a **year other than the current year** is selected, only "Vše" is available; "Budoucí" and "Proběhlé" SHALL be disabled in the UI. If the time-window selector was previously set to "Budoucí" or "Proběhlé", it SHALL be coerced to "Vše" at the moment the non-current year is selected.

When a non-current year is later replaced by "no year" or the current year, the disabled time-window options become available again. The coerced "Vše" value remains in effect until the user changes it; the previous time-window value is not restored.

#### Scenario: Default year selector value is the current year

- **GIVEN** the current year is 2026
- **WHEN** a user opens the events list page without any explicit filter
- **THEN** the year selector shows "2026" as the active value
- **AND** the list shows only events whose date is in 2026 combined with the default time-window selector value

#### Scenario: User filters by current year and "Budoucí"

- **GIVEN** the current year is 2026
- **WHEN** a user selects "2026" in the year selector and "Budoucí" in the time-window selector
- **THEN** the list shows only events whose date is in 2026 AND is today or later
- **AND** all three time-window options remain enabled

#### Scenario: User filters by current year and "Proběhlé"

- **GIVEN** the current year is 2026
- **WHEN** a user selects "2026" in the year selector and "Proběhlé" in the time-window selector
- **THEN** the list shows only events whose date is in 2026 AND is before today
- **AND** all three time-window options remain enabled

#### Scenario: User selects a past year

- **GIVEN** the current year is 2026 and the time-window selector is set to "Budoucí"
- **WHEN** the user selects "2024" in the year selector
- **THEN** the list shows only events whose date is in 2024
- **AND** the time-window selector is coerced to "Vše"
- **AND** the "Budoucí" and "Proběhlé" options are disabled

#### Scenario: User selects a future year

- **GIVEN** the current year is 2026 and the time-window selector is set to "Proběhlé"
- **WHEN** the user selects "2027" in the year selector
- **THEN** the list shows only events whose date is in 2027
- **AND** the time-window selector is coerced to "Vše"
- **AND** the "Budoucí" and "Proběhlé" options are disabled

#### Scenario: User changes time-window selector while a year is selected

- **GIVEN** the current year is 2026, the year selector is set to "2026", and the time-window selector is "Vše"
- **WHEN** the user changes the time-window selector to "Budoucí"
- **THEN** the year selector remains set to "2026"
- **AND** the list shows only events whose date is in 2026 AND is today or later

#### Scenario: User clears the year filter

- **GIVEN** the year filter is set to "2024" and the time-window selector is "Vše" (disabled options "Budoucí" and "Proběhlé")
- **WHEN** the user changes the year selector to "no year"
- **THEN** the year-based date constraint is removed
- **AND** all three time-window options become enabled
- **AND** the time-window selector remains set to "Vše"

#### Scenario: User switches from past year back to current year

- **GIVEN** the current year is 2026, the year selector is set to "2024", and the time-window selector is "Vše"
- **WHEN** the user changes the year selector to "2026"
- **THEN** the "Budoucí" and "Proběhlé" options become enabled
- **AND** the time-window selector remains set to "Vše" until the user changes it

#### Scenario: Year filter persists in the URL

- **WHEN** the user has selected a year and a time window and reloads the page
- **THEN** the same year and time window remain in effect
- **AND** the list shows only events matching both filters

### Requirement: Event Coordinator Label Reads as "Vedoucí"

In every part of the user interface that exposes the event coordinator (table column header, filter label, detail section heading, form field label), the label SHALL read "Vedoucí" (or "Vedoucí akce" where context demands the longer form), not "Koordinátor". This is a localization preference; the underlying data field name in the API and the domain remains unchanged (the field continues to be referred to as the event coordinator in API contracts, OpenAPI documentation, and developer documentation).

#### Scenario: Events table header

- **WHEN** a user views the events list
- **THEN** the column that shows the event coordinator is headed "Vedoucí" (not "Koordinátor")

#### Scenario: Filter bar label

- **WHEN** a user opens the events list filter bar
- **THEN** the filter that targets the coordinator is labelled "Vedoucí" (not "Koordinátor")

#### Scenario: Event detail section

- **WHEN** a user views an event detail
- **THEN** the section displaying the coordinator's name reads "Vedoucí" (not "Koordinátor")

#### Scenario: Event create / update form field

- **WHEN** an event manager opens the create or update form for an event
- **THEN** the field for assigning a coordinator is labelled "Vedoucí" (not "Koordinátor")

