# OAuth2 silent renew — authorization server handles prompt=none incorrectly

**Type:** Bug / queue task
**Created:** 2026-04-09
**Reported by:** QA testing for IMPLEMENTATION_ORDER.md (see `2026-04-09_implementation-order-qa-testing.md` issue #4)

## Problem

After the recent `tasks/completed/2026-04-08_15-00-00_oauth2-silent-renew-redirect-uri.md` fix, silent renew still fails in the browser. Symptoms:

- Console: `Refused to display 'https://localhost:8443/' in a frame because it set 'X-Frame-Options' to 'deny'.`
- Network tab: repeated `GET https://localhost:8443/login` requests (no `/oauth2/authorize?...&prompt=none` responses with a code)
- Silent renew never completes; after token expiry, user is forcibly logged out

## Root cause (from investigation)

The earlier fix correctly added `http://localhost:3000/silent-renew.html` and `https://localhost:8443/silent-renew.html` to the `klabis-web` OAuth2 client's `redirectUris` in `backend/src/main/java/com/klabis/common/bootstrap/OAuth2ClientProperties.java:11` and `BootstrapDataLoader.java:83-85`. This part is verified working.

However, in `backend/src/main/java/com/klabis/common/security/AuthorizationServerConfiguration.java:216-220`, the `@Order(1)` authorization server filter chain uses:

```java
.exceptionHandling(exceptions -> exceptions
    .defaultAuthenticationEntryPointFor(
        new LoginUrlAuthenticationEntryPoint("/login"),
        new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
);
```

This redirects ANY unauthenticated TEXT_HTML request to `/login`, including `GET /oauth2/authorize?...&prompt=none` from the silent renew iframe. Per OpenID Connect spec, when `prompt=none` is set and the user isn't authenticated, the authorization endpoint MUST return an `login_required` error back to the redirect_uri — it MUST NOT redirect to a login page.

Because the filter chain handles auth failure before the authorization endpoint ever inspects `prompt=none`, the 302→/login path is taken. The browser then refuses to load `/login` in the iframe due to `X-Frame-Options: deny`.

## Scope considerations

There may also be an orthogonal concern: the authorization server's session cookie is scoped to `https://localhost:8443`, while the SPA runs on `http://localhost:3000`. Even if the entry point handled `prompt=none`, the iframe may not carry the correct session cookie (cross-origin). This needs verification during implementation.

## Suggested fix direction (to be validated)

Option A (preferred): Configure the authorization server chain so that OAuth2 authorization endpoint requests are NOT routed through the default login-redirect entry point when the request carries `prompt=none`. Spring Authorization Server handles `prompt=none` internally when the endpoint is reached — the fix is to let the request reach the authorization endpoint rather than intercepting it in the auth entry point.

Option B: Add a request matcher that excludes `/oauth2/authorize?prompt=none` from the login entry point and instead returns a 302 back to `redirect_uri` with `error=login_required`.

Option C: If cross-origin session cookies are also a problem, the SPA may need to be served from the same origin as the authorization server (e.g., always run via backend at :8443) OR the auth server cookie needs `SameSite=None; Secure`.

## Files of interest

- `backend/src/main/java/com/klabis/common/security/AuthorizationServerConfiguration.java:193-223` (the `@Order(1)` chain)
- `backend/src/main/java/com/klabis/common/bootstrap/OAuth2ClientProperties.java` (client config — already correct)
- `frontend/src/api/klabisUserManager.ts:102` (silent_redirect_uri — already correct)
- `frontend/public/silent-renew.html` (static callback page)
- `backend/src/main/resources/static/silent-renew.html`

## Acceptance criteria

- Login as admin on `http://localhost:3000`
- Wait past token expiry (or trigger a manual silent renew)
- Silent renew iframe request `GET https://localhost:8443/oauth2/authorize?...&prompt=none&redirect_uri=http://localhost:3000/silent-renew.html` returns `HTTP 302` with `code` in the redirect URL
- Browser console has no `X-Frame-Options` errors and no repeated `/login` fetches
- User session remains active without forced re-login

## Notes

- This was discovered during QA of the completed `IMPLEMENTATION_ORDER.md` queue task. Marked done in that file, but the underlying silent renew behaviour is still broken. The earlier task fixed redirect_uri validation (a real bug); this one fixes the auth server filter chain.
