## MODIFIED Requirements

### Requirement: Legal Guardians Edit Their Minors' Data

A legal guardian group SHALL always delegate "Úprava údajů člena" to its guardians over its minors; this cannot be changed. A legal guardian therefore sees all data of each of their minor children and can edit it, except the administrator-reserved fields. The guardian loses this permission over a child when the child leaves the group after turning 18, or when the guardian stops being the child's legal guardian.

#### Scenario: Guardian edits their child's telephone

- **GIVEN** a member is the legal guardian of a minor
- **WHEN** the guardian opens the minor's detail page, clicks "Upravit profil", changes the minor's telephone and saves
- **THEN** the new telephone is shown on the minor's detail page

#### Scenario: Guardian cannot change reserved data of their child

- **WHEN** a legal guardian without MEMBERS:MANAGE opens the edit form of their minor child
- **THEN** first name, last name, date of birth, gender, nationality and birth number are shown read-only

#### Scenario: Non-member guardian edits their child's data

- **GIVEN** a non-member legal guardian logged in with their EXT login number
- **WHEN** the guardian opens their minor child's detail page
- **THEN** all of the child's data is shown
- **AND** the "Upravit profil" action is available

#### Scenario: Guardian loses the permission when the child turns 18

- **GIVEN** a legal guardian of a minor
- **WHEN** the minor turns 18 and the daily run has removed them from the legal guardian group
- **THEN** the former guardian can no longer edit the member's data
- **AND** the member can edit their own data

#### Scenario: Removed guardian loses the permission

- **GIVEN** a legal guardian of a minor
- **WHEN** an administrator changes the minor's legal guardians so that this person is no longer among them
- **THEN** that person can no longer edit the minor's data

#### Scenario: Guardian of one child cannot edit an unrelated minor

- **WHEN** a legal guardian opens the detail page of a minor who is not their child
- **THEN** the "Upravit profil" action is not available

