## Why

Silent token renewal in the SPA does not work. After the 5-minute access token TTL expires, the user on `http://localhost:3000` (Vite dev server) is forced to re-authenticate. A previous bug fix (`tasks/completed/2026-04-09_21-30-00_oauth2-silent-renew-prompt-none-handling.md`) removed the acute brokenness — the auth server no longer redirects `prompt=none` iframe requests to `/login`, so the app no longer crashes into an `X-Frame-Options` error. But the renewal itself still does not happen, and developers have to log in every five minutes during active work.

The reason silent renewal fails is architectural, not a bug:

- The SPA is registered as a **public OAuth2 client** (`klabis-web`, `ClientAuthenticationMethod.NONE` + PKCE) — correct per OAuth 2.1 BCP for Browser-Based Apps.
- Spring Authorization Server **hardcodes** (in `OAuth2RefreshTokenGenerator.isPublicClientForAuthorizationCodeGrant`) a refusal to issue refresh tokens to public clients on the `authorization_code` grant. This is the OAuth 2.1 default because bearer refresh tokens for public clients are exfiltratable from browser storage, and the mitigation is **sender-constrained tokens via DPoP** — which Klabis does not implement.
- Without a refresh token, `oidc-client-ts` falls back to iframe-based silent renew, which cannot carry the auth-server session cookie across origins (`:3000` parent → `:8443` iframe), so the iframe sees an unauthenticated session and returns `login_required`.

We evaluated two ways forward:

1. **Custom Spring AS component + DPoP** — override the default `OAuth2TokenGenerator` to issue refresh tokens for public clients, paired with full DPoP implementation (client key-pair, request proof signing, resource server validation on every API endpoint). This is the "production-grade" answer but is a multi-day implementation touching backend, frontend, and every resource server controller, with a security-critical custom component to maintain across Spring upgrades.
2. **Profile-based confidential client for local dev** — keep the production `klabis-web` public PKCE client untouched, and add a **second** registered client (`klabis-web-local`) that exists only under a new Spring profile (`local-dev`). The local client uses `CLIENT_SECRET_POST` authentication, which makes Spring AS willing to issue refresh tokens for the authorization_code grant through its default path — no custom code, no DPoP. The Vite dev server configuration uses this local client; all deployed environments continue to use the public PKCE client.

We choose **Option 2**. The reasoning — along with the conditions under which Option 1 would become necessary — is captured in `design.md`. In short: Option 2 is hours of work, zero custom security code, zero risk to production's security posture; Option 1 is days of work, a custom security component we'd own forever, and most of the benefit (production-grade refresh tokens) is irrelevant until Klabis actually has a production deployment with real browser-based users.

## What Changes

- **New Spring profile `local-dev`** that, when active, registers a second OAuth2 client `klabis-web-local` alongside the existing `klabis-web`.
- **`klabis-web-local` is a confidential client** (`ClientAuthenticationMethod.CLIENT_SECRET_POST`, PKCE still required) with the `REFRESH_TOKEN` grant type enabled. Its redirect URIs are restricted to `http://localhost:3000/*` (Vite dev server) to make misuse in other environments impossible even if the profile leaks.
- **`klabis-web-local` client secret has a hardcoded default value** (e.g. `local-dev-secret-please-change-nothing`) overridable via `KLABIS_OAUTH2_LOCAL_CLIENT_SECRET`. The default is documented as non-sensitive because `local-dev` profile activation is the gate, not the secret itself. Secret is NOT used in production.
- **The existing `klabis-web` public PKCE client is untouched.** Every currently-deployed code path, every test, every production configuration continues to use it. The local-dev client is purely additive.
- **Frontend (`klabisUserManager.ts`) reads the client id, client secret, and scope from Vite environment variables** (`VITE_OAUTH_CLIENT_ID`, `VITE_OAUTH_CLIENT_SECRET`, `VITE_OAUTH_SCOPE`). Default values in `.env` / `.env.development` match the production public client (`klabis-web`, no secret). A gitignored `.env.development.local` holds the local-dev client id and secret, documented in `frontend/CLAUDE.md`.
- **Backend default active profiles are unchanged** (`h2,ssl,debug,metrics,oris`). The `local-dev` profile is opt-in. `runLocalEnvironment.sh` is updated to activate `local-dev` by default so the local development workflow Just Works.
- **Documentation:**
  - `backend/CLAUDE.md` — add a short section describing the `local-dev` profile and why it exists. Link to this change for rationale.
  - `frontend/CLAUDE.md` — document the Vite env variable contract, the contents of `.env.development.local`, and the fact that production builds use the public client.
