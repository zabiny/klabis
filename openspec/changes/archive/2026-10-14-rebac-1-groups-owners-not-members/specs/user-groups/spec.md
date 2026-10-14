# Spec Delta

## ADDED Requirements

### Requirement: Owners Are Not Members

The system SHALL keep the owners of a group and its members as two separate sets of people. Nobody is at the same time an owner and a member of the same group. For training groups this means a trainer is never a trainee of the same group.

#### Scenario: Free group detail lists every person exactly once

- **GIVEN** a free group with two owners and three members
- **WHEN** any person in the group opens the free group detail page
- **THEN** the owners section shows the two owners
- **AND** the members section shows the three members
- **AND** no owner appears in the members section

#### Scenario: Trainee who becomes trainer leaves the trainee list

- **GIVEN** a member is a trainee of a training group
- **WHEN** user with GROUPS:TRAINING permission adds that member as trainer of the same training group
- **THEN** the member is shown among the trainers
- **AND** the member is no longer shown among the trainees of that group

#### Scenario: Automatic age-based assignment skips the group's own trainer

- **GIVEN** a trainer of a training group whose age falls within that group's age range
- **WHEN** the system automatically assigns eligible members to the training group
- **THEN** the trainer is NOT added as a trainee of that group

### Requirement: Member Leaves Free Group

The system SHALL allow a member of a free group to leave the group at any time from the free group detail page. Leaving does not require approval of the owners. Owners leave a group only by giving up ownership (see Group Owner Management). Members of training groups and legal guardian groups cannot leave those groups on their own.

#### Scenario: Member leaves a free group

- **GIVEN** a member of a free group who is not its owner
- **WHEN** the member opens the free group detail page, clicks "Opustit skupinu" and confirms
- **THEN** the member is no longer in the group
- **AND** the group no longer appears in the member's list of groups

#### Scenario: Leave action is offered only to members

- **WHEN** an owner of a free group views the free group detail page
- **THEN** no "Opustit skupinu" action is offered to them

#### Scenario: Member cannot leave a training group on their own

- **WHEN** a trainee views the detail page of their training group
- **THEN** no action to leave the group is offered

#### Scenario: Member who left can be invited again

- **GIVEN** a member left a free group
- **WHEN** an owner of that group invites the member again
- **THEN** the system creates a new pending invitation for the member

## MODIFIED Requirements

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Trainers can be managed by users with GROUPS:TRAINING permission, and trainers need not be current members of the training group. For free groups, owners manage other owners directly, but only current group members may be promoted to owner. Removing an owner removes that person from the group entirely; they do not stay in the group as a member.

#### Scenario: User with GROUPS:TRAINING permission adds a trainer to a training group

- **WHEN** user with GROUPS:TRAINING permission adds another member as trainer to a training group
- **THEN** the member is added as trainer of the training group

#### Scenario: User with GROUPS:TRAINING permission removes a trainer from a training group

- **WHEN** user with GROUPS:TRAINING permission removes a trainer from a training group
- **AND** the training group has more than one trainer
- **THEN** the trainer is removed from the trainer list
- **AND** the former trainer does NOT become a trainee of the group

#### Scenario: Attempt to remove the last trainer from a training group

- **WHEN** user attempts to remove the sole remaining trainer from a training group
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

#### Scenario: Owner adds an existing member as co-owner of a free group

- **WHEN** a free group owner promotes a current member of the group to co-owner
- **THEN** the member receives owner privileges for the group

#### Scenario: Owner removes a co-owner from a free group

- **GIVEN** a free group has more than one owner
- **WHEN** an owner removes another owner from the group's owners
- **THEN** the removed person is no longer in the group at all
- **AND** the group no longer appears in that person's list of groups

#### Scenario: Owner gives up ownership of a free group

- **GIVEN** a free group has more than one owner
- **WHEN** an owner removes themselves as owner
- **THEN** that person leaves the group entirely

#### Scenario: Owner attempts to remove the last owner from a free group

- **WHEN** the sole remaining owner of a free group attempts to remove themselves as owner
- **THEN** the system rejects the action
- **AND** displays a message requiring the user to designate a successor first

### Requirement: Create Free Group

The system SHALL allow any authenticated member to create free groups from the group management page. A free group is a user-defined collection of members managed entirely by its owner(s) through an invitation system. The creator becomes the owner of the group and is not one of its members.

#### Scenario: Member creates a free group

- **WHEN** authenticated member fills in group name on the group management page
- **THEN** the system creates the free group with the creating member as owner
- **AND** the group starts with no members

### Requirement: Owner Promotion in Invitation-Based Groups Requires Existing Membership

For any group type that uses an invitation-based membership flow (today: free groups), the system SHALL allow owner promotion only for members who are already current members of the same group. Attempting to promote a non-member directly to owner SHALL be rejected with an error. A promoted member moves from the members of the group to its owners.

#### Scenario: Owner promotes an existing member to co-owner

- **WHEN** a free group owner promotes a current member of the group to co-owner
- **THEN** the member receives owner privileges for the group
- **AND** the member is shown among the owners and no longer among the members

#### Scenario: Owner attempts to promote a non-member to owner

- **WHEN** a free group owner attempts to promote a user who is not a current member of the group to owner
- **THEN** the system rejects the action
- **AND** displays an error indicating that only existing members can be promoted to owner
- **AND** the candidate is NOT added to the group as a side effect

### Requirement: Training Group Member Management

The system SHALL allow users with GROUPS:TRAINING permission to view and manage training group members. Members are primarily managed through age-based assignment but can also be manually added or removed. Trainers are never trainees of the same group, so the trainee list and its management actions never include them.

#### Scenario: User with GROUPS:TRAINING permission views training group member list

- **WHEN** user with GROUPS:TRAINING permission navigates to the training group detail on the training groups page
- **THEN** the system displays all current members with their names and join dates
- **AND** the trainers of the group are not listed among the members

#### Scenario: User with GROUPS:TRAINING permission removes a member from a training group

- **WHEN** user with GROUPS:TRAINING permission removes a member who is not a trainer
- **THEN** the member is removed from the training group
- **AND** the member is not automatically reassigned to another training group

#### Scenario: User with GROUPS:TRAINING permission attempts to remove a trainer from members

- **WHEN** user with GROUPS:TRAINING permission views the member list of a training group
- **THEN** the trainers of the group are not in that list
- **AND** therefore no remove action is offered for them there; trainers are removed only through trainer management

#### Scenario: Trainer of the group cannot be manually added as its trainee

- **WHEN** user with GROUPS:TRAINING permission opens the "add member" dialog on a training group detail page
- **THEN** the member picker does NOT list the trainers of that training group
