# Spec Delta

## ADDED Requirements

### Requirement: Groups Delegate Permissions To Their Owners

A group SHALL be able to define a set of delegated permissions that every owner of the group holds over every member of the group. Members of the group SHALL NOT gain these permissions. Only permissions that concern individual members can be delegated; administrator permissions can never be delegated. Which permissions a group delegates depends on its type.

#### Scenario: Owner holds delegated permission over a member

- **GIVEN** a group delegates "Úprava údajů člena"
- **AND** member M belongs to the group
- **WHEN** an owner of the group opens M's detail page
- **THEN** the owner can edit M's data

#### Scenario: Members do not gain delegated permissions

- **GIVEN** a group delegates "Úprava údajů člena"
- **AND** members M and N belong to the group
- **WHEN** M opens N's detail page
- **THEN** M cannot edit N's data

#### Scenario: Owner loses the permission when the member leaves

- **GIVEN** an owner holds a delegated permission over member M through a group
- **WHEN** M leaves the group or is removed from it
- **THEN** the owner no longer holds that permission over M, unless another relationship grants it

#### Scenario: Former owner loses all delegated permissions

- **GIVEN** an owner holds delegated permissions over the members of a group
- **WHEN** the person stops being an owner of that group
- **THEN** they no longer hold any permission over the group's members through that group

#### Scenario: Permissions from several groups add up

- **GIVEN** a user owns two groups that both contain member M
- **WHEN** the user opens M's detail page
- **THEN** the user holds the union of the permissions both groups delegate

### Requirement: Free Group Delegated Permissions Are Chosen At Creation

The founder of a free group SHALL choose the delegated permissions when creating the group. The create form SHALL offer every delegatable permission (today only "Úprava údajů člena") with none selected by default. The delegated permissions of a free group SHALL NOT be changeable after creation.

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

### Requirement: Delegated Permissions Are Visible Before And After Joining

The system SHALL show which permissions the owners of a free group hold over its members: to an invited member before they decide on the invitation, and to everybody who can see the group detail. The text SHALL make clear that the permissions are held by all current and future owners of the group.

#### Scenario: Invitation shows delegated permissions

- **GIVEN** a free group delegates "Úprava údajů člena"
- **WHEN** an invited member views the invitation in their pending invitations list
- **THEN** the invitation states that by accepting, the owners of the group (current and future) will be able to edit the member's data

#### Scenario: Invitation of a group without delegation

- **GIVEN** a free group delegates no permissions
- **WHEN** an invited member views the invitation
- **THEN** the invitation states that the owners gain no permissions over the member

#### Scenario: Group detail shows delegated permissions

- **WHEN** an owner or a member views the free group detail page
- **THEN** the page shows the permissions the owners hold over the members

### Requirement: Training Groups Delegate No Permissions

The system SHALL NOT delegate any permission through a training group. Trainers gain no permissions over trainees by being their trainers, and this cannot be configured.

#### Scenario: Trainer cannot edit a trainee's data

- **GIVEN** a trainer of a training group without MEMBERS:MANAGE
- **WHEN** the trainer opens the detail page of a trainee of that group
- **THEN** the "Upravit profil" action is not available

#### Scenario: Training group forms offer no delegation

- **WHEN** a user with GROUPS:TRAINING permission creates or edits a training group
- **THEN** the form offers no choice of delegated permissions
