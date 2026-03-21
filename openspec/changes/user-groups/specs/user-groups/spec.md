## ADDED Requirements

### Requirement: User Group Types

The system SHALL support three types of user groups: Training, Family, and Free. Each type has distinct rules for creation, membership management, and lifecycle.

#### Scenario: Training group characteristics

- **WHEN** a training group exists
- **THEN** it has a name, age range (min/max), at least one owner, and zero or more members
- **AND** members are assigned based on their age
- **AND** a member SHALL belong to at most one training group

#### Scenario: Family group characteristics

- **WHEN** a family group exists
- **THEN** it has a name, at least one owner, and zero or more members
- **AND** a member SHALL belong to at most one family group

#### Scenario: Free group characteristics

- **WHEN** a free group exists
- **THEN** it has a name, at least one owner, zero or more members, and zero or more invitations
- **AND** a member MAY belong to multiple free groups

### Requirement: Create Training Group

The system SHALL allow users with training group management permission to create training groups with a name and age range.

#### Scenario: Successful training group creation

- **WHEN** authenticated user with USER_GROUPS:MANAGE_TRAINING permission submits POST /api/user-groups/training with name and age range
- **THEN** the system creates a training group
- **AND** the creating user's associated member becomes an owner
- **AND** HTTP 201 Created is returned with Location header
- **AND** response includes HAL+FORMS links

#### Scenario: Age range overlaps with existing training group

- **WHEN** authenticated user creates a training group with age range 10-14
- **AND** a training group with age range 12-16 already exists
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates age range overlaps with existing training group

#### Scenario: Invalid age range

- **WHEN** authenticated user creates a training group with minAge greater than maxAge
- **THEN** HTTP 400 Bad Request is returned

#### Scenario: Unauthorized user attempts training group creation

- **WHEN** authenticated user without USER_GROUPS:MANAGE_TRAINING permission attempts to create a training group
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Create Family Group

The system SHALL allow users with member management permission to create family groups.

#### Scenario: Successful family group creation

- **WHEN** authenticated user with MEMBERS:MANAGE permission submits POST /api/user-groups/family with name
- **THEN** the system creates a family group
- **AND** HTTP 201 Created is returned with Location header

#### Scenario: Unauthorized user attempts family group creation

- **WHEN** authenticated user without MEMBERS:MANAGE permission attempts to create a family group
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Create Free Group

The system SHALL allow any authenticated member to create a free group.

#### Scenario: Successful free group creation

- **WHEN** authenticated member submits POST /api/user-groups/free with name
- **THEN** the system creates a free group
- **AND** the creating member becomes an owner
- **AND** HTTP 201 Created is returned with Location header

#### Scenario: User without member record cannot create free group

- **WHEN** authenticated user without associated member record attempts to create a free group
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Delete Group

The system SHALL enforce type-specific rules for group deletion.

#### Scenario: Admin deletes training group

- **WHEN** authenticated user with USER_GROUPS:MANAGE_TRAINING permission submits DELETE /api/user-groups/{id}
- **AND** the group is a training group
- **THEN** the group is deleted
- **AND** HTTP 204 No Content is returned

#### Scenario: Admin deletes family group

- **WHEN** authenticated user with MEMBERS:MANAGE permission submits DELETE /api/user-groups/{id}
- **AND** the group is a family group
- **THEN** the group is deleted
- **AND** HTTP 204 No Content is returned

#### Scenario: Owner deletes free group

- **WHEN** authenticated member who is an owner of a free group submits DELETE /api/user-groups/{id}
- **THEN** the group is deleted
- **AND** HTTP 204 No Content is returned

#### Scenario: Non-owner attempts to delete free group

- **WHEN** authenticated member who is NOT an owner of a free group attempts to delete it
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: Owner attempts to delete training group

- **WHEN** authenticated member who is an owner but does not have USER_GROUPS:MANAGE_TRAINING permission attempts to delete a training group
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: Owner attempts to delete family group

- **WHEN** authenticated member who is an owner but does not have MEMBERS:MANAGE permission attempts to delete a family group
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Manage Group Owners

The system SHALL allow group owners to add and remove other owners. A group SHALL always have at least one owner.

#### Scenario: Owner adds another owner

- **WHEN** authenticated owner of a group submits POST /api/user-groups/{id}/owners with memberId
- **THEN** the specified member becomes an owner of the group
- **AND** HTTP 200 OK is returned

#### Scenario: Owner removes another owner

- **WHEN** authenticated owner of a group submits DELETE /api/user-groups/{id}/owners/{memberId}
- **AND** the group has more than one owner
- **THEN** the specified member is removed as owner
- **AND** HTTP 204 No Content is returned

