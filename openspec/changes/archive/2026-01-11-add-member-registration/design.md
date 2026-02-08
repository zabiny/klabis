# Technical Design: Member Registration

## Context

Member registration is the foundational capability for the Klabis orienteering club management system. It must handle
personal data (GDPR-sensitive) and follow HATEOAS principles for API discoverability.

Target users include club administrators with varying technical skills (age 10-70), requiring responsive web interfaces
and clear error messages.

## Goals / Non-Goals

### Goals

- Create secure, GDPR-compliant member data storage
- Implement HATEOAS-first API with HAL+FORMS
- Support conditional validation (nationality-based)
- Send welcome email with account setup link via OAuth2
- Follow DDD with bounded context for Members

### Non-Goals

- Self-service member registration (admin-only in this phase)
- Member profile editing (separate change)
- Bulk member import (future consideration)
- Payment processing (handled in Finances context)
- Training group assignment (separate change, added later with user groups)
- ORIS/CUS integration (separate change, added later)
- Native mobile app (responsive web is sufficient)

## Decisions

### Decision 1: Member Aggregate Design (DDD)

**What**: Member is the aggregate root with embedded value objects.

**Structure**:

```
Member (Aggregate Root)
├── MemberId (identity)
├── RegistrationNumber (VO - unique identifier)
├── PersonalInformation (VO)
│   ├── FirstName
│   ├── LastName
│   ├── DateOfBirth
│   ├── Nationality
│   ├── Gender
│   └── RodneCislo (optional, Czech only)
├── Address (VO)
├── ContactInformation (VO)
│   ├── Email (collection)
│   └── Phone (collection)
├── GuardianInformation (VO, optional for minors)
├── ChipNumber (optional)
└── BankAccount (optional)
```

**Invariants enforced by aggregate**:

- Registration number uniqueness
- At least one email and one phone (member OR guardian)
- RodneCislo only when nationality is Czech
- Guardian required for minors (<18 years)

**Why**: Encapsulates business rules, ensures data consistency, clear transactional boundary.

**Alternatives considered**:

- Anemic domain model - rejected (violates DDD principles, scatters business logic)
- Separate Guardian entity - rejected (guardian is part of member lifecycle, no independent identity)

### Decision 2: Registration Number Generation Strategy

**What**: Use database sequence with formatted output: `{clubCode}{birthYear}{sequence:02d}`

**Configuration**: Club code stored in `application.yml`:

```yaml
klabis:
  club:
    code: ZBM  # 3-character alphanumeric code (e.g., ZBM, AB1)
```

**Implementation**:

```java
// Pseudocode
class RegistrationNumberGenerator {
    @Value("${klabis.club.code}")
    private String clubCode;

    String generate(LocalDate dateOfBirth) {
        int birthYear = dateOfBirth.getYear() % 100; // YY (last 2 digits)
        int sequence = repository.getNextSequenceForBirthYear(birthYear);
        return String.format("%s%02d%02d", clubCode, birthYear, sequence);
        // Example: ZBM0101 (club ZBM, born 2001, 1st member)
        //          ZBM0501 (club ZBM, born 2005, 1st member)
    }
}
```

**Sequence logic**:

- Each birth year has independent sequence counter
- Member born in 2001: ZBM0101, ZBM0102, ZBM0103...
- Member born in 2005: ZBM0501, ZBM0502, ZBM0503...
- Database query: `SELECT MAX(sequence) FROM members WHERE registration_number LIKE 'ZBM01%'`

**Why**:

- Database sequence ensures uniqueness per birth year
- Birth year in registration number helps identify member's age category
- Alphanumeric club code matches Czech orienteering conventions
- Simple format for human readability

**Alternatives considered**:

