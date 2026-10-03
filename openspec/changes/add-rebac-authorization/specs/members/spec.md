## ADDED Requirements

### Requirement: Member Details Editing With Delegated Permission

The system SHALL allow editing a member's details by a user who holds the MEMBER:EDIT_DETAILS permission over that member, as well as by users with MEMBERS:MANAGE. The editable fields are those editable by an adult member on their own profile. The restriction that a minor cannot edit their own profile applies only when the minor edits their own profile; it does not restrict a different user who holds the permission over the minor.

#### Scenario: User with delegated permission edits a member

- **GIVEN** a user who holds MEMBER:EDIT_DETAILS over a member
- **WHEN** the user edits the member's details and saves
- **THEN** the updated information is saved

#### Scenario: Delegated permission does not allow editing administrator-only fields

- **GIVEN** a user who holds MEMBER:EDIT_DETAILS over a member and has no MEMBERS:MANAGE
- **WHEN** the user opens the member's edit form
- **THEN** the fields reserved for administrators are not editable

#### Scenario: Minor still cannot edit own profile

- **GIVEN** a minor member who also owns a group delegating MEMBER:EDIT_DETAILS and is a member of that group
- **WHEN** the minor views their own profile
- **THEN** the "Upravit" action is not displayed
- **AND** an attempt to save changes to their own profile is refused with a permission denied error

#### Scenario: Edit form is hidden without any applicable permission

- **GIVEN** a user with neither MEMBERS:MANAGE nor MEMBER:EDIT_DETAILS over a member
- **WHEN** the user views the member's detail
- **THEN** the "Upravit" action is not displayed
