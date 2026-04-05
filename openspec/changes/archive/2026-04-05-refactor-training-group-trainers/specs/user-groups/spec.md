## MODIFIED Requirements

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Owners (trainers) can be managed by users with the appropriate permission for the group type. For free and family groups, owners manage other owners directly.

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

#### Scenario: Owner adds another owner to a free or family group

- **WHEN** group owner adds another member as co-owner to a free or family group
- **THEN** the new member receives owner privileges for the group

#### Scenario: Owner attempts to remove the last owner from a free or family group

- **WHEN** the sole remaining owner of a free or family group attempts to remove themselves as owner
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

### Requirement: Group Editing

The system SHALL allow editing of group properties. For training groups, a single edit operation updates name, age range, and trainers — all fields are optional (only provided fields are changed). For family and free groups, only the name can be edited by group owners.

#### Scenario: User with GROUPS:TRAINING permission edits training group name

- **WHEN** user with GROUPS:TRAINING permission opens the training group edit form
- **AND** changes only the group name
- **THEN** the system updates the name
- **AND** the age range and trainers remain unchanged

#### Scenario: User with GROUPS:TRAINING permission edits training group age range

- **WHEN** user with GROUPS:TRAINING permission changes the age range of a training group
- **AND** the new range does not overlap with other training groups
- **THEN** the system updates the age range
- **AND** members who no longer match the new range are reassigned during the next automatic age-based reassignment run

#### Scenario: User with GROUPS:TRAINING permission edits training group with overlapping range

- **WHEN** user with GROUPS:TRAINING permission changes the age range to one that overlaps another training group
- **THEN** the system rejects the entire change with an error indicating the conflict

#### Scenario: User with GROUPS:TRAINING permission replaces trainers

- **WHEN** user with GROUPS:TRAINING permission edits a training group and provides a new trainer list
- **AND** the trainer list contains at least one member
- **THEN** the system replaces the entire trainer list with the provided list

#### Scenario: User with GROUPS:TRAINING permission provides empty trainer list

- **WHEN** user with GROUPS:TRAINING permission edits a training group and provides an empty trainer list
- **THEN** the system rejects the change with an error requiring at least one trainer

#### Scenario: User with GROUPS:TRAINING permission edits multiple fields atomically

- **WHEN** user with GROUPS:TRAINING permission changes both name and age range in a single edit
- **AND** the age range validation fails
- **THEN** the system rejects the entire change including the name update

#### Scenario: Owner edits free or family group name

- **WHEN** group owner changes the name of a free or family group
- **THEN** the system updates the name immediately

### Requirement: Group Deletion

The system SHALL allow group deletion. Training group deletion requires GROUPS:TRAINING permission. Family group deletion requires MEMBERS:MANAGE permission. Free group deletion requires group ownership.

#### Scenario: User with GROUPS:TRAINING permission deletes a training group

- **WHEN** user with GROUPS:TRAINING permission confirms training group deletion
- **THEN** the system removes the group
- **AND** all members are unassigned from the group

#### Scenario: Owner deletes a free group

- **WHEN** free group owner confirms group deletion
- **THEN** the system removes the group and all memberships
- **AND** pending invitations are cancelled

#### Scenario: Authorized user deletes a family group

- **WHEN** user with MEMBERS:MANAGE permission confirms family group deletion
- **THEN** the system removes the group and all memberships

### Requirement: Create Training Group

The system SHALL allow users with GROUPS:TRAINING permission to create training groups on a dedicated training groups page. A training group requires a name, a trainer (any club member), and an age range defining which members are eligible. The creating user is not automatically assigned as trainer.

#### Scenario: User creates a training group with valid data

- **WHEN** user with GROUPS:TRAINING permission navigates to the training groups page
- **AND** fills in group name, selects a trainer, and sets age range (min age, max age)
- **AND** the age range does not overlap with any existing training group's age range
- **THEN** the system creates the training group with the selected member as trainer
- **AND** the system automatically assigns all active members whose age falls within the range

#### Scenario: User creates a training group with overlapping age range

- **WHEN** user attempts to create a training group with an age range that overlaps an existing training group
- **THEN** the system rejects the creation
- **AND** displays an error indicating which existing group has a conflicting age range

#### Scenario: User without permission cannot access training groups page

- **WHEN** user without GROUPS:TRAINING permission attempts to access the training groups page
- **THEN** the system denies access

### Requirement: Training Group Member Management

The system SHALL allow users with GROUPS:TRAINING permission to view and manage training group members. Members are primarily managed through age-based assignment but can also be manually added or removed. A trainer cannot be removed from the group members.

#### Scenario: User with GROUPS:TRAINING permission views training group member list

- **WHEN** user with GROUPS:TRAINING permission navigates to the training group detail on the training groups page
- **THEN** the system displays all current members with their names and join dates

#### Scenario: User with GROUPS:TRAINING permission removes a member from a training group

- **WHEN** user with GROUPS:TRAINING permission removes a member who is not a trainer
- **THEN** the member is removed from the training group
- **AND** the member is not automatically reassigned to another training group

#### Scenario: User with GROUPS:TRAINING permission attempts to remove a trainer from members

- **WHEN** user with GROUPS:TRAINING permission attempts to remove a member who is a trainer
- **THEN** the system rejects the action
- **AND** the remove action is not available in the UI for trainers

### Requirement: Training Groups Page

The system SHALL provide a dedicated training groups page accessible only to users with GROUPS:TRAINING permission. The page lists all training groups with their age ranges, trainers, and member counts.

#### Scenario: Authorized user views training groups page

- **WHEN** user with GROUPS:TRAINING permission navigates to the training groups page
- **THEN** the system displays all training groups
- **AND** each group shows its name, age range, trainer(s), and member count

#### Scenario: Unauthorized user cannot access training groups page

- **WHEN** user without GROUPS:TRAINING permission attempts to access the training groups page
- **THEN** the system denies access

### Requirement: Training Group Info on Member Profile

The system SHALL display the member's training group information on their profile page. Every member can see the name of their training group and the name and contact information of the group trainer(s).

#### Scenario: Member views their training group on profile

- **WHEN** member who belongs to a training group views their profile page
- **THEN** the system displays the training group name
- **AND** displays the name and contact information of the training group trainer(s)

#### Scenario: Member without training group views profile

- **WHEN** member who does not belong to any training group views their profile page
- **THEN** the training group section is not displayed or shows "not assigned"

### Requirement: Training Group Membership Assignment

The system SHALL assign members to training groups based on their age. Each member can belong to at most one training group. When a member is assigned to a training group, the system records this as a domain event for downstream processing.

#### Scenario: New member is automatically assigned to a training group

- **WHEN** a new member is registered whose age falls within a training group's age range
- **THEN** the system automatically adds the member to that training group
- **AND** a member-assigned-to-training-group event is recorded

#### Scenario: Members are auto-assigned when training group is created

- **WHEN** a training group is created with an age range
- **THEN** the system automatically assigns all active members whose age falls within the range
- **AND** a member-assigned-to-training-group event is recorded for each assigned member

#### Scenario: Member is assigned to a second training group

- **WHEN** the system attempts to assign a member who already belongs to a training group to another training group
- **THEN** the system displays a warning informing that the member will be removed from the old training group
- **AND** upon confirmation, the member is moved to the new training group
