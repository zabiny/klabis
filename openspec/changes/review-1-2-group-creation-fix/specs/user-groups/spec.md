## MODIFIED Requirements

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
