## 1. Remove empty-set validation

- [x] 1.1 Remove the empty-check branch (and its `IllegalArgumentException("At least one authority required")`) from `AuthorityValidator.validate(Set<String>)` in `backend/src/main/java/com/klabis/common/users/domain/AuthorityValidator.java`
- [x] 1.2 Remove the equivalent empty-check branch from `AuthorityValidator.validateAuthorityEnums(Set<Authority>)` in the same file
- [x] 1.3 Confirm the null-check and invalid-authority-check branches in both methods are untouched
- [x] 1.4 Remove the `@NotEmpty` annotation from `UpdatePermissionsRequest.authorities` in `backend/src/main/java/com/klabis/common/users/infrastructure/restapi/PermissionController.java` (line ~164-165) — this is a second, independent guard (Jakarta Bean Validation via `@Valid` on the controller method) that would otherwise still reject empty authorities with a 400

## 2. Update tests

- [x] 2.1 Remove `shouldThrowExceptionForEmptyAuthorities` from `AuthorityValidatorTest.ValidateStringSetMethod` (`backend/src/test/java/com/klabis/common/users/AuthorityValidatorTest.java`)
- [x] 2.2 Remove `shouldThrowExceptionForEmptyAuthorityEnums` from `AuthorityValidatorTest.ValidateAuthorityEnumsMethod`
- [x] 2.3 Add a replacement test in each nested class asserting an empty set is now accepted without throwing (e.g. `shouldAcceptEmptyAuthorities` / `shouldAcceptEmptyAuthorityEnums`)

## 3. Verification

- [x] 3.1 Run `AuthorityValidatorTest` and confirm all tests pass
- [x] 3.2 Run the full `common.users` module test suite (including `PermissionServiceImplTest` / `AuthorizationPolicyTest` if present) to confirm the admin lockout guard for `MEMBERS:PERMISSIONS` still behaves as before and is unaffected by this change
- [x] 3.3 Run full backend test suite via the test-runner to catch any other caller relying on the empty-set exception
- [x] 3.4 Confirm no other DTO/controller carries an independent `@NotEmpty`/`@Size(min=1)` guard on an authorities collection that would silently keep blocking empty submissions at the API layer
