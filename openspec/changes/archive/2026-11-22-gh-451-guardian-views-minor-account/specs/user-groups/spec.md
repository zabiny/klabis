## MODIFIED Requirements

### Requirement: Free Group Delegated Permissions Are Chosen At Creation

The founder of a free group SHALL choose the delegated permissions when creating the group. The create form SHALL offer "Úprava údajů člena" with nothing selected by default. "Zobrazení financí člena" is delegated only by legal guardian groups and SHALL NOT be offered for free groups. The delegated permissions of a free group SHALL NOT be changeable after creation.

#### Scenario: Founder creates a free group without delegation

- **WHEN** a member fills in the group name in the "Create group" dialog, leaves "Úprava údajů člena" unchecked and submits
- **THEN** the free group is created
- **AND** its owners gain no permissions over its members

#### Scenario: Founder creates a free group delegating profile editing

- **WHEN** a member fills in the group name, checks "Úprava údajů člena" and submits
- **THEN** the free group is created
- **AND** its owners will hold "Úprava údajů člena" over every member who joins

#### Scenario: Delegated permissions cannot be changed later

- **WHEN** an owner opens the edit form of an existing free group
- **THEN** the form allows changing only the name
- **AND** the delegated permissions are shown but cannot be changed

#### Scenario: Administrator permissions are not offered for delegation

- **WHEN** a member opens the "Create group" dialog
- **THEN** no administrator permission (for example "Správa členů") is offered for delegation

#### Scenario: Viewing finances is not offered for free groups

- **WHEN** a member opens the "Create group" dialog
- **THEN** "Zobrazení financí člena" is not offered for delegation
