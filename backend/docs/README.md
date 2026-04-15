# Klabis Backend — Documentation Index

This directory holds Klabis-specific reference docs that don't fit into `CLAUDE.md` or the `backend-patterns` skill, plus a curated list of external resources for the frameworks and patterns the backend builds on.

## Klabis-specific docs

- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) — module dependencies, event flows, listener conventions

For day-to-day backend conventions (aggregates, ports, controllers, HATEOAS affordances, field-level authorization, JDBC mementos, testing) load the **`backend-patterns`** skill — it is the authoritative source. Build/run/test/profile information lives in [../CLAUDE.md](../CLAUDE.md).

## External resources

The references below explain the general principles, patterns and APIs the backend uses. Prefer these over duplicating their content here.

### Spring Framework

- [Spring Boot reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) — version 4.x is in use
- [Spring Modulith reference](https://docs.spring.io/spring-modulith/reference/) — modular monolith, event publication registry, application module tests
- [Spring Security reference](https://docs.spring.io/spring-security/reference/) — filter chain, method security, authorization
- [Spring Authorization Server reference](https://docs.spring.io/spring-authorization-server/reference/) — OAuth2 token endpoints, registered clients
- [Spring HATEOAS reference](https://docs.spring.io/spring-hateoas/docs/current/reference/html/) — `EntityModel`, `RepresentationModelAssembler`, affordances
- [Spring Data JDBC reference](https://docs.spring.io/spring-data/relational/reference/jdbc.html) — aggregate-oriented persistence (used with the memento pattern in Klabis)
- [Spring Framework reference](https://docs.spring.io/spring-framework/reference/) — core, transactions, validation

### Standards & specifications

- [HAL+FORMS](https://rwcbook.github.io/hal-forms/) — hypermedia format used by all Klabis API responses (`application/prs.hal-forms+json`)
- [HAL specification (draft-kelly-json-hal)](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal)
- [OAuth 2.0 (RFC 6749)](https://www.rfc-editor.org/rfc/rfc6749)
- [OAuth 2.1 draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/) — what Spring Authorization Server tracks
- [JWT (RFC 7519)](https://www.rfc-editor.org/rfc/rfc7519)
- [Problem Details for HTTP APIs (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457) — error response format

### Architecture & patterns

- [Domain-Driven Design Reference (Eric Evans, PDF)](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)
- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Transactional Outbox Pattern (microservices.io)](https://microservices.io/patterns/data/transactional-outbox.html) — why Klabis uses Spring Modulith's event publication registry
- [Domain Events (Martin Fowler)](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Memento Pattern (Refactoring Guru)](https://refactoring.guru/design-patterns/memento) — Klabis uses this to keep domain entities free of Spring Data annotations

### Tools

- [Flyway documentation](https://documentation.red-gate.com/fd) — DB migrations (V001/V002/V003)
- [TestContainers for Java](https://java.testcontainers.org/) — PostgreSQL integration tests
- [MapStruct documentation](https://mapstruct.org/documentation/) — DTO ↔ command/domain mapping
- [Resilience4j documentation](https://resilience4j.readme.io/) — rate limiting on sensitive endpoints
- [jMolecules](https://github.com/xmolecules/jmolecules) — DDD/hexagonal annotations used in the codebase
