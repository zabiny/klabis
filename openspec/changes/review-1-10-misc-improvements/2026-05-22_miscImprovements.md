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

### Slot N1 — Code Review Fix (frontend, CRITICAL + MEDIUM)

Two frontend findings addressed: (1) `INCORRECT_CURRENT_PASSWORD_MARKER` in `ChangePasswordDialog.tsx` corrected from `'Incorrect current password'` to `'Current password is incorrect'` to match the actual backend exception message — the `includes()` check now matches and the localized Czech error message shows correctly for a wrong current password; the two test cases that mocked the old (wrong) backend string were updated accordingly. (2) Dead label `successMessage: 'Heslo bylo úspěšně změněno.'` removed from `labels.changePassword` in `labels.ts` — confirmed unused by grep across all source files. All 19 `ChangePasswordDialog` tests pass; TypeScript type-check and production build are clean.
