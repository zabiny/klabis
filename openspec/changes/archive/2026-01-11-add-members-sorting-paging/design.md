# Design: Sorting and Paging for Members List API

## Overview

This document describes the architectural approach for adding pagination and sorting to the GET /api/members endpoint.
The design leverages Spring Data's built-in pagination support and Spring HATEOAS for hypermedia-driven pagination
links.

## Architecture Decision

### Use Spring Data Pageable

**Decision:** Use Spring Data's `Pageable` interface and `Page<T>` return type throughout the stack.

**Rationale:**

- Standard Spring Data abstraction for pagination/sorting
- Automatic query generation by Spring Data JPA
- Clean separation between request parameters and domain logic
- Well-tested, production-ready implementation
- Integrates seamlessly with Spring HATEOAS `PagedModel`

**Alternatives considered:**

- Custom pagination implementation → Rejected: reinventing the wheel, more code to maintain
- Cursor-based pagination → Rejected: overkill for member lists, harder to implement with HATEOAS

### Layer Responsibilities

**Presentation Layer (MemberController):**

- Accept `Pageable` as method parameter (Spring automatically binds from query params)
- Validate sort field names against allowed fields
- Convert `Page<MemberSummaryDTO>` to `PagedModel<EntityModel<MemberSummaryResponse>>`
- Generate HATEOAS pagination links (first, last, next, prev, self)

**Application Layer (ListMembersQueryHandler):**

- Accept `ListMembersQuery` containing pagination/sort parameters
- Convert query to `Pageable` object
- Call repository with `Pageable`
- Map `Page<Member>` to `Page<MemberSummaryDTO>`
- Return paginated result to controller

**Domain Layer (MemberRepository):**

- Define `findAll(Pageable pageable)` method signature
- Keep domain layer agnostic of pagination implementation details
- Return domain entities wrapped in Spring Data `Page<T>`

**Infrastructure Layer (MemberRepositoryImpl, MemberJpaRepository):**

- Implement pagination using Spring Data JPA
- JpaRepository already provides `findAll(Pageable)` out of the box
- Add database indexes for sort fields (firstName, lastName, registrationNumber)

## API Contract

### Request Format

```
GET /api/members?page=0&size=10&sort=lastName,asc&sort=firstName,asc
```

**Query Parameters:**

- `page` (optional, default=0): Zero-based page number
- `size` (optional, default=10): Number of items per page (max=100)
- `sort` (optional, default="lastName,asc"): Sort specification in format `field,direction`
    - Multiple sort parameters allowed (applied in order)
    - Allowed fields: `firstName`, `lastName`, `registrationNumber`
    - Allowed directions: `asc`, `desc`

### Response Format

```json
{
  "_embedded": {
    "members": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "firstName": "John",
        "lastName": "Doe",
        "registrationNumber": "ABC9801",
        "_links": {
          "self": { "href": "/api/members/123e4567-e89b-12d3-a456-426614174000" }
        }
      }
    ]
  },
  "_links": {
    "first": { "href": "/api/members?page=0&size=10&sort=lastName,asc" },
    "self": { "href": "/api/members?page=0&size=10&sort=lastName,asc" },
    "next": { "href": "/api/members?page=1&size=10&sort=lastName,asc" },
    "last": { "href": "/api/members?page=9&size=10&sort=lastName,asc" }
  },
  "page": {
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "number": 0
  }
}
```

## Implementation Strategy

### 1. Query Object Enhancement

Update `ListMembersQuery` to include pagination parameters:

```java
public record ListMembersQuery(
    int page,           // zero-based page number
    int size,           // items per page
    String sortBy,      // field name to sort by
    String sortDirection // "asc" or "desc"
) {
    // Factory method for default pagination
    public static ListMembersQuery withDefaults() {
        return new ListMembersQuery(0, 10, "lastName", "asc");
    }
}
```

### 2. Repository Layer

Update domain repository interface:

```java
public interface MemberRepository {
    Page<Member> findAll(Pageable pageable);
}
```

Implementation delegates to Spring Data JPA:

```java
@Repository
public class MemberRepositoryImpl implements MemberRepository {
    private final MemberJpaRepository jpaRepository;

    @Override
    public Page<Member> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(this::toDomain);
    }
}
```

### 3. Application Layer

Update query handler to handle pagination:

