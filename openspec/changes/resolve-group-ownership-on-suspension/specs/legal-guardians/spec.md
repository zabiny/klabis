## ADDED Requirements

### Requirement: Suspended Member Does Not Remain A Legal Guardian

The system SHALL NOT keep a suspended member as a legal guardian of a group that has other guardians: when a member is suspended, the member is removed from the guardians of every such group, and the group name and grouping follow the remaining guardians.

#### Scenario: Suspended guardian is removed from a group with another guardian

- **GIVEN** a group with guardians "Jan Novák" and "Eva Svobodová" and one child
- **WHEN** an admin suspends "Jan Novák"
- **THEN** the child lists only "Eva Svobodová" as legal guardian
- **AND** the group is named "Svobodová"

#### Scenario: Group merges with an existing group after the guardian is removed

- **GIVEN** a group A with guardians "Jan Novák" and "Eva Svobodová" and a group B whose only guardian is "Eva Svobodová"
- **WHEN** an admin suspends "Jan Novák"
- **THEN** the minors of group A join group B

#### Scenario: Resuming a guardian does not restore the guardian role

- **GIVEN** a suspended member was removed from the guardians of a group
- **WHEN** an admin resumes the membership
- **THEN** the member is not a legal guardian of that group until an admin sets the guardians again

#### Scenario: Suspended members are never offered as guardians

- **WHEN** an admin opens the guardian picker
- **THEN** suspended members are not offered

#### Scenario: Group whose only guardian is suspended is not changed

- **GIVEN** a group whose only guardian is "Jan Novák", suspended together with the group's minors
- **THEN** the group keeps "Jan Novák" as guardian
