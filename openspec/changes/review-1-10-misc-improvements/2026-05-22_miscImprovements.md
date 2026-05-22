# TCF — review-1-10-misc-improvements

Team coordination file. Each subagent: read this file first, append a concise summary of changes/issues at the end.

## Plan

3 vertical slices, each independently committable:
- **Slot N1** — Change password while authenticated (backend + frontend)
- **Slot N4** — Filter events by year (frontend only)
- **Slot K1** — Rename "Koordinátor" → "Vedoucí" (frontend labels, possibly backend HAL templates)

## Progress log

(subagents append below)

---

### Slot N1 — Backend: Change Password While Authenticated (tasks 1.1–1.4) ✅

**Files changed:**
- `domain/PasswordChangedEvent.java` — new `@DomainEvent` record with `fromUser(User)` factory
- `domain/IncorrectCurrentPasswordException.java` — new `InvalidDataException` subclass (→ HTTP 400)
- `domain/User.java` — added `changePassword(newPasswordHash)` method; returns new instance, registers `PasswordChangedEvent`
- `application/PasswordChangePort.java` — new `@PrimaryPort` with nested `ChangePasswordCommand` record
- `application/PasswordChangeService.java` — `@Service` impl: verifies current password via `passwordEncoder.matches`, validates via `PasswordComplexityValidator.validateBasic`, hashes and saves
- `infrastructure/restapi/PasswordChangeController.java` — `POST /api/me/password-change`, authenticated via `Authentication` parameter (cast to `KlabisJwtAuthenticationToken`), returns 204 No Content
- `UserTest.java` — added 4 domain unit tests for `changePassword()`
- `PasswordChangeControllerTest.java` — 10 E2E `@E2ETest` integration tests

**Key decisions:**
- Used `Authentication` parameter (not `@ActingUser UserId`) in controller because `common.users` module shouldn't depend on `members` module where `@ActingUser` lives; `CurrentUserArgumentResolver` is in `members.infrastructure.mvc`.
- `PasswordComplexityValidator.validateBasic()` used (not the overload with personal info) — matches what `completePasswordSetup` does for the initial setup.
- `IncorrectCurrentPasswordException` extends `InvalidDataException` → caught by global `MvcExceptionHandler.handleInvalidDataException` → HTTP 400.
- No session invalidation after change (per design decision).

**Tests:** 10 integration tests pass, 39 domain tests pass (including 4 new).

---

### Slot N1 — Frontend: Change Password Dialog (tasks 1.5–1.6) ✅

**Files changed:**
- `src/localization/labels.ts` — added `changePassword` label group (button, dialog title, field labels, error messages)
- `src/components/auth/ChangePasswordDialog.tsx` — new dialog component with 3 fields (current password, new password, confirm); reuses `PasswordField` + `PasswordStrengthIndicator` from password setup form; client-side complexity + confirmation validation; POSTs to `/api/me/password-change`; maps "Incorrect current password" server error to localized message; clears server error on typing in current password field
- `src/components/auth/ChangePasswordDialog.test.tsx` — 19 tests covering dialog visibility, form fields, client-side validation (complexity, mismatch, empty current), successful submission (payload + onClose), server-side error handling (incorrect password → specific message, other → generic), clear-on-type, cancel, disabled state
- `src/pages/members/MemberDetailPage.tsx` — added "Změnit heslo" button (with `KeyRound` icon) in the action button row; button shown only when `ical-token` link is present (own profile); wires to `ChangePasswordDialog`

**Key decisions:**
- Change password button gated on `ical-token` link presence — same heuristic as CalendarFeedSection; both are own-profile-only features
- `extractServerErrorMessage` uses duck-typed property check (`responseBody`) instead of `instanceof FetchError` to work correctly in tests where errors are plain `Error` objects with `responseBody` property injected

**Tests:** 1508/1508 frontend tests pass; TypeScript type-check clean

---

### Slot N1 — Code Review Findings

**CRITICAL:**
- `frontend/src/components/auth/ChangePasswordDialog.tsx:44` — `INCORRECT_CURRENT_PASSWORD_MARKER = 'Incorrect current password'` does NOT match the backend exception message `"Current password is incorrect"` (`IncorrectCurrentPasswordException.java:11`). The `includes()` check always fails; the generic fallback error always shows instead of the localized message. Fix: align both strings (e.g., change marker to `'Current password is incorrect'`).

**HIGH:**
- `backend/.../application/PasswordChangeService.java:35` — `UserNotFoundException` thrown when user from JWT is not found has no global handler in `MvcExceptionHandler`, resulting in HTTP 500. While unlikely in practice (JWT implies a valid user), this breaks the principle of explicit error handling. Either map `UserNotFoundException` to `ResourceNotFoundException` or add a global handler. This exception also leaks the `userId` value in the 500 response body.

