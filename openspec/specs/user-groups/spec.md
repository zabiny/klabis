# User Groups Specification

## Purpose

Defines the behavior of user groups in the system. Supports three group types: Training Groups (age-based assignment), Family Groups (parent-child linking), and Free Groups (user-managed invitation-based groups).

## Requirements

### Requirement: Group Types

The system SHALL provide three distinct group types with separate creation flows and access rules.

Creating any of the three group types SHALL succeed end-to-end through the user interface — selecting the create action, filling in the required fields, and submitting the form SHALL persist the group and add the creating user as the appropriate owner role for that group type.

#### Scenario: Each group type has distinct creation access

- **WHEN** users access the group management features
- **THEN** Training groups are created from a separate, restricted page accessible only to users with the GROUPS:TRAINING authority
- **AND** Family groups are created from the user management area accessible only to administrators
- **AND** Free groups are created by any authenticated member from the general group management page

#### Scenario: Each group type has distinct membership rules

- **WHEN** users join groups
- **THEN** Training groups have member assignment based on age fitting within the configured age range
- **AND** Family groups have member assignment limited to administrators or family owners
- **AND** Free groups have membership controlled exclusively through an invitation system: owners send invitations, invitees accept or reject them, and direct member addition is not permitted

#### Scenario: Member successfully creates a free group through the UI

- **GIVEN** an authenticated member is on the group management page
- **WHEN** the member opens the "Create group" dialog, fills in a non-empty name, and submits the form
- **THEN** the request succeeds without an error message
- **AND** the new free group appears in the member's "My groups" list
- **AND** the member is the owner of the new group

#### Scenario: Administrator successfully creates a family group through the UI

- **GIVEN** an authenticated administrator is on the family groups management page
- **WHEN** the administrator opens the "Create family group" dialog, fills in a non-empty name, and submits the form
- **THEN** the request succeeds without an error message
- **AND** the new family group appears in the family groups list
- **AND** the administrator (or the designated parent) is recorded as the owner of the new group

#### Scenario: Authorized user successfully creates a training group through the UI

- **GIVEN** an authenticated user with GROUPS:TRAINING authority is on the training groups management page
- **WHEN** the user opens the "Create training group" dialog, fills in a non-empty name and a valid age range, and submits the form
- **THEN** the request succeeds without an error message
- **AND** the new training group appears in the training groups list
- **AND** the creating user is recorded as a trainer of the new group

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

The system SHALL support an invitation-based membership flow for free groups. Group owners can invite members, and invited members can accept or reject invitations. Group owners can cancel pending invitations with an optional reason. Direct addition of members to a free group without an invitation is NOT allowed — membership can only be established through the invitation flow.

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
- **THEN** no "add member" action is offered
- **AND** only the "invite member" action is available

#### Scenario: Current owner cancels a pending invitation without reason

- **GIVEN** a free group has a pending invitation for a member
- **WHEN** a current group owner cancels the pending invitation without providing a reason
- **THEN** the invitation status changes to cancelled
- **AND** the invitation is no longer shown in the pending invitations list
- **AND** the invitee is notified that the invitation was cancelled
- **AND** all other current group owners are notified that the invitation was cancelled

#### Scenario: Current owner cancels a pending invitation with a reason

- **GIVEN** a free group has a pending invitation for a member
- **WHEN** a current group owner cancels the pending invitation and provides a reason
- **THEN** the invitation status changes to cancelled
- **AND** the invitee is notified that the invitation was cancelled, with the provided reason
- **AND** all other current group owners are notified that the invitation was cancelled, with the provided reason

#### Scenario: Former owner cannot cancel an invitation

- **GIVEN** a free group has a pending invitation
- **AND** a user who was previously an owner is no longer a current owner
- **WHEN** that former owner attempts to cancel the invitation
- **THEN** the system rejects the action
- **AND** the invitation remains pending

#### Scenario: Non-owner group member cannot cancel an invitation

- **GIVEN** a free group has a pending invitation
- **WHEN** a group member who is not an owner attempts to cancel the invitation
- **THEN** the system rejects the action
- **AND** the invitation remains pending

#### Scenario: Cancelling an already accepted invitation is rejected

- **GIVEN** an invitation has already been accepted
- **WHEN** a current group owner attempts to cancel that invitation
- **THEN** the system rejects the action with a state-conflict error
- **AND** the member remains in the group

#### Scenario: Cancelling an already rejected invitation is rejected

- **GIVEN** an invitation has been rejected by the invitee
- **WHEN** a current group owner attempts to cancel that invitation
- **THEN** the system rejects the action with a state-conflict error

#### Scenario: Cancelling an already cancelled invitation is rejected

- **GIVEN** an invitation has already been cancelled
- **WHEN** a current group owner attempts to cancel it again
- **THEN** the system rejects the action with a state-conflict error

