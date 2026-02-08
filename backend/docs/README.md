# Klabis Backend Documentation Index

This index provides an overview of all available documentation for the Klabis Backend project.

## Quick Start

**New to the project?** Start here:

1. [README.md](../README.md) - Setup, build, and run instructions
2. [ARCHITECTURE.md](./ARCHITECTURE.md) - High-level architecture overview

**Need to integrate with the API?**

1. [API.md](./API.md) - Complete API reference with examples
2. [HATEOAS-GUIDE.md](./HATEOAS-GUIDE.md) - Understanding HAL+FORMS hypermedia

**Troubleshooting issues?**

1. [OPERATIONS_RUNBOOK.md](./OPERATIONS_RUNBOOK.md) - Monitoring and troubleshooting guide

---

## Architecture & Design

### [ARCHITECTURE.md](./ARCHITECTURE.md)

**Purpose:** High-level system architecture overview

**Contents:**

- Technology stack
- Architecture principles (DDD, event-driven, Clean Architecture)
- Module structure and communication
- Layered architecture overview
- Key design patterns
- Links to detailed documentation

**Best for:** Understanding the overall system architecture and how components interact

---

### [DOMAIN-MODEL.md](./DOMAIN-MODEL.md)

**Purpose:** Domain model and bounded contexts

**Contents:**

- Bounded contexts (Members, Users, Events, Finances, Common)
- Aggregates (Member, User, PasswordSetupToken)
- Value objects (UserId, EmailAddress, PhoneNumber, Address, TokenHash)
- Domain events (MemberCreatedEvent)
- Business rules and invariants
- Repository interfaces

**Best for:** Understanding business entities, domain logic, and data structures

---

### [PACKAGE-STRUCTURE.md](./PACKAGE-STRUCTURE.md)

**Purpose:** Hybrid package structure guide

**Contents:**

- Package organization (model/ packages, feature packages, persistence)
- Key design principles (domain model centralization, feature-based organization)
- Module structure examples (members, users)
- Working with the structure (creating features, adding value objects)
- Benefits of hybrid structure
- Migration from layer-based structure

**Best for:** Understanding code organization and navigating the codebase

---

### [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md)

**Purpose:** Event-driven communication with Spring Modulith

**Contents:**

- Why event-driven architecture (dual-write problem)
- Spring Modulith modules and communication
- Event publication flow
- Transactional outbox pattern
- Event handler implementation
- Idempotent event handlers
- Event lifecycle and monitoring
- Email sending with events

**Best for:** Understanding cross-module communication, event handlers, and outbox pattern

---

### [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md)

**Purpose:** Security, authentication, and authorization

**Contents:**

- Spring Security filter chain
- OAuth2 token flows (client credentials, authorization code, password)
- JWT authentication process
- Authorization and role-to-authority mapping
- Security configuration details
- SSL/TLS configuration

**Best for:** Understanding security architecture, OAuth2 flows, and JWT handling

---

## Integration & Implementation

### [INTEGRATION-GUIDE.md](./INTEGRATION-GUIDE.md)

**Purpose:** External system integrations

**Contents:**

- Email service (Thymeleaf templates, configuration)
- Event-driven integration (event handlers, configuration)
- OAuth2 integration (endpoints, client configuration, JWT customization)
- External API integration guidelines (best practices, error handling, logging)

**Best for:** Implementing integrations with external systems and services

---

### [HATEOAS-GUIDE.md](./HATEOAS-GUIDE.md)

**Purpose:** HATEOAS implementation with HAL+FORMS

**Contents:**

- HATEOAS principles
- HAL+FORMS specification
- Affordances and form templates
- Link relations
- Implementation examples

**Best for:** Understanding and implementing hypermedia-driven API responses

---

## API & Operations

### [API.md](./API.md)

**Purpose:** Complete API reference

**Contents:**

- All endpoints with methods and URLs
- Request/response examples
- Authentication requirements
- Authorization rules
- Error responses

**Best for:** API usage and integration

---

### [OPERATIONS_RUNBOOK.md](./OPERATIONS_RUNBOOK.md)

**Purpose:** Monitoring and troubleshooting

**Contents:**

- Health checks
- Event publication monitoring
- Database queries for investigation
- Common issues and solutions
- Performance monitoring

**Best for:** Operating and troubleshooting the application in production

---

## Archive & Historical Documentation

### [archive/OUTBOX_PATTERN.md](./archive/OUTBOX_PATTERN.md)

**Purpose:** Historical migration proposal for Spring Modulith implementation

**Note:** This document describes the migration journey from the old event handling approach to Spring Modulith with the
transactional outbox pattern. The migration was completed on January 13, 2026.

