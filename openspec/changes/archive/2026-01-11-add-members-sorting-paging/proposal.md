# Change: Add Sorting and Paging to Members List API

## Why

The current GET /api/members endpoint (added in `add-list-members-endpoint`) returns all members in an unsorted,
unpaginated list. This creates several issues:

1. **Performance degradation** - As the club grows to hundreds of members, returning all members in a single response
   becomes inefficient and slow
2. **Poor user experience** - Frontend users cannot easily navigate large member lists or find specific members
3. **Missing essential functionality** - Users need to sort members alphabetically or by registration number for
   administrative tasks
4. **Scalability concerns** - The current implementation doesn't scale for clubs with 500-1000+ members (project target)

The project constraints specify:

- Support for clubs with up to 1000 members (openspec/project.md:147)
- API response time <500ms for standard operations (openspec/project.md:146)

Without pagination and sorting, the API will fail to meet these constraints as data volume grows.

## What Changes

This change enhances the GET /api/members endpoint with:

**Sorting capabilities:**

- Sort by `firstName` (ascending/descending)
- Sort by `lastName` (ascending/descending)
- Sort by `registrationNumber` (ascending/descending)
- Default sort: `lastName` ascending, then `firstName` ascending

**Pagination capabilities:**

- Page-based pagination using standard query parameters
- Default page size: 10 items
- Configurable page size (max 100 to prevent abuse)
- Page metadata in response (total items, total pages, current page)
- HATEOAS pagination links (first, last, next, prev, self)

**Technical implementation:**

- Use Spring Data's `Pageable` and `Page<T>` abstractions
- Add query parameters: `page`, `size`, `sort` to GET /api/members
- Update `ListMembersQuery` to include pagination and sorting parameters
- Update `MemberRepository.findAll()` to accept `Pageable` parameter
- Enhance response with Spring HATEOAS `PagedModel` for pagination links
- Maintain backward compatibility (parameters optional, sensible defaults)

**API contract:**

- `GET /api/members?page=0&size=10&sort=lastName,asc&sort=firstName,asc`
- Response includes: `_embedded.members`, `_links` (navigation), `page` metadata
- HAL+FORMS compliance maintained

## Impact

**Affected specs:**

- Existing capability: `members` (MODIFIED requirement for listing members)

**Affected code:**

- `klabis-backend/src/main/java/com/klabis/members/application/ListMembersQuery.java` - Add pagination/sort parameters
- `klabis-backend/src/main/java/com/klabis/members/application/ListMembersQueryHandler.java` - Handle pagination/sort
- `klabis-backend/src/main/java/com/klabis/members/domain/MemberRepository.java` - Update findAll() signature
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/MemberRepositoryImpl.java` - Implement
  pageable findAll()
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/MemberJpaRepository.java` - Add JPA
  pagination support
- `klabis-backend/src/main/java/com/klabis/members/presentation/MemberController.java` - Accept pagination query params,
  return PagedModel
- Test files for all layers (controller, service, repository)

**Benefits:**

- Scalable member list retrieval supporting 1000+ members
- Improved user experience with sorted, navigable results
- Meets project performance and scalability requirements
- Follows Spring Data and Spring HATEOAS best practices
- Maintains HATEOAS compliance with pagination links

**Risks:**

- **Breaking change potential** - Existing clients might not handle paginated responses
    - **Mitigation**: Make parameters optional with sensible defaults; response structure remains compatible (still
      returns `_embedded.members`)
- **Sort field validation** - Invalid sort fields could cause errors
    - **Mitigation**: Validate allowed sort fields, return 400 for invalid requests
- **Database performance** - Sorting large datasets could be slow without proper indexes
    - **Accepted**: Not expected to have enough members to cause visible performance impact without indexes

**Migration path:**

- Fully backward compatible - pagination/sort parameters are optional
- Default behavior provides sensible pagination (10 items, sorted by lastName)
- Existing clients continue to work; they receive first page of results
- No database migration required
- Can be deployed independently
