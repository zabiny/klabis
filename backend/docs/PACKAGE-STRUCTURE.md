# Package Structure Guide

## Overview

The Klabis Backend uses a **hybrid package structure** that balances Domain-Driven Design (DDD) principles with
feature-based organization. This structure was implemented on 2026-01-26 to improve code discoverability and modularity.

**Latest Update:** 2026-01-31 - Simplified package structure by moving domain types from `model/` subpackages to module
root for improved discoverability (KISS principle).

**Migration Details:** See `../openspec/changes/refactor-hybrid-package-structure/` for the complete migration proposal
and tasks.

## Package Structure

### Module-Level Organization

Each Spring Modulith module (e.g., `members`, `users`) follows this structure:

```
com.klabis.{module}/
├── {Aggregate}.java              # Aggregate root at module root
├── {Aggregate}s.java             # Public query API (read-only for other modules)
├── {Aggregate}Event.java         # Domain events at module root
├── {ValueObjects}               # All value objects (Address, Email, etc.) at module root
├── {Entities}                   # Non-aggregate entities at module root
├── {Enums}                      # Domain enums (Gender, Status, etc.) at module root
├── package-info.java            # @ApplicationModule annotation
├── {feature}/                   # Feature/use-case packages (@NamedInterface)
│   ├── {Feature}Service.java   # Service specific to this feature
│   ├── {Event}Handler.java     # Event handlers for this feature
│   ├── {Controller}.java       # REST controller
│   ├── *Request.java           # Request DTOs (directly in feature package)
│   ├── *Response.java          # Response DTOs (directly in feature package)
│   ├── *DTO.java               # Other DTOs (directly in feature package)
│   ├── *Exception.java         # Feature-specific exceptions
│   └── package-info.java       # @NamedInterface("{feature}")
├── persistence/                 # Infrastructure (internal, no @NamedInterface)
│   └── jdbc/                   # JDBC repository implementations
│       ├── {Aggregate}JdbcRepository.java    # Spring Data JDBC repository (returns Mementos)
│       ├── {Aggregate}Memento.java           # Memento pattern for persistence
│       ├── {Aggregate}RepositoryAdapter.java # Adapter implements public query API + internal repository
│       ├── converters/         # Value object converters
│       └── package-info.java  # No @NamedInterface (internal)
└── shared/                      # Shared utilities (internal, no @NamedInterface)
    ├── util/                   # Utility classes
    └── validation/             # Shared validators
```

## Key Design Principles

### 1. Domain Model at Module Root

**Location:** Directly in module package (e.g., `com.klabis.members`)

**Purpose:** All domain types immediately visible and discoverable

**Contents:**

- Aggregate roots (Member, User, Event)
- Value objects (EmailAddress, PhoneNumber, Address, etc.)
- Domain enums (Gender, AccountStatus, Authority, etc.)
- Non-aggregate entities
- Domain events published by aggregates
- Public query APIs (Members, Users, Events)

**Rationale:** Domain types are the most important part of a module. Placing them at the root level improves
discoverability and reduces unnecessary nesting.

### 2. Feature-Based Organization

**Location:** Feature packages (e.g., `registration/`, `management/`, `authentication/`)

**Purpose:** Group all code related to a business capability together

**Contents:**

- Feature-specific service (e.g., RegistrationService)
- REST controller for the feature
- Event handlers for the feature
- Request/Response DTOs (no subpackages)
- Feature-specific exceptions

**Annotation:** `@NamedInterface("{feature}")`

**Example:**

```java
package com.klabis.members.registration;

@NamedInterface("registration")
package com.klabis.members.registration;
```

### 3. Key Types at Module Root

**Location:** Module root package

**Purpose:** Easy access to fundamental types

**Contents:**

- Aggregate root (e.g., `Member.java`, `User.java`)
- Public query API (e.g., `Members.java`, `Users.java`) - read-only interface for other modules
- Domain events published by the aggregate

**Rationale:** These are the most important types in the module and should be immediately discoverable.

**Query API Pattern:**

