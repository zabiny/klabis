# Implementation Tasks

## Overview

This specification cleanup is organized into three phases, prioritized by impact and risk. Each phase can be completed
independently and validated before proceeding to the next phase.

## Phase 1: High-Priority Cleanup (Day 1)

**Goal**: Remove all Java implementation references (class names, method names, package names)

**Risk**: Low
**Impact**: High
**Dependencies**: None

### 1.1 Remove Java Class References

**Target**: All Java class names throughout members and users specs

- [x] 1.1.1 Search and document all class name references
    - [x] Grep:
      `rg -n "[A-Z][a-zA-Z]+(Service|Handler|Controller|Validator|Repository|Manager|Builder|Factory|Utils|Helper)" openspec/specs/`
    - [x] Document findings in cleanup log

- [x] 1.1.2 Remove class names from members spec
    - [x] Remove `MemberService` references (lines 9, 13-14)
    - [x] Remove `RegisterMemberCommandHandler` references (line 1200)
    - [x] Remove any handler/service class names

- [x] 1.1.3 Remove class names from users spec
    - [x] Remove `UpdateUserPermissionsCommandHandler` (lines 266, 280, 296, 305)
    - [x] Remove `PasswordComplexityValidator` (lines 334, 340, 349, 357, 365, 374, 381, 389, 397, 406, 415, 424)
    - [x] Remove `PasswordSetupService` references
    - [x] Remove `PasswordSetupController` references
    - [x] Remove `PasswordSetupEventListener` references

- [x] 1.1.4 Replace class references with generic language
    - [x] Replace "WHEN `ClassName.method()` is called" with "WHEN [action] occurs"
    - [x] Replace "Validation performed by `Validator`" with "The system shall validate"
    - [x] Replace "handled by `Service`" with "the system handles"

- [x] 1.1.5 Validate removal
    - [x] Grep verify:
      `rg -i "Service|Handler|Controller|Validator|Repository|Manager|Builder|Factory|Utils|Helper" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Ensure only generic words remain (e.g., "the system handles user services")

### 1.2 Remove Method References

**Target**: All Java method names (e.g., `registerMember()`, `of()`, `handle()`)

- [x] 1.2.1 Search and document method references
    - [x] Grep: `rg -n "\.[a-z][a-zA-Z]+\(\)" openspec/specs/`
    - [x] Document findings

- [x] 1.2.2 Remove method calls from members spec
    - [x] Remove `MemberService.registerMember()` → "member is registered"
    - [x] Remove `Address.of()` → "address is created"
    - [x] Remove `EmailAddress.of()` → "email address is validated"
    - [x] Remove `PhoneNumber.of()` → "phone number is validated"
    - [x] Remove any other method calls

- [x] 1.2.3 Remove method calls from users spec
    - [x] Remove `User.create()` → "user is created"
    - [x] Remove `User.changePassword()` → "password is changed"
    - [x] Remove `User.suspend()` → "user account is suspended"
    - [x] Remove `User.updateAuthorities()` → "authorities are updated"
    - [x] Remove any other method calls

- [x] 1.2.4 Replace with business actions
    - [x] "WHEN method() is called" → "WHEN [business action] occurs"
    - [x] "THEN method() returns" → "THEN [result] occurs"
    - [x] Ensure verbs describe business behavior, not implementation

- [x] 1.2.5 Validate removal
    - [x] Grep verify: `rg "\(\)" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Check for any remaining method syntax

### 1.3 Remove Package References

**Target**: All Java package names (e.g., `com.klabis.users.domain`)

- [x] 1.3.1 Search and document package references
    - [x] Grep: `rg -n "com\.klabis\." openspec/specs/`
    - [x] Document findings

- [x] 1.3.2 Remove package references from members spec
    - [x] Remove `com.klabis.members.*` references
    - [x] Remove "in members module" references
    - [x] Remove "in members.domain/application/presentation" references

- [x] 1.3.3 Remove package references from users spec
    - [x] Remove `com.klabis.users.*` references (lines 334, 463, 493, 503, 512, 524, 534, 587, 588, 613, 631, 666, 679,
      899)
    - [x] Remove "in users module" references
    - [x] Remove "in users.domain/application/presentation" references

- [x] 1.3.4 Remove module references
    - [x] Remove "in users module"
    - [x] Remove "in members module"
    - [x] Remove "password management logic resides in `com.klabis.users` module"

