# Design Document: Member Update API - PATCH vs PUT

## Context

The member self-edit and admin edit API requires an HTTP method for updating member records. We need to choose between
PATCH (partial updates) and PUT (complete replacement).

### Requirements

- Members can edit their own information (self-edit)
- Admins can edit any member's information
- Typical usage pattern: 1-5 fields changed at a time
- Both web and mobile clients
- Dual authorization model (member self-edit vs admin edit)

---

## Decision

**Chosen Approach: PATCH /api/members/{id}**

We will use HTTP PATCH for member updates with partial update semantics.

---

## Options Considered

### Option A: PUT /api/members/{id} (Complete Replacement)

**How it works:**

- Client sends complete member resource
- Server replaces entire member with provided data
- Missing fields are cleared/set to defaults

**Pros:**

- ✅ Simple implementation (replace logic)
- ✅ Clear semantics (complete replacement)
- ✅ Easier validation (validate entire resource)
- ✅ No "null semantics" ambiguity
- ✅ Lower initial complexity

**Cons:**

- ❌ Race conditions: Last write wins, concurrent edits can lose data
- ❌ Large payloads: Send all 15-20 fields even if only 1 changes
- ❌ Poor UX: Unnatural for typical edit patterns (1-3 field changes)
- ❌ Bandwidth inefficient: Especially problematic for mobile clients
- ❌ Overkill for use case: Members rarely change all fields

**Initial Implementation Complexity:** ⭐ Low (~10 LOC)

---

### Option B: PATCH /api/members/{id} (Partial Update) ✅ **CHOSEN**

**How it works:**

- Client sends only changed fields
- Server merges partial data with existing member state
- Unspecified fields remain unchanged

**Pros:**

- ✅ Natural UX: Matches real-world editing pattern (1-3 field changes)
- ✅ Bandwidth efficient: Send only changed fields (~90% payload reduction)
- ✅ Race condition resistant: Concurrent edits to different fields don't overwrite each other
- ✅ Mobile-friendly: Less data transfer over slow connections
- ✅ Data integrity: Prevents accidental data loss from concurrent edits
- ✅ Flexible: Supports both single-field and multi-field updates
- ✅ Industry standard: Modern RESTful APIs use PATCH for partial updates

**Cons:**

- ⚠️ Higher implementation complexity (~40-50 LOC)
- ⚠️ Merge logic required
- ⚠️ Null semantics ambiguity (does null mean "clear" or "don't change"?)
- ⚠️ Partial validation complexity (validate merged state, not just request)
- ⚠️ More test scenarios (field combinations)

**Initial Implementation Complexity:** ⭐⭐⭐ Medium

---

## Decision Matrix

| Factor                        | Weight | PUT Score | PATCH Score | Winner |
|-------------------------------|--------|-----------|-------------|--------|
| **UX fit (partial edits)**    | ⭐⭐⭐⭐⭐  | 2/5       | 5/5         | PATCH  |
| **Race condition safety**     | ⭐⭐⭐⭐⭐  | 1/5       | 5/5         | PATCH  |
| **Bandwidth efficiency**      | ⭐⭐⭐⭐   | 2/5       | 5/5         | PATCH  |
| **Mobile-friendly**           | ⭐⭐⭐⭐   | 2/5       | 5/5         | PATCH  |
| **Implementation simplicity** | ⭐⭐⭐    | 5/5       | 2/5         | PUT    |
| **Data integrity**            | ⭐⭐⭐⭐⭐  | 2/5       | 5/5         | PATCH  |
| **Industry best practice**    | ⭐⭐⭐    | 4/5       | 5/5         | PATCH  |

**Overall Winner: PATCH** (5/7 factors, including 3 critical ones)

---

## Key Reasoning

### Primary Driver: User Experience (UX)

The decision is primarily driven by **real-world usage patterns**, not technical purity:

1. **Member self-edit scenario:**
    - Member changes phone number: 1 field
    - Member updates address: 4 fields (street, city, postalCode, country)
    - Member adds trainer license: 2 fields (number, validity)

   **Typical update: 1-3 fields**

2. **Admin edit scenario:**
    - Admin corrects firstName: 1 field (typo fix)
    - Admin updates name and date of birth: 3 fields
    - Admin adds medical course: 2 fields

   **Typical update: 1-5 fields**

**PUT approach** requires sending all 15-20 fields even when only 1-3 change.
**PATCH approach** sends only the changed fields.

### Secondary Driver: Data Integrity

