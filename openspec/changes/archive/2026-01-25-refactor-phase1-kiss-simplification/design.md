# Design: Phase 1 KISS Refactoring

## Context

The klabis-backend codebase implements a modular monolith architecture using Spring Modulith with Domain-Driven Design (
DDD) principles. The current implementation uses several patterns that, while theoretically sound, add unnecessary
complexity for the current scale and requirements:

1. **CQRS (Command Query Responsibility Segregation)**: Separate handlers for commands and queries
2. **Audit Logging with SPeL**: Spring Expression Language for dynamic audit descriptions
3. **Command/Query DTOs**: Separate data transfer objects for each layer
4. **Validation Utilities**: Centralized static validation methods

### Current State

```
Request → Command → CommandHandler → Domain → DTO → Response
```

### Target State

```
Request → Service → Domain → Response
```

## Goals / Non-Goals

### Goals

- Simplify architecture by removing unnecessary abstraction layers
- Reduce codebase size by ~1,220 lines while maintaining 100% functionality
- Improve code navigation and reduce cognitive load for developers
- Maintain all transaction boundaries and business logic
- Preserve test coverage (no regressions)

### Non-Goals

- ~~Changing API contracts~~ (No changes to REST endpoints)
- ~~Modifying database schema~~ (No data model changes)
- ~~Altering business logic~~ (All logic preserved)
- ~~Removing validation~~ (Validation inlined, not removed)
- ~~Simplifying domain model~~ (Value objects, aggregates remain)

## Decisions

### Decision 1: Eliminate CQRS Handlers

**What**: Consolidate all Command and Query handlers into single `MemberService` and `UserService` classes.

**Why**:

- CQRS provides benefits only with complex read models, separate databases, or different scaling needs
- Current codebase has none of these justifications
- Handler classes average 150 lines but perform simple CRUD orchestration
- No complex read projections or denormalization

**Alternatives Considered**:

1. **Keep CQRS** - Rejected: Adds complexity without benefit
2. **Partial CQRS** - Rejected: Inconsistent architecture
3. **Service classes** - **Selected**: Simpler, clearer intent

**Trade-offs**:

- (+) Fewer files to navigate
- (+) Clearer method names (`registerMember()` vs `handle(command)`)
- (+) Easier to find related operations
- (-) Less theoretical purity (acceptable for pragmatic development)

### Decision 2: Remove Audit Logging Entirely

**What**: Delete the entire audit infrastructure including `@Auditable` annotation, `AuditLogAspect`, and
`AuditEventType`.

**Why**:

- Only 6 production usages for ~350 lines of infrastructure
- SPeL expression parsing adds complexity without clear benefit
- Audit logs written to application logs (not centralized audit store)
- No regulatory requirement for audit trail in current scope
- Can be re-added later if compliance requirements emerge

**Alternatives Considered**:

1. **Simplify to static descriptions** - Rejected: Still 100+ lines for 6 usages
2. **Use MDC (Mapped Diagnostic Context)** - Rejected: Still adds complexity
3. **Complete removal** - **Selected**: Maximizes simplification

**Trade-offs**:

- (+) Removes ~350 lines of complex code
- (+) Eliminates SPeL dependency and parsing errors
- (+) Simpler debugging (no aspect weaving)
- (-) No audit trail (acceptable for internal club management app)
- (-) Harder to track who did what (can re-add if needed)

### Decision 3: Inline Validation Utilities

**What**: Move validation logic from `StringValidator`, `DateValidator`, and `PasswordComplexityValidator` directly into
value objects and services.

**Why**:

- Validation utilities are thin wrappers around if-throw logic
- Indirection hides validation rules from value objects where they belong
- No reusability benefit (each value object has unique validation)
- Easier to understand validation when co-located with data

**Alternatives Considered**:

1. **Keep validation utilities** - Rejected: Adds unnecessary indirection
2. **Use Bean Validation (@Valid)** - Rejected: Doesn't cover domain validation rules
3. **Inline in value objects** - **Selected**: Clearer, simpler

**Trade-offs**:

- (+) Validation logic visible where data is defined
- (+) Easier to debug (no static method calls)
- (+) Fewer files to navigate
- (-) Slightly more code per value object (acceptable for clarity)

