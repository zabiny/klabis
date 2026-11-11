## MODIFIED Requirements

### Requirement: Membership Detection Based on Member Aggregate Existence

The system SHALL determine membership status by checking whether a Member aggregate exists for the authenticated user's account, regardless of the login name the user logs in with.

#### Scenario: User with a matching member record is identified as a member

- **WHEN** the authenticated user's account belongs to a member record
- **THEN** the system identifies the user as a member (`is_member: true`)

#### Scenario: Former guardian who became a member is identified as a member

- **GIVEN** a non-member legal guardian who was later registered as a club member and logs in with their former login number EXTnnnn
- **WHEN** they log in
- **THEN** the system identifies them as a member (`is_member: true`)

#### Scenario: User without a matching member record is identified as non-member

- **WHEN** the authenticated user's account does not belong to any member record
- **THEN** the system identifies the user as a non-member (`is_member: false`)
