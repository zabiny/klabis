## ADDED Requirements

### Requirement: Member Update API

The system SHALL provide a RESTful API endpoint for updating member information using HTTP PATCH method with HATEOAS
hypermedia controls. The endpoint shall support both member self-edit and administrator editing through role-based field
access control.

#### Scenario: Member updates their own information successfully

- **WHEN** authenticated member submits PATCH request to /api/members/{id}
- **AND** the {id} matches the authenticated user's member ID (OAuth2 subject match)
- **AND** request contains valid updates to member-editable fields
- **THEN** member information is updated
- **AND** HTTP 200 OK status is returned
- **AND** response includes updated member representation
- **AND** response includes HAL+FORMS links (self, edit, collection)

#### Scenario: Admin updates member information successfully

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request to /api/members/{id}
- **AND** request contains valid updates to any editable field (including admin-only fields)
- **THEN** member information is updated
- **AND** HTTP 200 OK status is returned
- **AND** response includes updated member representation
- **AND** response includes HAL+FORMS links (self, edit, collection)

#### Scenario: Member attempts to edit another member's information without admin permission

- **WHEN** authenticated member submits PATCH request to /api/members/{otherId}
- **AND** the {otherId} does NOT match the authenticated user's member ID
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** HTTP 403 Forbidden is returned
- **AND** error response indicates members can only edit their own information or requires admin permission
- **AND** no changes are made to the target member

#### Scenario: Unauthenticated edit attempt

- **WHEN** unauthenticated request attempts to PATCH /api/members/{id}
- **THEN** HTTP 401 Unauthorized is returned
- **AND** response includes authentication challenge

#### Scenario: Invalid data submitted for update

- **WHEN** authenticated member submits PATCH request with invalid data
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field
- **AND** response includes HAL+FORMS template showing correct format
- **AND** no changes are made to the member

### Requirement: Update Authorization

The system SHALL implement dual authorization model for member updates based on user roles and permissions.

#### Scenario: Member self-edit authorization (OAuth2 subject match)

- **WHEN** authenticated user's OAuth2 subject matches the requested member ID
- **THEN** authorization check passes for self-edit
- **AND** user can edit only member-editable fields
- **AND** update operation proceeds

#### Scenario: Admin edit authorization (MEMBERS:UPDATE permission)

- **WHEN** authenticated user has MEMBERS:UPDATE permission
- **THEN** authorization check passes for admin edit
- **AND** user can edit any member (any ID)
- **AND** user can edit both member-editable and admin-only fields
- **AND** update operation proceeds

#### Scenario: Member without admin permission attempts to edit other member

- **WHEN** authenticated user's OAuth2 subject does not match the requested member ID
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** authorization check fails
- **AND** HTTP 403 Forbidden is returned
- **AND** error message indicates insufficient permissions

#### Scenario: Admin can edit any member including themselves

- **WHEN** authenticated user has MEMBERS:UPDATE permission
- **AND** the user's own member ID matches the requested member ID
- **THEN** authorization check passes
- **AND** user can edit admin-only fields on their own record
- **AND** update operation proceeds

### Requirement: Member-Editable Fields

The system SHALL allow members to update the following fields and value objects on their own record:

- Chip number (numeric only)
- Nationality (required)
- Address value object (street, city, postalCode, country - all required)
- Member email address
- Member phone number
- Bank account number
- Legal guardian email
- Legal guardian phone number
- IdentityCard value object (card number, validity date)
- DrivingLicenseGroup enum (B, BE, C, D, etc.)
- MedicalCourse value object (completion date, optional validity date)
- TrainerLicense value object (license number, validity date)
- Dietary restrictions text field (optional, max 500 characters)

#### Scenario: Member updates subset of fields

- **WHEN** member submits PATCH request with only some member-editable fields
- **THEN** only provided fields are updated
- **AND** non-provided fields remain unchanged
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member attempts to update admin-only fields

- **WHEN** member submits PATCH request attempting to modify firstName, lastName, or dateOfBirth
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** these fields are ignored (not modified)
- **AND** only member-editable fields are processed
- **AND** HTTP 200 OK is returned (if other valid fields present)

### Requirement: Admin-Only Fields

The system SHALL allow users with MEMBERS:UPDATE permission to update the following admin-only fields on any member
record:

- First name (required)
- Last name (required)
- Date of birth (required)

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

#### Scenario: Admin updates all admin-only fields

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, and
  dateOfBirth
- **THEN** all three fields are updated
- **AND** all validation rules are enforced
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Non-admin attempts to update admin-only fields

- **WHEN** authenticated user without MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, or
  dateOfBirth
- **THEN** these fields are ignored (not modified)
- **AND** only member-editable fields are processed (if any)
- **AND** HTTP 200 OK is returned (if other valid fields present)

### Requirement: Contact Information Validation

The system SHALL require at least one email address and one phone number to be present after update, either from the
member or their legal guardian.

#### Scenario: Update removes all email addresses

- **WHEN** member submits update that removes all email addresses (both member and guardian)
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one email is required
- **AND** no changes are saved

