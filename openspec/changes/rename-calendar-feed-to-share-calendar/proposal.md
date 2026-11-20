# Proposal

## Why

Terms "kalendářový feed", "token" and "Subscribe URL" are technical and unclear to ordinary club members (feedback from Alfa, issue #444). The calendar-sharing dialog should speak plain language.

## What Changes

- Calendar page button and dialog title: "Kalendářový feed" → "Zpřístupnit kalendář".
- Create action: "Vytvořit kalendářový feed" → "Vytvořit odkaz pro kalendář".
- Regenerate action (and its confirmation dialog): "Vygenerovat nový token" → "Vytvořit nový odkaz".
- Dialog and help texts: "token", "Subscribe URL", "feed" replaced by "odkaz".
- No functional change (API, token handling, feed content stay the same).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `calendar-items`: requirement "Personal Calendar Feed for „Můj rozvrh"" — user-facing action and dialog names in the requirement text and scenarios change.

## Impact

- Frontend: `frontend/src/localization/labels.ts` (`calendarFeed`), tests `CalendarPage.test.tsx`, `CalendarFeedSection.test.tsx`.
- Backend, API, HAL affordances: no change.
- Spec: `openspec/specs/calendar-items/spec.md`.
