# Design: Specification Cleanup - Removing Implementation Details

## Context

The current `members` and `users` specifications were written during initial development and contain significant
implementation details that were useful for the first implementation but now create maintenance burden and coupling
between business requirements and technology choices.

### Problem Statement

The specifications mix business requirements with implementation details in the following ways:

1. **Direct code references**: Class names, method names, package names
2. **Technology coupling**: Specific frameworks (Spring, BCrypt, JWT)
3. **Architecture patterns**: Event handlers, value objects, repository patterns
4. **Configuration details**: application.yml, migration versions
5. **Database schema**: Table names, column names

This creates a maintainability issue where any implementation refactor potentially requires specification updates,
violating the principle that requirements should be stable and implementation should be able to evolve independently.

## Goals / Non-Goals

### Goals

- **Separate concerns**: Business requirements separate from implementation details
- **Technology independence**: Requirements don't change when technology stack changes
- **Improved clarity**: Business stakeholders can read and understand requirements
- **Reduced maintenance**: Fewer spec updates during code refactoring
- **Better traceability**: Clear mapping from business need to requirement

### Non-Goals

- ~~Changing business logic~~ - All business rules preserved
- ~~Changing API contracts~~ - HTTP endpoints and formats unchanged
- ~~Removing technical constraints~~ - Security/compliance requirements kept
- ~~Rewriting requirements~~ - Existing requirements refined, not rewritten
- ~~Creating implementation guidance~~ - Implementation patterns documented separately (ARCHITECTURE.md)

## Decisions

### Decision 1: Remove All Java Implementation References

**What**: Remove all references to Java classes, methods, packages, and language features

**Why**:

- Requirements should be technology-agnostic
- Java is an implementation detail, not a business requirement
- Future implementations could use Kotlin, Scala, or other JVM languages
- Method names and package structure are internal architecture choices

**Alternatives Considered**:

1. **Keep Java references** - Rejected: Ties requirements to specific technology
2. **Generalize to "object-oriented"** - Rejected: Still too specific
3. **Remove all code references** - **Selected**: Most flexible approach

**Trade-offs**:

- (+) Technology-independent requirements
- (+) Clearer business intent
- (+) Easier to maintain
- (-) Loses implementation guidance (mitigated by ARCHITECTURE.md)

### Decision 2: Simplify Technology References

**What**: Keep generic technology concepts, remove specific framework/product names

**Examples**:

- "Spring Authorization Server" → "OAuth2 authorization server"
- "BCrypt" → "cryptographically secure hashing" (unless compliance requires BCrypt)
- "JWT" → "access token" (keep "JWT" if specifically required)
- "SHA-256" → "cryptographic hash function" (unless compliance requires SHA-256)

**Why**:

- Frameworks change (Spring Boot 2→3→4, Spring Security changes)
- Cryptographic algorithms evolve (BCrypt→Argon2, SHA-256→SHA-3)
- Business requirements shouldn't prescribe technical choices

**Alternatives Considered**:

1. **Keep all technology names** - Rejected: Too rigid
2. **Remove all technology references** - Rejected: Loses important context
3. **Keep generic, remove specific** - **Selected**: Balanced approach

**Trade-offs**:

- (+) Implementation flexibility
- (+) Requirements stay current through tech upgrades
- (-) Less specific guidance for implementers
- (-) Could lead to inconsistent technology choices (mitigated by ARCHITECTURE.md)

### Decision 3: Keep External API Contracts

**What**: Keep HTTP endpoints, methods, status codes, request/response formats

**Why**:

- These define the external interface consumed by frontend
- Frontend team relies on these for integration
- API contracts are relatively stable (breaking changes are rare)
- These ARE business requirements (external behavior, not implementation)

**Scope**:

- **Keep**: Endpoint paths, HTTP verbs, status codes, media types
- **Keep**: Request/response structures (JSON fields)
- **Keep**: Error response formats (problem+json, HAL+FORMS)
- **Remove**: Internal implementation of endpoints

**Alternatives Considered**:

1. **Remove all HTTP details** - Rejected: Loses API contract definition
2. **Move to separate API doc** - Rejected: Duplicates maintenance, risks divergence
3. **Keep in spec** - **Selected**: API spec is part of requirements

**Trade-offs**:

- (+) Single source of truth for API contract
- (+) Frontend team has clear interface definition
- (+) Can generate API docs from requirements
- (-) Mixes API contract with business requirements (acceptable trade-off)

### Decision 4: Preserve Security and Compliance Requirements

**What**: Keep security requirements even if they reference specific technologies

**Examples**:

