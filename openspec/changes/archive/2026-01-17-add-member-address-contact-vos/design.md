# Design: Add Member Address and Contact Information Value Objects

## Problem Statement

The member domain currently has two gaps in personal information management:

1. **Missing Address**: The specification requires "Address (full postal address)" with structured fields (street, city,
   postalCode, country), but the current implementation has no address storage at all. The database has no address
   columns, and the domain model has no Address value object.

2. **Primitive Contact Storage**: Email addresses and phone numbers are stored as `Set<String>` which lacks:
    - Structure (just strings, no EmailAddress or PhoneNumber types)
    - Validation (minimal format checking)
    - Simplicity (allows multiple contacts but business only needs one)
    - Type safety (just strings, can mix emails and phones)

These gaps create:

- Incomplete member data (cannot send postal correspondence)
- Weak validation (invalid emails/phones can be stored)
- Unnecessary complexity (sets for what should be single values)
- Technical debt (implementation doesn't match specification)

## Design Decisions

### 1. Value Objects for Address and Contact Information

**Decision**: Create immutable value objects for Address, EmailAddress, and PhoneNumber. Keep Member with single email
and phone (not collections).

**Rationale**:

- **Domain-Driven Design**: Address and contact information are value objects (compared by value, not identity)
- **Encapsulation**: Validation logic centralized in value object factory methods
- **Type Safety**: `EmailAddress` and `PhoneNumber` types prevent mixing up strings
- **Immutability**: Value objects are immutable, preventing invalid state transitions
- **Simplicity**: Single email/phone per member matches business needs

**Alternatives Considered**:

- ❌ Keep as primitive strings: Loses validation and type safety
- ❌ Use Hibernate Embeddable: Ties domain to persistence framework
- ❌ Multiple contacts with primary designation: Over-engineering for current needs
- ✅ Single contact value objects: Simple, type-safe, sufficient

### 2. Simple EmailAddress and PhoneNumber Value Objects

**Decision**: Create EmailAddress and PhoneNumber value objects with format validation. Member has single email and
phone. GuardianInformation also uses these value objects.

**Rationale**:

- **Type Safety**: Prevents mixing emails and phones
- **Validation**: RFC 5322 (email) and E.164 (phone) format validation
- **Domain Expressiveness**: `EmailAddress` is more expressive than `String`
- **Business Alignment**: One contact per member/guardian sufficient for current needs
- **Consistency**: Guardian email/phone validated same way as member email/phone

**Structure**:

```java
class EmailAddress {
    private final String value;  // validated email
}

class PhoneNumber {
    private final String value;  // validated phone
}

class Member {
    private final EmailAddress email;  // single email
    private final PhoneNumber phone;   // single phone
}

class GuardianInformation {
    private final PersonName name;
    private final String relationship;
    private final EmailAddress email;  // now uses EmailAddress value object
    private final PhoneNumber phone;   // now uses PhoneNumber value object
}
```

**Validation Strategy**:

- **EmailAddress**: Basic RFC 5322 validation (contains @ and domain part)
    - Not using full RFC 5322 regex (complex, over-engineering for business needs)
    - Sufficient for business validation, deliverability checked by email service
- **PhoneNumber**: E.164 international format (`+[country code][number]`)
    - Requires + prefix for international format
    - Allows spaces for readability (e.g., "+420 123 456 789")
    - Prevents local-only numbers (business rule: international communication)

**Alternatives Considered**:

- ❌ Complex email regex (RFC 5322 full): Over-engineering, hard to maintain
- ❌ Phone number library (libphonenumber): External dependency for simple validation
- ❌ Multiple contacts with primary: Adds complexity not needed by business
- ✅ Simple single contact validation: Sufficient for business needs, easy to test

**Guardian Information Updates**:

- Guardian email/phone now use same EmailAddress and PhoneNumber value objects
- This ensures guardian contacts are validated consistently with member contacts
- Guardian construction now requires valid EmailAddress and PhoneNumber objects
- Validation errors in guardian email/phone fail fast during Member creation

### 3. Address Value Object Structure

**Decision**: Simple Western address format with street, city, postalCode, country fields.

**Rationale**:

- **Simplicity**: Covers most club member addresses (Czech Republic and European countries)
- **ISO 3166-1 alpha-2 Country Codes**: Standard format (e.g., "CZ", "US", "DE")
- **Validation**: Required fields, length limits, alphanumeric postal code
- **Future-Proof**: Can be extended with additional fields (state/region) if needed

**Structure**:

```java
class Address {
    private final String street;      // max 200 chars
    private final String city;        // max 100 chars
    private final String postalCode;  // max 20 chars, alphanumeric + hyphen
    private final String country;     // ISO 3166-1 alpha-2
}
```

**Validation Rules**:

- All fields required (no null/blank)
- Street ≤ 200 characters
- City ≤ 100 characters
- Postal code ≤ 20 characters, alphanumeric + hyphen only
- Country must be valid ISO 3166-1 alpha-2 code

**Alternatives Considered**:

- ❌ International address formats (Japan, Middle East, etc.): Over-engineering for current user base
- ❌ Geocoding/validation service: External dependency, complexity, cost
- ❌ Free-form address text: Loses structure for search/filtering
- ✅ Simple structured address: Sufficient for business needs

### 4. Database Schema Design

**Decision**: Update initial Flyway migration to include address columns and single email/phone columns.

**Note**: Application uses in-memory H2 database only. No data migration needed.

**Schema Changes**:

```sql
-- Update V001__create_members_table.sql
-- Address columns
street VARCHAR(200),
city VARCHAR(100),
postal_code VARCHAR(20),
country VARCHAR(2),  -- ISO 3166-1 alpha-2

-- Replace CSV contact columns with single values
email VARCHAR(255),
phone VARCHAR(50)
```

**Rationale**:

- **Simplification**: Single email/phone columns instead of CSV TEXT
- **Query Optimization**: VARCHAR columns enable efficient filtering/search
- **No Joins**: Address embedded in members table (not separate table) for read performance
- **In-Memory Only**: No migration complexity since database resets on each restart

**Alternatives Considered**:

- ❌ Separate addresses table: Over-normalization, members have single address
- ❌ Keep CSV for multiple contacts: Adds complexity not needed
- ❌ Separate emails/phones tables: Over-engineering
- ✅ Single email/phone columns: Simple, performant, matches business needs

### 5. API Breaking Changes

**Decision**: Change emails/phones from string arrays to single string fields.

**Before**:

```json
{
  "emails": ["john@example.com", "john.backup@example.com"],
  "phones": ["+420123456789", "+420987654321"]
}
```

**After**:

```json
{
  "email": "john@example.com",
  "phone": "+420123456789",
  "address": {
    "street": "Hlavní 123",
    "city": "Praha",
    "postalCode": "11000",
    "country": "CZ"
  }
}
```

**Rationale**:

- **Simplicity**: Single values instead of arrays
- **Business Alignment**: Matches business need (one contact per member)
- **Breaking Change Justified**: Better to break API now than carry technical debt

**Migration Path for Frontend**:

1. Update frontend to send single email/phone strings
2. Update frontend to include address object
3. Deploy backend (validates new structure)
4. Deploy frontend (uses new structure)
5. No backward compatibility layer (clean break)

**Alternatives Considered**:

- ❌ Keep arrays with single element: Confusing, unnecessary
- ❌ Backward compatibility layer: Adds complexity
- ✅ Clean API break: Clear structure, simple

### 6. Validation Strategy

**Decision**: Multi-layered validation (value objects → aggregate → application layer → presentation).

**Validation Layers**:

1. **Value Object Validation** (Address, EmailAddress, PhoneNumber):
    - Format validation (email has @, phone starts with +)
    - Length constraints (street ≤ 200 chars)
    - Required field checks
    - Throws `IllegalArgumentException` for invalid data

2. **Aggregate Validation** (Member):
    - Email and phone required (member OR guardian)
    - Throws `IllegalArgumentException` for business rule violations

3. **Application Layer Validation** (DTOs with Bean Validation):
    - `@NotBlank`, `@Size`, `@Email`, `@Pattern` annotations
    - Request validation before domain object creation
    - Returns 400 Bad Request with validation errors

4. **Presentation Layer Validation** (API):
    - `@Valid` annotation on request parameters
    - Automatic validation error serialization (ProblemDetail format)

**Rationale**:

- **Defense in Depth**: Multiple validation layers prevent invalid data
- **Clear Responsibility**: Each layer validates its concerns
- **Fail Fast**: Value objects throw exceptions immediately on invalid construction
- **User-Friendly Errors**: Application layer returns structured validation errors

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                          │
│  MemberController                                               │
│  - POST /api/members (validates AddressRequest, email, phone)   │
│  - GET /api/members/{id} (returns AddressResponse, email/phone) │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                            │
│  RegisterMemberCommandHandler                                   │
│  - Converts AddressRequest → Address value object               │
│  - Converts String email → EmailAddress value object            │
│  - Converts String phone → PhoneNumber value object             │
│  - Calls Member.create() with value objects                     │
│                                                                 │
│  DTOs: AddressRequest, AddressResponse                          │
│        email/phone as simple String fields                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Domain Layer                               │
│  Member Aggregate                                               │
│  - address: Address (value object)                              │
│  - email: EmailAddress (value object)                           │
│  - phone: PhoneNumber (value object)                            │
│                                                                 │
│  Value Objects:                                                 │
│  ┌───────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ Address           │  │ EmailAddress     │  │ PhoneNumber  │ │
│  │ - street          │  │ - value: String  │  │ - value: Str │ │
│  │ - city            │  └──────────────────┘  └──────────────┘ │
│  │ - postalCode      │                                          │
│  │ - country         │  GuardianInformation (also uses          │
│  └───────────────────┘  EmailAddress and PhoneNumber)           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                          │
│  MemberEntity (JPA)                                             │
│  - street, city, postal_code, country (columns)                 │
│  - email VARCHAR(255) (single value, no CSV)                    │
│  - phone VARCHAR(50) (single value, no CSV)                     │
│                                                                 │
│  MemberMapper                                                   │
│  - toEntity(): Address → street/city/postalCode/country         │
│  - toEntity(): EmailAddress → email VARCHAR                     │
│  - toEntity(): PhoneNumber → phone VARCHAR                      │
│  - toDomain(): columns → Address value object                   │
│  - toDomain(): email → EmailAddress value object                │
│  - toDomain(): phone → PhoneNumber value object                 │
└─────────────────────────────────────────────────────────────────┘
```

## Trade-offs

### Chosen: Simple Single Contact Value Objects

**Pros**:

- ✅ Type safety (EmailAddress vs String)
- ✅ Validation encapsulation
- ✅ Domain expressiveness
- ✅ Simplicity (single values, not collections)
- ✅ Business alignment (one contact sufficient)
- ✅ Easy to test
- ✅ No migration complexity (in-memory DB only)

**Cons**:

- ❌ Breaking API change (arrays → single strings)
- ❌ Cannot store backup contacts

**Why Chosen**: Simplicity and business alignment. Business confirmed one contact per member is sufficient.

### Alternative: Multiple Contacts with Primary Designation

**Pros**:

- ✅ No data loss (preserve secondary contacts)
- ✅ Backup contacts available

**Cons**:

- ❌ Added complexity (ContactInformation composite, isPrimary logic)
- ❌ More complex API (object arrays instead of strings)
- ❌ Over-engineering for current business needs
- ❌ More database columns and migration complexity

**Why Rejected**: Adds significant complexity for a feature business doesn't need.

## Testing Strategy

### Unit Tests (Domain Layer)

- **Address**: 10 tests (valid creation, null fields, oversized fields, invalid formats)
- **EmailAddress**: 4 tests (valid email, missing @, missing domain, blank)
- **PhoneNumber**: 5 tests (valid E.164, with spaces, missing +, with letters, blank)
- **Member**: Update 18 existing tests to use new value objects

**Total Domain Tests**: ~37 tests (18 existing + 19 new)

### Integration Tests (Infrastructure Layer)

- **MemberMapper**: 3 tests (toEntity/toDomain with address, null address handling)
- **Member Creation**: 3 tests (with address, validation failures, null address)
- **Data Migration**: 1 test (CSV to single value conversion)

**Total Integration Tests**: ~7 tests

### API Tests (Presentation Layer)

- **Controller**: 6 tests (create with address, validation errors, null address handling)

**Total API Tests**: ~6 tests

### E2E Tests

- Complete member creation flow with address and single contacts
- Legacy member retrieval with null address
- Contact migration from CSV format

**Total E2E Tests**: ~3 tests

**Grand Total**: ~53 tests for this change

## Risk Mitigation

### Risk: Breaking API Change Breaks Existing Clients

**Mitigation**:

- Frontend and backend deployed together (same team, coordinated release)
- No external API consumers yet (internal-only system)
- Clear migration guide in documentation
- Validation errors provide clear feedback on structure mismatch

### Risk: Address Format Doesn't Support International Addresses

**Mitigation**:

- Current user base is Czech club members (Western address format sufficient)
- ISO 3166-1 alpha-2 country codes support international addresses
- Can extend Address value object with optional fields (state, region) in future
- Address is value object (easy to replace if international format needed later)

## Success Criteria

1. ✅ All value objects (Address, EmailAddress, PhoneNumber) created with validation
2. ✅ Member aggregate updated to use value objects (single email/phone)
3. ✅ Database schema updated with address columns and single contact columns
4. ✅ API accepts and returns single email/phone strings and structured address
5. ✅ Address uses ISO 3166-1 alpha-2 country codes (CZ, US, DE)
6. ✅ All tests pass (53+ tests)
7. ✅ Manual testing confirms member creation and retrieval work correctly
8. ✅ Specification compliance gap closed (address and validated contacts implemented)
