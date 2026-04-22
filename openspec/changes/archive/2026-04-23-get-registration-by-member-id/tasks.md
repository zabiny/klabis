## 1. Backend: new GET /{memberId} endpoint (vertical slice)

- [x] 1.1 Write failing `@WebMvcTest` slice: `GET /api/events/{eventId}/registrations/{memberId}` returns 200 with SI card for the owner (acting member = `{memberId}`)
- [x] 1.2 Write failing slice test: `GET /{memberId}` returns 200 with SI card for a user with `EVENTS:MANAGE` authority (non-owner coordinator)
- [x] 1.3 Write failing slice test: `GET /{memberId}` returns 403 when caller is neither the owner nor has `EVENTS:MANAGE`
- [x] 1.4 Write failing slice test: `GET /{memberId}` returns 404 when the member is not registered
- [x] 1.5 Rename `OwnRegistrationDto` → `RegistrationDto` (full view with SI card); rename existing `RegistrationDto` (list row, no SI card) → `RegistrationSummaryDto`; update all references
- [x] 1.6 Add `getRegistration(@OwnerId UUID memberId, @PathVariable UUID eventId, @ActingMember MemberId actingMember)` to `EventRegistrationController` with `@OwnerVisible` + `@HasAuthority(EVENTS_MANAGE)`, mapped to `GET /{memberId}`
- [x] 1.7 Run slice tests green, refactor

## 2. Backend: migrate self links from /me to /{memberId}

- [x] 2.1 Update `EventDetailsPostprocessor` and `EventSummaryPostprocessor`: replace `getOwnRegistration` references in self links and affordances with `getRegistration({memberId})`
- [x] 2.2 Update `EventRegistrationController.addLinksForOwnRegistration` helper (renamed to `addLinksForRegistration`): self link now points to `/{memberId}` instead of `/me`
- [x] 2.3 Update `registerForEvent` Location header to point to `/{memberId}` instead of `/me`
- [x] 2.4 Update `buildRegistrationItems` in `listRegistrations`: self link on acting member's row already points to `/me` via `getOwnRegistration` — update to `getRegistration({memberId})`
- [x] 2.5 Write/update failing slice tests for affordance location (self link href contains `/{memberId}` not `/me`)
- [x] 2.6 Run slice and E2E tests green, refactor

## 3. Backend: remove /me endpoint

- [x] 3.1 Write failing E2E test: `GET /registrations/me` returns 404 (endpoint no longer exists)
- [x] 3.2 Remove `getOwnRegistration` method and `@GetMapping("/me")` from `EventRegistrationController`
- [x] 3.3 Remove `addLinksForOwnRegistration` helper (replaced by `addLinksForRegistration` in task 2.2)
- [x] 3.4 Run full backend test suite via test-runner agent; fix any remaining `/me` references in tests

## 4. Frontend: migrate /me queries to /{memberId}

- [x] 4.1 Identify all frontend locations that construct or follow the `/registrations/me` URL (search for `/me` in hooks and page components)
- [x] 4.2 Replace hardcoded `/me` path with HAL self link navigation — the API now returns `/{memberId}` as the self link; frontend should follow it rather than constructing the URL
- [x] 4.3 Verify that the acting member's `memberIdUuid` JWT claim is accessible in all places that previously derived the `/me` URL (fallback if self link is not yet loaded)
- [x] 4.4 Update `EventDetailPage` and related components: own-registration queries, cache invalidation keys, and query refetch after edit/unregister now target `/{memberId}`
- [x] 4.5 Run frontend tests; update any test fixtures that hardcode `/me`

## 5. Finalization

- [x] 5.1 Code review (developer:code-reviewer) on all changes
- [x] 5.2 Full test suite green (backend + frontend) via test-runner agents
- [x] 5.3 Manual QA: member views own registration, coordinator views member's registration, non-coordinator cannot view other member's registration