- Keep: "OAuth2 authentication" (industry standard)
- Keep: "Password SHALL be cryptographically hashed"
- Question: "BCrypt" (keep if compliance requires, else simplify)
- Question: "SHA-256" (keep if compliance requires, else simplify)

**Why**:

- Security requirements often driven by compliance (GDPR, SOC2, PCI-DSS)
- Specific algorithms may be required for certification
- Security is a business requirement, not implementation detail

**Decision Criteria**:

- If regulation requires specific algorithm → Keep it
- If it's internal security standard → Can simplify to "industry standard"
- When in doubt → Keep specific (can be simplified later)

**Alternatives Considered**:

1. **Remove all algorithm names** - Rejected: Loses compliance context
2. **Keep all technology names** - Rejected: Too rigid
3. **Context-dependent** - **Selected**: Keep if compliance-driven

**Trade-offs**:

- (+) Maintains compliance clarity
- (+) Clear security requirements
- (-) Some technology coupling (justified for security)

### Decision 5: Convert Event Flow to Business Flow

**What**: Remove event implementation details, keep business sequence

**Before**:
> "Token generation SHALL be triggered by `UserCreatedEvent` published when User entity is created. The event SHALL be
> handled by `PasswordSetupEventListener` in users module."

**After**:
> "WHEN a new user account is created with PENDING_ACTIVATION status, THEN a password setup token SHALL be generated and
> sent to the user's email."

**Why**:

- Event-driven architecture is implementation, not requirement
- Business stakeholders don't care about `UserCreatedEvent`
- Event handlers could change without business impact

**Alternatives Considered**:

1. **Keep event details** - Rejected: Implementation coupling
2. **Remove all flow** - Rejected: Loses business process context
3. **Describe business flow only** - **Selected**: Right level of abstraction

**Trade-offs**:

- (+) Clear business process
- (+) Implementation flexibility
- (-) Loses architectural guidance (mitigated by ARCHITECTURE.md)

## Architecture Impact

### Before: Mixed Requirements and Implementation

```
Business Requirement ← Tightly Coupled → Implementation Details

Example:
"WHEN MemberService.registerMember() is called
AND RegisterMemberRequest contains valid data
THEN User entity is created via User.create()
AND UserCreatedEvent is published
AND PasswordSetupEventListener handles event"
```

### After: Separated Concerns

```
Specifications: Pure Business Requirements
Design Docs: Architecture and Patterns
Code: Implementation

Example Spec:
"WHEN a new member is registered
AND all required information is provided
THEN a user account is created
AND a password setup token is generated and emailed"
```

### Documentation Structure

**After cleanup**, documentation will have clear separation:

1. **OpenSpec Specs** (this repository):
    - Business requirements (WHAT)
    - External API contracts (interface)
    - Validation rules
    - Business workflows

2. **ARCHITECTURE.md** (in klabis-backend):
    - Technology choices (WHY)
    - Architecture patterns (DDD, Clean Architecture)
    - Module structure
    - Design decisions

3. **ADR (Architecture Decision Records)** (in klabis-backend):
    - Why BCrypt was chosen
    - Why event-driven architecture
    - Why Spring Modulith
    - Technology upgrade decisions

4. **Code Comments**:
    - Implementation details
    - Class/method documentation
    - Design patterns in use

## Code Examples

### Example 1: Value Object Requirements

**Before (Implementation-Heavy)**:

```gherkin
### Requirement: EmailAddress Value Object

The system SHALL validate email addresses using RFC 5322 basic format (must contain @ symbol and valid domain).

#### Scenario: Valid email address accepted
- **GIVEN** email value is "john@example.com"
- **WHEN** EmailAddress.of() is called
- **THEN** EmailAddress value object is created successfully
```

**After (Business-Focused)**:

```gherkin
### Requirement: Email Address Validation

The system SHALL validate email addresses to ensure they are properly formatted and deliverable.

Validation requirements:
- Must contain @ symbol separating local part and domain
- Domain must be valid (at least one dot, valid characters)
- Local part must not be empty
- Overall length must not exceed 254 characters (RFC 5322 limit)

#### Scenario: Valid email address accepted
- **GIVEN** email value is "john@example.com"
- **WHEN** an email address is provided
- **THEN** validation passes
- **AND** the email is stored for future use
```

### Example 2: Authentication Flow

**Before (Implementation-Heavy)**:

