## MODIFIED Requirements

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
