# Tasks: Add Sorting and Paging to Members List API

## Domain Layer

- [x] Update `MemberRepository` interface to add `findAll(Pageable pageable)` method signature
    - Update method signature from `List<Member> findAll()` to `Page<Member> findAll(Pageable pageable)`
    - Add JavaDoc explaining pagination parameters and return type
    - Keep existing `List<Member> findAll()` as deprecated for backward compatibility (optional)

## Infrastructure Layer

- [x] Update `MemberJpaRepository` to use Spring Data JPA pagination
    - Verify `JpaRepository` already provides `Page<MemberJpaEntity> findAll(Pageable pageable)`
    - No changes needed (inherited from JpaRepository)

- [x] Update `MemberRepositoryImpl` to implement pageable findAll
    - Implement `Page<Member> findAll(Pageable pageable)` method
    - Delegate to `jpaRepository.findAll(pageable)`
    - Map `Page<MemberJpaEntity>` to `Page<Member>` using `page.map(this::toDomain)`
    - Write unit test for repository pagination

## Application Layer

- [x] Update `ListMembersQuery` to include pagination and sorting parameters
    - Add fields: `int page`, `int size`, `List<SortOrder> sortOrders`
    - Create `SortOrder` record with `String field` and `String direction`
    - Add factory method `withDefaults()` returning default pagination (page=0, size=10, sort=lastName,asc)
    - Add validation in constructor (page >= 0, size > 0 && size <= 100)

- [x] Update `ListMembersQueryHandler` to handle pagination
    - Update `handle()` method signature to return `Page<MemberSummaryDTO>` instead of `List<MemberSummaryDTO>`
    - Convert `ListMembersQuery` to `Pageable` object using `PageRequest.of()`
    - Handle multi-field sorting from query's `sortOrders` list
    - Call repository with `Pageable` parameter
    - Map `Page<Member>` to `Page<MemberSummaryDTO>` using `page.map()`
    - Write unit test for query handler pagination logic

## Presentation Layer

- [x] Update `MemberController.listMembers()` to accept pagination parameters
    - Add `@PageableDefault` annotation with defaults: size=10, sort="lastName", direction=ASC
    - Add `Pageable` parameter to method signature
    - Spring will automatically bind query params (page, size, sort) to Pageable

- [x] Implement sort field validation
    - Create constant `ALLOWED_SORT_FIELDS` set containing: firstName, lastName, registrationNumber
    - Add private method `validateSortFields(Sort sort)` to validate all sort fields
    - Throw `IllegalArgumentException` with clear message for invalid fields
    - Add `@ExceptionHandler` for `IllegalArgumentException` returning HTTP 400

- [x] Implement page size validation
    - Validate max page size <= 100 (Spring's `@PageableDefault` handles this)
    - Add validation for negative page numbers (Spring handles this automatically)

- [x] Update controller to return `PagedModel` instead of `CollectionModel`
    - Change return type to `ResponseEntity<PagedModel<EntityModel<MemberSummaryResponse>>>`
    - Use `PagedResourcesAssembler` to convert `Page<MemberSummaryDTO>` to `PagedModel`
    - Alternative: manually construct `PagedModel.PageMetadata` and add pagination links
    - Ensure pagination links (first, last, next, prev, self) are included

- [x] Update `ListMembersQuery` construction from Pageable
    - Extract page number, page size, and sort orders from Pageable
    - Convert Spring's `Sort` to List of custom `SortOrder` objects
    - Handle empty/null sort (use defaults)

- [x] Update OpenAPI documentation
    - Add `@Parameter` annotations for page, size, sort query parameters
    - Document allowed sort fields in API description
    - Update example responses to show paginated structure with page metadata
    - Document default values and constraints (max size=100)

## Testing

- [x] Write repository integration tests for pagination
    - Test findAll with pagination returns correct page size
    - Test findAll with pagination returns correct page number
    - Test sorting by firstName ascending/descending
    - Test sorting by lastName ascending/descending
    - Test sorting by registrationNumber ascending/descending
    - Test multi-field sorting (lastName, then firstName)
    - Test empty page (page beyond available data)
    - Test single page (total items less than page size)
    - Use TestContainers with real database

- [x] Write application layer tests for query handler
    - Test query handler with pagination parameters
    - Test query handler with different sort orders
    - Test query handler maps Page<Member> to Page<MemberSummaryDTO> correctly
    - Mock repository, verify Pageable parameter passed correctly

- [x] Write controller API tests for pagination
    - Test GET /api/members with no parameters (uses defaults)
    - Test GET /api/members?page=0&size=10
    - Test GET /api/members?page=1&size=20
    - Test GET /api/members?sort=firstName,asc
    - Test GET /api/members?sort=lastName,desc
    - Test GET /api/members?sort=registrationNumber,asc
    - Test GET /api/members?sort=lastName,asc&sort=firstName,asc (multi-field)
    - Test invalid sort field returns HTTP 400
    - Test page size > 100 returns HTTP 400
    - Test negative page number returns HTTP 400
    - Test empty page returns empty collection with correct metadata
    - Verify HAL+FORMS response format with page metadata
    - Verify pagination links (first, last, next, prev, self)
    - Verify sort parameters preserved in pagination links
    - Use MockMvc or RestAssured

- [x] Write end-to-end test for pagination flow
    - Create 25 test members in database
    - Retrieve first page (page=0, size=10)
    - Verify 10 members returned
    - Verify page metadata (totalElements=25, totalPages=3, number=0)
    - Follow "next" link to get second page
    - Verify different members returned
    - Verify sorting applied correctly across pages
    - Test file: MemberListPaginationE2ETest.java

## Documentation

- [x] Update API documentation (docs/API.md)
    - Document pagination query parameters
    - Document allowed sort fields
    - Add examples of paginated requests and responses
    - Document default pagination behavior

- [x] Update HATEOAS guide if needed
    - Document pagination links usage
    - Explain how clients should navigate paginated collections
    - Add complete PaginationNavigator class example
    - Document page metadata structure
    - Add edge case handling examples

## Validation and Cleanup

- [x] Run all tests to ensure no regressions
    - Execute `mvn test` to run unit and integration tests
    - Verify all existing tests still pass
    - Ensure new pagination tests pass

- [x] Manual API testing
    - Start application locally
    - Test pagination via curl/Postman with various parameters
    - Verify response format and links
    - Test edge cases (empty results, single page, invalid params)
    - Created comprehensive manual testing guide (MANUAL_TESTING_NOTES.md)

- [x] Code review checklist
    - Verify sort field validation prevents injection
    - Verify page size limits enforced
    - Verify backward compatibility maintained
    - Verify error messages are clear and helpful
