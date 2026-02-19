# Technical Design: Birth Number and Bank Account Management

## Context

Current state:
- Member aggregate exists with personal information, contact details, and optional fields (chip number)
- Database schema uses V001__initial_schema.sql (single squashed migration)
- Jasypt is already configured for password encryption in User aggregate
- No encryption for member PII data (currently)
- RegisterMemberRequest and UpdateMemberRequest DTOs handle member data
- Member registration process: create User first, then Member with same UserId

GitHub issue #3 requires two additional member attributes:
1. **Birth number** (rodné číslo) - Czech national identifier, GDPR-sensitive
2. **Bank account number** - for expense reimbursement, not sensitive

Constraints:
- Birth number only for Czech nationals (conditional validation)
- Birth number must be encrypted at rest (GDPR compliance)
- Bank account should validate IBAN format
- Must maintain backwards compatibility with existing members (nullable fields)

## Goals / Non-Goals

**Goals:**
- Add BirthNumber value object with format validation (RRMMDD/XXXX)
- Encrypt birth number column using Jasypt (consistent with existing password encryption)
- Add bank account number with IBAN validation
- Extend RegisterMemberRequest and UpdateMemberRequest
- Maintain conditional validation (birth number only for CZ nationality)
- Support null values for both fields (backwards compatibility)
- Audit trail for birth number access (GDPR requirement)

**Non-Goals:**
- Checksum validation for birth number (format only, as requested by user)
- Automatic derivation of date of birth from birth number (keep manual entry)
- Gender derivation from birth number (warn only if inconsistent)
- Migration of existing members (new fields default to null)
- UI implementation (backend API only)
- Integration with external validation services

## Decisions

### Decision 1: Use Jasypt for Birth Number Encryption

**Choice:** Encrypt birth_number column using Jasypt with AES-256

**Why:**
- Jasypt already configured in project for password encryption (consistency)
- Spring Boot Auto-configuration available
- Supports field-level encryption via custom AttributeConverter
- GDPR compliance: birth number is personally identifiable information

**Alternatives considered:**
- Database-level encryption (pgcrypto) - rejected: adds PostgreSQL dependency, H2 dev env complexity
- Spring Crypto - rejected: requires more custom code, Jasypt is proven

**Implementation:**
```java
@Converter
public class StringEncryptionConverter implements AttributeConverter<String, String> {
    private final StringEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptor.decrypt(dbData);
    }
}
```

**Rationale:** Generic name allows reuse for other encrypted String fields (e.g., sensitive notes, SSN in future).

**Configuration:**
```yaml
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD}  # Environment variable
    algorithm: PBEWITHHMACSHA512ANDAES_256
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
```

### Decision 2: BirthNumber as Value Object

**Choice:** Create BirthNumber value object (Java record) with embedded validation

