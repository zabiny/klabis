# Implementation Tasks: Add List Members Endpoint

## 1. Domain Layer - Repository Interface

- [x] 1.1 Add findAll() method to MemberRepository interface
    - [x] 1.1.1 Open `MemberRepository.java` in domain layer
    - [x] 1.1.2 Add method signature: `List<Member> findAll();`
    - [x] 1.1.3 Add JavaDoc explaining the method returns all members (unsorted for now)
- [x] 1.2 Write unit test for repository findAll() method
    - [x] 1.2.1 Add test case in `MemberRepositoryImplTest.java`
    - [x] 1.2.2 Test empty list when no members exist
    - [x] 1.2.3 Test returns all members when multiple exist

## 2. Infrastructure Layer - Repository Implementation

- [x] 2.1 Implement findAll() in MemberRepositoryImpl
    - [x] 2.1.1 Open `MemberRepositoryImpl.java`
    - [x] 2.1.2 Call `jpaRepository.findAll()` to get all MemberEntity records
    - [x] 2.1.3 Map each MemberEntity to Member domain object using mapper
    - [x] 2.1.4 Return List<Member>
- [x] 2.2 Verify MemberJpaRepository extends JpaRepository
    - [x] 2.2.1 Open `MemberJpaRepository.java`
    - [x] 2.2.2 Confirm it extends JpaRepository (provides findAll() automatically)
    - [x] 2.2.3 No changes needed if already extends JpaRepository
- [x] 2.3 Run integration test
    - [x] 2.3.1 Test findAll() against real database (H2 or TestContainers)
    - [x] 2.3.2 Verify entities mapped correctly to domain objects

## 3. Application Layer - Query Handler

- [x] 3.1 Create ListMembersQuery class
    - [x] 3.1.1 Create `ListMembersQuery.java` in application package
    - [x] 3.1.2 Define as simple record or class (no parameters for now)
    - [x] 3.1.3 Add JavaDoc explaining this is a query object for CQRS pattern
- [x] 3.2 Create MemberSummaryDTO
    - [x] 3.2.1 Create `MemberSummaryDTO.java` as Java record
    - [x] 3.2.2 Add fields: UUID id, String firstName, String lastName, String registrationNumber
    - [x] 3.2.3 Add JavaDoc explaining this is application layer DTO
- [x] 3.3 Create ListMembersQueryHandler
    - [x] 3.3.1 Create `ListMembersQueryHandler.java` in application package
    - [x] 3.3.2 Inject MemberRepository via constructor
    - [x] 3.3.3 Implement handle(ListMembersQuery) method
    - [x] 3.3.4 Call repository.findAll() to get domain objects
    - [x] 3.3.5 Map Member domain objects to MemberSummaryDTO
    - [x] 3.3.6 Return List<MemberSummaryDTO>
- [x] 3.4 Write unit test for ListMembersQueryHandler
    - [x] 3.4.1 Mock MemberRepository
    - [x] 3.4.2 Test empty list handling
    - [x] 3.4.3 Test multiple members returned and mapped correctly
    - [x] 3.4.4 Verify all required fields present in DTOs (firstName, lastName, registrationNumber)

## 4. Presentation Layer - Response DTO