#### Scenario: Update removes all phone numbers

- **WHEN** member submits update that removes all phone numbers (both member and guardian)
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one phone number is required
- **AND** no changes are saved

#### Scenario: Valid contact update

- **WHEN** member updates their email or phone while maintaining at least one of each
- **THEN** update succeeds
- **AND** new contact information is saved
- **AND** HTTP 200 OK is returned

### Requirement: Nationality and Rodne Cislo Validation

The system SHALL enforce conditional Rodne Cislo field based on nationality during member updates.

#### Scenario: Change to Czech nationality enables Rodne Cislo

- **WHEN** member updates nationality to Czech (CZ)
- **THEN** Rodne Cislo field becomes available for input
- **AND** member may provide Rodne Cislo value
- **AND** validation enforces Rodne Cislo format if provided

#### Scenario: Change from Czech to non-Czech nationality

- **WHEN** member updates nationality from Czech to non-Czech
- **THEN** Rodne Cislo field is cleared if previously present
- **AND** Rodne Cislo becomes unavailable for input
- **AND** update succeeds without Rodne Cislo value

#### Scenario: Rodne Cislo format validation

- **WHEN** member with Czech nationality provides invalid Rodne Cislo format
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates Rodne Cislo format is invalid
- **AND** no changes are saved

### Requirement: Address Update Validation

The system SHALL require all address fields (street, city, postalCode, country) to be provided when updating address.

#### Scenario: Complete address update

- **WHEN** member submits update with all address fields (street, city, postalCode, country)
- **THEN** address is updated
- **AND** validation ensures all fields are present
- **AND** country code is validated as ISO 3166-1 alpha-2

#### Scenario: Partial address update

- **WHEN** member submits update with only some address fields
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates all address fields are required
- **AND** no changes are saved

### Requirement: Chip Number Format Validation

The system SHALL validate that chip number contains only numeric characters when provided.

#### Scenario: Valid chip number

- **WHEN** member provides chip number with numeric characters only
- **THEN** chip number is accepted and saved
- **AND** update succeeds

#### Scenario: Invalid chip number format

- **WHEN** member provides chip number with non-numeric characters
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates chip number must be numeric
- **AND** no changes are saved

### Requirement: New Member Value Objects

The system SHALL support the following value objects and fields for member information:

- **IdentityCard** - ID card number and validity date
- **MedicalCourse** - Medical course completion and optional validity date
- **TrainerLicense** - Trainer license number and validity date
- **DrivingLicenseGroup** - Enum of driving license categories (B, BE, C, D, etc.)
- **DietaryRestrictions** - Text field for dietary restrictions (used for food ordering at accommodation)

#### Scenario: Add IdentityCard value object

- **GIVEN** IdentityCard contains cardNumber and validityDate
- **WHEN** member provides IdentityCard with valid data
- **THEN** IdentityCard value object is created
- **AND** cardNumber is stored as string (not blank, max 50 characters)
- **AND** validityDate is stored as LocalDate in ISO-8601 format
- **AND** validation ensures validityDate is not in the past
- **AND** update succeeds

#### Scenario: Reject IdentityCard with missing card number

- **GIVEN** IdentityCard.cardNumber is null or blank
- **WHEN** member attempts to add IdentityCard
- **THEN** validation fails with HTTP 400
- **AND** error message indicates card number is required

#### Scenario: Reject IdentityCard with expired validity

- **GIVEN** IdentityCard.validityDate is in the past
- **WHEN** member attempts to add IdentityCard
- **THEN** validation fails with HTTP 400
- **AND** error message indicates ID card is expired

#### Scenario: Add MedicalCourse value object

- **GIVEN** MedicalCourse contains completionDate and optional validityDate
- **WHEN** member provides MedicalCourse with valid data
- **THEN** MedicalCourse value object is created
- **AND** completionDate is stored as LocalDate (required)
- **AND** validityDate is stored as Optional<LocalDate> (optional)
- **AND** validation ensures validityDate is after completionDate if provided
- **AND** both dates are validated as ISO-8601 format
- **AND** update succeeds

#### Scenario: Add MedicalCourse without validity date

- **GIVEN** MedicalCourse contains only completionDate
- **WHEN** member provides MedicalCourse without validityDate
- **THEN** MedicalCourse value object is created successfully
- **AND** validityDate remains null (course does not expire)

#### Scenario: Reject MedicalCourse with validity before completion

- **GIVEN** MedicalCourse.validityDate is before completionDate
- **WHEN** member attempts to add MedicalCourse
- **THEN** validation fails with HTTP 400
- **AND** error message indicates validity must be after completion date

#### Scenario: Add TrainerLicense value object

- **GIVEN** TrainerLicense contains licenseNumber and validityDate
- **WHEN** member provides TrainerLicense with valid data
- **THEN** TrainerLicense value object is created
- **AND** licenseNumber is stored as string (not blank, max 50 characters)
- **AND** validityDate is stored as LocalDate in ISO-8601 format
- **AND** validation ensures validityDate is not in the past
- **AND** update succeeds

