# Project Todos

## Vyzkouset jMolecules JPA integration

**Status:** incomplete
**Priority:** medium
**Tags:** #architecture #jmolecules #jpa #exploration
**Created:** 2026-02-07

Prozkoumejte možnost integrace jMolecules s JPA (Hibernate) jako alternativy k současnému Spring Data JDBC řešení.

Mělo by se zaměřit na:
- Ověření, jak jMolecules ByteBuddy transformace fungují s JPA/Hibernate
- Posouzení kompatibility s modulární architekturou (Spring Modulith)
- Testování mapování domén entit a agregátů
- Výkon a komplexnost v porovnání s aktuálním JDBC přístupem
- Dokumentace zjištěných výhod a nevýhod
- Identifikace případů, kde by byla JPA lepší volbou než JDBC

## Migrate presentation mappers to MapStruct

**Status:** incomplete
**Priority:** medium
**Tags:** #refactor #backend #presentation
**Created:** 2026-01-20

Evaluate and migrate existing presentation layer mappers to MapStruct where feasible.

This should include:
- Audit current presentation layer mapping implementations
- Identify candidates for MapStruct migration
- Replace manual mapping code with MapStruct interfaces
- Ensure compilation-time safety and performance benefits
- Handle edge cases that may require custom mapping methods
- Update tests to work with MapStruct-generated mappers

## Add calendar aggregate

**Status:** incomplete
**Priority:** medium
**Tags:** #feature #backend #domain
**Created:** 2026-01-20

Implement the calendar aggregate in the domain model following DDD principles and the Spring Data JDBC migration pattern.

This should include:
- Calendar aggregate root entity
- Value objects for calendar-related concepts (events, schedules, etc.)
- JDBC repository implementation
- Aggregate design with proper boundaries
- Integration with existing Spring Modulith architecture

## Add finance account aggregate

**Status:** incomplete
**Priority:** medium
**Tags:** #feature #backend #domain
**Created:** 2026-01-20

Implement the finance account aggregate in the domain model following DDD principles and the Spring Data JDBC migration pattern.

This should include:
- Finance account aggregate root entity
- Value objects for account properties
- JDBC repository implementation
- Aggregate design with proper boundaries
- Integration with existing Spring Modulith architecture


## Add AOP aspect to handle event idempotency

**Status:** incomplete
**Priority:** high
**Tags:** #feature #aop #events #idempotency #spring-modulith
**Created:** 2026-01-23

Implement an AOP aspect to ensure event idempotency by preventing duplicate event processing.

The aspect should:
- Check if eventId was already processed by querying the database
- If eventId exists, skip processing (event already handled)
- If eventId is new, proceed with aspected code execution
- After successful processing, save eventId to database for future idempotency checks

**Technical Requirements:**
- Create custom annotation (e.g., `@IdempotentEventHandler`)
- Implement AOP aspect using `@Around` advice
- Create repository for storing processed event IDs
- Handle concurrent processing scenarios (use database constraints)
- Consider TTL/cleanup strategy for old event IDs

**Integration Points:**
- Apply to Spring Modulith event listeners
- Ensure aspect executes within existing transaction context
- Log when duplicate events are detected

### Notes

- Related to Spring Modulith event-driven architecture
- Should work with transactional outbox pattern
- Consider using event_publication table or separate idempotency table

## Add Member Update E2E Test

**Status:** incomplete
**Priority:** low
**Tags:** #testing #e2e #members
**Created:** 2026-01-23

Create automated E2E test for complete member update flow to complement existing manual tests in member-management.http.

Test should cover:
- Create a new member via POST /api/members
- Retrieve the created member via GET /api/members/{id}
- Update member fields via PATCH /api/members/{id}
- Retrieve updated member and verify changes persisted correctly
- Test both required and optional field updates

**Context:** Currently only manual .http tests exist for member updates. While this provides adequate coverage, an automated E2E test would improve regression testing and confidence in the update flow.

**Reference:** See docs/E2E_TEST_COVERAGE_ANALYSIS.md for gap analysis.

### Notes

- Integration tests already exist for UpdateMemberCommandHandler
- Manual tests in docs/examples/member-management.http (Tests 10-12) provide current coverage
- Consider adding this if update logic becomes more complex in future

## Add Concurrent Member Update Test

**Status:** incomplete
**Priority:** low
**Tags:** #testing #e2e #members #optimistic-locking
**Created:** 2026-01-23

Add test scenario for concurrent member updates to verify optimistic locking behavior.

Test should verify:
- Two users retrieve the same member simultaneously
- Both attempt to update different fields
- Second update receives 409 Conflict due to version mismatch
- Retry with latest version succeeds

**Context:** Spring Data JDBC implements optimistic locking via @Version annotation on Member entity. This behavior is not explicitly tested in E2E scenarios.

**Implementation:** Could be added as manual test scenario in member-management.http or as automated E2E test if concurrent update scenarios become common.

**Reference:** See docs/E2E_TEST_COVERAGE_ANALYSIS.md for gap analysis.

### Notes

- MemberMemento includes version field for optimistic locking
- Repository handles version conflict automatically
- Low priority because JDBC handles this at infrastructure level
- Consider documenting expected 409 Conflict response in API docs

## Document Token Cleanup Behavior

**Status:** incomplete
**Priority:** low
**Tags:** #documentation #password-setup
**Created:** 2026-01-23

Document automatic cleanup of expired password setup tokens in password-setup.http file.

Should include:
- Comment explaining tokens are automatically cleaned up
- Reference to scheduled task or cleanup mechanism
- Note that this is not tested in E2E as it's a background process

**Context:** Password setup tokens have expiration (4 hours by default). The PasswordSetupTokenRepository has deleteExpiredTokens() method but cleanup behavior is not documented in manual tests.

**Reference:** See docs/E2E_TEST_COVERAGE_ANALYSIS.md for coverage analysis.

### Notes

- Cleanup is a background task, not user-facing feature
- Unit tests exist for deleteExpiredTokens() in PasswordSetupTokenJdbcRepositoryTest
- E2E tests focus on user journeys, not background processes
