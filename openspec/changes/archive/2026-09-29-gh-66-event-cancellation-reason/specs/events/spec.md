## MODIFIED Requirements

### Requirement: List Events

The system SHALL show a paginated event list. DRAFT events are only visible to users with EVENTS:MANAGE permission. CANCELLED events are only visible to users with EVENTS:MANAGE permission and to members registered for that event — every other user does not see them in the list at all. By default the list shows only upcoming events (events whose date is today or later). The list can be filtered by status, organizer, date range, coordinator, fulltext (event name and location), and registered-by-me. Multiple filters combine with AND semantics.

#### Scenario: Regular user does not see DRAFT events

- **WHEN** user without EVENTS:MANAGE permission views the event list
- **THEN** no DRAFT events are shown

#### Scenario: Regular user does not see CANCELLED events

- **WHEN** user without EVENTS:MANAGE permission views the event list
- **AND** the user is not registered for a cancelled event
- **THEN** that cancelled event is not shown

#### Scenario: Registered member sees a cancelled event they are registered for

- **WHEN** a member without EVENTS:MANAGE permission views the event list
- **AND** an event the member is registered for has been cancelled
- **THEN** that cancelled event is shown in the list, subject to the other active filters

#### Scenario: Manager sees all events including DRAFT

- **WHEN** user with EVENTS:MANAGE permission views the event list
- **THEN** events in all statuses including DRAFT are shown

#### Scenario: User can filter events by status

- **WHEN** user applies a status filter (e.g., ACTIVE)
- **THEN** only events with that status are shown

#### Scenario: Regular user filtering by DRAFT sees no results

- **WHEN** user without EVENTS:MANAGE permission filters events by DRAFT status
- **THEN** the list is empty and no DRAFT events are disclosed

#### Scenario: Regular user filtering by CANCELLED sees only their own cancelled events

- **WHEN** user without EVENTS:MANAGE permission filters events by CANCELLED status
- **THEN** only cancelled events the user is registered for are shown
- **AND** a user not registered for any cancelled event sees no results

#### Scenario: User can filter events by organizer

- **WHEN** user filters the event list by organizer code
- **THEN** only events from that organizer are shown

#### Scenario: User can filter events by date range

- **WHEN** user filters the event list by a date range
- **THEN** only events within that date range are shown

#### Scenario: User can filter events by coordinator

- **WHEN** user filters the event list by a coordinator member
- **THEN** only events where that member appears anywhere in the coordinators collection are shown

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

The system SHALL display the events list as a table with key columns and a filter bar. The status column is only shown to users with EVENTS:MANAGE permission. The name of a cancelled event SHALL be rendered struck through for every user who can see the row; the rest of the row, including its actions, is displayed normally. The filter bar exposes fulltext search, a time window selector (Budoucí / Proběhlé / Vše), and — for users with a member profile — a "Moje přihlášky" toggle. The default sort order depends on the active time window: upcoming events are sorted by event date ascending (nearest first), past and all events are sorted by event date descending (most recent first).

#### Scenario: Regular member views events table

- **WHEN** a club member views the events list page
- **THEN** the table shows columns: date, name, location, organizer, website link, registration deadline, coordinator name, and registration action

#### Scenario: Manager views events table with status column

- **WHEN** user with EVENTS:MANAGE permission views the events list page
- **THEN** the table additionally shows the event status column

#### Scenario: Cancelled event name is struck through in the table

- **WHEN** a user who is allowed to see a cancelled event views the events list
- **THEN** the event name in that row is rendered struck through
- **AND** the struck name also carries the cancelled state as visually hidden text, so a screen reader announces it
- **AND** the rest of the row is displayed normally

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

The application SHALL display the event detail page with location and registration deadline (when set) and categories (when defined), and allow managers to edit them inline. The registrations section and the link to the registrations list SHALL only be shown for events that are not in DRAFT status. For a cancelled event the detail page SHALL render the event name struck through, show a prominent "Zrušeno" badge, and display the cancellation reason directly under the name when a reason was provided.

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

#### Scenario: Cancelled event detail shows struck-through name, badge and reason

- **WHEN** a user views the detail page of a cancelled event that has a cancellation reason
- **THEN** the event name is rendered struck through
- **AND** a prominent "Zrušeno" badge is shown next to the name
- **AND** the cancellation reason is displayed directly under the name

#### Scenario: Cancelled event detail without a reason

- **WHEN** a user views the detail page of a cancelled event that has no cancellation reason
- **THEN** the event name is rendered struck through
- **AND** the "Zrušeno" badge is shown next to the name
- **AND** no reason text is displayed

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions: DRAFT → ACTIVE → FINISHED or CANCELLED. The transition from ACTIVE to FINISHED is performed exclusively by the automatic completion process; there is no manual "finish" action available to managers.

When cancelling an event, the manager MAY provide an optional cancellation reason (free text, up to 500 characters). The reason SHALL be stored with the event and SHALL be displayed to viewers of the cancelled event detail directly under the event name. In summary views (e.g. the event list) a cancelled event is marked by its struck-through name.

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
- **AND** the cancellation reason is shown on the event detail page under the event name

#### Scenario: Manager cancels an ACTIVE event with a reason

- **WHEN** user with EVENTS:MANAGE permission cancels an ACTIVE event and provides a cancellation reason
- **THEN** the event becomes CANCELLED with the reason recorded
- **AND** existing registrations are preserved for records
- **AND** the cancellation reason is shown on the event detail page under the event name

#### Scenario: Cancelled event is marked in the event list

- **GIVEN** an event has been cancelled
- **WHEN** a user who is allowed to see that event views the event list
- **THEN** the event name on that row is rendered struck through

#### Scenario: Invalid status transition shows error

- **WHEN** user attempts an invalid status transition (e.g., FINISHED → ACTIVE)
- **THEN** the system shows an error that the transition is not allowed

### Requirement: Get Event Detail

The system SHALL display complete event detail including categories. DRAFT events are only visible to users with EVENTS:MANAGE permission. CANCELLED events remain accessible to every authenticated user — hiding cancelled events applies to the events list only, not to the detail page.

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

#### Scenario: Any authenticated user can open a cancelled event detail

- **WHEN** an authenticated user without EVENTS:MANAGE permission opens the detail page of a cancelled event, even without being registered for it
- **THEN** the detail page is displayed with the cancelled treatment (struck-through name, "Zrušeno" badge, and the reason when provided)

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
