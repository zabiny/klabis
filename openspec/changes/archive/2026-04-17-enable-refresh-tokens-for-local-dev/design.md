## Context

The Klabis SPA uses OAuth2 authorization code flow with PKCE against a Spring Authorization Server built into the same backend process. The client `klabis-web` is registered as a public client (`ClientAuthenticationMethod.NONE`) and has `AUTHORIZATION_CODE` + `REFRESH_TOKEN` grant types enabled. `TokenSettings` configures refresh token rotation (`reuseRefreshTokens=false`, `refreshTokenTimeToLive=24h`).

In development, the frontend runs on the Vite dev server (`http://localhost:3000`) and the backend runs on `https://localhost:8443`. In every deployed environment the frontend is served by the backend on `:8443` (same origin) — there is no separate frontend host.

During active development, silent token renewal fails silently: after the access token expires (5 minutes), the user is forced to log in again. Investigation (2026-04-09) identified the root cause:

Spring Authorization Server 7.0.4 **hardcodes** a refusal to issue refresh tokens to public clients on the `authorization_code` grant. The relevant code is in `OAuth2RefreshTokenGenerator.isPublicClientForAuthorizationCodeGrant(OAuth2TokenContext)` — if the grant type is `AUTHORIZATION_CODE` and the client authenticated with `ClientAuthenticationMethod.NONE`, the generator returns `null` instead of a refresh token, regardless of whether the client has `REFRESH_TOKEN` in its registered grant types.

Without a refresh token in the user object, `oidc-client-ts` falls back to iframe-based silent renewal: it opens a hidden iframe with `src=https://localhost:8443/oauth2/authorize?...&prompt=none&redirect_uri=http://localhost:3000/silent-renew.html`. The iframe request is cross-origin (parent is `:3000`, iframe is `:8443`), so the browser does not send the auth server session cookie (`SameSite=Lax` default), the auth server sees an unauthenticated request, and responds with the OIDC `login_required` error (after the fix in `tasks/completed/2026-04-09_21-30-00_oauth2-silent-renew-prompt-none-handling.md`). Before that fix, the response was a 302 to `/login` which the browser refused to render in the iframe due to `X-Frame-Options: deny`.

In deployed environments this problem does not arise because the frontend and backend share the `:8443` origin — the iframe carries the session cookie and silent renewal works end-to-end with the public client.

This design documents the two candidate solutions we evaluated, the choice, and the conditions under which we would revisit.

## Goals / Non-Goals

**Goals:**
- Restore working silent token renewal during local development (`http://localhost:3000`).
- Keep the production authentication model (public PKCE client) intact. Production security posture must not regress.
- Keep implementation scope bounded to hours, not days. This is a development-ergonomics fix, not a production architecture change.
- Keep the decision reversible: if we later decide to invest in production-grade sender-constrained tokens, the local-dev shortcut should be easy to retire without having to untangle a large rewrite.

**Non-Goals:**
- Implementing DPoP or any other sender-constrained token mechanism. (See "Decision 1" and "When Option 1 becomes necessary" below.)
- Replacing the existing iframe-based silent renewal for deployed environments. It works there because of same-origin and does not need replacement.
- Migrating the production SPA to a BFF (backend-for-frontend) pattern. That is a separate, much larger discussion.
- Changing the scopes, token lifetimes, or grant types of the existing public client.
- Supporting refresh tokens for public clients in any environment other than local development.

## Decisions

### Decision 1: Choose profile-based confidential client over custom Spring AS component + DPoP

**Choice:** Add a second OAuth2 client `klabis-web-local` registered only under a new Spring profile `local-dev`. It is a confidential client (`CLIENT_SECRET_POST`) with PKCE still required, and benefits from Spring AS's default refresh token path because Spring only rejects refresh tokens for public clients on the authorization code grant.

**Why:** The evaluation of the two candidate options produced the following cost/benefit:

