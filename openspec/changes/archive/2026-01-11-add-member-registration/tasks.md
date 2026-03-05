# Implementation Tasks

## Progress Summary

**Status:** ✅ **COMPLETE** - All in-scope tasks implemented and tested (75 tests passing)

**Completed Implementation:**

- ✅ Domain layer with Member aggregate and business rules (35 tests passing)
- ✅ PersonalInformation value object encapsulating personal data (18 tests)
- ✅ Domain events: MemberCreatedEvent published on member creation (17 tests)
- ✅ Infrastructure layer with JPA persistence and database migration
- ✅ Application layer with CQRS command handlers and unit tests (6 unit tests)
- ✅ Event publishing via Spring ApplicationEventPublisher
- ✅ Presentation layer with REST API and HATEOAS support
- ✅ Security & Authorization with OAuth2 Authorization Server and JWT
- ✅ Method-level security with @PreAuthorize and authority-based access control
- ✅ Input validation with Jakarta Bean Validation
- ✅ Error handling with RFC 7807 Problem Details (problem+json)
- ✅ Integration tests with TestContainers (PostgreSQL)
- ✅ API tests with MockMvc (validation, authorization, HATEOAS)
- ✅ Security tests for 401/403 scenarios (6 security tests)
- ✅ End-to-end tests for complete registration flow
- ✅ OpenAPI/Swagger documentation with annotations
- ✅ HATEOAS guide documentation
- ✅ Email service implementation (JavaMailEmailService with JavaMailSender)
- ✅ Event handlers for post-registration actions (MemberCreatedEventHandler with @Async)
- ✅ SMTP configuration with STARTTLS and auth
- ✅ OAuth2 account activation links in welcome emails
- ✅ Email template rendering with Thymeleaf (HTML + text)
- ✅ Welcome email with activation URL
- ✅ Configuration: klabis.club.code, SMTP settings, OAuth2 issuer URL

**DEFERRED (Out of Scope):** Features explicitly excluded from this change

- Transactional outbox pattern for reliable event publishing (to be implemented with Spring Modulith in future change)
- GDPR compliance enhancements (data encryption for rodne_cislo, audit logging for data access)
- Address value object (using individual fields instead)
- Rodne cislo field and validation (nationality-based conditional logic)
- PersonalInformation and ContactInformation value objects (using individual fields and Sets)

**ARCHITECTURAL NOTES:**

- **Event Publishing Strategy**: Current implementation publishes events synchronously within the transaction
  using Spring's ApplicationEventPublisher. This provides defensive behavior (transaction rollback on listener failure)
  but does not guarantee delivery if the application crashes after commit.

  **Future Enhancement**: Implement transactional outbox pattern using Spring Modulith:
    - Store events in database table within same transaction as aggregate changes
    - Publish events asynchronously after transaction commit
    - Guarantees exactly-once delivery and eventual consistency
    - Prevents dual-write problems (database + message broker)
    - Should be implemented together with email service (3.3) and event handlers (section 6)

  Reference: https://spring.io/projects/spring-modulith
  Reference: https://microservices.io/patterns/data/transactional-outbox.html

---

## 1. Domain Layer (DDD)

- [x] 1.1 Create Member aggregate root entity with invariants
- [x] 1.2 Create RegistrationNumber value object with generation logic
- [x] 1.3 Create PersonalInformation value object (name, birth date, nationality, gender) - COMPLETED (with 18 tests)
- [ ] 1.4 Create ContactInformation value object (email, phone) - DEFERRED (using Sets)
- [x] 1.5 Create GuardianInformation value object for minors
- [ ] 1.6 Create Address value object - DEFERRED (out of scope for this change)
- [x] 1.7 Create MemberRepository interface
- [x] 1.8 Write unit tests for Member aggregate business rules (10 tests)
- [x] 1.9 Write unit tests for value objects validation (13 RegistrationNumber + 12 RegistrationNumberGenerator tests)