**Race condition example:**

```
Timeline with concurrent edits:
10:00:00 - Admin opens member (GET) → sees firstName="Jan"
10:00:01 - Member opens own profile (GET) → sees phone="+420123456789"

10:00:05 - Admin submits: PATCH {"firstName": "Jan Novák"}
10:00:06 - Member submits: PATCH {"phone": "+420987654321"}

Result with PATCH:
✅ firstName = "Jan Novák" (admin's change)
✅ phone = "+420987654321" (member's change)
✅ Both preserved!

Same scenario with PUT:
10:00:05 - Admin submits: PUT {..., firstName="Jan Novák", ...}
10:00:06 - Member submits: PUT {..., firstName="Jan", ...}  (from their GET)

Result with PUT:
❌ firstName = "Jan" (admin's change LOST)
✅ phone = "+420987654321"
❌ Data loss!
```

---

## Acknowledged Costs

### Higher Initial Complexity

We acknowledge that PATCH requires more complex implementation:

**Estimated complexity:**

- PUT implementation: ~10 lines of code
- PATCH implementation: ~40-50 lines of code
- **Complexity multiplier: ~4-5x**

**Specific complexities:**

1. **Merge logic:** Must fetch existing state and merge partial updates
2. **Null handling:** Define clear semantics for null values in PATCH
3. **Partial validation:** Validate merged state, not just request
4. **More tests:** Test various field combinations

### Mitigation Strategies

1. **Use shared service method** to reduce code duplication if PUT is added later
2. **Clear null semantics:** Null in PATCH = ignore field; use empty string or explicit sentinel to clear
3. **Leverage Java 8+ Optional:** Cleaner null handling
4. **Comprehensive testing:** Unit tests for merge logic, integration tests for endpoints

---

## Implementation Approach

### Null Semantics

**Decision:** Null in PATCH request means "ignore this field"

```java
// Ignore this field (don't change it)
PATCH {"phone": null}

// To clear a field, use empty string or explicit marker
PATCH {"chipNumber": ""}
```

**Rationale:** Safer default - prevents accidental data clearing.

### Merge Strategy

```java
@PatchMapping("/members/{id}")
public ResponseEntity<Member> updateMember(
    @PathVariable UUID id,
    @RequestBody MemberPatchRequest patch
) {
    Member existing = repository.findById(id)
        .orElseThrow(() -> new NotFoundException());

    // Safe merge - only non-null fields
    Optional.ofNullable(patch.getPhone()).ifPresent(existing::setPhone);
    Optional.ofNullable(patch.getAddress()).ifPresent(existing::setAddress);
    Optional.ofNullable(patch.getChipNumber()).ifPresent(existing::setChipNumber);
    // ... etc for all 15+ fields

    // Validate merged state
    validator.validate(existing);

    Member saved = repository.save(existing);
    return ResponseEntity.ok(assembler.toModel(saved));
}
```

---

## Domain Model Design: Value Objects

### Decision: Use Value Objects for Complex Fields

Following Domain-Driven Design (DDD) principles, new member fields are implemented as value objects rather than
primitive fields scattered across the Member entity.

### Value Objects Defined

#### 1. IdentityCard

**Purpose:** Encapsulates ID card number and validity date

**Fields:**

- `cardNumber`: String (not blank, max 50 chars)
- `validityDate`: LocalDate (must not be in past)

**Validation rules:**

- Card number required (not blank)
- Validity date required
- Validity date must be in future or today

**Example:**

```java
public final class IdentityCard {
    private final String cardNumber;
    private final LocalDate validityDate;

    private IdentityCard(String cardNumber, LocalDate validityDate) {
        this.cardNumber = cardNumber;
        this.validityDate = validityDate;
    }

    public static IdentityCard of(String cardNumber, LocalDate validityDate) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new IllegalArgumentException("Card number is required");
        }
        if (validityDate == null) {
            throw new IllegalArgumentException("Validity date is required");
        }
        if (validityDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("ID card is expired");
        }
        return new IdentityCard(cardNumber, validityDate);
    }
}
```

**Database storage:** `identity_card_number`, `identity_card_validity` columns

---

#### 2. MedicalCourse

**Purpose:** Encapsulates medical course completion and optional validity

**Fields:**

- `completionDate`: LocalDate (required)
- `validityDate`: Optional<LocalDate> (optional)

**Validation rules:**

