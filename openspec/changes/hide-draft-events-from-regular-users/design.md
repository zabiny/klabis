## Context

Currently, all authenticated users with `EVENTS:READ` authority can see all events regardless of status, including DRAFT events. DRAFT events represent events still being planned and should only be visible to users who can manage them (`EVENTS:MANAGE`). The `EVENTS:READ` authority is granted to every standard member by default.

The filtering needs to happen at the service layer — the repository remains unaware of authorization concerns, keeping the domain clean.

## Goals / Non-Goals

**Goals:**
- DRAFT events are invisible to users without `EVENTS:MANAGE` in both list and detail endpoints
- Filtering is enforced at the backend — frontend simply consumes filtered results
- No changes to the authority model or existing permissions

**Non-Goals:**
- Changing how DRAFT events work (lifecycle, creation, publishing)
- Adding new API endpoints or query parameters
- Modifying the frontend (it will naturally stop showing DRAFT events once the API filters them)

## Decisions

### Decision 1: Add status filter to service list method, controller decides the filter

**Choice:** Extend the service's list method to accept an optional status exclusion filter. The controller checks the current user's authority and passes the appropriate filter — excluding DRAFT for users without `EVENTS:MANAGE`, no exclusion for managers.

**Why:** This keeps the service layer authorization-unaware. The service simply accepts a data filter; the controller (which already has access to the security context) makes the authorization decision. This follows the existing pattern where controllers handle permission checks via `@HasAuthority` and pass clean parameters to the service.

**Alternative considered:** Authority-aware filtering inside the service — rejected because it couples authorization logic to the service layer, which should remain focused on business operations.

### Decision 2: Return 404 (not 403) for DRAFT event detail access

**Choice:** When a user without `EVENTS:MANAGE` requests a DRAFT event by ID, the controller checks the event's status and returns 404 Not Found instead of 403 Forbidden.

**Why:** Returning 403 would reveal that the event exists. 404 treats DRAFT events as if they don't exist for unauthorized users, which is the intended behavior — these events are not yet "public" within the system.

### Decision 3: Controller checks authority via SecurityContext

**Choice:** The controller uses `SecurityContextHolder` to check if the current user has `EVENTS:MANAGE` authority, then decides which service method/filter to use.

**Why:** This is consistent with how authorization is already handled in controllers (via `@HasAuthority`). The controller is the natural place for this decision — it sits between the HTTP layer and the service layer.

## Risks / Trade-offs

- **[Pagination count changes]** → Users without `EVENTS:MANAGE` will see fewer total results when DRAFT events exist. This is expected and correct behavior — page metadata will reflect the filtered count.
- **[Controller responsibility]** → Controllers gain filtering logic beyond simple delegation. Mitigated by keeping it to a single authority check that selects the appropriate service call.