### Decision 4: Use Request Objects Directly

**What**: Service methods accept presentation layer Request objects instead of converting to Command objects.

**Why**:

- Request objects already have all necessary validation annotations
- No transformation logic between Request and Command (identical fields)
- Reduces mapping layers from 4 to 2 (Request ↔ Domain ↔ Response)
- Eliminates 6 Command/Query DTOs

**Alternatives Considered**:

1. **Keep Command/Query DTOs** - Rejected: Unnecessary transformation
2. **Use domain objects in controllers** - Rejected: Violates clean architecture
3. **Use Request objects directly** - **Selected**: Pragmatic balance

**Trade-offs**:

- (+) Fewer DTO classes to maintain
- (+) No mapping boilerplate
- (+) Clear data flow
- (-) Services depend on presentation layer (acceptable for this scale)

## Architecture Impact

### Before

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Request    │→ │   Controller │→ │   Response   │      │
│  └──────────────┘  └──────┬───────┘  └──────────────┘      │
└────────────────────────────┼────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Command    │→ │    Handler   │→ │     DTO      │      │
│  └──────────────┘  └──────┬───────┘  └──────────────┘      │
└────────────────────────────┼────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                          │
│              ┌──────────────┐  ┌──────────────┐            │
│              │   Entity     │← │  Repository  │            │
│              └──────────────┘  └──────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### After

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Request    │→ │   Controller │→ │   Response   │      │
│  └──────────────┘  └──────┬───────┘  └──────────────┘      │
└────────────────────────────┼────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│                   ┌──────────────┐  ┌──────────────┐        │
│                   │    Service   │→ │   Response   │        │
│                   └──────┬───────┘  └──────────────┘        │
└──────────────────────────┼──────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                          │
│              ┌──────────────┐  ┌──────────────┐            │
│              │   Entity     │← │  Repository  │            │
│              └──────────────┘  └──────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

## Code Examples

### Before: CQRS Handler

```java
// Command DTO
public record RegisterMemberCommand(
    String firstName, String lastName, LocalDate dateOfBirth,
    String email, String phone, AddressRequest address,
    GuardianDTO guardian
) {}

// Handler
@Service
public class RegisterMemberCommandHandler {
    @Auditable(event = MEMBER_CREATED, description = "New member registered")
    @Transactional
    public UUID handle(RegisterMemberCommand command) {
        // ... orchestrate member creation ...
        return memberRepository.save(member).getId().uuid();
    }
}

// Controller
@PostMapping
public ResponseEntity<?> registerMember(@Valid @RequestBody RegisterMemberRequest request) {
    RegisterMemberCommand command = toCommand(request);
    UUID memberId = registerMemberCommandHandler.handle(command);
    // ... build response ...
}
```

### After: Service

```java
// Service
@Service
public class MemberService {
    @Transactional  // No @Auditable
    public UUID registerMember(RegisterMemberRequest request) {
        // ... orchestrate member creation (same logic) ...
        return memberRepository.save(member).getId().uuid();
    }
}

// Controller
@PostMapping
public ResponseEntity<?> registerMember(@Valid @RequestBody RegisterMemberRequest request) {
    UUID memberId = memberService.registerMember(request);  // Direct call
    // ... build response ...
}
```

### Before: Validation Utility

```java
// Utility class
public final class StringValidator {
    public static String requireNonBlankAndMatches(
        String value, String fieldName, String pattern, String errorMessage) {
        String trimmed = requireNonBlank(value, fieldName);
        requireMatches(trimmed, pattern, errorMessage);
        return trimmed;
    }
}

// Value object
public record EmailAddress(String email) {
    public EmailAddress {
        this.email = StringValidator.requireNonBlankAndMatches(
            email, "Email", EMAIL_PATTERN, "Invalid email format"
        );
    }
}
```

### After: Inline Validation

```java
// Value object (no utility)
public record EmailAddress(String value) {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String trimmed = value.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        value = trimmed;  // Canonical form
    }
}
```

## Migration Plan

### Step-by-Step Process

1. **Create Services First** (No deletions yet)
    - Create `MemberService` and `UserService`
    - Copy all methods from handlers
    - Verify services compile