| Criterion | Option 1: Custom generator + DPoP | Option 2: Profile-based confidential client |
|---|---|---|
| Implementation scope | Days to a week (custom generator, DPoP client support, DPoP resource server validation on every API endpoint, security tests) | A few hours (one extra client registration, small frontend env wiring) |
| Custom security code to maintain | A custom `OAuth2TokenGenerator` overriding Spring's default behavior, plus a DPoP proof verifier integrated into every resource server controller | Zero custom security code |
| Spring AS upgrade risk | High: depends on Spring AS internal APIs; future versions may change `OAuth2TokenGenerator` contract or `OAuth2RefreshTokenGenerator` behavior | None: we use Spring AS exactly as documented |
| Production security posture | Improved (DPoP is state of the art for browser-based public clients) | Unchanged (production public client untouched) |
| Dev ergonomics (Vite HMR) | Works | Works |
| Security review required before merge | Yes (custom security component) | No (standard Spring AS configuration) |
| Test surface | Very large (DPoP proof lifecycle, nonce handling, clock skew, rotation-under-replay, resource server enforcement) | Small (one happy-path test for the local client flow) |

The Klabis project has no production deployment yet and no urgent threat model that demands DPoP. Investing a week of engineering effort in a custom security component to improve an environment that does not exist yet is the textbook definition of premature optimization. The local-dev client unblocks the development workflow in an afternoon, costs nothing in production, and leaves the door open to adopt Option 1 later if the situation changes.

**Alternatives considered:**
- **Option 1 as described above.** Rejected for the reasons in the table.
- **Option 3: Serve the frontend through the backend on `:8443` in development too (abandon Vite dev server).** Rejected — loses Vite HMR, which noticeably slows the frontend development loop. The project's current guidance explicitly prefers `:3000` for UI testing because of HMR.
- **Option 4: Configure the auth server session cookie as `SameSite=None; Secure` and serve the Vite dev server on `https://localhost:3000`.** Rejected — requires a self-signed cert on the dev server (trust friction), weakens CSRF defenses by removing the `SameSite=Lax` safety net, and keeps the legacy iframe-based silent renewal path instead of moving toward refresh tokens.
- **Option 5: Do nothing and extend `accessTokenTimeToLive` to 8 hours for dev only.** Rejected as the permanent answer — a workaround, not a fix. Mentioned as a fallback in the previous task file in case this proposal gets delayed.

### Decision 2: Keep the existing `klabis-web` public client completely untouched

**Choice:** `klabis-web` is not modified. No change to its `ClientAuthenticationMethod`, redirect URIs, grant types, or token settings. The new `klabis-web-local` client is added alongside it.

**Why:** The existing public client is the production configuration. Touching it would couple a development-ergonomics fix to a production-affecting change and would require broader review. Adding a second client is strictly additive — if the local-dev client registration fails for any reason, production behavior is unchanged.

It also keeps the decision trivially reversible. To retire the local-dev shortcut (e.g. if we later implement Option 1), delete the `local-dev` profile, delete the `klabis-web-local` registration code, revert the frontend env wiring, and done. No migration, no data cleanup, no user impact.

**Alternatives considered:**
- **Change `klabis-web` from public to confidential.** Rejected — violates OAuth 2.1 BCP for Browser-Based Apps (SPAs are public clients by definition; a client secret shipped in a JS bundle is not a secret), and would ship the dev workaround to production.
- **Change `klabis-web` to confidential only via a profile override.** Rejected — conflating the identity of the client with an environment-dependent authentication method is confusing and error-prone. A separate client is cleaner.

### Decision 3: Gate the local-dev client behind a Spring profile, not an environment variable

**Choice:** Registration of `klabis-web-local` is conditional on the `local-dev` Spring profile being active (checked via `Environment.acceptsProfiles`). It is not conditional on the presence of an env var or a configuration property value.

