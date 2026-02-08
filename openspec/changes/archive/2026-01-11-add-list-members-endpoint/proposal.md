# Change: Add API Endpoint for Listing Members

## Why

The current Members API only supports creating new members (POST /api/members) and has a placeholder for retrieving
individual members (GET /api/members/{id}). There is no endpoint to retrieve a list of all members, which is essential
for:

1. **Admin and organizer workflows** - Viewing all registered club members for management and reporting
2. **Member directory** - Displaying member roster in the frontend application
3. **HATEOAS compliance** - Providing a collection resource that frontend can discover and navigate

Without a list endpoint, the API is incomplete and the frontend cannot display member lists or search functionality.

## What Changes

- **Add GET /api/members endpoint** that returns a paginated list of members
- **Return minimal member information** in the response: firstName, lastName, and registrationNumber (as specified in
  requirement)
- **Implement HATEOAS links** using Spring HATEOAS `CollectionModel` for hypermedia navigation
- **Add repository method** `findAll()` to MemberRepository interface and implementation
- **Add query handler** or service method to retrieve member list from application layer
- **Add response DTO** for member list representation (MemberSummaryResponse)
- **Require authentication** with `MEMBERS:READ` permission following existing security patterns
- **Add OpenAPI documentation** following existing Swagger annotation patterns
- **Write comprehensive tests** covering controller, service, and repository layers

## Impact

**Affected specs:**

- Existing capability: `members` (ADDED requirement for listing members)

**Affected code:**

- `klabis-backend/src/main/java/com/klabis/members/domain/MemberRepository.java` - Add findAll() method
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/MemberRepositoryImpl.java` - Implement
  findAll()
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/MemberJpaRepository.java` - JPA findAll
  method (likely auto-generated)
- `klabis-backend/src/main/java/com/klabis/members/application/ListMembersQueryHandler.java` - New query handler
- `klabis-backend/src/main/java/com/klabis/members/presentation/MemberController.java` - Add GET /api/members endpoint
- `klabis-backend/src/main/java/com/klabis/members/presentation/MemberSummaryResponse.java` - New response DTO
- Test files for all layers

**Benefits:**

- Complete CRUD API for members (Create + Read list)
- Frontend can display member directory and search
- Follows HATEOAS principles with collection resources
- Consistent with existing API patterns and security model

**Risks:**

- **None identified** - Straightforward read-only endpoint following existing patterns
- ~~Future consideration: Pagination, filtering, and sorting (not in this change)~~
    - **COMPLETED**: Pagination and sorting implemented in `add-members-sorting-paging` (2026-01-11)
    - **REMAINING**: Filtering/search still deferred

**Migration path:**

- Fully backward compatible - no existing endpoints modified
- No data migration required
- Can be deployed independently
