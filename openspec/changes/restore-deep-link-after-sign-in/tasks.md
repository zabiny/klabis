# Tasks

## 1. Safe return path (design D3)

- [ ] 1.1 Write failing Vitest tests for a pure `toSafeReturnPath(value: unknown): string` helper (e.g. `frontend/src/api/returnPath.ts`): accepts `/events/42`, `/members?q=x#top`; returns `/` for `undefined`, non-strings, `''`, `events`, `//evil.example`, `/\evil.example`, `https://evil.example/x`, `javascript:alert(1)`, values with control characters, over-long values, `/auth/callback?code=x` and `/login`. Verify they fail.
- [ ] 1.2 Implement the helper (same-origin check via `new URL(value, window.location.origin)`, returns `pathname + search + hash`, `/` on any failure) and verify the 1.1 tests pass.

## 2. Remember and restore the requested page (spec: Return to the Originally Requested Page After Sign-In)

- [ ] 2.1 Add failing tests to `frontend/src/contexts/AuthContext2.test.tsx`: `login()` on `/events/42?x=1#y` calls `signinRedirect` with `state` holding that return path; `login()` on `/auth/callback` passes `/`; `login()` during logout still does not call `signinRedirect`. Verify they fail (except the existing guard test).
- [ ] 2.2 Add failing tests for the callback branch: `signinRedirectCallback` resolving a user with `state.returnTo = '/membership-fee-tiers'` navigates there with `replace`; missing state, `//evil.example` and `/auth/callback` navigate to `/`. Verify they fail.
- [ ] 2.3 Change `login()` to pass `{state: {returnTo: toSafeReturnPath(current location)}}` to `signinRedirect`, and the callback branch to `navigate(toSafeReturnPath(user.state?.returnTo), {replace: true})`. Keep the `logoutInProgress` guard and the StrictMode `initialized` ref unchanged. Verify all `AuthContext2.test.tsx` and `klabisUserManager.test.ts` tests pass.
- [ ] 2.4 Run `npm run lint` and `npm run build` in `frontend/` and verify both succeed (tsc alone misses some CI failures).

## 3. E2E: real deep links instead of the router-push workaround

- [ ] 3.1 Add a step `When I open the application at {text}` to `frontend/e2e/steps/navigation.steps.ts` (`page.goto(path)` then `waitForApp`), and a step `When I sign in as {string} on the login page` to `frontend/e2e/steps/auth.steps.ts` (fills the login form with the role's credentials from `support/roles.ts`). Verify `npx tsc -p tsconfig.e2e.json --noEmit` passes.
- [ ] 3.2 Add `frontend/e2e/features/users.feature` tagged `@spec:users` with the scenarios "Member opens a bookmarked page and signs in on the login form" (no `Given I am logged in`; reuse `Given a published event {text} with open registration exists`, which prepares data via `apiAs("admin")`; a new step `When I open the address of event {text}` resolves the event's SPA path via `apiAs` and calls `page.goto`; sign in on the form; assert the event name is shown) and "Member opens a shared link while still signed in at the login service" (`Given I am logged in as "member"`, open `/membership-fee-tiers`, assert the tier catalog is shown and no login form). Verify both pass with `npm run test:e2e -- --grep "users"` against `./runLocalEnvironment.sh` on :3000.
- [ ] 3.3 Replace `When I open the application` + `I navigate in the application to "/membership-fee-tiers"` in `membership-fees.feature` with `When I open the application at "/membership-fee-tiers"`, and the campaign scenario in `membership-fee-campaign-manual-close.feature` with a `page.goto` based step `When I open the scenario campaign` (replacing `I navigate in the application to the scenario campaign` in `membership-fees.steps.ts`). Verify the affected scenarios pass.
- [ ] 3.4 Delete the `When I navigate in the application to {text}` step and its comment; verify `grep -rn "navigate in the application" frontend/e2e` returns nothing and `bddgen` succeeds.
- [ ] 3.5 Update `frontend/e2e/README.md`: remove the "No deep links" rule, replace the workaround rows in the step vocabulary with the new steps. Verify the README mentions no `pushState` workaround.

## 4. Integration verification

- [ ] 4.1 Run the full frontend Vitest suite via the test-runner and verify it is green.
- [ ] 4.2 Run the whole E2E suite against :3000 and verify it is green.
- [ ] 4.3 Run `publish-frontend-resources`, restart the backend, run the E2E suite with `E2E_BASE_URL=https://localhost:8443` and verify the `users.feature` scenarios pass there too.
- [ ] 4.4 Manual QA on :3000 with Playwright MCP: open `/events/<id>` with a fresh browser (login form) and with only the AS session (no form) and verify the event detail is shown; log out from an event detail 3× per user and verify each time the backend log shows `/connect/logout` and the next sign-in lands on the home page.