- Public query interfaces (Members, Users, Events) expose only read operations to other modules
- Write operations (save, delete) are hidden in internal repository interfaces in persistence/ package
- This prevents other modules from accidentally modifying aggregates they don't own
- Repository adapters in persistence/jdbc implement both public query API and internal repository interface

### 4. Infrastructure Separation

**Location:** `persistence/jdbc/` package

**Purpose:** Hide implementation details from other modules

**Contents:**

- Spring Data JDBC repositories (return Mementos)
- Mementos for persistence (bridge between domain entities and database)
- Repository adapters (implement domain repository interfaces)
- Converters for value objects

**Annotation:** No `@NamedInterface` (internal package)

**Architecture:**

- Public query APIs at module root (e.g., `Members.java`, `Users.java`) - read-only for other modules
- Internal repository interfaces in persistence/ package (e.g., `MemberRepository.java`)
- JDBC repositories handle Spring Data JDBC operations (return Mementos)
- Repository adapters in persistence/jdbc implement both public query API and internal repository interface
- Memento pattern keeps domain entities pure (no Spring annotations)

**Rationale:** Persistence details and write operations are implementation concerns and should not be exposed outside
the module.

### 5. DTO and Exception Co-location

**Location:** Directly in feature packages

**Purpose:** Keep DTOs and exceptions close to where they're used

**Rule:** No `dto/` or `exception/` subpackages

**Rationale:** Improves discoverability - when you open a feature package, you see all related code.

## Examples

### Members Module Structure

```
com.klabis.members/
├── Member.java                              # Aggregate root
├── Members.java                             # Public query API (read-only)
├── MemberNotFoundException.java             # Domain exception
├── MemberCreatedEvent.java                  # Domain event
├── PersonalInformation.java                 # Value object
├── Address.java                             # Value object
├── PersonName.java                          # Value object
├── EmailAddress.java                        # Value object
├── PhoneNumber.java                         # Value object
├── RegistrationNumber.java                  # Value object
├── Gender.java                              # Domain enum
├── Nationality.java                         # Value object
├── package-info.java                        # @ApplicationModule
├── registration/                            # Registration feature
│   ├── RegistrationService.java            # Service with registerMember()
│   ├── MemberCreatedEventHandler.java      # Event handler
│   ├── RegisterMemberRequest.java          # DTO
│   ├── MemberRegistrationResponse.java     # DTO
│   └── package-info.java                    # @NamedInterface("registration")
├── management/                              # Management feature
│   ├── ManagementService.java              # Service with getMember(), updateMember()
│   ├── MemberController.java               # REST controller
│   ├── UpdateMemberRequest.java            # DTO
│   ├── MemberDetailsResponse.java          # DTO
│   ├── SelfEditNotAllowedException.java    # Exception
│   ├── AdminFieldAccessException.java      # Exception
│   └── package-info.java                    # @NamedInterface("management")
├── persistence/
│   ├── MemberRepository.java               # Internal repository interface (@apiNote documentation)
│   └── jdbc/                               # Internal (no @NamedInterface)
│       ├── MemberJdbcRepository.java       # Spring Data JDBC repository
│       ├── MemberRepositoryAdapter.java    # Adapter implements Members + MemberRepository
│       ├── MemberMemento.java              # Memento pattern
│       └── converters/
│           ├── AddressConverter.java
│           ├── GenderConverter.java
│           └── ...
└── shared/                                  # Internal utilities
    ├── validation/
    │   └── ValidationPatterns.java
    └── ...
```

### Users Module Structure

