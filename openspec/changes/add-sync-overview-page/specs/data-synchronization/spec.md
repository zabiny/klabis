# Spec Delta

## ADDED Requirements

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
