# API Usage Examples: Member Self-Edit and Admin Edit API

**Endpoint:** `PATCH /api/members/{id}`
**Authentication:** OAuth2 required
**Media Type:** `application/json` (request), `application/prs.hal-forms+json` (response)

---

## Table of Contents

1. [Authentication Setup](#authentication-setup)
2. [Member Self-Edit Scenarios](#member-self-edit-scenarios)
3. [Admin Edit Scenarios](#admin-edit-scenarios)
4. [Partial Updates](#partial-updates)
5. [Error Responses](#error-responses)
6. [HAL+FORMS Response Examples](#halforms-response-examples)
7. [OAuth2 Authentication Examples](#oauth2-authentication-examples)

---

## Authentication Setup

### Using OAuth2 Authorization Code Flow

**Prerequisites:**

- OAuth2 client configured (e.g., `klabis-web`)
- User authenticated via OAuth2 authorization server
- Access token obtained

**Setup in IntelliJ HTTP Client:**

```http
### Setup: Get OAuth2 Token
# @name login
POST {{authServerUrl}}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code={{authorizationCode}}
&redirect_uri={{redirectUri}}
&client_id={{clientId}}
&client_secret={{clientSecret}}

### Extract token for subsequent requests
@token = {{login.response.body.access_token}}
```

**Using the Token:**

```http
PATCH /api/members/{{memberId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "email": "new.email@example.com"
}
```

---

## Member Self-Edit Scenarios

### Scenario 1: Update Email Address

**Use Case:** Member changes their own email address

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "jan.novak@example.com"
}
```

**Response (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "registrationNumber": "ZBM9001",
  "firstName": "Jan",
  "lastName": "Novák",
  "email": "jan.novak@example.com",
  "phone": "+420123456789",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/members/550e8400-e29b-41d4-a716-446655440000"
    },
    "collection": {
      "href": "http://localhost:8080/api/members"
    },
    "edit": {
      "href": "http://localhost:8080/api/members/550e8400-e29b-41d4-a716-446655440000"
    }
  }
}
```

**Authorization:** Member must be authenticated and OAuth2 subject email must match member's email

---

### Scenario 2: Update Phone Number

**Use Case:** Member updates their phone number

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "phone": "+420777123456"
}
```

**Response:** (200 OK with updated member)

**Validation:** Phone must match pattern `^\+?[0-9]{9,15}$`

---

### Scenario 3: Update Address

**Use Case:** Member moves to new address

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "address": {
    "street": "Nová 123",
    "city": "Praha",
    "postalCode": "11000",
    "country": "CZE"
  }
}
```

**Response:** (200 OK with updated address)

**Validation:** All address fields required (street, city, postalCode, country)

---

### Scenario 4: Update Dietary Restrictions

**Use Case:** Member specifies dietary restrictions

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "dietaryRestrictions": "Vegetarian, no nuts"
}
```

**Response:** (200 OK with updated dietary restrictions)

**Validation:** Max 500 characters

---

## Admin Edit Scenarios

### Scenario 5: Admin Updates Chip Number

**Use Case:** Admin updates member's chip number

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "chipNumber": "12345"
}
```

**Response:** (200 OK with updated chip number)

**Authorization:** Requires `MEMBERS:UPDATE` permission
**Validation:** Chip number must be numeric (pattern `^[0-9]+$`)

---

### Scenario 6: Admin Updates Identity Card

**Use Case:** Admin adds/updates member's identity card information

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "identityCard": {
    "cardNumber": "12345678",
    "validityDate": "2026-12-31"
  }
}
```

**Response:** (200 OK with updated identity card)

**Authorization:** Requires `MEMBERS:UPDATE` permission
**Validation:**

- Card number: Required, max 50 characters
- Validity date: Required, must not be in past

---

### Scenario 7: Admin Updates Medical Course

**Use Case:** Admin records member's medical course completion

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "medicalCourse": {
    "completionDate": "2024-01-15",
    "validityDate": "2025-01-15"
  }
}
```

**Response:** (200 OK with updated medical course)

**Authorization:** Requires `MEMBERS:UPDATE` permission
**Validation:**

- Completion date: Required
- Validity date: Optional, must be after completion date if provided

---

### Scenario 8: Admin Updates Trainer License

**Use Case:** Admin adds trainer license information

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "trainerLicense": {
    "licenseNumber": "TL-2024-001",
    "validityDate": "2025-06-30"
  }
}
```

**Response:** (200 OK with updated trainer license)

**Authorization:** Requires `MEMBERS:UPDATE` permission
**Validation:**

- License number: Required, max 50 characters
- Validity date: Required, must not be in past

---

### Scenario 9: Admin Updates Driving License Group

**Use Case:** Admin records member's driving license category

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "drivingLicenseGroup": "B"
}
```

**Response:** (200 OK with updated driving license group)

**Authorization:** Requires `MEMBERS:UPDATE` permission
**Valid Values:** B, BE, C, C1, D, D1, T, AM, A1, A2, A

---

### Scenario 10: Admin Edits Another Member

**Use Case:** Admin updates information for different member

**Request:**

```http
PATCH /api/members/660e8400-e29b-41d4-a716-446655440001
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "chipNumber": "67890",
  "identityCard": {
    "cardNumber": "87654321",
    "validityDate": "2027-12-31"
  }
}
```

**Response:** (200 OK with updated member)

**Authorization:** Requires `MEMBERS:UPDATE` permission
**Note:** Admin can edit any member, not just their own record

---

## Partial Updates

### Scenario 11: Update Multiple Fields

**Use Case:** Update multiple fields in single request

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "jan.novak@example.com",
  "phone": "+420777123456",
  "dietaryRestrictions": "Vegetarian"
}
```

**Response:** (200 OK with all fields updated)

**PATCH Semantics:** Only provided fields are updated, null/missing fields remain unchanged

---

### Scenario 12: Single Field Update

**Use Case:** Update only one field (most common pattern)

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "newemail@example.com"
}
```

**Response:** (200 OK with only email updated)

**Efficiency:** PATCH reduces payload by ~90% compared to PUT for single-field updates

---

## Error Responses

### Error 1: Empty Update (400 Bad Request)

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{}
```

**Response (400 Bad Request):**

```json
{
  "type": "https://klabis.com/problems/validation-error",
  "title": "Invalid Update",
  "status": 400,
  "detail": "Update request must contain at least one field to update"
}
```

---

### Error 2: Invalid Email Format (400 Bad Request)

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "invalid-email"
}
```

**Response (400 Bad Request):**

```json
{
  "type": "https://klabis.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Email must be valid"
}
```

---

### Error 3: Member Editing Another Member (403 Forbidden)

**Request:**

```http
### Non-admin member tries to edit different member
PATCH /api/members/660e8400-e29b-41d4-a716-446655440001
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "newemail@example.com"
}
```

**Response (403 Forbidden):**

```json
{
  "type": "https://klabis.com/problems/self-edit-not-allowed",
  "title": "Self-Edit Not Allowed",
  "status": 403,
  "detail": "Members can only edit their own information. Please contact an administrator to update other members."
}
```

**Cause:** OAuth2 subject email doesn't match target member's email

---

### Error 4: Member Accessing Admin-Only Fields (403 Forbidden)

**Request:**

```http
### Non-admin member tries to update chipNumber (admin-only)
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "chipNumber": "12345"
}
```

**Response (403 Forbidden):**

```json
{
  "type": "https://klabis.com/problems/admin-field-access",
  "title": "Admin Field Access Denied",
  "status": 403,
  "detail": "The following fields require admin privileges: chipNumber. Please contact an administrator to update these fields."
}
```

**Cause:** Non-admin user attempting to update admin-only fields

---

### Error 5: Member Not Found (404 Not Found)

**Request:**

```http
PATCH /api/members/non-existent-id
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "test@example.com"
}
```

**Response (404 Not Found):**

```json
{
  "type": "https://klabis.com/problems/member-not-found",
  "title": "Member Not Found",
  "status": 404,
  "detail": "Member with ID non-existent-id not found"
}
```

---

### Error 6: Concurrent Update (409 Conflict)

**Request:**

```http
### Two users update same member simultaneously
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "test@example.com"
}
```

**Response (409 Conflict):**

```json
{
  "type": "https://klabis.com/problems/concurrent-update",
  "title": "Concurrent Update Conflict",
  "status": 409,
  "detail": "This member was modified by another user. Please refresh and try again."
}
```

**Cause:** Optimistic locking detected version mismatch
**Solution:** Client should fetch fresh data and retry

---

### Error 7: Invalid Identity Card (400 Bad Request)

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "identityCard": {
    "cardNumber": "12345678",
    "validityDate": "2020-01-01"
  }
}
```

**Response (400 Bad Request):**

```json
{
  "type": "https://klabis.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Identity card validity date must not be in past"
}
```

**Cause:** Validity date is in the past

---

### Error 8: Invalid Medical Course (400 Bad Request)

**Request:**

```http
PATCH /api/members/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "medicalCourse": {
    "completionDate": "2024-01-15",
    "validityDate": "2023-01-15"
  }
}
```

**Response (400 Bad Request):**

```json
{
  "type": "https://klabis.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Medical course validity date must be after completion date"
}
```

**Cause:** Validity date before completion date

---

## HAL+FORMS Response Examples

### Example 1: Full Response with Links

**Response Structure:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "registrationNumber": "ZBM9001",
  "firstName": "Jan",
  "lastName": "Novák",
  "dateOfBirth": "1990-05-15",
  "nationality": "CZE",
  "gender": "MALE",
  "email": "jan.novak@example.com",
  "phone": "+420777123456",
  "address": {
    "street": "Nová 123",
    "city": "Praha",
    "postalCode": "11000",
    "country": "CZE"
  },
  "guardian": null,
  "active": true,
  "chipNumber": "12345",
  "identityCard": {
    "documentType": "IDENTITY_CARD",
    "number": "12345678",
    "validityDate": "2026-12-31"
  },
  "medicalCourse": {
    "completionDate": "2024-01-15",
    "validityDate": "2025-01-15"
  },
  "trainerLicense": {
    "documentType": "TRAINER_LICENSE",
    "number": "TL-2024-001",
    "validityDate": "2025-06-30"
  },
  "drivingLicenseGroup": "B",
  "dietaryRestrictions": "Vegetarian",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/members/550e8400-e29b-41d4-a716-446655440000"
    },
    "collection": {
      "href": "http://localhost:8080/api/members"
    },
    "edit": {
      "href": "http://localhost:8080/api/members/550e8400-e29b-41d4-a716-446655440000"
    }
  }
}
```

