## ADDED Requirements

### Requirement: Family Group Follows a Minor's Guardians

The system SHALL keep a minor's family group in step with the minor's guardians. Every guardian of a minor SHALL be a parent of the family group the minor belongs to. When a minor with guardians belongs to no family group, the system SHALL place the minor into a family group automatically.

#### Scenario: Family group is created for a newly registered minor

- **WHEN** admin registers a minor whose guardians are not parents of any family group
- **THEN** a family group named after the minor's last name (e.g., "Novák") is created
- **AND** the minor is a child of the new family group
- **AND** every guardian of the minor is a parent of the new family group

#### Scenario: Sibling joins the existing family group of their guardian

- **GIVEN** a guardian who is already a parent of family group "Novákovi"
- **WHEN** admin registers another minor with that guardian
- **THEN** the new minor is added as a child to family group "Novákovi"
- **AND** no new family group is created
- **AND** any further guardians of the new minor become parents of "Novákovi"

#### Scenario: Guardians who are parents of different family groups

- **GIVEN** the two guardians of a new minor are each a parent of a different family group
- **WHEN** admin registers the minor
- **THEN** a new family group is created for the minor with both guardians as parents
- **AND** the existing family groups are left unchanged

#### Scenario: Adding a guardian makes them a parent of the minor's family group

- **WHEN** a guardian is added to a minor who is a child of a family group
- **THEN** the new guardian becomes a parent of that family group

#### Scenario: Removing a guardian removes them as parent

- **WHEN** a guardian is removed from a minor
- **AND** the guardian is not a guardian of any other child in the same family group
- **THEN** the guardian is no longer a parent of that family group

#### Scenario: Removed guardian stays parent while guarding a sibling

- **WHEN** a guardian is removed from one minor
- **AND** the guardian is still a guardian of another child in the same family group
- **THEN** the guardian remains a parent of that family group

### Requirement: Guardians Who Are Not Club Members as Family Group Parents

The system SHALL allow a legal guardian who is not a club member to be a parent of a family group. Such a parent SHALL be shown in the family group with their name and SHALL be able to open the family group detail after logging in. Only club members MAY be children of a family group.

#### Scenario: Family group detail lists a non-member parent

- **WHEN** user opens the detail of a family group whose parent is a non-member guardian
- **THEN** the guardian is listed among the parents with their name
- **AND** the guardian's name is not a link to a member profile

#### Scenario: Non-member parent views their family group

- **WHEN** a logged-in non-member guardian opens the family group they are a parent of
- **THEN** the system displays the family group detail

## MODIFIED Requirements

### Requirement: Create Family Group

The system SHALL allow users with MEMBERS:MANAGE permission to create family groups from the members list page. A family group links one designated parent with their children. Exactly one parent SHALL be designated at creation time. No additional children are added during creation; children can be added afterwards from the family group detail page. The creating user does not automatically become a parent. A person who is already a parent of another family group MAY be designated as parent of a new one.

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

- **WHEN** user attempts to designate as parent a member who is a child of another family group
- **THEN** the system rejects the creation
- **AND** displays an error indicating the member is already in a family group

#### Scenario: Designated parent is already a parent of another family group

- **WHEN** user designates as parent a person who is already a parent of another family group
- **THEN** the system creates the family group
- **AND** the person is a parent of both family groups

### Requirement: Exclusive Family Group Membership

The system SHALL enforce that each member belongs as a child to at most one family group at any time. A person MAY be a parent of any number of family groups.

#### Scenario: User attempts to add member to a second family group

- **WHEN** user tries to add as a child a member who is already a child of a family group
- **THEN** the system rejects the action with an error indicating existing family group membership

#### Scenario: Guardian is a parent in two family groups

- **GIVEN** a guardian who is a parent of family group "Novákovi"
- **WHEN** the guardian becomes a parent of family group "Dvořákovi"
- **THEN** the guardian is a parent of both family groups

### Requirement: Family Group Detail Access for Members

The system SHALL allow any member or parent of a family group, including a parent who is not a club member, to view the detail page of that family group.

#### Scenario: Family group member views group detail

- **WHEN** a member who belongs to a family group clicks the group navigation button on their profile page
- **THEN** the system displays the family group detail
- **AND** does NOT return an access denied error

#### Scenario: Non-member parent views group detail

- **WHEN** a non-member guardian who is a parent of a family group opens that family group
- **THEN** the system displays the family group detail

#### Scenario: User who is not a member of a family group cannot access its detail

- **WHEN** a user who is not a member, parent, or MEMBERS:MANAGE permission holder attempts to access a family group detail
- **THEN** the system denies access
