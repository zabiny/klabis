# Remove ROLE field from User aggregate

## Change ID

`remove-user-roles`

## Status

DRAFT

## Summary

Remove the `roles` field (Set<UserRole>) from the User aggregate, UserMemento, database tables, and domain events. Roles
are currently documented as "organizational labels only" that don't grant authorities - they serve no functional purpose
since the system uses only the `authorities` set for access control.

## Motivation

### Problem

The User aggregate currently maintains two separate collections:

1. **`Set<UserRole> roles`** - Stored in `user_roles` table, described as "organizational labels only"
2. **`Set<Authority> authorities`** - Stored in `user_authorities` table, actual permissions used for authorization

The roles field creates confusion and maintenance burden:

- Code comments state "Roles are organizational labels only and do not grant any authorities"
- All authorization checks use only the `authorities` set
- Having two similar-sounding concepts (roles vs authorities) is confusing
- The `user_roles` table and Role enum are unused for any business logic
- Tests must provide meaningless role data when creating users
- UserCreatedEvent includes unused roles field

### Solution

Remove the roles concept entirely from the system:

- Remove `roles` field from User domain model
- Remove `Set<UserRole> roles` from User aggregate
- Drop `user_roles` database table
- Remove Role enum and UserRole entity
- Remove roles field from UserCreatedEvent
- Update all User factory methods to remove roles parameter
- Update all tests to stop providing roles
- Update RegisterMemberCommandHandler to create users without roles

This simplifies the domain model to have a single, clear concept: authorities are the only permissions mechanism.

## Scope

### Affected Specifications

- **users**: User aggregate structure, authentication, authorization

### Affected Capabilities

- User creation and reconstruction
- User authentication (no functional impact - already uses authorities only)
- Member registration (RegisterMemberCommandHandler)
- Event-driven architecture (UserCreatedEvent structure change)

## Business Impact

### Benefits

1. **Simplified domain model**: Single clear concept for permissions (authorities)
2. **Reduced confusion**: Eliminates "roles vs authorities" cognitive overhead
3. **Less maintenance**: Fewer database tables, entities, and test fixtures
4. **Clearer intent**: Code explicitly shows authorities are the only permission mechanism

### Risks

1. **Breaking change**: UserCreatedEvent structure changes (roles field removed)
    - **Mitigation**: Application not in production yet, only in-memory DB used
2. **Test updates**: All tests using Role enum need updates
    - **Mitigation**: Straightforward mechanical changes, tests will fail clearly if missed

### Non-Functional Impact

- **Performance**: Negligible improvement (one fewer join on user load)
- **Data Migration**: Not required (in-memory H2 database, no production data)
- **Backward Compatibility**: Not required (application not in production)

## Dependencies

### Prerequisites

None - this is a self-contained refactoring

### Blocking Issues

None

### Related Changes

None

## Technical Approach

### High-Level Design

1. Remove `roles` field from User aggregate and all factory methods
2. Remove UserRole entity and Role enum
3. Drop `user_roles` table from database schema
4. Remove roles from UserCreatedEvent
5. Update UserMemento to stop persisting roles
6. Update RegisterMemberCommandHandler to create users without roles
7. Update BootstrapDataLoader to stop creating admin user with roles
8. Update all test data and test builders to remove roles
9. Update all domain tests to remove Role usage

### Implementation Strategy

- **Approach**: Bottom-up removal (domain → infrastructure → tests)
- **Rollback Plan**: Git revert (no production deployment yet)
- **Testing Strategy**:
    - All existing unit tests must pass (after updates)
    - All integration tests must pass
    - Verify user creation via RegisterMemberCommandHandler
    - Verify authentication still works (uses authorities only)

## Open Questions

None - all clarifications received from user.

## References

- Current User.java implementation: Lines 48-52 (roles field), 378-382 (getRoles method)
- Current database schema: V002__create_users_and_oauth2_tables.sql (user_roles table)
- RegisterMemberCommandHandler.java: Line 128 (creates users with Role.ROLE_MEMBER)
- UserCreatedEvent.java: Line 35 (roles field)
- BootstrapDataLoader.java: Lines 139-140 (note about user_roles table)

## Approval

- [ ] Product Owner
- [ ] Tech Lead
- [ ] Security Review: N/A (no security impact)
- [ ] Architecture Review: Approved (simplification, reduces complexity)
