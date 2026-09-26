## MODIFIED Requirements

### Requirement: Row-Level Management Actions in Events Table

The system SHALL expose row-level management actions for events directly in the events list for users with EVENTS:MANAGE permission. The available actions for each row depend on the event status and whether the event was imported from ORIS. The actions are driven by HAL-Forms affordances attached to each row and render only when the current user is authorized. For an ORIS-imported DRAFT or ACTIVE event, the row SHALL show a synchronisation status indicator instead of a direct "Synchronizovat" action; opening it lets the manager trigger synchronisation and see the current synchronisation state, consistent with the event detail page.

#### Scenario: Manager sees Upravit and Zrušit actions for a DRAFT event in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **THEN** each DRAFT event row shows "Upravit" and "Zrušit" actions

#### Scenario: Manager sees Upravit and Zrušit actions for an ACTIVE event in the list

- **WHEN** user with EVENTS:MANAGE permission views the events list
- **THEN** each ACTIVE event row shows "Upravit" and "Zrušit" actions

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

- **WHEN** user without EVENTS:MANAGE permission views the events list
- **THEN** event rows show only the register or unregister action (when applicable)
- **AND** no management actions are shown

#### Scenario: Register action in the list is preserved

- **WHEN** user views the events list
- **AND** an event has open registrations
- **THEN** the row shows the register or unregister action as described by the existing events table scenarios

### Requirement: Synchronisation State Is Reachable From The Event

An ORIS-imported event SHALL expose its synchronisation state to users allowed to manage synchronisation: whether it is in step, failing, waiting for a decision or stopped, when it was last successfully synchronised, and what differs when a decision is needed. This state SHALL be reachable both from the events list and from the event detail page.

#### Scenario: The manager opens an event's synchronisation state from the detail page

- **WHEN** a user allowed to manage synchronisation views an ORIS-imported event's detail page
- **THEN** they can open its synchronisation state from there

#### Scenario: The manager opens an event's synchronisation state from the events list

- **WHEN** a user allowed to manage synchronisation views the events list
- **THEN** they can open an ORIS-imported event's synchronisation state directly from its row

#### Scenario: An event not imported from ORIS has no synchronisation state

- **WHEN** a user views an event that was created manually in Klabis
- **THEN** no synchronisation state is offered for it, neither in the list nor on the detail page
