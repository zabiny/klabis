# Delta Spec: user-groups

## MODIFIED Requirements

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

## ADDED Requirements

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
