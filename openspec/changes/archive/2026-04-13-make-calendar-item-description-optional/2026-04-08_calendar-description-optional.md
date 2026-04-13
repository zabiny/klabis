# TCF: make-calendar-item-description-optional

**Started:** 2026-04-08
**Team lead:** Opus 4.6

## Purpose

Shared coordination file for subagents implementing this proposal. Every subagent MUST:
1. Read this file first to understand current state
2. Append a concise summary of their changes / issues at the end of this file when finished

## Proposal summary

Make `description` optional on manual calendar items (create + update). Normalize empty/whitespace to `null` at API boundary. Preserve 1000-char limit. Detail view must hide the field when null. Event-linked items are unaffected.

Key design decisions (already resolved in design.md):
- Normalize blank → null at controller/DTO mapper layer (not in domain).
- Remove `validateDescription()` helper entirely from `CalendarItem`.
- Update V001 migration in-place (no new migration script — project policy).

## Plan

Single iteration (small vertical slice, one module):
1. **Backend** (backend-developer): tasks 1–6 from tasks.md (domain relax, controller normalization, schema update, repository round-trip test, HAL-Forms metadata check). Follow TDD.
2. **Frontend** (frontend-developer): tasks 7 from tasks.md (detail view handles null description).
3. **Simplify** skill over the diff.
4. **Code review** + fix high-priority findings.
5. **Test runner** — green on backend + frontend.
6. **Commit**.

Task 8 (manual QA walkthrough) is not blocking — will be left to user/follow-up since the proposal workflow covers it via QA skill after commit.

## Progress log

### 2026-04-09 — backend-developer (tasks 1–6)

**What changed:**
- `CalendarItem.java`: Removed `@NotBlank` from `CreateCalendarItem.description` and `UpdateCalendarItem.description` (kept `@Size(max=1000)`). Removed `validateDescription()` call from `create()` and `update()`, and deleted the helper method. Added Javadoc clarifying null = "no description".
- `CalendarController.java`: Added two private `normalizeDescription()` overloads (one for Create, one for Update) that convert blank/whitespace-only description to `null` before delegating to the service. Called from `createCalendarItem()` and `updateCalendarItem()`.
- `V001__initial_schema.sql`: Changed `description TEXT NOT NULL` to `description TEXT NULL` on `calendar_items` table (in-place per project policy).
- `CalendarItemAssert.java`: Fixed `hasDescription()` to handle `null` correctly (NPE when actual is null).
- **Tests added:** 6 new domain unit tests (create/update with null description, invariants still enforced), 3 new `@WebMvcTest` controller tests (empty string and whitespace normalization on create; empty string on update), 1 `@DataJdbcTest` round-trip test (null description persists and reloads as null), 2 HAL-Forms metadata tests (description not required on create and update templates).

**Surprises / deviations:**
- `CalendarItemAssert.hasDescription()` used `actual.getDescription().equals(expected)` which NPE'd when description is null — had to fix the assert helper as part of GREEN phase (not in tasks.md but necessary).
- Task 4.2 (manual Flyway restart verification) left unchecked — requires a running local backend, outside automated test scope.

**Test status:** 110/110 calendar tests pass (all calendar test classes green).

### 2026-04-08 — backend-developer (code review fixes)

Two findings from code review resolved in `CalendarControllerTest.java`:
- **Finding 1 (HIGH):** Added `hasSize(1)` presence assertion before the `not(contains(true))` required check for both `createCalendarItem` and `updateCalendarItem` templates — prevents false-negative when `description` property is absent from the template entirely.
- **Finding 2 (MEDIUM):** Added `shouldNormalizeWhitespaceDescriptionToNullOnUpdate` test in `UpdateCalendarItemTests` — mirrors the existing whitespace-only create test, using an `ArgumentCaptor` to verify `description` is null after PUT with `"   "`.

Test status: 25/25 `CalendarControllerTest` tests pass.

### 2026-04-08 — frontend-developer (task 7)

**Components touched:**
- `frontend/src/pages/GenericHalPage.tsx` — `GenericItemDisplay`: added `value !== null && value !== undefined` to the `Object.entries` filter so null fields are omitted from the properties table entirely. Calendar item detail navigates to the `GenericHalPage` catch-all; this fix covers the spec scenario "Member views a calendar item without a description".
- `frontend/src/pages/calendar/CalendarPage.tsx` — fixed `description: string` → `string | null` in the `CalendarItem` interface; fixed tooltip: `title={item.description ? \`${item.name}\n${item.description}\` : item.name}` so no "null" leaks into the hover text.

**Approach:** Idiomatic null guard in both places — no framework-specific mechanism was needed; the generic item display is a plain `Object.entries` filter, and the tooltip uses a ternary.

**Tests added:** 2 new tests (RED-GREEN TDD cycle followed):
1. `GenericHalPage.test.tsx` — "should not render a row for null fields in single item view"
2. `CalendarPage.test.tsx` — "should not show 'null' in tooltip when calendar item has no description"

**Check status:** 1077/1077 frontend tests pass; `npm run build` (TypeScript) clean; lint errors in changed files are all pre-existing.
