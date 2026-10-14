# Spec Delta

## MODIFIED Requirements

### Requirement: Member Detail Response — Conditional Edit Template

The system SHALL include a PATCH template in the member detail response only for users authorized to edit the member — users with MEMBERS:MANAGE authority or users holding "Úprava údajů člena" over the member — and the template SHALL contain the fields the caller is permitted to modify. Administrator-reserved fields SHALL be included as read-only for a caller who may see but not modify them.

#### Scenario: Admin retrieves member detail

- **WHEN** user with MEMBERS:MANAGE authority requests a member detail
- **THEN** response includes a PATCH template with all editable fields

#### Scenario: Member retrieves own profile

- **WHEN** an authenticated adult member requests their own member detail
- **THEN** response includes a PATCH template with the member-editable fields
- **AND** the administrator-reserved fields (firstName, lastName, dateOfBirth, gender, birthNumber) are present as read-only

#### Scenario: Holder of profile editing retrieves another member's detail

- **WHEN** a user holding "Úprava údajů člena" over a member, without MEMBERS:MANAGE, requests that member's detail
- **THEN** response includes a PATCH template with the member-editable fields
- **AND** the administrator-reserved fields are present as read-only

#### Scenario: Minor retrieves own profile

- **WHEN** a minor requests their own member detail
- **THEN** response does not include a PATCH template

#### Scenario: Member retrieves another member's profile

- **WHEN** authenticated member requests a different member's detail without admin permission and without "Úprava údajů člena" over that member
- **THEN** response does not include a PATCH template
