---
name: backend-patterns
description: Backend implementation patterns. Use this skill proactively whenever implementing, modifying, or fixing any backend Java code in this project — including aggregates, domain commands, application services (ports), spec-first REST controllers implementing generated *Api interfaces, HAL/HATEOAS wiring (HalResponseContext, ModelWithDomainPostprocessor, klabisLinkTo/klabisAfford), DTO↔domain mapping via ConversionService Converters, JDBC persistence (memento pattern, repository adapters), domain events and listeners, field-level authorization (@OwnerVisible, @HasAuthority, JsonNullable), or adding new modules. This is the authoritative source for how Klabis backend code should be structured.
user-invocable: false
version: 0.8.0
---

# Klabis Backend Patterns

Project-specific architecture patterns for the Klabis Spring Modulith application, derived from the `members` module as the canonical reference implementation.

For generic framework knowledge, use the `developer:*` skills. This skill covers **Klabis-specific conventions** only.

## Which reference to read

The layer you are touching decides what to load. Read the matching file **before** writing code — each one is the authoritative source for its layer, and the patterns differ enough between layers that guessing from a neighbouring layer produces wrong code.

| Working on | Read |
|---|---|
| Aggregates, value objects, type-safe IDs, application services/ports, exceptions | `references/domain-layer.md` |
| Controllers, generated `*Api` interfaces, HAL links/affordances, postprocessors, DTO↔domain converters, `@MvcComponent` | `references/rest-adapter.md` |
| Mementos, repository adapters, Spring Data repositories | `references/jdbc-adapter.md` |
| Publishing or consuming domain events, cross-module listeners | `references/domain-events.md` |
| Hiding/masking response fields, authorizing PATCH request fields | `references/field-security.md` |
| Adding a whole new aggregate end-to-end | `references/aggregate-checklist.md` (walks every layer in order) |
| Writing tests for any of the above | `references/testing-guide.md` |

Adding a REST endpoint also means editing the OpenAPI spec — the `klabis-api-spec` skill covers that side.

## Module Package Structure

Every Spring Modulith module follows this exact layout:

```
com.klabis.<module>/
├── domain/                    # Pure domain — NO Spring imports
│   ├── <Aggregate>.java       # Aggregate root (extends KlabisAggregateRoot)
│   ├── <Aggregate>Repository.java  # Domain port interface
│   └── ...value objects, enums
│
├── application/               # Orchestration layer
│   ├── <Feature>Port.java     # @PrimaryPort, Interface with nested command record
│   ├── <Feature>Service.java  # @Service implementation
│   └── <Module>Configuration.java  # @Configuration for module beans (if needed)
│
├── infrastructure/
│   ├── restapi/               # REST controllers, converters, postprocessors
│   │   ├── <Aggregate>Controller.java  # @RestController @PrimaryAdapter, implements <X>Api
│   │   ├── <Dto>Converter.java         # MapStruct @Mapper extends Converter<S,T>
│   │   └── ...postprocessors (often nested at the end of the controller file)
│   │   # NOTE: the <X>Api interface and request/response DTOs are GENERATED from
│   │   #       docs/openapi/spec/<module>.yaml — they are not in src/main/java
│   │
│   ├── jdbc/                  # Persistence
│   │   ├── <Aggregate>RepositoryAdapter.java  # @SecondaryAdapter
│   │   ├── <Aggregate>JdbcRepository.java     # Spring Data interface
│   │   └── <Aggregate>Memento.java            # @Table persistence class
│   │
│   └── listeners/             # Cross-module event listeners (if any)
│       └── <Module>EventsListener.java  # @PrimaryAdapter @Component
│
├── <Aggregate>Id.java         # Type-safe ID record — in root if referenced by other modules
├── <Aggregate>CreatedEvent.java  # Domain events — in root if consumed by other modules
└── <Module>Dto.java           # Cross-module read DTO (if needed)
```

**Root vs. domain decision:** Classes referenced by other modules stay in root package (public API). Everything else goes into `domain/`. To check cross-module usage:
```bash
grep -rh "import com.klabis.<module>" src/main/java/com/klabis/<other-modules>/ --include="*.java" | sort -u
```

### Cross-Module Ports Live in `<module>.application`

Ports consumed across module boundaries (jMolecules `@PrimaryPort` / `@SecondaryPort` used by another Modulith module) live in the `<module>.application` package, exposed via `@NamedInterface("application")` declared in that package's `package-info.java` — **not** in the module root package.

```java
// com/klabis/<module>/application/package-info.java
@org.springframework.modulith.NamedInterface("application")
package com.klabis.<module>.application;
```

Canonical examples: `events.application` (`EventDataProvider`, `EventScheduleQuery`), `members.application` (`MemberFinancialStatePort`, implemented by finance's `MemberFinancialStateAdapter`), `finance.application`. A consuming module imports the port from the foreign `<module>.application` named interface.

**A module depends only on another module's PRIMARY port — never on a foreign repository or any other secondary port.** Reach for the other module's `@PrimaryPort` application service; do not inject its `<Aggregate>Repository`. Example: `KlabisUserDetailsService` consumes `com.klabis.common.users.application.PermissionService` (a primary port), not `UserPermissionsRepository` (a secondary port). The Modulith `ModuleStructureVerificationTest` enforces these named-interface boundaries.

## Coding Conventions

### Jackson 3 Annotation Changes (Spring Boot 4)

Spring Boot 4 uses Jackson 3, which moved some packages — but Spring Boot wrapper annotations changed names too:
- `@JsonComponent` → `@JacksonComponent` (Spring Boot annotation)
- `@JsonMixin` → `@JacksonMixin` (Spring Boot annotation)
- Core/databind packages: `tools.jackson.core`, `tools.jackson.databind`
- **Exception**: `@JsonCreator`, `@JsonValue`, `@JsonInclude` stay in `com.fasterxml.jackson.annotation` — NOT moved

### General

- Use package-protected visibility as default for new classes — make public only when accessed from another package
- Use `org.springframework.util.Assert` for parameter validation inside methods and command record compact constructors (not raw `if` throws)
- Use `@NonNull` (from `org.jspecify`) on required service parameters; handle defaults in controller before delegating
- Refactor methods with more than 4 parameters — introduce parameter objects or command records
- Use `@MvcComponent` annotation on components in the presentation (restapi) layer
- Do not use Lombok in domain classes — use records or plain Java
- Use `@RecordBuilder` (from `io.soabase.recordbuilder`) on command records, events, and response DTOs — generates builder classes
- If object has builder, prefer builder to constructor (especially for generated classes)