- Completion date required
- Validity date optional (some courses don't expire)
- If validity date provided, must be after completion date

**Example:**

```java
public final class MedicalCourse {
    private final LocalDate completionDate;
    private final Optional<LocalDate> validityDate;

    private MedicalCourse(LocalDate completionDate, Optional<LocalDate> validityDate) {
        this.completionDate = completionDate;
        this.validityDate = validityDate;
    }

    public static MedicalCourse of(LocalDate completionDate, Optional<LocalDate> validityDate) {
        if (completionDate == null) {
            throw new IllegalArgumentException("Completion date is required");
        }
        validityDate.ifPresent(validity -> {
            if (validity.isBefore(completionDate)) {
                throw new IllegalArgumentException("Validity must be after completion date");
            }
        });
        return new MedicalCourse(completionDate, validityDate);
    }
}
```

**Database storage:** `medical_course_completion`, `medical_course_validity` (nullable) columns

---

#### 3. TrainerLicense

**Purpose:** Encapsulates trainer license number and validity

**Fields:**

- `licenseNumber`: String (not blank, max 50 chars)
- `validityDate`: LocalDate (must not be in past)

**Validation rules:**

- License number required (not blank)
- Validity date required
- Validity date must be in future or today

**Example:**

```java
public final class TrainerLicense {
    private final String licenseNumber;
    private final LocalDate validityDate;

    private TrainerLicense(String licenseNumber, LocalDate validityDate) {
        this.licenseNumber = licenseNumber;
        this.validityDate = validityDate;
    }

    public static TrainerLicense of(String licenseNumber, LocalDate validityDate) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new IllegalArgumentException("License number is required");
        }
        if (validityDate == null) {
            throw new IllegalArgumentException("Validity date is required");
        }
        if (validityDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("License is expired");
        }
        return new TrainerLicense(licenseNumber, validityDate);
    }
}
```

**Database storage:** `trainer_license_number`, `trainer_license_validity` columns

---

#### 4. Dietary Restrictions

**Purpose:** Text field for member's dietary restrictions (used for food accommodation requests)

**Field:**

- `dietaryRestrictions`: String (optional, max 500 characters)

**Validation rules:**

- Optional field (can be null)
- Max 500 characters
- Any text allowed

**Example:**

```java
// Member entity
public class Member {
    private String dietaryRestrictions; // Optional, max 500 chars

    public void setDietaryRestrictions(String restrictions) {
        if (restrictions != null && restrictions.length() > 500) {
            throw new IllegalArgumentException("Dietary restrictions must be max 500 characters");
        }
        this.dietaryRestrictions = restrictions.isEmpty() ? null : restrictions;
    }
}
```

**Database storage:** `dietary_restrictions` column (varchar, nullable, max 500 chars)

---

#### 5. DrivingLicenseGroup (Enum)

**Purpose:** Represents driving license categories

**Values:** B, BE, C, C1, D, D1, T, AM, A1, A2, A

**Example:**

```java
public enum DrivingLicenseGroup {
    B, BE, C, C1, D, D1, T, AM, A1, A2, A
}
```

**Database storage:** `driving_license_group` column (enum or varchar)

---

### Value Object Benefits

1. **Encapsulation:** Related fields grouped together
2. **Validation:** Business rules enforced at construction
3. **Immutability:** Thread-safe, no unexpected mutations
4. **Self-documenting:** Clear domain concepts
5. **Testability:** Easy to test in isolation
6. **Type safety:** Compiler checks correct usage
7. **Reuse:** Can be used across different contexts

### Alternative Rejected: Primitive Fields

**Rejected approach:**

```java
// Member entity with primitive fields
public class Member {
    private String idCardNumber;        // ❌ Scattered
    private LocalDate idCardValidity;   // ❌ Scattered
    private LocalDate medicalCourseDate; // ❌ Scattered
    private LocalDate medicalCourseValidity; // ❌ Scattered
    // ... etc
}
```

**Why rejected:**

- Violates DDD principles
- No encapsulation of related concepts
- Validation scattered across entity
- Harder to test and maintain
- Less type-safe
- Primitive obsession anti-pattern

---

## Validation Timing: Set vs Read

### Critical Design Decision: Expiry Validation Only on Set

**Problem:** If we validate expiry dates on every update, members with expired documents can't update other fields.

**Wrong Approach (validate on every read):**

```java
@PatchMapping("/members/{id}")
public Member update(@PathVariable UUID id, @RequestBody MemberPatchRequest patch) {
    Member existing = repository.findById(id).orElseThrow();

    // Update phone
    if (patch.getPhone() != null) {
        existing.setPhone(patch.getPhone());
    }

    // ❌ WRONG: Validates ALL value objects, even unchanged ones
    if (existing.getIdentityCard() != null &&
        existing.getIdentityCard().getValidityDate().isBefore(LocalDate.now())) {
        throw new ValidationException("ID card expired");
        // Blocks phone update if ID card expired!
    }
}
```

**Correct Approach (validate only on set):**

```java
@PatchMapping("/members/{id}")
public Member update(@PathVariable UUID id, @RequestBody MemberPatchRequest patch) {
    Member existing = repository.findById(id).orElseThrow();

    // ✅ CORRECT: Validate only when SETTING the value object
    if (patch.getIdentityCard() != null) {
        // Validation happens HERE in IdentityCard.of()
        IdentityCard newCard = IdentityCard.of(
            patch.getIdentityCard().getCardNumber(),
            patch.getIdentityCard().getValidityDate()
        );
        existing.setIdentityCard(newCard);
    }

    // Can update other fields even if ID card expired
    if (patch.getPhone() != null) {
        existing.setPhone(patch.getPhone());
    }

    return repository.save(existing);
}
```

**Value object validates itself:**

```java
public static IdentityCard of(String cardNumber, LocalDate validityDate) {
    if (validityDate.isBefore(LocalDate.now())) {
        throw new IllegalArgumentException("ID card is expired");
        // ❌ Only blocks setting ID card
        // ✅ Doesn't block phone/email/other updates
    }
    return new IdentityCard(cardNumber, validityDate);
}
```

**Benefits:**

- Member can update phone number even if ID card expired
- Member can update address even if trainer license expired
- Expiry validation only applies to the document being set
- Natural user experience: update what you want, when you want

**Trade-off:**

- Members can have expired documents in database (acceptable)
- Can query for expired documents separately (background job)
- UI can show warnings without blocking updates

---

## Exit Strategy

If PATCH proves to be overkill, we have two options:

### Option 1: Add PUT Later

**Complexity:** Low ⭐⭐

PUT can be added alongside PATCH without breaking existing clients:

```java
@PutMapping("/members/{id}")
public ResponseEntity<Member> replaceMember(
    @PathVariable UUID id,
    @RequestBody MemberRequest request
) {
    // Simple replace logic
    Member member = request.toMember();
    member.setId(id);
    Member saved = repository.save(member);
    return ResponseEntity.ok(assembler.toModel(saved));
}
```

**Use cases for PUT:**

- Bulk import from external systems
- Complete member data refresh
- Admin "replace all" operations

### Option 2: Switch to PUT-Only

**Complexity:** Medium ⭐⭐⭐

If PATCH doesn't provide value, we can deprecate it:

1. Keep PATCH endpoint, mark as `@Deprecated`
2. Add new PUT endpoint
3. Document migration path for clients
4. Remove PATCH in future major version

**Triggers for reconsideration:**

- PATCH merge logic proves too buggy
- Clients always send full data anyway
- Team struggles with complexity
- Performance impact is negligible

---

## Success Criteria

We will validate the PATCH decision based on:

1. **Usage metrics:** Average fields updated per request < 5
2. **Bug reports:** Race condition data loss issues < 1 per month
3. **Client feedback:** Mobile app performance acceptable
4. **Team velocity:** Complexity doesn't slow development significantly

**Reconsider if:**

- Average fields updated > 10 (indicates PUT might be better fit)
- Race condition bugs > 3 per month
- Team spends significant time maintaining merge logic
- Client complaints about complexity

---

## References

- HTTP PATCH specification: RFC 5789
- REST API design patterns: Richardson Maturity Model
- Spring Framework: `@PatchMapping` support (Spring 4.3+)
- Industry examples: JSON:API, HAL, GraphQL mutations

---

## Appendix: Migration Complexity (PUT ↔ PATCH)

| Direction   | Complexity         | Why                                            |
|-------------|--------------------|------------------------------------------------|
| PUT → PATCH | ⭐⭐⭐⭐ Moderate-High | Need to add merge logic, decide null semantics |
| PATCH → PUT | ⭐⭐ Low-Moderate    | Simple replace logic, clear semantics          |

**Conclusion:** Starting with PATCH makes future PUT addition easier than the reverse.

---

**Decision Date:** 2025-01-17
**Decision Maker:** Architecture team
**Review Date:** 2025-07-17 (6 months)
**Status:** Approved ✅
