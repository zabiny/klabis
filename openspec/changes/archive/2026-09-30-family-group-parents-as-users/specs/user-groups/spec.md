## MODIFIED Requirements

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Trainers can be managed by users with GROUPS:TRAINING permission, and trainers need not be current members of the training group. For family groups, owners are called "parents"; a parent MAY be any user of the system and need not be a club member. Family group management (adding/removing parents and children) is exclusively controlled by users with MEMBERS:MANAGE permission — parents themselves have no management capabilities over the group. For free groups, owners manage other owners directly, but only current group members may be promoted to owner.

#### Scenario: User with GROUPS:TRAINING permission adds a trainer to a training group

- **WHEN** user with GROUPS:TRAINING permission adds another member as trainer to a training group
- **THEN** the member is added as trainer of the training group

#### Scenario: User with GROUPS:TRAINING permission removes a trainer from a training group

- **WHEN** user with GROUPS:TRAINING permission removes a trainer from a training group
- **AND** the training group has more than one trainer
- **THEN** the trainer is removed from the trainer list

#### Scenario: Attempt to remove the last trainer from a training group

- **WHEN** user attempts to remove the sole remaining trainer from a training group
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

#### Scenario: Admin adds a parent to a family group

- **WHEN** user with MEMBERS:MANAGE permission adds a user as parent to a family group
- **THEN** the new user receives parent privileges for the group
- **AND** the new user is automatically added as a member of the group

#### Scenario: Admin adds a parent who is not a club member

- **WHEN** user with MEMBERS:MANAGE permission adds as parent a user who has no member profile
- **THEN** the user receives parent privileges for the group
- **AND** the user is listed among the parents of the group

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
- **AND** the system does NOT display actions for adding or removing children

#### Scenario: Owner adds an existing member as co-owner of a free group

- **WHEN** a free group owner promotes a current member of the group to co-owner
- **THEN** the member receives owner privileges for the group

#### Scenario: Owner attempts to remove the last owner from a free group

- **WHEN** the sole remaining owner of a free group attempts to remove themselves as owner
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

### Requirement: Create Family Group

The system SHALL allow users with MEMBERS:MANAGE permission to create family groups from the members list page. A family group links one designated parent with their children. The designated parent MAY be any user of the system and need not be a club member. Exactly one parent SHALL be designated at creation time. No additional children are added during creation; children can be added afterwards from the family group detail page. The creating user does not automatically become a parent.

#### Scenario: User creates a family group with a single parent

- **WHEN** user with MEMBERS:MANAGE permission initiates family group creation
- **AND** fills in the group name and selects exactly one user as the parent
- **THEN** the system creates the family group with the selected user as the sole parent
- **AND** the parent is automatically included as a member of the group
- **AND** the system opens the family group detail page so further children can be added

#### Scenario: User creates a family group with a parent who is not a club member

- **WHEN** user with MEMBERS:MANAGE permission creates a family group designating a user without member profile as the parent
- **THEN** the system creates the family group with that user as the sole parent

#### Scenario: User attempts to create a family group without designating a parent

- **WHEN** user with MEMBERS:MANAGE permission attempts to create a family group without selecting a parent
- **THEN** the system rejects the creation
- **AND** displays an error indicating a parent is required

#### Scenario: Designated parent is already in another family group

- **WHEN** user attempts to designate as parent a user who already belongs to a family group
- **THEN** the system rejects the creation
- **AND** displays an error indicating the user is already in a family group

### Requirement: Exclusive Family Group Membership

The system SHALL enforce that each user (parent) and each member (child) belongs to at most one family group at any time.

#### Scenario: User attempts to add member to a second family group

- **WHEN** user tries to add a member as child or a user as parent who already has a family group
- **THEN** the system rejects the action with an error indicating existing family group membership

### Requirement: Parent and Child Roles Are Exclusive Within a Family Group

The system SHALL ensure that a person holds at most one role within any single family group. A parent of a family group SHALL NOT simultaneously be a child of the same family group, and vice versa.

#### Scenario: Admin cannot add existing parent as a child of the same family group

- **WHEN** user with MEMBERS:MANAGE permission attempts to add a member as a child of a family group
- **AND** the member is already a parent of that same family group
- **THEN** the system rejects the action
- **AND** displays an error indicating the member is already a parent of this family group

#### Scenario: Admin cannot add existing child as a parent of the same family group by creating a duplicate membership

- **WHEN** user with MEMBERS:MANAGE permission promotes an existing child to parent of the same family group
- **THEN** the system updates the member's role in place so they become the parent
- **AND** the member is not listed twice in the group

### Requirement: Family Group Detail Access for Members

The system SHALL allow any member of a family group, including parents without a member profile, to view the detail page of that family group.

#### Scenario: Family group member views group detail

- **WHEN** a member who belongs to a family group clicks the group navigation button on their profile page
- **THEN** the system displays the family group detail
- **AND** does NOT return an access denied error

#### Scenario: Parent without member profile views group detail

- **WHEN** a parent of a family group who has no member profile requests the family group detail
- **THEN** the system displays the family group detail
- **AND** does NOT return an access denied error

#### Scenario: User who is not a member of a family group cannot access its detail

- **WHEN** a user who is not a member, parent, or MEMBERS:MANAGE permission holder attempts to access a family group detail
- **THEN** the system denies access

#### Scenario: Parents are listed without a link to a member profile

- **WHEN** the family group detail is displayed
- **THEN** each parent is listed by user identifier without a link to a member profile
