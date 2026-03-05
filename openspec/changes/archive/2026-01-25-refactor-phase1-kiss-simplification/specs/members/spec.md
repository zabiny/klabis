# Members Spec Delta

## REMOVED Requirements

### Requirement: CQRS Handler Pattern

**Reason**: CQRS pattern adds unnecessary complexity for simple CRUD operations with no complex read models or separate
databases. Migrating to simpler service-based architecture.

**Migration**: All handler logic migrated to `MemberService`. Controllers updated to use services directly. No API
changes.

### Requirement: Command DTO Transformation

**Reason**: Command/Query DTOs create unnecessary transformation layer. Request objects contain all necessary validation
and can be used directly in services.

**Migration**: Services now accept `RegisterMemberRequest` and `UpdateMemberRequest` directly. Commands
`RegisterMemberCommand`, `UpdateMemberCommand`, `GetMemberQuery`, `ListMembersQuery` deleted.

## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a User entity first, then creating a Member entity that uses
the User's UserId. This flow is implemented in `MemberService.registerMember()` which accepts `RegisterMemberRequest`
directly without Command DTO transformation.

#### Scenario: Member registration creates User then Member

- **WHEN** authenticated user with MEMBERS:CREATE permission submits member data via POST /api/members
- **THEN** `MemberService.registerMember()` creates a User entity with generated UserId
- **AND** `MemberService.registerMember()` creates a Member entity with the same UserId
- **AND** member is created with generated registration number
- **AND** response includes HAL+FORMS links for viewing, editing, and related actions
- **AND** HTTP 201 Created status is returned with Location header

#### Scenario: Unauthorized user attempts creation

- **WHEN** user without MEMBERS:CREATE permission attempts to create member
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes error details with problem+json media type

#### Scenario: Invalid data submission

- **WHEN** user submits incomplete or invalid member data
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field
- **AND** response includes HAL+FORMS template showing required fields

#### Scenario: User creation failure prevents member creation

- **WHEN** User creation fails during member registration
- **THEN** Member creation is not attempted
- **AND** transaction is rolled back

### Requirement: Member Query Operations

The system SHALL provide member query operations through `MemberService` with methods for retrieving individual members
and listing members with pagination.

#### Scenario: Get member by ID

- **WHEN** authenticated user with MEMBERS:READ permission requests GET /api/members/{id}
- **THEN** `MemberService.getMember()` returns member details
- **AND** response includes complete member information including personal details, contact info, guardian (if
  applicable), certifications
- **AND** HTTP 200 OK is returned

#### Scenario: List members with pagination

- **WHEN** authenticated user with MEMBERS:READ permission requests GET /api/members?page=0&size=20
- **THEN** `MemberService.listMembers()` returns paginated member list
- **AND** response includes member summary information (ID, name, registration number)
- **AND** pagination metadata included in HAL+FORMS response

#### Scenario: Member not found

- **WHEN** requested member ID does not exist
- **THEN** HTTP 404 Not Found is returned
- **AND** response includes error details

### Requirement: Member Update Flow

The system SHALL process member updates through `MemberService.updateMember()` which accepts `UpdateMemberRequest`
directly. The system implements field-level authorization where non-admin users can only update specific fields.

#### Scenario: Admin updates all member fields

- **WHEN** authenticated user with ROLE_ADMIN and MEMBERS:UPDATE permission submits PATCH /api/members/{id}
- **THEN** `MemberService.updateMember()` allows updates to all fields
- **AND** only provided fields are updated (PATCH semantics)
- **AND** response includes updated member information
- **AND** HTTP 200 OK is returned

#### Scenario: Non-admin updates allowed fields only

- **WHEN** authenticated non-admin user with MEMBERS:UPDATE permission attempts to update member
- **THEN** `MemberService.updateMember()` allows updates only to member-editable fields (email, phone, address)
- **AND** attempts to update admin-only fields are rejected
- **AND** HTTP 403 Forbidden is returned for admin-only fields

#### Scenario: Empty update request rejected

- **WHEN** update request contains no fields to update
- **THEN** HTTP 400 Bad Request is returned
- **AND** response indicates at least one field must be provided

## ADDED Requirements

### Requirement: Value Object Inline Validation

Value objects SHALL contain validation logic directly in their canonical constructors instead of using external
validation utilities.

#### Scenario: EmailAddress validates format

- **WHEN** `EmailAddress` is created with invalid email format
- **THEN** `IllegalArgumentException` is thrown
- **AND** validation logic is inline in `EmailAddress` constructor
- **AND** no external `StringValidator` utility is used

#### Scenario: PhoneNumber validates format

- **WHEN** `PhoneNumber` is created with invalid phone format
- **THEN** `IllegalArgumentException` is thrown
- **AND** validation checks for E.164 format (+ prefix)
- **AND** validation logic is inline in `PhoneNumber` constructor
