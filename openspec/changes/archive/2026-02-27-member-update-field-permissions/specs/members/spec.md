## MODIFIED Requirements

### Requirement: Member-Editable Fields

The system SHALL allow members to update the following fields on their own record:

- Email address
- Phone number
- Address (street, city, postalCode, country — all required when updating address)
- Chip number (numeric only)
- Nationality (ISO 3166-1 alpha-2)
- Bank account number
- Legal guardian email
- Legal guardian phone number
- Identity card (card number and validity date)
- Driving license group (B, BE, C, C1, D, D1, etc.)
- Medical course (completion date and optional validity date)
- Trainer license (license number and validity date)
- Dietary restrictions text field (optional, max 500 characters)

#### Scenario: Member updates chip number

- **WHEN** authenticated member submits PATCH request to /api/members/{id} with chipNumber field
- **AND** the {id} matches the authenticated user's member ID
- **THEN** chip number is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates identity card

- **WHEN** authenticated member submits PATCH request with identityCard (cardNumber, validityDate)
- **AND** the {id} matches the authenticated user's member ID
- **THEN** identity card information is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates driving license group

- **WHEN** authenticated member submits PATCH request with drivingLicenseGroup field
- **AND** the {id} matches the authenticated user's member ID
- **THEN** driving license group is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates medical course

- **WHEN** authenticated member submits PATCH request with medicalCourse (completionDate, optional validityDate)
- **AND** the {id} matches the authenticated user's member ID
- **THEN** medical course information is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates trainer license

- **WHEN** authenticated member submits PATCH request with trainerLicense (licenseNumber, validityDate)
- **AND** the {id} matches the authenticated user's member ID
- **THEN** trainer license information is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates nationality

- **WHEN** authenticated member submits PATCH request with nationality field
- **AND** the {id} matches the authenticated user's member ID
- **THEN** nationality is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member changes nationality from Czech to non-Czech and birth number is cleared

- **WHEN** authenticated member submits PATCH request changing nationality from CZ to a non-Czech value
- **AND** the member previously had a birth number stored
- **THEN** birth number is cleared
- **AND** nationality is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates guardian contact information

- **WHEN** authenticated member submits PATCH request with guardian email and/or phone
- **AND** the {id} matches the authenticated user's member ID
- **THEN** guardian contact information is updated
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member updates subset of fields

- **WHEN** member submits PATCH request with only some member-editable fields
- **THEN** only provided fields are updated
- **AND** non-provided fields remain unchanged
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member attempts to update admin-only fields

- **WHEN** member submits PATCH request containing firstName, lastName, dateOfBirth, gender, or birthNumber
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** these fields are silently ignored
- **AND** only member-editable fields are processed
- **AND** HTTP 200 OK is returned if at least one valid member-editable field is present

### Requirement: Admin-Only Fields

The system SHALL allow users with MEMBERS:UPDATE permission to update the following admin-only fields on any member record:

- First name (required, not blank)
- Last name (required, not blank)
- Date of birth (required, valid ISO-8601 date, not in the future)
- Gender (MALE, FEMALE, OTHER)
- Birth number (Czech ID number — only for Czech nationality, cryptographically validated format)

#### Scenario: Admin updates firstName on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName field
- **THEN** firstName is updated
- **AND** validation ensures firstName is not blank
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates lastName on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with lastName field
- **THEN** lastName is updated
- **AND** validation ensures lastName is not blank
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates dateOfBirth on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with dateOfBirth field
- **THEN** dateOfBirth is updated
- **AND** validation ensures dateOfBirth is valid ISO-8601 date format
- **AND** validation ensures dateOfBirth is not in the future
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates gender on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with gender field
- **THEN** gender is updated to one of MALE, FEMALE, OTHER
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates birth number on Czech member

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with birthNumber field
- **AND** member has Czech nationality (CZ)
- **THEN** birth number is validated, encrypted, and stored
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin attempts to set birth number on non-Czech member

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with birthNumber field
- **AND** member does NOT have Czech nationality
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates birth number is only allowed for Czech nationals

#### Scenario: Admin updates all admin-only fields

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, dateOfBirth, gender
- **THEN** all fields are updated
- **AND** all validation rules are enforced
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Non-admin attempts to update admin-only fields

- **WHEN** authenticated user without MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, dateOfBirth, gender, or birthNumber
- **THEN** these fields are silently ignored
- **AND** only member-editable fields are processed if present
- **AND** HTTP 200 OK is returned if at least one valid member-editable field is present
