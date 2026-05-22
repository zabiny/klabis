## ADDED Requirements

### Requirement: Change Password While Authenticated

An authenticated user SHALL be able to change their own password from the user profile by providing their current password together with a new password. The new password MUST satisfy the same complexity rules as the initial password setup. If the current password does not match, the request SHALL be rejected with an error indicating that the current password is incorrect.

After a successful password change, the user SHALL remain logged in for the current session — no forced sign-out occurs.

#### Scenario: User changes password successfully

- **GIVEN** an authenticated user is on their profile page
- **WHEN** the user opens the "Změnit heslo" dialog, fills in current password (matching their stored password), a new password meeting all complexity rules, and confirms the new password
- **THEN** the system updates the stored password to the new value
- **AND** the user remains logged in for the current session

#### Scenario: Current password does not match

- **GIVEN** an authenticated user is on their profile page
- **WHEN** the user submits a password change with a current password that does not match their stored password
- **THEN** the system rejects the change with an error indicating that the current password is incorrect
- **AND** the stored password is not changed

#### Scenario: New password fails complexity rules

- **WHEN** the user submits a password change with a new password that fails one or more complexity rules (length, character classes, contains personal information)
- **THEN** the system rejects the change with the same error messages as the initial password setup form
- **AND** the stored password is not changed

#### Scenario: Confirmation does not match new password

- **WHEN** the user enters a new password in the "new" field and a different value in the "confirm" field
- **THEN** the form shows an error that the confirmation does not match
- **AND** submission is blocked

#### Scenario: Unauthenticated user cannot change a password

- **WHEN** an unauthenticated request is sent to the change-password endpoint
- **THEN** the system responds with an authentication error
