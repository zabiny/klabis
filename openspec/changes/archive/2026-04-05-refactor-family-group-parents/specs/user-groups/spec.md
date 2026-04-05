## MODIFIED Requirements

### Requirement: Create Family Group

The system SHALL allow users with MEMBERS:MANAGE permission to create family groups from the members list page. A family group links parents with their children within the club. At least one parent SHALL be designated during creation. The creating user does not automatically become a parent.

#### Scenario: User creates a family group with parents and members

- **WHEN** user with MEMBERS:MANAGE permission selects members on the members list page
- **AND** initiates family group creation
- **AND** fills in group name, designates at least one parent, and optionally selects additional members
- **THEN** the system creates the family group with the designated parents and selected members
- **AND** parents are automatically included as members of the group

#### Scenario: User creates a family group without designating a parent

- **WHEN** user with MEMBERS:MANAGE permission attempts to create a family group without designating any parent
- **THEN** the system rejects the creation
- **AND** displays an error indicating at least one parent is required

#### Scenario: Member is already in another family group

- **WHEN** user attempts to add a member who already belongs to a family group
- **THEN** the system rejects the addition
- **AND** displays an error indicating the member is already in a family group

#### Scenario: Designated parent is already in another family group

- **WHEN** user attempts to designate a parent who already belongs to a family group
- **THEN** the system rejects the creation
- **AND** displays an error indicating the member is already in a family group

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For family groups, owners are called "parents". Family group management (adding/removing parents and members) is exclusively controlled by users with MEMBERS:MANAGE permission — parents themselves have no management capabilities over the group.

#### Scenario: Admin adds a parent to a family group

- **WHEN** user with MEMBERS:MANAGE permission adds a member as parent to a family group
- **THEN** the new member receives parent privileges for the group
- **AND** the new member is automatically added as a member of the group

#### Scenario: Admin removes a parent from a family group

- **WHEN** user with MEMBERS:MANAGE permission removes a parent from a family group
- **THEN** the removed parent loses parent privileges
- **AND** the removed parent is removed from the group entirely

#### Scenario: Admin attempts to remove the last parent from a family group

- **WHEN** user with MEMBERS:MANAGE permission attempts to remove the sole remaining parent
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

#### Scenario: Parent cannot manage family group

- **WHEN** a family group parent who does not have MEMBERS:MANAGE permission views the family group
- **THEN** the system does NOT display actions for adding or removing parents
- **AND** the system does NOT display actions for adding or removing members

#### Scenario: Owner adds another owner to a non-family group

- **WHEN** group owner of a training or free group adds another member as co-owner
- **THEN** the new member receives owner privileges for the group

#### Scenario: Owner attempts to remove the last owner

- **WHEN** the sole remaining owner of any group attempts to remove themselves as owner
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first
