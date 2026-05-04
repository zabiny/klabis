# Non-Functional Requirements Specification

## Purpose

Captures technical API requirements shared across all bounded contexts: response format standards, serialization conventions, hypermedia link structure, pagination metadata, concurrent update handling, value object technical details, backward compatibility, and server infrastructure (HTTPS, TLS, SSL).

## Requirements

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

- **WHEN** a SI card number is created with non-digit characters or a length outside 4-8 characters
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

### Requirement: HTTPS Protocol Enforcement

The system SHALL use HTTPS on port 8443 when the `ssl` profile is active, and HTTP on port 8080 without the `ssl` profile.

#### Scenario: Application runs on HTTPS when ssl profile is active

- **WHEN** the application starts with the `ssl` profile
- **THEN** the application is accessible via HTTPS on port 8443
- **AND** the HTTP port 8080 is not available

#### Scenario: Application runs on HTTP without ssl profile

- **WHEN** the application starts without the `ssl` profile
- **THEN** the application is accessible via HTTP on port 8080

### Requirement: Reserved URL Paths Not Captured by SPA Fallback

The system SHALL ensure that browser navigation to backend-served pages — Swagger UI, OpenAPI document, and developer manual — returns the actual page content from the corresponding backend handler. These URLs SHALL never return the React SPA shell, regardless of which client (browser, API consumer) issues the request.

#### Scenario: Developer reads Swagger UI in a browser

- **GIVEN** a developer opens `/swagger-ui.html` in a browser
- **THEN** the page redirects to `/swagger-ui/index.html`
- **AND** the page renders the Swagger UI listing the public API endpoints
- **AND** the page does not render the SPA shell or a SPA "page not found" message

#### Scenario: Developer downloads the OpenAPI document

- **GIVEN** a developer opens `/v3/api-docs` in a browser or via API client
- **THEN** the response is the OpenAPI document in JSON format
- **AND** the response is not the SPA shell

#### Scenario: Authenticated club user reads the developer manual

- **GIVEN** a club user has logged in via the login form (session is established)
- **WHEN** the user opens `/docs/index.html` in the browser
- **THEN** the page renders the developer manual home page
- **AND** the page does not render the SPA shell or a SPA "page not found" message

#### Scenario: SPA route serves the React application shell

- **WHEN** a user opens any SPA route (`/`, `/events`, `/members/{id}`) in the browser
- **THEN** the response is the React SPA shell page
- **AND** client-side routing renders the requested screen

#### Scenario: Unknown API path returns a JSON error, not the SPA shell

- **GIVEN** an API consumer requests `/api/foo-bar-not-existing` with `Accept: application/json`
- **THEN** the response status is 404
- **AND** the response body is JSON
- **AND** the response is not the SPA shell HTML

### Requirement: HTTP/2 Protocol Support

The system SHALL support HTTP/2 for all HTTPS connections. Cleartext HTTP/2 (h2c) is disabled.

#### Scenario: Application supports HTTP/2 over HTTPS

- **WHEN** a client connects via HTTPS and supports HTTP/2
- **THEN** the server uses HTTP/2 for the connection

#### Scenario: Legacy clients use HTTP/1.1 fallback

- **WHEN** a client does not support HTTP/2
- **THEN** the server falls back to HTTP/1.1 and the request is processed normally

### Requirement: SSL Certificate Configuration

The system SHALL configure SSL/TLS certificates via system configuration with profile-specific keystore settings.

#### Scenario: SSL profile loads keystore from configured path

- **WHEN** application starts with the `ssl` profile
- **THEN** the SSL certificate is loaded from the configured keystore path
- **AND** the keystore password and certificate alias are taken from configuration

#### Scenario: Missing keystore file causes startup failure with clear error

- **WHEN** the keystore file does not exist at the configured path
- **THEN** the application fails to start
- **AND** the error message indicates the missing keystore file and its expected path

### Requirement: OAuth2 HTTPS Configuration

The system SHALL configure all OAuth2-related URLs to use HTTPS on port 8443.

#### Scenario: OAuth2 issuer URL uses HTTPS

- **WHEN** the OAuth2 authorization server initializes with the ssl profile
- **THEN** the issuer URL is configured as `https://localhost:8443`
- **AND** all OAuth2 redirect URIs use HTTPS

### Requirement: Local Development OAuth2 Client

The system SHALL optionally register a second OAuth2 client `klabis-web-local` when the `local-dev` Spring profile is active, distinct from the production `klabis-web` public client, to enable refresh-token-based silent token renewal during development on a separate frontend origin.

#### Scenario: Local-dev profile registers a confidential client alongside the public client

- **WHEN** the system starts with the `local-dev` Spring profile active
- **THEN** both `klabis-web` (public, PKCE, no client secret) and `klabis-web-local` (confidential, PKCE required, `client_secret_post` authentication method) are registered
- **AND** `klabis-web-local` has `authorization_code` and `refresh_token` grant types enabled
- **AND** `klabis-web-local` redirect URIs are restricted to `http://localhost:*` addresses only

#### Scenario: Local-dev profile inactive means local client does not exist

- **WHEN** the system starts without the `local-dev` profile
- **THEN** only `klabis-web` (public client) is registered
- **AND** `klabis-web-local` does not exist in the registered client repository
- **AND** any authorization request using `client_id=klabis-web-local` is rejected as an unknown client

