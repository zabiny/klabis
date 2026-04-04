# User Groups Specification

## Purpose

Defines the behavior of user groups in the system. Supports three group types: Training Groups (age-based assignment), Family Groups (parent-child linking), and Free Groups (user-managed invitation-based groups).

## Requirements

### Requirement: Group Types

The system SHALL support three types of user groups: Training Group, Family Group, and Free Group. Each group type has distinct behavior for membership management, creation rules, and lifecycle. Training and Family groups are internal groups supporting application features. Free groups are user-managed groups.

#### Scenario: Each group type has distinct creation access

- **WHEN** a training group is created
- **THEN** it requires GROUPS:TRAINING permission and is managed on a dedicated training groups page
- **WHEN** a family group is created
- **THEN** it requires MEMBERS:MANAGE permission and is created from the members list page
- **WHEN** a free group is created
- **THEN** any authenticated member can create it from the group management page

#### Scenario: Each group type has distinct membership rules

- **WHEN** a member is added to a training group
- **THEN** membership is based on age range and limited to one training group per member
- **WHEN** a member is added to a family group
- **THEN** membership is limited to one family group per member
- **WHEN** a member is invited to a free group
- **THEN** the member can belong to multiple free groups simultaneously

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Trainers can be managed by users with GROUPS:TRAINING permission. For family groups, owners are called "parents". Family group management (adding/removing parents and members) is exclusively controlled by users with MEMBERS:MANAGE permission — parents themselves have no management capabilities over the group. For free groups, owners manage other owners directly.

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
- **AND** the system does NOT display actions for adding or removing members

#### Scenario: Owner adds another owner to a free group

- **WHEN** free group owner adds another member as co-owner
- **THEN** the new member receives owner privileges for the group

#### Scenario: Owner attempts to remove the last owner from a free group

- **WHEN** the sole remaining owner of a free group attempts to remove themselves as owner
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

### Requirement: Warning on Last Owner Deactivation

The system SHALL warn when deactivating a member who is the last owner of any group. The behavior depends on the group type.

#### Scenario: Deactivating last trainer of a training group

- **WHEN** admin initiates suspension of a member who is the sole trainer of a training group
- **THEN** the system displays a warning that the member is the last trainer
- **AND** requires the admin to designate a successor trainer before proceeding

#### Scenario: Deactivating last owner of a family or free group

- **WHEN** admin initiates suspension of a member who is the sole owner of a family or free group
- **THEN** the system displays a warning with options to either designate a successor or dissolve the group
- **AND** the admin must choose one option before the suspension proceeds

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

### Requirement: Create Free Group

The system SHALL allow any authenticated member to create free groups from the group management page. A free group is a user-defined collection of members managed entirely by its owner(s) through an invitation system.

#### Scenario: Member creates a free group

- **WHEN** authenticated member fills in group name on the group management page
- **THEN** the system creates the free group with the creating member as owner
- **AND** the group starts with no additional members

### Requirement: Free Group Invitation System

The system SHALL support an invitation-based membership flow for free groups. Group owners can invite members, and invited members can accept or reject invitations. Direct addition of members to a free group without an invitation is NOT allowed — membership can only be established through the invitation flow.

#### Scenario: Owner invites a member to a free group

- **WHEN** group owner selects a member to invite
- **THEN** the system creates a pending invitation for that member
- **AND** the invited member can see the invitation in their group invitations list

#### Scenario: Member accepts an invitation

- **WHEN** invited member accepts a pending invitation
- **THEN** the member is added to the free group
- **AND** the invitation status changes to accepted

#### Scenario: Member rejects an invitation

- **WHEN** invited member rejects a pending invitation
- **THEN** the member is NOT added to the free group
- **AND** the invitation status changes to rejected

#### Scenario: Owner cannot invite existing member or owner

- **WHEN** group owner attempts to invite a member who is already a member or owner of the group
- **THEN** the system rejects the invitation
- **AND** displays an error indicating the member is already in the group

#### Scenario: Owner cannot invite member with pending invitation

