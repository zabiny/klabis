## ADDED Requirements

### Requirement: ISO-8601 Date and DateTime Serialization

All API endpoints SHALL serialize date and datetime fields using ISO-8601 format.

#### Scenario: Date fields serialized as ISO-8601 date strings

- **WHEN** any API endpoint returns a date field (e.g., member's dateOfBirth, event date)
- **THEN** the field is serialized as an ISO-8601 date string in format `YYYY-MM-DD`
- **EXAMPLE**: January 15, 1990 is serialized as `"1990-01-15"`

#### Scenario: Date fields handle edge cases correctly

- **WHEN** a date field represents a leap year date (e.g., February 29, 2000)
- **THEN** the field is serialized as `"2000-02-29"` in valid ISO-8601 format

#### Scenario: DateTime fields serialized as ISO-8601 datetime strings

- **WHEN** any API endpoint returns a datetime field (e.g., registration timestamp, suspendedAt)
- **THEN** the field is serialized as an ISO-8601 datetime string in format `YYYY-MM-DDTHH:MM:SS`
- **EXAMPLE**: May 15, 1990 at 14:30:00 is serialized as `"1990-05-15T14:30:00"`

#### Scenario: DateTime fields with timezone offset include the offset

- **WHEN** a datetime field includes a timezone offset
- **THEN** the field is serialized with the offset (e.g., `"1990-05-15T14:30:00+02:00"`)

#### Scenario: All date/time fields use consistent format across all endpoints

- **WHEN** multiple API endpoints return date/time fields
- **THEN** all fields use ISO-8601 format consistently
- **AND** clients can parse all values using standard ISO-8601 parsers

### Requirement: HAL+FORMS Media Type and Response Structure

All API endpoints SHALL use the HAL+FORMS media type and response structure.

#### Scenario: Member list response uses HAL+FORMS media type

- **WHEN** the list members endpoint is called
- **THEN** the response Content-Type is `application/prs.hal-forms+json`
- **AND** the response follows HAL+FORMS specification for paged collections
- **AND** the response includes `_embedded` object containing member summaries
- **AND** the response includes `_links` object for hypermedia navigation

#### Scenario: Member list response includes pagination metadata

- **WHEN** the list members endpoint returns a paginated response
- **THEN** the response includes a `page` object with fields: size, totalElements, totalPages, number

#### Scenario: Event response uses HAL+FORMS media type

- **WHEN** any event endpoint returns a response
- **THEN** Content-Type is `application/prs.hal-forms+json`
- **AND** response includes `_links` object for hypermedia navigation

#### Scenario: Calendar item response uses HAL+FORMS media type

- **WHEN** any calendar endpoint returns a response
- **THEN** Content-Type is `application/prs.hal-forms+json`
- **AND** response includes `_links` object for hypermedia navigation

#### Scenario: Registration response uses HAL+FORMS media type

- **WHEN** any event registration endpoint returns a response
- **THEN** Content-Type is `application/prs.hal-forms+json`
- **AND** response includes `_links` object for hypermedia navigation

### Requirement: HATEOAS Link Structure

The system SHALL include hypermedia links in API responses reflecting the current user's authorized actions.

#### Scenario: Member creation response includes hypermedia links

- **WHEN** a member is created successfully
- **THEN** the response includes a `self` link to the member resource
- **AND** the response includes an `edit` link if the user has MEMBERS:UPDATE permission
- **AND** the response includes a `collection` link to the members list

#### Scenario: Unauthorized user receives limited links

- **WHEN** user without edit permission views a member
- **THEN** the response includes the `self` link
- **AND** the response does not include the `edit` link

#### Scenario: Member details response includes permissions link for authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views a member detail
- **THEN** the response includes a `permissions` link pointing to `/api/users/{id}/permissions`

#### Scenario: Permissions link excluded for unauthorized user

- **WHEN** user without MEMBERS:PERMISSIONS authority views a member detail
- **THEN** the response does not include a `permissions` link

#### Scenario: Member details response includes suspend link for active member

- **WHEN** user with MEMBERS:UPDATE permission views an active member
- **THEN** the response includes a `suspend` link

#### Scenario: Suspend link excluded for suspended member

- **WHEN** user views a suspended member
- **THEN** the response does not include a `suspend` link

#### Scenario: Event summary includes registration affordance when open

- **WHEN** an event in the list has open registrations and the current user is not registered
- **THEN** the event summary self link includes a `registerForEvent` affordance

#### Scenario: Event summary includes unregistration affordance when registered

- **WHEN** an event in the list has open registrations and the current user is already registered
- **THEN** the event summary self link includes an `unregisterFromEvent` affordance

#### Scenario: DRAFT event links for manager

- **WHEN** user with EVENTS:MANAGE permission views a DRAFT event
- **THEN** response includes links: self, edit, publish, cancel, registrations

#### Scenario: ACTIVE event links for manager

- **WHEN** user with EVENTS:MANAGE permission views an ACTIVE event
- **THEN** response includes links: self, edit, cancel, finish, registrations

#### Scenario: FINISHED or CANCELLED event links

- **WHEN** user views a FINISHED or CANCELLED event
- **THEN** response includes links: self, registrations
- **AND** edit, publish, cancel, finish links are not included

#### Scenario: Event-linked calendar item includes link to event

- **WHEN** an event-linked calendar item is returned
- **THEN** response includes `_links.event.href` pointing to `/api/events/{eventId}`

#### Scenario: Calendar response includes month navigation links

- **WHEN** user views calendar items for June 2026
- **THEN** response includes HATEOAS `next` link with startDate=2026-07-01&endDate=2026-07-31
- **AND** response includes HATEOAS `prev` link with startDate=2026-05-01&endDate=2026-05-31
- **AND** links preserve the same sort parameter as the original request

#### Scenario: Member list response includes pagination links

- **WHEN** the list members endpoint returns a paginated response
- **THEN** the response `_links` object includes: self, first, last
- **AND** includes `prev` link if not on the first page
- **AND** includes `next` link if not on the last page
- **AND** all pagination links preserve the sort and size query parameters

### Requirement: Pagination Metadata Format

Member list and event list endpoints SHALL return pagination metadata in the `page` object.

#### Scenario: Page metadata included in paginated response

- **WHEN** a paginated list endpoint returns a response
- **THEN** the response includes a `page` object with: size (items per page), totalElements (total across all pages), totalPages (total number of pages), number (current page, zero-based)

#### Scenario: Empty collection pagination metadata

- **WHEN** a paginated list returns an empty result set
- **THEN** the response includes page metadata with totalElements=0 and totalPages=0
- **AND** `_embedded` collection array is empty

### Requirement: Concurrent Update Handling (Optimistic Locking)

The system SHALL handle concurrent updates to member information safely using optimistic locking.

#### Scenario: First concurrent update wins

- **WHEN** two users attempt to update the same member simultaneously
- **THEN** the first update succeeds
- **AND** the second update receives a conflict error indicating the member was modified by another user

#### Scenario: User can retry after a conflict

- **WHEN** user receives a conflict error on a member update
- **THEN** the user can retrieve the current member data and resubmit the update with the latest values

#### Scenario: Concurrent suspension attempts result in conflict

- **WHEN** two users attempt to suspend the same member simultaneously
- **THEN** the first suspension succeeds
- **AND** the second suspension receives a conflict error indicating the member was already suspended

### Requirement: Value Object Technical Details

The system SHALL validate and enforce value object constraints for Member, User, Event, and CalendarItem domain objects.

#### Scenario: UserId wraps a UUID value

- **WHEN** a UserId is created with a valid UUID
- **THEN** the identifier is created successfully, is immutable, and provides equality based on the wrapped UUID

#### Scenario: UserId prevents null UUID

- **WHEN** a UserId is created with a null UUID
- **THEN** validation fails with an error indicating UUID cannot be null

#### Scenario: Member and User share the same UserId

- **WHEN** a Member is created as part of member registration
- **THEN** the Member's UserId equals the User's UserId (same UUID value)
- **AND** no join operation is required for queries by user ID

#### Scenario: EventId wraps a UUID value

- **WHEN** an EventId is created with a valid UUID
- **THEN** the identifier is created successfully, is immutable, and provides equality based on the wrapped UUID

#### Scenario: EventId prevents null UUID

- **WHEN** an EventId is created with a null UUID
- **THEN** validation fails with an error indicating UUID cannot be null

#### Scenario: CalendarItemId wraps a UUID value

- **WHEN** a CalendarItemId is created with a valid UUID
- **THEN** the identifier is created successfully, is immutable, and provides equality based on the wrapped UUID

#### Scenario: Address value object validates required fields

- **WHEN** an Address is created with a blank street, city, postal code, or country
- **THEN** validation fails with an error indicating which field is required

#### Scenario: Address validates country code format

- **WHEN** an Address is created with a country code that is not a valid ISO 3166-1 alpha-2 code
- **THEN** validation fails with an error indicating the country code is invalid

#### Scenario: Address validates field length constraints

- **WHEN** an Address is created with a street exceeding 200 characters, city exceeding 100 characters, or postal code exceeding 20 characters
- **THEN** validation fails with an error indicating which field exceeds maximum length

#### Scenario: EmailAddress validates RFC 5322 format

- **WHEN** an email address value is created without an @ symbol or without a domain
- **THEN** validation fails with an appropriate error message

#### Scenario: PhoneNumber validates E.164 format

- **WHEN** a phone number value is created without a leading + prefix or containing letters
- **THEN** validation fails with an appropriate error message

#### Scenario: WebsiteUrl validates http/https scheme

- **WHEN** a website URL value is created with a non-http/https scheme or invalid format
- **THEN** validation fails with an error indicating only http/https URLs are accepted

#### Scenario: SiCardNumber validates digit-only content and length

- **WHEN** a SI card number is created with non-digit characters or a length outside 4–8 characters
- **THEN** validation fails with an appropriate error message

#### Scenario: Type-safe identifiers prevent accidental cross-type usage

- **WHEN** calling member, event, or user services
- **THEN** each service requires its own specific identifier type
- **AND** the system prevents accidental use of an identifier of the wrong type

### Requirement: Backward Compatibility

The member list API SHALL maintain backward compatibility with clients not using pagination parameters.

#### Scenario: Default pagination is transparent to legacy clients

- **WHEN** an existing client calls the member list endpoint without pagination parameters
- **THEN** the system returns the first page of results (page 0, size 10)
- **AND** the response structure remains compatible (`_embedded.members` array)
- **AND** clients not parsing pagination metadata still receive valid member data
