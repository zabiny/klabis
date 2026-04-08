## ADDED Requirements

### Requirement: Parent and Child Roles Are Exclusive Within a Family Group

The system SHALL ensure that a member holds at most one role within any single family group. A member who is a parent of a family group SHALL NOT simultaneously be a child of the same family group, and vice versa.

#### Scenario: Admin cannot add existing parent as a child of the same family group

- **WHEN** user with MEMBERS:MANAGE permission attempts to add a member as a child of a family group
- **AND** the member is already a parent of that same family group
- **THEN** the system rejects the action
- **AND** displays an error indicating the member is already a parent of this family group

#### Scenario: Admin cannot add existing child as a parent of the same family group by creating a duplicate membership

- **WHEN** user with MEMBERS:MANAGE permission promotes an existing child to parent of the same family group
- **THEN** the system updates the member's role in place so they become the parent
- **AND** the member is not listed twice in the group

### Requirement: Add and Remove Child Members of a Family Group

The system SHALL allow users with MEMBERS:MANAGE permission to add and remove non-parent (child) members of an existing family group from the family group detail page.

#### Scenario: Admin adds a child to a family group

- **WHEN** user with MEMBERS:MANAGE permission adds a member as a child to a family group
- **AND** the member is not already in any family group
- **THEN** the member is added as a non-parent member of the family group

#### Scenario: Admin cannot add a child who already belongs to another family group

- **WHEN** user with MEMBERS:MANAGE permission attempts to add a member as a child to a family group
- **AND** the member already belongs to a different family group
- **THEN** the system rejects the action
- **AND** displays an error indicating the member is already in a family group

#### Scenario: Admin removes a child from a family group

- **WHEN** user with MEMBERS:MANAGE permission removes a child from a family group
- **THEN** the child is removed from the group entirely

#### Scenario: Child add and remove actions require MEMBERS:MANAGE

- **WHEN** a user without MEMBERS:MANAGE permission views a family group detail page
- **THEN** the system does NOT display add-child or remove-child actions

### Requirement: Manual Training Group Trainee Exclusivity

The system SHALL reject manual assignment of a member as a trainee to a training group when the member is already a trainee of another training group. This rule applies only to the manual "add member" action; the automatic age-based assignment path retains its existing move-on-conflict behavior. Members who act as trainers are exempt: a club member MAY serve as a trainer in multiple training groups simultaneously.

#### Scenario: User with GROUPS:TRAINING permission manually adds a member to a second training group

- **WHEN** user with GROUPS:TRAINING permission attempts to manually add a member to a training group
- **AND** the member is already a trainee of another training group
- **THEN** the system rejects the action
- **AND** displays an error indicating the member is already a trainee of another training group

#### Scenario: User with GROUPS:TRAINING permission manually adds a non-trainee member

- **WHEN** user with GROUPS:TRAINING permission manually adds a member to a training group
- **AND** the member is not a trainee of any other training group
- **THEN** the member is added as a trainee of the selected training group

#### Scenario: Member can be a trainer in multiple training groups

- **WHEN** user with GROUPS:TRAINING permission assigns a member as trainer to a training group
- **AND** the same member is already a trainer of another training group
- **THEN** the action succeeds and the member becomes a trainer of both groups

### Requirement: Owner Promotion in Invitation-Based Groups Requires Existing Membership

For any group type that uses an invitation-based membership flow (today: free groups), the system SHALL allow owner promotion only for members who are already current members of the same group. Attempting to promote a non-member directly to owner SHALL be rejected with an error.

#### Scenario: Owner promotes an existing member to co-owner

- **WHEN** a free group owner promotes a current member of the group to co-owner
- **THEN** the member receives owner privileges for the group
- **AND** the member's membership is unchanged

#### Scenario: Owner attempts to promote a non-member to owner

- **WHEN** a free group owner attempts to promote a user who is not a current member of the group to owner
- **THEN** the system rejects the action
- **AND** displays an error indicating that only existing members can be promoted to owner
- **AND** the candidate is NOT added to the group as a side effect

### Requirement: Member-Picker Dialogs Hide Existing Members

The system SHALL hide members who are already in the target group from the candidate list in every dialog that selects a member to add to a group, regardless of group type (family, training, or free). This includes dialogs for adding owners, parents, children, trainees, and trainers.

#### Scenario: Family group "add member" dialog excludes current members and parents

- **WHEN** user with MEMBERS:MANAGE permission opens the "add member" dialog on a family group detail page
- **THEN** the member picker does NOT list members who are already parents or children of that family group

#### Scenario: Training group "add trainee" dialog excludes current trainees

- **WHEN** user with GROUPS:TRAINING permission opens the "add member" dialog on a training group detail page
- **THEN** the member picker does NOT list members who are already trainees of that training group

#### Scenario: Free group "promote to owner" dialog excludes current owners

- **WHEN** a free group owner opens the "promote to owner" dialog
- **THEN** the picker does NOT list members who are already owners of that free group
- **AND** the picker lists only current members of the group (consistent with the owner-promotion rule)

## MODIFIED Requirements

### Requirement: Create Family Group

The system SHALL allow users with MEMBERS:MANAGE permission to create family groups from the members list page. A family group links one designated parent with their children. Exactly one parent SHALL be designated at creation time. No additional children are added during creation; children can be added afterwards from the family group detail page. The creating user does not automatically become a parent.

#### Scenario: User creates a family group with a single parent

- **WHEN** user with MEMBERS:MANAGE permission initiates family group creation
- **AND** fills in the group name and selects exactly one member as the parent
- **THEN** the system creates the family group with the selected member as the sole parent
- **AND** the parent is automatically included as a member of the group
- **AND** the system opens the family group detail page so further children can be added

#### Scenario: User attempts to create a family group without designating a parent

- **WHEN** user with MEMBERS:MANAGE permission attempts to create a family group without selecting a parent
- **THEN** the system rejects the creation
- **AND** displays an error indicating a parent is required

#### Scenario: Designated parent is already in another family group

- **WHEN** user attempts to designate as parent a member who already belongs to a family group
- **THEN** the system rejects the creation
- **AND** displays an error indicating the member is already in a family group

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Trainers can be managed by users with GROUPS:TRAINING permission, and trainers need not be current members of the training group. For family groups, owners are called "parents". Family group management (adding/removing parents and children) is exclusively controlled by users with MEMBERS:MANAGE permission — parents themselves have no management capabilities over the group. For free groups, owners manage other owners directly, but only current group members may be promoted to owner.

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
- **AND** the system does NOT display actions for adding or removing children

#### Scenario: Owner adds an existing member as co-owner of a free group

- **WHEN** a free group owner promotes a current member of the group to co-owner
- **THEN** the member receives owner privileges for the group

#### Scenario: Owner attempts to remove the last owner from a free group

- **WHEN** the sole remaining owner of a free group attempts to remove themselves as owner
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first