**Link Descriptions:**

- `self`: Current member resource
- `collection`: All members list
- `edit`: Update endpoint for this member

---

### Example 2: Minimal Response (Partial Member)

**Response:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "registrationNumber": "ZBM9001",
  "firstName": "Jan",
  "lastName": "Novák",
  "email": "jan.novak@example.com",
  "phone": "+420123456789",
  "address": {
    "street": "Stará 456",
    "city": "Brno",
    "postalCode": "62000",
    "country": "CZE"
  },
  "guardian": null,
  "active": true,
  "chipNumber": null,
  "identityCard": null,
  "medicalCourse": null,
  "trainerLicense": null,
  "drivingLicenseGroup": null,
  "dietaryRestrictions": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/members/550e8400-e29b-41d4-a716-446655440000"
    },
    "collection": {
      "href": "http://localhost:8080/api/members"
    },
    "edit": {
      "href": "http://localhost:8080/api/members/550e8400-e29b-41d4-a716-446655440000"
    }
  }
}
```

**Note:** Null values for optional fields not yet set

---

## OAuth2 Authentication Examples

### Setup 1: Configure OAuth2 in IntelliJ HTTP Client

**File:** `http-client.env.json`

```json
{
  "dev": {
    "authServerUrl": "http://localhost:8080",
    "apiBaseUrl": "http://localhost:8080",
    "clientId": "klabis-web",
    "clientSecret": "test-secret-123",
    "redirectUri": "http://localhost:3000/oauth/callback"
  }
}
```

---

### Setup 2: Configure Authorization Code Flow

**File:** `http-client.private.env.json` (not committed to git)

```json
{
  "AuthorizationCode": {
    "type": "oauth2",
    "authorizationCode": {
      "authorizationUrl": "http://localhost:8080/oauth2/authorize",
      "tokenUrl": "http://localhost:8080/oauth2/token",
      "clientId": "klabis-web",
      "clientSecret": "test-secret-123",
      "scopes": ["openid", "profile", "email"]
    }
  }
}
```

---

### Example 1: Member Self-Edit with OAuth2

**Request:**

```http
PATCH /api/members/{{memberId}}
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "email": "newemail@example.com",
  "phone": "+420777123456",
  "dietaryRestrictions": "Vegetarian"
}
```

**Flow:**

1. IntelliJ checks for valid access token
2. If expired, prompts user to login via browser
3. User authorizes the OAuth2 client
4. Access token obtained and cached
5. Request sent with Bearer token

---

### Example 2: Admin Edit with OAuth2

**Request:**

```http
### Admin updates member's chip number and documents
PATCH /api/members/{{targetMemberId}}
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
Content-Type: application/json

