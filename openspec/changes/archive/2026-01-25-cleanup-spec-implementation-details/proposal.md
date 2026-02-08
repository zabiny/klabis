# Proposal: Remove Implementation Details from Specifications

## Status

**DRAFT** - Proposed for review

## Quick Reference

| Aspect            | Decision                                                                    |
|-------------------|-----------------------------------------------------------------------------|
| **Goal**          | Remove 100+ implementation references from members and users specifications |
| **Timeline**      | 3 days (phased approach)                                                    |
| **Risk Level**    | LOW (documentation-only change, zero behavior changes)                      |
| **Scope**         | members (39 requirements), users (28 requirements)                          |
| **API Details**   | ✅ **Keep** - HTTP endpoints, status codes, formats (API contract)           |
| **Security**      | ✅ **Simplify** - Generic terms (document specifics in ARCHITECTURE.md)      |
| **HATEOAS**       | ✅ **Keep** - HAL+FORMS is part of API contract                              |
| **Documentation** | ✅ **Multi-layered** - OpenSpec + ARCHITECTURE.md + ADR + code               |
| **Validation**    | All business rules preserved, 100% scenarios retained                       |
| **Next Step**     | Stakeholder review and approval                                             |

---

## Key Decisions Summary

1. **Keep HTTP/API Details**: Endpoints, methods, status codes are part of the API contract
2. **Simplify Security References**: "BCrypt" → "cryptographically hashed" (document in ARCHITECTURE.md)
3. **Keep HAL+FORMS**: Part of mandatory HATEOAS API contract
4. **Multi-Layered Docs**: Separate concerns across OpenSpec, ARCHITECTURE.md, ADR, and code
5. **No Explicit Versioning**: Git history provides traceability
6. **Preserve Ubiquitous Language**: Keep domain terms (UserId, EmailAddress) even if technical

---

## Why

The current `members` and `users` specifications contain significant implementation details that couple business
requirements to specific technology choices and code structure. This creates several problems:

### Problems Identified

1. **Implementation Leakage**: Requirements describe **HOW** the system is built rather than **WHAT** business
   functionality it provides
    - Example: "Validation performed by `PasswordComplexityValidator` in `com.klabis.users.domain`"
    - Should be: "The system shall validate passwords according to complexity rules"

2. **Technology Coupling**: Business rules tied to specific frameworks and libraries
    - Example: "BCrypt-hashed password", "JWT access token", "SHA-256 hashed"
    - Makes technology changes (e.g., switching from BCrypt to Argon2) require spec updates

3. **Architecture Brittleness**: Detailed event-driven, package structure, and service layer exposed in requirements
    - Example: "Token generation triggered by `UserCreatedEvent`"
    - Refactoring implementation becomes a "breaking change" to the spec

4. **Maintainability Burden**: 100+ implementation-specific references across both specs
    - Members spec: 45+ implementation references
    - Users spec: 60+ implementation references
    - Each code refactor potentially requires spec updates

5. **Audience Confusion**: Specs mix business requirements with implementation guidance
    - Business stakeholders can't distinguish actual requirements from technical details
    - Developers may over-constrain implementations by following specs too literally

### Benefits of Cleanup

- **Clearer Business Intent**: Requirements focus on WHAT, not HOW
- **Technology Independence**: Implementation can evolve without breaking specs
- **Reduced Maintenance**: Fewer spec updates during refactoring
- **Better Separation**: Business requirements separate from technical design
- **Future-Proof**: Accommodates technology changes (e.g., new authentication protocols)

## What Changes

### Scope

**Affected Specifications**:

- `members` - Remove 45+ implementation references
- `users` - Remove 60+ implementation references

**Not in Scope**:

- API endpoint contracts (paths, HTTP methods, status codes for external API)
- Business rules and validation logic (keep, just rephrase)
- Data format requirements (ISO-8601 dates, JSON structure)
- Security requirements (authentication, authorization) - keep, simplify implementation details

### Changes by Category

#### 1. Remove All Java Implementation References

**Remove**:

- Class names: `MemberService`, `RegisterMemberRequest`, `UpdateUserPermissionsCommandHandler`
- Method names: `registerMember()`, `Address.of()`, `EmailAddress.of()`
- Package references: `com.klabis.users.domain`, `com.klabis.members.application`
- Java patterns: "Java record", "compact constructor", "equals()", "hashCode()"