**Why:** Spring profiles are the project's existing mechanism for environment-dependent behavior — `application.yml` already uses `h2`, `ssl`, `debug`, `metrics`, `oris`, and `test` profiles. Profile-based gating is familiar to everyone working on the project and is visible at a glance in every place that checks the active profile. A conditional based on "is property X set" would hide the gate behind an implementation detail.

Profile-based gating also composes cleanly: a developer can run `SPRING_PROFILES_ACTIVE=h2,ssl,debug,metrics,local-dev` for their local setup, while CI tests run with the `test` profile group (no `local-dev`), and any deployed environment runs with whatever production profile set is configured (no `local-dev`). There is no path by which a deployed environment could accidentally activate `local-dev` unless an operator explicitly puts it in the profile list.

**Alternatives considered:**
- **Env var like `KLABIS_OAUTH2_LOCAL_CLIENT_ENABLED=true`.** Rejected — invents a new mechanism alongside the existing profile system and splits the project's "how do we express environment differences" story into two.
- **Always register both clients, and rely on the client id the frontend uses to pick one.** Rejected — it would permanently pollute every environment (including production) with a confidential client that nobody should ever use. Even if no production client ever authenticates as `klabis-web-local`, the registration is an attack surface that does not need to exist.

### Decision 4: Hardcode a known-default client secret for the local-dev client, overridable by env var

**Choice:** The `klabis-web-local` client secret has a hardcoded default value (e.g. `local-dev-secret-please-change-nothing`) in `OAuth2ClientProperties`. It can be overridden via `KLABIS_OAUTH2_LOCAL_CLIENT_SECRET` for developers who want a per-machine value, but the default is sufficient and does not need to be secret.

**Why:** The security of this client does not depend on the secrecy of its secret. It depends on two other guarantees:
1. The client is registered only under the `local-dev` profile, which is activated only on developer machines.
2. The client's redirect URIs are restricted to `http://localhost:3000/*`, so even if an attacker somehow learned the secret, they could only use it to get a code that redirects to localhost on the attacker's own machine — not a useful attack.

Given that, the secret exists purely because Spring AS requires one for confidential clients. Hardcoding a known default removes a friction point for every new developer (no setup step, no "where do I get the secret" question). The override env var exists as a safety valve for teams that want a stricter convention.

This is the exact same pattern as `spring.datasource.password=password` in development config: the password is not a secret, it is a placeholder required by the framework.

**Alternatives considered:**
- **Generate a random secret on first boot.** Rejected — means the frontend `.env.development.local` must be regenerated every time the H2 database is reset (which is every backend restart under the `h2` profile). Breaks DX.
- **Require developers to set the env var with no default.** Rejected — adds an onboarding step for a value that has no security meaning.

### Decision 5: Frontend reads client id, client secret, and scope from Vite env variables

**Choice:** `klabisUserManager.ts` replaces hardcoded `client_id: 'klabis-web'` and `scope: 'openid profile MEMBERS EVENTS'` with reads from `import.meta.env.VITE_OAUTH_CLIENT_ID`, `VITE_OAUTH_CLIENT_SECRET`, and `VITE_OAUTH_SCOPE`. When `VITE_OAUTH_CLIENT_SECRET` is set (non-empty), it is passed to `UserManager` as `client_secret`, which causes `oidc-client-ts` to use `client_secret_post` authentication at the token endpoint.

Default values in `.env` match the production public client (`klabis-web`, empty secret, production scopes). A gitignored `.env.development.local` overrides them for developers who want refresh-token-based silent renew.

**Why:** Vite's env var mechanism is well-documented, standard, and supports `.env` layering (`.env` < `.env.development` < `.env.development.local`) out of the box. `.env.development.local` is gitignored by Vite by default, which matches the pattern we want: production config is committed, developer-specific overrides are not.

Using env vars keeps the code generic: `klabisUserManager.ts` has no knowledge that there are "two clients" — it just uses whatever client the environment provides. If we later add a third client, or retire the local-dev client, no code changes are needed, only env files.

