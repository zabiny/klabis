---
name: mdc-user-id-leak-from-auth-event-listener
description: userId MDC must only be written inside RequestLoggingFilter's /api scope; a global AuthenticationSuccessEvent listener leaked it onto pooled/test threads
metadata:
  type: project
---

`RequestLoggingFilter` must stay the only writer of the `userId` MDC key, and only inside its `/api/**` try/finally.

**Why:** until 2026-11-16 it also put `userId` from a global `@EventListener(AuthenticationSuccessEvent)`. Form login (`POST /login`) and other non-`/api` authentications publish that event, and `AbstractAuthenticationProcessingFilter`'s success handler terminates the filter chain — no downstream filter gets to clear the thread-local, so the servlet (or test-worker) thread keeps e.g. `[user=admin]`. Symptom: `MdcFilterIntegrationTest.shouldCleanupMDCAfterRequest` failed only in full-suite runs with `expected null but was {"userId"="admin"}`, when an earlier class in the same Gradle fork had logged in.

**How to apply:** never set MDC from an event listener/aspect outside a filter's request scope. `RequestLoggingFilter` is ordered at `SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1` so the SecurityContext is already populated when it runs; it reads `SecurityContextHolder` (skipping anonymous auth), puts the key inside the try, and `MDC.clear()`s in `finally`. New MDC keys belong in that same scope. See [[field-level-auth-pattern]] for the other request-scoped mechanism that must not rely on out-of-band callbacks.