2. **Migrate Tests** (Safety net maintained)
    - Create `MemberServiceTest` and `UserServiceTest`
    - Copy and adapt tests from handlers
    - Verify all tests pass

3. **Update Controllers** (Layer above services)
    - Replace handler injections with service injections
    - Remove conversion methods
    - Verify controller tests pass

4. **Delete Old Files** (After verification)
    - Delete Command/Query DTOs
    - Delete Handler classes
    - Delete old Handler tests
    - Verify no compilation errors

5. **Inline Validation** (Move before delete)
    - Move validation into value objects
    - Update imports
    - Delete validation utilities
    - Verify all tests pass

6. **Remove Audit Logging** (Delete infrastructure)
    - Delete audit infrastructure files
    - Remove `@Auditable` annotations
    - Remove audit imports
    - Delete audit test
    - Verify all tests pass

### Rollback Strategy

If critical issues occur at any step:

```bash
# Revert all changes
git checkout main
git branch -D refactor/phase1-kiss-simplification

# Or revert specific steps
git revert <commit-hash>
```

### Testing Strategy

**Before starting:**

```bash
mvn test > baseline-test-results.txt 2>&1
# Save test count: Tests run: 200, Failures: 0
```

**After each step:**

```bash
mvn test -Dtest="*Member*Test"  # Quick module test
```

**After completion:**

```bash
mvn test  # Full suite
mvn verify  # Integration tests
diff baseline-test-results.txt current-test-results.txt
```

## Risks / Trade-offs

### Risk 1: Breaking Transaction Boundaries

**Risk**: Losing `@Transactional` annotations when migrating code

**Mitigation**:

- Double-check all service methods have `@Transactional`
- Use `@Transactional(readOnly = true)` for query methods
- Test transaction rollback behavior

### Risk 2: Losing Business Logic

**Risk**: Accidentally omitting logic when copying methods

**Mitigation**:

- Comprehensive test coverage (existing tests serve as safety net)
- Line-by-line comparison when copying methods
- Test after each service creation

### Risk 3: Compilation Errors

**Risk**: Other code still references deleted handlers/DTOs

**Mitigation**:

- Search for all references before deletion
- Fix compilation errors before proceeding
- Incremental deletion with verification

### Risk 4: Test Failures

**Risk**: Tests fail after refactoring

**Mitigation**:

- Create new tests before deleting old ones
- Run tests after each file modification
- Fix failures immediately before proceeding

### Risk 5: No Audit Trail

**Risk**: Cannot track who did what after audit removal

**Mitigation**:

- Accept for current scope (internal club management)
- Can re-add audit logging if compliance requirements emerge
- Application logs still contain transaction information

## Open Questions

### Q1: Should we keep ANY audit capability?

**Answer**: No. Current implementation (~350 lines for 6 usages) does not justify complexity. Can re-add later if
needed.

### Q2: What about regulatory compliance?

**Answer**: No current GDPR or compliance requirements for audit trail. System is internal club management, not
financial/healthcare.

### Q3: Will service classes become too large?

**Answer**: No. `MemberService` will be ~300 lines (4 methods), `UserService` ~100 lines (2 methods). Well within
acceptable limits.

### Q4: What if we need CQRS later?

**Answer**: Re-introduce when actual need arises (complex read models, separate databases). YAGNI principle - don't
implement for hypothetical future requirements.

## Success Criteria

### Quantitative Metrics

| Metric                | Before  | After   | Target         |
|-----------------------|---------|---------|----------------|
| Production Java files | ~130    | ~109    | -21 files      |
| Production code lines | ~13,123 | ~11,900 | -1,220 lines   |
| Handler classes       | 6       | 0       | -6 files       |
| Command/Query DTOs    | 6       | 0       | -6 files       |
| Validation utilities  | 2       | 0       | -2 files       |
| Audit infrastructure  | 3       | 0       | -3 files       |
| Test pass rate        | 100%    | 100%    | No regressions |

### Qualitative Outcomes

- ✅ All business logic preserved
- ✅ API contracts unchanged
- ✅ Transaction boundaries maintained
- ✅ Validation logic intact (inlined, not removed)
- ✅ Code easier to navigate (fewer files)
- ✅ Mental model simpler (services vs CQRS)
- ✅ Faster onboarding for new developers
- ✅ Fewer abstractions to understand
