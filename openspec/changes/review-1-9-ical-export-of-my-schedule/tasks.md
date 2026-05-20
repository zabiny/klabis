## 1. Open events.domain for cross-module read access

- [x] 1.1 Make the `events.domain` package's query API public: the `Events` interface (`findById`, `findAll`) and the `Event` aggregate become part of the events module's public surface
- [x] 1.2 Verify Spring Modulith module rules still pass — the `calendar → events` dependency already exists (via `EventScheduleQuery`, `EventDataProvider`); confirm no new violation is introduced
- [x] 1.3 Module integration test (`ApplicationModules.verify()`) stays green

## 2. Calendar feed token storage (separate table, owned by calendar module)

- [x] 2.1 DB migration: new table `calendar_feed_token` with `user_id`, `token_hash VARCHAR(255)`, `token_lookup VARCHAR(16)` (indexed), `last_set_at TIMESTAMP` — update `V001__initial_schema.sql` in place (production runs on H2 without persistent data, see Review #1)
- [x] 2.2 Domain entity/aggregate `CalendarFeedToken` in the calendar module — owns `userId`, hashed token, lookup prefix, `lastSetAt`
- [x] 2.3 Domain method `regenerate(passwordEncoder)` that produces a new raw token (returned to caller), recomputes hash + lookup prefix, updates `lastSetAt`
- [x] 2.4 Repository / persistence (memento mapping) for `CalendarFeedToken`
- [x] 2.5 Domain unit tests + persistence integration test

## 3. Token validation service

- [x] 3.1 Create `IcalTokenService` (calendar application layer): `generate(userId)`, `regenerate(userId)`, `validate(rawToken) -> Optional<UserId>`
- [x] 3.2 `validate`: look up the row by `token_lookup` prefix (indexed, O(1)), then `passwordEncoder.matches(raw, token_hash)` — constant-time, no full-table scan
- [x] 3.3 Unit tests for generate / regenerate / validate flows including invalid token handling

## 4. Schedule data assembly (reuse EventScheduleQuery + Events)

- [x] 4.1 Inject `EventScheduleQuery` and the now-public `Events` interface into the iCal feed application service in the calendar module
- [x] 4.2 Default feed window: configuration properties `klabis.ical.window.past=P30D`, `klabis.ical.window.future=P12M`; compute `[today - past, today + future]` per request
- [x] 4.3 Application service: `getMySchedule(userId, now)` → resolve `User → MemberId` → `findEventIdsForMemberSchedule(memberId, from, to)` → load full `Event` aggregates via `Events.findById`
- [x] 4.4 For each event, determine the user's role for the description text: `event.coordinatorId().equals(memberId)` → coordinator; otherwise participant. Data comes from the loaded `Event` aggregate — no extra DB queries
- [x] 4.5 Unit tests covering: user with only registrations, only coordinator role, both, neither (empty list), event present for both participant and coordinator role appears exactly once

## 5. iCalendar serialization

- [x] 5.1 Create `ICalendarRenderer` utility: takes `User` + list of `(Event, isCoordinator)` pairs + base URL, returns iCalendar text (UTF-8, CRLF line endings)
- [x] 5.2 VEVENT mapping: UID = event UUID, DTSTAMP = now UTC, DTSTART = event date (whole-day), DTEND = event date + 1, SUMMARY = name, LOCATION = location (if present), URL = event detail URL, DESCRIPTION = organizer + Klabis link + optional event website + "Role: koordinátor" line when `isCoordinator`, STATUS = CANCELLED for cancelled events
- [x] 5.3 Escape rules for SUMMARY/LOCATION/DESCRIPTION: backslash, comma, semicolon, newline (per RFC 5545)
- [x] 5.4 Unit tests with golden output: 0 events, 1 event registered, 1 event coordinated, event with both roles, cancelled event, special characters in name/location

## 6. Feed endpoint + iCal-token authentication

- [x] 6.1 `IcalTokenAuthenticationProvider` in `com.klabis.calendar.infrastructure.security` — accepts an `IcalTokenAuthenticationToken`, validates via `IcalTokenService.validate(token)`, returns an authenticated principal on success or throws `AuthenticationException`
- [x] 6.2 `IcalTokenAuthenticationFilter` in `com.klabis.calendar.infrastructure.security` — `RequestMatcher` limited to `/ical/**`; when the request carries a `token` query param, builds an unauthenticated `IcalTokenAuthenticationToken` and delegates to the `AuthenticationManager`; when absent, does nothing (request falls through to OAuth2)
- [x] 6.3 Register the filter into the existing resource-server `SecurityFilterChain` before `BearerTokenAuthenticationFilter`, using the project's cross-module security composition mechanism; introduce a minimal extension hook in `common.security` only if no usable one exists (verify against `WebSecurityCommonConfiguration` / `ResourceServerSecurityConfiguration`)
- [x] 6.4 Add `/ical` to `EXCLUDED_PREFIXES` in `com.klabis.common.ui.SpaFallbackFilter` so the feed URL is never forwarded to the SPA shell (independent of review-1-1)
- [x] 6.5 Controller `IcalFeedController` (calendar module) with `@GetMapping("/ical/my-schedule.ics")` — relies on the authenticated principal from the filter, calls the application service, renders feed
- [x] 6.6 Response: `Content-Type: text/calendar; charset=UTF-8`; `Cache-Control: max-age=600`
- [x] 6.7 Security tests: `/ical/**` with valid `token` → authenticated; with invalid/unknown `token` → 401; `/ical/**` without `token` → falls through to OAuth2; `?token=` on `/api/**` is ignored (no auth bypass)
- [x] 6.8 Integration tests: valid token → 200 with iCal body; user with empty schedule → 200 empty calendar; event with coordinator-only role present; cancelled events → STATUS:CANCELLED in output

## 7. Token management API

- [ ] 7.1 `GET /api/me/ical-token` — returns `{ url, lastSetAt }` if token exists, else `{ url: null }`; URL is the masked subscribe URL (last 4 chars only) plus a separate `revealUrl` link
- [ ] 7.2 `POST /api/me/ical-token` — generate or regenerate token; returns the raw URL once (with full token); updates `lastSetAt`
- [ ] 7.3 Integration tests for both endpoints (authenticated, regular user only)

## 8. Frontend — profile section

- [ ] 8.1 In `MyProfile` page add "Kalendářový feed" section
- [ ] 8.2 If no token: show "Vytvořit kalendářový feed" button → POST regenerate → display the URL once with copy button
- [ ] 8.3 If token exists: show masked URL + "Zobrazit celou URL" toggle + "Kopírovat" + "Vygenerovat nový token" (with confirm dialog warning that the previous URL will stop working)
- [ ] 8.4 Help text in CS with brief instructions for Google Calendar / Apple Calendar / Outlook; mention that the feed mirrors the "Můj rozvrh" filter (registrations + events you coordinate)
- [ ] 8.5 Localization labels in `src/localization/labels.ts`
- [ ] 8.6 Frontend tests for the section

## 9. End-to-end verification

- [ ] 9.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 9.2 Generate token in profile, copy URL
- [ ] 9.3 Open URL in a browser tab — confirm `text/calendar` content with at least one VEVENT for a known registered event AND a known coordinated event for a seeded user with both roles
- [ ] 9.4 Add the URL to Google Calendar (test account); wait for first sync; confirm the event appears on the correct date
- [ ] 9.5 Assign coordinator role to the test user on a new event; refresh; confirm the event appears with "Role: koordinátor" in the description
- [ ] 9.6 Cancel one of the events in Klabis; refresh the calendar manually; confirm the event is marked cancelled (or removed, depending on Google Calendar's STATUS:CANCELLED handling)
- [ ] 9.7 Unregister from another event (and verify the user is not its coordinator); refresh; confirm the event disappears from the calendar
- [ ] 9.8 Regenerate token in profile; confirm the old URL stops working (401)

## 10. Documentation

- [ ] 10.1 Update `docs/developerManual` with feed URL format, token lifecycle, the shared "Můj rozvrh" semantics (link to archived `calendar-my-schedule-filter`), and operational notes (caching, leak handling)
- [ ] 10.2 Sync the modified `calendar-items` spec into `openspec/specs/calendar-items/spec.md` after archiving