{
  "chipNumber": "12345",
  "identityCard": {
    "cardNumber": "12345678",
    "validityDate": "2026-12-31"
  },
  "medicalCourse": {
    "completionDate": "2024-01-15",
    "validityDate": "2025-01-15"
  }
}
```

**Requirements:**

- User has `MEMBERS:UPDATE` authority
- OAuth2 token includes required scopes/permissions

---

### Example 3: Handle Token Expiration

**Scenario:** Access token expires

**Error Response (401 Unauthorized):**

```json
{
  "type": "https://klabis.com/problems/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Access token expired"
}
```

**Solution:**

1. IntelliJ HTTP Client automatically prompts for re-authentication
2. User logs in again via browser
3. New access token obtained
4. Request retried with new token

---

## Field Reference

### Member-Editable Fields (Self-Edit)

| Field                 | Type   | Required         | Validation                    | Example                 |
|-----------------------|--------|------------------|-------------------------------|-------------------------|
| `email`               | String | No               | Valid email format            | `"jan@example.com"`     |
| `phone`               | String | No               | Pattern: `^\+?[0-9]{9,15}$`   | `"+420123456789"`       |
| `address`             | Object | No               | All subfields required        | See below               |
| `address.street`      | String | Yes (if address) | Not blank                     | `"Nová 123"`            |
| `address.city`        | String | Yes (if address) | Not blank                     | `"Praha"`               |
| `address.postalCode`  | String | Yes (if address) | Not blank                     | `"11000"`               |
| `address.country`     | String | Yes (if address) | Not blank, ISO 3166-1 alpha-3 | `"CZE"`                 |
| `dietaryRestrictions` | String | No               | Max 500 characters            | `"Vegetarian, no nuts"` |

---

### Admin-Only Fields

| Field                          | Type   | Required                | Validation                      | Example                         |
|--------------------------------|--------|-------------------------|---------------------------------|---------------------------------|
| `gender`                       | Enum   | No                      | Valid values                    | `"MALE"`, `"FEMALE"`, `"OTHER"` |
| `chipNumber`                   | String | No                      | Numeric only                    | `"12345"`                       |
| `identityCard`                 | Object | No                      | See below                       | See below                       |
| `identityCard.cardNumber`      | String | Yes (if identityCard)   | Max 50 chars, not blank         | `"12345678"`                    |
| `identityCard.validityDate`    | String | Yes (if identityCard)   | ISO-8601 date, not in past      | `"2026-12-31"`                  |
| `medicalCourse`                | Object | No                      | See below                       | See below                       |
| `medicalCourse.completionDate` | String | Yes (if medicalCourse)  | ISO-8601 date                   | `"2024-01-15"`                  |
| `medicalCourse.validityDate`   | String | No                      | ISO-8601 date, after completion | `"2025-01-15"`                  |
| `trainerLicense`               | Object | No                      | See below                       | See below                       |
| `trainerLicense.licenseNumber` | String | Yes (if trainerLicense) | Max 50 chars, not blank         | `"TL-2024-001"`                 |
| `trainerLicense.validityDate`  | String | Yes (if trainerLicense) | ISO-8601 date, not in past      | `"2025-06-30"`                  |
| `drivingLicenseGroup`          | Enum   | No                      | Valid values                    | `"B"`, `"BE"`, `"C"`, etc.      |

---

### Read-Only Fields (Cannot Update)

| Field                | Reason                                             |
|----------------------|----------------------------------------------------|
| `id`                 | Immutable identifier                               |
| `registrationNumber` | Assigned at registration, cannot change            |
| `firstName`          | Not in current implementation (future enhancement) |
| `lastName`           | Not in current implementation (future enhancement) |
| `dateOfBirth`        | Not in current implementation (future enhancement) |
| `nationality`        | Not in current implementation (future enhancement) |
| `active`             | Managed by admin, not via self-edit                |

---

## Best Practices

### 1. Use PATCH for Partial Updates

**Good:**

```http
PATCH /api/members/{{memberId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "email": "newemail@example.com"
}
```

**Avoid:** Sending all fields when only one changes

---

### 2. Handle 409 Conflicts Gracefully

**Client Retry Logic:**

```javascript
async function updateMember(memberId, updates) {
  let retries = 3;
  while (retries > 0) {
    try {
      const response = await fetch(`/api/members/${memberId}`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updates)
      });

      if (response.status === 409) {
        // Conflict - fetch fresh data and retry
        const freshData = await fetchMember(memberId);
        retries--;
        continue; // Retry with fresh data
      }

      return await response.json();
    } catch (error) {
      if (retries === 0) throw error;
      retries--;
    }
  }
}
```

---

### 3. Validate Input Before Sending

**Client-Side Validation:**

```javascript
function validateEmail(email) {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}