**Replace with**: Generic "the system" language

#### 2. Remove Framework-Specific Technology References

**Remove**:

- "Spring Authorization Server" → Keep "OAuth2 authorization server"
- "BCrypt" → Keep "cryptographically hashed" (or keep if security requirement)
- "JWT" vs "opaque token" → Keep as "access token" and "refresh token"
- "SHA-256" → Keep "cryptographically secure hash" (unless compliance requires specific algorithm)
- "@Auditable annotation" → Replace with "the system shall log/audit"

#### 3. Remove Configuration Implementation Details

**Remove**:

- "configured in application.yml" → "configured by system administrator"
- "database migration V002 executes" → "when system is initialized"
- "stored in password_setup_tokens table" → "stored persistently"

#### 4. Remove Event-Driven Architecture Implementation

**Remove**:

- "triggered by `UserCreatedEvent`"
- "handled by `PasswordSetupEventListener`"
- "Spring Data publishes event"
- "event outbox"

**Replace with**: Business flow description (WHEN X happens, THEN Y occurs)

#### 5. Simplify Value Object References

**Remove**:

- "stored as EmailAddress value object"
- "WHEN `Address.of()` is called"
- "PhoneNumber value object"

**Replace with**:

- "email is validated and stored"
- "WHEN an address is created"
- "phone number is validated and stored"

#### 6. Keep API Contract Details (Context-Dependent)

**Keep** (if defining external API):

- HTTP methods (GET, POST, PATCH, PUT, DELETE)
- Endpoint paths (/api/members, /api/users/{id}/permissions)
- HTTP status codes (200, 201, 400, 403, 404, etc.)
- Request/response formats (JSON structure, HAL+FORMS)
- Media types (application/prs.hal-forms+json, application/problem+json)

**Question for stakeholders**: Are these specs for business requirements or API contract definition?

- If business requirements: Consider moving API details to separate API documentation
- If API contract: Keep HTTP details as they define the external interface

## Examples

### Example 1: Password Validation

**Before (Implementation-Specific)**:

```gherkin
Password complexity requirements:
- Minimum length: 12 characters
- Maximum length: 128 characters
- Must contain: at least one uppercase letter, one lowercase letter, one digit, one special character
- Must NOT contain: user's registration number, first name, or last name
- Implementation: Validation performed by `PasswordComplexityValidator` in `com.klabis.users.domain`

#### Scenario: Valid password accepted
- **GIVEN** a user is setting or changing their password
- **AND** the password meets all complexity requirements
- **WHEN** the password is validated by `PasswordComplexityValidator`
- **THEN** the validation SHALL pass
- **AND** the password SHALL be accepted
- **AND** the validator SHALL be located in users.domain package
```

**After (Business-Focused)**:

```gherkin
The system SHALL enforce password complexity requirements to ensure user account security.

Password complexity requirements:
- Minimum length: 12 characters
- Maximum length: 128 characters
- Must contain: at least one uppercase letter, one lowercase letter, one digit, one special character
- Must NOT contain: user's registration number, first name, or last name

#### Scenario: Valid password accepted
- **GIVEN** a user is setting or changing their password
- **AND** the password meets all complexity requirements
- **WHEN** the password is submitted
- **THEN** the validation SHALL pass
- **AND** the password SHALL be accepted
```

### Example 2: User Identifier

**Before (Implementation-Specific)**:

```gherkin
### Requirement: UserId Value Object

The system SHALL implement UserId as a Java record to type-safely represent identifiers for User and Member aggregates. UserId SHALL wrap a UUID and provide type safety to prevent accidental mixing of IDs from different aggregates. The record SHALL be immutable and automatically generate equals(), hashCode(), and constructor.

#### Scenario: UserId record wraps UUID value
- **WHEN** UserId record is instantiated with a valid UUID
- **THEN** UserId instance is successfully created
- **AND** UserId exposes the UUID value through accessor method (UUID uuid())
- **AND** UserId automatically implements equals() and hashCode() based on the wrapped UUID
- **AND** UserId is immutable (record property)
```

**After (Business-Focused)**:

```gherkin
### Requirement: User and Member Identifiers

The system SHALL use unique identifiers for User and Member entities. Identifiers SHALL be universally unique, immutable, and prevent accidental mixing between User and Member contexts.

#### Scenario: Unique identifier assigned
- **WHEN** a User or Member entity is created
- **THEN** a unique identifier is generated
- **AND** the identifier is immutable once assigned
- **AND** the identifier cannot be confused between User and Member entities
```

