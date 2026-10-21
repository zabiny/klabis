## MODIFIED Requirements

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Trainers can be managed by users with GROUPS:TRAINING permission, and trainers need not be current members of the training group. For free groups, owners manage other owners directly, but only current group members may be promoted to owner; in addition, a user with MEMBERS:MANAGE permission may add an owner to any free group, so that a group can get a successor for an owner who is about to be suspended. Removing an owner removes that person from the group entirely; they do not stay in the group as a member.

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

#### Scenario: User with MEMBERS:MANAGE permission adds an owner to a free group

- **GIVEN** a user with MEMBERS:MANAGE permission who is not an owner of a free group
- **WHEN** the user promotes a current member of that group to owner
- **THEN** the member receives owner privileges for the group

#### Scenario: User with MEMBERS:MANAGE permission cannot promote a non-member

- **WHEN** a user with MEMBERS:MANAGE permission attempts to promote a person who is not a current member of the free group
- **THEN** the system rejects the action
- **AND** displays an error indicating that only existing members can be promoted to owner

#### Scenario: Member without MEMBERS:MANAGE permission cannot add an owner

- **WHEN** a free group member who is not an owner and does not hold MEMBERS:MANAGE permission views the free group detail page
- **THEN** no action for adding an owner is offered

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

### Requirement: Warning on Last Owner Deactivation

The system SHALL warn when deactivating a member who is the last owner of any group and SHALL let the user resolve the warning from the warning dialog. The resolution options depend on the group type, and each option is offered only to a user who may perform it on that group.

#### Scenario: Deactivating last trainer of a training group

- **WHEN** admin initiates suspension of a member who is the sole trainer of a training group
- **THEN** the system displays a warning that the member is the last trainer
- **AND** requires the admin to designate a successor trainer before proceeding
- **AND** does not offer dissolving the training group

#### Scenario: Successor trainer is designated only by a user with GROUPS:TRAINING permission

- **WHEN** admin without GROUPS:TRAINING permission sees the warning for a training group
- **THEN** no action is offered for that group
- **AND** the warning tells the admin that a training groups administrator has to designate the successor trainer

#### Scenario: Deactivating last owner of a free group

- **WHEN** admin initiates suspension of a member who is the sole owner of a free group
- **THEN** the system displays a warning with options to either designate a successor or dissolve the group
- **AND** the admin must choose one option before the suspension proceeds

#### Scenario: Successor of a free group owner is chosen among the group's members

- **WHEN** admin designates a successor owner of a free group from the warning
- **THEN** the offered candidates are the current members of that group
- **AND** the option to designate a successor is not offered when the group has no other members

#### Scenario: Deactivating the sole legal guardian of a minor

- **WHEN** admin initiates suspension of a member who is the sole legal guardian of one or more minors
- **THEN** the system displays a warning listing the affected legal guardian groups
- **AND** the suspension does not proceed until the affected minors have another legal guardian or the whole group is suspended together with the member
- **AND** the admin may set the guardians on the group's page in "Zákonní zástupci" or choose to suspend the whole group
- **AND** dissolving the legal guardian group is not offered

### Requirement: Group Deletion

The system SHALL allow group deletion. Training group deletion requires GROUPS:TRAINING permission. Free group deletion requires group ownership or MEMBERS:MANAGE permission. Legal guardian groups cannot be deleted by hand.

#### Scenario: User with GROUPS:TRAINING permission deletes a training group

- **WHEN** user with GROUPS:TRAINING permission confirms training group deletion
- **THEN** the system removes the group
- **AND** all members are unassigned from the group

#### Scenario: Owner deletes a free group

- **WHEN** free group owner confirms group deletion
- **THEN** the system removes the group and all memberships
- **AND** pending invitations are cancelled

#### Scenario: User with MEMBERS:MANAGE permission deletes a free group

- **GIVEN** a user with MEMBERS:MANAGE permission who is not an owner of the free group
- **WHEN** the user confirms deletion of the free group
- **THEN** the system removes the group and all memberships
- **AND** pending invitations are cancelled

#### Scenario: Member without MEMBERS:MANAGE permission cannot delete a free group

- **WHEN** a free group member who is not an owner and does not hold MEMBERS:MANAGE permission views the free group detail page
- **THEN** no action for deleting the group is offered

### Requirement: Free Group Detail View

The system SHALL provide a detail page for each free group showing its name, owners, members, and pending invitations. Any member of the group and any user with MEMBERS:MANAGE permission SHALL be able to view the group detail page. Current group owners SHALL be offered a cancel action on each pending invitation row.

#### Scenario: Owner views free group detail

- **WHEN** free group owner navigates to the group detail page
- **THEN** the system displays group name, owners, members, and pending invitations

#### Scenario: Member views free group detail

- **WHEN** free group member (non-owner) navigates to the group detail page
- **THEN** the system displays group name, owners, and members
- **AND** the system does NOT display invitation management controls

#### Scenario: User with MEMBERS:MANAGE permission views free group detail

- **WHEN** a user with MEMBERS:MANAGE permission who is neither owner nor member opens the group detail
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