**Alternatives considered:**
- **Hardcode both clients in the frontend and pick one at runtime based on `window.location.hostname === 'localhost'`.** Rejected — couples client identity to hostname detection, which is fragile and cannot be turned off per-developer. Also puts the client secret in the bundled JS unconditionally.
- **Feature flag via the HAL root response (backend tells frontend which client to use).** Rejected — overcomplicates the bootstrap, and the frontend needs the client id before it can even talk to the backend for anything.

### Decision 6: Do not remove the iframe-based silent renewal path from the frontend

**Choice:** Leave `silent_redirect_uri`, `silent-renew.html`, and the iframe silent renewal code path in the frontend. It becomes unused on developer machines that activate the local-dev client, but remains the mechanism of choice for production builds (and any other environment using the public `klabis-web` client).

**Why:** The iframe silent renewal path works correctly in production because the SPA runs same-origin with the auth server on `:8443`. The cross-origin failure only affects development. Removing the iframe code path would break silent renewal in production. Keeping it as a parallel path means `oidc-client-ts` automatically picks refresh token flow when a refresh token exists, and falls back to iframe only when it doesn't — which is exactly the behavior we want.

**Alternatives considered:**
- **Delete the iframe code now to simplify the frontend.** Rejected — would break production silent renewal.
- **Keep the iframe code but mark it deprecated with a comment.** Rejected — it is not deprecated; it is load-bearing for production.

## Risks / Trade-offs

### Risk: Dev/prod divergence creates bugs that only appear in one configuration

**What could go wrong:** The local-dev client uses a different authentication method (`CLIENT_SECRET_POST`) than the production client (`NONE`). A bug in the auth server that affects one code path might not affect the other. A fix that works for one client might break the other. Developers test against `klabis-web-local`; QA discovers regressions against `klabis-web` in production.

**Mitigation:**
- The divergence is narrow. The only paths that differ are the token endpoint's client authentication step and the refresh-token-issuance branch inside `OAuth2AuthorizationCodeAuthenticationProvider`. Everything else (authorization endpoint, scopes, PKCE validation, ID token generation, UserInfo, CSRF, session cookie handling, filter chain) is shared.
- Before any release, a manual smoke test must be performed against `klabis-web` on `:8443` (served through the backend) to verify public-client flow still works. This is already the natural pre-release QA target because it matches production.
- Existing backend tests (`AuthorizationServerTest`, `AuthorizationServerPromptNoneTest`, `OidcRegisteredClientsBootstrapTest`) continue to validate public-client behavior. They do not use the local-dev profile and therefore do not drift.
- If a reported issue only reproduces in local dev, the first step is to retest on `:8443` with the public client to isolate whether it is a local-dev artifact.

### Risk: Developers forget to gitignore `.env.development.local` and commit their "secret"

**What could go wrong:** The hardcoded default secret ends up in git history.

**Mitigation:**
- Vite's default `.gitignore` (created by `npm create vite`) already includes `.env*.local`. Verify this is in place in the project's `frontend/.gitignore` as part of implementation.
- Commit a `.env.development.local.example` file that documents the contract. Developers copy this file to `.env.development.local` rather than inventing their own.
- The "secret" is documented as non-sensitive in `frontend/CLAUDE.md`, so even if it leaks, the impact is nil. It cannot be used against production because the `klabis-web-local` client does not exist in production.

### Risk: Future Spring AS upgrade removes the "public clients don't get refresh tokens" rule, making this workaround pointless

**What could go wrong:** Spring AS 8.x adds a per-client setting to allow refresh tokens for public clients. The workaround becomes obsolete but the local-dev profile is still present, adding confusion.

**Mitigation:**
- The workaround is small enough to retire quickly when that day comes. Delete the profile, delete the `klabis-web-local` registration, revert the env wiring in the frontend. Less than an hour of work.
- This design document captures the Spring AS behavior that motivated the workaround. When a future developer wonders "why is this here?", the answer is one `openspec` query away.

