# Data Synchronization Specification

## ADDED Requirements

### Requirement: Records Are Kept In Step Automatically

Once an entity in Klabis is linked to its counterpart in an external system, the system SHALL keep the two in step on its own. The system SHALL determine which side was changed since they last agreed and SHALL update the other side accordingly. The system SHALL NOT write anything when neither side has changed.

#### Scenario: A change in the external system reaches Klabis

- **WHEN** the external system's copy of a linked entity changes and nobody has changed it in Klabis
- **THEN** the entity in Klabis is updated to match the external system
- **AND** the record shows that the last successful synchronisation ran in the inward direction

#### Scenario: Nothing changed on either side

- **WHEN** a linked entity is checked and neither side has changed since they last agreed
- **THEN** nothing is written on either side
- **AND** the record continues to show it is in step

#### Scenario: Both sides received the same change

- **WHEN** both sides of a linked entity have changed since they last agreed, but they now hold the same values
- **THEN** the record is treated as being in step again
- **AND** no decision is asked of anyone, because the two sides already agree

#### Scenario: First synchronisation after linking

- **WHEN** an entity is linked to an external counterpart for the first time
- **THEN** the first synchronisation takes the external system's values
- **AND** from that point on the system can tell which side subsequently changed

### Requirement: A Local Change Is Never Silently Overwritten

When someone changes a field in Klabis that the external system also owns, the system SHALL NOT discard that change on the next synchronisation. If the change cannot be sent onward — because the external system offers no way to change that entity — the record SHALL stop being synchronised and SHALL ask for a decision instead.

#### Scenario: A local change that cannot be sent onward stops for a decision

- **GIVEN** an entity linked to an external system that does not allow Klabis to change it
- **WHEN** a user changes one of the fields that the external system owns
- **AND** the entity is synchronised afterwards
- **THEN** the user's change is kept
- **AND** the record is marked as needing a decision, naming the fields that differ

#### Scenario: Synchronisation pauses while a decision is outstanding

- **WHEN** a record is waiting for a decision
- **THEN** neither side is written until the decision is made
- **AND** later synchronisation runs leave both sides untouched

#### Scenario: A local change that can be sent onward is sent

- **GIVEN** an entity linked to an external system that allows Klabis to change it
- **WHEN** a user changes one of the shared fields and the entity is synchronised afterwards
- **THEN** the external system is updated to match Klabis
- **AND** no decision is asked of anyone

### Requirement: Conflicting Changes Are Reported, Never Merged

When both sides changed to different values since they last agreed, the system SHALL NOT choose between them and SHALL NOT combine them. The system SHALL report which fields differ and, for each of them, which side moved away from the last agreed state.

#### Scenario: Both sides changed differently

- **WHEN** both sides of a linked entity changed since they last agreed and now hold different values
- **THEN** neither side is written
- **AND** the record is marked as needing a decision

#### Scenario: The user sees what differs and who moved

- **WHEN** a user with the synchronisation permission looks at a record that needs a decision
- **THEN** they see the values held by each side
- **AND** for each differing field they see whether Klabis changed it, the external system changed it, or both did

#### Scenario: A conflict that resolves itself

- **GIVEN** a record waiting for a decision
- **WHEN** the two sides come to hold the same values again — because a change was reverted, or because the user corrected the entity directly in the external system
- **THEN** the record stops asking for a decision on its own
- **AND** normal synchronisation resumes

### Requirement: Resolving A Conflict Takes Two Deliberate Steps

Because resolving a conflict can discard someone's work, the system SHALL require the user to first confirm they have seen the specific difference, and SHALL only then offer the choice of what to do about it. The offered choice SHALL be: take the external system's values, send the Klabis values onward, or record that the two sides are deliberately allowed to differ.

#### Scenario: The resolution choice is only offered after confirming the difference

- **WHEN** a user with the synchronisation permission views a record that needs a decision and has not yet confirmed seeing it
- **THEN** only the action to confirm the difference is offered
- **AND** the actions that resolve it are not offered yet

#### Scenario: Taking the external system's values

- **GIVEN** a user has confirmed seeing the difference
- **WHEN** they choose to take the external system's values
- **THEN** the entity in Klabis is updated to match the external system
- **AND** the record no longer needs a decision

#### Scenario: Accepting that the two sides differ

- **GIVEN** a user has confirmed seeing the difference
- **WHEN** they choose to accept that the two sides deliberately differ
- **THEN** nothing is written on either side
- **AND** both sides keep the values they currently hold
- **AND** the record no longer needs a decision

#### Scenario: An accepted difference is protected from later overwriting

