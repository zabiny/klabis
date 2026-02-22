# Reorganize User Creation Methods

**Status:** READY_FOR_IMPLEMENTATION
**Created:** 2026-02-22

## Context

Currently the `User` aggregate has multiple `createPendingActivation*` factory methods and the `UserService` exposes `createUserPendingActivation(UserCreationParams)`. The password setup flow initialization is implicitly tied to `AccountStatus.PENDING_ACTIVATION` in `UserCreatedEventHandler`. The goal is to make the two creation paths explicit and self-descriptive: one for users who need to set their password, one for users created with an immediate active password.

## Requirements

- Add `User.createdUser(username)` static factory method — generates a random placeholder password hash internally, sets `AccountStatus.PENDING_ACTIVATION`, publishes `UserCreatedEvent`.
- Add `User.createdUser(username, passwordHash)` static factory method — accepts a pre-encoded password hash, sets `AccountStatus.ACTIVE`, does NOT publish `UserCreatedEvent` (or publishes an event that the password setup handler ignores).
- Expose two corresponding methods on `UserService` (and implement in `UserServiceImpl`):
  - `createUser(username, email, authorities)` — delegates to `User.createdUser(username)`, includes email in event for password setup.
  - `createUser(username, passwordHash, authorities)` — delegates to `User.createdUser(username, passwordHash)`, no password setup flow.
- `UserCreatedEventHandler` initiates password setup only when the event carries `AccountStatus.PENDING_ACTIVATION` (existing condition — keep it, no change needed if the ACTIVE-path event is not published or has ACTIVE status).
- Update `RegistrationServiceImpl` to use `userService.createUser(username, email, authorities)` instead of `createUserPendingActivation(UserCreationParams)`.
- Update `BootstrapDataLoader` to use `userService.createUser(username, passwordHash, authorities)` (or encode the password and call the new method directly if `UserService` is not injected there — prefer going through `UserService` for consistency).
- Remove the old `createUserPendingActivation` method and the `createPendingActivation*` factory methods on `User` once all callers are migrated. Migrate existing usages to one of newly added methods
- Remove `UserCreationParams` if it has no remaining usages after migration.

## Success Criteria

- [ ] `User.createdUser(username)` creates a user with `AccountStatus.PENDING_ACTIVATION` and a randomly generated password hash.
- [ ] `User.createdUser(username, passwordHash)` creates a user with `AccountStatus.ACTIVE` and the provided password hash.
- [ ] Member registration flow continues to trigger password setup email (E2E / integration test passes).
- [ ] Bootstrap admin user is created with `AccountStatus.ACTIVE` — no password setup email is sent.
- [ ] All existing tests pass without modification (or minimal adjustments for renamed methods only).
- [ ] Old `createUserPendingActivation` / `createPendingActivation*` methods are removed (no dead code left).

## Scope

**In scope:**
- `User` domain aggregate factory methods
- `UserService` interface and `UserServiceImpl`
- `RegistrationServiceImpl` (caller update)
- `BootstrapDataLoader` (caller update)
- Removal of `UserCreationParams` if unused

**Out of scope:**
- Password setup token generation logic — no changes to `PasswordSetupService` or `UserCreatedEventHandler` beyond what's needed to handle the new event shape.
- `UserCreatedEvent` schema changes — only add/keep `accountStatus` field (already present) to allow handler to distinguish paths.
- Any frontend or API contract changes.

## Notes

- Relevant files:
  - `backend/src/main/java/com/klabis/users/User.java`
  - `backend/src/main/java/com/klabis/users/UserCreatedEvent.java`
  - `backend/src/main/java/com/klabis/users/UserService.java`
  - `backend/src/main/java/com/klabis/users/application/UserServiceImpl.java`
  - `backend/src/main/java/com/klabis/users/UserCreationParams.java`
  - `backend/src/main/java/com/klabis/users/passwordsetup/UserCreatedEventHandler.java`
  - `backend/src/main/java/com/klabis/members/management/RegistrationServiceImpl.java`
  - `backend/src/main/java/com/klabis/config/BootstrapDataLoader.java`
- `UserCreatedEventHandler` already guards on `isPendingActivation()` — the ACTIVE path just needs to either not publish the event or publish with ACTIVE status.
- Email for password setup remains a parameter of `UserService.createUser(username, email, authorities)` — not moved into the event handler.
- **Current usage analysis:** The 3-parameter `createUserPendingActivation(username, passwordHash, authorities)` method exists but is NOT called anywhere in production code — only in tests. All production code uses `createUserPendingActivation(UserCreationParams)` which includes email. This confirms the old method is dead code from production perspective and can be safely removed.