- **WHEN** group owner attempts to invite a member who already has a pending invitation to the same group
- **THEN** the system rejects the invitation
- **AND** displays an error indicating a pending invitation already exists

#### Scenario: Member can be re-invited after rejection

- **WHEN** a member has previously rejected an invitation to a free group
- **AND** the group owner sends a new invitation to that member
- **THEN** the system creates a new pending invitation for the member

#### Scenario: Only group owner can invite members

- **WHEN** a non-owner group member attempts to invite another member to the free group
- **THEN** the system rejects the action
- **AND** the invitation is not created

#### Scenario: Member has multiple free group memberships

- **WHEN** a member accepts invitations to multiple free groups
- **THEN** the system allows membership in all accepted groups without restriction

#### Scenario: Owner cannot add member directly without invitation

- **WHEN** group owner attempts to add a member directly to a free group without sending an invitation
- **THEN** the system rejects the action with an error
- **AND** the member is NOT added to the group

#### Scenario: Direct add action is not available in the UI for free groups

- **WHEN** group owner views the free group detail page
- **THEN** the system does NOT display a "add member directly" action
- **AND** only the "invite member" action is available for adding new members

### Requirement: Free Group Member Management

The system SHALL allow free group owners to view and manage group members through the group detail page.

#### Scenario: Owner views free group member list

- **WHEN** free group owner navigates to the group detail page
- **THEN** the system displays all current members with their names and join dates

#### Scenario: Owner removes a member from a free group

- **WHEN** free group owner removes a member
- **THEN** the member is removed from the group immediately

### Requirement: Free Group List View

The system SHALL provide a group management page listing free groups where the current user is a member or has a pending invitation. Groups where the user has no membership and no pending invitation are not visible.

#### Scenario: User views group management page

- **WHEN** user navigates to the group management page
- **THEN** the system displays free groups the user owns or is a member of
- **AND** the system displays free groups where the user has a pending invitation

#### Scenario: User has pending invitations

- **WHEN** user with pending free group invitations navigates to the group management page
- **THEN** the system displays the pending invitations with group name and inviter
- **AND** the user can accept or reject each invitation directly from the list

#### Scenario: User with no free groups

- **WHEN** user with no free group memberships navigates to the group management page
- **THEN** the system displays an option to create a new free group
- **AND** displays pending invitations if any exist

### Requirement: Free Group Detail View

The system SHALL provide a detail page for each free group showing its name, owners, members, and pending invitations.

#### Scenario: Owner views free group detail

- **WHEN** free group owner navigates to the group detail page
- **THEN** the system displays group name, owners, members, and pending invitations

#### Scenario: Member views free group detail

- **WHEN** free group member (non-owner) navigates to the group detail page
- **THEN** the system displays group name, owners, and members
- **AND** the system does NOT display invitation management controls

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

### Requirement: Age-Based Automatic Reassignment

The system SHALL periodically check whether members still match their training group's age range and reassign them when they age out.

#### Scenario: Member ages out of current training group

- **WHEN** the periodic age check runs
- **AND** a member's current age no longer falls within their training group's age range
- **AND** another training group's age range matches the member's current age
- **THEN** the system moves the member to the matching training group
- **AND** the owners of both groups are notified of the change

#### Scenario: Member ages out with no matching group

- **WHEN** the periodic age check runs
- **AND** a member's age no longer matches any training group's age range
- **THEN** the member is removed from their current training group
- **AND** the owner of the group is notified

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

### Requirement: Exclusive Family Group Membership

The system SHALL enforce that each member belongs to at most one family group at any time.

#### Scenario: User attempts to add member to a second family group

- **WHEN** user tries to add a member who already has a family group
- **THEN** the system rejects the action with an error indicating existing family group membership

### Requirement: Family Group Info on Member Profile

The system SHALL display the member's family group information on their profile page for members assigned to a family group.

#### Scenario: Member views their family group on profile

- **WHEN** member who belongs to a family group views their profile page
- **THEN** the system displays the family group name

#### Scenario: Member without family group views profile

- **WHEN** member who does not belong to any family group views their profile page
- **THEN** the family group section is not displayed