### Example 3: Permission Audit Logging

**Before (Implementation-Specific)**:

```gherkin
### Requirement: Permission Change Audit Logging

The system SHALL log all permission changes via @Auditable annotation for audit trail and compliance.

#### Scenario: Permission update triggers audit logging via @Auditable
- **WHEN** UpdateUserPermissionsCommandHandler.handle() method completes successfully
- **THEN** @Auditable annotation triggers AuditLogAspect
- **AND** audit log entry is created with:
  - Event type: USER_PERMISSIONS_CHANGED
  - Actor: authenticated user's registration number (from SecurityContext)
  - Target: user ID from command
  - IP address: extracted from HTTP request
  - Old value: previous authorities list
  - New value: updated authorities list
  - Timestamp
- **AND** audit entry is persisted in same transaction as permission update
```

**After (Business-Focused)**:

```gherkin
### Requirement: Permission Change Audit Trail

The system SHALL record all permission changes in an audit log for compliance and security monitoring.

#### Scenario: Permission update creates audit entry
- **WHEN** a user's permissions are successfully updated
- **THEN** an audit log entry is created with:
  - Event type: USER_PERMISSIONS_CHANGED
  - Actor: user who made the change
  - Target: user whose permissions were changed
  - IP address: requester's network address
  - Old permissions: permissions before the change
  - New permissions: permissions after the change
  - Timestamp
- **AND** the audit entry is persisted atomically with the permission change
```

### Example 4: Token Generation Flow

**Before (Implementation-Specific)**:

```gherkin
#### Scenario: Token generated on member registration
- **GIVEN** a new member is being registered
- **WHEN** the member registration completes successfully
- **THEN** a unique password setup token SHALL be generated
- **AND** the token SHALL be hashed using SHA-256 before storage
- **AND** the token expiration SHALL be set to 4 hours from creation
- **AND** the token SHALL be stored in the password_setup_tokens table
- **AND** the token generation SHALL be triggered by `UserCreatedEvent` published when User entity is created
- **AND** the event SHALL be handled by `PasswordSetupEventListener` in users module
```

**After (Business-Focused)**:

```gherkin
#### Scenario: Token generated on member registration
- **GIVEN** a new member is being registered
- **WHEN** the member registration completes successfully
- **THEN** a unique password setup token is generated
- **AND** the token is cryptographically hashed before storage
- **AND** the token expires after 4 hours
- **AND** the token is stored persistently
- **AND** the token is sent to the member's email address
```

## Impact

### Affected Specifications

- `members` - 39 requirements, 45+ implementation references to remove
- `users` - 28 requirements, 60+ implementation references to remove

### Affected Documentation

- No code changes (this is a spec-only cleanup)
- No breaking changes to system behavior
- No API contract changes (HTTP endpoints unchanged)

### Migration Impact

- **Zero downtime** - Documentation-only change
- **Zero behavior changes** - System functionality unchanged
- **Spec versioning**: Recommend creating spec v2 to mark the transition

### Risks

#### Risk 1: Over-Simplification

**Risk**: Removing too much detail could make specs ambiguous for implementers

**Mitigation**:

- Keep all business rules and validation logic
- Keep all external API contracts (endpoints, request/response formats)
- Add implementation guidance as separate "implementation notes" if needed
- Review with developers to ensure clarity

#### Risk 2: Loss of Implementation Context

**Risk**: Developers might lose valuable implementation guidance

**Mitigation**:

- Document architecture patterns in separate design documents (ARCHITECTURE.md)
- Keep code comments and design docs for implementation details
- Consider adding "Implementation Guidance" section separate from requirements
- Use ADRs (Architecture Decision Records) for technical choices

#### Risk 3: Stakeholder Confusion During Transition

**Risk**: Team members accustomed to current spec format may be confused

**Mitigation**:

- Clearly document the rationale for changes
- Provide before/after examples (included in this proposal)
- Team training on business-focused requirements writing
- Version specs to mark the transition (v1 with implementation details, v2 business-focused)

## Alternatives Considered

### Alternative 1: Keep Current Specs + Add Implementation Docs

**Approach**: Keep implementation details in specs, add separate architecture documentation

**Rejected Because**:

