## Why

Password handling is fundamentally a User entity responsibility (authentication, credentials, account lifecycle).
However, the current implementation places password setup email logic in `MemberCreatedEventHandler` within the members
module, because the User aggregate lacks email information needed to send the email.

This violates Domain-Driven Design principles - the Member domain knows too much about password setup, which is a User
concern. The UserCreatedEvent is published but contains no email, so it cannot trigger password setup. This
architectural debt should be resolved now before the password setup flow becomes more complex.

## What Changes

- **UserCreatedEvent enhancement**: Add optional `email` field to UserCreatedEvent to carry PII from Member context
  during registration
- **UserCreationParams builder**: Introduce builder pattern for user creation parameters with optional email field
- **UserCreatedEventHandler (new)**: Create event handler in users module to send password setup emails when
  UserCreatedEvent is received
- **MemberCreatedEventHandler simplification**: Remove password setup logic from members module event handler
- **UserService API extension**: Add new `createUserPendingActivation(UserCreationParams)` method
- **Email greeting change**: Use username (registration number) instead of firstName in password setup email greeting
- **RegistrationService update**: Use builder pattern to pass email from Member request to User creation

## Capabilities

### New Capabilities

- `user-creation-with-pii`: User creation with optional Personally Identifiable Information (email) for cross-module
  coordination during member registration

### Modified Capabilities

- `user-registration`: Password setup email flow changes from Member-triggered to User-triggered

## Impact

**Affected Code:**

- `com.klabis.users.UserCreatedEvent`: Add email field and factory method
- `com.klabis.users.UserService`: Add overloaded method accepting UserCreationParams
- `com.klabis.users.application.UserServiceImpl`: Implement new method
- `com.klabis.users` (new): UserCreationParams builder class
- `com.klabis.users` (new): UserCreatedEventHandler
- `com.klabis.members.registration.MemberCreatedEventHandler`: Remove password setup logic
- `com.klabis.members.registration.RegistrationService`: Use builder pattern

**Architectural Impact:**

- Strengthens module boundaries - User module handles all password-related operations
- Members → Users dependency remains (allowed direction)
- User module now handles its complete lifecycle including activation emails

**Breaking Changes:**

- None - this is internal refactoring, public APIs unchanged

**Testing Impact:**

- Update tests for MemberCreatedEventHandler (password setup removed)
- Add tests for UserCreatedEventHandler (password setup added)
- Update RegistrationService integration tests