### Risk: The `local-dev` profile leaks into a deployed environment by operator error

**What could go wrong:** Someone puts `local-dev` into `SPRING_PROFILES_ACTIVE` on a deployed host. The confidential client is registered in a place where it should not be. If the secret is the hardcoded default, an attacker who guesses the client id and secret can obtain tokens (limited to the local-dev redirect URIs, so their usefulness is still bounded).

**Mitigation:**
- The `klabis-web-local` client's redirect URIs must be **only** `http://localhost:*` URIs. Even if an attacker obtains a code for this client, they cannot redirect it anywhere useful off the victim's machine.
- The frontend in deployed environments does not read `VITE_OAUTH_CLIENT_ID=klabis-web-local` — it uses the baked-in production env (`klabis-web`). So an activated `local-dev` profile on a deployed host does nothing unless someone also deploys a frontend build that uses the local-dev client, which would require an explicit malicious act.
- Document the profile as "local-dev only" in `backend/CLAUDE.md` and in the profile's own comments.
- Defense in depth: the bootstrap code logs a warning at startup if the `local-dev` profile is active on a host where the `ssl` profile is also active AND the issuer URL is not `localhost` — this would catch the operator-error case.

### Trade-off: Two clients in the bootstrap registration code adds a small maintenance burden

**What this costs:** Every time we add a new redirect URI or a new scope, we might need to update both clients. There is a risk of drift (e.g. adding a new redirect URI only to one of them).

**Accepted because:** The two clients have intentionally different purposes. The local-dev client only needs `http://localhost:3000/*` URIs — it should NOT mirror the production client's URIs. The scope lists, grant types, and token settings naturally track each other because both are driven from the same `resolveScopes()` helper. The maintenance delta is small and scoped to the bootstrap file, which is easy to audit.

## When Option 1 becomes necessary

Option 1 (custom Spring AS component with DPoP) is the right answer under any of the following conditions:

1. **Klabis gains a production deployment where the frontend and backend live on different origins.** If at some point the SPA is served from `app.klabis.cz` and the backend from `api.klabis.cz`, the iframe-based silent renewal path stops working in production for the same reason it stops working in local dev today. At that point the production SPA needs refresh tokens, and refresh tokens for a browser-based public client must be sender-constrained (DPoP) to meet OAuth 2.1 BCP.

2. **The Klabis security model evolves to require sender-constrained tokens.** For example, if a regulator, auditor, or partner integration demands that access/refresh tokens cannot be replayed after exfiltration, the current bearer-token model is insufficient regardless of the development story.

3. **The OAuth 2.1 BCP for Browser-Based Apps is updated to formally deprecate non-DPoP refresh tokens for public clients**, and the project needs to comply.

4. **A security incident or near-miss involves token exfiltration from browser storage.** This is hypothetical today but would force the issue.

Under any of these conditions, the local-dev shortcut introduced by this proposal is **not in the way**. Retiring it is a one-hour task: delete the profile, delete the client registration, revert the frontend env wiring. The DPoP work can then proceed against the one remaining production client, with all the scope and complexity that entails.

The value of Option 2 is that it buys us development ergonomics today **without paying the DPoP cost up front**, and without building any infrastructure that would have to be unwound later if DPoP does become necessary.

## Migration

No migration. This change is purely additive: a new profile, a new client registration conditional on that profile, and new frontend env variables with defaults that preserve current behavior. Nothing is renamed, removed, or migrated.

If a developer is running a local environment right now, the change takes effect after:
1. Pulling the new code
2. Copying `frontend/.env.development.local.example` to `frontend/.env.development.local`
3. Restarting the backend with the `local-dev` profile activated (automatic via updated `runLocalEnvironment.sh`)
4. Logging out and back in (to obtain a fresh token response that includes the refresh token)

The first silent renewal after that will use the refresh token flow; no iframe request will be made.
