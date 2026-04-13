## MODIFIED Requirements

### Requirement: Free Group Detail View

The system SHALL provide a detail page for each free group showing its name, owners, members, and pending invitations. Any member of the group SHALL be able to view the group detail page.

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

## ADDED Requirements

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
