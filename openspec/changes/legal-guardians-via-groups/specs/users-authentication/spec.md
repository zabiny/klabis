## MODIFIED Requirements

### Requirement: Membership Status Claim in UserInfo Response

The OIDC UserInfo endpoint SHALL include an `is_member` boolean claim when the `profile` scope is authorized.

#### Scenario: Member user receives is_member true claim

- **WHEN** user with an associated member profile requests UserInfo with `profile` scope
- **THEN** the response includes `"is_member": true`
- **AND** standard profile claims (given_name, family_name, updated_at) are included

#### Scenario: Non-member legal guardian receives their name

- **WHEN** a non-member legal guardian requests UserInfo with `profile` scope
- **THEN** the response includes `"is_member": false`
- **AND** given_name and family_name are taken from the guardian's profile

#### Scenario: Admin user without member profile receives is_member false claim

- **WHEN** user without an associated member or legal guardian profile requests UserInfo with `profile` scope
- **THEN** the response includes `"is_member": false`
- **AND** member-specific profile claims are not included

#### Scenario: UserInfo without profile scope does not include is_member claim

- **WHEN** user requests UserInfo without `profile` scope
- **THEN** the `is_member` claim is not included in the response

### Requirement: Membership Detection Based on Member Aggregate Existence

The system SHALL determine membership status by checking whether a Member aggregate exists for the authenticated user's account, regardless of the login name the user logs in with.

#### Scenario: User with a matching member record is identified as a member

- **WHEN** the authenticated user's account belongs to a member record
- **THEN** the system identifies the user as a member (`is_member: true`)

#### Scenario: Former guardian who became a member is identified as a member

- **GIVEN** a non-member legal guardian who was later registered as a club member and still logs in with their e-mail
- **WHEN** they log in
- **THEN** the system identifies them as a member (`is_member: true`)

#### Scenario: User without a matching member record is identified as non-member

- **WHEN** the authenticated user's account does not belong to any member record
- **THEN** the system identifies the user as a non-member (`is_member: false`)
