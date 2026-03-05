# Change: Migrate from Spring Data JPA to Spring Data JDBC

## Why

The current implementation uses Spring Data JPA with Hibernate, which introduces unnecessary complexity and runtime
overhead for our use case. Our domain model is intentionally denormalized (no JPA relationships, no lazy loading) to
maintain simplicity and performance. This design pattern aligns perfectly with Spring Data JDBC's philosophy.

**Key motivations:**

1. **Reduced Complexity**: JPA's object-relational mapping features (lazy loading, cascading, dirty checking) are
   disabled or avoided in our current design. Spring Data JDBC provides a simpler persistence model that matches our
   actual usage patterns.

2. **Improved Performance**: Without Hibernate's session management, proxy objects, and first-level cache overhead, we
   can achieve better performance with more predictable behavior.

3. **Better Alignment with DDD**: Spring Data JDBC is designed for aggregate-oriented persistence, which matches our
   Clean Architecture and Domain-Driven Design principles. Each aggregate root is saved as a complete unit.

4. **Transparent Persistence**: With JDBC, what you write is what gets saved - no hidden state synchronization, no
   detached entity issues, no persistence context complications.

5. **Simpler Event Publishing**: Spring Data JDBC has built-in support for domain event publishing that integrates
   seamlessly with Spring Modulith, eliminating the need for `AbstractAggregateRoot` workarounds.

## What Changes

**BREAKING CHANGE**: Complete replacement of persistence layer implementation

### Removed Components

- All JPA entity classes (`UserEntity`, `MemberEntity`, `PasswordSetupTokenEntity`)
- All JPA repositories (`UserJpaRepository`, `MemberJpaRepository`, `PasswordSetupTokenJpaRepository`)
- JPA-specific mappers between domain and entity models
- JPA auditing configuration (`JpaAuditingConfiguration`)
- Hibernate dependencies from Maven POM
- JPA configuration in `application.yml`

### Added Components

- Spring Data JDBC aggregate roots (replacing entities)
- Spring Data JDBC repositories (replacing JPA repositories)
- JDBC-specific type converters for value objects
- JDBC auditing configuration
- Custom database callbacks for lifecycle events
- Liquibase schema migrations to match JDBC conventions

### Modified Components

- Repository implementations will be simplified (no manual entity-domain mapping needed)
- Domain aggregates will extend `org.springframework.data.domain.Persistable<UUID>` instead of `AbstractAggregateRoot`
- Application service layer remains unchanged (same domain repository interfaces)
- SQL queries will be rewritten for JDBC template if needed

### Database Schema Changes

- Table structure remains largely the same (UUIDs, columns, constraints)
- Version columns for optimistic locking preserved
- Audit columns (`created_at`, `modified_at`, `created_by`, `modified_by`) preserved
- Collection tables (e.g., `user_roles`) will use Spring Data JDBC conventions

## Impact

### Affected Specifications

- **auth**: User aggregate persistence changes
- **members**: Member aggregate persistence changes
- **user-activation**: PasswordSetupToken persistence changes

### Affected Code Areas

- **Domain Layer**: Minimal changes (remove `AbstractAggregateRoot`, implement `Persistable`)
- **Infrastructure Layer**: Complete rewrite of persistence package
    - `/klabis-backend/src/main/java/com/klabis/users/infrastructure/persistence/`
    - `/klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/`
- **Configuration**: New JDBC auditing and event publishing configuration
    - `/klabis-backend/src/main/java/com/klabis/config/`
- **Dependencies**: Maven POM changes (remove Hibernate, add JDBC)
    - `/klabis-backend/pom.xml`
- **Database Migrations**: New Liquibase migrations for schema alignment
    - `/klabis-backend/src/main/resources/db/migration/`
- **Tests**: All persistence layer tests require updates
    - Repository tests
    - Integration tests using TestContainers

### Compatibility

- **Database Schema**: Minor changes only (backward-compatible data)
- **REST API**: No changes (domain model and API layer unaffected)
- **Domain Model**: Minimal changes (interface adjustments only)
- **Application Services**: No changes (same repository interfaces)

### Migration Path

1. Create new JDBC-based implementation alongside JPA (feature toggle)
2. Run both implementations in parallel with verification
3. Switch to JDBC implementation after successful testing
4. Remove JPA dependencies and code

### Risk Assessment

- **Medium Risk**: Complete persistence layer rewrite
- **Mitigation**: Comprehensive test coverage, parallel implementation, gradual rollout
- **Rollback Plan**: Feature toggle allows instant revert to JPA implementation