#### Scenario: Local-dev client receives a refresh token on authorization code exchange

- **WHEN** a client authenticates with `klabis-web-local` and exchanges an authorization code for tokens via `client_secret_post`
- **THEN** the token response contains `access_token`, `id_token`, and `refresh_token`
- **AND** refresh token rotation is in effect (the next refresh invalidates the previous refresh token)

#### Scenario: Production public client does not receive a refresh token on authorization code exchange

- **WHEN** a client authenticates with the public `klabis-web` client and exchanges an authorization code for tokens
- **THEN** the token response contains `access_token` and `id_token`
- **AND** the token response does not contain a `refresh_token`
- **AND** silent token renewal in same-origin deployments is served by the authorization endpoint's `prompt=none` mechanism (iframe silent renewal)

#### Scenario: Local-dev client redirect URIs cannot point outside localhost

- **WHEN** an authorization request for `klabis-web-local` specifies a `redirect_uri` that is not a `http://localhost:*` URL
- **THEN** the authorization server rejects the request with `invalid_request`
- **AND** no authorization code is issued

#### Scenario: Bootstrap logs a warning if local-dev profile is active in a non-local environment

- **WHEN** the system starts with the `local-dev` profile active
- **AND** the configured authorization server issuer URL is not a `localhost`-based URL
- **THEN** the bootstrap logs a warning indicating that `local-dev` should only be activated on local developer machines
- **AND** the client is still registered (the warning does not block startup, to allow legitimate edge cases like tunneled local sessions)

### Requirement: Application Base URLs

The system SHALL configure all application base URLs (email links, API documentation, frontend integration) to use HTTPS on port 8443.

#### Scenario: Password setup email links use HTTPS

- **WHEN** a password setup email is generated
- **THEN** the email contains an HTTPS link to the activation page

#### Scenario: Member activation email links use HTTPS

- **WHEN** a member activation email is generated
- **THEN** the email contains an HTTPS link to the activation endpoint

### Requirement: Development Certificate Generation

The system SHALL provide tooling for generating self-signed SSL certificates for local development.

#### Scenario: Developer generates a self-signed certificate

- **WHEN** developer runs the certificate generation script
- **THEN** a keystore file is generated with a self-signed certificate for localhost
- **AND** the certificate is valid for localhost, 127.0.0.1, and ::1

#### Scenario: Documentation covers certificate trust setup

- **WHEN** developer reads the README
- **THEN** instructions are available for trusting the self-signed certificate on Linux, macOS, and Windows

### Requirement: Production Certificate Configuration

The system SHALL support production-grade SSL certificates configured via environment variables.

#### Scenario: Production uses external certificate from environment variables

- **WHEN** production deployment starts
- **THEN** the keystore path, password, and alias are loaded from environment variables
- **AND** no default values are provided for the production profile

#### Scenario: Missing production certificate configuration causes startup failure

- **WHEN** production profile starts without the required SSL environment variables
- **THEN** the application fails to start with an error listing the required variables

### Requirement: TLS Security Configuration

The system SHALL enforce secure TLS settings: minimum TLS 1.2, strong cipher suites, and HTTP Strict Transport Security header.

#### Scenario: Outdated TLS versions are rejected

- **WHEN** a client attempts to connect using TLS 1.0 or TLS 1.1
- **THEN** the connection is rejected

#### Scenario: HTTPS responses include HSTS header

- **WHEN** client makes an HTTPS request
- **THEN** the response includes a Strict-Transport-Security header with a 1-year max-age

### Requirement: Test Environment SSL Support

The system SHALL provide pre-configured SSL certificates for automated testing.

#### Scenario: CI pipeline can build and run tests with HTTPS

- **WHEN** the CI pipeline runs integration tests
- **THEN** a test keystore committed to the repository is used
- **AND** tests make requests to `https://localhost:8443`

### Requirement: Domain Event Publishing

The system SHALL publish domain events for cross-module integration.

#### Scenario: MemberSuspendedEvent is published on suspension

- **WHEN** a membership suspension is successfully committed to the database
- **THEN** MemberSuspendedEvent is published containing memberId, suspensionReason, suspendedAt timestamp, and suspendedBy user ID

#### Scenario: EventUpdatedEvent is published on event update

- **WHEN** an event is updated (name, date, location, organizer, or websiteUrl)
- **THEN** EventUpdatedEvent is published with full event data
- **AND** the Calendar module can use this event to update linked CalendarItem

#### Scenario: Event publishing failure does not block the originating operation

- **WHEN** an operation succeeds but event publishing fails
- **THEN** the originating operation is committed
- **AND** the event failure is logged for retry

### Requirement: Member Detail Response — Conditional Edit Template

The system SHALL include a PATCH template in the member detail response only for users authorized to edit the member, and the template SHALL contain only the fields the caller is permitted to modify.

#### Scenario: Admin retrieves member detail

- **WHEN** user with MEMBERS:MANAGE authority requests a member detail
- **THEN** response includes a PATCH template with all editable fields

#### Scenario: Member retrieves own profile

- **WHEN** authenticated member requests their own member detail
- **THEN** response includes a PATCH template with only self-editable fields (email, phone, address, dietaryRestrictions)

#### Scenario: Member retrieves another member's profile

- **WHEN** authenticated member requests a different member's detail without admin permission
- **THEN** response does not include a PATCH template
