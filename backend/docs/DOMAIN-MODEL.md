# Domain Model

## Overview

This document describes the domain model of the Klabis Backend, including bounded contexts, aggregates, value objects,
and domain events. The domain model follows **Domain-Driven Design (DDD)** principles with clear boundaries between
business domains.

## Bounded Contexts

### 1. Members Bounded Context

**Purpose:** Manages club member registration and information

**Key Entities:**

- Member (aggregate root)
- RegistrationNumber (value object)
- EmergencyContact (value object)
- MembershipStatus (value object)

**Responsibilities:**

- Member data management
- Registration number generation
- Membership tracking
- Member profile updates

**Domain Events:**

- `MemberCreatedEvent` - Published when new member registered

**Dependencies:**

- Reads from Users context (for password setup)
- No events published to other contexts (sends email via own event handler)

### 2. Users Bounded Context

**Purpose:** Manages user authentication and account lifecycle

**Key Entities:**

- User (aggregate root)
- AccountStatus (enum)
- Role (enum)
- PasswordSetupToken (aggregate root)
- TokenHash (value object)

**Responsibilities:**

- User authentication
- Account activation
- Password management
- Security token handling
- Rate limiting for sensitive operations

**Domain Events:**

- None (currently consumes events only)

**Dependencies:**

- No dependencies on other bounded contexts

### 3. Events Bounded Context (Future)

**Purpose:** Domain events and event sourcing

**Status:** Planned implementation

**Planned Features:**

- Event store for audit trail
- Event replay capabilities
- Temporal queries

### 4. Finances Bounded Context (Future)

**Purpose:** Financial transactions and billing

**Status:** Planned implementation

**Planned Features:**

- Membership fees
- Payment processing
- Financial reporting

### 5. Common Kernel

**Purpose:** Shared utilities and cross-cutting concerns

**Components:**

- Email service
- Exception handling
- Value objects shared across contexts
- Utilities

## Aggregates

### Member Aggregate

**Aggregate Root:** `Member`

**Purpose:** Encapsulates member data and business rules

**Key Invariants:**

- Registration number is unique and immutable
- Email address is required and must be valid
- Birth date cannot be in the future
- Emergency contact is required

**Value Objects:**

- `RegistrationNumber` - Unique member identifier (ZBM format)
- `EmailAddress` - Validated email
- `PhoneNumber` - E.164 format phone number
- `Address` - Postal address
- `EmergencyContact` - Emergency contact details

**Domain Events:**

- `MemberCreatedEvent` - Published on member creation

**Example:**

```java
Member member = Member.create(
    firstName,
    lastName,
    birthDate,
    email,
    phone,
    address,
    emergencyContact
);
// MemberCreatedEvent automatically registered
```

### User Aggregate

**Aggregate Root:** `User`

**Purpose:** Encapsulates user authentication and account management

**Key Invariants:**

- Registration number is unique and immutable
- Password hash is required for active accounts
- Account status must be valid
- At least one role assigned

**Value Objects:**

- `UserId` - UUID-based identifier (shared with Member aggregate)
- `RegistrationNumber` - Same as Member's registration number

**States:**

- `PENDING_ACTIVATION` - Account created, awaiting password setup
- `ACTIVE` - Account fully activated
- `LOCKED` - Account locked (future implementation)

**Relationship with Member:**

- User and Member aggregates share the same `UserId` value object
- When a new member is registered:
    1. User is created first with a generated `UserId`
    2. Member is created using the same `UserId`
    3. This ensures `Member.getId() == User.getId()` for all members

### PasswordSetupToken Aggregate

**Aggregate Root:** `PasswordSetupToken`

**Purpose:** Manages secure password setup for new users

**Key Invariants:**

- Token must have valid expiration time in the future
- Token can only be used once (single-use)
- Token hash is immutable (never changes after creation)
- Plain text tokens are never persisted

**Value Objects:**

- `TokenHash` - SHA-256 hash of token
- `UserId` - References the user

**Lifecycle:**

1. Token generated via `PasswordSetupToken.generateFor()`
2. Hash stored in database (plain text returned for email)
3. Token validated via `verify()`
4. Marked as used after password set
5. Expired tokens cleaned up by scheduled job

