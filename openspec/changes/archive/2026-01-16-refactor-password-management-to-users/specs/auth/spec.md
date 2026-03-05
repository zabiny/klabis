## MODIFIED Requirements

### Requirement: Password Complexity Validation

The system SHALL enforce password complexity requirements to ensure user account security.

Password complexity requirements:

- Minimum length: 12 characters
- Maximum length: 128 characters
- Must contain: at least one uppercase letter, one lowercase letter, one digit, one special character
- Must NOT contain: user's registration number, first name, or last name
- Implementation: Validation performed by `PasswordComplexityValidator` in `com.klabis.users.domain`

#### Scenario: Valid password accepted

- **GIVEN** a user is setting or changing their password
- **AND** the password meets all complexity requirements
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL pass
- **AND** the password SHALL be accepted
- **AND** the validator SHALL be located in users.domain package

#### Scenario: Password too short rejected

- **GIVEN** a user is setting their password
- **AND** the password is less than 12 characters
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate minimum length requirement

#### Scenario: Password too long rejected

- **GIVEN** a user is setting their password
- **AND** the password exceeds 128 characters
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate maximum length requirement

#### Scenario: Password missing uppercase rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no uppercase letters
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate uppercase letter requirement

#### Scenario: Password missing lowercase rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no lowercase letters
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate lowercase letter requirement

#### Scenario: Password missing digit rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no digits
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate digit requirement

#### Scenario: Password missing special character rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no special characters
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate special character requirement

#### Scenario: Password contains registration number rejected

- **GIVEN** a user is setting their password
- **AND** the password contains the user's registration number
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate password cannot contain personal information
- **AND** the validator SHALL check against user's registration number from User entity

#### Scenario: Password contains first name rejected

- **GIVEN** a user is setting their password
- **AND** the password contains the user's first name (from linked Member entity)
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate password cannot contain personal information
- **AND** the validator SHALL access member data via registration number lookup

#### Scenario: Password contains last name rejected

- **GIVEN** a user is setting their password
- **AND** the password contains the user's last name (from linked Member entity)
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate password cannot contain personal information
- **AND** the validator SHALL access member data via registration number lookup

#### Scenario: Multiple validation errors reported

- **GIVEN** a user is setting their password
- **AND** the password fails multiple complexity requirements
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL fail
- **AND** error messages SHALL be provided for ALL failed requirements
- **AND** the user SHALL see all issues in a single response
