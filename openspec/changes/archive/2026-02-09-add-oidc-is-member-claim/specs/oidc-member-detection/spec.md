## ADDED Requirements

### Requirement: Membership status claim in userinfo response
The OIDC userinfo endpoint SHALL include an `is_member` boolean claim when the `profile` scope is authorized, indicating whether the authenticated user has an associated Member profile.

#### Scenario: Member user requests userinfo with profile scope
- **WHEN** a user with an associated Member profile requests the `/userinfo` endpoint with `profile` scope authorized
- **THEN** the response SHALL include `"is_member": true` claim
- **AND** the response SHALL include standard profile claims (`given_name`, `family_name`, `updated_at`)

#### Scenario: Admin user requests userinfo with profile scope
- **WHEN** a user without an associated Member profile requests the `/userinfo` endpoint with `profile` scope authorized
- **THEN** the response SHALL include `"is_member": false` claim
- **AND** the response SHALL NOT include Member-specific profile claims (`given_name`, `family_name`, `updated_at`)

#### Scenario: User requests userinfo without profile scope
- **WHEN** a user requests the `/userinfo` endpoint without `profile` scope authorized
- **THEN** the response SHALL NOT include the `is_member` claim
- **AND** the response SHALL only include the `sub` claim

### Requirement: Membership detection based on Member aggregate existence
The system SHALL determine membership status by checking for the existence of a Member aggregate with a registration number matching the authenticated user's username.

#### Scenario: Username matches registration number format and Member exists
- **WHEN** the authenticated username matches the registration number format (XXXYYDD)
- **AND** a Member aggregate exists with that registration number
- **THEN** the system SHALL set `is_member` to `true`

#### Scenario: Username matches registration number format but Member does not exist
- **WHEN** the authenticated username matches the registration number format (XXXYYDD)
- **AND** no Member aggregate exists with that registration number
- **THEN** the system SHALL set `is_member` to `false`

#### Scenario: Username does not match registration number format
- **WHEN** the authenticated username does not match the registration number format
- **THEN** the system SHALL set `is_member` to `false`
- **AND** the system SHALL NOT query the Member repository
