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

## 3. iCalendar serialization

- [ ] 3.1 Create `ICalendarRenderer` utility: takes `User` + list of registered events + base URL, returns iCalendar text (UTF-8, CRLF line endings)
- [ ] 3.2 VEVENT mapping: UID = event UUID, DTSTAMP = now UTC, DTSTART = event date (whole-day), DTEND = event date + 1, SUMMARY = name, LOCATION = location (if present), URL = event detail URL, DESCRIPTION = organizer + Klabis link + optional event website, STATUS = CANCELLED for cancelled events
- [ ] 3.3 Escape rules for SUMMARY/LOCATION/DESCRIPTION: backslash, comma, semicolon, newline (per RFC 5545)
- [ ] 3.4 Unit tests with golden output: 0 events, 1 event, multiple events, cancelled event, special characters in name/location

## 4. Feed endpoint

- [ ] 4.1 Add Spring Security filter chain matcher `/ical/**` ordered before SPA fallback; chain bypasses JWT auth and delegates to `IcalTokenService.validate(token)` from query param
- [ ] 4.2 Controller `IcalFeedController` with `@GetMapping("/ical/my-registrations.ics")`; reads `?token=`, validates, looks up user's registered events, renders feed
- [ ] 4.3 Response: `Content-Type: text/calendar; charset=UTF-8`; `Cache-Control: max-age=600`
- [ ] 4.4 Integration tests: valid token → 200 with iCal body; invalid token → 401; user with 0 registrations → 200 empty calendar; cancelled events → STATUS:CANCELLED in output

## 5. Token management API

- [ ] 5.1 `GET /api/me/ical-token` — returns `{ url, lastSetAt }` if token exists, else `{ url: null }`; URL is the masked subscribe URL (last 4 chars only) plus a separate `revealUrl` link
- [ ] 5.2 `POST /api/me/ical-token` — generate or regenerate token; returns the raw URL once (with full token); updates `lastSetAt`
- [ ] 5.3 Integration tests for both endpoints (authenticated, regular user only)

## 6. Frontend — profile section

- [ ] 6.1 In `MyProfile` page add "Kalendářový feed" section
- [ ] 6.2 If no token: show "Vytvořit kalendářový feed" button → POST regenerate → display the URL once with copy button
- [ ] 6.3 If token exists: show masked URL + "Zobrazit celou URL" toggle + "Kopírovat" + "Vygenerovat nový token" (with confirm dialog warning that the previous URL will stop working)
- [ ] 6.4 Help text in CS with brief instructions for Google Calendar / Apple Calendar / Outlook (link by URL → "subscribe to calendar")
- [ ] 6.5 Localization labels in `src/localization/labels.ts`
- [ ] 6.6 Frontend tests for the section

## 7. SPA filter routing exclusion (depends on review-1-1)

- [ ] 7.1 Ensure `/ical/...` is in the SPA fallback exclusion list (proposal 1.1) so the feed URL is never captured by the SPA
- [ ] 7.2 Browser smoke test: open `https://api.klabis.otakar.io/ical/my-registrations.ics?token=<valid>` directly — expect download of iCal file or rendering as plain text (not SPA)

## 8. End-to-end verification

- [ ] 8.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 8.2 Generate token in profile, copy URL
- [ ] 8.3 Open URL in a browser tab — confirm `text/calendar` content with at least one VEVENT for a known registered event
- [ ] 8.4 Add the URL to Google Calendar (test account); wait for first sync; confirm the event appears on the correct date
- [ ] 8.5 Cancel one of the events in Klabis; refresh the calendar manually; confirm the event is marked cancelled (or removed, depending on Google Calendar's STATUS:CANCELLED handling)
- [ ] 8.6 Unregister from another event; refresh; confirm the event disappears from the calendar
- [ ] 8.7 Regenerate token in profile; confirm the old URL stops working (401)

## 9. Documentation

- [ ] 9.1 Update `docs/developerManual` with feed URL format, token lifecycle, and operational notes (caching, leak handling)
- [ ] 9.2 Sync new spec into `openspec/specs/ical-export/spec.md` after archiving
