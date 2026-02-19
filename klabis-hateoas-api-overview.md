# Klabis HATEOAS API Overview

Complete overview of all Klabis API endpoints with their HATEOAS link relations and authorization requirements.

**Generated:** 2026-02-19
**API Base URL:** `https://localhost:8443/api`
**Media Type:** `application/hal+forms+json`

---

## Table of Contents

- [API Root](#api-root)
- [Members API](#members-api)
- [Events API](#events-api)
- [Event Registrations API](#event-registrations-api)
- [Calendar API](#calendar-api)
- [User Permissions API](#user-permissions-api)
- [Password Setup API](#password-setup-api)
- [Authorization Reference](#authorization-reference)

---

## API Root

### GET `/api`

**Authorization:** `openid` scope required

**Description:** Entry point for HAL+JSON navigation. Returns root links to all major API resources.

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `members` | GET `/api/members` | `MEMBERS:READ` |
| `events` | GET `/api/events` | `EVENTS:READ` |
| `admin` | `/sandplace` | Only for authenticated admin user |

---

## Members API

Base path: `/api/members`

### GET `/api/members`

**Authorization:** `@HasAuthority(MEMBERS_READ)`

**Description:** Lists members with pagination and sorting.

**Request parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `sort` (default: lastName,asc)
- Allowed sort fields: firstName, lastName, registrationNumber

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/members` | `MEMBERS:READ` |
| `self` (affordance) | POST `/api/members` | `MEMBERS:CREATE` |
| `first` | First page of members | `MEMBERS:READ` |
| `last` | Last page of members | `MEMBERS:READ` |
| `next` | Next page of members | `MEMBERS:READ` |
| `prev` | Previous page of members | `MEMBERS:READ` |

**Item links (for each member in collection):**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/members/{id}` | `MEMBERS:READ` |

---

### GET `/api/members/{id}`

**Authorization:** `@HasAuthority(MEMBERS_READ)`

**Description:** Retrieves detailed member information by ID.

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/members/{id}` | `MEMBERS:READ` |
| `self` (affordance) | PATCH `/api/members/{id}` | `isAuthenticated()` + field-level authorization |
| `collection` | GET `/api/members` | `MEMBERS:READ` |
| `permissions` | GET `/api/users/{id}/permissions` | `MEMBERS:PERMISSIONS` (conditional - only if user has this authority) |

**Notes:**
- `permissions` link is added by `MemberPermissionsLinkProcessor` only if authenticated user has `MEMBERS:PERMISSIONS` authority
- PATCH affordance supports dual authorization:
  - **Self-edit:** Authenticated members can update their own information (limited fields: email, phone, address, dietaryRestrictions)
  - **Admin edit:** Users with `MEMBERS:UPDATE` authority can edit any member (all fields)

---

### PATCH `/api/members/{id}`

**Authorization:** `@PreAuthorize("isAuthenticated()")` with field-level and role-based authorization

**Description:** Updates member information with PATCH semantics (partial update).

**Request body:** `UpdateMemberRequest` (only fields to update should be provided)

**Authorization details:**
- **Self-edit:** Members can update their own data
  - Allowed fields: email, phone, address, dietaryRestrictions
  - Verified by matching authenticated user's ID with member ID
- **Admin edit:** Users with `MEMBERS:UPDATE` authority
  - Can update all fields including: firstName, lastName, dateOfBirth, gender, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/members/{id}` | `MEMBERS:READ` |
| `self` (affordance) | PATCH `/api/members/{id}` | `isAuthenticated()` + field-level authorization |
| `collection` | GET `/api/members` | `MEMBERS:READ` |

---

### POST `/api/members`

**Authorization:** `@HasAuthority(MEMBERS_CREATE)`

**Description:** Registers a new member with personal information, contact details, and optional guardian information.

**Request body:** `RegisterMemberRequest`

**Response:** 201 Created with Location header

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/members/{id}` | `MEMBERS:READ` |

---

## Events API

Base path: `/api/events`

### GET `/api/events`

**Authorization:** `@HasAuthority(EVENTS_READ)`

**Description:** Lists events with pagination, sorting, and optional status filtering.

**Request parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `sort` (default: eventDate,desc)
- Allowed sort fields: id, name, eventDate, location, organizer, status
- `status` (optional): Filter by EventStatus (DRAFT, ACTIVE, FINISHED, CANCELLED)

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/events` | `EVENTS:READ` |
| `self` (affordance) | POST `/api/events` | `EVENTS:MANAGE` |
| `first` | First page of events | `EVENTS:READ` |
| `last` | Last page of events | `EVENTS:READ` |
| `next` | Next page of events | `EVENTS:READ` |
| `prev` | Previous page of events | `EVENTS:READ` |

**Item links (for each event in collection):**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/events/{id}` | `EVENTS:READ` |

---

### GET `/api/events/{id}`

**Authorization:** `@HasAuthority(EVENTS_READ)`

**Description:** Retrieves detailed event information by ID.

**Links returned (status-dependent):**

| Link Relation | Target | Authorization | Event Status |
|---------------|--------|---------------|--------------|
| `self` | GET `/api/events/{id}` | `EVENTS:READ` | All |
| `self` (affordance: update) | PATCH `/api/events/{id}` | `EVENTS:MANAGE` | DRAFT, ACTIVE |
| `self` (affordance: publish) | POST `/api/events/{id}/publish` | `EVENTS:MANAGE` | DRAFT |
| `self` (affordance: cancel) | POST `/api/events/{id}/cancel` | `EVENTS:MANAGE` | DRAFT, ACTIVE |
| `self` (affordance: finish) | POST `/api/events/{id}/finish` | `EVENTS:MANAGE` | ACTIVE |
| `collection` | GET `/api/events` | `EVENTS:READ` | All |
| `registrations` | `/api/events/{id}/registrations` | See Event Registrations API | All |

**Notes:**
- Affordances are dynamically added based on event status
- DRAFT events can be updated, published, or cancelled
- ACTIVE events can be updated, cancelled, or finished
- FINISHED and CANCELLED events are read-only (no mutation affordances)

---

### POST `/api/events`

**Authorization:** `@HasAuthority(EVENTS_MANAGE)`

**Description:** Creates a new event in DRAFT status.

**Request body:** `CreateEventCommand`

**Response:** 201 Created with Location header

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/events/{id}` | `EVENTS:READ` |

---

### PATCH `/api/events/{id}`

**Authorization:** `@HasAuthority(EVENTS_MANAGE)`

**Description:** Updates event information. Only allowed for DRAFT and ACTIVE events.

**Request body:** `UpdateEventCommand`

**Links returned:** Same as GET `/api/events/{id}`

---

### POST `/api/events/{id}/publish`

**Authorization:** `@HasAuthority(EVENTS_MANAGE)`

**Description:** Transitions event from DRAFT to ACTIVE status.

**Links returned:** Same as GET `/api/events/{id}` (with updated affordances)

---

### POST `/api/events/{id}/cancel`

**Authorization:** `@HasAuthority(EVENTS_MANAGE)`

**Description:** Transitions event to CANCELLED status (from DRAFT or ACTIVE).

**Links returned:** Same as GET `/api/events/{id}` (read-only after cancellation)

---

### POST `/api/events/{id}/finish`

**Authorization:** `@HasAuthority(EVENTS_MANAGE)`

**Description:** Transitions event from ACTIVE to FINISHED status.

**Links returned:** Same as GET `/api/events/{id}` (read-only after finishing)

---

## Event Registrations API

Base path: `/api/events/{eventId}/registrations`

### GET `/api/events/{eventId}/registrations`

**Authorization:** None (public endpoint)

**Description:** Lists all registrations for an event. SI card numbers are not included for privacy.

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/events/{eventId}/registrations` | None |
| `self` (affordance) | POST `/api/events/{eventId}/registrations` | None (service enforces authentication) |
| `event` | GET `/api/events/{eventId}` | `EVENTS:READ` |

---

### POST `/api/events/{eventId}/registrations`

**Authorization:** None specified on endpoint (service layer enforces authentication)

**Description:** Register the authenticated member for an event with SI card number. Only allowed for ACTIVE events.

**Request body:** `RegisterForEventCommand`

**Response:** 201 Created with Location header

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/events/{eventId}/registrations/me` | None (service enforces authentication) |
| `self` (affordance) | DELETE `/api/events/{eventId}/registrations` | None (service enforces authentication) |
| `event` | GET `/api/events/{eventId}` | `EVENTS:READ` |

---

### GET `/api/events/{eventId}/registrations/me`

**Authorization:** None (service layer enforces authentication)

**Description:** Get the authenticated member's registration details including SI card number.

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/events/{eventId}/registrations/me` | None (service enforces authentication) |
| `self` (affordance) | DELETE `/api/events/{eventId}/registrations` | None (service enforces authentication) |
| `event` | GET `/api/events/{eventId}` | `EVENTS:READ` |

---

### DELETE `/api/events/{eventId}/registrations`

**Authorization:** None (service layer enforces authentication)

**Description:** Unregister the authenticated member from an event. Only allowed before the event date.

**Response:** 204 No Content

---

## Calendar API

Base path: `/api/calendar-items`

### GET `/api/calendar-items`

**Authorization:** None (public endpoint)

**Description:** Lists calendar items with pagination, sorting, and optional date range filtering.

**Request parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `sort` (default: startDate,asc)
- Allowed sort fields: id, name, startDate, endDate
- `startDate` (optional, requires endDate)
- `endDate` (optional, requires startDate)

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/calendar-items` | None |
| `self` (affordance) | POST `/api/calendar-items` | `CALENDAR:MANAGE` |
| `first` | First page of calendar items | None |
| `last` | Last page of calendar items | None |
| `next` | Next page of calendar items | None |
| `prev` | Previous page of calendar items | None |

**Item links (for each calendar item in collection):**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/calendar-items/{id}` | None |

---

### GET `/api/calendar-items/{id}`

**Authorization:** None (public endpoint)

**Description:** Retrieves detailed calendar item information.

**Links returned (type-dependent):**

| Link Relation | Target | Authorization | Calendar Item Type |
|---------------|--------|---------------|-------------------|
| `self` | GET `/api/calendar-items/{id}` | None | All |
| `self` (affordance: update) | PUT `/api/calendar-items/{id}` | `CALENDAR:MANAGE` | Manual only |
| `self` (affordance: delete) | DELETE `/api/calendar-items/{id}` | `CALENDAR:MANAGE` | Manual only |
| `collection` | GET `/api/calendar-items` | None | All |
| `event` | GET `/api/events/{id}` | `EVENTS:READ` | Event-linked only |

**Notes:**
- Manual items (not linked to events) can be updated and deleted
- Event-linked items are read-only and managed automatically
- Event-linked items include a link to the source event

---

### POST `/api/calendar-items`

**Authorization:** `@HasAuthority(CALENDAR_MANAGE)`

**Description:** Creates a new manual calendar item (not linked to an event).

**Request body:** `CreateCalendarItemCommand`

**Response:** 201 Created with Location header

**Links returned:** Same as GET `/api/calendar-items/{id}`

---

### PUT `/api/calendar-items/{id}`

**Authorization:** `@HasAuthority(CALENDAR_MANAGE)`

**Description:** Updates calendar item information. Only allowed for manual items (not event-linked).

**Request body:** `UpdateCalendarItemCommand`

**Links returned:** Same as GET `/api/calendar-items/{id}`

---

### DELETE `/api/calendar-items/{id}`

**Authorization:** `@HasAuthority(CALENDAR_MANAGE)`

**Description:** Deletes a manual calendar item. Only allowed for manual items (not event-linked).

**Response:** 204 No Content

---

## User Permissions API

Base path: `/api/users`

### GET `/api/users/{id}/permissions`

**Authorization:** `@HasAuthority(MEMBERS_PERMISSIONS)`

**Description:** Retrieves user permissions including granted authorities.

**Links returned:**

| Link Relation | Target | Authorization |
|---------------|--------|---------------|
| `self` | GET `/api/users/{id}/permissions` | `MEMBERS:PERMISSIONS` |
| `permissions` (affordance) | PUT `/api/users/{id}/permissions` | `MEMBERS:PERMISSIONS` (conditional) |

**Notes:**
- The `permissions` link with affordance is only added if the authenticated user has `MEMBERS:PERMISSIONS` authority
- This check is performed in the controller method using `hasMembersPermissionsAuthority()`

---

### PUT `/api/users/{id}/permissions`

**Authorization:** `@HasAuthority(MEMBERS_PERMISSIONS)`

**Description:** Updates user permissions.

**Request body:** `UpdatePermissionsRequest` with set of Authority values

**Response:** 200 OK with updated permissions

**Links returned:** Same as GET `/api/users/{id}/permissions`

---

## Password Setup API

Base path: `/api/auth/password-setup`

### GET `/api/auth/password-setup/validate`

**Authorization:** None (public endpoint)

**Description:** Validates a password setup token before showing the password setup form.

**Request parameters:**
- `token` (required): The plain text token from the email link

**Response:** `ValidateTokenResponse` with masked email and expiration time

**No HATEOAS links** (simple validation endpoint)

---

### POST `/api/auth/password-setup/complete`

**Authorization:** None (public endpoint)

**Description:** Completes the password setup flow by setting the user's password and activating the account.

**Request body:** `SetPasswordRequest`
- `token`: The plain text token
- `password`: New password
- `passwordConfirmation`: Password confirmation

**Response:** `PasswordSetupResponse` with registration number

**No HATEOAS links** (terminal endpoint in setup flow)

---

### POST `/api/auth/password-setup/request`

**Authorization:** None (public endpoint)

**Description:** Requests a new password setup token if the previous one expired. Rate limited to 3 requests per hour per registration number.

**Request body:** `TokenRequestRequest`
- `registrationNumber`: Member's registration number
- `email`: Member's email address

**Response:** `TokenRequestResponse` with success message

**No HATEOAS links** (utility endpoint)

---

## Authorization Reference

### Authority Constants

All authorities are defined in `com.klabis.users.Authority` enum:

| Authority | Value | Scope | Description |
|-----------|-------|-------|-------------|
| `CALENDAR_MANAGE` | `CALENDAR:MANAGE` | CONTEXT_SPECIFIC | Create, update, delete manual calendar items |
| `MEMBERS_CREATE` | `MEMBERS:CREATE` | CONTEXT_SPECIFIC | Register new members |
| `MEMBERS_READ` | `MEMBERS:READ` | CONTEXT_SPECIFIC | View member information and lists |
| `MEMBERS_UPDATE` | `MEMBERS:UPDATE` | CONTEXT_SPECIFIC | Update member information (admin edit all fields, self edit limited fields) |
| `MEMBERS_DELETE` | `MEMBERS:DELETE` | CONTEXT_SPECIFIC | Delete members (not currently used in API) |
| `MEMBERS_PERMISSIONS` | `MEMBERS:PERMISSIONS` | GLOBAL | Manage user permissions |
| `EVENTS_READ` | `EVENTS:READ` | GLOBAL | View event information and lists |
| `EVENTS_MANAGE` | `EVENTS:MANAGE` | CONTEXT_SPECIFIC | Create, update, publish, cancel, finish events |

### OAuth2 Scopes

| Scope | Used By | Description |
|-------|---------|-------------|
| `CALENDAR` | Calendar API | Calendar access scope |
| `MEMBERS` | Members API, Permissions API | Member management scope |
| `EVENTS` | Events API, Event Registrations API | Event management scope |
| `openid` | Root API | OpenID Connect scope for API root |

### Authorization Annotations

Klabis uses custom `@HasAuthority` annotation for method-level security:

```java
@HasAuthority(Authority.MEMBERS_READ)  // Single authority
@PreAuthorize("isAuthenticated()")     // Standard Spring Security expression
```

The `@HasAuthority` annotation is processed by `HasAuthorityAspect` which checks if the authenticated user has the required authority before allowing method execution.

### Standard User Authorities

Standard users (non-admin) receive these authorities by default:
- `MEMBERS_READ`
- `EVENTS_READ`

### Conditional Links

Some HATEOAS links are conditionally added based on user permissions:

1. **Member permissions link** (`MemberPermissionsLinkProcessor`)
   - Adds `permissions` link to member detail responses
   - Only if authenticated user has `MEMBERS:PERMISSIONS` authority

2. **Event status affordances** (`EventController.addLinksForEvent()`)
   - Different affordances based on event status (DRAFT, ACTIVE, FINISHED, CANCELLED)
   - Dynamically shows available actions

3. **Calendar item type affordances** (`CalendarController.addLinksForCalendarItem()`)
   - Manual items: include update/delete affordances
   - Event-linked items: read-only, include link to source event

4. **Permissions update affordance** (`PermissionController.getUserPermissions()`)
   - Only adds affordance if user has `MEMBERS:PERMISSIONS` authority

---

## Implementation Notes

### HATEOAS Support Class

All links and affordances are created using `HalFormsSupport.klabisLinkTo()` and `HalFormsSupport.klabisAfford()` methods instead of standard Spring HATEOAS `WebMvcLinkBuilder` methods.

**Non-negotiable rule:** Always use `HalFormsSupport` methods for HATEOAS navigation in controllers.

### Representation Model Processors

Several processors add links to responses:

- `MembersRootPostprocessor` - Adds `members` link to root
- `EventsRootPostprocessor` - Adds `events` link to root
- `MemberPermissionsLinkProcessor` - Conditionally adds `permissions` link to member details

### Link Relations

Klabis uses both standard and custom link relations:

**Standard IANA relations:**
- `self` - The current resource
- `collection` - The collection containing this item
- `first`, `last`, `next`, `prev` - Pagination links

**Custom relations:**
- `members` - Link to members collection
- `events` - Link to events collection
- `permissions` - Link to user permissions
- `registrations` - Link to event registrations
- `event` - Link to parent event
- `admin` - Admin-specific link

### Affordances

Affordances describe possible actions on a resource and are included in HAL+FORMS responses. They use the `_templates` property in HAL+FORMS JSON format.

Example affordance structure:
```json
{
  "_links": {
    "self": {
      "href": "/api/members/123",
      "type": "application/hal+forms+json"
    }
  },
  "_templates": {
    "default": {
      "method": "patch",
      "target": "/api/members/123",
      "properties": [...]
    }
  }
}
```

---

## Testing HATEOAS Links

When testing endpoints with HAL responses:

1. Use `/api` as entry point to discover available resources
2. Follow `self` links to navigate to resource details
3. Check `_templates` or affordances for available actions
4. Verify that authorization requirements are enforced
5. Test conditional links with different user permissions

Example with IntelliJ HTTP files:

```http request
# Get API root
GET {{apiBaseUrl}}/api

# Get members list
GET {{apiBaseUrl}}/api/members
Authorization: Bearer {{$auth.token("AuthorizationCode")}}

# Get specific member
GET {{apiBaseUrl}}/api/members/{{memberId}}
Authorization: Bearer {{$auth.token("AuthorizationCode")}}
```

---

## Summary Table

| API | Endpoints | Public | Auth Required | Admin Required |
|-----|-----------|--------|---------------|----------------|
| Root | 1 | ✓ | - | - |
| Members | 4 | - | 3 (all) | 1 (POST) |
| Events | 6 | - | 2 (GET) | 4 (POST, PATCH, status changes) |
| Event Registrations | 4 | ✓ (GET) | POST, DELETE (service enforces) | - |
| Calendar | 4 | ✓ (GET) | 3 (POST, PUT, DELETE) | - |
| Permissions | 2 | - | 2 (all) | 2 (all) |
| Password Setup | 3 | ✓ (all) | - | - |

**Total:** 24 endpoints across 7 APIs

---

## Document Maintenance

This document should be updated when:
- New API endpoints are added
- Authorization requirements change
- New HATEOAS links or affordances are added
- New authorities are defined
- Representation model processors are added/modified

**Automation recommendation:** Consider generating this document from code annotations to ensure it stays synchronized with the implementation.
