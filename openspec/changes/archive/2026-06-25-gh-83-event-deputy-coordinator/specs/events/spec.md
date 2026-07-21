## MODIFIED Requirements

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

#### Scenario: Single coordinator shown as clickable link

- **WHEN** an event in the table has exactly one coordinator assigned
- **THEN** the coordinator column shows that coordinator's full name as a link to their member detail page

#### Scenario: Multiple coordinators shown as first name plus badge

- **WHEN** an event in the table has more than one coordinator assigned
- **THEN** the coordinator column shows the first coordinator's full name as a link to their member detail page, followed by a "+N" badge indicating the count of additional coordinators

#### Scenario: Coordinator column empty when no coordinators assigned

- **WHEN** an event in the table has no coordinators assigned
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

### Requirement: Create Event

The system SHALL allow users with EVENTS:MANAGE permission to create events. Required fields: name, event date, organizer code. Optional: location, website URL, coordinators, registration deadline, categories.

#### Scenario: Manager creates an event with all required fields

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form with name, event date, and organizer code
- **THEN** the event is created in DRAFT status
- **AND** appears in the event list

#### Scenario: Manager creates an event with optional fields

- **WHEN** user with EVENTS:MANAGE permission fills in optional fields (location, website URL, coordinators, registration deadline, categories) and submits
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

#### Scenario: Duplicate coordinator in collection is deduplicated

- **WHEN** user adds the same member to the coordinators field more than once and submits
- **THEN** the event is created with that member listed once

#### Scenario: Registration deadline after event date shows error

- **WHEN** user sets a registration deadline after the event date
- **THEN** the form shows an error that the deadline must be on or before the event date

#### Scenario: Manager creates event with multiple coordinators

- **WHEN** user with EVENTS:MANAGE permission adds two distinct members to the coordinators field and submits
- **THEN** the event is created with both members as coordinators

### Requirement: Update Event

The system SHALL allow users with EVENTS:MANAGE permission OR any member in the event's coordinators collection to update events in DRAFT or ACTIVE status. Editable fields include categories and the coordinators collection.

#### Scenario: Manager updates a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission edits and saves a DRAFT event
- **THEN** the event is updated with the new values

#### Scenario: Manager updates an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission edits and saves an ACTIVE event
- **THEN** the event is updated with the new values

#### Scenario: Coordinator edits the event without EVENTS:MANAGE

- **WHEN** a member who is listed as a coordinator of a DRAFT or ACTIVE event edits and saves that event
- **THEN** the event is updated with the new values
- **AND** no EVENTS:MANAGE permission is required

#### Scenario: Finished event cannot be edited

- **WHEN** user attempts to edit a FINISHED event
- **THEN** the system shows an error that finished events cannot be modified

#### Scenario: Cancelled event cannot be edited

- **WHEN** user attempts to edit a CANCELLED event
- **THEN** the system shows an error that cancelled events cannot be modified

#### Scenario: Update action not shown without permission or coordinator role

- **WHEN** user without EVENTS:MANAGE permission views an event
- **AND** the user is not listed as a coordinator of that event
- **THEN** no edit action is available

#### Scenario: Duplicate coordinator in update is deduplicated

- **WHEN** user adds the same member twice to the coordinators field during update and submits
- **THEN** the event is updated with that member listed once

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
- **THEN** only events where that member appears anywhere in the coordinators collection are shown

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

### Requirement: Get Event Detail

The system SHALL display complete event detail including categories. DRAFT events are only visible to users with EVENTS:MANAGE permission.

#### Scenario: User views event detail

- **WHEN** authenticated user navigates to an event detail page
- **THEN** all available event information is displayed (name, date, location when set, organizer, website, coordinators list, registration deadline, categories)

#### Scenario: Event detail shows all coordinators

- **WHEN** authenticated user navigates to an event detail page
- **AND** the event has multiple coordinators
- **THEN** all coordinators are listed by full name, each as a link to their member detail page

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

### Requirement: Event Coordinator Label Reads as "Vedoucí"

In every part of the user interface that exposes the event coordinators (table column header, filter label, detail section heading, form field label), the label SHALL read "Vedoucí" (or "Vedoucí akce" where context demands the longer form), not "Koordinátor". This is a localization preference; the underlying data field name in the API and the domain remains unchanged.

#### Scenario: Events table header

- **WHEN** a user views the events list
- **THEN** the column that shows the event coordinators is headed "Vedoucí" (not "Koordinátor")

#### Scenario: Filter bar label

- **WHEN** a user opens the events list filter bar
- **THEN** the filter that targets coordinators is labelled "Vedoucí" (not "Koordinátor")

#### Scenario: Event detail section

- **WHEN** a user views an event detail
- **THEN** the section displaying the coordinator list reads "Vedoucí" (not "Koordinátor")

#### Scenario: Event create / update form field

- **WHEN** an event manager opens the create or update form for an event
- **THEN** the field for assigning coordinators is labelled "Vedoucí" (not "Koordinátor")

## ADDED Requirements

### Requirement: Coordinator Implicit Edit Authority

Any member listed in an event's coordinators collection SHALL have the same implicit edit authority over that event as a user with EVENTS:MANAGE permission, limited to updating the event and viewing registrations. This authority is event-scoped: it does not extend to other events.

#### Scenario: Coordinator sees edit action on their event

- **WHEN** a member who is a coordinator of an event views that event's detail page
- **THEN** the edit action is available even without EVENTS:MANAGE permission

#### Scenario: Coordinator of one event cannot edit another

- **WHEN** a member is a coordinator of event A but not event B
- **THEN** the edit action is NOT available on event B (unless the member also has EVENTS:MANAGE)

#### Scenario: ORIS import does not assign coordinators on initial import

- **WHEN** a new event is imported from ORIS for the first time
- **THEN** the coordinators collection is empty after import
- **AND** no coordinator is derived from ORIS organizer data

#### Scenario: ORIS sync does not overwrite coordinators

- **WHEN** an already-imported event is updated by syncing from ORIS
- **THEN** the coordinators collection is left unchanged
