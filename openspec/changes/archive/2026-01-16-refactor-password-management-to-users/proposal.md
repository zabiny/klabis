# Change: Relocate Password Management from Members to Users Module

## Why

Password management functionality is currently split between the members and users modules, with the core implementation
in the members module as a workaround to avoid circular dependencies. This creates architectural confusion because
password management is fundamentally about user authentication (users domain), not member registration (members domain).
Members represent people in the club membership context, while users represent authentication accounts. Having password
logic in the members module violates domain boundaries and the Single Responsibility Principle.

## What Changes

- **Move `PasswordSetupService`** from `com.klabis.members.application` to `com.klabis.users.application`
- **Move `PasswordSetupController`** from `com.klabis.members.presentation` to `com.klabis.users.presentation`
- **Move `PasswordComplexityValidator`** from `com.klabis.members.application` to `com.klabis.users.domain`
- **Move password setup token repository implementations** from `com.klabis.members.infrastructure.persistence` to
  `com.klabis.users.infrastructure.persistence`
- **Move related test files** from members test directories to users test directories
- **Update event-driven communication** between modules using Spring's ApplicationEventPublisher
- **Update all imports and references** across the codebase to point to new locations
- **No API endpoint changes** - REST endpoints remain at `/api/auth/password-setup/*`
- **No database schema changes** - `password_setup_tokens` table already references `users` table

## Impact

### Affected Specs

- `user-activation` - Implementation details updated to reference users module instead of members module
- `auth` - PasswordComplexityValidator location updated to users.domain

### Affected Code

**Primary files to move:**

- `klabis-backend/src/main/java/com/klabis/members/application/PasswordSetupService.java` → `users/application/`
- `klabis-backend/src/main/java/com/klabis/members/presentation/PasswordSetupController.java` → `users/presentation/`
- `klabis-backend/src/main/java/com/klabis/members/application/PasswordComplexityValidator.java` → `users/domain/`
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/PasswordSetupTokenEntity.java` →
  `users/infrastructure/persistence/`
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/PasswordSetupTokenRepositoryImpl.java` →
  `users/infrastructure/persistence/`
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/PasswordSetupTokenJpaRepository.java` →
  `users/infrastructure/persistence/`
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/PasswordSetupTokenMapper.java` →
  `users/infrastructure/persistence/`

**Files to update:**

- `klabis-backend/src/main/java/com/klabis/users/domain/User.java` - Publish UserCreatedEvent when user is created
- `klabis-backend/src/main/java/com/klabis/users/application/PasswordSetupService.java` - Create new event listener for
  UserCreatedEvent
- All test files referencing moved classes
- Configuration files if component scanning needs adjustment

**Benefits:**

- Clearer domain boundaries: Users module owns all user authentication concerns
- Better cohesion: Password management logic grouped with user management
- Easier maintenance: Password features centralized in one module
- Better testability: Well-defined module boundaries
- Aligns with Domain-Driven Design principles

**Risks:**

- Breaking existing functionality if not tested thoroughly
- Potential for introducing circular dependencies if event-driven boundaries not maintained
- Configuration changes that might affect Spring context initialization

**Migration Notes:**

- No breaking API changes - REST endpoints unchanged
- No database migration required
- Existing tokens remain valid
- Event-driven communication ensures loose coupling between modules
- UserCreatedEvent can be triggered from any user creation context (members, admin, future import scripts, etc.)