**MEDIUM:**
- `frontend/src/localization/labels.ts:396` — `successMessage: 'Heslo bylo úspěšně změněno.'` is defined but never used in `ChangePasswordDialog.tsx`. The dialog closes on success without showing a confirmation. Either remove the unused label or implement a success toast/message.

**LOW:**
- `backend/.../restapi/PasswordChangeController.java:43` — unchecked cast `(KlabisJwtAuthenticationToken) authentication` has no guard. If the authentication object is not of this type (e.g., in a misconfigured test or future auth change), it throws `ClassCastException` → 500 instead of a clear error. The cast is safe under current JWT config, but a defensive `instanceof` check or a comment documenting the assumption would improve robustness.
- `frontend/src/components/auth/ChangePasswordDialog.test.tsx` — the mock setup for `useAuthorizedMutation` is duplicated inline in 4 test cases (`shows server error message inline when mutation fails`, `shows generic error message for non-password errors`, `clears server error when user starts typing`, `calls onClose after successful password change`). These could share a helper similar to `mockMutation()` that captures the `onSuccess`/`onError` callbacks.

---

### Slot N1 — Code Review Fix (backend, HIGH)

`UserNotFoundException` in `common.users.domain` was changed to extend `ResourceNotFoundException` (from `RuntimeException`), aligning it with the project-wide convention used by `MemberNotFoundException`, `GroupNotFoundException`, and others. `MvcExceptionHandler` already handles `ResourceNotFoundException` globally and maps it to HTTP 404, so no new handler was needed. The message in `PasswordChangeService` was also sanitised from `"User not found: " + command.userId()` to a generic `"User not found"` — the user ID is now never included in the client-facing error body. The local `@ExceptionHandler(UserNotFoundException.class)` in `PermissionController` remains valid (more-specific handler takes precedence) and is out of scope for this change. All 432 backend tests pass.

---

---

### Slot N4 — Filter Events by Year (tasks 2.1–2.6) ✅

**Files changed:**
- `src/components/events/eventsFilterUtils.ts` — added `yearToDateParams(year)`, `getYearFromParams(dateFrom, dateTo)`, `getYearRange()` utility functions
- `src/components/events/EventsFilterBar.tsx` — added `selectedYear: number | null` to `EventsFilterValue`; added `<select>` year dropdown (label "Rok", options "—" + currentYear±range); time window change clears `selectedYear`; year change forces `timeWindow: 'vse'`
- `src/pages/events/useEventsFilterState.ts` — derives `selectedYear` via `getYearFromParams` on URL load; `handleFilterChange` routes to `yearToDateParams` (when year set) or `timeWindowToDateParams` (when year cleared); `filterValue` exposes `selectedYear`
- `src/localization/labels.ts` — added `eventsFilterYear: 'Rok'` and `noYear: '—'` to `eventsFilter` group
- `src/components/events/eventsFilterUtils.test.ts` — 7 new tests for `yearToDateParams`, `getYearFromParams`, `getYearRange`
- `src/components/events/EventsFilterBar.test.tsx` — 9 new tests for year selector rendering, default state, year selection (forces `timeWindow=vse`), clear, mutual-exclusion with time window
- `src/pages/events/useEventsFilterState.test.ts` — new test file, 9 tests covering URL-load derivation, year selection URL params, clear behavior

**Key decisions:**
- `selectedYear` lives in `EventsFilterValue` alongside `timeWindow`; the component enforces mutual exclusion: selecting a year forces `timeWindow='vse'`, clicking a time window pill clears `selectedYear=null`
- URL always stores `dateFrom`/`dateTo` directly (no `?year=` alias) per design decision; frontend derives the displayed year from the range on load
- Year range generated dynamically from `new Date().getFullYear()` — no hardcoded values

**Tests:** 1535/1535 frontend tests pass; TypeScript type-check and production build clean

---

### Slot N1 — Code Review Fix (frontend, CRITICAL + MEDIUM)

Two frontend findings addressed: (1) `INCORRECT_CURRENT_PASSWORD_MARKER` in `ChangePasswordDialog.tsx` corrected from `'Incorrect current password'` to `'Current password is incorrect'` to match the actual backend exception message — the `includes()` check now matches and the localized Czech error message shows correctly for a wrong current password; the two test cases that mocked the old (wrong) backend string were updated accordingly. (2) Dead label `successMessage: 'Heslo bylo úspěšně změněno.'` removed from `labels.changePassword` in `labels.ts` — confirmed unused by grep across all source files. All 19 `ChangePasswordDialog` tests pass; TypeScript type-check and production build are clean.

---

### Slot N4 — Code Review Findings

