## MODIFIED Requirements

### Requirement: Group Types

The system SHALL provide two distinct group types with separate creation flows and access rules: training groups and free groups. Legal guardian groups, which replace family groups, are described in the legal-guardians capability and are not created from the group management pages.

Creating either of the two group types SHALL succeed end-to-end through the user interface — selecting the create action, filling in the required fields, and submitting the form SHALL persist the group and add the creating user as the appropriate owner role for that group type.

#### Scenario: Each group type has distinct creation access

- **WHEN** users access the group management features
- **THEN** Training groups are created from a separate, restricted page accessible only to users with the GROUPS:TRAINING authority
- **AND** Free groups are created by any authenticated member from the general group management page

#### Scenario: Each group type has distinct membership rules

- **WHEN** users join groups
- **THEN** Training groups have member assignment based on age fitting within the configured age range
- **AND** Free groups have membership controlled exclusively through an invitation system: owners send invitations, invitees accept or reject them, and direct member addition is not permitted

#### Scenario: Member successfully creates a free group through the UI

- **GIVEN** an authenticated member is on the group management page
- **WHEN** the member opens the "Create group" dialog, fills in a non-empty name, and submits the form
- **THEN** the request succeeds without an error message
- **AND** the new free group appears in the member's "My groups" list
- **AND** the member is the owner of the new group

#### Scenario: Authorized user successfully creates a training group through the UI

- **GIVEN** an authenticated user with GROUPS:TRAINING authority is on the training groups management page
- **WHEN** the user opens the "Create training group" dialog, fills in a non-empty name and a valid age range, and submits the form
- **THEN** the request succeeds without an error message
- **AND** the new training group appears in the training groups list
- **AND** the creating user is recorded as a trainer of the new group

### Requirement: Group Owner Management

The system SHALL require every group to have at least one owner. For training groups, owners are referred to as "trainers" in the user interface. Trainers can be managed by users with GROUPS:TRAINING permission, and trainers need not be current members of the training group. For free groups, owners manage other owners directly, but only current group members may be promoted to owner.

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

#### Scenario: Deactivating last owner of a free group

- **WHEN** admin initiates suspension of a member who is the sole owner of a free group
- **THEN** the system displays a warning with options to either designate a successor or dissolve the group
- **AND** the admin must choose one option before the suspension proceeds

#### Scenario: Deactivating the sole legal guardian of a minor

- **WHEN** admin initiates suspension of a member who is the sole legal guardian of one or more minors
- **THEN** the system displays a warning listing the affected legal guardian groups
- **AND** the suspension does not proceed until the affected minors have another legal guardian

### Requirement: Group Editing

The system SHALL allow editing of group properties. For training groups, a single edit operation updates name, age range, and trainers — all fields are optional (only provided fields are changed). For free groups, only the name can be edited by group owners.

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

#### Scenario: Owner edits free group name

- **WHEN** group owner changes the name of a free group
- **THEN** the system updates the name immediately

### Requirement: Group Deletion

The system SHALL allow group deletion. Training group deletion requires GROUPS:TRAINING permission. Free group deletion requires group ownership.

#### Scenario: User with GROUPS:TRAINING permission deletes a training group

- **WHEN** user with GROUPS:TRAINING permission confirms training group deletion
- **THEN** the system removes the group
- **AND** all members are unassigned from the group

#### Scenario: Owner deletes a free group

- **WHEN** free group owner confirms group deletion
- **THEN** the system removes the group and all memberships
- **AND** pending invitations are cancelled

### Requirement: Member-Picker Dialogs Hide Existing Members

The system SHALL hide members who are already in the target group from the candidate list in every dialog that selects a member to add to a group, regardless of group type (training or free). This includes dialogs for adding owners, trainees, and trainers.

#### Scenario: Training group "add trainee" dialog excludes current trainees

- **WHEN** user with GROUPS:TRAINING permission opens the "add member" dialog on a training group detail page
- **THEN** the member picker does NOT list members who are already trainees of that training group

#### Scenario: Free group "promote to owner" dialog excludes current owners

- **WHEN** a free group owner opens the "promote to owner" dialog
- **THEN** the picker does NOT list members who are already owners of that free group
- **AND** the picker lists only current members of the group (consistent with the owner-promotion rule)

## REMOVED Requirements

### Requirement: Create Family Group

**Reason**: Family groups are replaced by legal guardian groups, which are created automatically when guardians of a minor are set.
**Migration**: See "Legal Guardian Groups" and "Setting The Legal Guardians Of A Minor" in the legal-guardians capability.

### Requirement: Exclusive Family Group Membership

**Reason**: A legal guardian may represent minors in several groups; only minors remain limited to one group.
**Migration**: See "Legal Guardian Groups" in the legal-guardians capability.

### Requirement: Family Group Info on Member Profile

**Reason**: A minor's detail lists their legal guardians directly; the group is an administrative view.
**Migration**: See "Legal Guardians Of A Minor" in the legal-guardians capability and "Member Detail Page Layout" in the members capability.

### Requirement: Parent and Child Roles Are Exclusive Within a Family Group

**Reason**: Guardians and minors are separate roles by definition; a guardian is always an adult and a minor never a guardian.
**Migration**: See "Legal Guardian Candidates" in the legal-guardians capability.

### Requirement: Add and Remove Child Members of a Family Group

**Reason**: Minors join and leave groups only through setting their guardians and when they turn 18.
**Migration**: See "Setting The Legal Guardians Of A Minor" and "Minors Leave Guardian Groups When They Turn 18" in the legal-guardians capability.

### Requirement: Family Group Detail Access for Members

**Reason**: Legal guardian groups are an administrative view available only to users with MEMBERS:MANAGE.
**Migration**: See "Legal Guardian Groups Page" in the legal-guardians capability.

### Requirement: Family Groups Navigation Visibility

**Reason**: The "Rodinné skupiny" item is replaced by "Zákonní zástupci".
**Migration**: See "Legal Guardian Groups Page" in the legal-guardians capability.
