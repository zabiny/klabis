# Tasks

## 1. Backend and API spec: `ical-token` link on the calendar collection

- [ ] 1.1 Update `IcalTokenControllerTest`/`CalendarControllerTest` first: calendar items list contains `ical-token` link with `generateToken` affordance for any authenticated user; verify the new test fails
- [ ] 1.2 Add `ical-token` link in `CalendarItemListPostprocessor` and verify the test from 1.1 passes
- [ ] 1.3 Remove `IcalTokenMemberDetailLinkProcessor` and its test; drop the `ical-token` expectation from `MemberControllerApiTest`; verify member detail has no `ical-token` link
- [ ] 1.4 Update `docs/openapi/spec/members.yaml` and `docs/openapi/spec/calendar.yaml`, regenerate the bundle and frontend types, and verify `./gradlew build` for the module compiles and the spec checks pass
- [ ] 1.5 Run calendar and members backend tests via test-runner and verify they pass

## 2. Frontend: feed dialog on the calendar page

- [ ] 2.1 Move `CalendarFeedSection` (and its test) to `pages/calendar`, render it as dialog content with inline regeneration confirmation; verify its tests cover create, masked URL with last-set date, reveal-and-copy, inline confirm/cancel of regeneration, and discarding the full URL after close
- [ ] 2.2 Add the "Kalendářový feed" button to the `CalendarPage` header, shown only when the `ical-token` link is present; verify with `CalendarPage.test.tsx` (button shown with link, hidden without, opens the dialog)
- [ ] 2.3 Remove the feed section and `ical-token` handling from `MemberDetailPage`; update `MemberDetailPage.test.tsx` and verify the profile shows no feed section
- [ ] 2.4 Update labels in `localization/labels.ts` (button/dialog title) and verify `npm run build` and the touched vitest files pass (one file per command)

## 3. Integration

- [ ] 3.1 Verify on http://localhost:3000 as `ZBM9500` and as `admin`: calendar header shows the button, the dialog creates and regenerates the token, the profile has no feed section
- [ ] 3.2 Run `openspec validate move-calendar-feed-to-calendar-page --strict` and verify it passes