**BLOCKING:**

None.

**WARNING:**

- `frontend/src/pages/events/EventsPage.tsx:242` — `key={filterValue.timeWindow}` was not updated to incorporate `selectedYear`. When the user changes year (timeWindow stays `'vse'`), the table key does not change, so the table is NOT remounted. If the original behavior relied on key-change to reset pagination/sort state, changing between different year values won't trigger that reset. Consider `key={filterValue.selectedYear ?? filterValue.timeWindow}` or a combined key.

- `frontend/src/components/events/eventsFilterUtils.ts:77` — `getYearFromParams` validates suffix patterns (`endsWith('-01-01')`, `endsWith('-12-31')`) but does not validate that the parsed year is a valid integer (e.g., `NaN` if `dateFrom` is `'xxxx-01-01'`). `parseInt('xxxx', 10)` returns `NaN`; the `fromYear !== toYear` check passes for two `NaN` values because `NaN !== NaN` is `true` in JS, so `null` is returned — this accidentally produces the correct result, but it is a fragile coincidence rather than an explicit guard. Consider adding `Number.isNaN(fromYear)` check for correctness and readability.

- `frontend/src/pages/events/useEventsFilterState.ts:38` — when `selectedYear` is not null (i.e., URL has a full year range), `timeWindow` is hard-coded to `'vse'`. However, the `defaultSort` at line 100 is derived from this `timeWindow`. With a year selected, `timeWindow='vse'` → sort defaults to `desc` (most recent first), which is correct for a year view. This is fine but worth noting: the sort default is not independently configurable for the year-filter mode; it borrows `'vse'` semantics. Not a bug, but worth a design note.

**SUGGESTION:**

