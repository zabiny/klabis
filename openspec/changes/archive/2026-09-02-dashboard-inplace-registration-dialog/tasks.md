## 1. Backend — shared-offer flags on the event summary

- [x] 1.1 Add `sharedTransportEnabled` and `sharedAccommodationEnabled` (plain booleans, same shape as on `EventDto`) to `EventSummaryDto` in `docs/openapi/spec/events.yaml`
- [x] 1.2 Regenerate codegen DTO types (`./gradlew` openapi generation) and verify `EventSummaryDto` carries the new fields; `EventSummaryDtoConverter` maps them by name from the domain `Event`
- [x] 1.3 Add/extend a backend test asserting the events list response rows expose both flags (TDD: test first, then confirm mapping)

## 2. Frontend — customized dialog in the dashboard widget

- [x] 2.1 Regenerate frontend API types (`npm run openapi`)
- [x] 2.2 Extend `UpcomingDeadlineItem` in `useUpcomingDeadlines.ts` with `deadlines` array, `location`, `sharedTransportEnabled`, `sharedAccommodationEnabled`; keep the picked relevant deadline for row display
- [x] 2.3 Replace the generic `RegistrationModal` in `UpcomingDeadlinesWidget.tsx` with `EventRegistrationDialog` (`mode="new"`, row template, `prefillHref` from `newRegistration` link, `event` from the row, `onRegistered` refetches the widget query)
- [x] 2.4 Update `UpcomingDeadlinesWidget.test.tsx`: dialog swap test (customized dialog opens, no navigation), offer-flag pass-through, refresh-after-register
- [x] 2.5 Run frontend tests via test-runner agent (sequential) and `npm run build`

## 3. Verification and delivery

- [x] 3.1 Run backend events-module tests via test-runner agent (sequential, no parallel Gradle)
- [x] 3.2 Manual QA on http://localhost:3000: dashboard "Přihlásit se" opens the customized dialog with context header and offer-aware checkboxes; registering removes the row after refresh
- [x] 3.3 Code review agent pass, `npm run refresh-backend-server-resources`, commit via git-operator
