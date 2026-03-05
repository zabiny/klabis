# Change: Fix Date/DateTime Serialization to ISO-8601 Format

## Why

Date and DateTime fields in API responses are being serialized incorrectly:

- `LocalDate` fields (e.g., `dateOfBirth`) are serialized as JSON arrays `[1990, 5, 15]` instead of ISO-8601 date
  strings `"1990-05-15"`
- `LocalDateTime` fields (if any) would be serialized as arrays instead of ISO-8601 datetime strings
  `"1990-05-15T10:30:00"`

This happens because Jackson's default behavior for Java 8 date/time types is to serialize them as arrays. The API
specification requires ISO-8601 format for all date and datetime fields to ensure proper interoperability with frontend
clients and API consumers.

## What Changes

- Create `JacksonConfig` configuration class to register `JavaTimeModule`
- Configure Jackson to serialize all date/time types as ISO-8601 strings
- Ensure ALL `LocalDate`, `LocalDateTime`, and other Java 8 date/time types across the API are properly serialized
- No breaking changes - response format becomes compliant with ISO-8601 standard

## Impact

- **Affected specs**: All capabilities (global serialization change)
- **Affected code**:
    - New: `com.klabis.config.JacksonConfig`
    - All endpoints returning date/time fields will be fixed automatically:
        - Members: `dateOfBirth`
        - Events: event dates, registration deadlines
        - Finances: payment dates, fee periods
        - Any future endpoints with date/time fields
