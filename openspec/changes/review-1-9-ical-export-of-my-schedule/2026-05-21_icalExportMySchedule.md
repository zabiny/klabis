# TCF: iCal Export of "Můj rozvrh" — review-1-9

Team coordination file. Each subagent: read this file first, append a concise summary of changes + issues at the end.

## Proposal summary

iCalendar subscribe feed `GET /ical/my-schedule.ics?token=<PAT>` returning events where the user has an active registration OR holds the coordinator role (same set as the "Můj rozvrh" web filter). Per-user PAT in a separate `calendar_feed_token` table owned by the `calendar` module. Token management REST API + profile UI section.

## Iteration plan

- **Iter 1 — Open `events.domain` + token storage + service** (tasks 1, 2, 3): make `Events`/`Event` public for cross-module read; new `calendar_feed_token` table + `CalendarFeedToken` aggregate; `IcalTokenService`. App stays functional (additive only).
- **Iter 2 — Schedule assembly + iCalendar serialization** (tasks 4, 5): `getMySchedule` application service reusing `EventScheduleQuery` + `Events`; `ICalendarRenderer`. App stays functional (no wiring yet).
- **Iter 3 — Feed endpoint + iCal-token auth filter** (task 6): `IcalTokenAuthenticationFilter`/`Provider`, register into resource-server chain, `SpaFallbackFilter` exclusion, `IcalFeedController`. Feed endpoint goes live.
- **Iter 4 — Token management API** (task 7): `GET`/`POST /api/me/ical-token`.
- **Iter 5 — Frontend profile section** (task 8): "Kalendářový feed" section in `MyProfile`.
- **Final — simplify review, code review fixes, full test run, commit per iteration.**

Tasks 9 (e2e deploy verification) and 10 (developer manual) are handled separately / out of automated scope where they need deployment.

## Progress log

<!-- subagents append below -->

### Iter 1 — 2026-05-21 (tasks 1, 2, 3)

**Task 1 — events.domain exposed:** Added `package-info.java` with `@NamedInterface("events.domain")` to `com.klabis.events.domain`. Updated `calendar/package-info.java` javadoc to document the dependency. `ModuleStructureVerificationTest` stays green.

**Task 2 — CalendarFeedToken aggregate + persistence:**
- `calendar_feed_token` table added in-place to `V001__initial_schema.sql` (PK: `user_id`, indexed `token_lookup`)
- `CalendarFeedToken` aggregate in `com.klabis.calendar.domain` — `generate(userId, encoder)` returns `Result(token, rawToken)`, `regenerate(encoder)` rotates in-place, `reconstruct()` for load path. Carries `isNew` flag for Spring Data JDBC upsert dispatch.
- Memento/adapter/Spring Data JDBC repo in `calendar.infrastructure.jdbc`
- `cleanup.sql` updated to delete `calendar_feed_token` before `users`
- 6 persistence integration tests green

**Task 3 — IcalTokenService:**
- `IcalTokenPort` interface + `IcalTokenService` in `calendar.application`
- `generate`/`regenerate` both create-or-rotate (identical logic, separate port methods for semantic clarity)
- `validate`: lookup by 8-char prefix → bcrypt.matches — no full-table scan
- 13 unit tests green (generate/regenerate/validate including null/short/tampered token cases)

**Pre-existing failure:** `EventManagementE2ETest.should create and retrieve event without a location` — unrelated to this iteration, present before these changes.

### Iter 2 — 2026-05-21 (tasks 4, 5)

**Task 4 — Schedule assembly:**
- `IcalWindowProperties` (`@ConfigurationProperties(prefix = "klabis.ical.window")`) — `past`/`future` Period fields, defaults P30D/P12M. Registered via `@ConfigurationPropertiesScan` on main class. Defaults added to `application.yml`.
- `IcalFeedPort` (primary port) — `getMySchedule(MemberId, LocalDate)` → `List<EventScheduleEntry>`. `EventScheduleEntry` is a nested record `(Event, boolean isCoordinator)`.
- `IcalFeedService` (`@Service`, package-private) — injects `EventScheduleQuery`, `Events`, `IcalWindowProperties`. Computes window as `[now - past, now + future]`. Loads full `Event` aggregates via `Events.findById`. Determines coordinator role via `memberId.equals(event.getEventCoordinatorId())`. Uses `Optional::stream` flatMap to skip missing events.
- Note: design.md says `getMySchedule(userId, now)` but Iter 1 already stores MemberId in `IcalTokenService` returning `UserId`; the service signature uses `MemberId` directly (conversion `MemberId.fromUserId(userId)` happens at the caller — controller in Iter 3). No `User` argument needed; coordinator check only needs `MemberId`.
- 7 unit tests green (empty, participant, coordinator, both roles, skips missing events, window offset check, null guards).