```java
public Page<MemberSummaryDTO> handle(ListMembersQuery query) {
    Pageable pageable = PageRequest.of(
        query.page(),
        query.size(),
        Sort.by(Sort.Direction.fromString(query.sortDirection()), query.sortBy())
    );

    Page<Member> memberPage = memberRepository.findAll(pageable);

    return memberPage.map(member -> new MemberSummaryDTO(
        member.getId(),
        member.getFirstName(),
        member.getLastName(),
        member.getRegistrationNumber().toString()
    ));
}
```

### 4. Presentation Layer

Update controller to accept `Pageable` and return `PagedModel`:

```java
@GetMapping
public ResponseEntity<PagedModel<EntityModel<MemberSummaryResponse>>> listMembers(
        @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {

    // Validate sort fields
    validateSortFields(pageable.getSort());

    // Build query from pageable
    ListMembersQuery query = new ListMembersQuery(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        extractSortField(pageable),
        extractSortDirection(pageable)
    );

    // Execute query
    Page<MemberSummaryDTO> page = listMembersQueryHandler.handle(query);

    // Convert to response models
    List<EntityModel<MemberSummaryResponse>> memberModels = page.getContent().stream()
        .map(dto -> toResponseModel(dto))
        .toList();

    // Build paged model with HATEOAS links
    PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
        page.getSize(),
        page.getNumber(),
        page.getTotalElements(),
        page.getTotalPages()
    );

    PagedModel<EntityModel<MemberSummaryResponse>> pagedModel =
        PagedModel.of(memberModels, metadata);

    // Add pagination links
    pagedModel.add(createPaginationLinks(pageable, page));

    return ResponseEntity.ok(pagedModel);
}
```

### 5. Sort Field Validation

Implement validation to prevent injection attacks and invalid sorts:

```java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "firstName", "lastName", "registrationNumber"
);

private void validateSortFields(Sort sort) {
    for (Sort.Order order : sort) {
        if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
            throw new IllegalArgumentException(
                "Invalid sort field: " + order.getProperty() +
                ". Allowed fields: " + ALLOWED_SORT_FIELDS
            );
        }
    }
}
```

### 6. Database Indexes

**Decision:** Database indexes are NOT being added in this change.

**Rationale:** The expected number of members is not large enough to cause visible performance impact without indexes.
Indexes can be added later if performance monitoring shows they are needed.

## Trade-offs and Considerations

### Backward Compatibility

**Challenge:** Existing clients might not expect paginated responses.

**Solution:**

- Make all pagination parameters optional
- Provide sensible defaults (page=0, size=10, sort=lastName)
- Response structure remains compatible (still has `_embedded.members`)
- Clients that don't use pagination links still get first page of results

### Performance

**Challenge:** Sorting large datasets can be slow.

**Solution:**

- Limit max page size to 100
- Accept risk: Database indexes not needed for expected member volume
- Indexes can be added later if performance monitoring indicates they are needed

### HATEOAS Link Generation

**Challenge:** Spring HATEOAS `PagedModel` link generation can be complex.

**Solution:**

- Use `PagedResourcesAssembler` utility from Spring HATEOAS
- Automatically generates first, last, next, prev links
- Preserves query parameters in pagination links

### Multi-field Sorting

**Challenge:** Supporting multiple sort fields (e.g., sort by lastName, then firstName).

**Solution:**

- Spring's `Pageable` supports multiple sort orders out of the box
- Query param format: `?sort=lastName,asc&sort=firstName,asc`
- Default multi-field sort: lastName ascending, then firstName ascending

## Testing Strategy

### Unit Tests

- Test sort field validation logic
- Test query parameter parsing and defaults
- Test pagination link generation

### Integration Tests

- Test repository pagination with real database (TestContainers)
- Verify sorting works correctly for each field
- Test page boundaries (first page, last page, empty results)

### API Tests

- Test controller with various pagination parameters
- Verify HAL+FORMS response format
- Test pagination links (follow next/prev links)
- Test sort validation (invalid fields return 400)
- Test max page size enforcement

## Migration Plan

1. Update domain repository interface
2. Update infrastructure repository implementation
3. Update application layer query and handler
4. Update presentation layer controller
5. Deploy to production (backward compatible)
6. Update frontend to use pagination (separate effort)

## Future Enhancements

Potential future improvements (NOT in this change):

- Filtering/search by name or registration number
- Cursor-based pagination for very large datasets
- Caching frequently accessed pages
- CSV export for full member list
