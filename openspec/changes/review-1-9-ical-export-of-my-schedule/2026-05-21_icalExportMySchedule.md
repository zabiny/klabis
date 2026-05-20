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