**For current implementation:** See [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md)

---

### [KISS_PRINCIPLE_REVIEW.md](./KISS_PRINCIPLE_REVIEW.md)

**Purpose:** Review of codebase for KISS principle adherence

**Best for:** Understanding code quality principles and simplicity guidelines

---

## Documentation by Role

### For Backend Developers

- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
- [PACKAGE-STRUCTURE.md](./PACKAGE-STRUCTURE.md) - Package organization guide
- [DOMAIN-MODEL.md](./DOMAIN-MODEL.md) - Business entities and rules
- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Event-driven communication
- [INTEGRATION-GUIDE.md](./INTEGRATION-GUIDE.md) - External integrations
- [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - Security implementation

### For Frontend Developers

- [API.md](./API.md) - API reference
- [HATEOAS-GUIDE.md](./HATEOAS-GUIDE.md) - Understanding hypermedia
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System overview

### For DevOps Engineers

- [README.md](../README.md) - Build, deploy, configuration
- [ARCHITECTURE.md](./ARCHITECTURE.md) - Architecture overview
- [OPERATIONS_RUNBOOK.md](./OPERATIONS_RUNBOOK.md) - Monitoring and troubleshooting
- [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - SSL/TLS configuration

### For New Team Members

1. [README.md](../README.md) - Start here
2. [ARCHITECTURE.md](./ARCHITECTURE.md) - Understand the system
3. [PACKAGE-STRUCTURE.md](./PACKAGE-STRUCTURE.md) - Learn code organization
4. [DOMAIN-MODEL.md](./DOMAIN-MODEL.md) - Learn the domain
5. [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Understand events
6. [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - Learn security

---

## Documentation Conventions

### File Naming

- `ARCHITECTURE.md` - Top-level architecture (kebab-case for multi-word)
- `EVENT-DRIVEN-ARCHITECTURE.md` - Specific architecture topic
- `DOMAIN-MODEL.md` - Domain and business logic
- `INTEGRATION-GUIDE.md` - Implementation guides
- `API.md` - API reference
- `*_RUNBOOK.md` - Operational procedures

### Content Guidelines

- **Architecture docs** - High-level, conceptual, diagrams
- **Implementation guides** - Practical, code examples, how-to
- **API docs** - Reference, examples, contracts
- **Operations docs** - Procedures, troubleshooting, monitoring

### Updates

- All documents include "Last Updated" date
- Version number for major revisions
- Status indicator (Active, Deprecated, Superseded)

---

## Contributing to Documentation

When adding new documentation:

1. **Choose appropriate file** based on type:
    - Architecture/Design → `ARCHITECTURE.md`, `DOMAIN-MODEL.md`, `EVENT-DRIVEN-ARCHITECTURE.md`
    - Implementation → `INTEGRATION-GUIDE.md`, `HATEOAS-GUIDE.md`
    - API/Operations → `API.md`, `OPERATIONS_RUNBOOK.md`

2. **Follow conventions:**
    - Use kebab-case for multi-word filenames (e.g., `EVENT-DRIVEN-ARCHITECTURE.md`)
    - Include table of contents for longer documents
    - Add "Last Updated" date at the end
    - Use Mermaid for diagrams

3. **Update this index** with new documents and cross-references

4. **Keep content focused:**
    - Architecture docs: High-level, conceptual, diagrams
    - Implementation guides: Practical, code examples, how-to
    - API docs: Reference, examples, contracts
    - Operations docs: Procedures, troubleshooting, monitoring

---

## External Resources

### Spring Framework Documentation

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/) - Comprehensive Spring Security guide
- [Spring Authorization Server](https://docs.spring.io/spring-authorization-server/reference/) - OAuth2 authorization
  server
- [Spring HATEOAS Reference](https://docs.spring.io/spring-hateoas/docs/current/reference/html/) - HATEOAS
  implementation
- [Spring Modulith Documentation](https://docs.spring.io/spring-modulith/reference/) - Event-driven modular architecture

### Standards & Specifications

- [OAuth 2.0 (RFC 6749)](https://tools.ietf.org/html/rfc6749) - OAuth2 authorization framework
- [JWT (RFC 7519)](https://tools.ietf.org/html/rfc7519) - JSON Web Token specification
- [Problem Details (RFC 7807)](https://tools.ietf.org/html/rfc7807) - Error response format
- [HAL Specification](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal) - Hypertext Application Language

### Architecture Patterns

- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html) - Reliable event
  delivery
- [Domain Events by Martin Fowler](https://martinfowler.com/eaaDev/DomainEvent.html) - Domain-driven design events
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html) - REST API maturity levels

---

**Last Updated:** 2026-01-26
