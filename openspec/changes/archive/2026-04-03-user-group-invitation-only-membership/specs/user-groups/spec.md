## MODIFIED Requirements

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