- **GIVEN** a user has accepted that the two sides deliberately differ
- **WHEN** the external system changes afterwards
- **THEN** the accepted values in Klabis are not overwritten
- **AND** the record asks for a decision again, against the new external values

#### Scenario: The situation changed while the user was deciding

- **GIVEN** a user has confirmed seeing a difference
- **WHEN** either side changes before they choose how to resolve it
- **THEN** their choice is refused
- **AND** they are shown the new difference and asked to decide again

#### Scenario: A direction the external system does not allow is never offered

- **WHEN** a user resolves a difference on an entity the external system does not allow Klabis to change
- **THEN** sending the Klabis values onward is not offered as a choice
- **AND** taking the external system's values and accepting the difference remain available

### Requirement: Failed Synchronisation Is Retried, Then Stops And Waits

A synchronisation that fails SHALL be tried again later, with a growing delay between attempts. Once a record has failed as many times in a row as the configured limit allows, the system SHALL stop attempting it and SHALL wait for someone to look at it. A failure caused by the external system being unavailable SHALL NOT count towards that limit.

#### Scenario: A temporary failure is retried

- **WHEN** synchronising a record fails for a reason that may pass
- **THEN** the record is marked as failing and is tried again later
- **AND** each further failure lengthens the wait before the next attempt

#### Scenario: A failing record is not reported as being in step

- **WHEN** a user looks at a record whose most recent attempt failed
- **THEN** the record is shown as failing rather than as being in step
- **AND** the reason of the most recent failure is available

#### Scenario: Persistent failure stops the record

- **WHEN** a record has failed as many times in a row as the configured limit allows
- **THEN** it stops being attempted
- **AND** it is marked as needing manual attention

#### Scenario: The user restarts a stopped record

- **GIVEN** a record that stopped after repeated failures
- **WHEN** a user with the synchronisation permission restarts it
- **THEN** it is synchronised again from the next run onwards
- **AND** its earlier failures no longer count against the limit

#### Scenario: An outage of the external system does not strand records

- **WHEN** the external system is unavailable while records are being synchronised
- **THEN** the remaining records in that run are left untouched rather than each failing in turn
- **AND** failures caused by the outage do not bring any record closer to stopping

### Requirement: Synchronisation State Is Visible Per Entity

For every linked entity, the system SHALL show a user with the synchronisation permission how it stands: whether it is in step, failing, waiting for a decision or stopped; when it was last successfully synchronised and in which direction; and, where a decision is needed, what differs.

#### Scenario: The user reads the state of one entity

- **WHEN** a user with the synchronisation permission opens the synchronisation state of a linked entity
- **THEN** they see its current state
- **AND** they see when it was last successfully synchronised and in which direction

#### Scenario: The user triggers a synchronisation themselves

- **WHEN** a user with the synchronisation permission asks for a linked entity to be synchronised now
- **THEN** the entity is synchronised immediately rather than waiting for the next scheduled run
- **AND** the resulting state is shown to them

#### Scenario: Synchronisation actions require the permission

- **WHEN** a user without the synchronisation permission views a linked entity
- **THEN** no synchronisation actions are offered to them

#### Scenario: An entity that is not linked has no synchronisation state

- **WHEN** a user opens the synchronisation state of an entity that is not linked to any external system
- **THEN** they are told that no synchronisation exists for it

### Requirement: Finished Entities Stop Being Synchronised

When an entity reaches the end of its life in Klabis, the system SHALL stop synchronising it, and SHALL keep everything recorded about its past synchronisations.

#### Scenario: A finished entity is no longer synchronised

- **WHEN** a linked entity reaches the end of its life
- **THEN** it is no longer included in scheduled synchronisation runs
- **AND** its last successful synchronisation and its history remain visible

### Requirement: Every Synchronisation Attempt Is Recorded

The system SHALL keep a history of what it did for each linked entity: when each attempt ran, what triggered it, which direction it went, how it ended and why it failed. For actions a user performed themselves, the history SHALL also record who performed them.

#### Scenario: Automatic attempts are recorded

- **WHEN** a record is synchronised by a scheduled run or because the entity changed in Klabis
- **THEN** the attempt is added to that record's history with its outcome

#### Scenario: A user's decision records who made it

- **WHEN** a user confirms a difference, resolves it, or restarts a stopped record
- **THEN** the history records the action together with the user who performed it

#### Scenario: History is kept for a limited period

- **WHEN** entries in a record's history become older than the configured retention period
- **THEN** they are removed
- **AND** the record's own last successful synchronisation information remains visible
