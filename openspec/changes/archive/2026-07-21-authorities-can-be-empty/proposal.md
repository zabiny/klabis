## Why

`AuthorityValidator` in `common.users` rejects an empty authorities set with `IllegalArgumentException`, even though the `UserPermissions` aggregate already treats an empty set as a valid state (`UserPermissions.empty()`, used for every newly created user). This extra guard is redundant internal validation that doesn't correspond to any documented requirement — it just duplicates/narrows what the aggregate already allows.

## What Changes

- Remove the empty-set check from `AuthorityValidator.validate(Set<String>)` and `AuthorityValidator.validateAuthorityEnums(Set<Authority>)` (`backend/src/main/java/com/klabis/common/users/domain/AuthorityValidator.java`).
- Update `AuthorityValidatorTest` to remove/adjust the two tests asserting `IllegalArgumentException("At least one authority required")` for empty sets.
- Remove the `@NotEmpty` Jakarta Bean Validation annotation from `UpdatePermissionsRequest.authorities` (`backend/src/main/java/com/klabis/common/users/infrastructure/restapi/PermissionController.java:164-165`). This is a second, independent enforcement point: since the controller method uses `@Valid`, this annotation alone would keep rejecting empty authorities with a 400 even after `AuthorityValidator` is relaxed — without removing it, the change would not actually achieve its stated goal at the API layer.
- No change to `PermissionServiceImpl.updateUserPermissions` logic, or `AuthorizationPolicy.checkAdminLockoutPrevention` — the existing lockout guard (last holder of `MEMBERS:PERMISSIONS` cannot be stripped of it) is untouched and keeps enforcing its own rule independently.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/users/spec.md` — "Update User Permissions API" (Requirement, line 425-438) only specifies that submitted authorities replace the user's direct authorities, and that removing the last `MEMBERS:PERMISSIONS` holder is rejected. It does not require rejecting an otherwise-empty authorities list. "User Permissions Aggregate" (line 358-366) already documents that a user can hold no authorities ("New user starts with no authorities"). Removing the redundant validator check does not contradict or narrow any scenario in this spec.
- `openspec/specs/member-permissions-dialog/spec.md` — reviewed; describes frontend dialog behavior for editing permissions, does not assert a non-empty constraint.

**Why no spec update is needed:**
The change removes an internal guard that was stricter than what the spec and the `UserPermissions` aggregate already allow. The one functional protection actually described in the spec — admin lockout prevention on `MEMBERS:PERMISSIONS` — is implemented separately in `AuthorizationPolicy` and is not affected by this change.

**Note discovered during implementation:** `PermissionServiceImpl.updateUserPermissions` always merges in the standard authorities (`Authority.withStandard`, currently `MEMBERS_READ` + `EVENTS_READ`) before saving. This means submitting an empty authorities set via the API never results in a truly empty persisted set — it results in exactly the standard set. This doesn't change the conclusion above (no spec scenario is violated), it just means "empty authorities" in practice bottoms out at the standard baseline rather than literally nothing.

## Impact

- `backend/src/main/java/com/klabis/common/users/domain/AuthorityValidator.java`
- `backend/src/test/java/com/klabis/common/users/AuthorityValidatorTest.java`
- `backend/src/main/java/com/klabis/common/users/infrastructure/restapi/PermissionController.java` (remove `@NotEmpty` on `UpdatePermissionsRequest.authorities`)
- API contract change: the `PATCH`/`PUT` update-permissions endpoint no longer returns 400 for an empty `authorities` list; it now accepts and persists it (subject to the existing admin lockout guard). No frontend changes required — the frontend already only submits full replace lists, and the empty-authorities case matches the "New user starts with no authorities" state already handled elsewhere in the UI.
