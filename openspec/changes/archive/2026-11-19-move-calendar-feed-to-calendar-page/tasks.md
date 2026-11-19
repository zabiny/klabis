# Tasks

## 1. Backend and API spec: `ical-token` link on the calendar collection

- [x] 1.1 Update `IcalTokenControllerTest`/`CalendarControllerTest` first: calendar items list contains `ical-token` link with `generateToken` affordance for any authenticated user; verify the new test fails
- [x] 1.2 Add `ical-token` link in `CalendarItemListPostprocessor` and verify the test from 1.1 passes
- [x] 1.3 Remove `IcalTokenMemberDetailLinkProcessor` and its test; drop the `ical-token` expectation from `MemberControllerApiTest`; verify member detail has no `ical-token` link
- [x] 1.4 Update `docs/openapi/spec/members.yaml` and `docs/openapi/spec/calendar.yaml`, regenerate the bundle and frontend types, and verify `./gradlew build` for the module compiles and the spec checks pass
- [x] 1.5 Run calendar and members backend tests via test-runner and verify they pass

## 2. Backend: `changePassword` affordance on own member detail

- [x] 2.1 Write a test first: member detail contains `changePassword` link with affordance for the caller's own detail and not for another member's detail; verify it fails
- [x] 2.2 Add a member-detail postprocessor adding the `changePassword` link/affordance for the caller's own detail (respect module boundaries) and verify the test passes
- [x] 2.3 Add `changePassword` to the member detail links in `docs/openapi/spec/members.yaml`, regenerate bundle and frontend types, and verify calendar and members backend tests pass

## 3. Frontend: feed dialog on the calendar page

- [x] 3.1 Move `CalendarFeedSection` (and its test) to `pages/calendar`, render it as dialog content with inline regeneration confirmation; verify its tests cover create, masked URL with last-set date, reveal-and-copy, inline confirm/cancel of regeneration, and discarding the full URL after close
- [x] 3.2 Add the "Kalendářový feed" button to the `CalendarPage` header, shown only when the `ical-token` link is present; verify with `CalendarPage.test.tsx` (button shown with link, hidden without, opens the dialog)
- [x] 3.3 Remove the feed section and `ical-token` handling from `MemberDetailPage`; update `MemberDetailPage.test.tsx` and verify the profile shows no feed section
- [x] 3.4 Update labels in `localization/labels.ts` (button/dialog title) and verify `npm run build` and the touched vitest files pass (one file per command)

- [x] 3.5 Show "Změnit heslo" in `MemberDetailPage` based on the `changePassword` affordance instead of comparing member ids; update tests and verify the button shows on own profile and not on another member's

## 4. Integration

- [x] 4.1 Verify on http://localhost:3000 as `ZBM9500` and as `admin`: calendar header shows the button, the dialog creates and regenerates the token, the profile has no feed section
- [x] 4.2 Run `openspec validate move-calendar-feed-to-calendar-page --strict` and verify it passes