```
com.klabis.users/
├── User.java                                # Aggregate root
├── Users.java                               # Public query API (read-only)
├── UserService.java                         # Business service interface (exposed to other modules)
├── UserId.java                              # ID value object
├── UserCreatedEvent.java                    # Domain event
├── AccountStatus.java                       # Domain enum
├── Authority.java                           # Domain enum
├── ActivationToken.java                     # Value object
├── TokenHash.java                           # Value object
├── PasswordSetupToken.java                  # Entity
├── UserAuditMetadata.java                   # Value object
├── AuthorityValidator.java                  # Validator
├── package-info.java                        # @ApplicationModule
├── application/                             # Application services
│   └── UserServiceImpl.java                # UserService implementation (@Transactional)
├── authentication/                          # Authentication feature
│   ├── UserService.java                    # Legacy service (consider renaming)
│   ├── KlabisUserDetailsService.java       # Security integration
│   ├── UserController.java                 # REST controller
│   ├── PermissionsResponse.java            # DTO
│   ├── UserNotFoundException.java          # Exception
│   └── package-info.java                    # @NamedInterface("authentication")
├── authorization/                          # Authorization feature
│   ├── UserPermissions.java                # Aggregate root
│   ├── PermissionService.java              # Permission management
│   ├── AuthorizationQueryService.java     # Authorization queries
│   └── package-info.java                    # @NamedInterface("authorization")
├── passwordsetup/                           # Password setup feature
│   ├── PasswordSetupService.java           # Service
│   ├── PasswordSetupController.java        # REST controller
│   ├── PasswordSetupEventListener.java     # Event handler
│   ├── PasswordSetupRequest.java           # DTO
│   ├── PasswordSetupResponse.java          # DTO
│   ├── TokenValidationException.java       # Exception
│   ├── TokenExpiredException.java          # Exception
│   └── package-info.java                    # @NamedInterface("passwordsetup")
├── persistence/                            # Persistence layer (internal)
│   ├── UserRepository.java                 # Repository interface (hidden from other modules)
│   ├── UserPermissionsRepository.java      # Repository interface (hidden from other modules)
│   └── jdbc/                               # JDBC implementations
│       ├── UserJdbcRepository.java
│       ├── UserRepositoryAdapter.java
│       ├── UserMemento.java
│       ├── UserPermissionsJdbcRepository.java
│       └── converters/
└── shared/
    └── util/
        └── EmailUtil.java
```

**Architecture Notes:**

- `Users.java` public query API at module root exposes read operations to other modules
- `UserService` interface exposes command operations (createUserPendingActivation)
- Repository interfaces in `persistence/` package are hidden implementation details
- Other modules depend on `Users` or `UserService`, not on repository interfaces
- This follows Clean Architecture principles and proper module boundaries

## Working with the Structure

### Creating a New Feature

1. **Create feature package:** `com.klabis.{module}/{feature}/`
2. **Add @NamedInterface:** Create `package-info.java` with `@NamedInterface("{feature}")`
3. **Create service:** `{Feature}Service.java` with business logic
4. **Create controller:** `{Feature}Controller.java` for REST endpoints
5. **Add DTOs:** Request/Response classes directly in feature package (no subpackages)
6. **Add exceptions:** Feature-specific exceptions directly in feature package

**Example:**

```java
// Create feature package
package com.klabis.members.export;

@NamedInterface("export")
package com.klabis.members.export;
```

### Adding a New Value Object

1. **Place in module package:** `com.klabis.{module}/{ValueObject}.java`
2. **Make it immutable:** Use Java record
3. **Add validation:** In compact constructor

**Example:**

```java
package com.klabis.members;

public record PostalCode(String value) {
    public PostalCode {
        Assert.hasText(value, "Postal code is required");
        Assert.isTrue(value.matches("[0-9]{5}"), "Invalid format");
    }
}
```

### Creating a New Event Handler

1. **Place in feature package:** Co-locate with the feature that handles the event
2. **Annotate with @EventListener:** Spring Modulith will register it
3. **Make it idempotent:** Handle duplicate event delivery

**Example:**

```java
package com.klabis.members.registration;

@Service
public class MemberCreatedEventHandler {

    @EventListener
    void on(MemberCreatedEvent event) {
        // Handle event - idempotent implementation
    }
}
```

### Adding a Repository Implementation

1. **Public query API at module root:** `{Aggregate}s.java` (e.g., `Members.java`)
2. **Internal repository interface in persistence/:** `{Aggregate}Repository.java`
3. **Spring Data JDBC repository in persistence/jdbc:** `{Aggregate}JdbcRepository.java`
4. **Memento in persistence/jdbc:** `{Aggregate}Memento.java`
5. **Adapter in persistence/jdbc:** `{Aggregate}RepositoryAdapter.java`
6. **No @NamedInterface:** The `persistence/` package is internal