#### Scenario: Reject TrainerLicense with missing license number

- **GIVEN** TrainerLicense.licenseNumber is null or blank
- **WHEN** member attempts to add TrainerLicense
- **THEN** validation fails with HTTP 400
- **AND** error message indicates license number is required

#### Scenario: Reject TrainerLicense with expired validity

- **GIVEN** TrainerLicense.validityDate is in the past
- **WHEN** member attempts to add TrainerLicense
- **THEN** validation fails with HTTP 400
- **AND** error message indicates license is expired

#### Scenario: Set DrivingLicenseGroup

- **WHEN** member provides drivingLicenseGroup value
- **THEN** driving license group is saved as enum
- **AND** validation ensures value is from allowed set (B, BE, C, C1, D, D1, etc.)
- **AND** update succeeds

#### Scenario: Reject invalid DrivingLicenseGroup

- **WHEN** member provides drivingLicenseGroup not in allowed set
- **THEN** validation fails with HTTP 400
- **AND** error message lists valid driving license groups

#### Scenario: Set dietary restrictions

- **WHEN** member provides dietaryRestrictions text field
- **THEN** dietary restrictions are saved as text string
- **AND** field can contain any text (optional, max 500 characters)
- **AND** update succeeds

#### Scenario: Clear dietary restrictions

- **WHEN** member provides dietaryRestrictions as null or empty string
- **THEN** dietary restrictions are cleared
- **AND** field is set to null
- **AND** update succeeds

### Requirement: PATCH Request Semantics

The system SHALL support HTTP PATCH semantics where only fields present in the request body are updated.

#### Scenario: Partial update with single field

- **WHEN** member submits PATCH request with only chipNumber field
- **THEN** only chipNumber is updated
- **AND** all other fields remain unchanged
- **AND** response includes complete updated member representation

#### Scenario: Partial update with multiple fields

- **WHEN** member submits PATCH request with chipNumber and address fields
- **THEN** only chipNumber and address are updated
- **AND** all other fields remain unchanged
- **AND** response includes complete updated member representation

#### Scenario: Empty request body

- **WHEN** member submits PATCH request with empty request body
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one field must be provided for update

### Requirement: Member Update Response Format

The member update endpoint SHALL return complete updated member information in HATEOAS-compliant HAL+FORMS format.

#### Scenario: Response includes updated member data

- **WHEN** member information is successfully updated via PATCH
- **THEN** response Content-Type is application/prs.hal-forms+json
- **AND** response includes all member fields with updated values
- **AND** response includes _links object with hypermedia controls
- **AND** response includes _templates object for further actions

#### Scenario: Response includes hypermedia links

- **WHEN** member update succeeds
- **THEN** response includes `self` link to the updated member resource
- **AND** response includes `edit` link for further updates
- **AND** response includes `collection` link to members list

#### Scenario: Response includes dynamic HAL+FORMS templates based on user role

- **WHEN** member update succeeds
- **THEN** response includes _templates with update template
- **AND** template shows all updatable fields based on user role
- **AND** for members: template shows member-editable fields only
- **AND** for admins: template shows both member-editable and admin-only fields
- **AND** template indicates required vs optional fields
- **AND** template indicates which fields are read-only for current user

#### Scenario: Admin response includes admin-only fields in template

- **WHEN** authenticated user with MEMBERS:UPDATE permission retrieves member update template
- **THEN** template includes firstName, lastName, and dateOfBirth fields
- **AND** template includes all member-editable fields
- **AND** all fields are marked as editable

#### Scenario: Member response excludes admin-only fields from template

- **WHEN** authenticated member without admin permission retrieves update template
- **THEN** template excludes firstName, lastName, and dateOfBirth fields
- **AND** template includes only member-editable fields
- **AND** member-editable fields are marked as editable

### Requirement: Update Error Responses

The system SHALL return detailed error responses using problem+json format for validation and authorization failures.

#### Scenario: Validation error with multiple issues

- **WHEN** member submits update with multiple validation errors
- **THEN** HTTP 400 Bad Request is returned
- **AND** Content-Type is application/problem+json
- **AND** response includes array of validation errors
- **AND** each error indicates field and specific issue
- **AND** no changes are saved

#### Scenario: Authorization error includes helpful message

- **WHEN** member attempts to edit another member's information
- **THEN** HTTP 403 Forbidden is returned
- **AND** Content-Type is application/problem+json
- **AND** error message explains self-edit restriction
- **AND** error includes reference to correct member ID for editing

### Requirement: Concurrent Update Handling

The system SHALL handle concurrent updates to member information safely.

#### Scenario: Optimistic locking on concurrent updates

- **WHEN** two users attempt to update the same member simultaneously
- **THEN** the first update succeeds
- **AND** the second update receives HTTP 409 Conflict
- **AND** error message indicates the member was modified by another user
- **AND** error response includes current member data

#### Scenario: Retry after conflict

- **WHEN** member receives 409 Conflict response
- **THEN** member can retrieve current member data via GET
- **AND** member can submit PATCH with updated values
- **AND** subsequent update may succeed if no further conflicts occur