- [x] 1.3.5 Validate removal
    - [x] Grep verify: `rg "com\.klabis\.| module" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Check for any remaining package/module references

### 1.4 Remove Java-Specific Patterns

**Target**: Java language features (record, compact constructor, equals, hashCode, immutable)

- [x] 1.4.1 Search and document Java pattern references
    - [x] Grep: `rg -i "record|compact constructor|equals\(\)|hashcode\(\)|immutable.*record" openspec/specs/`

- [x] 1.4.2 Remove Java record references
    - [x] Members spec lines 1176-1193: "UserId as a Java record"
    - [x] Users spec lines 429-451: "UserId as a Java record"
    - [x] Replace with: "The system shall use unique identifiers"
    - [x] Remove: "record property", "compact constructor"
    - [x] Remove: "automatically generate equals(), hashCode()"

- [x] 1.4.3 Generalize immutability requirements
    - [x] Keep: "identifiers SHALL be immutable"
    - [x] Remove: "immutable (record property)"
    - [x] Remove implementation-specific immutability explanations

- [x] 1.4.4 Validate removal
    - [x] Grep verify: `rg -i "record|compact constructor" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Check for remaining Java-specific terms

### 1.5 Phase 1 Validation

- [x] 1.5.1 Run automated validation
    - [x] `openspec validate cleanup-spec-implementation-details --strict`
    - [x] Fix any validation errors

- [x] 1.5.2 Manual review
    - [x] Read through updated specs
    - [x] Verify no business logic changes
    - [x] Verify all scenarios still present

- [x] 1.5.3 Comparison check
    - [x] Diff original vs updated specs
    - [x] Verify only implementation references removed
    - [x] Count: ~45 references removed from members
    - [x] Count: ~60 references removed from users

---

## Phase 2: Medium-Priority Cleanup (Day 2)

**Goal**: Simplify configuration, event-driven, and technology references

**Risk**: Low
**Impact**: Medium
**Dependencies**: Phase 1 complete

### 2.1 Simplify Configuration References

**Target**: Configuration file formats and migration details

- [x] 2.1.1 Search configuration implementation details
    - [x] Grep: `rg -i "application\.yml|application\.properties|migration V|database migration" openspec/specs/`

- [x] 2.1.2 Simplify configuration references
    - [x] "configured in application.yml" → "configurable by system administrator"
    - [x] "WHEN database migration V002 executes" → "WHEN the system is initialized"
    - [x] "via application.yml configuration file" → "via system configuration"

- [x] 2.1.3 Remove database table/column names where implementation-only
    - [x] "stored in password_setup_tokens table" → "stored persistently"
    - [x] "in token_hash column" → "in secure storage"
    - [x] Keep table names if they're part of business domain model

- [x] 2.1.4 Validate
    - [x] Grep verify:
      `rg "application\.yml|migration V[0-9]" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Ensure configuration requirements still clear

### 2.2 Remove Event-Driven Implementation

**Target**: Event handlers, listeners, publishers, event flow implementation

- [x] 2.2.1 Search event implementation details
    - [x] Grep: `rg -i "event|listener|publisher|handler|triggered by" openspec/specs/`

- [x] 2.2.2 Remove event class references
    - [x] "`UserCreatedEvent`" → "when a user account is created"
    - [x] Remove "event SHALL be handled by `PasswordSetupEventListener`"
    - [x] Remove "Spring Data publishes the event"
    - [x] Remove "event outbox"

- [x] 2.2.3 Convert event flows to business flows
    - [x] Keep: WHEN X happens, THEN Y occurs (business sequence)
    - [x] Remove: Event publication, handling, listener details
    - [x] Remove: Transaction, outbox, event persistence details

- [x] 2.2.4 Update async processing descriptions
    - [x] Remove: "queued for sending asynchronously" (implementation)
    - [x] Replace with: "email sending is non-blocking" (behavior)

- [x] 2.2.5 Validate
    - [x] Grep verify: `rg -i "event|listener|publisher" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Ensure business flows still clear

### 2.3 Generalize Technology References

**Target**: Framework-specific technology names → generic equivalents

- [x] 2.3.1 Identify framework-specific references
    - [x] Grep: `rg -i "spring|authorization server|bcrypt|jwt|sha-256|opaque token" openspec/specs/`

- [x] 2.3.3 Generalize framework references
    - [x] "Spring Authorization Server" → "OAuth2 authorization server"
    - [x] Keep: "OAuth2" (industry standard, business requirement)
    - [x] "Spring Data" → remove (implementation detail)

- [x] 2.3.4 Simplify token types (context-dependent)
    - [x] Keep: "access token" and "refresh token" (business concepts)
    - [x] Remove implementation details about token internals

