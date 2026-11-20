# Tasks

## 1. Rename calendar sharing wording

- [x] 1.1 Update `CalendarPage.test.tsx` and `CalendarFeedSection.test.tsx` to expect new names ("Zpřístupnit kalendář", "Vytvořit odkaz pro kalendář", "Vytvořit nový odkaz"); verify they fail
- [x] 1.2 Update `calendarFeed` values in `frontend/src/localization/labels.ts` per design.md (no "token", "Subscribe URL", "feed" left in user-visible values); verify the two test files pass (`npx vitest run <file>` one file per command)
- [x] 1.3 Add a test asserting the dialog texts contain none of "token", "Subscribe URL", "feed" (case-insensitive); verify it passes
- [x] 1.4 Run `npm run build` in `frontend/`; verify it succeeds
- [x] 1.5 Check `frontend/e2e` and `docs/` for references to the old names; verify `grep -ri "kalendářový feed"` finds none outside archived specs
