# Change: Add Member Address and Contact Information Value Objects

## Why

The member domain currently stores address and contact information in a simplified form that was deferred during initial
implementation:

- **Address**: Not implemented at all - the spec requires "Address (full postal address)" and an address object with
  street, city, postalCode, and country, but the database has no address columns and the domain model has no Address
  value object
- **Contact Information**: Currently stored as primitive `Set<String>` for emails and phones without proper structure or
  validation

This technical debt creates several problems:

1. **No address capture**: Members cannot provide their postal address, which is required for official correspondence,
   emergency contacts, and regulatory compliance
2. **Weak contact validation**: Email and phone validation is minimal (basic format checks only)
3. **Poor encapsulation**: Contact validation scattered across layers
4. **Multiple contacts complexity**: Current implementation allows multiple emails/phones but business only needs one
   per member
5. **Spec compliance gap**: Current implementation doesn't match the spec requirements for address object structure

Adding proper Address, EmailAddress, and PhoneNumber value objects will:

- Complete the deferred member personal information requirements
- Enable proper validation of postal addresses and contact data
- Simplify contact model (single email and phone per member)
- Improve domain model expressiveness and type safety
- Align implementation with existing spec requirements

## What Changes

### Address Value Object

- Create `Address` value object with street, city, postalCode, and country fields
- Add validation for required fields and format constraints (ISO 3166-1 alpha-2 country codes)
- Add `address` field to Member aggregate
- Update database schema to add address columns to members table
- Update API request/response DTOs to include address object

### Contact Information Simplification

- Create `EmailAddress` value object with RFC 5322 basic format validation
- Create `PhoneNumber` value object with E.164 international format validation
- Replace `Set<String> emails` and `Set<String> phones` with single `EmailAddress` and `PhoneNumber` in Member aggregate
- Update database schema: single email and phone columns (replacing CSV TEXT columns)
- Update API: single email and phone fields (breaking change from array to string)
- **Update GuardianInformation to use EmailAddress and PhoneNumber value objects** instead of plain strings for guardian
  email/phone

### Breaking API Changes

- Member creation request: `emails` array becomes single `email` string, `phones` array becomes single `phone` string
- Member detail response: `address` added as required object
- Member detail response: `emails` array becomes single `email` string, `phones` array becomes single `phone` string

## Out of Scope (Future Changes)

- International address format variations (using simplified Western address format)
- Address verification/geocoding integration
- Email verification workflow (deliverability checks)
- Phone number verification (SMS confirmation)
- Multiple email/phone support per member (simplified to single contact per member)
- Historical tracking of address/contact changes

## Impact

### Affected Specs

- **members**: Add Address requirement, simplify contact information to single email/phone

### Affected Code

- **Domain layer**:
    - New: `com.klabis.members.domain.Address` value object
    - New: `com.klabis.members.domain.EmailAddress` value object
    - New: `com.klabis.members.domain.PhoneNumber` value object
    - Modified: `com.klabis.members.domain.Member` - add address field, replace email/phone sets with single
      EmailAddress and PhoneNumber
    - Modified: `com.klabis.members.domain.GuardianInformation` - replace plain string email/phone with EmailAddress and
      PhoneNumber value objects
- **Application layer**:
    - Modified: `RegisterMemberCommand` - update request structure (single email/phone, add address)
    - Modified: `MemberResponse` - update response structure
    - Modified: `RegisterMemberCommandHandler` - handle new value objects and construct EmailAddress/PhoneNumber for
      guardian
- **Infrastructure layer**:
    - Modified: `MemberEntity` - add address columns, change emails/phones from TEXT to VARCHAR single values
    - Modified: `MemberMapper` - map new value objects, handle EmailAddress/PhoneNumber for guardian
    - Modified: `V001__create_members_table.sql` - update initial schema

### Database Changes

**Note**: Application currently uses in-memory H2 database only. No data migration needed.

Update initial Flyway migration (`V001__create_members_table.sql`):

- Add columns: `street` VARCHAR(200), `city` VARCHAR(100), `postal_code` VARCHAR(20), `country` VARCHAR(2)
- Replace columns: `emails` TEXT → `email` VARCHAR(255), `phones` TEXT → `phone` VARCHAR(50)
- All columns nullable except email/phone (at least one contact required)
- Country uses ISO 3166-1 alpha-2 format (e.g., "CZ", "US", "DE")

## Breaking Changes

**API Request Format**:

```json
// BEFORE
{
  "emails": ["john@example.com", "john.backup@example.com"],
  "phones": ["+420123456789", "+420987654321"],
  "guardian": {
    "firstName": "Pavel",
    "lastName": "Novák",
    "relationship": "PARENT",
    "email": "pavel.novak@example.com",
    "phone": "+420987654321"
  }
}

// AFTER
{
  "email": "john@example.com",
  "phone": "+420123456789",
  "address": {
    "street": "Hlavní 123",
    "city": "Praha",
    "postalCode": "11000",
    "country": "CZ"
  },
  "guardian": {
    "firstName": "Pavel",
    "lastName": "Novák",
    "relationship": "PARENT",
    "email": "pavel.novak@example.com",
    "phone": "+420987654321"
  }
}
```

**API Response Format**:

- Same structure changes as request
- `emails` array → `email` string (member)
- `phones` array → `phone` string (member)
- `address` field added as required object (member)
- Guardian email/phone now validated with same rules as member (E.164 format for phone)

**Migration Path for Clients**:

1. Update frontend to send single email/phone instead of arrays
2. Update frontend to include address object in member creation
3. Update frontend to use ISO 3166-1 alpha-2 country codes (CZ, US, DE instead of CZE, USA, DEU)
4. Deploy backend (accepts new structure)
5. Deploy frontend (uses new structure)