#### Scenario: Attempt to remove last owner

- **WHEN** authenticated owner attempts to remove the only remaining owner
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates group must have at least one owner

### Requirement: Manage Training Group Members

The system SHALL support automatic age-based assignment and manual assignment of members to training groups.

#### Scenario: Automatic assignment on member registration

- **WHEN** a new member is registered in the club
- **AND** a training group exists whose age range includes the member's age
- **THEN** the member is automatically assigned to that training group

#### Scenario: No matching training group for member's age

- **WHEN** a new member is registered in the club
- **AND** no training group's age range includes the member's age
- **THEN** the member is not assigned to any training group

#### Scenario: Manual assignment by owner

- **WHEN** authenticated owner of a training group submits POST /api/user-groups/{id}/members with memberId
- **THEN** the specified member is added to the training group
- **AND** HTTP 200 OK is returned

#### Scenario: Member already in another training group

- **WHEN** owner attempts to add a member who is already in another training group
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates member is already in a training group

#### Scenario: Owner removes member from training group

- **WHEN** authenticated owner submits DELETE /api/user-groups/{id}/members/{memberId}
- **THEN** the member is removed from the training group
- **AND** HTTP 204 No Content is returned

#### Scenario: Member cannot leave training group themselves

- **WHEN** authenticated member who is not an owner attempts to remove themselves from a training group
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Periodic Age-Based Reassignment

The system SHALL periodically reassign training group members when their age no longer falls within the group's age range.

#### Scenario: Member outgrows current training group

- **WHEN** the periodic reassignment runs
- **AND** a member's current age no longer falls within their training group's age range
- **AND** another training group's age range includes the member's current age
- **THEN** the member is moved to the matching training group

#### Scenario: Member outgrows with no matching group

- **WHEN** the periodic reassignment runs
- **AND** a member's current age no longer falls within their training group's age range
- **AND** no other training group's age range includes the member's current age
- **THEN** the member is removed from their current training group

### Requirement: Manage Family Group Members

The system SHALL allow only users with member management permission to add and remove members from family groups.

#### Scenario: Admin adds member to family group

- **WHEN** authenticated user with MEMBERS:MANAGE permission submits POST /api/user-groups/{id}/members with memberId
- **AND** the group is a family group
- **THEN** the member is added to the family group
- **AND** HTTP 200 OK is returned

#### Scenario: Member already in another family group

- **WHEN** admin attempts to add a member who is already in another family group
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates member is already in a family group

#### Scenario: Owner attempts to add member to family group

- **WHEN** authenticated owner of a family group (without MEMBERS:MANAGE permission) attempts to add a member
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: Admin removes member from family group

- **WHEN** authenticated user with MEMBERS:MANAGE permission submits DELETE /api/user-groups/{id}/members/{memberId}
- **AND** the group is a family group
- **THEN** the member is removed from the family group
- **AND** HTTP 204 No Content is returned

#### Scenario: Member cannot leave family group themselves

- **WHEN** authenticated member attempts to remove themselves from a family group
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Free Group Invitation System

The system SHALL allow owners of free groups to invite members via invitations that must be accepted.

#### Scenario: Owner invites member

- **WHEN** authenticated owner of a free group submits POST /api/user-groups/{id}/invitations with memberId
- **THEN** a pending invitation is created
- **AND** HTTP 201 Created is returned

#### Scenario: Invite already invited member

- **WHEN** owner invites a member who already has a pending invitation to the same group
- **THEN** HTTP 409 Conflict is returned

#### Scenario: Invite existing member

- **WHEN** owner invites a member who is already a member of the group
- **THEN** HTTP 409 Conflict is returned

#### Scenario: Non-owner attempts to invite

- **WHEN** authenticated member who is not an owner attempts to invite someone
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Accept or Reject Invitation

The system SHALL allow invited members to accept or reject invitations to free groups.

#### Scenario: Member accepts invitation

- **WHEN** invited member submits POST /api/user-groups/invitations/{invitationId}/accept
- **THEN** the invitation status changes to ACCEPTED
- **AND** the member is added to the free group
- **AND** HTTP 200 OK is returned

#### Scenario: Member rejects invitation

- **WHEN** invited member submits POST /api/user-groups/invitations/{invitationId}/reject
- **THEN** the invitation status changes to REJECTED
- **AND** the member is NOT added to the group
- **AND** HTTP 200 OK is returned

#### Scenario: Non-invited member attempts to accept

- **WHEN** authenticated member who is not the invitee attempts to accept an invitation
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: Accept already processed invitation

