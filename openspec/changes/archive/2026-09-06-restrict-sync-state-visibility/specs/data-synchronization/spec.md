## MODIFIED Requirements

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
