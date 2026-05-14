## ADDED Requirements

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
