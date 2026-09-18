## ADDED Requirements

### Requirement: An External Record Can Be Brought In And Kept In Step

The system SHALL allow a user to name a record in an external system and have it brought into Klabis in one action. The system SHALL create the corresponding entity in Klabis from the external record, link the two, and perform the first synchronisation immediately, so that the entity is in step and kept in step from the moment the action completes. The action SHALL be recorded in the entity's synchronisation history together with who performed it.

The system SHALL offer this action only for kinds of entity an external system can create in Klabis. Where it cannot, the action SHALL be refused rather than silently doing nothing.

#### Scenario: A user brings in an external record that Klabis does not have

- **WHEN** a user brings in a record from an external system that has no counterpart in Klabis yet
- **THEN** the corresponding entity is created in Klabis from the external record
- **AND** the entity is linked to the external record and shows as being in step
- **AND** the synchronisation history shows one attempt for it, recording who performed it

#### Scenario: The external system cannot be read

- **WHEN** a user brings in a record from an external system and the external system cannot be reached
- **THEN** no entity is created in Klabis
- **AND** the user is told the record could not be brought in

#### Scenario: Bringing in a kind of entity that cannot be created from outside

- **WHEN** a user brings in an external record for a kind of entity that no external system is allowed to create in Klabis
- **THEN** the action is refused
- **AND** nothing is created or linked

### Requirement: Bringing In A Record That Is Already Linked Synchronises It

When the external record a user brings in is already linked to an entity in Klabis, the system SHALL NOT create a second entity and SHALL NOT report the action as a failure. The system SHALL synchronise the existing pair instead, deciding the direction the same way it does for any other synchronisation, so that a change made in Klabis is never discarded by someone repeating the action.

If the pair is waiting for someone to decide about a difference between the two sides, or has stopped after repeated failures, the system SHALL refuse the action and SHALL point the user at the outstanding decision instead of synchronising.

#### Scenario: The same external record is brought in twice

- **WHEN** a user brings in an external record that is already linked to an entity in Klabis
- **THEN** no second entity is created
- **AND** the existing entity is synchronised with the external record
- **AND** the user is given the existing entity

#### Scenario: A change made in Klabis is not discarded by repeating the action

- **WHEN** someone has changed a field in Klabis that the external system also owns, and a user then brings in that external record again
- **THEN** the change made in Klabis is not overwritten
- **AND** the pair is treated exactly as any other synchronisation would treat it, asking for a decision if the two sides now differ

#### Scenario: The pair is waiting for a decision

- **WHEN** a user brings in an external record whose pair is waiting for someone to decide about a difference between the two sides
- **THEN** the action is refused
- **AND** the user is pointed at the outstanding decision

#### Scenario: The pair has stopped after repeated failures

- **WHEN** a user brings in an external record whose pair has stopped after repeated failures
- **THEN** the action is refused
- **AND** the user is told the pair must be restarted first

## MODIFIED Requirements

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

#### Scenario: An entity brought in from an external system is in step straight away

- **WHEN** a user brings in an external record and the entity is created in Klabis from it
- **THEN** the first synchronisation runs as part of the same action, without waiting for a scheduled run
- **AND** the entity shows as being in step once the action completes

### Requirement: Finished Entities Stop Being Synchronised

When an entity reaches the end of its life in Klabis, the system SHALL stop synchronising it, and SHALL keep everything recorded about its past synchronisations.

A pairing that has stopped being synchronised SHALL be brought back into service when a user deliberately brings in the same external record again. The system SHALL then start over from the external system's values rather than from what the two sides last agreed on before they stopped being synchronised, because either side may have moved in the meantime. The system SHALL NOT bring such a pairing back on its own.

#### Scenario: A finished entity is no longer synchronised

- **WHEN** a linked entity reaches the end of its life
- **THEN** it is no longer included in scheduled synchronisation runs
- **AND** its last successful synchronisation and its history remain visible

#### Scenario: A user brings back a pairing that had stopped

- **WHEN** a user brings in an external record whose pairing had stopped being synchronised
- **THEN** the pairing is returned to service and included in scheduled synchronisation runs again
- **AND** the synchronisation starts over from the external system's values
- **AND** everything recorded about its past synchronisations remains visible

#### Scenario: A stopped pairing is not brought back on its own

- **WHEN** scheduled synchronisation runs and a pairing has stopped being synchronised
- **THEN** it stays out of those runs
- **AND** only a user deliberately bringing in the same external record returns it to service