**Task 5 — iCalendar serialization:**
- `ICalendarRenderer` in `com.klabis.calendar.infrastructure.ical` — plain utility class (no Spring bean), CRLF line endings.
- VEVENT fields: UID (`{uuid}@klabis`), DTSTAMP (UTC ISO), DTSTART/DTEND (`VALUE=DATE`), SUMMARY, LOCATION (omitted if null/blank), URL, DESCRIPTION (organizer + event link + optional website URL + optional `Role: koordinátor`), STATUS (`CONFIRMED`/`CANCELLED`).
- `escapeText()` is package-accessible static method, escapes `\` → `\\`, `,` → `\,`, `;` → `\;`, newlines → `\n` (backslash first to avoid double-escaping).
- Renderer signature: `render(List<EventScheduleEntry>, String baseUrl, Instant dtstamp)` — `dtstamp` injectable for deterministic tests.
- 17 unit tests green (empty calendar, participant event, coordinator event with role line, cancelled status, omit LOCATION, website URL in description, special chars in summary, backslash escape, both-roles-in-one-render, all `escapeText()` cases).
- 24 tests total for this iteration, all green.

### Iter 3 — 2026-05-21 (task 6)

**Task 6.3 — ResourceServerCustomizer extension hook:**
- New `ResourceServerCustomizer` interface in `com.klabis.common.security` (modeled after `AuthorizationServerCustomizer`).
- `WebSecurityCommonConfiguration.defaultSecurityFilterChain` now accepts `ObjectProvider<ResourceServerCustomizer>` and applies all registered customizers before `http.build()`.
- `/ical/**` added to the authenticated request matchers.

**Task 6.1+6.2+6.3 — iCal authentication filter chain:**
- `IcalTokenAuthenticationToken` (public) — unauthenticated/authenticated constructors; `getUserId()` for controller use.
- `IcalTokenAuthenticationProvider` (package-private) — validates via `IcalTokenPort.validate()`, throws `BadCredentialsException` on failure.
- `IcalTokenAuthenticationFilter` (package-private) — `OncePerRequestFilter`, scoped to `/ical/**` via `PathPatternRequestMatcher`, handles `?token=` param; missing token falls through; invalid token → 401 direct write (no failure handler).
- `CalendarSecurityConfiguration` — contributes a `ResourceServerCustomizer` bean that wires the filter before `BasicAuthenticationFilter`.

**Task 6.4 — SpaFallbackFilter:** `/ical` added to `EXCLUDED_PREFIXES`.

**Task 6.5+6.6 — IcalFeedController:**
- `GET /ical/my-schedule.ics` (package-private `@RestController`) — reads `IcalTokenAuthenticationToken` from `SecurityContextHolder`, converts `UserId → MemberId`, calls `IcalFeedPort.getMySchedule()`, renders via `ICalendarRenderer`.
- Response: `Content-Type: text/calendar; charset=UTF-8`, `Cache-Control: max-age=600, public, no-transform`.
- `CalendarInfrastructureConfiguration` in `infrastructure.restapi` provides `ICalendarRenderer` as a Spring bean (moved from `application` layer to satisfy `LayerArchitectureTest`).

**Task 6.7 — Security unit tests:** 10 tests green (`IcalTokenAuthenticationProviderTest` + `IcalTokenAuthenticationFilterTest`).

**Task 6.8 — Integration tests:** 8 E2E tests green (`IcalFeedControllerTest`) — valid token, invalid token, missing token, /api bypass, Cache-Control header, empty schedule, coordinator event (Role: koordinátor in DESCRIPTION), cancelled event (STATUS:CANCELLED).

**Side fix:** `CalendarEventSyncIntegrationTest` (STANDALONE modulith test) needed `@MockitoBean Events events` after `IcalFeedService` introduced a new `Events` dependency into the calendar application context.

**Pre-existing failure (unchanged):** `EventManagementE2ETest.should create and retrieve event without a location`.

**Compile-error fix:** `IcalFeedController` no longer imports or casts to the concrete `IcalTokenAuthenticationToken`; it reads the `UserId` principal via the standard `Authentication.getPrincipal()` contract, allowing `IcalTokenAuthenticationToken` and its constructors to revert to package-private visibility. Unused imports `BadCredentialsException` and `List` removed from `IcalTokenAuthenticationFilterTest`. 14/14 tests green.

### Iter 4 — 2026-05-21 (task 7)

**Task 7.1+7.2 — Token management API (`GET`/`POST /api/me/ical-token`):**
- `IcalTokenPort` extended with `getTokenState(UserId) → Optional<TokenState>` where `TokenState` is a nested record `(tokenLookup, lastSetAt)`. Implemented in `IcalTokenService` (trivial repository lookup).
- `IcalTokenController` (package-private `@RestController`) in `calendar.infrastructure.restapi`:
  - `GET /api/me/ical-token` — resolves `@ActingUser UserId`, calls `getTokenState`. Returns `IcalTokenResponse(url, lastSetAt)`: masked URL (full token portion replaced by `••••••••…`) when token exists, `url=null` when no token. Self link with `generateToken` affordance (POST).
  - `POST /api/me/ical-token` — calls `icalTokenPort.generate()` (create-or-rotate semantics), returns the full subscribe URL exactly once with `lastSetAt=Instant.now()`.
  - Response record `IcalTokenResponse(String url, Instant lastSetAt)` declared at package level in same file.
  - `IcalTokenRootPostprocessor` (`@MvcComponent`) adds `ical-token` link to root model.
  - Base URL reuses `${klabis.password-setup.base-url}` property consistent with `IcalFeedController`.

**`revealUrl` decision:** tasks.md mentioned a `revealUrl` affordance for retrieving the full token. This is cryptographically impossible — the raw token is only known at generate time; only `token_hash` and `token_lookup` (first 8 chars) are stored. Showing the last 4 chars (as Decision 9 UI mockup suggests) would require storing them plaintext, which weakens the "shown once" security guarantee. Resolution: `GET` returns a fully masked URL and a `regenerate` (POST) affordance; no `revealUrl` endpoint exists. Frontend (Iter 5) must rely on POST to expose the token.

**Task 7.3 — Integration tests:**
- `IcalTokenControllerTest` (`@E2ETest`) — 9 tests:
  - `GET`: no-token returns `url=null`; token exists returns masked URL + `lastSetAt`; masked URL does not contain raw token; response includes `generateToken` POST affordance; unauthenticated → 401.
  - `POST`: no token → generates, returns full URL; existing token → regenerates, old token invalidated; POST returns full URL without mask characters; unauthenticated → 401.
- 36/36 iCal-related tests green (IcalTokenControllerTest + IcalFeedControllerTest + IcalTokenServiceTest + IcalFeedServiceTest).

**Pre-existing failure (unchanged):** `EventManagementE2ETest.should create and retrieve event without a location`.

### Iter 5 — 2026-05-21 (task 8)

**Task 8 — Frontend "Kalendářový feed" section in MemberDetailPage:**

- `CalendarFeedSection` component (`frontend/src/pages/members/CalendarFeedSection.tsx`) — standalone section component accepting `icalTokenHref` prop:
  - Fetches `GET /api/me/ical-token` via `useAuthorizedQuery` to read current token state.
  - **No token:** shows "Vytvořit kalendářový feed" button → POST → reveals full URL with copy button and warning that this is shown only once.
  - **Token exists:** shows masked URL + lastSetAt date + "Vygenerovat nový token" button. Clicking opens a Modal confirm dialog (warns that previous URL stops working). Confirming POSTs and then shows the new full URL with copy button.
  - **CopyButton** sub-component with clipboard write + 2-second "Zkopírováno" feedback.
  - **HelpText** sub-component with Google Calendar / Apple Calendar / Outlook instructions and a note about "Můj rozvrh" filter consistency.

- `MemberDetailPage.tsx` updated: imports `CalendarFeedSection`, fetches root `/api` resource (staleTime 5 min) to extract `ical-token` href, renders `CalendarFeedSection` below deactivation section when not in edit mode and when link is present.

- `labels.ts` updated: new `calendarFeed` category with 16 Czech labels covering all states (create, regenerate, copy, confirm dialog, help text).

- **UX decision:** tasks.md 8.3 mentioned "Zobrazit celou URL" toggle but the task description and TCF note clarify honest UX — masked URL on returning visit + full URL only after (re)generation. Implemented accordingly: no reveal toggle (token hashed, irrecoverable); copy button shown only when full URL is available after POST.

- **17 frontend tests** covering: loading state, no-token state (button, help text, Můj rozvrh, POST call, disabled during pending), token-exists state (masked URL, lastSetAt, confirm dialog open/cancel/confirm, POST call), post-success state (full URL display, copy button).

- All 1486 frontend tests green. TypeScript build clean.

### Code review fixes — backend (2026-05-21)

**HIGH-1 — Collapsed `generate()`/`regenerate()` into `generateOrRotate()`:** Both port methods were functionally identical (both delegated to `createOrRotate()`). Merged into a single `IcalTokenPort.generateOrRotate(UserId)` returning a new `GenerateResult(rawToken, lastSetAt)` record. Updated `IcalTokenService`, `IcalTokenController` (POST handler), `IcalTokenControllerTest`, `IcalFeedControllerTest`, `IcalTokenServiceTest` accordingly. Removed the false `@throws` Javadoc and the no-longer-needed `import java.time.Instant` from the port.

**HIGH-2 — Eliminated duplicate `LOOKUP_LENGTH`:** Made `CalendarFeedToken.LOOKUP_LENGTH` `public static final`. Removed the private copy from `IcalTokenService`; `validate()` now references `CalendarFeedToken.LOOKUP_LENGTH` directly as the single source of truth.

**HIGH-3 — RFC 5545 line folding:** Added `appendLine(StringBuilder, String)` to `ICalendarRenderer` that folds lines exceeding 75 octets at UTF-8 octet boundaries (CRLF + leading SPACE per RFC 5545 §3.1). All property lines are now emitted via `appendLine`. Added `shouldFoldLongDescriptionLines` unit test verifying no physical line exceeds 75 octets and at least one folded continuation exists. Fixed `shouldIncludeWebsiteUrlInDescription` to unfold before asserting logical content.

**MEDIUM — POST response uses persisted `lastSetAt`:** `generateOrRotate()` returns `GenerateResult` carrying both `rawToken` and the persisted `lastSetAt` (from the saved aggregate). Controller builds the response from `result.lastSetAt()` instead of `Instant.now()`.

**MEDIUM — Dedicated `klabis.ical.base-url` property:** Added `klabis.ical.base-url: ${KLABIS_BASE_URL:https://localhost:8443}` to `application.yml`. Both `IcalFeedController` and `IcalTokenController` now inject from `${klabis.ical.base-url:...}` instead of the unrelated `klabis.password-setup.base-url`.

**MEDIUM — SecurityContext cleared on downstream exception:** `IcalTokenAuthenticationFilter.doFilterInternal` wraps `chain.doFilter(...)` in a `try/finally` block that calls `SecurityContextHolder.clearContext()`, matching Spring's `BearerTokenAuthenticationFilter` pattern. Updated the valid-token filter test to capture the authentication during chain execution (via `doAnswer`) instead of reading the context after the filter returns.

**53/53 iCal-related tests green.**

### Code review fix — duplicate helpText (2026-05-21)

`CalendarFeedSection` rendered `l.helpText` twice: once in the inline calendar-icon intro (line 88) and once inside the `HelpText` block (line 44). Fixed by adding a distinct `intro` label ("Přihlaste se k odběru svých akcí v externím kalendáři.") to `labels.calendarFeed` and using it for the inline intro. The detailed `HelpText` block (with client instructions and "Můj rozvrh" note) is the sole occurrence of `helpText`. 17/17 CalendarFeedSection tests green, TypeScript build clean.