**Example:**

```java
// Public query API at module root (for other modules)
package com.klabis.members;

public interface Members {
    Optional<Member> findById(UserId id);
    Optional<Member> findByEmail(String email);
    // ... other read-only methods
}

// Internal repository interface in persistence/ (within module only)
package com.klabis.members.persistence;

/**
 * @apiNote This is internal API for use within the members module only.
 *          Other modules should use {@link Members} interface.
 */
public interface MemberRepository {
    Member save(Member member);  // Write operations hidden from other modules
    Optional<Member> findById(UserId id);
    // ... all operations
}

// Spring Data JDBC repository in persistence/jdbc
package com.klabis.members.persistence.jdbc;

@Repository
public interface MemberJdbcRepository extends CrudRepository<MemberMemento, UUID> {
    // Spring Data JDBC methods (returns Mementos)
}

// Adapter implements both public query API and internal repository
package com.klabis.members.persistence.jdbc;

@Component
@Transactional
class MemberRepositoryAdapter implements Members, MemberRepository {
    // Converts between domain entities and mementos
    // Public methods implement Members (read-only for external modules)
    // All methods implement MemberRepository (internal)
}
```

## Benefits of This Structure

### 1. Improved Discoverability

- Related code grouped by business feature
- No need to navigate across multiple layer packages
- DTOs and exceptions co-located with features that use them

### 2. Clear Module Boundaries

- `@NamedInterface` annotations explicitly define public API
- Infrastructure kept internal (no `@NamedInterface`)
- Spring Modulith validates dependencies

### 3. Easier Onboarding

- New developers navigate by business feature
- Clear separation between domain model and feature logic
- Consistent structure across all modules

### 4. Better Modularity

- Features are self-contained (service, controller, DTOs, exceptions)
- Domain model centralized for reuse
- Infrastructure hidden from other modules

## Migration from Layer-Based Structure

If you're working with code that predates 2026-01-26, you may see references to the old layer-based structure:

**Old Structure:**

```
com.klabis.{module}/
├── domain/           # Entities, value objects, repository interfaces
├── application/      # Services, event handlers, DTOs
├── infrastructure/   # Repository implementations
└── presentation/     # Controllers, request/response models
```

**New Structure (Hybrid):**

```
com.klabis.{module}/
├── {Aggregate}.java           # At module root
├── {Aggregate}Repository.java # At module root
├── model/                     # Domain model
├── {feature}/                 # Feature packages
└── persistence/jdbc/          # Infrastructure
```

**Key Changes:**

- Services split by feature (RegistrationService, ManagementService)
- DTOs moved to feature packages (no dto/ subpackages)
- Exceptions moved to feature packages (no exception/ subpackages)
- Domain model centralized in model/ package
- Key types (Aggregate, Repository) at module root

## Spring Modulith Integration

### @NamedInterface Usage

Spring Modulith uses `@NamedInterface` to control package visibility:

**Public APIs (with @NamedInterface):**

- `model/` - Exposes domain model (`@NamedInterface("domain-model")`)
- Feature packages - Expose feature APIs (`@NamedInterface("registration")`)
- Other modules can depend on these interfaces

**Internal Packages (no @NamedInterface):**

- `persistence/` - Implementation details, not exposed
- `shared/` - Internal utilities, not exposed
- Other modules cannot depend on these packages

### Module Dependencies

**Valid Dependencies:**

- Feature packages can depend on domain types at module root
- Feature packages can depend on other modules' domain types at module root
- Infrastructure can depend on any package within its module

**Invalid Dependencies (prevented by Spring Modulith):**

- Feature packages cannot depend on `persistence/` (internal)
- External modules cannot depend on `persistence/` or `shared/`
- Circular dependencies between modules

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Overall architecture overview
- [DOMAIN-MODEL.md](./DOMAIN-MODEL.md) - Bounded contexts and aggregates
- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Event-driven communication
- [OpenSpec Migration Proposal](../../../openspec/changes/refactor-hybrid-package-structure/) - Complete migration
  details

---

**Last Updated:** 2026-01-31
**Status:** Active
**Migration Date:** 2026-01-26
**Version:** 1.1
