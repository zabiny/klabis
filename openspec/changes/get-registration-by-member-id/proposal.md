## Why

`GET /api/events/{eventId}/registrations/me` returns the acting user's own registration, but there is no stable, addressable URL for a specific member's registration — the URL changes based on who is calling it. This makes it impossible to link to a registration by member identity, breaks REST content-addressability, and prevents event coordinators from viewing or acting on a specific member's registration without that member being the caller.

The `PUT /api/events/{eventId}/registrations/{memberId}` edit endpoint (added in gh-92) already uses `{memberId}` as the identifier; the corresponding GET should use the same URL pattern for consistency.

## What Changes

- **NEW** `GET /api/events/{eventId}/registrations/{memberId}` — returns the full registration for the given member (including SI card number). Accessible by:
  - The member themselves (same behavior as current `/me`)
  - Event coordinators and club managers (admin view)
- **BREAKING** `GET /api/events/{eventId}/registrations/me` — deprecated and removed; all callers migrate to `GET /api/events/{eventId}/registrations/{memberId}` using the acting member's own ID.
- HAL-FORMS affordances currently pointing to `/me` are updated to point to `/{memberId}`.
- Frontend navigation and queries that use `/me` are updated to use `/{memberId}`.

## Capabilities

### New Capabilities

<!-- None — this is a migration + extension of an existing capability, not a new top-level capability. -->

### Modified Capabilities

- `event-registrations`: Replace `View Own Registration` (/me) with `View Registration by Member ID` (/{memberId}). Add authorization rule: own registration visible to self; any registration visible to event coordinator / manager. Mark `/me` as removed (**BREAKING**).

## Impact

**Affected specs:**
- `openspec/specs/event-registrations/spec.md` — update `View Own Registration` requirement: rename to `View Registration`, change URL pattern, add authorization scenarios for self vs. coordinator access.

**Affected backend code:**
- `EventRegistrationController` — rename `getOwnRegistration()` to `getRegistration()`, change path from `/me` to `/{memberId}`, add `@OwnerVisible` OR `@HasAuthority(EVENTS_MANAGE)` for combined authorization.
- `EventDetailsPostprocessor` / `EventSummaryPostprocessor` — update self link on own registration from `/me` to `/{memberId}`.
- `EventRegistrationController.addLinksForOwnRegistration()` — update self link URL.
- `EventRegistrationE2ETest` — update all `/me` references.

**Affected frontend code:**
- Any query using `/registrations/me` URL — switch to `/registrations/{memberId}` using the current user's member ID from JWT.
- HAL link navigation — follow updated self link from API response (no hardcoded URL if HAL navigation is used correctly).

**APIs:** **BREAKING** — `/me` endpoint removed. Clients must migrate to `/{memberId}`.

**Dependencies:** Requires acting member's UUID to be available in frontend context (already present as JWT claim `memberIdUuid`).
