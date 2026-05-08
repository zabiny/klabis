## 1. Hide registration time for non-authorized members (N9)

Reuses the existing `@OwnerVisible` + `@OwnerId` field-level authorization mechanism — no new annotation, no new advice. The DTO marks `registrationTime` with `@OwnerVisible @HasAuthority(EVENTS_REGISTRATIONS)` and a sibling `eventCoordinatorId` field with `@OwnerId`. Existing field-security advice already grants visibility when the current user matches the owner ID or holds the named authority.

- [x] 1.1 Verify existing `@OwnerVisible` + `@OwnerId` semantics support the "owner OR authority" rule used here (read `OwnerVisible.java`, `RequestBodyFieldAuthorizationAdvice.java`, `DefaultOwnershipResolver.java`); if any aspect of the rule isn't covered, document the gap and extend the advice with the minimal addition required (do NOT introduce a new annotation)
- [x] 1.2 Update `EventRegistrationSummaryDto`: add `@OwnerVisible @HasAuthority(EVENTS_REGISTRATIONS)` to `registrationTime`; add new sibling `eventCoordinatorId` annotated with `@OwnerId`, populated by the mapper from the surrounding event's `eventCoordinatorId`
- [x] 1.3 Update the registration list mapper / service to ensure `eventCoordinatorId` is populated on every row of the response
- [x] 1.4 Add integration test on `EventRegistrationController.list`: regular member → response JSON has no `registrationTime`; event coordinator (member matches `eventCoordinatorId`) → includes `registrationTime`; EVENTS:REGISTRATIONS user → includes `registrationTime`
- [x] 1.5 Update frontend registration list table to skip rendering the timestamp column when the field is missing in the HAL+FORMS response
- [ ] 1.6 Browser smoke test (production-like): regular member sees registration list without timestamp column; admin (EVENTS:REGISTRATIONS) sees the column; coordinator of the specific event sees the column for that event

## 2. Sortable headers in registration list (N10)

- [ ] 2.1 Extend the registration list endpoint to accept `sort` query param with values `firstName`, `lastName`, `category`, and (for authorized callers) `registrationTime`
- [ ] 2.2 Server-side: if caller is unauthorized to view registration time, `sort=registrationTime` is silently ignored and default sort is applied. Same fallback for any unknown/unsupported sort field — no 400/403
- [ ] 2.3 Default sort when no `sort` is provided remains `registrationTime ASC` (server-side; preserves first-come-first-served visual order even when the column is hidden)
- [ ] 2.4 Frontend: enable sortable headers on `firstName`, `lastName`, `category` for everyone, and on `registrationTime` only when the column is visible
- [ ] 2.5 Integration tests for each sort param + silent-fallback scenario (unauthorized `sort=registrationTime` and unknown field name → response sorted by default)

## 3. Accommodation list endpoint and UI (N11)

- [ ] 3.1 Backend: add controller endpoint `GET /api/events/{eventId}/accommodation-list` returning JSON (HAL); guard with method-level "owner OR authority" — caller must match `event.eventCoordinatorId` OR have EVENTS:REGISTRATIONS authority. Use the existing `OwnershipResolver` (or `@PreAuthorize` SpEL referencing it) to express the rule; do not introduce a new annotation
- [ ] 3.2 Service layer: assemble `AccommodationListItem` records for each registration by following the registration → member relation; include first name, last name, `identityCard.cardNumber`, `identityCard.validityDate`, `dateOfBirth`, and `address`. Missing nested values (no `identityCard`, no `address`) are returned as `null` — frontend handles fallback text
- [ ] 3.3 Add HAL `accommodation-list` link to event detail response, exposed only to authorized callers (coordinator OR EVENTS:REGISTRATIONS)
- [ ] 3.4 Frontend: add a route (e.g. `/events/:id/accommodation-list`) that fetches the endpoint and renders a print-friendly table; CSS `@media print` for legible printout; "Tisknout" button calls `window.print()`; render "neuvedeno" in cells where backend returned `null`
- [ ] 3.5 Frontend: in event detail action bar, render "Seznam pro ubytování" action only when the `accommodation-list` link is present in the response; clicking navigates to the new route
- [ ] 3.6 Integration tests (backend): authorized caller (coordinator + authority) → 200 JSON with expected items; unauthorized caller → 403; member with empty `identityCard` → corresponding fields are `null` in the response
- [ ] 3.7 Browser smoke test: log in as event coordinator, open accommodation list route, verify printable layout, content, and "neuvedeno" fallback for incomplete records

## 4. End-to-end verification on the deployed environment

- [ ] 4.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 4.2 Browser test: regular member opens an event with registrations — confirms timestamp column is hidden, sort works on remaining columns
- [ ] 4.3 Browser test: admin (EVENTS:REGISTRATIONS) opens the same event — confirms timestamp column visible, sortable
- [ ] 4.4 Browser test: log in as event coordinator (member assigned as `eventCoordinatorId`) — confirms accommodation list affordance is exposed, list contains all registered members with full identity data, printout looks legible

## 5. Documentation

- [ ] 5.1 Update `backend-patterns` skill: extend the `@OwnerVisible` example section with the "event coordinator as owner" use case (sibling `@OwnerId` on `eventCoordinatorId`); no new annotation introduced
- [ ] 5.2 Sync the spec change into `openspec/specs/event-registrations/spec.md` after archiving
