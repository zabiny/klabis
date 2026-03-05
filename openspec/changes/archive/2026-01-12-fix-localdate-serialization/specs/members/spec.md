## ADDED Requirements

### Requirement: ISO-8601 Date and DateTime Serialization

All API endpoints SHALL serialize date and datetime fields using ISO-8601 format to ensure consistent and
standards-compliant JSON responses.

This requirement applies to ALL Java 8 date/time types across the entire API:

- `LocalDate` - serialized as ISO-8601 date strings (YYYY-MM-DD)
- `LocalDateTime` - serialized as ISO-8601 datetime strings (YYYY-MM-DDTHH:MM:SS)
- `OffsetDateTime` - serialized as ISO-8601 datetime strings with offset (YYYY-MM-DDTHH:MM:SS±HH:MM)
- `ZonedDateTime` - serialized as ISO-8601 datetime strings with timezone (YYYY-MM-DDTHH:MM:SS±HH:MM[Region/City])
- `Instant` - serialized as ISO-8601 instant strings (YYYY-MM-DDTHH:MM:SSZ)

#### Scenario: LocalDate fields serialize as ISO-8601 date strings

- **WHEN** any API endpoint returns a `LocalDate` field (e.g., member's dateOfBirth, event date)
- **THEN** the field SHALL be serialized as an ISO-8601 date string in format `YYYY-MM-DD`
- **EXAMPLE**: January 15, 1990 SHALL be serialized as `"1990-01-15"`
- **AND** it SHALL NOT be serialized as an array `[1990, 1, 15]`

#### Scenario: LocalDate handles edge cases (leap years, month boundaries)

- **WHEN** a `LocalDate` field represents February 29, 2000
- **THEN** the field SHALL be serialized as `"2000-02-29"`
- **AND** the format SHALL be valid ISO-8601

#### Scenario: LocalDateTime fields serialize as ISO-8601 datetime strings

- **WHEN** any API endpoint returns a `LocalDateTime` field (e.g., event registration deadline, payment timestamp)
- **THEN** the field SHALL be serialized as an ISO-8601 datetime string in format `YYYY-MM-DDTHH:MM:SS`
- **EXAMPLE**: May 15, 1990 at 14:30:00 SHALL be serialized as `"1990-05-15T14:30:00"`
- **AND** it SHALL NOT be serialized as an array `[1990, 5, 15, 14, 30, 0, 0]`

#### Scenario: OffsetDateTime fields include timezone offset

- **WHEN** any API endpoint returns an `OffsetDateTime` field
- **THEN** the field SHALL be serialized as ISO-8601 with offset (e.g., `"1990-05-15T14:30:00+02:00"`)

#### Scenario: All date/time fields across all endpoints use consistent format

- **WHEN** multiple endpoints return date/time fields
- **THEN** all fields SHALL use ISO-8601 format consistently
- **AND** clients SHALL be able to parse all date/time values using standard ISO-8601 parsers

## MODIFIED Requirements

### Requirement: Member Details Response Format

The member details endpoint SHALL return complete member information in a HATEOAS-compliant HAL+FORMS format with proper
ISO-8601 date serialization.

#### Scenario: Response contains all personal information with ISO-8601 dates

- **WHEN** a member details response is returned
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID)
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth **as ISO-8601 date string (YYYY-MM-DD)**
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE, OTHER)
    - `address` - Object containing street, city, postalCode, country
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - `emails` - Array of email addresses
    - `phones` - Array of phone numbers
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `active` - Boolean indicating if member is active
    - `guardian` - Guardian information object (present if member has guardian)

#### Scenario: DateOfBirth is serialized in ISO-8601 format

- **WHEN** a member's date of birth is January 15, 1990
- **THEN** the response SHALL serialize `dateOfBirth` as `"1990-01-15"`
- **AND** it SHALL NOT be serialized as an array `[1990, 1, 15]`

#### Scenario: DateOfBirth handles leap years and edge cases

- **WHEN** a member's date of birth is February 29, 2000
- **THEN** the response SHALL serialize `dateOfBirth` as `"2000-02-29"`
- **AND** the format SHALL be valid ISO-8601

#### Scenario: Response uses HAL+FORMS media type

- **WHEN** the get member endpoint is called
- **THEN** the response Content-Type SHALL be application/prs.hal-forms+json
- **AND** the response SHALL follow HAL+FORMS specification for collections
- **AND** the response SHALL include _embedded object containing member summaries
- **AND** the response SHALL include _links object for hypermedia navigation
