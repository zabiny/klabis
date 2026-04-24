## 1. Dashboard resource (backend)

- [x] 1.1 Write `@WebMvcTest` slice test for `DashboardController`: authenticated GET `/api/dashboard` returns HAL response with a `self` link; unauthenticated request is rejected by existing security chain
- [x] 1.2 Implement `DashboardController` in `common.ui` (sibling of `RootController`) with `GET /api/dashboard` returning `EntityModel<DashboardModel>` with a `self` link; use `MediaTypes.HAL_JSON_VALUE` and `HAL_FORMS_JSON_VALUE` like RootController
- [x] 1.3 Refactor: extract `DashboardModel` as a thin marker record if needed to match `RootModel` shape
- [x] 1.4 Verify the endpoint is reachable with the standard `openid` scope; adjust `@SecurityRequirement` to match

## 2. Upcoming registrations link (backend, events module)

- [x] 2.1 Write `@WebMvcTest` slice test: authenticated user with a member profile gets `upcomingRegistrations` link on `/api/dashboard` whose href is `/api/events?registeredBy=me&dateFrom=<today>&sort=eventDate,ASC&size=3`
- [x] 2.2 Write `@WebMvcTest` slice test: authenticated user without a member profile gets `/api/dashboard` with no `upcomingRegistrations` link
- [x] 2.3 Implement `DashboardUpcomingRegistrationsLinkProcessor` in events module (follow `MemberPermissionsLinkProcessor` pattern) that adds the `upcomingRegistrations` link when the current authentication carries a `memberIdUuid` claim
- [x] 2.4 Refactor: extract the `dateFrom` "today" resolution to a small helper/clock abstraction already used elsewhere if present; otherwise inline with `LocalDate.now(clock)` where `clock` is injected
- [x] 2.5 Run relevant events-module and common-ui slice tests via test-runner agent

## 3. Frontend widget driven by link presence

- [x] 3.1 Write test for `useDashboard` hook: mocks `/api/dashboard` response and returns the HAL resource; handles loading and error states
- [x] 3.2 Implement `useDashboard` hook (TanStack Query) fetching `/api/dashboard` via `authorizedFetch`
- [x] 3.3 Write test for `useMyUpcomingRegistrations(href)` hook: enabled only when href is present; follows the provided href and returns the events page
- [x] 3.4 Implement `useMyUpcomingRegistrations(href)` hook
- [x] 3.5 Write `UserDashboard` tests: widget hidden when dashboard response has no `upcomingRegistrations` link; widget shown with three event rows when link present and data returned; empty-state CTA shown when link present but zero events
- [x] 3.6 Replace `mockMyEvents` consumption in `UserDashboard` with link-driven rendering: use `getLinkHref(dashboard, 'upcomingRegistrations')` to decide whether to render the widget
- [x] 3.7 Wire widget rows to render event name, location, and formatted date; clicking a row navigates to the event detail page
- [x] 3.8 Wire "Zobrazit všechny" to route `/events?registeredBy=me&time=budouci` (frontend uses Czech `time` values; design.md's `time=future` was a drafting error)
- [x] 3.9 Add empty-state copy and secondary CTA ("Prohlédnout nadcházející akce klubu") linking to `/events?time=budouci`
- [x] 3.10 Add any new labels to `src/localization/labels.ts` (empty-state secondary CTA, widget title if not already present); reuse `labels.dashboard.noUpcomingEvents`

## 4. Cleanup

- [x] 4.1 Remove `mockMyEvents` (and `MyEvent` type if unused elsewhere) from `frontend/src/pages/dashboard/mockDashboardData.ts`
- [x] 4.2 Verify admin dashboard mocks remain untouched
- [x] 4.3 Run frontend test suite via test-runner agent; fix regressions
- [x] 4.4 Regenerate OpenAPI TypeScript types if backend OpenAPI doc changed (`npm run openapi`)

## 5. Manual verification

- [x] 5.1 Start local environment via `runLocalEnvironment.sh`
- [x] 5.2 Log in as `ZBM9500` (member user), confirm widget appears on home dashboard with real registrations (or empty state if none) — verified: 3 rows (Žďárský pohár, Jihlavský noční sprint, Pohár OOB – 2. kolo)
- [x] 5.3 Click "Zobrazit všechny" — verify navigation lands on events list with "Moje přihlášky" active and time window "Budoucí" — verified
- [x] 5.4 Log in as `ZBM9000` (admin) — widget renders for admin too because admin has a member profile (spec rule is member-profile presence, not admin/non-admin); admin navigation shows expected Administrace section
- [ ] 5.5 Log in as a user with no member profile (if available) — NOT VERIFIED (no such test user exists locally); backend test covers this path

## 6. GitHub issue

- [x] 6.1 Add label `BackendCompleted` to GitHub issue #88 after backend tasks (1.* and 2.*) pass
- [x] 6.2 Post a short implementation summary comment on issue #88 linking the commit range once merged