- **WHEN** member attempts to accept an invitation that is already ACCEPTED or REJECTED
- **THEN** HTTP 409 Conflict is returned

### Requirement: Re-invitation After Rejection

The system SHALL allow owners to re-invite a member who previously rejected an invitation.

#### Scenario: Re-invite after rejection

- **WHEN** owner invites a member who previously rejected an invitation to the same group
- **THEN** a new pending invitation is created
- **AND** HTTP 201 Created is returned

### Requirement: Free Group Member Departure

The system SHALL allow members to voluntarily leave free groups and owners to remove members.

#### Scenario: Member leaves free group

- **WHEN** authenticated member submits DELETE /api/user-groups/{id}/members/me
- **AND** the group is a free group
- **THEN** the member is removed from the group
- **AND** HTTP 204 No Content is returned

#### Scenario: Owner removes member from free group

- **WHEN** authenticated owner submits DELETE /api/user-groups/{id}/members/{memberId}
- **AND** the group is a free group
- **THEN** the member is removed from the group
- **AND** HTTP 204 No Content is returned

### Requirement: List My Groups

The system SHALL allow authenticated members to list groups they are a member of or own.

#### Scenario: Member lists their groups

- **WHEN** authenticated member submits GET /api/user-groups/me
- **THEN** HTTP 200 OK is returned
- **AND** response contains all groups where the member is a member or owner
- **AND** each group includes name, type, and member count
- **AND** response follows HAL+FORMS format with links to group details

#### Scenario: Member with no groups

- **WHEN** authenticated member with no group memberships or ownership submits GET /api/user-groups/me
- **THEN** HTTP 200 OK is returned
- **AND** response contains an empty collection

### Requirement: View Group Detail

The system SHALL allow group members and owners to view group details including the member list.

#### Scenario: Member views group detail

- **WHEN** authenticated member who is a member or owner of a group submits GET /api/user-groups/{id}
- **THEN** HTTP 200 OK is returned
- **AND** response contains group name, type, and list of members (first name, last name)
- **AND** response contains list of owners
- **AND** response follows HAL+FORMS format

#### Scenario: Non-member attempts to view group detail

- **WHEN** authenticated member who is not a member or owner of the group submits GET /api/user-groups/{id}
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: Admin views any group detail

- **WHEN** authenticated user with USER_GROUPS:MANAGE_TRAINING or MEMBERS:MANAGE permission submits GET /api/user-groups/{id}
- **THEN** HTTP 200 OK is returned regardless of membership

### Requirement: List All Groups (Admin)

The system SHALL allow administrators to list all groups in the system.

#### Scenario: Admin lists all groups

- **WHEN** authenticated user with USER_GROUPS:MANAGE_TRAINING or MEMBERS:MANAGE permission submits GET /api/user-groups
- **THEN** HTTP 200 OK is returned
- **AND** response contains all groups in the system
- **AND** each group includes name, type, owner names, and member count

#### Scenario: Regular member attempts to list all groups

- **WHEN** authenticated member without admin permissions submits GET /api/user-groups
- **THEN** HTTP 403 Forbidden is returned

### Requirement: List My Pending Invitations

The system SHALL allow authenticated members to view their pending invitations.

#### Scenario: Member lists pending invitations

- **WHEN** authenticated member submits GET /api/user-groups/invitations/me
- **THEN** HTTP 200 OK is returned
- **AND** response contains all pending invitations for the member
- **AND** each invitation includes group name, invited by (name), and creation date
- **AND** each invitation includes HAL+FORMS links for accept and reject actions

#### Scenario: Member with no pending invitations

- **WHEN** authenticated member with no pending invitations submits GET /api/user-groups/invitations/me
- **THEN** HTTP 200 OK is returned
- **AND** response contains an empty collection

### Requirement: User Group Response Format

All user group API responses SHALL follow HAL+FORMS specification.

#### Scenario: Group response uses HAL+FORMS media type

- **WHEN** any user group endpoint returns a response
- **THEN** Content-Type is application/prs.hal-forms+json
- **AND** response includes _links object for hypermedia navigation

#### Scenario: Group detail includes contextual action links

- **WHEN** an owner views a free group detail
- **THEN** response includes affordances for invite member, remove member, add owner, and delete group

#### Scenario: Group detail for non-owner member

- **WHEN** a non-owner member views a free group detail
- **THEN** response includes affordance for leave group
- **AND** response does NOT include affordances for invite, remove member, or delete

#### Scenario: Training group detail includes age range

- **WHEN** a user views a training group detail
- **THEN** response includes the age range (minAge, maxAge) of the training group
