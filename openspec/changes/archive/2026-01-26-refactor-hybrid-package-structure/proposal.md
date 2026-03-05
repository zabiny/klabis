# Change: Refactor to Hybrid Package Structure

## Why

The current Spring Modulith modules use a technical layer-based package structure (`application/`, `domain/`,
`presentation/`, `infrastructure/`). Following the KISS simplification refactoring (which removed CQRS pattern and
consolidated handlers into services), this structure still creates several practical problems:

1. **Poor discoverability**: When implementing a feature like "member registration", developers must navigate across
   multiple layer packages to find all related code
2. **Scattered concerns**: Domain concepts are buried deep in layer packages rather than being exposed at the module
   level
3. **Unclear module boundaries**: Spring Modulith's `@NamedInterface` annotations are difficult to apply meaningfully
   when packages are organized by technical layers
4. **Feature fragmentation**: Code for a single business capability (e.g., member registration) is spread across
   `application/`, `domain/`, `presentation/`, and sometimes `infrastructure/`
5. **DTO confusion**: DTOs split between `application/` and `presentation/` packages with no clear ownership
6. **Team cognitive load**: New developers must understand both the business domain AND the technical layering to
   navigate effectively

The proposed **hybrid structure** balances domain-driven design with feature-based organization, building on the KISS
simplification already completed:

- **Domain model centralized** in `model/` package for clear exposure and reuse
- **Features/use-cases grouped** in feature packages with services and controllers together
- **Infrastructure separated** but not exposed outside module
- **Key types at module root** for easy access (Aggregate, Repository, Events)
- **DTOs and exceptions co-located** directly with the features that use them (no subpackages)

## What Changes

**BREAKING CHANGE**: Complete reorganization of package structure for all Spring Modulith modules

### Removed Package Structure

The following layer-based package structure will be removed:

- `application/` - Services, DTOs, event handlers
- `domain/` - Entities, value objects, repository interfaces
- `presentation/` - Controllers, request/response objects
- `infrastructure/` - Persistence implementations

### New Package Structure (Hybrid)

Each module will be reorganized as follows:

```
com.klabis.{module}/
├── {Aggregate}.java              # Aggregate root at root
├── {Aggregate}Repository.java    # Repository interface at root
├── package-info.java             # @ApplicationModule
├── model/                        # Shared domain model
│   ├── {ValueObjects}           # All value objects
│   ├── {Entities}               # Non-aggregate entities
│   ├── {Enums}                  # Domain enums
│   └── package-info.java        # @NamedInterface("domain-model")
├── {feature}/                   # Feature/use-case packages
│   ├── {Feature}Service.java   # Service specific to this feature
│   ├── {Event}Handler.java     # Event handlers for this feature
│   ├── {Controller}.java       # REST controller
│   ├── *Request.java           # Request DTOs
│   ├── *Response.java          # Response DTOs
│   ├── *DTO.java               # Other DTOs
│   ├── *Exception.java         # Feature-specific exceptions
│   └── package-info.java       # @NamedInterface("{feature}")
└── persistence/                 # Infrastructure (internal)
    ├── jdbc/
    │   ├── {Aggregate}JdbcRepository.java
    │   ├── {Aggregate}RepositoryImpl.java
    │   ├── converters/
    │   └── package-info.java  # No @NamedInterface (internal)
```

**Example for Members Module**:

```
com.klabis.members/
├── Member.java                      # Aggregate root
├── MemberRepository.java            # Repository interface
├── model/                           # Domain model
│   ├── PersonalInformation.java
│   ├── Address.java
│   └── ...
├── registration/                    # Registration feature
│   ├── RegistrationService.java    # Service with registerMember()
│   ├── MemberCreatedEventHandler.java
│   ├── RegisterMemberRequest.java
│   ├── MemberRegistrationResponse.java
│   └── package-info.java           # @NamedInterface("registration")
├── management/                      # Management feature
│   ├── ManagementService.java      # Service with getMember(), updateMember(), listMembers()
│   ├── MemberController.java
│   ├── UpdateMemberRequest.java
│   ├── MemberDetailsResponse.java
│   ├── SelfEditNotAllowedException.java
│   ├── AdminFieldAccessException.java
│   └── package-info.java           # @NamedInterface("management")
└── persistence/
    └── jdbc/                        # Internal (no @NamedInterface)
```

**Key Design Decision**: Services are split by feature (RegistrationService, ManagementService) rather than having a
single MemberService. This ensures each feature package is self-contained with its own service, DTOs, controller, and
exceptions.

### Modified Components

All Java source files will be moved to new packages, requiring import statement updates across:

- All services (MemberService, UserService, etc.)
- All controllers
- All event handlers
- All DTOs
- Tests for all affected modules

**Note**: Following the KISS simplification refactoring, Command/Query handlers have already been consolidated into
services. This migration focuses on reorganizing the existing simpler structure.

### Test Structure Changes

Test packages will mirror the new source structure:

```
src/test/java/com/klabis.{module}/
├── model/                       # Tests for domain model
├── {feature}/                   # Tests for features
│   ├── {Feature}ServiceTest.java
│   └── {Controller}Test.java
└── persistence/                 # Tests for persistence layer
```

## Impact

### Affected Specifications

- **members**: Complete package reorganization
- **users**: Complete package reorganization
- **codebase-organization**: New architectural pattern for all future modules

### Affected Code Areas

**Modules affected**:

- `/klabis-backend/src/main/java/com/klabis/members/`
- `/klabis-backend/src/main/java/com/klabis/users/`
- `/klabis-backend/src/test/java/com/klabis/members/`
- `/klabis-backend/src/test/java/com/klabis/users/`

**File operations**:

- ~100+ files moved to new packages
- ~200+ import statements updated
- All `package-info.java` files updated with `@NamedInterface` annotations
- All test files moved to mirror new structure

**Spring Modulith changes**:

- New `@NamedInterface` annotations for visibility control
- `model/` package exposes domain concepts to other modules
- Feature packages (`registration/`, `management/`) expose use-case APIs
- `persistence/` remains internal (no `@NamedInterface`)

### Compatibility

- **REST API**: No changes (endpoints, request/response formats unchanged)
- **Database Schema**: No changes
- **Domain Model**: No changes (only moved to new packages)
- **Business Logic**: No changes (only package declarations)
- **Inter-module Communication**: No changes (existing event handlers and queries work, just different imports)

### Migration Path

This is a pure refactoring with no behavioral changes:

1. Create new package structure
2. Move classes to new packages
3. Update all import statements
4. Run full test suite to verify
5. Commit as single atomic change (or module-by-module)

### Risk Assessment

- **Medium Risk**: Large-scale file movement and import updates
- **Mitigation**:
    - Migrate one module at a time (members → users → others)
    - Run full test suite after each module migration
    - Use IDE refactoring tools to automatically update imports
    - Keep migration in feature branch until complete
- **Rollback Plan**: Simple git revert of migration commit(s)

### Benefits

- **Improved Discoverability**: Related code grouped by feature
- **Clearer Domain**: Domain model exposed in dedicated `model/` package
- **Better Modularity**: `@NamedInterface` annotations meaningfully applied
- **Easier Onboarding**: New developers navigate by business feature
- **Spring Modulith Alignment**: Structure matches modular architecture principles