- `frontend/src/components/events/EventsFilterBar.tsx:29` — `YEAR_OPTIONS` is computed at module-load time via `getYearRange()`, which calls `new Date().getFullYear()`. This is evaluated once when the module is first imported. For a long-lived browser session spanning midnight on Dec 31, the year options would be stale until page reload. In practice this is not a real issue (users don't leave the app open across year boundaries), but making it a lazy `useMemo` with no deps would be strictly correct. Low priority.

- `frontend/src/components/events/eventsFilterUtils.test.ts:73-75` — the test is labelled `'returns Budoucí as default when neither param is set (default state)'` but its assertion is `expect(...).toBe('vse')`. The description is misleading — it says "Budoucí" but asserts "Vše". Fix the description to accurately reflect that `getTimeWindowFromParams(null, null)` returns `'vse'` (not `'budouci'`).

- `frontend/src/pages/events/useEventsFilterState.test.ts:283-295` (EventsFilterBar.test.tsx) — the test `'does NOT reset timeWindow when "no year" is selected'` only asserts `selectedYear: null` but does not verify that the existing `timeWindow` value is preserved in the `onChange` call. The spec says clearing year SHALL restore the previous time-window. The test description promises this but the assertion body doesn't check `timeWindow` on the emitted value. A missing assertion weakens spec coverage.

- `frontend/src/components/events/EventsFilterBar.tsx:74-90` — the `<label>` has `htmlFor="events-year-filter"` and the `<select>` has both `id="events-year-filter"` and `aria-label`. The `aria-label` on the select is redundant when the `<label>` is already properly associated via `for`/`id`. Not a bug (both accessible), but the `aria-label` duplication adds noise.

---

### Slot N4 — Code Review Fix (frontend, WARNING + SUGGESTION)

Four review findings addressed: (1) `EventsPage.tsx:242` — table `key` changed from `filterValue.timeWindow` to `filterValue.selectedYear ?? filterValue.timeWindow` so switching between different years (where `timeWindow` stays `'vse'`) correctly remounts the table and resets pagination/sort state. (2) `eventsFilterUtils.ts` — `getYearFromParams` now has an explicit `Number.isNaN` guard before the year equality check, replacing the accidental-correctness of `NaN !== NaN`; a malformed date prefix now deterministically returns `null`. (3) `eventsFilterUtils.test.ts:73` — test description corrected from `'returns Budoucí as default when neither param is set (default state)'` to `'returns vse when neither param is set'` to match the actual assertion. (4) `EventsFilterBar.test.tsx:284-295` — added `expect(called.timeWindow).toBe('vse')` assertion to the "does NOT reset timeWindow" test, verifying the previous time-window value is preserved in the `onChange` payload when year is cleared. All 61 relevant tests pass; TypeScript type-check clean.

---

### Slot K1 — Rename "Koordinátor" → "Vedoucí" (tasks 3.1, 3.2, 3.4) ✅

**Backend:** No changes required. grep across all backend Java source confirmed zero hard-coded Czech "Koordinátor" strings in HAL template metadata or anywhere else. All backend coordinator references are identifier strings (`eventCoordinatorId`, `event_coordinator_id`, `coordinator`) — unchanged.

**Frontend files changed:**
- `src/localization/labels.ts` — three label values updated:
  - `fields.eventCoordinatorId: 'Koordinátor'` → `'Vedoucí'` (form field label + detail row; used in EventsPage create form and EventDetailPage)
  - `tables.coordinator: 'Koordinátor'` → `'Vedoucí'` (events table column header)
  - `sections.eventCoordination: 'KOORDINACE'` → `'VEDENÍ AKCE'` (section heading in create/edit form)
- `src/localization/labels.test.ts` — updated assertion for `fields.eventCoordinatorId` to `'Vedoucí'`
- `src/pages/events/EventDetailPage.test.tsx` — updated mock HAL template prompt for `eventCoordinatorId` to `'Vedoucí'`

**Task 3.4 verification:** `eventCoordinatorId` property name in OpenAPI types (`klabisApi.d.ts`), backend DTOs, domain code, and database column (`event_coordinator_id`) all untouched. ORIS import service (`OrisEventImportService.java`) has no coordinator references at all.

**Tests:** 1535/1535 frontend tests pass.

---

### Slot K1 — Code Review Findings

**BLOCKING:**

None.

**WARNING:**

- **Spec gap — filter bar coordinator filter does not exist:** The spec (spec.md, "Scenario: Filter bar label") requires the events filter bar to show a coordinator filter labelled "Vedoucí". However, `EventsFilterBar.tsx` has no coordinator filter control at all — the API supports a `coordinator` query param (`klabisApi.d.ts:3708`) but no UI exposes it. Task 3.3 (visual smoke test) is still open and should catch this, but it is noted here: the label change for a non-existent filter does not satisfy the spec requirement. Either the spec is aspirational (filter needs to be built as a separate task) or the spec intent is that "filter bar" referred to a different mechanism. Clarify before marking K1 fully complete.

**SUGGESTION:**

- `frontend/src/localization/labels.ts:416-417` — Two iCal help-text strings contain the word "koordinátora" in lowercase generic usage (describing a role in an explanatory sentence, not a UI label for the coordinator field). These are not in scope for K1 (different feature, different context), and their meaning as a role description is still accurate. No change needed, but flagging for awareness.
- `frontend/src/localization/labels.test.ts` — only `fields.eventCoordinatorId` is asserted; `tables.coordinator` and `sections.eventCoordination` have no assertions. Low risk since labels.ts is straightforward, but adding assertions for the two other renamed values would ensure regressions are caught.

---

### Slot N1 — Code cleanup (LOW review findings, follow-up commit)

Removed the hand-written `toString()` override from `PasswordChangedEvent` record (no redaction, identical to the default record `toString`). Removed the Javadoc on `User.changePassword()` that narrated what the code does rather than why. All 49 affected tests pass.

---

### Slot N1 — Code cleanup (change-password feature, review-1-10 follow-up)

Three code-quality fixes applied to the change-password frontend feature: (1) Introduced `isOwnProfile` boolean in `MemberDetailPage` (derived from `icalTokenHref != null`) so the "Změnit heslo" button is gated on an explicitly named condition rather than reusing the iCal variable name — intent is now clear without coupling to iCal naming. (2) Extracted a shared `buildPasswordRequirements(password)` function to `src/components/auth/passwordRequirements.ts`; a single `RULE_DEFINITIONS` array (id, label, test) is the sole source of truth for the 5 complexity rules, their regex patterns, and their Czech labels — both `ChangePasswordDialog` and `PasswordSetupForm` now import this helper, eliminating three duplicate copies. (3) Replaced `requirements` as React state in both components with `useMemo(() => buildPasswordRequirements(...), [password])`, removing manual sync in `updateField` and manual reset in `handleClose`. All 64 affected tests pass.

---

### Slot K1 — Finalization (code-review suggestion + task 3.3 closure)

**Labels test hardened:** Added two missing regression assertions to `labels.test.ts` — `labels.tables.coordinator` (expects `'Vedoucí'`) in the existing "has table headers" test, and a new "has section labels" test asserting `labels.sections.eventCoordination` equals `'VEDENÍ AKCE'`. All 25 tests in the file pass; TypeScript type-check clean.

**Task 3.3 closed:** Marked done in `tasks.md` with an inline note: the events filter bar has no coordinator filter control (the API exposes a `coordinator` query param but no UI widget surfaces it), so the spec's "Filter bar label" scenario is vacuously satisfied — nothing existed to rename. The three real UI places (table header, detail section, form field) are covered by the label tests. The missing coordinator filter is a separate out-of-scope feature, per user decision.
