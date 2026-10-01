## MODIFIED Requirements

### Requirement: Member Update

The system SHALL allow adult members to update their own profile data, and admins to update any member's data. A minor member (under 18 years) cannot update their own profile; only admins can change a minor's data. Field access is role-based. Legal guardians are not part of the member edit form; they are managed separately on the minor's detail page.

#### Scenario: Member updates their own information

- **WHEN** authenticated adult member edits their own profile and saves the changes
- **THEN** the updated information is saved
- **AND** the member is taken to their updated profile

#### Scenario: Minor cannot edit own profile

- **WHEN** a minor member views their own profile
- **THEN** the "Upravit" action is not displayed
- **AND** an attempt to save changes to their own profile is refused with a permission denied error
- **AND** no changes are saved

#### Scenario: Admin updates a minor's information

- **WHEN** user with MEMBERS:UPDATE permission edits a minor member's profile and saves the changes
- **THEN** the updated information is saved

#### Scenario: Member can edit own profile after turning 18

- **GIVEN** a member who was a minor and has turned 18
- **WHEN** the member views their own profile
- **THEN** the "Upravit" action is displayed

#### Scenario: Admin updates a member's information

- **WHEN** user with MEMBERS:UPDATE permission edits a member's profile and saves the changes
- **THEN** the updated information is saved including admin-only fields (first name, last name, date of birth, gender, birth number)

#### Scenario: Member cannot edit another member's profile

- **WHEN** authenticated member attempts to edit a different member's profile without admin permission
- **THEN** the system shows a permission denied error
- **AND** no changes are saved

#### Scenario: Form shows validation errors on invalid update

- **WHEN** user submits an update with invalid data
- **THEN** the form shows inline validation errors
- **AND** no changes are saved

#### Scenario: Member-editable fields

- **WHEN** an adult member (without admin permission) opens their own edit form
- **THEN** the form shows fields: email, phone, address, chip number, nationality, bank account, identity card, driving license, medical course, trainer license, dietary restrictions

#### Scenario: Admin-only fields

- **WHEN** user with MEMBERS:UPDATE permission opens a member's edit form
- **THEN** the form additionally shows admin-only fields: first name, last name, date of birth, gender, birth number

#### Scenario: Non-admin submits admin-only fields

- **WHEN** member submits a form update that includes admin-only fields (e.g., firstName)
- **THEN** those fields are silently ignored
- **AND** only member-editable fields are updated

#### Scenario: Address update requires all fields

- **WHEN** user submits an update with only some address fields
- **THEN** the form shows an error that all address fields (street, city, postal code, country) are required
- **AND** no changes are saved

#### Scenario: Contact removal prevented

- **WHEN** an adult member submits an update that would remove their e-mail address
- **THEN** the form shows an error that at least one email is required

#### Scenario: Minor's own contact can be removed when a guardian covers it

- **GIVEN** a minor whose legal guardian has an e-mail address
- **WHEN** an admin removes the minor's own e-mail and saves
- **THEN** the change is saved

#### Scenario: Changing nationality to Czech enables birth number field

- **WHEN** member changes nationality to Czech (CZ) in the edit form
- **THEN** the birth number field becomes available for input

#### Scenario: Changing nationality away from Czech clears birth number

- **WHEN** member changes nationality from Czech to non-Czech in the edit form
- **THEN** the birth number field is cleared and hidden

#### Scenario: Partial update preserves unchanged fields

- **WHEN** member submits a PATCH update with only some fields
- **THEN** only those fields are updated
- **AND** all other fields remain unchanged

#### Scenario: Empty update rejected

- **WHEN** member submits a PATCH update with an empty request body
- **THEN** the form shows an error that at least one field must be provided
