# data-synchronization Specification

## Purpose
TBD - created by archiving change add-bidirectional-sync-engine. Update Purpose after archive.

## Requirements

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

For every linked entity, the system SHALL show any signed-in user whether it is kept in step with an external system, which system that is, how it currently stands, and when it was last successfully synchronised.

Beyond that, for every linked entity the system SHALL show a user with the synchronisation permission how it stands in full: when it was last successfully synchronised and in which direction; how it is identified in the external system; when it will next be attempted and how many attempts have failed since it was last in step; where a decision is needed, what differs and on which side; and the data held on each side. A user without the synchronisation permission SHALL NOT be shown any of that detail.

#### Scenario: A signed-in user reads the headline state of one entity

- **WHEN** a signed-in user without the synchronisation permission opens the synchronisation state of a linked entity
- **THEN** they see which external system it is linked to and its current state
- **AND** they see when it was last successfully synchronised
- **AND** they are shown nothing further about the synchronisation

#### Scenario: The user reads the state of one entity

- **WHEN** a user with the synchronisation permission opens the synchronisation state of a linked entity
- **THEN** they see its current state
- **AND** they see when it was last successfully synchronised and in which direction

#### Scenario: A user without the permission is not shown the data held on either side

- **WHEN** a signed-in user without the synchronisation permission opens the synchronisation state of a linked entity that is waiting for a decision
- **THEN** they are not shown what differs between the two sides
- **AND** they are not shown the data held on either side

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

### Requirement: Reference Data Is Discovered And Brought In Automatically

For a kind of entity that is external reference data with no single natural record for a user to name and bring in one at a time, the system SHALL discover every such record in the external system on its own and bring each one in automatically, without waiting for a user action.

#### Scenario: A reference-data record is discovered and brought in on its own

- **WHEN** the external system holds a reference-data record that Klabis does not yet have
- **THEN** the system creates the corresponding entity in Klabis and links it to the external record the next time it looks
- **AND** it is kept in step from that point on the same way as any other linked entity, with no user having brought it in explicitly

### Requirement: Synchronisation Overview Lists Every Recorded Attempt

The system SHALL offer a user with the synchronisation permission a synchronisation overview page listing every recorded synchronisation attempt across all linked entities, newest first and in pages. Each attempt SHALL show when it ran, which entity it concerned, what triggered it, which direction it went, how it ended, why it failed if it did, and who performed it if a user did.

#### Scenario: Sync manager opens the synchronisation overview

- **WHEN** a user with the synchronisation permission opens "Synchronizace" from the main menu
- **THEN** they see a list of recorded synchronisation attempts across members, events, disciplines and registrations
- **AND** the most recent attempt is shown first

#### Scenario: Each attempt shows what happened

- **WHEN** a sync manager looks at an attempt in the list
- **THEN** they see when it ran, the entity it concerned, whether it was started by schedule, by a change in Klabis or by a user, the direction it went and its outcome

#### Scenario: A failed attempt shows why it failed

- **WHEN** a sync manager looks at an attempt that failed
- **THEN** the reason of the failure is shown with it

#### Scenario: An attempt performed by a user shows who performed it

- **WHEN** a sync manager looks at an attempt that a user performed themselves, such as synchronising now, confirming a difference, resolving it or restarting a stopped record
- **THEN** the user who performed it is shown with it

#### Scenario: Older attempts are reached page by page

- **WHEN** there are more attempts than fit on one page
- **THEN** the sync manager can move to further pages to see older attempts

#### Scenario: Attempts removed by retention are no longer listed

- **WHEN** attempts become older than the configured history retention period
- **THEN** they no longer appear in the overview

### Requirement: The Attempt List Can Be Narrowed Down

The system SHALL let a sync manager narrow the list of attempts by the kind of entity, by outcome and by the date range in which the attempts ran. Filters SHALL be combinable.

#### Scenario: Sync manager shows only failed attempts

- **WHEN** a sync manager filters the attempt list by the outcome "failed"
- **THEN** only attempts that ended in failure are listed

#### Scenario: Sync manager shows only attempts for members

- **WHEN** a sync manager filters the attempt list by the entity kind "members"
- **THEN** only attempts that concerned members are listed

#### Scenario: Sync manager shows attempts from a date range

- **WHEN** a sync manager sets a date range on the attempt list
- **THEN** only attempts that ran within that range are listed

#### Scenario: Filters combine

- **WHEN** a sync manager filters by the entity kind "events" and by the outcome "conflict"
- **THEN** only attempts that concerned events and ended in a conflict are listed

### Requirement: Attempts That Changed Nothing Are Hidden Unless Asked For

Most automatic attempts find both sides already in step and write nothing. The system SHALL leave such attempts out of every list of attempts by default and SHALL let a sync manager choose to show them. Attempts a user performed themselves SHALL always be listed.

#### Scenario: The attempt list shows only attempts that did something

- **WHEN** a sync manager opens the list of attempts
- **THEN** automatic attempts that succeeded without writing anything to either side are not listed
- **AND** attempts that wrote to one of the sides, found a difference, failed, or were performed by a user are listed

#### Scenario: Sync manager asks to see attempts that changed nothing

- **WHEN** a sync manager chooses to show attempts that changed nothing
- **THEN** those attempts are listed as well, in their place among the others

#### Scenario: The same applies to an entity's recent attempts

- **WHEN** a sync manager looks at the recent attempts of one entity in its synchronisation state
- **THEN** automatic attempts that changed nothing are left out by default
- **AND** the sync manager can choose to show them

### Requirement: Entities Needing Attention Are Listed Separately

