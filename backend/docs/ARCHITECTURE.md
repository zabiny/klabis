# Klabis Backend Architecture

## Overview

The Klabis Backend is a **Spring Modulith** application implementing **Domain-Driven Design (DDD)** with event-driven
communication between bounded contexts. It follows **Clean Architecture** principles with clear separation between
domain, application, infrastructure, and presentation layers.

**Key Characteristics:**

- Modular monolith architecture with bounded contexts
- Event-driven communication using the transactional outbox pattern
- Clean Architecture layering (presentation → application → domain ← infrastructure)
- HATEOAS-based REST API with HAL+FORMS
- OAuth2 stateless authentication with JWT tokens

**For detailed implementation guides, see:**

- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Event-driven communication and outbox pattern
- [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - OAuth2 and JWT security
- [HATEOAS-GUIDE.md](./HATEOAS-GUIDE.md) - HATEOAS implementation
- [DOMAIN-MODEL.md](./DOMAIN-MODEL.md) - Domain model and data persistence

## Architecture Principles

### 1. Domain-Driven Design (DDD)

The system is organized around **bounded contexts** - distinct business domains with clear boundaries:

- **Members** - Member registration and management
- **Users** - User authentication, password setup, account lifecycle
- **Events** - Competitions, trainings and other events organized by club and member registrations to these events (
  future)
- **Finances** - Financial transactions and billing (future)
- **Common Kernel** - Shared utilities and cross-cutting concerns

### 2. Event-Driven Communication

Modules communicate **primarily via domain events** (asynchronous, transactional outbox pattern):

- Direct method calls across module boundaries are discouraged (but may be used if necessary)
- Events persisted atomically with business data (strong consistency)
- Automatic retry on failures
- Guaranteed at-least-once delivery

**For detailed event-driven architecture documentation**,
see [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md)

### 3. Modular Monolith with Spring Modulith

Spring Modulith provides:

- Automatic module structure validation
- Event-driven communication with outbox pattern
- Dependency verification (no circular dependencies)
- Module testing support

## Module Structure

```
com.klabis/
├── members/              # Member bounded context
│   ├── Member.java       # Aggregate root
│   ├── Members.java      # Public query API (read-only)
│   ├── *.java            # Domain types at root (Address, EmailAddress, etc.)
│   ├── registration/     # Registration feature (RegistrationService, Controller)
│   ├── management/       # Management feature (ManagementService, Controller)
│   ├── persistence/      # Repository interfaces + JDBC implementations
│   └── shared/           # Shared utilities
├── users/                # User bounded context
│   ├── User.java         # Aggregate root (authentication only - identity, credentials, status)
│   ├── Users.java        # Public query API (read-only)
│   ├── *.java            # Domain types at root (AccountStatus, Authority, etc.)
│   ├── authorization/    # Authorization feature (PermissionService, PermissionController)
│   ├── passwordsetup/    # Password setup feature (PasswordSetupService, Controller)
│   ├── persistence/      # Repository interfaces + JDBC implementations
│   └── shared/           # Shared utilities
├── events/               # Events bounded context
│   ├── Event.java        # Aggregate root
│   ├── Events.java       # Public query API (read-only)
│   ├── *.java            # Domain types at root (EventId, EventStatus, etc.)
│   ├── management/       # Event management feature
│   ├── registration/     # Event registration feature
│   └── persistence/      # Repository interfaces + JDBC implementations
├── config/               # Spring configuration (Security, OAuth2, HATEOAS, Modulith)
└── common/               # Shared kernel (email, rate limiting, utilities)
```

**Key Principles:**

- Each bounded context have own module (users, members, events, .. )
- Domain types (aggregates, value objects, enums) at module root for immediate discoverability
- Features/use-cases grouped in feature packages with services and controllers together
- Infrastructure separated but not exposed outside module
- DTOs and exceptions co-located with the features that use them

**For complete package structure documentation**, see [PACKAGE-STRUCTURE.md](./PACKAGE-STRUCTURE.md)

## Module Communication

```
┌─────────────────────────────────────────────────────────────┐
│                    members (Module)                          │
│  Domain: Member registration, membership management         │
│  Events: MemberCreatedEvent                                 │
│  Dependencies: users, config                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ MemberCreatedEvent
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    users (Module)                            │
│  Domain: User authentication, authorization, password setup  │
│  - User aggregate: Identity, credentials, account status    │
│  - UserPermissions aggregate: Authorities and permissions   │
│  Events: UserCreatedEvent (triggers password setup email)   │
│  Dependencies: config                                        │
│  Exposed to other modules: UserPermissions, UserPermissionsRepository (via NamedInterface) │
└─────────────────────────────────────────────────────────────┘
```

**Rules:**

- Modules communicate via domain events only
- No direct method calls across module boundaries
- Each module has its own package structure
- Shared types (value objects) defined in `common` module
- Circular dependencies are prevented (verified by Spring Modulith tests)

**For detailed event flow documentation**, see [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md)

## Current Event Flows

**Member Created → Password Setup Email**

- Event: `MemberCreatedEvent`
- Published by: `members` module (when Member aggregate saved)
- Consumed by: `members` module (`MemberCreatedEventHandler`)
- Side Effect: Sends password setup email via email service

## Clean Architecture Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Members   │  │     Users   │  │   Common    │         │
│  │ Controllers │  │ Controllers │  │  Controllers │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Member      │  │ User       │  │ Password   │         │
│  │ Service     │  │ Service    │  │ Setup Service│         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Event       │  │ Rate       │  │    Email    │         │
│  │ Handlers   │  │ Limiter    │  │   Service   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Member      │  │     User    │  │  TokenHash  │         │
│  │ Aggregate   │  │ Aggregate   │  │ Value Object│         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Repository  │  │ Repository  │  │ Value Objects│        │
│  │ Interfaces  │  │ Interfaces  │  │             │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ JDBC        │  │ JDBC       │  │    SMTP     │         │
│  │ Repositories │  │ Repositories│  │   Client    │         │
│  │ (Mementos)  │  │ (Mementos) │  │             │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Thymeleaf   │  │   Redis    │  │   Flyway    │         │
│  │ Templates   │  │   Cache    │  │  Migrations │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## Key Design Patterns

| Pattern                        | Purpose                                                                        | Implementation                                                                                                                                  |
|--------------------------------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Domain-Driven Design**       | Bounded contexts, aggregates, value objects                                    | Module structure, domain types at module root                                                                                                   |
| **Feature-Based Organization** | Group related code by business capability                                      | Feature packages (registration/, management/, etc.)                                                                                             |
| **Service Layer**              | Application services encapsulate use cases                                     | Feature-specific services (RegistrationService, ManagementService)                                                                              |
| **Event-Driven Architecture**  | Asynchronous cross-module communication                                        | Spring Modulith outbox pattern                                                                                                                  |
| **Repository Pattern**         | Separate domain from persistence                                               | Public query APIs at module root (read-only), internal repository interfaces in persistence/, adapters + JDBC repositories in persistence/jdbc/ |
| **Memento Pattern**            | Bridge domain entities and persistence                                         | Memento classes with Spring Data JDBC annotations                                                                                               |
| **Adapter Pattern**            | Bridge public query APIs, internal repository interfaces and JDBC repositories | Repository adapters implement both public query API and internal repository, delegate to JDBC repositories                                      |
| **HATEOAS**                    | Hypermedia REST API                                                            | Spring HATEOAS with HAL+FORMS                                                                                                                   |
| **OAuth2 + JWT**               | Stateless authentication                                                       | Spring Authorization Server                                                                                                                     |

## API Design

**Media Type:** `application/prs.hal-forms+json`

**Key Features:**

- Hypermedia links (self, edit, collection, etc.)
- Embedded resources for related data
- Form templates for affordances
- RFC 7807 Problem Details for errors

**For complete HATEOAS implementation guide**, see [HATEOAS-GUIDE.md](./HATEOAS-GUIDE.md)

**For complete API reference**, see [API.md](./API.md)

## Security

**Authentication:**

- OAuth2 Authorization Server (client credentials, authorization code, resource owner password)
- JWT access tokens (15 min TTL) with custom claims
- Stateless session management

**Authorization:**

- Authority-based access control (MEMBERS:CREATE, MEMBERS:READ, etc.)
- Method-level security with `@PreAuthorize`
- Role-to-authority mapping

**Password & Token Security:**

- BCrypt password hashing (12 rounds)
- SHA-256 token hashing
- SSL/TLS 1.2+ (HTTPS only on port 8443)

**For complete security documentation**, see [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md)

## Data & Persistence

**Database:**

- PostgreSQL (production)
- H2 in-memory (development/testing)

**Migrations:**

- Flyway versioned migrations
- Naming: `V{version}__{description}.sql`

**Schema:**

- Users and authentication
- OAuth2 clients and authorizations
- Event publication (outbox pattern)
- Member data with GDPR encryption

## Email & Integration

**Email Service:**

- Thymeleaf templates (HTML + plain text)
- Graceful failure handling (email failures don't break business operations)
- Event-driven sending (asynchronous, outbox pattern)

**For complete integration documentation**, see [INTEGRATION-GUIDE.md](./INTEGRATION-GUIDE.md)

## Configuration

**Profiles:**

- `dev` - H2 database, SQL logging, H2 console enabled
- `test` - Isolated H2 for integration tests
- `prod` - PostgreSQL, production settings

**Key Configuration:**

- Application properties: `application.yml`, `application-dev.yml`, `application-prod.yml`
- Environment variables for secrets (SMTP, OAuth2, encryption)

**See:** [README.md](../README.md) for setup instructions

## Testing Strategy

**Unit Tests:**

- Domain entities and value objects
- Application services (mocked dependencies)
- Pure JUnit 5, no Spring context

**Integration Tests:**

- Repository implementations (with test database)
- Controller endpoints (MockMvc)
- Spring slice tests (`@DataJpaTest`, `@WebMvcTest`, etc.)

**E2E Tests:**

- Complete user journeys across modules
- `@ApplicationModuleTest` with full Spring context
- Real database, real event flow

**For development guidelines**, see [../CLAUDE.md](../CLAUDE.md)

## Documentation

**Architecture & Design:**

- [DOMAIN-MODEL.md](./DOMAIN-MODEL.md) - Bounded contexts, aggregates, value objects
- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Spring Modulith, outbox pattern
- [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - Security, OAuth2, JWT
- [INTEGRATION-GUIDE.md](./INTEGRATION-GUIDE.md) - Email, events, external integrations
- [PACKAGE-STRUCTURE.md](./PACKAGE-STRUCTURE.md) - Package organization guide

**API & Operations:**

- [API.md](./API.md) - Complete API reference
- [HATEOAS-GUIDE.md](./HATEOAS-GUIDE.md) - HATEOAS implementation guide
- [OPERATIONS_RUNBOOK.md](./OPERATIONS_RUNBOOK.md) - Monitoring, troubleshooting

**Getting Started:**

- [README.md](../README.md) - Setup, build, run, development guide
- [CLAUDE.md](../CLAUDE.md) - Developer guidelines and common pitfalls

---

**Last Updated:** 2026-01-31
**Version:** 3.2
**Status:** Active
