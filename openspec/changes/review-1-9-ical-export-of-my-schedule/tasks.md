## 1. Token storage on User aggregate

- [ ] 1.1 DB migration: add `ical_token_hash VARCHAR(255) NULL` and `ical_token_last_set TIMESTAMP NULL` to `users` table — update `V001__initial_schema.sql` in place (production runs on H2 without persistent data, see Review #1)
- [ ] 1.2 Extend `User` aggregate with `iCalAccessTokenHash: Optional<String>` and `iCalAccessTokenLastSet: Optional<Instant>` fields
- [ ] 1.3 Domain method `generateNewToken(passwordEncoder)` that produces a new raw token (returned to caller) and stores its hash on the aggregate; updates `iCalAccessTokenLastSet`
- [ ] 1.4 Update `UserMemento` mapping
- [ ] 1.5 Domain unit tests + persistence integration test (TestContainers Postgres)

## 2. Token validation service

- [ ] 2.1 Create `IcalTokenService` (application layer): `generate(userId)`, `regenerate(userId)`, `validate(rawToken) -> Optional<UserId>`
- [ ] 2.2 `validate` looks up by token hash — to keep it constant-time, brute-force the candidate users (small N) or, more practically, store an additional non-secret prefix (first 8 chars of raw) as searchable lookup index
- [ ] 2.3 Unit tests for generate / regenerate / validate flows including invalid token handling

## 3. Schedule data assembly (reuse `EventScheduleQuery`)

- [ ] 3.1 Inject `EventScheduleQuery` (existing port in `com.klabis.events`) into the iCal export application service — same port the calendar module uses for the "Můj rozvrh" web filter
- [ ] 3.2 Default feed window: configuration properties `klabis.ical.window.past=P30D`, `klabis.ical.window.future=P12M`; compute `[today - past, today + future]` per request
- [ ] 3.3 Application service: `getMySchedule(userId, now)` → resolve `User → MemberId` → call `findEventIdsForMemberSchedule(memberId, from, to)` → load `Event` aggregates by ids
- [ ] 3.4 For each event, determine the user's role for the description text: `event.coordinatorId().equals(memberId)` → mark as coordinator; otherwise participant. Use the already-loaded `Event` aggregates — no extra DB queries
- [ ] 3.5 Unit tests covering: user with only registrations, only coordinator role, both, neither (empty list), event present for both participant and coordinator role appears exactly once

## 4. iCalendar serialization

- [ ] 4.1 Create `ICalendarRenderer` utility: takes `User` + list of `(Event, isCoordinator)` pairs + base URL, returns iCalendar text (UTF-8, CRLF line endings)
- [ ] 4.2 VEVENT mapping: UID = event UUID, DTSTAMP = now UTC, DTSTART = event date (whole-day), DTEND = event date + 1, SUMMARY = name, LOCATION = location (if present), URL = event detail URL, DESCRIPTION = organizer + Klabis link + optional event website + "Role: koordinátor" line when `isCoordinator`, STATUS = CANCELLED for cancelled events
- [ ] 4.3 Escape rules for SUMMARY/LOCATION/DESCRIPTION: backslash, comma, semicolon, newline (per RFC 5545)
- [ ] 4.4 Unit tests with golden output: 0 events, 1 event registered, 1 event coordinated, event with both roles, cancelled event, special characters in name/location

## 5. Feed endpoint

- [ ] 5.1 Add Spring Security filter chain matcher `/ical/**` ordered before SPA fallback; chain bypasses JWT auth and delegates to `IcalTokenService.validate(token)` from query param
- [ ] 5.2 Controller `IcalFeedController` with `@GetMapping("/ical/my-schedule.ics")`; reads `?token=`, validates, calls the application service, renders feed
- [ ] 5.3 Response: `Content-Type: text/calendar; charset=UTF-8`; `Cache-Control: max-age=600`
- [ ] 5.4 Integration tests: valid token → 200 with iCal body; invalid token → 401; user with empty schedule → 200 empty calendar; event with coordinator-only role present; cancelled events → STATUS:CANCELLED in output

## 6. Token management API

- [ ] 6.1 `GET /api/me/ical-token` — returns `{ url, lastSetAt }` if token exists, else `{ url: null }`; URL is the masked subscribe URL (last 4 chars only) plus a separate `revealUrl` link
- [ ] 6.2 `POST /api/me/ical-token` — generate or regenerate token; returns the raw URL once (with full token); updates `lastSetAt`
- [ ] 6.3 Integration tests for both endpoints (authenticated, regular user only)

## 7. Frontend — profile section

- [ ] 7.1 In `MyProfile` page add "Kalendářový feed" section
- [ ] 7.2 If no token: show "Vytvořit kalendářový feed" button → POST regenerate → display the URL once with copy button
- [ ] 7.3 If token exists: show masked URL + "Zobrazit celou URL" toggle + "Kopírovat" + "Vygenerovat nový token" (with confirm dialog warning that the previous URL will stop working)
- [ ] 7.4 Help text in CS with brief instructions for Google Calendar / Apple Calendar / Outlook (link by URL → "subscribe to calendar"); mention that the feed mirrors the "Můj rozvrh" filter (registrations + events you coordinate)
- [ ] 7.5 Localization labels in `src/localization/labels.ts`
- [ ] 7.6 Frontend tests for the section

## 8. SPA filter routing exclusion (depends on review-1-1)

- [ ] 8.1 Ensure `/ical/...` is in the SPA fallback exclusion list (proposal 1.1) so the feed URL is never captured by the SPA
- [ ] 8.2 Browser smoke test: open `https://api.klabis.otakar.io/ical/my-schedule.ics?token=<valid>` directly — expect download of iCal file or rendering as plain text (not SPA)

## 9. End-to-end verification

- [ ] 9.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 9.2 Generate token in profile, copy URL
- [ ] 9.3 Open URL in a browser tab — confirm `text/calendar` content with at least one VEVENT for a known registered event AND a known coordinated event for a seeded user with both roles
- [ ] 9.4 Add the URL to Google Calendar (test account); wait for first sync; confirm the event appears on the correct date
- [ ] 9.5 Assign coordinator role to the test user on a new event; refresh; confirm the event appears in the calendar with "Role: koordinátor" in the description
- [ ] 9.6 Cancel one of the events in Klabis; refresh the calendar manually; confirm the event is marked cancelled (or removed, depending on Google Calendar's STATUS:CANCELLED handling)
- [ ] 9.7 Unregister from another event (and verify the user is not its coordinator); refresh; confirm the event disappears from the calendar
- [ ] 9.8 Regenerate token in profile; confirm the old URL stops working (401)

## 10. Documentation

- [ ] 10.1 Update `docs/developerManual` with feed URL format, token lifecycle, the shared "Můj rozvrh" semantics (link to archived `calendar-my-schedule-filter`), and operational notes (caching, leak handling)
- [ ] 10.2 Sync new spec into `openspec/specs/ical-export/spec.md` after archiving