- UUID - rejected (not human-readable, doesn't match required format)
- Application-level counter - rejected (race conditions in concurrent scenarios)
- Current year instead of birth year - rejected (doesn't match business requirements)

### Decision 3: HATEOAS Implementation with HAL+FORMS

**What**: Use Spring HATEOAS with custom HAL+FORMS media type.

**Response structure**:

```json
{
  "memberId": "123e4567-e89b-12d3-a456-426614174000",
  "registrationNumber": "ZBM0501",
  "firstName": "Jan",
  "lastName": "Novák",
  "dateOfBirth": "2005-03-15",
  "isActive": true,
  "_links": {
    "self": { "href": "/api/members/123e4567-e89b-12d3-a456-426614174000" },
    "edit": { "href": "/api/members/123e4567-e89b-12d3-a456-426614174000" },
    "collection": { "href": "/api/members" }
  },
  "_templates": {
    "update": {
      "method": "PUT",
      "contentType": "application/json",
      "properties": [
        { "name": "firstName", "required": true, "type": "text" },
        { "name": "nationality", "required": true, "type": "text" }
      ]
    }
  }
}
```

**Note**: `deactivate` and `activate` links will be added in a separate change when member lifecycle management is
implemented.

**Why**:

- Self-documenting API for frontend developers
- Client navigates via links, not hardcoded URLs
- Forms provide metadata for UI generation
- Aligns with project mandate for HATEOAS

**Alternatives considered**:

- Plain JSON REST - rejected (violates HATEOAS requirement)
- GraphQL - rejected (overkill, adds complexity, different paradigm)
- JSON:API - rejected (HAL+FORMS better for form-heavy workflows)

### Decision 4: Post-Registration Email Flow

**What**: Send welcome email with OAuth2 account setup link via Spring Email.

**Flow**:

1. Member created → MemberCreatedEvent published
2. WelcomeEmailHandler sends email asynchronously:
    - Uses Spring JavaMailSender (SMTP)
    - Email contains link to OAuth2 account activation
    - Link points to Spring Authorization Server on same host
3. Failures logged for retry

**Email service configuration** (`application.yml`):

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

**Why**:

- Member creation succeeds even if email fails
- Spring Email is simple and well-integrated
- SMTP is sufficient for MVP
- OAuth2 provides secure account activation

**Alternatives considered**:

- Synchronous email - rejected (timeout risks, poor UX)
- External email service (SendGrid) - deferred (SMTP sufficient for MVP)
- Message queue (RabbitMQ) - deferred (over-engineering for MVP)

### Decision 5: Nationality-Based Conditional Logic

**What**: Frontend and backend both enforce RodneCislo availability.

**Backend validation**:

```java
if (nationality != Nationality.CZECH && rodneCislo != null) {
    throw new ValidationException("RodneCislo only allowed for Czech nationality");
}
```

**Frontend behavior**:

- Field disabled when nationality != CZ
- Value cleared on nationality change

**Why**:

- Double validation (defense in depth)
- Clear UX feedback
- GDPR compliance (minimize sensitive data collection)

**Alternatives considered**:

- Backend-only validation - rejected (poor UX, confusing for users)
- Always allow field - rejected (violates privacy requirements)

### Decision 6: Authorization Model

**What**: Role-Based Access Control (RBAC) with permission checks.

**Permissions** (for this change):

- `MEMBERS:CREATE` - Create members
- `MEMBERS:READ` - View members
- `MEMBERS:UPDATE` - Edit members (deferred to separate change)

**Future permissions** (deferred):

- `MEMBERS:DEACTIVATE` - Deactivate members (soft delete)
- `MEMBERS:ACTIVATE` - Reactivate deactivated members

**Implementation**: Spring Security with `@PreAuthorize` annotations.

**Why**:

- Granular control
- Standard Spring Security pattern
- Database schema supports soft delete via `is_active` field for future use
- Easy to extend with more permissions

**Alternatives considered**:

- Hard delete - rejected (loses audit trail, violates data retention policies)
- Attribute-Based Access Control (ABAC) - deferred (too complex for current needs)
- Hard-coded roles - rejected (not flexible enough)

## Data Model

### Database Schema (JPA Entities)

```sql
CREATE TABLE members (
    member_id UUID PRIMARY KEY,
    registration_number VARCHAR(10) UNIQUE NOT NULL,

    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    nationality VARCHAR(3) NOT NULL,  -- ISO 3166-1 alpha-3 or alpha-2
    gender VARCHAR(10) NOT NULL,
    rodne_cislo VARCHAR(11),  -- Czech ID, encrypted, optional for CZ only

    -- Address
    street VARCHAR(200),
    city VARCHAR(100),
    postal_code VARCHAR(10),
    country VARCHAR(3),

    -- Contact
    email VARCHAR(255),  -- primary email
    phone VARCHAR(20),   -- primary phone

    -- Optional
    chip_number BIGINT,
    bank_account VARCHAR(50),

    -- Membership status (soft delete)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP,
    deactivated_by VARCHAR(100),
    deactivation_reason TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    -- RodneCislo only allowed for Czech nationality
    CONSTRAINT chk_nationality_rodne CHECK (
        nationality = 'CZ' OR rodne_cislo IS NULL
    )
);

CREATE TABLE member_contacts (
    contact_id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES members(member_id),
    contact_type VARCHAR(20) NOT NULL, -- MEMBER, GUARDIAN
    email VARCHAR(255),
    phone VARCHAR(20),
    is_primary BOOLEAN DEFAULT FALSE
);

CREATE TABLE guardian_information (
    guardian_id UUID PRIMARY KEY,
    member_id UUID NOT NULL REFERENCES members(member_id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50) NOT NULL, -- PARENT, LEGAL_GUARDIAN
    UNIQUE(member_id)
);

CREATE INDEX idx_members_registration_number ON members(registration_number);
CREATE INDEX idx_member_contacts_member ON member_contacts(member_id);
```

## Risks / Trade-offs

### Risk: Registration Number Collision (edge case)

- **Impact**: Rare race condition in high-concurrency scenarios
- **Mitigation**: Database unique constraint + optimistic locking, retry logic

### Risk: GDPR Compliance Complexity

- **Impact**: Encryption, audit logging, consent management overhead
- **Mitigation**: Use proven libraries (Jasypt for encryption), defer consent UI to later phase

### Risk: Email Delivery Failures

- **Impact**: Member created but welcome email not received
- **Mitigation**: Async processing with retry, admin UI to resend email

### Trade-off: Eventually Consistent Email

- **Benefit**: Fast response, resilient to SMTP failures
- **Cost**: User might not receive email immediately

### Trade-off: Rich Value Objects vs Simple DTOs

- **Benefit**: Strong domain model, encapsulated validation
- **Cost**: More classes, mapping overhead (mitigated by MapStruct)

## Migration Plan

**N/A** - This is the initial implementation, no migration needed.

## Resolved Questions

1. **Club code source**: ✅ Application property `klabis.club.code=ZBM` in `application.yml`
    - 3-character alphanumeric code (letters and digits allowed)

2. **Training group assignment**: ✅ Deferred to separate change (added with user groups feature)

3. **Email service**: ✅ SMTP via Spring JavaMailSender for MVP

4. **ORIS/CUS integration**: ✅ Deferred to separate change

5. **Authentication flow**: ✅ OAuth2 via Spring Authorization Server (same host as backend API)
    - Welcome email contains link to account activation

6. **UI approach**: ✅ Responsive web (Vite + React), no native app needed
