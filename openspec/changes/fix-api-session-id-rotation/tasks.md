## 1. Stop session id rotation on the API chain (backend, one commit)

Test and fix ship together so that every commit stays green (Red-Green inside the group).

- [x] 1.1 Red: add an integration test in `backend/src/test/java/com/klabis/common/security/` (design D2). It gets an AS session + real access token via form login and code exchange, then calls `GET /api` with `Authorization: Bearer` and the same `MockHttpSession`. It asserts `200`, no `Set-Cookie` response header, and an unchanged session id. Run it and confirm it fails on the current configuration.
- [x] 1.2 In the same test class, add a scenario: after the bearer `/api` call, `/oauth2/authorize?prompt=none` with the same session redirects with `code=` (not `error=login_required`).
- [x] 1.3 Add a scenario for `/ical/**` with a valid iCal token and an existing session: the session id is unchanged.
- [x] 1.4 Green: in `WebSecurityCommonConfiguration.defaultSecurityFilterChain`, add `.sessionFixation(fixation -> fixation.none())` next to `SessionCreationPolicy.STATELESS` (design D1). Add a one-line comment with the reason (authentication is per request, never stored in a session, rotation would break the AS session needed for `prompt=none`).
- [x] 1.5 Run the new test class plus `AuthorizationServerPromptNoneTest`, `OidcFlowE2ETest` and `common.security` tests. All pass without modification.
- [x] 1.6 Run the full backend test suite. Only known pre-existing failures may remain.
- [x] 1.7 Manual check: run `./runLocalEnvironment.sh`, log in on `http://localhost:3000`, and load several pages. The backend log shows no `ChangeSessionIdAuthenticationStrategy - Changed session id` lines.

## 2. Re-evaluate the E2E authentication workaround (frontend E2E, one commit)

- [ ] 2.1 In `frontend/e2e/support/fixtures.ts`, change `loginAs` to restore only the stored cookies (no `oidc.user:*` init script), with the backend from group 1 running. Keep the `oidc` entry in the stored role state for the `api` fixture (design D3).
- [ ] 2.2 Run the full E2E suite locally against `http://localhost:3000` at least twice. Cookie-only re-entry must pass in every scenario without the login form appearing.
- [ ] 2.3 If 2.2 is stable, keep the simplification and remove the workaround comment above `loginAs`. If it is not stable, revert 2.1, keep the restore, and rewrite the comment to name the actual remaining reason.

## 3. Documentation (one commit)

- [ ] 3.1 Update the "Authentication" paragraph of the E2E section in `frontend/CLAUDE.md`. Remove the "known backend defect" sentence and describe the mechanism that resulted from group 2.
- [ ] 3.2 If group 2 changed the re-entry mechanism, add a note to the D4 section of `openspec/changes/add-e2e-playwright-tests/design.md` (or its archived copy) that cookie-only re-entry works after `fix-api-session-id-rotation`.
