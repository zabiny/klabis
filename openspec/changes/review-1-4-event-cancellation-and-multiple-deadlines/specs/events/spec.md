## MODIFIED Requirements

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

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions: DRAFT → ACTIVE → FINISHED or CANCELLED. The transition from ACTIVE to FINISHED is performed exclusively by the automatic completion process; there is no manual "finish" action available to managers.

When cancelling an event, the manager MAY provide an optional cancellation reason (free text, up to 500 characters). The reason SHALL be stored with the event and SHALL be displayed to viewers of the cancelled event detail; if a reason is set, summary views (e.g. event list) SHALL surface it as supplementary text on the cancelled row.

#### Scenario: Manager publishes a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission publishes an event in DRAFT status
- **THEN** the event becomes ACTIVE

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