function validatePhone(phone) {
  const regex = /^\+?[0-9]{9,15}$/;
  return regex.test(phone);
}

function validateAddress(address) {
  return address.street &&
         address.city &&
         address.postalCode &&
         address.country;
}
```

---

### 4. Check Authorization Before Request

**Check User Permissions:**

```javascript
async function canEditMember(memberId) {
  const userInfo = await getUserInfo();
  const member = await fetchMember(memberId);

  // Self-edit: Check if email matches
  if (userInfo.email === member.email) {
    return { canEdit: true, fields: ['email', 'phone', 'address', 'dietaryRestrictions'] };
  }

  // Admin: Check if has MEMBERS:UPDATE permission
  if (userInfo.authorities.includes('MEMBERS:UPDATE')) {
    return { canEdit: true, fields: ['all'] };
  }

  return { canEdit: false, reason: 'Not authorized' };
}
```

---

## Testing with IntelliJ HTTP Client

### Quick Start

1. Open `update-member.http` in IntelliJ IDEA
2. Set variables in `http-client.env.json`:
    - `{{memberId}}` - UUID of member to update
    - `{{ownMemberId}}` - Your own member ID (for self-edit tests)
    - `{{otherMemberId}}` - Different member ID (for authorization tests)
3. Configure OAuth2 in `http-client.private.env.json`
4. Click "Run" next to each request

### Variable Examples

```json
{
  "memberId": "550e8400-e29b-41d4-a716-446655440000",
  "ownMemberId": "550e8400-e29b-41d4-a716-446655440000",
  "otherMemberId": "660e8400-e29b-41d4-a716-446655440001"
}
```

---

## Summary

This API provides:

- ✅ **Member self-edit**: Update own email, phone, address, dietary restrictions
- ✅ **Admin edit**: Update all fields for any member
- ✅ **PATCH semantics**: Partial updates, bandwidth efficient
- ✅ **Field-based access control**: Role-based filtering
- ✅ **HAL+FORMS**: Hypermedia responses with links
- ✅ **Error handling**: RFC 7807 Problem Details
- ✅ **Optimistic locking**: 409 Conflict on concurrent updates
- ✅ **Comprehensive validation**: Clear error messages

**Status:** Production Ready ✅

**Documentation Date:** January 18, 2026
