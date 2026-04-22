## Context

Today `GET /api/events/{eventId}/registrations/me` returns the calling member's own registration. The URL is not stable — it depends on who is authenticated, not on which resource is being addressed. This creates two concrete problems:

1. The `PUT /api/events/{eventId}/registrations/{memberId}` edit endpoint (added in gh-92) already addresses registrations by member ID. The matching GET must use the same URL shape to make the resource content-addressable. Currently the self link on the edit affordance points to `/me`, which is technically correct but breaks REST's uniform interface principle.

2. Event coordinators and managers have no way to view a specific member's registration (including their SI card number) without impersonating that member. There is no admin read path for individual registrations today.

Current endpoint: `GET /api/events/{eventId}/registrations/me` — accessible only to the acting member.  
New endpoint: `GET /api/events/{eventId}/registrations/{memberId}` — accessible to the member themselves OR to a user with `EVENTS:MANAGE` authority.

## Goals / Non-Goals

**Goals:**
- Replace `/me` with `/{memberId}` as the canonical URL for a single registration resource.
- Allow event coordinators (`EVENTS:MANAGE`) to view any registration via the new endpoint.
- Update all internal self links, affordances, and frontend queries to use the new URL.
- Remove the `/me` endpoint entirely (no deprecation period — internal SPA only, no external API clients).

**Non-Goals:**
- Admin ability to edit or delete another member's registration (tracked separately in #32/#33).
- Bulk registration export.
- Changing what data is returned — the response shape (`OwnRegistrationDto` / `RegistrationDto`) stays the same.

## Decisions

### D1 — Authorization: `@OwnerVisible` OR `@HasAuthority(EVENTS_MANAGE)`

The new endpoint must be accessible by two distinct principals:
- The member identified by `{memberId}` (owner access).
- Any user with `EVENTS:MANAGE` authority (coordinator access).

The existing `@OwnerVisible` annotation enforces owner-only access and returns 403 otherwise. `@HasAuthority` enforces a single authority gate. The `common.security.fieldsecurity` framework already supports OR-combining these via stacking both annotations — when either condition is satisfied the request proceeds.

Implementation shape:
```
@GetMapping("/{memberId}")
@OwnerVisible
@HasAuthority(Authority.EVENTS_MANAGE)
public ResponseEntity<EntityModel<OwnRegistrationDto>> getRegistration(
    @OwnerId @PathVariable UUID memberId,
    @PathVariable UUID eventId) { … }
```

The framework combines `@OwnerVisible` and `@HasAuthority` with OR semantics — the request proceeds when either the caller is the owner, or the caller holds the specified authority. No custom authorization expression is needed.

**Alternatives considered:**
- *`@PreAuthorize` SpEL expression.* Rejected — verbose and bypasses the project's typed authority pattern.
- *Two separate endpoints (`/me` stays, `/{memberId}` added for admin only).* Rejected — having two URLs for the same resource violates content-addressability and doubles the maintenance surface.

### D2 — Remove `/me` entirely, no redirect

`/me` is consumed exclusively by the Klabis SPA. There are no external API clients. A redirect from `/me` → `/{memberId}` would require the server to know the acting member's ID at redirect time (already available from JWT), but it introduces an extra round-trip and keeps dead code in the controller. A clean removal is simpler.

Frontend migration path: replace the hardcoded `/me` path with `/{memberIdUuid}` derived from the JWT claim already in scope in every authenticated query.

**Alternative considered:** Keep `/me` as a permanent redirect (301). Rejected — adds complexity for zero benefit given single-client SPA.

### D3 — Response DTO: rename `OwnRegistrationDto` → `RegistrationDto` (full view)

`OwnRegistrationDto` already includes SI card number (the sensitive field). Both the member and a coordinator with `EVENTS:MANAGE` should see the full record including SI card — coordinators legitimately need SI card numbers to verify participants at events. The "Own" prefix is no longer accurate once coordinators can also access the endpoint, so the DTO is renamed to `RegistrationDto`.

Note: the existing public `RegistrationDto` (used in the registrations list, without SI card) must be renamed to avoid the collision — e.g. `RegistrationSummaryDto`.

Field-level visibility (`@OwnerVisible` on DTO fields) is not used here because the coordinator path is an explicit authority grant, not an implicit ownership inference.

### D4 — `getEvent(…, hasEventsManageAuthority())` call in new endpoint

The existing `getOwnRegistration` calls `eventManagementService.getEvent(eventId, true)` — the boolean flag controls whether the full event (including unpublished details) is loaded based on `EVENTS:MANAGE`. The new endpoint keeps this behavior unchanged: coordinators loading the event still get the privileged view; members get the standard view.

## Risks / Trade-offs

- **Frontend must derive `memberIdUuid` from JWT for own-registration queries.** The claim `memberIdUuid` is already present in every access token (set by `KlabisAuthorizationServerCustomizer`). The frontend already reads it for other purposes. Risk: low.
- **Self links in existing cached HAL responses point to `/me`.** Since H2 resets on restart in dev/test and there is no production environment yet, stale cache is not a concern. If a client has a cached `/me` link, it will receive 404 after deployment. Acceptable for an internal SPA.
- **`@OwnerVisible` + `@HasAuthority` OR-combining.** This combination must be verified against the current `HasAuthorityMethodInterceptor` implementation to confirm it correctly short-circuits on authority when the caller is not the owner. If the interceptor applies AND semantics, the implementation must be adjusted. → Verify in tests before merging.

## Migration Plan

1. Add `GET /{memberId}` with combined authorization.
2. Update self links and affordances in `EventRegistrationController`, `EventDetailsPostprocessor`, `EventSummaryPostprocessor` to reference `/{memberId}` instead of `/me`.
3. Update `EventController.buildRegistrationDtos` and `getEvent` response (if `/me` link is embedded).
4. Update frontend: replace every `…/registrations/me` query URL with `…/registrations/{memberIdUuid}`.
5. Remove `getOwnRegistration` (`/me`) endpoint and associated `addLinksForOwnRegistration` helper.
6. Update all E2E and controller tests.

Rollback: revert the commit — no data migration, no schema change.

## Open Questions

1. **Should the coordinator view of a registration also expose edit/unregister affordances?** Out of scope per proposal (admin override tracked in #32/#33), but worth confirming the affordance logic does not accidentally expose mutation templates to coordinators.
