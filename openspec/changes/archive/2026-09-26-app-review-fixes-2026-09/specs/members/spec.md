## MODIFIED Requirements

### Requirement: Member Detail

The system SHALL display complete member details. Inactive members are not accessible to users without MEMBERS:MANAGE authority. The active/inactive status indicator on the detail page is only shown to users with MEMBERS:MANAGE authority.

#### Scenario: Authorized user views active member detail

- **WHEN** user with MEMBERS:READ permission navigates to a member's detail page
- **AND** the member is active
- **THEN** all member information is displayed including personal data, address, contact, and supplementary info

#### Scenario: Admin views suspended member detail

- **WHEN** user with MEMBERS:MANAGE authority navigates to an inactive member's detail page
- **THEN** the member detail is displayed including suspension reason, date, and note

#### Scenario: Regular user cannot access inactive member detail

- **WHEN** user without MEMBERS:MANAGE authority navigates to an inactive member's detail page
- **THEN** the page shows a not-found error

#### Scenario: Edit button shown to authorized user

- **WHEN** user with MEMBERS:UPDATE permission views a member detail page
- **THEN** an "Upravit profil" button is displayed

#### Scenario: Edit button not shown without permission

- **WHEN** user without MEMBERS:UPDATE permission views a member detail page
- **THEN** no edit button is shown for modifying the member's data

#### Scenario: Permissions button shown to authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views a member detail page
- **THEN** an "Oprávnění" button is displayed

#### Scenario: Suspend button shown for active member

- **WHEN** user with MEMBERS:UPDATE permission views an active member's detail page
- **THEN** an "Ukončit členství" button is displayed

#### Scenario: Suspend button not shown for suspended member

- **WHEN** user views a suspended member's detail page
- **THEN** no "Ukončit členství" button is shown

#### Scenario: Admin sees active/inactive status indicator

- **WHEN** user with MEMBERS:MANAGE authority views a member's detail page
- **THEN** an "Aktivní" or "Neaktivní" status indicator is shown next to the member's name

#### Scenario: Regular user does not see active/inactive status indicator

- **WHEN** user without MEMBERS:MANAGE authority views a member's detail page
- **THEN** no active/inactive status indicator is shown next to the member's name