- [x] 4.1 Create MemberSummaryResponse record
    - [x] 4.1.1 Create `MemberSummaryResponse.java` in presentation package
    - [x] 4.1.2 Define as Java record with fields: UUID id, String firstName, String lastName, String registrationNumber
    - [x] 4.1.3 Add Swagger @Schema annotations for OpenAPI documentation
    - [x] 4.1.4 Add example values in annotations (e.g., firstName="Jan", lastName="Novák", registrationNumber="
      ZBM0501")
- [x] 4.2 Verify record follows existing patterns
    - [x] 4.2.1 Compare with MemberRegistrationResponse for consistency
    - [x] 4.2.2 Ensure naming conventions match project standards

## 5. Presentation Layer - Controller Endpoint

- [x] 5.1 Add GET /api/members endpoint to MemberController
    - [x] 5.1.1 Open `MemberController.java`
    - [x] 5.1.2 Inject ListMembersQueryHandler via constructor
    - [x] 5.1.3 Add @GetMapping method for GET /api/members (without path parameter)
    - [x] 5.1.4 Add @PreAuthorize("hasAuthority('MEMBERS:READ')") annotation
    - [x] 5.1.5 Call query handler to get List<MemberSummaryDTO>
    - [x] 5.1.6 Map DTOs to MemberSummaryResponse objects
    - [x] 5.1.7 Wrap in CollectionModel for HATEOAS support
    - [x] 5.1.8 Add self link to collection using linkTo(methodOn(MemberController.class).listMembers())
    - [x] 5.1.9 Add self link to each member using linkTo(methodOn(MemberController.class).getMember(id))
    - [x] 5.1.10 Return ResponseEntity with CollectionModel<EntityModel<MemberSummaryResponse>>
- [x] 5.2 Add OpenAPI annotations
    - [x] 5.2.1 Add @Operation annotation with summary "List all members"
    - [x] 5.2.2 Add description explaining endpoint returns member summaries
    - [x] 5.2.3 Add @SecurityRequirement(name = "OAuth2")
    - [x] 5.2.4 Add @ApiResponses for 200 OK, 401 Unauthorized, 403 Forbidden
    - [x] 5.2.5 Reference MemberSummaryResponse schema in 200 response

## 6. Testing - Controller Layer

- [x] 6.1 Write unit tests for MemberController.listMembers()
    - [x] 6.1.1 Create or update `MemberControllerTest.java`
    - [x] 6.1.2 Mock ListMembersQueryHandler
    - [x] 6.1.3 Test successful retrieval with multiple members (verify 200 OK, HATEOAS links)
    - [x] 6.1.4 Test empty list (verify 200 OK with empty collection)
    - [x] 6.1.5 Test unauthorized access (verify 401 without authentication)
    - [x] 6.1.6 Test forbidden access (verify 403 without MEMBERS:READ permission)
    - [x] 6.1.7 Verify HAL+FORMS media type in response
    - [x] 6.1.8 Verify self links present on collection and individual items
- [x] 6.2 Write integration tests using MockMvc
    - [x] 6.2.1 Test full request/response flow with real Spring context
    - [x] 6.2.2 Use @SpringBootTest or @WebMvcTest
    - [x] 6.2.3 Mock authentication/authorization
    - [x] 6.2.4 Verify JSON response structure matches HAL+FORMS spec

## 7. Testing - End-to-End

- [x] 7.1 Write E2E test for list members flow
    - [x] 7.1.1 Create test class using RestAssured or @SpringBootTest
    - [x] 7.1.2 Insert test members into database
    - [x] 7.1.3 Call GET /api/members with proper authentication
    - [x] 7.1.4 Verify response contains all members
    - [x] 7.1.5 Verify each member has firstName, lastName, registrationNumber
    - [x] 7.1.6 Verify HATEOAS links present and correct
    - [x] 7.1.7 Clean up test data

## 8. Documentation

- [x] 8.1 Update API documentation
    - [x] 8.1.1 Verify Swagger UI shows new GET /api/members endpoint
    - [x] 8.1.2 Test endpoint documentation in Swagger UI
    - [x] 8.1.3 Verify example responses generated correctly
- [x] 8.2 Update README if needed
    - [x] 8.2.1 Add GET /api/members to API endpoints list (if such list exists)
    - [x] 8.2.2 Document required permissions (MEMBERS:READ)

## 9. Code Review and Cleanup

- [x] 9.1 Review all code follows project conventions
    - [x] 9.1.1 Verify package structure correct (domain, application, infrastructure, presentation)
    - [x] 9.1.2 Verify naming conventions (PascalCase classes, camelCase methods)
    - [x] 9.1.3 Verify JavaDoc present on public methods
    - [x] 9.1.4 Verify Spring util.Assert used for parameter validation
- [x] 9.2 Run all tests
    - [x] 9.2.1 Run unit tests (mvn test)
    - [x] 9.2.2 Run integration tests (mvn verify)
    - [x] 9.2.3 Verify code coverage >80%
- [x] 9.3 Manual testing
    - [x] 9.3.1 Start application locally
    - [x] 9.3.2 Register test members via POST /api/members
    - [x] 9.3.3 Call GET /api/members and verify response
    - [x] 9.3.4 Verify HATEOAS links clickable in HAL Browser (if available)

## Dependencies

- **Tasks 1-2** can run in parallel (repository interface and implementation)
- **Task 3** depends on tasks 1-2 (needs repository available)
- **Task 4** can run in parallel with task 3 (independent DTO creation)
- **Task 5** depends on tasks 3-4 (needs query handler and response DTO)
- **Task 6** depends on task 5 (controller must exist to test)
- **Task 7** depends on all previous tasks (full integration test)
- **Tasks 8-9** can start after task 5 (enough implemented to document and review)

## Estimated Effort

- **Phase 1 (Tasks 1-2)**: 1-2 hours - Repository layer
- **Phase 2 (Task 3)**: 2-3 hours - Application layer with query handler
- **Phase 3 (Tasks 4-5)**: 2-3 hours - Presentation layer with HATEOAS
- **Phase 4 (Tasks 6-7)**: 3-4 hours - Comprehensive testing
- **Phase 5 (Tasks 8-9)**: 1-2 hours - Documentation and cleanup

**Total estimated effort**: 9-14 hours (1-2 days for one developer)

## Success Criteria

- ✅ GET /api/members endpoint returns 200 OK
- ✅ Response contains firstName, lastName, and registrationNumber for each member
- ✅ Response uses HAL+FORMS media type with proper _embedded and _links structure
- ✅ HATEOAS self links present on collection and individual members
- ✅ Endpoint requires authentication (401 if not authenticated)
- ✅ Endpoint requires MEMBERS:READ permission (403 if forbidden)
- ✅ All unit and integration tests pass
- ✅ Code coverage >80%
- ✅ Swagger UI documentation complete and accurate
- ✅ Manual testing successful with real database
