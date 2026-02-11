# member-management Delta Specification

## Purpose

This delta spec modifies the member-management capability to add conditional HATEOAS link to user permissions endpoint. Uses existing `id` field (which is UserId due to 1:1 Member-User relationship).

## MODIFIED Requirements

### Requirement: Member Details Response Format

The member details endpoint SHALL return complete member information in a HATEOAS-compliant HAL+FORMS format with proper ISO-8601 date serialization, structured address and contact information. The `id` field represents UserId (1:1 relationship) and can be used for cross-aggregate navigation.

#### Scenario: Response contains all personal information with structured address

- **WHEN** a member details response is returned
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID), also UserId due to 1:1 relationship
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth as ISO-8601 date string (YYYY-MM-DD)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE, OTHER)
    - **`address` - Object containing street, city, postalCode, country (ISO 3166-1 alpha-2)**
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - **`email` - Single email address string**
    - **`phone` - Single phone number string**
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `active` - Boolean indicating if member is active
    - `guardian` - Guardian information object (present if member has guardian)

#### Scenario: Address serialized as structured object

- **WHEN** a member has address with street="Hlavní 123", city="Praha", postalCode="11000", country="CZ"
- **THEN** the response SHALL serialize address as:
  ```json
  "address": {
    "street": "Hlavní 123",
    "city": "Praha",
    "postalCode": "11000",
    "country": "CZ"
  }
  ```

#### Scenario: Email serialized as single string

- **WHEN** a member has email "john@example.com"
- **THEN** the response SHALL serialize email as:
  ```json
  "email": "john@example.com"
  ```

#### Scenario: Phone serialized as single string

- **WHEN** a member has phone "+420123456789"
- **THEN** the response SHALL serialize phone as:
  ```json
  "phone": "+420123456789"
  ```

### Requirement: HATEOAS Links for Member Details

The member details response SHALL include hypermedia links following HAL+FORMS specification to enable API discoverability and navigation, **including conditional link to user permissions**.

#### Scenario: Self link included

- **WHEN** the get member endpoint returns a response
- **THEN** the response SHALL include a `self` link pointing to /api/members/{id}
- **AND** the link SHALL use the rel "self"

#### Scenario: Collection link included

- **WHEN** the get member endpoint returns a response
- **THEN** the response SHALL include a `collection` link pointing to /api/members
- **AND** the link SHALL enable navigation back to the members list

#### Scenario: Edit link conditionally included

- **WHEN** a user with MEMBERS:UPDATE permission views a member
- **THEN** the response SHALL include an `edit` link
- **AND** the link SHALL point to the member resource for update operations

#### Scenario: Edit link excluded for unauthorized users

- **WHEN** a user without MEMBERS:UPDATE permission views a member
- **THEN** the response SHALL NOT include an `edit` link
- **AND** HAL+FORMS templates SHALL reflect available actions only

**#### Scenario: Permissions link conditionally included**

- **WHEN** an authenticated user with MEMBERS:PERMISSIONS authority views a member
- **THEN** the response SHALL include a `permissions` link
- **AND** the link SHALL point to /api/users/{id}/permissions (where id = member.id = userId)
- **AND** the link SHALL use the rel "permissions"
- **AND** the link SHALL enable navigation to user permissions management

**#### Scenario: Permissions link excluded for users without permission**

- **WHEN** an authenticated user without MEMBERS:PERMISSIONS authority views a member
- **THEN** the response SHALL NOT include a `permissions` link
- **AND** only links for authorized actions are included

**#### Scenario: Unauthenticated users receive no permissions link**

- **WHEN** an unauthenticated user views a member (if allowed)
- **THEN** the response SHALL NOT include a `permissions` link
- **AND** only publicly available links are included
