## ADDED Requirements

### Requirement: Legal Guardians Edit Their Minors' Details

Every legal guardian group SHALL delegate the permission to edit member details (MEMBER:EDIT_DETAILS) to its guardians. A legal guardian therefore SHALL be able to edit the details of each minor in a group in which they are a guardian, with the same editable fields as an adult member editing their own profile. They SHALL NOT be able to edit members outside those groups, and the permission ends when a minor leaves the group (for example on turning 18) or the guardian is removed.

#### Scenario: Guardian edits a minor's details

- **GIVEN** a legal guardian of a minor
- **WHEN** the guardian opens the minor's detail and uses the "Upravit" action, changes the editable fields, and saves
- **THEN** the changes are saved

#### Scenario: Guardian cannot edit an unrelated member

- **GIVEN** a legal guardian of a minor
- **WHEN** the guardian opens the detail of a member who is not their minor
- **THEN** the "Upravit" action is not displayed
- **AND** an attempt to save changes to that member is refused with a permission denied error

#### Scenario: Guardian loses the permission when the minor turns 18

- **GIVEN** a minor in a legal guardian group has turned 18 and left the group
- **WHEN** the former guardian opens the member's detail
- **THEN** the "Upravit" action is not displayed

#### Scenario: Guardian cannot change administrator-only fields

- **WHEN** a legal guardian edits a minor's details
- **THEN** the fields reserved for administrators (first name, last name, date of birth, gender, birth number) are not editable
