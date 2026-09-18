# Events Specification

## ADDED Requirements

### Requirement: ORIS-Imported Events Are Kept In Step Automatically

The system SHALL keep an event imported from ORIS in step with ORIS from then on, without a manager having to ask for it. Changes published in ORIS SHALL reach the event on their own. The system SHALL stop keeping an event in step once it is finished or cancelled, and SHALL keep the record of its past synchronisations available.

#### Scenario: An event imported from ORIS starts being kept in step

- **WHEN** an event manager imports an event from ORIS
- **THEN** the event is from then on kept in step with its ORIS counterpart automatically
- **AND** the manager can still synchronise it on demand as before

#### Scenario: A change published in ORIS reaches the event

- **GIVEN** an ORIS-imported event that nobody has edited in Klabis
- **WHEN** the organiser changes the event in ORIS — for example moving the registration deadline
- **THEN** the event in Klabis is updated to match ORIS without anyone asking for it

#### Scenario: A finished event stops being kept in step

- **WHEN** an ORIS-imported event becomes finished or is cancelled
- **THEN** it is no longer synchronised with ORIS
- **AND** its last successful synchronisation remains visible

### Requirement: A Manager's Edit To An ORIS Field Is Never Silently Overwritten

ORIS does not accept changes to an event from Klabis, so an edit made in Klabis to a field that ORIS owns — the name, date, location, organiser, website, registration deadlines, ranking or base entry fee — SHALL NOT be sent to ORIS. The next synchronisation SHALL NOT discard such an edit. Instead the event SHALL stop being synchronised and SHALL ask the manager to decide, showing what differs from ORIS.

#### Scenario: A manager's edit survives the next synchronisation

- **GIVEN** an ORIS-imported event
- **WHEN** an event manager edits the event's name
- **AND** the event is synchronised with ORIS afterwards
- **THEN** the manager's name is kept
- **AND** the event is marked as needing a decision about the difference against ORIS

#### Scenario: The manager sees what differs from ORIS

- **WHEN** an event manager opens the synchronisation state of an event that needs a decision
- **THEN** they see the values held in Klabis and the values held in ORIS
- **AND** for each differing field they see whether it was changed in Klabis, in ORIS, or in both

#### Scenario: Synchronisation of that event pauses until the manager decides

- **GIVEN** an ORIS-imported event that needs a decision
- **WHEN** further synchronisation runs take place
- **THEN** the event is left untouched
- **AND** it continues to be reported as needing a decision

#### Scenario: The manager discards their edit and takes the ORIS values

- **GIVEN** an event that needs a decision, whose difference the manager has confirmed seeing
- **WHEN** the manager chooses to take the ORIS values
- **THEN** the event is updated to match ORIS
- **AND** the event is kept in step automatically again

#### Scenario: The manager keeps a deliberate difference from ORIS

- **GIVEN** an event that needs a decision, whose difference the manager has confirmed seeing
- **WHEN** the manager chooses to keep the difference — because the Klabis value is deliberately not the ORIS one
- **THEN** the event keeps its Klabis values and ORIS keeps its own
- **AND** the event is kept in step automatically again
- **AND** a later change in ORIS asks the manager to decide again instead of overwriting the value they kept

#### Scenario: Sending Klabis values to ORIS is never offered

- **WHEN** an event manager resolves a difference on an ORIS-imported event
- **THEN** sending the Klabis values to ORIS is not offered, because ORIS does not accept event changes from Klabis
- **AND** taking the ORIS values and keeping the difference remain available

#### Scenario: Editing fields ORIS does not own never causes a decision

- **GIVEN** an ORIS-imported event
- **WHEN** an event manager changes a category fee override, adds a category of their own, or sets the event type
- **AND** the event is synchronised with ORIS afterwards
- **THEN** those changes are kept
- **AND** no decision is asked of anyone, because ORIS does not own those fields

#### Scenario: The manager fixes the event in ORIS instead

- **GIVEN** an ORIS-imported event that needs a decision, which the manager organises themselves
- **WHEN** the manager corrects the event directly in ORIS so that both sides hold the same values
- **THEN** the event stops needing a decision on its own
- **AND** it is kept in step automatically again

### Requirement: An Event Stopped By Repeated Synchronisation Failures Can Be Restarted

When synchronising an event with ORIS keeps failing, the system SHALL stop attempting the event and SHALL let it wait for a manager, rather than failing silently every night. The system SHALL allow a manager to restart it once the cause is gone.

#### Scenario: A repeatedly failing event stops and says so

- **WHEN** synchronising an ORIS-imported event fails repeatedly
- **THEN** the event stops being synchronised
- **AND** its synchronisation state reports that it needs manual attention, with the reason of the most recent failure

#### Scenario: The manager restarts a stopped event

- **GIVEN** an ORIS-imported event stopped after repeated failures
- **WHEN** an event manager restarts its synchronisation
- **THEN** the event is synchronised again from the next run onwards

### Requirement: Synchronisation State Is Reachable From The Event

An ORIS-imported event SHALL expose its synchronisation state to users allowed to manage synchronisation: whether it is in step, failing, waiting for a decision or stopped, when it was last successfully synchronised, and what differs when a decision is needed.

#### Scenario: The manager opens an event's synchronisation state

- **WHEN** a user allowed to manage synchronisation views an ORIS-imported event
- **THEN** they can open its synchronisation state from the event

#### Scenario: An event not imported from ORIS has no synchronisation state

- **WHEN** a user views an event that was created manually in Klabis
- **THEN** no synchronisation state is offered for it

## MODIFIED Requirements

### Requirement: Bulk Synchronize ORIS-Imported Upcoming Events

The system SHALL provide a single action that synchronizes every ORIS-imported event whose status is DRAFT or ACTIVE and whose event date is today or in the future. The action SHALL be available only to users with the EVENTS:MANAGE authority and SHALL be exposed as a global toolbar action above the events list (not as a per-row action).

The action SHALL process matching events sequentially. A failure on one event SHALL NOT abort the operation; the system SHALL continue processing the remaining events and report a summary at the end with the number of successful syncs, the number of failures, and per-event details for failures (event id, name, error description).

Events that are waiting for a manager's decision about a difference against ORIS, and events that have stopped after repeated failures, SHALL NOT be synchronized by this action. They SHALL be reported separately in the summary, with their name and what they are waiting for, so that the manager can resolve them individually. The action SHALL synchronize every other matching event regardless of when it was last synchronized.

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

#### Scenario: Bulk sync reports events waiting for a decision instead of syncing them

- **GIVEN** the events list contains four ORIS-imported upcoming events, one of which is waiting for a manager's decision about a difference against ORIS
- **WHEN** the manager triggers the bulk sync
- **THEN** the three other events are synchronized
- **AND** the event waiting for a decision is left untouched
- **AND** the summary names it separately as waiting for a decision, rather than counting it as a failure

#### Scenario: Bulk sync reports events stopped by repeated failures

- **GIVEN** the events list contains ORIS-imported upcoming events, one of which stopped after repeated failures
- **WHEN** the manager triggers the bulk sync
- **THEN** the stopped event is not attempted
- **AND** the summary names it separately as needing manual attention

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
