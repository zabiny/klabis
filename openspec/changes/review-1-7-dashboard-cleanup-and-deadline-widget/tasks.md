## 1. Backend — extend EventFilter for the new widget query (N8)

- [ ] 1.1 Add `deadlineWithin: Optional<Period>` to `EventFilter` (in `com.klabis.events.domain`); semantic: events whose nearest future registration deadline falls within `[today, today + period]`
- [ ] 1.2 Add `notRegisteredBy: Optional<MemberId>` to `EventFilter`; semantic: events where the given member is NOT in the registrations
- [ ] 1.3 Update repository adapter (Spring Data JDBC) to translate the new filter fields into SQL (e.g. EXISTS / NOT EXISTS subqueries against registrations table; deadline check accounting for multiple deadlines from proposal 1.4 — use the nearest future deadline)
- [ ] 1.4 Integration tests: `deadlineWithin` returns events with deadlines in range; `notRegisteredBy` excludes registered events; combined filter returns expected subset
- [ ] 1.5 Update event list controller / DTO if needed to expose the new filter via query params (HAL+FORMS template metadata)

## 2. Frontend — UpcomingDeadlinesWidget (N8)

- [ ] 2.1 Create `UpcomingDeadlinesWidget.tsx` under `frontend/src/components/dashboard/`
- [ ] 2.2 Implement TanStack Query call to events endpoint with params: `status=ACTIVE`, `deadlineWithin=P7D`, `notRegisteredBy=me`, `size=5`, `sort=registrationDeadline,asc`
- [ ] 2.3 Render rows with event name, event date, formatted deadline ("Uzávěrka: DD. MM."), "Přihlásit se" button (links to event detail)
- [ ] 2.4 Hide widget entirely when result is empty (no rendering, no placeholder)
- [ ] 2.5 Integrate the widget into `Dashboard.tsx` / `UserDashboard.tsx` below the existing "Moje nadcházející akce" widget
- [ ] 2.6 Unit tests: widget renders with mock data (5 events, 2 events, 0 events); deadline format matches spec

## 3. Frontend — remove welcome block (K3)

- [ ] 3.1 Locate the welcome heading and tagline rendering in dashboard component (`Dashboard.tsx` or `UserDashboard.tsx`)
- [ ] 3.2 Remove "Vítejte v Klabis, [name]" heading and the "Moderní systém pro správu členského klubu" subtitle
- [ ] 3.3 Verify shortcut cards ("Můj profil", "Akce") remain in their current position below the welcome (now first content)
- [ ] 3.4 Verify AdminDashboard variant — if it has the same welcome block, remove it consistently; if it has a different layout, leave a note in PR description for follow-up
- [ ] 3.5 Frontend snapshot or visual test: dashboard renders without welcome block, layout still looks balanced

## 4. Localization

- [ ] 4.1 Add new labels to `frontend/src/localization/labels.ts`:
  - `dashboardUpcomingDeadlinesTitle = "Končící přihlášky tento týden"`
  - `dashboardDeadlinePrefix = "Uzávěrka:"`
  - any other strings used by the widget

## 5. End-to-end verification

- [ ] 5.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 5.2 Browser test (member with at least one event closing in 7 days that they are not registered to): verify widget visible, lists correct events ordered by deadline
- [ ] 5.3 Browser test (member with no deadlines this week): verify widget is hidden completely
- [ ] 5.4 Browser test (admin): verify dashboard does not show welcome heading; verify widget rendering same as for member if admin has a member profile
- [ ] 5.5 Click an event in the widget → expect event detail open
- [ ] 5.6 Click "Přihlásit se" in the widget → expect event detail open with registration form ready

## 6. Documentation

- [ ] 6.1 Update memory `project_dashboard.md` with the new widget and removed welcome block
- [ ] 6.2 Sync the spec change into `openspec/specs/dashboard/spec.md` after archiving
