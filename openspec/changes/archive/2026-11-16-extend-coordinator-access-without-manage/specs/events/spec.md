# Spec Delta

## MODIFIED Requirements

### Requirement: Row-Level Management Actions in Events Table

The system SHALL expose row-level management actions for events directly in the events list for users with EVENTS:MANAGE permission, and for event coordinators on the rows of the events they coordinate. The available actions for each row depend on the event status and whether the event was imported from ORIS. The actions are driven by HAL-Forms affordances attached to each row and render only when the current user is authorized. For an ORIS-imported DRAFT or ACTIVE event, the row SHALL show a synchronisation status indicator instead of a direct "Synchronizovat" action; opening it lets the manager trigger synchronisation and see the current synchronisation state, consistent with the event detail page.

#### Scenario: Manager sees Upravit and Zrušit actions for a DRAFT event in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **THEN** each DRAFT event row shows "Upravit" and "Zrušit" actions

#### Scenario: Manager sees Upravit and Zrušit actions for an ACTIVE event in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **THEN** each ACTIVE event row shows "Upravit" and "Zrušit" actions

#### Scenario: Coordinator sees management actions on the rows of their own events

- **GIVEN** a member without EVENTS:MANAGE permission who is a coordinator of one DRAFT event and one ACTIVE event
- **WHEN** the member views the events list
- **THEN** the row of their DRAFT event shows "Upravit", "Publikovat" and "Zrušit" actions
- **AND** the row of their ACTIVE event shows "Upravit" and "Zrušit" actions

#### Scenario: Coordinator sees no management actions on events they do not coordinate

- **WHEN** a member without EVENTS:MANAGE permission views the events list
- **AND** an event row belongs to an event the member does not coordinate
- **THEN** that row shows no management actions

#### Scenario: Manager sees synchronisation status indicator for an ORIS-imported DRAFT or ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **AND** an event row is in DRAFT or ACTIVE status and was imported from ORIS
- **THEN** the row additionally shows a synchronisation status indicator
- **AND** opening it offers a way to synchronise the event now

#### Scenario: Non-ORIS event does not show a synchronisation status indicator in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **AND** an event row was not imported from ORIS
- **THEN** the row does NOT show a synchronisation status indicator

#### Scenario: FINISHED or CANCELLED event has no management actions in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **AND** an event row is in FINISHED or CANCELLED status
- **THEN** the row shows no management actions (no edit, cancel, or synchronisation status indicator)

#### Scenario: Regular member sees only the register action in the list

- **WHEN** user without EVENTS:MANAGE permission who coordinates no event views the events list
- **THEN** event rows show only the register or unregister action (when applicable)
- **AND** no management actions are shown

#### Scenario: Register action in the list is preserved

- **WHEN** user views the events list
- **AND** an event has open registrations
- **THEN** the row shows the register or unregister action as described by the existing events table scenarios

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions: DRAFT → ACTIVE → FINISHED or CANCELLED. The transition from ACTIVE to FINISHED is performed exclusively by the automatic completion process; there is no manual "finish" action available to managers. Publishing and cancelling an event SHALL be available to users with EVENTS:MANAGE permission and to the coordinators of that event.

When cancelling an event, the manager or coordinator MAY provide an optional cancellation reason (free text, up to 500 characters). The reason SHALL be stored with the event and SHALL be displayed to viewers of the cancelled event detail directly under the event name. In summary views (e.g. the event list) a cancelled event is marked by its struck-through name.

#### Scenario: Manager publishes a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission publishes an event in DRAFT status
- **THEN** the event becomes ACTIVE
- **AND** members can now register for it

#### Scenario: Coordinator publishes their own DRAFT event

- **WHEN** a member without EVENTS:MANAGE permission who is a coordinator of a DRAFT event clicks "Publikovat" on that event
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

#### Scenario: Coordinator cancels their own event

- **WHEN** a member without EVENTS:MANAGE permission who is a coordinator of a DRAFT or ACTIVE event cancels that event and provides a cancellation reason
- **THEN** the event becomes CANCELLED with the reason recorded
- **AND** existing registrations are preserved for records

#### Scenario: Coordinator cannot publish or cancel an event they do not coordinate

- **WHEN** a member without EVENTS:MANAGE permission views a DRAFT or ACTIVE event they do not coordinate
- **THEN** no "Publikovat" or "Zrušit" action is offered

#### Scenario: Cancelled event is marked in the event list

- **GIVEN** an event has been cancelled
- **WHEN** a user who is allowed to see that event views the event list
- **THEN** the event name on that row is rendered struck through

#### Scenario: Invalid status transition shows error

- **WHEN** user attempts an invalid status transition (e.g., FINISHED → ACTIVE)
- **THEN** the system shows an error that the transition is not allowed

### Requirement: List Events

The system SHALL show a paginated event list. DRAFT events are only visible to users with EVENTS:MANAGE permission and to the coordinators of that event. CANCELLED events are only visible to users with EVENTS:MANAGE permission and to members registered for that event — every other user does not see them in the list at all. By default the list shows only upcoming events (events whose date is today or later). The list can be filtered by status, organizer, date range, coordinator, fulltext (event name and location), and registered-by-me. Multiple filters combine with AND semantics.

#### Scenario: Regular user does not see DRAFT events

- **WHEN** user without EVENTS:MANAGE permission views the event list
- **THEN** no DRAFT events are shown except those the user coordinates

#### Scenario: Coordinator sees their own DRAFT event in the list

- **WHEN** a member without EVENTS:MANAGE permission who is a coordinator of a DRAFT event views the event list
- **THEN** that DRAFT event is shown, subject to the other active filters
- **AND** DRAFT events the member does not coordinate are not shown

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