- **Testing:**
  - A new backend test verifies that when `local-dev` profile is active, `klabis-web-local` is registered and (via the full OIDC flow using `CLIENT_SECRET_POST` authentication) the token endpoint response contains a `refresh_token`.
  - Existing security tests continue to validate public-client behavior for `klabis-web`.
  - Manual frontend QA: after logging in on `http://localhost:3000`, `sessionStorage['oidc.user:http://localhost:3000/:klabis-web-local']` contains `refresh_token`; after 5 minutes, `signinSilent` renews the token via `/oauth2/token` (refresh_token grant) with no iframe request and no console errors.

- **The task file `tasks/oauth2-silent-renew-strategy-decision.md` is archived** (moved under `tasks/completed/` with today's date) after this change lands — it has served its purpose of capturing the decision.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `non-functional-requirements`: add a new requirement **"Local Development OAuth2 Client"** describing the conditionally-registered `klabis-web-local` confidential client, the profile that gates it, and the constraint that it must not be registered in any non-local profile.

## Impact

**Backend:**
- `backend/src/main/java/com/klabis/common/bootstrap/OAuth2ClientProperties.java` — add optional local-client properties (`localId`, `localSecret`, `localRedirectUris`). Defaults point to `klabis-web-local`, a hardcoded dev secret, and `http://localhost:3000/*` URIs.
- `backend/src/main/java/com/klabis/common/bootstrap/BootstrapDataLoader.java` — in `OidcRegisteredClientsBootstrap.bootstrapData()`, check if `local-dev` profile is active (via `Environment`), and if so also register `klabis-web-local` as a confidential client with `AUTHORIZATION_CODE` + `REFRESH_TOKEN` grants and PKCE required. The method must be idempotent (bootstrap already uses `requiresBootstrap()` check).
- `backend/src/main/resources/application.yml` — document the `local-dev` profile in comments. Do not add it to the default active list.
- (Optional) a new `backend/src/main/resources/application-local-dev.yml` if additional profile-specific properties make sense, otherwise profile activation alone is enough.
- New test `backend/src/test/java/com/klabis/common/security/LocalDevRefreshTokenFlowTest.java` — `@ApplicationModuleTest` with `@ActiveProfiles({"test", "local-dev"})` that runs the full authorization-code flow against `klabis-web-local` and asserts the token response contains `refresh_token`.
- Existing `OidcRegisteredClientsBootstrapTest` extended (or mirrored) to verify `klabis-web-local` is NOT registered when `local-dev` profile is inactive.

**Frontend:**
- `frontend/src/api/klabisUserManager.ts` — replace hardcoded `client_id: 'klabis-web'` and `scope: 'openid profile MEMBERS EVENTS'` with Vite env reads. Pass `client_secret` to `UserManager` when `VITE_OAUTH_CLIENT_SECRET` is set (non-empty).
- `frontend/.env` — default public config (`VITE_OAUTH_CLIENT_ID=klabis-web`, empty secret, default scopes).
- `frontend/.env.development` — same as `.env` (explicit).
- `frontend/.env.development.local.example` — committed example file showing the local-dev values (`VITE_OAUTH_CLIENT_ID=klabis-web-local`, `VITE_OAUTH_CLIENT_SECRET=local-dev-secret-please-change-nothing`).
- `frontend/.gitignore` — add `.env.development.local` if not already ignored (Vite defaults should already ignore it, but verify).
- `frontend/src/api/klabisUserManager.ts` — remove `silent_redirect_uri` and the iframe-based silent renewal fallback path? **No** — keep it as a documented fallback, because production builds use the public client and still need the iframe path as the only available silent renewal mechanism. The distinction is: with the local-dev confidential client, `oidc-client-ts` will prefer refresh token flow and never use the iframe; with the production public client, it falls back to the iframe (which, in production, runs same-origin so the existing cookie-based mechanism works).

**Configuration:**
- `runLocalEnvironment.sh` — add `local-dev` to the backend profile activation so developers get refresh-token-based silent renew by default. Leave a comment explaining that this profile is local-only.

**Documentation:**
- `backend/CLAUDE.md` — new subsection under "Application Profiles" describing `local-dev` and its rationale (one paragraph + link to this change).
- `frontend/CLAUDE.md` — new subsection under "Authentication" describing the env-var contract and the local-dev client.
- `openspec/specs/non-functional-requirements/spec.md` — new requirement added via the delta in `specs/non-functional-requirements/spec.md`.

**Production / deployed environments:**
- **Zero impact.** The `local-dev` profile is never activated in any deployed environment. Production continues to use the public `klabis-web` client exactly as before. This change is additive, not a migration.

**Tests:**
- One new backend test (`LocalDevRefreshTokenFlowTest`) and possibly one assertion added to `OidcRegisteredClientsBootstrapTest`. Total test count grows by 1–3 tests, no changes to existing tests.