#### Scenario: Member can be re-invited after cancellation

- **GIVEN** a member's previous invitation to a free group was cancelled
- **WHEN** a current group owner sends a new invitation to that member
- **THEN** the system creates a new pending invitation for the member

#### Scenario: Canceller is excluded from the cancellation notification

- **GIVEN** a free group has two or more current owners
- **WHEN** one of the owners cancels a pending invitation
- **THEN** the canceller does NOT receive a cancellation notification
- **AND** every other current owner does receive a cancellation notification

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

The system SHALL provide a detail page for each free group showing its name, owners, members, and pending invitations. Any member of the group SHALL be able to view the group detail page. Current group owners SHALL be offered a cancel action on each pending invitation row.

#### Scenario: Owner views free group detail

- **WHEN** free group owner navigates to the group detail page
- **THEN** the system displays group name, owners, members, and pending invitations

#### Scenario: Member views free group detail

- **WHEN** free group member (non-owner) navigates to the group detail page
- **THEN** the system displays group name, owners, and members
- **AND** the system does NOT display invitation management controls

#### Scenario: Group member without ownership accesses detail via profile button

- **WHEN** a free group member clicks the group navigation button on their profile page
- **THEN** the system displays the group detail page
- **AND** does NOT return an access denied error

#### Scenario: Owner sees cancel action on each pending invitation

- **WHEN** a current group owner views the free group detail page
- **THEN** every row in the pending invitations list offers a "cancel invitation" action

#### Scenario: Non-owner does not see cancel action on pending invitations

- **WHEN** a group member who is not an owner views the free group detail page
- **THEN** no "cancel invitation" action is offered on any pending invitation row

#### Scenario: Cancel action opens a confirmation with optional reason

- **WHEN** a current owner triggers the cancel action on a pending invitation
- **THEN** the system asks for confirmation
- **AND** offers an optional free-text field for the cancellation reason
- **AND** only submits the cancellation after the owner confirms

### Requirement: Invitation Auto-Cancel on Invitee Deactivation

The system SHALL automatically cancel all of a member's pending free-group invitations when that member is deactivated or their membership is terminated. The cancellation SHALL NOT notify the deactivated member but SHALL notify the current owners of each affected group.

#### Scenario: Member deactivation cancels their pending invitations

- **GIVEN** a member has one or more pending invitations to free groups
- **WHEN** the member is deactivated or their membership is terminated
- **THEN** every pending invitation for that member transitions to cancelled
- **AND** each affected group's current owners are notified that the invitation was cancelled
- **AND** the deactivated member is NOT notified

#### Scenario: Member deactivation leaves non-pending invitations untouched

- **GIVEN** a member has invitations in accepted, rejected, or already-cancelled states
- **WHEN** the member is deactivated
- **THEN** those non-pending invitations are NOT modified

#### Scenario: Auto-cancelled invitation allows re-invitation if the member reactivates

- **GIVEN** a member's pending invitation was auto-cancelled by deactivation
- **AND** the member is later reactivated
- **WHEN** a current group owner invites the member again
- **THEN** the system creates a new pending invitation

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

### Requirement: Training Group Detail Access for Members

The system SHALL allow any member of a training group to view the detail page of that training group.

#### Scenario: Training group member views group detail

- **WHEN** a member who belongs to a training group navigates to the training group detail page
- **THEN** the system displays the training group detail
- **AND** does NOT return an access denied error

#### Scenario: User who is not a member of a training group cannot access its detail

- **WHEN** a user who is not a member, trainer, or GROUPS:TRAINING permission holder attempts to access a training group detail
- **THEN** the system denies access

### Requirement: Family Group Detail Access for Members

The system SHALL allow any member of a family group to view the detail page of that family group.

#### Scenario: Family group member views group detail

- **WHEN** a member who belongs to a family group clicks the group navigation button on their profile page
- **THEN** the system displays the family group detail
- **AND** does NOT return an access denied error

#### Scenario: User who is not a member of a family group cannot access its detail

- **WHEN** a user who is not a member, parent, or MEMBERS:MANAGE permission holder attempts to access a family group detail
- **THEN** the system denies access

### Requirement: Family Groups Navigation Visibility

The system SHALL display the "Family Groups" navigation item in the Administration section only to users with MEMBERS:MANAGE permission.

#### Scenario: User with MEMBERS:MANAGE sees family groups navigation item

- **WHEN** a user with MEMBERS:MANAGE permission views the application navigation
- **THEN** the "Rodinné skupiny" item is visible in the Administration section

#### Scenario: User without MEMBERS:MANAGE does not see family groups navigation item

- **WHEN** a user without MEMBERS:MANAGE permission views the application navigation
- **THEN** the "Rodinné skupiny" item is NOT visible in the Administration section
