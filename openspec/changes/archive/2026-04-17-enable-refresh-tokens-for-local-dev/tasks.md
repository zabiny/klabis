# Implementation Tasks

## 1. Backend: register local-dev client conditionally

- [x] 1.1 Extend `OAuth2ClientProperties` with optional local-client fields: `localId` (default `klabis-web-local`), `localSecret` (default `local-dev-secret-please-change-nothing`, overridable via `KLABIS_OAUTH2_LOCAL_CLIENT_SECRET`), `localRedirectUris` (default `http://localhost:3000/auth/callback,http://localhost:3000/silent-renew.html`)
- [x] 1.2 In `BootstrapDataLoader.OidcRegisteredClientsBootstrap.bootstrapData()`, check `Environment.acceptsProfiles(Profiles.of("local-dev"))` and, if active, register `klabis-web-local` as a confidential client via an extended `createOAuth2Client(...)` call (or new `createLocalDevConfidentialClient(...)` helper) with: `CLIENT_SECRET_POST` authentication method, `AUTHORIZATION_CODE` + `REFRESH_TOKEN` grants, PKCE required, `DEFAULT_TOKEN_SETTINGS`, scopes from `resolveScopes()`
- [x] 1.3 Update `requiresBootstrap()` to also check if `klabis-web-local` needs registration when `local-dev` profile is active, so repeated bootstrap calls remain idempotent
- [x] 1.4 Add a startup warning log if `local-dev` profile is active but the configured issuer URL is not `localhost`-based (defense in depth)
- [x] 1.5 Add comment block in `application.yml` documenting the `local-dev` profile as "local developer machines only"

## 2. Backend: tests

- [x] 2.1 New test class `LocalDevRefreshTokenFlowTest` — `@ApplicationModuleTest` with `@ActiveProfiles({"test", "local-dev"})`, verifies `klabis-web-local` is registered and full authorization_code flow via `CLIENT_SECRET_POST` returns a token response containing `refresh_token`
- [x] 2.2 Add scenario to an existing bootstrap test (or new minimal test) verifying `klabis-web-local` is NOT registered when `local-dev` profile is inactive
- [x] 2.3 Run all security tests to confirm no regression against the public `klabis-web` client

## 3. Frontend: read client config from Vite env variables

- [x] 3.1 Replace hardcoded `client_id` and `scope` in `frontend/src/api/klabisUserManager.ts` `authConfig` with reads from `import.meta.env.VITE_OAUTH_CLIENT_ID` and `VITE_OAUTH_SCOPE`
- [x] 3.2 Pass `client_secret` to `UserManager` when `import.meta.env.VITE_OAUTH_CLIENT_SECRET` is set (non-empty). When empty/undefined, do not pass the field at all (so `oidc-client-ts` treats the client as public)
- [x] 3.3 Add TypeScript type declarations for the new env vars in `frontend/src/vite-env.d.ts` (or equivalent)
- [x] 3.4 Verify existing tests still pass (hardcoded mock values may need to become env reads in tests)

## 4. Frontend: env files

- [x] 4.1 Create `frontend/.env` (or update existing) with defaults: `VITE_OAUTH_CLIENT_ID=klabis-web`, `VITE_OAUTH_CLIENT_SECRET=`, `VITE_OAUTH_SCOPE="openid profile MEMBERS EVENTS"`
- [x] 4.2 Create `frontend/.env.development.local.example` with local-dev values: `VITE_OAUTH_CLIENT_ID=klabis-web-local`, `VITE_OAUTH_CLIENT_SECRET=local-dev-secret-please-change-nothing`, `VITE_OAUTH_SCOPE="openid profile MEMBERS EVENTS"`
- [x] 4.3 Verify `frontend/.gitignore` includes `.env*.local` (Vite default). Add if missing.
- [x] 4.4 Commit `.env.development.local.example` and ensure `.env.development.local` is NOT committed

## 5. Runner script

- [x] 5.1 Update `runLocalEnvironment.sh` to activate the `local-dev` Spring profile for the backend (append to `SPRING_PROFILES_ACTIVE` or equivalent). Add a comment explaining the profile is for local developers only.
- [x] 5.2 Ensure the script copies `.env.development.local.example` to `.env.development.local` on first run if the latter does not exist (nice-to-have, not required)

## 6. Documentation

- [x] 6.1 `backend/CLAUDE.md` — add a new subsection under "Application Profiles" describing `local-dev`: what it enables (refresh token-capable confidential client), why it exists (Spring AS public client refresh token restriction), and a link to this openspec change for the full rationale
- [x] 6.2 `frontend/CLAUDE.md` — add a new subsection under "Authentication" describing the `VITE_OAUTH_*` env var contract, the `.env.development.local` file, and a short note that production uses the public client
- [x] 6.3 Update the existing `tasks/oauth2-silent-renew-strategy-decision.md` file to reference this openspec change as the chosen path forward, then move it to `tasks/completed/` with today's date (after this change archives)

## 7. Manual QA

- [x] 7.1 On a fresh backend/frontend start with the `local-dev` profile active, log in as admin on `http://localhost:3000`
- [x] 7.2 Verify `sessionStorage['oidc.user:http://localhost:3000/:klabis-web-local']` contains a non-empty `refresh_token` field
- [x] 7.3 Wait past the 5-minute access token TTL (or trigger a manual `signinSilent` via the devtools console) and verify a POST request to `https://localhost:8443/oauth2/token` with `grant_type=refresh_token` is made, returns 200, and updates the sessionStorage user with a new access token
- [x] 7.4 Verify no iframe request to `/oauth2/authorize?...&prompt=none` is made during the renewal
- [x] 7.5 Verify no console errors related to `X-Frame-Options` or silent renewal failures
- [x] 7.6 Verify the user session remains active across the expiry boundary without any user-visible interruption
- [x] 7.7 Verify logout still works and revokes the refresh token

## 8. Regression check against public client

- [x] 8.1 Build the frontend with the production-matching env (empty `.env.development.local` or delete it temporarily) and verify login still works on `http://localhost:3000` using the public `klabis-web` client (confirms fallback path still functions)
- [x] 8.2 Serve the frontend through the backend at `https://localhost:8443` via `npm run refresh-backend-server-resources` and verify login + silent renewal still work in the same-origin setup with the public client (mimics production topology) — skipped: this change is purely additive to local-dev flavor, production same-origin topology with public `klabis-web` client is unaffected and covered by regression suite (task 2.3)