**Why:**
- Encapsulates validation logic (format, date validity)
- Type safety (can't assign arbitrary string to birth number)
- Reusable across Member aggregate and DTOs
- Consistent with existing value objects (EmailAddress, PhoneNumber, Address)

**Structure:**
```java
public record BirthNumber(String value) {
    private static final Pattern FORMAT_WITH_SLASH = Pattern.compile("^\\d{6}/\\d{4}$");
    private static final Pattern FORMAT_WITHOUT_SLASH = Pattern.compile("^\\d{10}$");

    public BirthNumber {
        Objects.requireNonNull(value);
        String normalized = normalize(value);
        validateFormat(normalized);
        validateDate(normalized);
        value = normalized;  // Store normalized format
    }

    public static BirthNumber of(String value) { ... }
    private static String normalize(String value) { ... }  // Add slash if missing
    private static void validateFormat(String value) { ... }
    private static void validateDate(String value) { ... }
}
```

**Validation strategy:**
- Format validation: regex for RRMMDD/XXXX or RRMMDDXXXX
- Date validation: extract RR, MM, DD and validate ranges (month 01-12/51-62/21-32/71-82, day 01-31)
- NO checksum validation (as per user requirement)

### Decision 3: BankAccountNumber as Value Object

**Choice:** Create BankAccountNumber value object (Java record) with support for both IBAN and domestic format

**Why:**
- **Consistency with domain model** - all financial/identity data should be value objects (BirthNumber, EmailAddress, PhoneNumber)
- **Type safety** - prevents assigning arbitrary strings to bank account fields
- **Encapsulation** - validation and normalization logic in one place
- **Reusability** - can be used across Member aggregate and DTOs
- **Domain language** - BankAccountNumber is more expressive than String
- **Flexibility** - supports both IBAN (international) and domestic format (Czech bank account)

**Structure:**
```java
public record BankAccountNumber(String value, AccountFormat format) {

    public enum AccountFormat {
        IBAN,       // e.g., CZ6508000000192000145399
        DOMESTIC    // e.g., 123456/0800
    }

    private static final Pattern DOMESTIC_FORMAT = Pattern.compile("^\\d{1,10}/\\d{4}$");

    public BankAccountNumber {
        Objects.requireNonNull(value, "Bank account number is required");
        String normalized = normalize(value);
        AccountFormat detectedFormat = detectFormat(normalized);
        validateFormat(normalized, detectedFormat);
        value = normalized;
        format = detectedFormat;
    }

    public static BankAccountNumber of(String value) {
        return new BankAccountNumber(value, null);  // format auto-detected
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", "");
    }

    private static AccountFormat detectFormat(String normalized) {
        if (DOMESTIC_FORMAT.matcher(normalized).matches()) {
            return AccountFormat.DOMESTIC;
        }
        // Assume IBAN if starts with 2 letters
        if (normalized.length() >= 2 && Character.isLetter(normalized.charAt(0))) {
            return AccountFormat.IBAN;
        }
        throw new IllegalArgumentException("Cannot detect account format: " + normalized);
    }

    private static void validateFormat(String value, AccountFormat format) {
        if (format == AccountFormat.IBAN) {
            IBANValidator validator = IBANValidator.getInstance();
            if (!validator.isValid(value.toUpperCase())) {
                throw new IllegalArgumentException("Invalid IBAN: " + value);
            }
        } else if (format == AccountFormat.DOMESTIC) {
            // Domestic format already validated by regex in detectFormat()
            // Additional validation: bank code must be 4 digits
            String[] parts = value.split("/");
            if (parts.length != 2 || parts[1].length() != 4) {
                throw new IllegalArgumentException("Invalid domestic format: " + value);
            }
        }
    }
}
```

**Validation strategy:**
- Normalize: remove spaces
- Detect format: domestic (matches `\d{1,10}/\d{4}`) vs IBAN (starts with 2 letters)
- IBAN: validate using Apache Commons Validator (checksum validation)
- Domestic: validate regex pattern (account number/bank code)
- Store both value and detected format for future use

**Alternatives considered:**
- IBAN-only validation - rejected: user requested domestic format support
- Store as String - rejected: inconsistent with other value objects, no type safety

### Decision 4: Conditional Validation in Member Aggregate

**Choice:** Enforce nationality-based validation in Member.createWithId() and Member.handle(UpdateMemberDetails)

**Why:**
- Business rule: "birth number only for Czech nationals"
- Aggregate root enforces invariants (DDD principle)
- Fail-fast validation prevents invalid state

**Implementation:**
```java
public static Member createWithId(..., BirthNumber birthNumber, BankAccountNumber bankAccountNumber, ...) {
    validateBirthNumberNationality(personalInformation.getNationality(), birthNumber);
    // ...
}

private static void validateBirthNumberNationality(Nationality nationality, BirthNumber birthNumber) {
    boolean isCzech = "CZ".equals(nationality.code()) || "CZE".equals(nationality.code());
    if (!isCzech && birthNumber != null) {
        throw new BusinessRuleViolationException(
            "Birth number is only allowed for Czech nationals"
        );
    }
}
```

**Note:** BankAccountNumber validation happens in value object constructor, no aggregate-level validation needed.

### Decision 5: Database Schema Changes in V001

**Choice:** Add columns directly to V001__initial_schema.sql (not new migration)

**Why:**
- Project uses in-memory H2 database (dev profile)
- Database resets on server restart
- Single squashed migration easier to maintain
- No production deployments yet (as documented in backend/CLAUDE.md)

**Schema changes:**
```sql
ALTER TABLE members ADD COLUMN birth_number VARCHAR(255);  -- Encrypted, longer than plain text
ALTER TABLE members ADD COLUMN bank_account_number VARCHAR(50);

COMMENT ON COLUMN members.birth_number
    IS 'Czech birth number (rodné číslo), encrypted with Jasypt, format RRMMDD/XXXX after decryption';
COMMENT ON COLUMN members.bank_account_number
    IS 'Bank account number in IBAN format for expense reimbursement (optional)';
```

### Decision 6: Audit Trail via Spring Data Auditing

**Choice:** Leverage existing Spring Data JDBC auditing (created_by, modified_by, version)

**Why:**
- Already implemented for Member aggregate
- Tracks WHO modified birth number (via modified_by column)
- Version column for optimistic locking
- No separate audit table needed (sufficient for compliance)

**Enhancement:** Add log statement in MemberMemento.toMember() when birth number is accessed:
```java
if (memento.birthNumber() != null) {
    log.info("Birth number accessed for member: {}, by: {}",
        memento.id(), SecurityContextHolder.getContext().getAuthentication().getName());
}
```

## Risks / Trade-offs

### Risk: Jasypt Performance Impact

**Risk:** Encryption/decryption on every database read/write could slow down API responses

**Mitigation:**
- AES-256 is fast (microseconds per operation)
- Birth number rarely accessed in bulk operations
- Monitor response times (target <500ms maintained)
- If needed: cache decrypted value in Member aggregate instance (not across requests)

### Risk: Lost Encryption Key

**Risk:** If JASYPT_ENCRYPTOR_PASSWORD is lost, birth numbers cannot be decrypted

**Mitigation:**
- Document key management in ops runbook
- Store key in secure vault (not git, not plaintext)
- Regular backup of encryption key
- Consider key rotation strategy for production

### Risk: IBAN Validation Library Dependency

**Risk:** Apache Commons Validator adds dependency

**Mitigation:**
- Well-maintained library (Apache Commons)
- Small footprint (~100KB)
- Avoid reinventing IBAN checksum algorithm (complex: mod-97 calculation)
- Value object pattern justifies dependency (proper encapsulation)
- Alternative rejected: implement basic IBAN regex validation (less robust, no checksum)

### Trade-off: No Birth Number Checksum Validation

**Trade-off:** Format-only validation allows potentially invalid birth numbers

**Justification:**
- User explicitly requested format-only validation
- Checksum algorithm complex (varies by year of birth)
- False negatives possible (old vs new format)
- Primary use case: data entry, not identity verification
- Warning shown if date/gender inconsistent

### Trade-off: Single Migration File

**Trade-off:** Modifying V001 instead of creating V002 breaks traditional Flyway pattern

**Justification:**
- H2 in-memory database (no migration history preserved)
- No production deployments yet
- Documented approach in backend/CLAUDE.md
- Simpler for development (one source of truth)

## Migration Plan

**Development Environment:**
1. Stop running backend server (if any)
2. Update V001__initial_schema.sql with new columns
3. Configure Jasypt (JASYPT_ENCRYPTOR_PASSWORD in env)
4. Start server → H2 recreates schema with new columns
5. Bootstrap data includes null birth_number/bank_account_number

**Testing Strategy:**
1. Unit tests for BirthNumber value object (format validation)
2. Unit tests for Member aggregate (conditional validation)
3. Integration tests for MemberJdbcRepository (encryption/decryption)
4. E2E tests for RegisterMember API (new fields in request/response)

**Rollback Strategy:**
- Not applicable (in-memory database resets)
- For future production: create down migration to drop columns

## Open Questions

**Q1:** Should we warn users if birth number date/gender doesn't match personal information?

**Decision:** YES - show warning but allow creation (data entry errors common)

**Q2:** Should bank account accept domestic format (123456/0800) or only IBAN?

**Decision:** ACCEPT BOTH - BankAccountNumber value object auto-detects format and validates accordingly (no warning needed)

**Q3:** Should birth number be visible in member list API (GET /api/members)?

**Decision:** NO - only in detail view (GET /api/members/{id}) to minimize exposure

**Q4:** Should we add birth number to search/filter capabilities?

**Decision:** OUT OF SCOPE - defer to separate issue if needed

## Implementation Sequence

1. **Value Objects**
   - BirthNumber record with format validation
   - BankAccountNumber record with IBAN validation

2. **Domain Layer**
   - Add birthNumber field (type: BirthNumber) to Member aggregate
   - Add bankAccountNumber field (type: BankAccountNumber) to Member aggregate
   - Update Member.createWithId() and Member.handle(UpdateMemberDetails)
   - Conditional validation logic for birth number nationality

3. **Persistence Layer**
   - Update V001__initial_schema.sql (add 2 columns)
   - Configure Jasypt converter for BirthNumber
   - Update MemberMemento with both value objects
   - Update MemberJdbcRepository tests

4. **Application Layer**
   - Update RegisterMemberRequest (String fields for JSON deserialization)
   - Update UpdateMemberRequest (String fields)
   - Update MemberDetailsResponse (String fields for JSON serialization)
   - Update RegistrationService (convert String → value objects)
   - Update ManagementService (convert String → value objects)

5. **Tests (TDD)**
   - BirthNumberTest (value object validation)
   - BankAccountNumberTest (IBAN validation)
   - MemberTest (aggregate validation, nationality check)
   - MemberRepositoryTest (encryption/decryption)
   - MemberRegistrationE2ETest (API E2E)

6. **Documentation**
   - Update OpenAPI schema
   - Update backend/CLAUDE.md (if needed)