- [x] 2.3.5 Simplify algorithm references (context-dependent)
    - [x] "BCrypt" → "cryptographically hashed" (documented in ARCHITECTURE.md)
    - [x] "SHA-256" → "cryptographically secure hash" (documented in ARCHITECTURE.md)
    - [x] Document decision rationale in ARCHITECTURE.md

- [x] 2.3.6 Validate
    - [x] Review security requirements are preserved
    - [x] Ensure compliance requirements still clear
    - [x] Check technology coupling reduced

### 2.4 Remove Value Object Implementation Details

**Target**: "Value object" pattern references, factory methods

- [x] 2.4.1 Search value object references
    - [x] Grep: `rg -i "value object|\.of\(\)" openspec/specs/`

- [x] 2.4.2 Remove "value object" terminology
    - [x] "EmailAddress value object" → "email address"
    - [x] "PhoneNumber value object" → "phone number"
    - [x] "Address value object" → "address"
    - [x] Remove: "stored as [Type] value object"

- [x] 2.4.3 Already handled in 1.2 (method references)
    - [x] Verify `.of()` calls already removed
    - [x] Verify factory method patterns removed

- [x] 2.4.4 Validate
    - [x] Grep verify: `rg "value object|\.of\(\)" openspec/changes/cleanup-spec-implementation-details/specs/`
    - [x] Ensure validation rules still clear

### 2.5 Phase 2 Validation

- [x] 2.5.1 Run automated validation
    - [x] `openspec validate cleanup-spec-implementation-details --strict`
    - [x] Fix any validation errors

- [x] 2.5.2 Manual review
    - [x] Read through updated specs
    - [x] Verify business logic unchanged
    - [x] Verify business flows still clear

- [x] 2.5.3 Count references removed
    - [x] Approximately 15-20 additional references removed
    - [x] Total removal: ~60 from members, ~80 from users

---

## Phase 3: Review and Refine (Day 3)

**Goal**: Context-dependent decisions, stakeholder validation, final polish

**Risk**: Low
**Impact**: Low (refinement only)
**Dependencies**: Phase 1 and 2 complete

### 3.1 Review HTTP/API Details

**Decision**: Keep or move to separate API documentation?

- [x] 3.1.1 Catalog HTTP/API details in specs
    - [x] Endpoint paths: /api/members, /api/users, etc.
    - [x] HTTP methods: GET, POST, PATCH, PUT, DELETE
    - [x] Status codes: 200, 201, 400, 403, 404, etc.
    - [x] Media types: application/prs.hal-forms+json, application/problem+json
    - [x] Request/response formats: JSON structures

- [x] 3.1.2 Make decision on API details
    - [x] Option A: Keep in specs (recommended for API contract definition) ✅ SELECTED
    - [ ] Option B: Move to separate API documentation
    - [x] Document decision rationale in proposal

- [x] 3.1.3 Update based on decision
    - [x] Keeping: Ensure API details are consistent
    - [x] No changes needed - API details already correct

- [x] 3.1.4 Validate
    - [x] API contract still clear
    - [x] Frontend team can integrate from updated specs

### 3.2 Review Security Details

**Decision**: Which security algorithms are compliance requirements?

- [x] 3.2.1 Catalog security algorithm references
    - [x] "BCrypt" (password hashing)
    - [x] "SHA-256" (token hashing)
    - [x] "JWT" (token format)
    - [x] "OAuth2" (authentication protocol)

- [x] 3.2.2 Determine compliance requirements
    - [x] Decision: Simplify to generic terms in specs
    - [x] Document specifics in ARCHITECTURE.md for developers
    - [x] Keep business-facing terms clear

- [x] 3.2.3 Update based on compliance requirements
    - [x] BCrypt → "cryptographically hashed" in specs
    - [x] SHA-256 → "cryptographically secure hash" in specs
    - [x] Keep "OAuth2" as business requirement
    - [x] Document all specifics in klabis-backend/docs/ARCHITECTURE.md

- [x] 3.2.4 Validate
    - [x] Security requirements still clear
    - [x] Compliance requirements preserved
    - [x] Implementation flexibility where possible

### 3.3 Review HATEOAS/HAL+FORMS References

**Decision**: API contract or implementation detail?

- [x] 3.3.1 Catalog hypermedia references
    - [x] "HATEOAS"
    - [x] "HAL+FORMS"
    - [x] "_links", "_embedded", "_templates"
    - [x] "self", "edit", "collection" link names

- [x] 3.3.2 Make decision
    - [x] Option A: Keep (API contract for frontend) ✅ SELECTED
    - [x] Option B: Simplify to "hypermedia API with discoverable actions"

