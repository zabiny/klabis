# Test Compilation Fixes - UUID → UserId Migration

## Summary

Fixed all remaining User-related test compilation errors after the UUID→UserId value object refactoring.
All 92 User-related tests now pass successfully.

## Changes Made

### 1. Core Domain Enhancement

**File**: `src/main/java/com/klabis/users/domain/UserId.java`

- **Added**: `@JsonValue` annotation to `uuid()` method
- **Purpose**: Ensures UserId serializes to JSON as UUID string, not as `UserId[uuid=...]`

### 2. Test Files Fixed

#### Integration Tests

**File**: `src/test/java/com/klabis/users/UserPermissionsIntegrationTest.java`

- Changed `testUserId` type from `UUID` to `UserId`
- Updated all usages: `testUserId` → `testUserId.uuid()` for JPA repository calls
- Updated all MockMvc path variables: `testUserId` → `testUserId.uuid()`
- Wrapped UUID literals with `new UserId()` for UserEntity constructor calls

**File**: `src/test/java/com/klabis/users/application/EventPublishingIntegrationTest.java`

- Changed `userId` variable from auto-converted to explicit: `new UserId(userId)` for repository calls

**File**: `src/test/java/com/klabis/users/application/PasswordSetupEventListenerTest.java`

- Changed `USER_ID` constant from `UUID` to `UserId`
- Updated mock verifications: `findById(USER_ID.uuid())` → `findById(USER_ID)`
- Updated event construction: `USER_ID` → `USER_ID.uuid()` in UserCreatedEvent

#### Domain Tests

**File**: `src/test/java/com/klabis/users/domain/PasswordSetupTokenTest.java`

- Updated all `PasswordSetupToken.reconstruct()` calls
- Changed: `userId.uuid()` → `userId` (where userId is already UserId type)
- Wrapped UUID literals with `new UserId()` where needed

#### Repository Tests

**File**: `src/test/java/com/klabis/users/infrastructure/persistence/PasswordSetupTokenRepositoryIntegrationTest.java`

- Updated User constructor call: `UUID` → `new UserId(UUID)` for test data creation

#### Controller Tests

**File**: `src/test/java/com/klabis/users/presentation/PasswordSetupControllerIntegrationTest.java`

- Updated UserEntity constructor calls: `userId` → `new UserId(userId)` for proper type
- Fixed test data creation in helper methods

**File**: `src/test/java/com/klabis/users/presentation/UserControllerPermissionsTest.java`

- Updated all MockMvc path variables: `USER_ID` → `USER_ID.uuid()`
- Fixed location header assertions to use `USER_ID.uuid()` instead of `USER_ID`

#### Member Tests (Cross-Module)

**File**: `src/test/java/com/klabis/members/application/RegisterMemberCommandHandlerTest.java`

- Fixed mock return values: `thenReturn(new UserId(memberId))` → `thenReturn(memberId)`
- Note: Member.getId() returns UUID, not UserId

## Patterns Applied

### UUID → UserId Wrapper Pattern

```java
// Before
UUID userId = UUID.randomUUID();
UserEntity entity = new UserEntity(userId, ...);

// After
UUID userId = UUID.randomUUID();
UserEntity entity = new UserEntity(new UserId(userId), ...);
```

### UserId → UUID Extraction Pattern

```java
// Before
UserId userId = new UserId(UUID.randomUUID());
userJpaRepository.deleteById(userId);

// After
UserId userId = new UserId(UUID.randomUUID());
userJpaRepository.deleteById(userId.uuid());
```

### Mock Repository Pattern

```java
// When UserRepository.findById() expects UserId
when(userRepository.findById(eq(userId)))
    .thenReturn(Optional.of(user));

// When mock return type is UUID (Member.getId())
when(mockMember.getId()).thenReturn(memberId); // not new UserId(memberId)
```

### Controller Path Variable Pattern

```java
// Spring MVC path variables must be UUID
@GetMapping("/{id}/permissions")
public ResponseEntity<?> getUserPermissions(@PathVariable UUID id) {
    // Convert to UserId internally
    UserId userId = new UserId(id);
}

// Tests must pass UUID
mockMvc.perform(get("/api/users/{id}/permissions", userId.uuid()))
```

## Test Results

**Before**: 32 compilation errors, 9 test failures (91 tests)
**After**: 0 compilation errors, 0 test failures (92 tests)

All User-related tests now pass:

- Unit tests: ✓
- Integration tests: ✓
- Controller tests: ✓
- Repository tests: ✓

## Key Lessons

1. **JSON Serialization**: Value objects need `@JsonValue` on the accessor method for proper JSON serialization
2. **Type Safety**: UserId provides type safety but requires careful conversion at boundaries (JPA, MVC, JSON)
3. **Test Patterns**: Consistent patterns for wrapping/unwrapping UUIDs at different layers prevent confusion
4. **Mock Types**: Mock return types must match actual method signatures (UUID vs UserId)

## Files Modified

- 1 domain class (UserId.java)
- 7 test classes
- 0 production code changes (except UserId @JsonValue)

Total lines changed: ~150 lines across 8 files
