## 1. Investigation

- [x] 1.1 Verify current serialization behavior of date/time types in API responses
    - Check GET /api/members/{id} response for `LocalDate` fields
    - Confirm dates are serialized as arrays `[year, month, day]` instead of ISO-8601

- [x] 1.2 Identify ALL endpoints and DTOs with date/time fields
    - Search for all `LocalDate` fields in DTOs across the entire codebase
    - Search for `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, `Instant` fields
    - Document all affected endpoints:
        - Members: dateOfBirth
        - Events: event dates, registration deadlines, start/end times (not implemented yet)
        - Finances: payment dates, fee periods, transaction timestamps (not implemented yet)
        - Any other date/time fields in the codebase

## 2. Implementation

- [x] 2.1 Create `JacksonConfig` configuration class
    - Add `@Configuration` annotation
    - Create `@Bean` method for `ObjectMapper` (or customize the existing one)
    - Register `JavaTimeModule` with the ObjectMapper
    - Configure `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` to false
    - Ensure configuration is in Spring component scan path (`com.klabis.config`)

- [x] 2.2 Verify configuration is loaded by Spring
    - Test that ObjectMapper bean is properly registered
    - Verify JavaTimeModule is active
    - Confirm no conflicts with existing Spring Boot auto-configuration

## 3. Testing

- [x] 3.1 Update existing tests to expect ISO-8601 date format
    - Update `GetMemberApiTest` assertions for dateOfBirth to expect ISO-8601 strings
    - Update `GetMemberE2ETest` assertions for dateOfBirth
    - Update any other tests with date/time assertions
    - Ensure all tests pass with new format

- [x] 3.2 Add comprehensive test cases for date/datetime serialization
    - Test `LocalDate` serialization as "YYYY-MM-DD"
    - Test `LocalDateTime` serialization as "YYYY-MM-DDTHH:MM:SS" (if any in codebase)
    - Test edge cases: leap years (Feb 29), month boundaries, year boundaries
    - Test dates across different time zones (if applicable)

- [x] 3.3 Run full test suite to ensure no regressions
    - Verify all member-related tests pass
    - Verify all event-related tests pass
    - Verify all finance-related tests pass
    - Check for any other affected endpoints

- [x] 3.4 Test all date/time types currently in use
    - Confirm each date/time type in the codebase serializes correctly
    - Validate format matches ISO-8601 standard
    - Test with real-world data from existing test fixtures

## 4. Verification

- [x] 4.1 Manually test all affected endpoints via Swagger UI or curl
    - Test GET /api/members/{id} - confirm dateOfBirth is "1990-05-15" format
    - Test any event endpoints - confirm event dates are ISO-8601
    - Test any finance endpoints - confirm payment dates are ISO-8601
    - Verify all responses are valid JSON

- [x] 4.2 Verify OpenAPI documentation reflects correct format
    - Check Swagger UI shows date/datetime fields correctly
    - Confirm schema annotations match ISO-8601 format
    - Validate API documentation is accurate

- [x] 4.3 Test interoperability with frontend
    - Confirm standard JavaScript Date.parse() works with responses
    - Confirm standard JSON libraries can parse the dates
    - Verify no timezone handling issues

## 5. Documentation

- [x] 5.1 Update coding standards if needed
    - Document that all new date/time fields will use ISO-8601
    - Add guidance for developers on date/time handling
    - Update API design guidelines

- [x] 5.2 Add migration notes
    - Document the change from array format to ISO-8601 strings
    - Note any frontend changes required (if any)
    - Provide examples of old vs new format for reference