```gherkin
### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 with Spring Authorization Server using registrationNumber as username and BCrypt-hashed password.

#### Scenario: Successful authentication with valid credentials
- **WHEN** user submits valid registrationNumber and password to /oauth2/token
- **THEN** system returns JWT access token (15 min TTL) and opaque refresh token (30 day TTL)
- **AND** access token contains claims: registrationNumber, authorities, expiration
```

**After (Business-Focused)**:

```gherkin
### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 using their registration number and password.

#### Scenario: Successful authentication with valid credentials
- **WHEN** user submits valid registration number and password to the authentication endpoint
- **THEN** the system returns an access token and refresh token
- **AND** the access token expires after 15 minutes
- **AND** the refresh token expires after 30 days
- **AND** the access token contains user identification and permissions
- **AND** the password is verified using secure cryptographic comparison
```

### Example 3: Audit Logging

**Before (Implementation-Heavy)**:

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
  - IP address: extracted from HTTP request by AuditLogAspect
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
  - Actor: user who made the change (registration number)
  - Target: user whose permissions were changed
  - IP address: requester's network address
  - Old permissions: list of permissions before the change
  - New permissions: list of permissions after the change
  - Timestamp
- **AND** the audit entry is persisted atomically with the permission change
```

## Migration Plan

### Step-by-Step Process

#### Phase 1: High-Priority Cleanup (Day 1)

**Target**: Remove all Java implementation references

1. **Remove class/method names**:
    - `MemberService`, `UserService`, `RegisterMemberRequest`
    - `UpdateUserPermissionsCommandHandler`, `PasswordComplexityValidator`
    - `registerMember()`, `Address.of()`, `EmailAddress.of()`

2. **Remove package references**:
    - `com.klabis.users.domain`
    - `com.klabis.members.application`
    - All `in users module`, `in members module`

3. **Remove Java patterns**:
    - "Java record", "compact constructor"
    - "equals()", "hashCode()", "immutable (record property)"

**Validation**:

- Grep for remaining class names:
  `rg -i "class|\.of\(|Service|Handler|Validator" openspec/changes/cleanup-spec-implementation-details/specs/`
- Grep for packages: `rg "com\.klabis\." openspec/changes/cleanup-spec-implementation-details/specs/`

#### Phase 2: Medium-Priority Cleanup (Day 2)

**Target**: Simplify configuration and architecture details

1. **Simplify configuration references**:
    - "application.yml" → "system configuration"
    - "database migration V002" → "system initialization"

2. **Remove event implementation**:
    - "triggered by `UserCreatedEvent`"
    - "handled by `PasswordSetupEventListener`"
    - "Spring Data publishes event"

3. **Generalize technology references**:
    - "Spring Authorization Server" → "OAuth2 authorization server"
    - "BCrypt" → "cryptographic hashing" (unless compliance)
    - Keep as generic: "access token", "refresh token"

**Validation**:

- Grep for events: `rg -i "event|listener|publisher" openspec/changes/cleanup-spec-implementation-details/specs/`
- Grep for config: `rg -i "application\.yml|migration V" openspec/changes/cleanup-spec-implementation-details/specs/`

#### Phase 3: Review and Refine (Day 3)

**Target**: Context-dependent cleanup decisions

1. **Review HTTP/API details**:
    - Keep: Endpoint paths, methods, status codes
    - Keep: Request/response formats
    - Question: Should these be in separate API doc?

2. **Review security details**:
    - Keep: OAuth2, authentication, authorization
    - Question: BCrypt, SHA-256 (keep if compliance)

3. **Review HAL+FORMS references**:
    - Keep if API contract definition
    - Simplify if business requirements only

4. **Validate with stakeholders**:
    - Product owner: Are requirements clear?
    - Developers: Is implementation guidance sufficient?
    - Security: Are compliance requirements preserved?

**Validation**:

- OpenSpec validation: `openspec validate cleanup-spec-implementation-details --strict`
- Review with team
- Update based on feedback

### Rollback Strategy

If issues arise during cleanup:

```bash
# Revert to original specs
git checkout openspec/specs/members/spec.md
git checkout openspec/specs/users/spec.md