## 2. Application Layer

- [x] 2.1 Create CreateMemberCommand DTO (implemented as RegisterMemberCommand)
- [x] 2.2 Create MemberRegistrationService with TDD (implemented as RegisterMemberCommandHandler)
- [x] 2.3 Implement registration number generation service (RegistrationNumberGenerator)
- [x] 2.4 Create MemberCreatedEvent domain event (published via ApplicationEventPublisher with 17 tests)
- [x] 2.5 Write unit tests for application services (RegisterMemberCommandHandlerTest with 6 unit tests)

## 3. Infrastructure Layer

- [x] 3.1 Implement JPA MemberRepository with Spring Data (MemberJpaRepository + MemberRepositoryImpl)
- [x] 3.2 Create database schema migration (Flyway) - V001__create_members_table.sql
- [x] 3.3 Implement welcome email service with JavaMailSender (JavaMailEmailService, EmailMessage, EmailProperties)
- [x] 3.4 Write integration tests for repository with TestContainers

## 4. Presentation Layer (REST + HATEOAS)

- [x] 4.1 Create MemberController with POST /api/members endpoint
- [x] 4.2 Create CreateMemberRequest DTO with validation annotations (implemented as RegisterMemberRequest)
- [x] 4.3 Create MemberResponse DTO with HAL+FORMS support (MemberRegistrationResponse with EntityModel)
- [x] 4.4 Implement HAL+FORMS hypermedia assembler (basic implementation with self link)
- [x] 4.5 Implement MEMBERS:CREATE authorization check (@PreAuthorize with authority-based security)
- [x] 4.6 Implement validation error handling with problem+json
- [x] 4.7 Write API tests with MockMvc/RestAssured

## 5. Security & Authorization

- [x] 5.1 Configure Spring Authorization Server integration (AuthorizationServerConfiguration.java with JWT)
- [x] 5.2 Define MEMBERS:CREATE permission in security config (User.getAuthorities() - role-based authorities)
- [x] 5.3 Implement authorization service with @PreAuthorize (@EnableMethodSecurity in SecurityConfiguration)
- [x] 5.4 Write tests for permission checks (401, 403 scenarios) (MemberControllerSecurityTest with 6 tests)

## 6. Event Handlers (Post-Registration)

- [x] 6.1 Create MemberCreatedEventHandler (implemented with @Async and @TransactionalEventListener)
- [x] 6.2 Implement async welcome email trigger (using @Async with proper error handling)
- [x] 6.3 Generate OAuth2 account activation link (activation URL with token)
- [x] 6.4 Write tests for event handlers (MemberCreatedEventHandlerTest)

## 7. Integration & E2E

- [x] 7.1 Write end-to-end test for complete registration flow
- [ ] 7.2 Test nationality-based "rodne cislo" conditional logic (deferred - rodne cislo not implemented yet)
- [x] 7.3 Test guardian contact requirements for minors
- [x] 7.4 Test authorization scenarios (403, 401) - MemberControllerSecurityTest with 6 security tests
- [x] 7.5 Test HATEOAS link generation

## 8. Configuration

- [x] 8.1 Add `klabis.club.code` property to application.yml
- [x] 8.2 Configure SMTP settings in application.yml (spring.mail with SMTP auth and STARTTLS)
- [x] 8.3 Configure Spring Authorization Server issuer URL (spring.security.oauth2.authorizationserver.issuer)

## 9. Documentation

- [x] 9.1 Generate OpenAPI/Swagger documentation
- [x] 9.2 Document HAL+FORMS media type usage
- [x] 9.3 Add API examples to documentation

## 10. GDPR Compliance - DEFERRED (out of scope for this change)

- [ ] 10.1 Implement data encryption for sensitive fields (rodne_cislo)
- [ ] 10.2 Add audit logging for member data access
- [ ] 10.3 Document data retention policy
