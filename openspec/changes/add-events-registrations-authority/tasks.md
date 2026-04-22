## 1. Authority enum

- [ ] 1.1 Add red test to `AuthorityValidatorTest` asserting `EVENTS:REGISTRATIONS` is a known authority
- [ ] 1.2 Add `EVENTS_REGISTRATIONS("EVENTS:REGISTRATIONS", Scope.CONTEXT_SPECIFIC)` to `Authority` enum
- [ ] 1.3 Change scope of `EVENTS_MANAGE` from `CONTEXT_SPECIFIC` to `GLOBAL`
- [ ] 1.4 Verify `EVENTS_REGISTRATIONS` is NOT added to `getStandardUserAuthorities()`
- [ ] 1.5 Update `JwtParamsTest` and any other test enumerating valid authorities to include `EVENTS:REGISTRATIONS`
- [ ] 1.6 Run affected unit tests — all green

## 2. Backend — authorization of other members' registrations (vertical slice)

- [ ] 2.1 Add red test to `EventRegistrationControllerTest`: user with `EVENTS:REGISTRATIONS` can `GET /api/events/{eventId}/registrations/{memberId}` for another member and receives SI card number
- [ ] 2.2 Add red test: user with only `EVENTS:MANAGE` (no `EVENTS:REGISTRATIONS`) receives 403 on the same endpoint
- [ ] 2.3 Change `@HasAuthority(EVENTS_MANAGE)` to `@HasAuthority(EVENTS_REGISTRATIONS)` on `EventRegistrationController.getRegistration`
- [ ] 2.4 Update OpenAPI description strings on `getRegistration` to reference `EVENTS:REGISTRATIONS`
- [ ] 2.5 Run affected tests — all green

## 3. Backend — editing another member's registration (vertical slice)

- [ ] 3.1 Add red test: user with `EVENTS:REGISTRATIONS` can `PUT /api/events/{eventId}/registrations/{memberId}` for another member and the registration is updated
- [ ] 3.2 Add red test: user without `EVENTS:REGISTRATIONS` (and not the owner) receives 403 on the same endpoint
- [ ] 3.3 Add red test: member without any special authority can still `PUT` their own registration (owner flow preserved)
- [ ] 3.4 Add `@HasAuthority(EVENTS_REGISTRATIONS)` to `EventRegistrationController.editRegistration` (alongside existing `@OwnerVisible`)
- [ ] 3.5 Run affected tests — all green

## 4. Backend — affordances in registrations list (vertical slice)

- [ ] 4.1 Add red test: holder of `EVENTS:REGISTRATIONS` viewing the registrations list of an event with open window sees `editRegistration` affordance on EVERY row
- [ ] 4.2 Add red test: regular member viewing the same list sees `editRegistration` affordance ONLY on their own row
- [ ] 4.3 Add red test: when registrations are closed, no `editRegistration` affordance is emitted for any row (for anyone)
- [ ] 4.4 In `EventRegistrationController.buildRegistrationItems` remove the `actingMember.equals(registration.memberId())` branch; attach the `editRegistration` affordance to every row when `event.areRegistrationsOpen()` is true, relying on `klabisAfford` + annotations to filter per user
- [ ] 4.5 Emit a self-link on every row using `klabisLinkTo(...getRegistration(...))` so holders of `EVENTS:REGISTRATIONS` receive it too (framework filters for regular members automatically)
- [ ] 4.6 Run affected tests — all green

## 5. Backend — bootstrap and documentation

- [ ] 5.1 Update `BootstrapDataLoader` admin user to include `EVENTS:REGISTRATIONS` so local dev workflows keep working
- [ ] 5.2 Update `EventSummaryDto` docstrings only if they reference the old behaviour; otherwise leave as-is (status column still guarded by `EVENTS:MANAGE`)
- [ ] 5.3 Run full backend test suite via test-runner agent — all green

## 6. Frontend — OpenAPI regeneration

- [ ] 6.1 Rebuild backend and regenerate `frontend/src/api/klabisApi.d.ts` via `npm run openapi`
- [ ] 6.2 Verify the `authorities` enum in `MemberPermissionsDto` now includes `EVENTS:REGISTRATIONS`

## 7. Frontend — permissions dialog

- [ ] 7.1 Add failing test to `PermissionsDialog.test.tsx` asserting the `EVENTS:REGISTRATIONS` toggle renders with label "Správa přihlášek" and its description
- [ ] 7.2 Add `'EVENTS:REGISTRATIONS'` entry to `PERMISSION_COLORS` in `PermissionsDialog.tsx` (distinct color, e.g. purple)
- [ ] 7.3 Add `'EVENTS:REGISTRATIONS': { label: 'Správa přihlášek', description: 'Editace přihlášek ostatních členů na akce' }` to `labels.permissions` in `labels.ts`
- [ ] 7.4 Update `labels.test.ts` if it snapshots the permissions list
- [ ] 7.5 Run affected frontend tests — all green

## 8. Frontend — stay on event page after registration

- [ ] 8.1 Update existing `EventDetailPage.test.tsx` test that asserts redirect after successful `registerForEvent`: expect user to stay on event detail page and the registration list to reflect the new registration
- [ ] 8.2 Remove the code in `EventDetailPage.tsx` (or `EventsPage.tsx` if that is where `registerForEvent` is wired) that follows the `Location` header; invalidate the event-detail and registration-list queries instead
- [ ] 8.3 Run affected frontend tests — all green

## 9. Frontend — edit button visible for EVENTS:REGISTRATIONS holders in list

- [ ] 9.1 Add test to `EventDetailPage.test.tsx`: given every row of the registrations list contains an `editRegistration` template, the edit button renders on every row
- [ ] 9.2 Verify no code change is needed in `EventDetailPage.tsx` beyond existing template-driven rendering (button already conditional on `_templates.editRegistration`); fix if a defensive "own row only" check is found
- [ ] 9.3 Run affected frontend tests — all green

## 10. Verification

- [ ] 10.1 Run full backend + frontend test suites via test-runner agent — all green
- [ ] 10.2 Run `openspec validate add-events-registrations-authority --strict`
- [ ] 10.3 Manual QA via Playwright: register as admin, toggle `EVENTS:REGISTRATIONS` for a regular member, log in as that member, edit another member's registration; log in as a member without the authority and confirm the edit button is hidden on other rows
- [ ] 10.4 Manual QA: register for an event and confirm the browser stays on the event detail page and the registration list reflects the new registration