- **WHEN** user without EVENTS:MANAGE permission who coordinates no DRAFT event filters events by DRAFT status
- **THEN** the list is empty and no DRAFT events are disclosed

#### Scenario: Coordinator filtering by DRAFT sees only their own drafts

- **WHEN** a member without EVENTS:MANAGE permission who coordinates a DRAFT event filters events by DRAFT status
- **THEN** only the DRAFT events the member coordinates are shown

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

- **WHEN** user picks a member in the "Vedoucí" filter of the events list filter bar
- **THEN** only events where that member appears anywhere in the coordinators collection are shown
- **AND** the filter respects the current time window and other active filters

#### Scenario: Clearing the coordinator filter shows events of all coordinators

- **WHEN** user clears the "Vedoucí" filter
- **THEN** the list is no longer restricted by coordinator

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

The system SHALL display complete event detail including categories. DRAFT events are only visible to users with EVENTS:MANAGE permission and to the coordinators of that event. CANCELLED events remain accessible to every authenticated user — hiding cancelled events applies to the events list only, not to the detail page.

#### Scenario: User views event detail

- **WHEN** authenticated user navigates to an event detail page
- **THEN** all available event information is displayed (name, date, location when set, organizer, website, coordinators list, registration deadline, categories)

#### Scenario: Event detail shows all coordinators

- **WHEN** authenticated user navigates to an event detail page
- **AND** the event has multiple coordinators
- **THEN** all coordinators are listed by full name, each as a link to their member detail page

#### Scenario: Regular user cannot access DRAFT event detail

- **WHEN** user without EVENTS:MANAGE permission navigates to the detail page of a DRAFT event they do not coordinate
- **THEN** the page shows not found

#### Scenario: Coordinator views their own DRAFT event detail

- **WHEN** a member without EVENTS:MANAGE permission navigates to the detail page of a DRAFT event they coordinate
- **THEN** the full event detail is displayed

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

#### Scenario: DRAFT event actions available to coordinator

- **WHEN** a member without EVENTS:MANAGE permission views a DRAFT event they coordinate
- **THEN** available actions are: edit, publish, cancel

#### Scenario: ACTIVE event actions available to coordinator

- **WHEN** a member without EVENTS:MANAGE permission views an ACTIVE event they coordinate
- **THEN** available actions are: edit, cancel

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

The system SHALL allow anyone who can create or edit an event — event managers, and coordinators for the events they coordinate — to populate the categories field in the event create/edit form by selecting an existing Category Preset. Selecting a preset replaces the current categories field value with the preset's categories; the user can further edit the categories manually. Applying a preset copies its categories into the event: no ongoing link is kept, so later changes to the preset do not affect the event.

#### Scenario: Manager applies a category preset in the event form

- **WHEN** a manager opens the event create or edit form
- **AND** at least one Category Preset exists
- **AND** the manager clicks the "Select from templates" button next to the categories field
- **AND** selects a preset from the list
- **THEN** the categories field is populated with the categories from the selected preset
- **AND** the manager can still edit the categories manually before saving

#### Scenario: Coordinator applies a category preset when editing their event

- **WHEN** a member without EVENTS:MANAGE permission who coordinates an event opens the edit form of that event
- **AND** at least one Category Preset exists
- **AND** the member clicks the "Select from templates" button next to the categories field and selects a preset
- **THEN** the categories field is replaced with the categories from the selected preset
- **AND** the member can still edit the categories manually before saving

#### Scenario: Category preset picker is not shown when no presets exist

- **WHEN** a manager or coordinator opens the event create or edit form
- **AND** no Category Presets exist in the system
- **THEN** the "Select from templates" button is NOT shown next to the categories field

#### Scenario: Changing a preset does not affect events it was applied to

- **WHEN** a category preset is modified or deleted after being applied to events
- **THEN** the events that previously used the preset retain their original categories unchanged

### Requirement: Coordinator Implicit Edit Authority

Any member listed in an event's coordinators collection SHALL have the same implicit authority over that event as a user with EVENTS:MANAGE permission, limited to updating the event, publishing it, cancelling it and viewing its registrations — including seeing the event while it is a DRAFT. Changing the coordinators, ORIS import and synchronisation stay with EVENTS:MANAGE. This authority is event-scoped: it does not extend to other events.

#### Scenario: Coordinator sees edit action on their event

- **WHEN** a member who is a coordinator of an event views that event's detail page
- **THEN** the edit action is available even without EVENTS:MANAGE permission

#### Scenario: Coordinator sees publish and cancel actions on their event

- **WHEN** a member who is a coordinator of a DRAFT event views that event's detail page
- **THEN** the publish and cancel actions are available even without EVENTS:MANAGE permission

#### Scenario: Coordinator does not get synchronisation actions

- **WHEN** a member without EVENTS:MANAGE permission who is a coordinator of an ORIS-imported event views that event
- **THEN** no synchronisation action is offered to them

#### Scenario: Coordinator of one event cannot edit another

- **WHEN** a member is a coordinator of event A but not event B
- **THEN** the edit, publish and cancel actions are NOT available on event B (unless the member also has EVENTS:MANAGE)

#### Scenario: ORIS import does not assign coordinators on initial import

- **WHEN** a new event is imported from ORIS for the first time
- **THEN** the coordinators collection is empty after import
- **AND** no coordinator is derived from ORIS organizer data

#### Scenario: ORIS sync does not overwrite coordinators

- **WHEN** an already-imported event is updated by syncing from ORIS
- **THEN** the coordinators collection is left unchanged