- Doesn't solve the core problem (requirements coupled to implementation)
- Duplicate maintenance (specs still change when code refactors)
- Business stakeholders still can't read specs easily

### Alternative 2: Split Business and Technical Specs

**Approach**: Create separate "business requirements" and "technical implementation" specs

**Rejected Because**:

- Creates synchronization burden (two specs to maintain)
- Risk of divergence between business and technical specs
- Harder to trace requirements through to implementation

### Alternative 3: No Change - Accept Current State

**Approach**: Keep specs as-is, accept implementation coupling

**Rejected Because**:

- Specs will continue to drift with implementation changes
- Technology changes become breaking changes to requirements
- Violates principle that requirements should be stable

### Alternative 4: Complete Rewrite of Specs

**Approach**: Rewrite all requirements from scratch in business language

**Rejected Because**:

- Too time-consuming and risky
- Could inadvertently change business rules
- Better to iteratively refine existing specs

**Selected: Cleanup Current Specs (This Proposal)** - Pragmatic balance

- Removes implementation details while preserving business logic
- Incremental approach reduces risk
- Maintains traceability to current requirements
- Can be done in phases (high/medium/low priority items)

## Success Criteria

### Quantitative Metrics

- **Implementation references removed**: 100+ (45 in members, 60+ in users)
- **Class/method/package names removed**: All
- **Framework-specific references removed**: 90%+ (keep only if security/compliance requirement)
- **Requirements preserved**: 100% (no business logic changes)

### Qualitative Outcomes

- ✅ All requirements focus on WHAT, not HOW
- ✅ Business stakeholders can understand requirements
- ✅ Technology changes don't require spec updates
- ✅ Implementation refactoring doesn't break requirements
- ✅ Specs remain stable through codebase evolution

## Open Questions (All Resolved ✅)

### Q1: Should we keep HTTP status codes and endpoint paths? ✅ RESOLVED

**Decision**: **Keep in specifications**

