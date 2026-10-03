## ADDED Requirements

### Requirement: Delegated Permissions Of A Group

Every group SHALL carry a fixed set of delegated permissions, which gives its owners those permissions over the group's members (see relationship-authorization). The set is defined when the group is created and SHALL NOT be changed afterwards. Training groups and legal guardian groups use predefined sets that cannot be changed through the application. A group created before this requirement existed has an empty set.

#### Scenario: Member creates a free group with delegated permissions

- **GIVEN** an authenticated member is on the group management page
- **WHEN** the member opens the "Create group" dialog, enters a name, selects the permission "Edit member details" to delegate, and submits
- **THEN** the free group is created with that permission delegated to its owners
- **AND** the permission is shown on the group detail

#### Scenario: Create dialog offers every delegable permission

- **WHEN** a member opens the "Create group" dialog
- **THEN** the dialog offers every permission that groups can delegate over their members, currently "Edit member details"

#### Scenario: Member creates a free group without delegated permissions

- **WHEN** the member creates a free group without selecting any permission
- **THEN** the group delegates no permissions

#### Scenario: Delegated permissions cannot be changed afterwards

- **GIVEN** an existing free group with delegated permissions
- **WHEN** the owner opens the group edit form
- **THEN** the form offers only the group name and does not allow changing the delegated permissions

#### Scenario: Only delegable permissions can be chosen

- **WHEN** a request to create a free group names a permission that cannot be delegated
- **THEN** the system rejects the request with a validation error
- **AND** no group is created

### Requirement: Invitation Shows Delegated Permissions

An invitation to a free group SHALL show the invited member which permissions the group's owners hold over the group's members, so that the member can decide with that knowledge.

#### Scenario: Invited member sees delegated permissions

- **GIVEN** a free group that delegates "Edit member details" to its owners
- **WHEN** an invited member opens their pending invitations
- **THEN** the invitation states that the group's owners will be able to edit the member's details

#### Scenario: Invitation to a group without delegated permissions

- **GIVEN** a free group that delegates no permissions
- **WHEN** an invited member opens their pending invitations
- **THEN** the invitation shows no delegated permissions
