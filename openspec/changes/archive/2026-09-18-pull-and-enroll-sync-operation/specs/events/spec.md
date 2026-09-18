## ADDED Requirements

### Requirement: Imported ORIS Events Are Synchronized From The Moment They Are Imported

The system SHALL keep an event imported from ORIS synchronized with ORIS from the moment the import completes, without waiting for a scheduled synchronization run. The import itself SHALL appear in the event's synchronization history, recording the manager who performed it.

#### Scenario: Manager imports an event and sees it as synchronized

- **WHEN** event manager imports an event from ORIS
- **THEN** the event is created in DRAFT status with the values from ORIS
- **AND** the event immediately shows as being in step with ORIS, without a scheduled run having to happen first

#### Scenario: The import appears in the event's synchronization history

- **WHEN** event manager imports an event from ORIS
- **AND** the manager then opens that event's synchronization state
- **THEN** the history shows the synchronization that ran as part of the import, recording the manager who imported it

### Requirement: Re-Importing An ORIS Event Synchronizes It Instead Of Failing

The system SHALL NOT report an error when an event manager imports an ORIS event that is already present in Klabis. The system SHALL synchronize the existing event with ORIS instead and give the manager that event. A change the club made in Klabis to a field ORIS owns SHALL NOT be discarded by a repeated import; where the two sides now differ, the manager SHALL be asked to decide, exactly as for any other synchronization.

Where the event is already waiting for a manager's decision about a difference against ORIS, or has stopped after repeated failures, the system SHALL refuse the import and point the manager at the outstanding decision.

#### Scenario: Manager imports an ORIS event that is already in Klabis

- **WHEN** event manager imports an ORIS event that is already present in Klabis
- **THEN** no second event is created
- **AND** the existing event is synchronized with ORIS
- **AND** the manager is taken to the existing event

#### Scenario: A club's own change survives a repeated import

- **WHEN** someone in the club has changed a field on an imported event that ORIS also owns
- **AND** an event manager then imports that same ORIS event again
- **THEN** the club's change is not overwritten
- **AND** the event is shown as waiting for a manager's decision about the difference against ORIS

#### Scenario: Manager re-imports an event that is waiting for a decision

- **WHEN** event manager imports an ORIS event whose Klabis event is already waiting for a decision about a difference against ORIS
- **THEN** the import is refused
- **AND** the manager is pointed at the outstanding decision for that event

#### Scenario: Batch import counts an already-present event as imported

- **WHEN** event manager imports several ORIS events at once and one of them is already present in Klabis
- **THEN** that event is synchronized and reported as imported alongside the newly created ones
- **AND** the manager is not shown a failure for it

### Requirement: Re-Importing A Finished Or Cancelled ORIS Event Resumes Its Synchronization

An event that has finished or been cancelled stops being synchronized with ORIS. When an event manager deliberately imports that same ORIS event again, the system SHALL resume synchronizing it and SHALL take the current values from ORIS, rather than continuing from what the two sides last agreed on before synchronization stopped. Scheduled synchronization runs SHALL NOT resume such an event on their own.

#### Scenario: Manager re-imports a cancelled event

- **WHEN** event manager imports an ORIS event whose Klabis event was cancelled and is no longer synchronized
- **THEN** the event is synchronized with ORIS again and takes the current ORIS values
- **AND** it is included in scheduled synchronization runs from then on
- **AND** its earlier synchronization history remains visible

#### Scenario: A finished event is not resumed by a scheduled run

- **WHEN** an imported event has finished and scheduled synchronization runs
- **THEN** the finished event stays out of those runs
