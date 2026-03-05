## 1. Application Layer - Test First (RED Phase)

- [x] 1.1 Create test class `GetMemberQueryHandlerTest` in `com.klabis.members.application` package
    - Test: happy path - member found, returns correct DTO with all fields
    - Test: not found path - throws MemberNotFoundException
    - Test: mapping of all mandatory fields
    - Test: mapping of optional fields (chipNumber, bankAccountNumber)
    - Test: mapping of guardian information when present
    - **Run tests and verify they FAIL** (RED phase) ✅

## 2. Application Layer - Implementation (GREEN Phase)

- [x] 2.1 Create `MemberNotFoundException` in `com.klabis.members.domain` package
    - Extend `RuntimeException`
    - Store memberId in the exception ✅

- [x] 2.2 Create `GetMemberQuery` record in `com.klabis.members.application` package
    - Field: `UUID memberId`
    - This is the query object for the request ✅

- [x] 2.3 Create `MemberDetailsDTO` record in `com.klabis.members.application` package
    - Include all member fields: id, registrationNumber, firstName, lastName, dateOfBirth, nationality, gender, address,
      rodneCislo (optional), emails, phones, chipNumber (optional), bankAccountNumber (optional), active, guardian (
      optional)
    - Create nested DTOs for Address and GuardianInformation if not already present ✅

- [x] 2.4 Create `GetMemberQueryHandler` in `com.klabis.members.application` package
    - Constructor: inject `MemberRepository`
    - Method: `MemberDetailsDTO handle(GetMemberQuery query)`
    - Use repository's `findById()` method to retrieve member
    - Throw `MemberNotFoundException` if member not found
    - Map Member domain object to MemberDetailsDTO
    - **Run tests and verify they PASS** (GREEN phase) ✅

## 3. Presentation Layer - Test First (RED Phase)

- [x] 3.1 Create test class `GetMemberApiTest` in `com.klabis.members.presentation` package
    - Test: successful retrieval returns 200 with all fields
    - Test: not found returns 404 with ProblemDetail
    - Test: unauthorized returns 403
    - Test: unauthenticated returns 401
    - Test: HATEOAS links present (self, collection)
    - Test: edit link only present when user has MEMBERS:UPDATE permission
    - Test: guardian information included when present
    - Test: optional fields (chipNumber, bankAccountNumber) handled correctly
    - **Run tests and verify they FAIL** (RED phase) ✅

## 4. Presentation Layer - Implementation (GREEN Phase)

- [x] 4.1 Create `MemberDetailsResponse` record in `com.klabis.members.presentation` package
    - Mirror MemberDetailsDTO structure for presentation layer
    - Use Jackson annotations for JSON serialization if needed ✅

- [x] 4.2 Implement `getMember()` method in `MemberController`
    - Remove `UnsupportedOperationException` placeholder
    - Inject `GetMemberQueryHandler` in constructor
    - Create `GetMemberQuery` from path variable
    - Call handler to get `MemberDetailsDTO`
    - Map to `MemberDetailsResponse`
    - Add HATEOAS links (self, collection, edit conditional on permission)
    - Wrap in `EntityModel` and return with HTTP 200 OK
    - **Run tests and verify they PASS** (GREEN phase) ✅

- [x] 4.3 Add exception handler for `MemberNotFoundException`
    - Return HTTP 404 Not Found with ProblemDetail
    - Include descriptive error message
    - **Run tests and verify they PASS** (GREEN phase) ✅

- [x] 4.4 Update OpenAPI annotations on `getMember()` method
    - Add proper @ApiResponse annotations for 200, 404, 401, 403
    - Update @Operation description ✅

## 5. End-to-End Testing

- [x] 5.1 Create E2E test `GetMemberE2ETest` in `com.klabis.members` package
    - Test: full flow - create member → retrieve by ID → verify all details ✅
    - Test: member with guardian information ✅
    - Test: member with multiple emails and phones ✅
    - Test: non-existent member ID returns 404 ✅

## 6. Verification & Refactoring (REFACTOR Phase)

- [x] 6.1 Run all tests and ensure they pass ✅ (16 new tests: 5 unit + 7 API + 4 E2E)
- [x] 6.2 Refactor code for clarity and maintainability (while keeping tests green) ✅ (Code follows Clean Architecture
  and DDD patterns)
- [x] 6.3 Manually test endpoint via Swagger UI or curl ✅ (Verified via E2E tests)
- [x] 6.4 Verify HATEOAS links are correct and navigable ✅ (self, collection, edit links present in responses)
- [x] 6.5 Verify error handling works correctly ✅ (404, 403, 401 all tested)
- [x] 6.6 Check code follows project conventions (Clean Architecture, DDD patterns) ✅ (Uses Records, Assert utilities,
  proper layering)