# Or revert specific changes
git revert <commit-hash>
```

### Testing Strategy

**Validation Tests** (not unit tests):

1. **Requirement completeness**:
    - All business rules preserved
    - No validation logic lost
    - All scenarios still present

2. **Requirement clarity**:
    - Business stakeholders understand requirements
    - No ambiguous requirements
    - Clear WHAT without describing HOW

3. **OpenSpec validation**:
   ```bash
   openspec validate cleanup-spec-implementation-details --strict
   ```

4. **Manual review**:
    - Compare old and new specs side-by-side
    - Verify no functional changes
    - Check all implementation references removed

## Risks / Trade-offs

### Risk 1: Loss of Implementation Context

**Risk**: Developers lose valuable implementation guidance

**Probability**: Medium
**Impact**: Medium

**Mitigation**:

- Update ARCHITECTURE.md with implementation patterns
- Create ADRs for key technical decisions
- Add code comments for complex implementations
- Document design patterns in separate technical specs

**Success Criteria**:

- Developers can implement from updated specs + ARCHITECTURE.md
- Implementation quality maintained
- No increase in implementation questions

### Risk 2: Requirements Become Too Vague

**Risk**: Over-simplification creates ambiguous requirements

**Probability**: Low
**Impact**: High

**Mitigation**:

- Keep all business rules and validation logic
- Keep all external API contracts
- Add examples for complex requirements
- Peer review all requirement changes
- Validate with business stakeholders

**Success Criteria**:

- No ambiguous requirements
- All validation rules explicit
- Business stakeholders approve changes

### Risk 3: Stakeholder Resistance

**Risk**: Team prefers current format with implementation details

**Probability**: Medium
**Impact**: Low

**Mitigation**:

- Clearly communicate benefits
- Provide before/after examples
- Phased approach (gradual cleanup)
- Team training on requirements writing
- Gather feedback early and often

**Success Criteria**:

- Team understands and supports changes
- Updated specs approved by stakeholders
- No request to revert changes

### Risk 4: Incomplete Cleanup

**Risk**: Some implementation references missed

**Probability**: Medium
**Impact**: Low

**Mitigation**:

- Systematic grep searches for patterns
- Multiple review passes
- Automated validation where possible
- Team reviews for missed references

**Success Criteria**:

- All known patterns removed
- Grep searches return minimal results
- No implementation references in final specs

## Open Questions

### Q1: Where to document implementation patterns?

**Options**:

1. ARCHITECTURE.md in klabis-backend
2. ADR (Architecture Decision Records)
3. Code comments and documentation
4. Separate technical specification docs

**Recommendation**: Combination of all

- ARCHITECTURE.md for overall architecture
- ADRs for specific decisions
- Code documentation for implementation details

### Q2: Should we version the specifications?

**Options**:

1. v1 (with implementation) and v2 (business-focused)
2. Just replace current specs
3. Git history only

**Recommendation**: v1→v2 transition

- Mark as breaking change to spec format
- Archive old specs for reference
- Clear communication of change

### Q3: How to handle compliance requirements?

**Context**: Some implementation details driven by compliance (GDPR, security standards)

**Options**:

1. Keep specific algorithms in compliance notes
2. Reference compliance documents
3. Simplify to "compliant with [standard]"

**Recommendation**: Context-dependent

- If regulation requires specific tech → Keep with compliance reference
- If internal standard → Can simplify
- Add compliance notes where relevant

## Success Criteria

### Quantitative Metrics

| Metric                  | Before | After | Target         |
|-------------------------|--------|-------|----------------|
| Java class references   | 45+    | 0     | 100% removal   |
| Method references       | 30+    | 0     | 100% removal   |
| Package references      | 20+    | 0     | 100% removal   |
| Framework-specific refs | 15+    | <5    | ~95% removal   |
| Requirements (members)  | 39     | 39    | 100% preserved |
| Requirements (users)    | 28     | 28    | 100% preserved |
| Scenarios (members)     | ~120   | ~120  | 100% preserved |
| Scenarios (users)       | ~100   | ~100  | 100% preserved |

### Qualitative Outcomes

- ✅ All requirements focus on business WHAT, not technical HOW
- ✅ Business stakeholders can read and understand specs
- ✅ Technology changes don't require spec updates
- ✅ Implementation refactoring doesn't break requirements
- ✅ Specs remain stable through codebase evolution
- ✅ Implementation guidance available in ARCHITECTURE.md
- ✅ Team trained on writing business-focused requirements

## Validation Checklist

Before marking proposal complete:

- [ ] All Java class/method names removed
- [ ] All package references removed
- [ ] All Java-specific patterns removed (record, equals, hashCode)
- [ ] Configuration implementation simplified
- [ ] Event-driven implementation details removed
- [ ] Technology references generalized (where appropriate)
- [ ] Business rules preserved (no changes)
- [ ] Scenarios preserved (no changes)
- [ ] API contracts preserved (HTTP, endpoints, formats)
- [ ] Security requirements preserved
- [ ] OpenSpec validation passes
- [ ] Reviewed with development team
- [ ] Reviewed with product owner/stakeholders
- [ ] ARCHITECTURE.md updated if needed