The synchronisation overview SHALL offer a separate list of linked entities that currently need a person: those waiting for a decision about a difference between the two sides, and those that stopped after repeated failures. An entity SHALL leave this list as soon as it no longer needs a person.

#### Scenario: Sync manager sees what needs attention

- **WHEN** a sync manager opens the "Vyžaduje pozornost" view of the synchronisation overview
- **THEN** they see every linked entity that is waiting for a decision or has stopped after repeated failures
- **AND** for each one they see its current state and when it was last successfully synchronised

#### Scenario: An entity that is only being retried does not need attention

- **WHEN** a linked entity has failed but is still being retried automatically
- **THEN** it is not listed in the "Vyžaduje pozornost" view
- **AND** its failed attempts remain visible in the list of attempts

#### Scenario: A resolved entity leaves the list

- **GIVEN** an entity listed in the "Vyžaduje pozornost" view because it was waiting for a decision
- **WHEN** a sync manager resolves the difference
- **THEN** the entity is no longer listed in the "Vyžaduje pozornost" view

#### Scenario: Nothing needs attention

- **WHEN** no linked entity is waiting for a decision or has stopped
- **THEN** the "Vyžaduje pozornost" view tells the sync manager that nothing needs attention

### Requirement: Actions Act On The Entity As It Stands Now

From any attempt or entity listed in the synchronisation overview, the system SHALL let a sync manager open that entity's synchronisation state and act on it there. The actions offered SHALL be those the entity's current state allows. An earlier attempt SHALL never be replayed.

#### Scenario: Synchronising again from an earlier failed attempt

- **GIVEN** an attempt that failed for an entity which is now being retried
- **WHEN** a sync manager opens that attempt's entity from the overview and chooses to synchronise it now
- **THEN** the entity is synchronised immediately against both sides as they stand now
- **AND** the new attempt appears at the top of the attempt list

#### Scenario: Resolving a conflict from the overview

- **GIVEN** an entity waiting for a decision listed in the "Vyžaduje pozornost" view
- **WHEN** a sync manager opens it from the overview
- **THEN** they are offered to confirm the difference first and only then to resolve it, exactly as from the entity's own page

#### Scenario: An old conflict that has since been resolved offers no resolution

- **GIVEN** an attempt that ended in a conflict which has since been resolved
- **WHEN** a sync manager opens that attempt's entity from the overview
- **THEN** no action to confirm or resolve a difference is offered
- **AND** the actions offered are those the entity's current state allows

#### Scenario: Restarting a stopped entity from the overview

- **GIVEN** an entity that stopped after repeated failures listed in the "Vyžaduje pozornost" view
- **WHEN** a sync manager opens it from the overview and restarts it
- **THEN** it is synchronised again from the next run onwards
- **AND** it is no longer listed in the "Vyžaduje pozornost" view

#### Scenario: A finished entity offers no actions

- **WHEN** a sync manager opens from the overview an entity that has stopped being synchronised because it reached the end of its life
- **THEN** no synchronisation action is offered for it

### Requirement: Entities In The Overview Are Named Readably

The synchronisation overview SHALL name each entity the way users know it — a member by name, an event by name and date, a discipline by name, a registration by the member and the event — and SHALL let the sync manager go to the entity's own page. When the entity no longer exists in Klabis, the overview SHALL identify it by its identifier in the external system.

#### Scenario: A member is shown by name

- **WHEN** a sync manager looks at an attempt that concerned a member
- **THEN** the member is shown by their name and registration number

#### Scenario: A registration is shown by member and event

- **WHEN** a sync manager looks at an attempt that concerned a registration
- **THEN** the registration is shown by the name of the member and the name of the event

#### Scenario: Going to the entity's own page

- **WHEN** a sync manager follows the entity named in an attempt
- **THEN** the entity's own page opens, such as the member detail or the event detail

#### Scenario: An entity that no longer exists in Klabis

- **WHEN** a sync manager looks at an attempt for an entity that no longer exists in Klabis
- **THEN** the entity is identified by its identifier in the external system
- **AND** no way to open the entity's own page is offered

### Requirement: The Synchronisation Overview Requires The Synchronisation Permission

Only a user with the synchronisation permission SHALL be able to reach the synchronisation overview. Other users SHALL NOT be offered it and SHALL NOT be shown any attempt or entity from it.

#### Scenario: A user without the permission is not offered the overview

- **WHEN** a signed-in user without the synchronisation permission uses the application
- **THEN** no way to reach the synchronisation overview is offered to them

### Requirement: An Entity's Recent Attempts Are Shown In Its Synchronisation State

When a user with the synchronisation permission opens the synchronisation state of a linked entity, the system SHALL show that entity's most recent recorded attempts, newest first, with the same details as the synchronisation overview shows for an attempt. A user without the permission SHALL NOT be shown them.

#### Scenario: Sync manager sees a member's recent attempts

- **WHEN** a sync manager opens the synchronisation state of a member from the member detail page
- **THEN** they see that member's most recent synchronisation attempts, newest first
- **AND** for each attempt they see when it ran, what triggered it, the direction, the outcome, the failure reason and who performed it, where applicable

#### Scenario: Recent attempts are shown wherever the synchronisation state opens

- **WHEN** a sync manager opens the synchronisation state of an entity from the synchronisation overview
- **THEN** the same list of that entity's recent attempts is shown

#### Scenario: A user without the permission sees no attempts

- **WHEN** a signed-in user without the synchronisation permission opens the synchronisation state of a linked entity
- **THEN** no attempts are shown to them

#### Scenario: An entity with no recorded attempts

- **WHEN** a sync manager opens the synchronisation state of an entity whose attempts have all been removed by retention
- **THEN** they are told no attempts are recorded within the retention period
- **AND** the entity's last successful synchronisation remains visible