## Value Objects

### UserId

**Purpose:** Type-safe identifier shared between User and Member aggregates

**Implementation:** Java record

```java
public record UserId(UUID value) {
    public UserId {
        Assert.notNull(value, "UserId cannot be null");
    }

    public static UserId fromUUID(UUID uuid) {
        return new UserId(uuid);
    }

    public static UserId fromString(String uuidString) {
        try {
            return new UserId(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuidString, e);
        }
    }
}
```

**Key Features:**

- Immutable (record property)
- Automatic equals() and hashCode() based on wrapped UUID
- Prevents accidental mixing of IDs from different aggregates
- Factory methods for creation

### RegistrationNumber

**Purpose:** Unique member registration number in ZBM format

**Format:** `ZBM{YY}{DD}` where:

- `ZBM` - Club code (configurable)
- `YY` - Birth year (last 2 digits)
- `DD` - Sequential number (2 digits, 01-99)

**Implementation:** Java record with validation

**Example:** `ZBM9001` - First member born in 1990

### EmailAddress

**Purpose:** Type-safe email validation

**Implementation:** Java record with compact constructor

```java
public record EmailAddress(String value) {
    public EmailAddress {
        Assert.hasText(value, "Email is required");
        Assert.isTrue(value.contains("@"), "Email must contain @ symbol");
        Assert.isTrue(value.indexOf("@") < value.lastIndexOf(".") - 1,
                      "Email must have valid domain");
    }

    public static EmailAddress of(String email) {
        return new EmailAddress(email);
    }
}
```

### PhoneNumber

**Purpose:** Type-safe phone number validation (E.164 format)

**Implementation:** Java record with validation

```java
public record PhoneNumber(String value) {
    public PhoneNumber {
        Assert.hasText(value, "Phone number is required");
        Assert.isTrue(value.startsWith("+"),
                      "Phone must start with + (E.164 format)");
        Assert.isTrue(value.substring(1).replaceAll("[\\s+]", "")
                      .matches("[0-9]+"),
                      "Phone can only contain digits and spaces after +");
    }

    public static PhoneNumber of(String phone) {
        return new PhoneNumber(phone);
    }
}
```

**Example:** `+420 123 456 789`

### Address

**Purpose:** Type-safe postal address with validation

**Implementation:** Java record with compact constructor

```java
public record Address(
    String street,
    String city,
    String postalCode,
    String country
) {
    public Address {
        Assert.hasText(street, "Street is required");
        Assert.hasText(city, "City is required");
        Assert.hasText(postalCode, "Postal code is required");
        Assert.hasText(country, "Country is required");
        Assert.isTrue(postalCode.length() <= 20 &&
                      postalCode.matches("[A-Za-z0-9-]+"),
                      "Postal code format invalid");
        Assert.isTrue(country.length() == 2 &&
                      country.matches("[A-Z]{2}"),
                      "Country must be ISO 3166-1 alpha-2");
        Assert.isTrue(street.length() <= 200, "Street too long");
        Assert.isTrue(city.length() <= 100, "City too long");
    }

    public static Address of(String street, String city, String postalCode, String country) {
        return new Address(street, city, postalCode, country);
    }
}
```

### TokenHash

**Purpose:** Secure token hashing for password setup tokens

**Algorithm:** SHA-256

**Implementation:** Java record with validation

**Key Features:**

- One-way hash (cannot be reversed)
- Immutable
- Secure comparison method to prevent timing attacks

## Domain Events

### MemberCreatedEvent

**Purpose:** Notification that a new member has been registered

**Published by:** Members module (Member aggregate)

**Consumed by:** Members module (MemberCreatedEventHandler)

**Payload:**

```java
public record MemberCreatedEvent(
    UUID eventId,
    UUID memberId,
    RegistrationNumber registrationNumber,
    String firstName,
    String lastName,
    EmailAddress email,
    Instant occurredOn
) {
    public static MemberCreatedEvent fromMember(Member member) {
        return new MemberCreatedEvent(
            UUID.randomUUID(),
            member.getId().value(),
            member.getRegistrationNumber(),
            member.getFirstName(),
            member.getLastName(),
            member.getEmail(),
            Instant.now()
        );
    }
}
```