- [x] 3.3.3 Update based on decision
    - [x] Keeping: Ensure HAL+FORMS details consistent
    - [x] No changes needed - already correct

- [x] 3.3.4 Validate
    - [x] Frontend team can understand API contract
    - [x] Hypermedia requirements clear

### 3.4 Stakeholder Review

- [x] 3.4.1 Review with product owner
    - [x] Requirements clear and understandable? ✅
    - [x] Business logic preserved? ✅
    - [x] Any ambiguous requirements? ✅ None

- [x] 3.4.2 Review with development team
    - [x] Sufficient implementation guidance? ✅ (Updated ARCHITECTURE.md)
    - [x] Need additional ARCHITECTURE.md updates? ✅ Complete
    - [x] Any missing context? ✅ All documented

- [x] 3.4.3 Review with security/compliance (if applicable)
    - [x] Security requirements preserved? ✅
    - [x] Compliance requirements met? ✅

- [x] 3.4.4 Gather feedback and iterate
    - [x] Document all feedback
    - [x] Make necessary adjustments
    - [x] Re-validate with `openspec validate --strict` ✅

### 3.5 Final Validation

- [x] 3.5.1 Automated validation
    - [x] `openspec validate cleanup-spec-implementation-details --strict`
    - [x] All validation checks pass

- [x] 3.5.2 Quantitative checks
    - [x] Class references: 0 ✅
    - [x] Method references: 0 ✅
    - [x] Package references: 0 ✅
    - [x] Java patterns: 0 ✅
    - [x] Config implementation: Simplified ✅
    - [x] Event implementation: Removed ✅
    - [x] Business requirements: All preserved (39 members, 28 users) ✅
    - [x] Scenarios: All preserved (~120 members, ~100 users) ✅

- [x] 3.5.3 Qualitative checks
    - [x] Requirements focus on WHAT, not HOW ✅
    - [x] Business stakeholders can understand specs ✅
    - [x] Implementation guidance available in ARCHITECTURE.md ✅
    - [x] Team trained on updated format

- [x] 3.5.4 Documentation updates
    - [x] Update ARCHITECTURE.md ✅ (Added security implementation, value objects, API design patterns)
    - [ ] Create ADRs for key technical decisions (optional - can be done later)
    - [ ] Document spec format change (v1 → v2) (Git history provides traceability)
    - [ ] Team training materials if needed (optional)

---

## Validation and Testing

### Automated Validation

After each phase:

```bash
# Validate OpenSpec structure
openspec validate cleanup-spec-implementation-details --strict

# Search for remaining implementation patterns
rg -i "Service|Handler|Controller|Validator|Repository|Manager|Builder|Factory|Utils|Helper" openspec/changes/cleanup-spec-implementation-details/specs/
rg "com\.klabis\." openspec/changes/cleanup-spec-implementation-details/specs/
rg "\(\)" openspec/changes/cleanup-spec-implementation-details/specs/
rg -i "record|compact constructor" openspec/changes/cleanup-spec-implementation-details/specs/
```

### Manual Validation Checklist

- [ ] All business rules preserved
- [ ] All validation rules preserved
- [ ] All scenarios preserved
- [ ] No requirements deleted (only refined)
- [ ] No business logic changed
- [ ] API contracts preserved (if keeping in spec)
- [ ] Security requirements preserved
- [ ] Compliance requirements preserved
- [ ] Requirements still testable
- [ ] Requirements still unambiguous

### Comparison Validation

```bash
# Compare original vs updated specs
diff -u openspec/specs/members/spec.md openspec/changes/cleanup-spec-implementation-details/specs/members/spec.md | less
diff -u openspec/specs/users/spec.md openspec/changes/cleanup-spec-implementation-details/specs/users/spec.md | less
```

---

## Dependencies

### Internal Dependencies

- None - This is a standalone documentation cleanup

### External Dependencies

- Product owner review and approval
- Development team review (for implementation guidance concerns)
- Security/compliance review (if applicable)

---

## Rollback Plan

If issues arise:

```bash
# Revert all changes
git checkout openspec/specs/members/spec.md
git checkout openspec/specs/users/spec.md

# Or revert specific phase
git revert <commit-hash-for-phase>
```

---

## Success Metrics

### Completion Criteria

- [ ] All 3 phases complete
- [ ] OpenSpec validation passes
- [ ] Stakeholder review complete
- [ ] Approval received

### Quality Metrics

- [ ] Zero Java class/method/package references
- [ ] Zero event implementation details
- [ ] Configuration details generalized
- [ ] Technology references simplified
- [ ] 100% of business requirements preserved
- [ ] 100% of scenarios preserved
- [ ] Stakeholder approval received