**Rationale**:
See [Decision 1: Keep HTTP/API Details in Specifications](#decision-1-keep-httpapi-details-in-specifications-)

**Original Options**:

1. **Keep** - Treat as API contract specification ✅ **SELECTED**
2. **Remove** - Move to separate API documentation

---

### Q2: Should we keep cryptographic algorithm names (BCrypt, SHA-256)? ✅ RESOLVED

**Decision**: **Simplify to generic terms, document specifics in ARCHITECTURE.md/ADRs**

**Rationale**:
See [Decision 2: Simplify Security Algorithm References](#decision-2-simplify-security-algorithm-references-)

**Original Options**:

1. **Keep** - If security compliance requires specific algorithms
2. **Simplify** - Replace with "industry-standard cryptography" ✅ **SELECTED**

---

### Q3: Should we keep HAL+FORMS and HATEOAS details? ✅ RESOLVED

**Decision**: **Keep as-is (part of API contract)**

**Rationale**: See [Decision 3: Keep HAL+FORMS and HATEOAS Details](#decision-3-keep-halforms-and-hateoas-details-)

**Original Options**:

1. **Keep** - As part of API contract definition ✅ **SELECTED**
2. **Simplify** - To "hypermedia API with discoverable actions"

---

### Q4: How to handle implementation guidance? ✅ RESOLVED

**Decision**: **Multi-layered documentation approach (OpenSpec + ARCHITECTURE.md + ADR + code docs)**

**Rationale**: See [Decision 4: Multi-Layered Documentation Approach](#decision-4-multi-layered-documentation-approach-)

**Original Options**:

1. **Add separate sections** - "Implementation Notes" in requirements
2. **Separate design docs** - Document patterns in ARCHITECTURE.md ✅ **SELECTED** (as part of multi-layered approach)
3. **Code comments** - Rely on code documentation ✅ **SELECTED** (as part of multi-layered approach)

---

## Decisions

The following decisions have been made to address the open questions and guide the implementation:

### Decision 1: Keep HTTP/API Details in Specifications ✅

**Decision**: Keep HTTP endpoints, methods, status codes, and request/response formats in the specifications.

**Rationale**:

- These define the **external API contract** that the frontend application relies on
- OpenSpec is designed for both business requirements AND API contract definition
- `project.md` states: "HATEOAS compliance is mandatory" - this is an architectural requirement, not implementation
  detail
- Frontend team needs a single source of truth for API integration
- Separating these into different documents would create synchronization burden and risk divergence

**What's Kept**:

- HTTP methods (GET, POST, PATCH, PUT, DELETE)
- Endpoint paths (/api/members, /api/users/{id}/permissions)
- HTTP status codes (200, 201, 400, 403, 404, etc.)
- Request/response formats (JSON structure, HAL+FORMS)
- Media types (application/prs.hal-forms+json, application/problem+json)
- Error response formats

**Implementation**:

- No changes to HTTP/API details in this proposal
- These remain as part of the requirements because they define externally visible behavior
- Future API changes will still require spec updates (as they should - they're breaking changes)

---

### Decision 2: Simplify Security Algorithm References ✅

**Decision**: Simplify specific algorithm names to generic cryptographic terms. Document specific choices in
ARCHITECTURE.md and ADRs.

**Rationale**:

- Compliance requirements are about "industry-standard cryptography" and "secure hashing", not specific algorithms
- Allows technology evolution (e.g., BCrypt → Argon2, SHA-256 → SHA-3) without breaking requirements
- Spec deltas already implement this correctly: "BCrypt" → "cryptographically hashed", "SHA-256" → "cryptographically
  secure hash"
- Specific algorithm choices are implementation decisions that should be documented separately
- If compliance requires specific algorithms in the future, they can be added with compliance notes

**What's Changed in Specs**:

- "BCrypt-hashed password" → "cryptographically hashed password"
- "SHA-256 hashed" → "cryptographically secure hash"
- "JWT access token" → "access token" (keep as "access token" and "refresh token")
- Remove implementation details like "BCrypt (12 rounds)", "SHA-256 hashing"

**Where to Document Specific Algorithms**:

**ARCHITECTURE.md** (in klabis-backend):

```markdown
## Security Implementation

### Password Storage
- **Algorithm**: BCrypt
- **Work Factor**: 12 rounds
- **Library**: Spring Security BCryptEncoder
- **Rationale**: Proven, industry-standard, adaptive work factor

### Token Hashing
- **Algorithm**: SHA-256
- **Use Case**: Password setup tokens
- **Rationale**: Fast, secure, one-way hash

### Access Tokens
- **Format**: JWT (JSON Web Token)
- **TTL**: 15 minutes
- **Claims**: registrationNumber, authorities, expiration
```

**ADR Template** (docs/adr/001-security-algorithms.md):

```markdown
# ADR 001: Security Algorithm Choices

## Status
Accepted

## Context
Need to secure passwords, tokens, and implement authentication.

## Decision
- Password hashing: BCrypt with 12 rounds
- Token hashing: SHA-256
- Access tokens: JWT format

## Rationale
- BCrypt: Adaptive work factor, proven track record
- SHA-256: Industry standard, fast for token hashing
- JWT: Standard for OAuth2 access tokens

## Consequences
- **Positive**: Industry-standard, compliant with security best practices
- **Negative**: BCrypt slower than alternatives but acceptable for authentication
- **Neutral**: JWT token revocation requires additional infrastructure
```

---

### Decision 3: Keep HAL+FORMS and HATEOAS Details ✅

**Decision**: Keep all HAL+FORMS, HATEOAS, `_links`, `_embedded`, `_templates` references in specifications.

**Rationale**:

- These are part of the **API contract**, not implementation details
- `project.md` explicitly states: "HATEOAS compliance is mandatory"
- HAL+FORMS is the specified media type: `application/prs.hal-forms+json`
- Frontend needs specific link names (`self`, `edit`, `collection`) and template structures
- These define the external interface, not how it's implemented internally

**What's Kept**:

- HAL+FORMS media type specification
- HATEOAS principles and requirements
- Link relation names: `self`, `edit`, `collection`, etc.
- `_embedded`, `_links`, `_templates` structures
- Form template specifications

**No Changes Required**: These references are appropriate for API specifications and should remain.

---

### Decision 4: Multi-Layered Documentation Approach ✅

**Decision**: Use a layered documentation strategy to separate concerns while providing complete guidance.

**Rationale**:

- Each layer serves a different purpose and audience
- Prevents overloading any single document with mixed concerns
- Allows each document to evolve independently
- Follows industry best practices for documentation

**Documentation Structure**:

#### Layer 1: OpenSpec Specifications (This Repository)

**Purpose**: Business requirements and external API contracts
**Audience**: Business stakeholders, frontend developers, API consumers
**Content**:

- Business requirements (WHAT the system does)
- API contracts (endpoints, request/response formats)
- Validation rules
- Business workflows
- **No** implementation details

#### Layer 2: ARCHITECTURE.md (In klabis-backend)

**Purpose**: Overall architecture and technology choices
**Audience**: Developers, architects, technical stakeholders
**Content**:

```markdown
## Architecture Overview

### Technology Stack
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Architecture**: Clean Architecture + DDD
- **API Style**: REST with HATEOAS/HAL+FORMS

### Module Structure
- members: Member domain and application logic
- users: User management and authentication
- common: Shared utilities and cross-cutting concerns

### Security Implementation
- Password hashing: BCrypt (12 rounds)
- Token hashing: SHA-256
- Authentication: OAuth2 with Spring Authorization Server

### Design Patterns
- Aggregates: Member, User
- Value Objects: EmailAddress, PhoneNumber, Address
- Repository Pattern: Spring Data JDBC
- Event-Driven: Spring Modulith events
```

#### Layer 3: Architecture Decision Records (ADRs)

**Purpose**: Document specific technical decisions and their rationale
**Audience**: Developers, future maintainers
**Location**: `docs/adr/` in klabis-backend
**Content**:

- `001-bcrypt-password-hashing.md`
- `002-event-driven-architecture.md`
- `003-hateoas-api-design.md`
- `004-spring-modulith-integration.md`
- `005-jdbc-over-jpa.md`

**ADR Template**:

```markdown
# ADR [NN]: [Title]

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
[What is the issue that we're seeing that is motivating this decision or change?]

## Decision
[What is the change that we're proposing and/or doing?]

## Rationale
[Why is this solution the best one? What are the trade-offs?]

## Consequences
- [Positive]: [Benefits]
- [Negative]: [Drawbacks]
- [Neutral]: [Other considerations]

## References
- [Links to relevant resources]
```

#### Layer 4: Code Documentation

**Purpose**: Implementation-level guidance
**Audience**: Developers working on the code
**Content**:

- Class-level Javadoc: "Handles member registration using DDD patterns"
- Method-level comments: Complex business logic explanations
- Inline comments: Non-obvious implementation details

**Example**:

```java
/**
 * Service for managing member registration and updates.
 *
 * <p>This service orchestrates the member registration flow, including:
 * <ul>
 *   <li>User entity creation</li>
 *   <li>Member entity creation with shared UserId</li>
 *   <li>Validation of business rules</li>
 *   <li>Welcome email triggering</li>
 * </ul>
 *
 * <p>Business rules:
 * <ul>
 *   <li>Registration number format: XXXYYDD (club code + birth year + sequence)</li>
 *   <li>Contact information required: either member's or guardian's email/phone</li>
 *   <li>Conditional rodne cislo field: only for Czech nationality</li>
 * </ul>
 *
 * @see Member
 * @see User
 * @see MemberRepository
 */
@Service
public class MemberService {
    // ...
}
```

---

### Decision 5: No Explicit Spec Versioning ✅

**Decision**: Rely on git history for spec version tracking. No explicit v1/v2 versioning in specs.

**Rationale**:

- Git provides complete history and traceability
- No "spec version" field in OpenSpec schema
- Simpler to maintain without version numbers
- Can use git tags for significant milestones if needed
- Reduces overhead and confusion

**Implementation**:

- All changes tracked via git commits
- Use descriptive commit messages: "docs(specs): remove implementation details from members and users specs"
- Optional: Create git tag for major spec format changes:
  ```bash
  git tag -a specs-v2-business-focused -m "Removed implementation details, focus on business requirements"
  git push origin specs-v2-business-focused
  ```

---

### Decision 6: Preserve Ubiquitous Language Terms ✅

**Decision**: Keep technical terms that are part of the domain's ubiquitous language, even if they sound
implementation-focused.

**Rationale**:

- Domain-Driven Design emphasizes ubiquitous language shared by developers and domain experts
- Some technical terms become business concepts through common usage
- Removing these would create confusion rather than clarity

**What's Kept** (Ubiquitous Language):

- **"UserId"** - Core domain concept, even if implemented as UUID
- **"EmailAddress"** and **"PhoneNumber"** - Business concepts, even if value objects
- **"Address"** - Domain concept, even if implemented as value object
- **"Member"** and **"User"** - Domain entities, not database tables
- **"Aggregate"** - DDD pattern that's part of the domain model
- **"Repository"** - DDD pattern for data access (business concept, not just Spring Data)

**What's Removed** (Pure Implementation):

- **Class names**: `MemberService`, `RegisterMemberRequest`, `PasswordComplexityValidator`
- **Method names**: `.of()`, `.handle()`, `.registerMember()`
- **Package names**: `com.klabis.users.domain`, etc.
- **Java specifics**: "Java record", "compact constructor", "equals()"
- **Framework details**: "Spring Authorization Server", "BCrypt", "SHA-256"

**Guideline**: If the term appears in business conversations with domain experts, keep it. If it only appears in code
reviews, remove it.

---

## Related Changes

### Documentation Updates Required in klabis-backend

This OpenSpec change requires corresponding documentation updates in the `klabis-backend` repository to provide
implementation guidance:

#### 1. Update ARCHITECTURE.md

**Add new sections**:

```markdown
## Security Implementation

### Password Storage
- **Algorithm**: BCrypt
- **Work Factor**: 12 rounds
- **Rationale**: Proven, industry-standard, adaptive work factor against brute force
- **Implementation**: Spring Security `BCryptPasswordEncoder`

### Token Hashing
- **Algorithm**: SHA-256
- **Use Case**: Password setup tokens
- **Rationale**: Fast, secure, one-way hash suitable for token validation
- **Implementation**: Java `MessageDigest` with SHA-256

### Access Tokens
- **Format**: JWT (JSON Web Token)
- **Access Token TTL**: 15 minutes
- **Refresh Token TTL**: 30 days
- **Claims**: registrationNumber, authorities, expiration, issuer
- **Rationale**: Industry standard for OAuth2, self-contained tokens
- **Implementation**: Spring Authorization Server JWT encoding

## API Design Patterns

### HATEOAS Compliance
- **Media Type**: application/prs.hal-forms+json
- **Link Relations**: self, edit, collection, first, last, next, prev
- **Form Templates**: _templates property with affordances
- **Rationale**: Hypermedia-driven API, discoverable actions for clients
- **Implementation**: Spring HATEOAS with HAL+FORMS

### Value Objects
- **Pattern**: Immutable value objects for domain concepts
- **Examples**: EmailAddress, PhoneNumber, Address, UserId
- **Rationale**: Type safety, validation at creation, prevent primitive obsession
- **Implementation**: Java records with compact constructors for validation
```

#### 2. Create ADR (Architecture Decision Records)

**Create directory structure**:

```bash
mkdir -p klabis-backend/docs/adr
```

**Create ADRs**:

**`docs/adr/001-bcrypt-password-hashing.md`**:

```markdown
# ADR 001: Use BCrypt for Password Hashing

## Status
Accepted

## Context
User passwords need to be stored securely. Must protect against:
- Rainbow table attacks
- Brute force attacks
- Hardware acceleration (GPU, ASIC)

## Decision
Use BCrypt algorithm with 12 rounds work factor.

## Rationale
- **Adaptive Work Factor**: Can increase as hardware improves
- **Salt Built-in**: Automatic salt generation, no separate storage
- **Proven**: Battle-tested, widely adopted (password hashing competition)
- **Spring Support**: Built-in BCryptPasswordEncoder

## Consequences
- **Positive**: Industry-standard, compliant with security best practices
- **Positive**: Adaptive to future hardware improvements
- **Negative**: Slower than SHA-256 but acceptable for authentication (not a bottleneck)
- **Neutral**: 12 rounds is current recommendation, may need adjustment over time

## Implementation
```java
PasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hashedPassword = encoder.encode(plainPassword);
```

## References

- [BCrypt on Wikipedia](https://en.wikipedia.org/wiki/Bcrypt)
- [Spring Security BCryptPasswordEncoder](https://docs.spring.io/spring-security/site/docs/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)
- [Password Hashing Competition](https://www.password-hashing.net/)

```

**`docs/adr/002-sha256-token-hashing.md`**:
```markdown
# ADR 002: Use SHA-256 for Token Hashing

## Status
Accepted

## Context
Password setup tokens need to be stored securely in database without storing plain text.

## Decision
Use SHA-256 for one-way hashing of password setup tokens.

## Rationale
- **Fast**: Token validation happens frequently, performance matters
- **Secure**: One-way hash, irreversible, no rainbow tables (with salt)
- **Standard**: Widely supported, FIPS approved
- **Simple**: No key management required

## Consequences
- **Positive**: Fast validation, industry standard
- **Positive**: Token value never stored in plain text
- **Negative**: Unlike BCrypt, not adaptive (acceptable for tokens - not passwords)
- **Neutral**: Tokens already have expiration as additional security layer

## Implementation
```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(plainTextToken.getBytes(StandardCharsets.UTF_8));
String hashedToken = Hex.encodeHexString(hash);
```

## References

- [NIST SHA-256 Standard](https://csrc.nist.gov/projects/hash-functions)
- [Spring Security Crypto](https://docs.spring.io/spring-security/site/docs/api/org/springframework/security/crypto/codec/Hex.html)

```

**`docs/adr/003-hateoas-api-design.md`**:
```markdown
# ADR 003: HATEOAS API Design with HAL+FORMS

## Status
Accepted

## Context
Backend API must be consumed by separate frontend application. Need discoverable, self-describing API.

## Decision
Use HATEOAS principles with HAL+FORMS media type for all REST endpoints.

## Rationale
- **Discoverability**: Frontend can navigate API via hypermedia links
- **Decoupling**: Server can change URL structure without breaking clients
- **Self-Describing**: Forms describe allowed operations and input formats
- **Standard**: HAL is mature, widely supported, IETF RFC

## Consequences
- **Positive**: Frontend doesn't hardcode URLs
- **Positive**: API evolution is backward compatible
- **Positive**: Forms provide validation rules and affordances
- **Negative**: More complex response structures
- **Negative**: Requires HAL+FORMS client library

## Implementation
- Spring HATEOAS for link generation
- RepresentationModelAssembler for resource assembly
- AffordanceBuilder for form templates

## References
- [HAL Specification (RFC 8288)](https://datatracker.ietf.org/doc/html/rfc8288)
- [HAL+FORMS Specification](https://rwmartin.org/halts/specification/)
- [Spring HATEOAS](https://spring.io/projects/spring-hateoas)
```

#### 3. Update Package Documentation

**Add/Update package-info.java files**:

```java
// com.klabis.members/domain/package-info.java
/**
 * Member domain layer.
 *
 * <p>Contains core member business logic and domain models:
 * <ul>
 *   <li>{@link com.klabis.members.domain.Member} - Member aggregate root</li>
 *   <li>{@link com.klabis.members.domain.valueobjects} - Value objects (EmailAddress, PhoneNumber, Address)</li>
 *   <li>{@link com.klabis.members.domain.repository} - Repository interfaces</li>
 * </ul>
 *
 * <h3>Domain Rules</h3>
 * <ul>
 *   <li>Registration number format: XXXYYDD (club code + birth year + sequence)</li>
 *   <li>Contact info required: Either member's or guardian's email/phone</li>
 *   <li>Rodne cislo: Only for Czech nationality</li>
 * </ul>
 *
 * @see com.klabis.members.application
 * @see com.klabis.members.presentation
 */
package com.klabis.members.domain;
```

### Action Items for Backend Team

After OpenSpec proposal is applied:

- [ ] Update `ARCHITECTURE.md` with Security and API Design sections
- [ ] Create `docs/adr/` directory
- [ ] Write ADR 001: BCrypt Password Hashing
- [ ] Write ADR 002: SHA-256 Token Hashing
- [ ] Write ADR 003: HATEOAS API Design
- [ ] Add/update `package-info.java` for key packages
- [ ] Review and approve documentation changes

---

## OpenSpec Changes Only

This OpenSpec change (`openspec/` repository) is **standalone** and does not include the backend documentation updates
listed above, which should be completed in parallel or immediately after.

## Dependencies

None - This change can proceed independently

## Timeline Estimate

- **Proposal review**: 1-2 days
- **Spec updates**: 2-3 days (can be done incrementally)
- **Validation and review**: 1-2 days
- **Total**: 1 week

**Phased Approach**:

- **Phase 1**: High-priority items (class/method/package references) - 1 day
- **Phase 2**: Medium-priority items (configuration, events) - 1 day
- **Phase 3**: Low-priority items (review HTTP/API details) - 1 day
- **Phase 4**: Validation and stakeholder review - 1-2 days