**Side Effects:**

- Sends password setup email to member
- Creates password setup token in Users context

## Business Rules

### Registration Number Generation

**Algorithm:**

1. Extract birth year from member's birth date
2. Count existing members with same birth year
3. Format: `{clubCode}{birthYear % 100}{sequence + 1}`

**Example:**

```
Club code: ZBM
Birth year: 1990
Existing members born in 1990: 5
New registration number: ZBM9006
```

**Uniqueness:** Enforced at database level with unique constraint

### Account Activation Flow

1. **Member Created** → User account created with status `PENDING_ACTIVATION`
2. **Token Generated** → PasswordSetupToken created with 4-hour expiration
3. **Email Sent** → Password setup email with secure token link
4. **User Clicks Link** → Token validated (not expired, not used)
5. **Password Set** → User activated, token marked as used
6. **Account Active** → User can now authenticate

**Constraints:**

- Token expires after 4 hours
- Token can only be used once
- Rate limited to 3 token requests per hour per registration number
- Password must meet complexity requirements

### Password Complexity Requirements

**Minimum Requirements:**

- At least 12 characters
- Contains uppercase letter
- Contains lowercase letter
- Contains digit
- Contains special character

**Additional Validation:**

- Cannot contain common passwords
- Cannot contain personal information (first name, last name, registration number)

## Repository Interfaces

### Domain Layer (Interfaces)

```java
// Members context
public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(UserId id);
    Optional<Member> findByRegistrationNumber(RegistrationNumber number);
    List<Member> findAll();
    void delete(Member member);
    long countByBirthYear(int year);
}

// Users context
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByRegistrationNumber(RegistrationNumber number);
    boolean existsByRegistrationNumber(RegistrationNumber number);
    void delete(User user);
}

public interface PasswordSetupTokenRepository {
    PasswordSetupToken save(PasswordSetupToken token);
    Optional<PasswordSetupToken> findByTokenHash(TokenHash hash);
    void invalidateAllForUser(UserId userId);
    void delete(PasswordSetupToken token);
}
```

**Key Points:**

- Interfaces defined in domain layer (at module root)
- Repository adapters in infrastructure layer implement domain interfaces
- No Spring Data annotations in domain layer
- Returns domain entities, not mementos
- Spring Data JDBC repositories work with mementos internally
- Adapters handle conversion between domain entities and mementos

## Data Mapping

### Domain Entities vs. Memento Pattern

**Architecture Decision:** Separate domain entities from persistence using Memento pattern

**Domain Entities:**

- Pure business logic
- No framework annotations
- Located at module root (aggregates, value objects, enums, events)
- Used by feature packages

**Memento Pattern:**

- Persistence concerns handled by dedicated Memento classes
- Spring Data JDBC annotations on Memento classes
- Located in `persistence/jdbc/` package (internal)
- Map to database tables
- Convert to/from domain entities via `from()` and `toDomain()` methods

**Repository Architecture:**

```
Domain Repository Interface (e.g., MemberRepository)
    ↓
Repository Adapter (e.g., MemberRepositoryAdapter)
    - Converts domain entities ↔ mementos
    - Implements domain repository interface
    ↓
Spring Data JDBC Repository (e.g., MemberJdbcRepository)
    - Handles CRUD operations
    - Returns Memento instances
    ↓
Memento (e.g., MemberMemento)
    - Contains Spring Data JDBC annotations
    - Flat structure matching database schema
    - Converts to/from domain entity
```

**Benefits:**

- Domain entities remain pure (no Spring annotations)
- Clear separation between business logic and persistence
- Easy to test domain logic in isolation
- Memento classes handle all database mapping concerns

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Overall architecture overview
- [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Event-driven communication
- [SPRING_SECURITY_ARCHITECTURE.md](./SPRING_SECURITY_ARCHITECTURE.md) - Security and authentication

---

**Last Updated:** 2026-01-31
**Version:** 3.1
**Status:** Active
