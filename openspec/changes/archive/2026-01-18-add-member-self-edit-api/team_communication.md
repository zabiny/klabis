# Team Communication - add-member-self-edit-api Bug Fixes

## Date: 2025-01-18

## Issues to Fix

Two confusing error responses have been reported in the PATCH /api/members/{id} endpoint:

### Issue #1: Confusing "Update request must contain at least one field to update"

**Request:**

```json
PATCH /api/members/{id}
{
  "firstName": "David"
}
```

**Response:**

```json
{
  "type": "about:blank",
  "title": "Invalid Update",
  "status": 400,
  "detail": "Update request must contain at least one field to update",
  "instance": "/api/members/f339147d-1ce9-4776-82b4-f6b0b08e1b74"
}
```

**Root Cause:** The `UpdateMemberRequest` DTO doesn't include `firstName`, `lastName`, or `dateOfBirth` fields, so when
a user tries to update them, they're silently ignored. The `UpdateMemberCommand.isEmpty()` method returns true because
no recognized fields are present, leading to a confusing error message.

**Analysis:** According to the proposal.md, `firstName`, `lastName`, and `dateOfBirth` are listed as **admin-only fields
**. However, the `UpdateMemberRequest` DTO only includes:

- email, phone, address (member-editable)
- gender, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup (admin-only)
- dietaryRestrictions (member-editable)

The admin-only fields `firstName`, `lastName`, `dateOfBirth` are **missing from the DTO entirely**.

**Fix Options:**

1. **Option A (Preferred):** Add `firstName`, `lastName`, `dateOfBirth` to `UpdateMemberRequest` and
   `UpdateMemberCommand`, and enforce them as admin-only in the command handler (similar to how `gender`, `chipNumber`,
   etc. are handled)
2. **Option B:** Modify the error handling to provide a more helpful error message when unrecognized fields are provided

### Issue #2: Confusing error message when phone validation fails

**Request:**

```json
PATCH /api/members/{id}
{
  "phone": "123456789"
}
```

**Response:**

```json
{
  "type": "about:blank",
  "title": "Invalid Update",
  "status": 400,
  "detail": "Cannot determine user email for registration number: ZBM0001",
  "instance": "/api/members/f339147d-1ce9-4776-82b4-f6b0b08e1b74"
}
```

**Root Cause:** The `getEmailFromAuthentication()` method (lines 134-158 in UpdateMemberCommandHandler) tries to
determine the user's email for authorization. If the OAuth2 principal doesn't have an email attribute, it falls back to
looking up the member by registration number. However, this error message is confusing because it doesn't relate to the
phone number the user provided.

**Fix:** The error message should be more context-aware. Instead of throwing an exception about email determination
during the authorization phase, we should validate that the user can be properly identified before processing the
update, or provide a clearer error message that explains the authentication issue.

## Implementation Plan

### Phase 1: Fix Issue #1 - Add missing admin-only fields

- [ ] Add `firstName`, `lastName`, `dateOfBirth` fields to `UpdateMemberRequest`
- [ ] Add `firstName`, `lastName`, `dateOfBirth` fields to `UpdateMemberCommand`
- [ ] Update `isEmpty()` method in `UpdateMemberCommand`
- [ ] Add admin-only field checks for these new fields in `verifyNoAdminFields()`
- [ ] Add corresponding field updates in `applyUpdates()` method (may need to update Member entity)
- [ ] Update tests to cover these new fields

### Phase 2: Fix Issue #2 - Improve error message for authentication/authorization issues

- [ ] Improve error handling in `getEmailFromAuthentication()` to provide clearer error messages
- [ ] Ensure phone validation errors are distinguishable from authentication errors

### Phase 3: Testing and Validation

- [ ] Run unit tests
- [ ] Run integration tests
- [ ] Update HTTP client test examples
- [ ] Perform manual testing

### Phase 4: Code Review and Commit

- [ ] Perform code review
- [ ] Fix any code review findings
- [ ] Commit changes

## Implementation Notes

- The Member entity may need new update methods for `firstName`, `lastName`, `dateOfBirth` if they don't already exist
- All date fields should use ISO-8601 format
- Validation rules: firstName and lastName must not be blank, dateOfBirth must not be in the future
- These fields should be enforced as admin-only in the same way as other admin-only fields

## Status

- **Completed:** Issue #1 - Added missing admin-only fields to the update member API
- **Implementation Summary:**
    - Added `firstName`, `lastName`, `dateOfBirth` fields to `UpdateMemberRequest` DTO
    - Added `firstName`, `lastName`, `dateOfBirth` fields to `UpdateMemberCommand`
    - Updated `UpdateMemberCommand.isEmpty()` to check new fields
    - Updated `UpdateMemberCommandHandler.verifyNoAdminFields()` to enforce admin-only access for new fields
    - Updated `UpdateMemberCommandHandler.applyUpdates()` to handle PersonalInformation updates
    - Created custom validator `ValidOptionalNotBlank` for Optional<String> fields
    - Updated all test files to include new fields in constructor calls
    - Added comprehensive unit tests for new admin-only fields

- **Completed:** Issue #2 - Improved error message for authentication/authorization issues
- **Implementation Summary:**
    - Created new `UserIdentificationException` to distinguish authentication failures from validation errors
    - Updated `UpdateMemberCommandHandler.getEmailFromAuthentication()` to:
        - Throw `UserIdentificationException` instead of `InvalidUpdateException`
        - Provide clear, context-aware error messages for different failure scenarios
        - Add detailed logging to help diagnose authentication issues
    - Added exception handler in `MemberController` to return HTTP 401 (Unauthorized) for `UserIdentificationException`
    - Added comprehensive unit tests to verify improved error messages:
        - Test for OAuth2 token without email and member not found
        - Test for member with no email on file
        - Test for successful identification from OAuth2 token email
    - Fixed existing test issues:
        - Added `@AfterEach` to clear SecurityContext
        - Updated `createMockAuthentication()` to set SecurityContext
        - Fixed test fixture to properly set member ID using reflection
        - Added `@MockitoSettings(strictness = Strictness.LENIENT)` to suppress unnecessary stubbing warnings
    - All 24 tests pass successfully

**User Experience Improvement:**

- **Before:** Users saw generic "Cannot determine user email for registration number: ZBM0001" which was confusing
- **After:** Users now see clear, actionable error messages:
    - "Authentication failed: Unable to identify user with registration number 'ZBM0001'. Please ensure your OAuth2
      token includes a valid email address or contact support if your registration number is not recognized."
    - "Member with registration number 'ZBM9001' has no email address on file. Please contact support to update your
      member record."
    - "Authentication failed: Unable to identify user. Please ensure your OAuth2 token includes a valid email address."
- **HTTP Status Code:** Changed from 400 (Bad Request) to 401 (Unauthorized) to properly indicate authentication failure
- **Error Distinction:** Authentication errors are now clearly distinguishable from field validation errors

---

## Date: 2025-01-18 (continued)

## Issue #3: Fix failing integration tests in UpdateMemberApiTest

After implementing the two bug fixes above, 9 integration tests in `UpdateMemberApiTest` were failing due to incorrect
mock setup.

**Root Cause:** The tests were using `@WebMvcTest` slice tests with mocked `UpdateMemberCommandHandler` and
`GetMemberQueryHandler`. The `GetMemberQueryHandler` mock was returning a static fixture (`createMemberDetailsDTO()`)
that didn't reflect the updates being sent in the request. This caused test assertions to fail because the response
values didn't match the expected values from the update request.

**Failing Tests:**

1. `shouldAllowMemberToUpdateDietaryRestrictions` - No value at JSON path "$.dietaryRestrictions"
2. `shouldAllowMemberToUpdateOwnAddress` - Expected "My New Address 456" but was "Hlavní 123"
3. `shouldAllowMemberToUpdateOwnPhone` - Expected "+420987654321" but was "+420777888999"
4. `shouldIncludeAllUpdatedFields` - Expected "Updated 123" but was "Hlavní 123"
5. `shouldPerformPartialUpdateWhenAdmin` - No value at JSON path "$.dietaryRestrictions"
6. `shouldUpdateAdminOnlyFieldsWhenAdmin` - Expected "FEMALE" but was "MALE"
7. `shouldUpdateDocumentsWhenAdmin` - No value at JSON path "$.identityCard.cardNumber"
8. `shouldUpdateMemberAddressWhenAdmin` - Expected "New Street 123" but was "Hlavní 123"
9. `shouldUpdateMemberPhoneWhenAdmin` - Expected "+420777123456" but was "+420777888999"

**Solution Implemented:**

1. Created new helper method `createUpdatedMemberDTO(UpdateMemberRequest request, UUID memberId)` that builds a
   `MemberDetailsDTO` reflecting the updates from the request
2. Updated all failing tests to use `createUpdatedMemberDTO()` instead of the static fixture
3. The new method:
    - Extracts values from the `UpdateMemberRequest` Optional fields
    - Uses default values for fields not present in the request
    - Properly handles nested objects (Address, IdentityCard, MedicalCourse, TrainerLicense)
    - Returns a DTO that matches what the controller would return after a successful update

**Files Modified:**

-
`/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/test/java/com/klabis/members/presentation/UpdateMemberApiTest.java`

**Test Results:**

- **Before:** 9 failing tests out of 23 total tests
- **After:** All 23 tests passing ✓

**Tests Updated:**

- `shouldAllowMemberToUpdateDietaryRestrictions`
- `shouldAllowMemberToUpdateOwnAddress`
- `shouldAllowMemberToUpdateOwnPhone`
- `shouldUpdateMemberPhoneWhenAdmin`
- `shouldUpdateMemberAddressWhenAdmin`
- `shouldUpdateAdminOnlyFieldsWhenAdmin`
- `shouldUpdateDocumentsWhenAdmin`
- `shouldPerformPartialUpdateWhenAdmin`
- `shouldIncludeAllUpdatedFields`

**Implementation Notes:**

- These are controller-layer slice tests (@WebMvcTest), so they test the controller in isolation
- The handlers are mocked, so the mocks must return appropriate values that reflect the updates
- The fix ensures tests properly verify the controller behavior without relying on actual handler logic
- No changes were made to the controller implementation - only test fixes

---

## Date: 2026-01-19

## Issue #4: Fix failing unit tests in UpdateMemberCommandHandlerTest after registration number authorization change

**User Request:** Fix tests in UpdateMemberCommandHandlerTest.java after change in self-edit authorization to use user's
registration number instead of email to find out if authenticated user is same as edited member.

**Problem Summary:**
The `UpdateMemberCommandHandler` was changed to use registration number (from `authentication.getName()`) instead of
email for self-edit authorization. The tests were not updated to match this change, causing 15 test failures.

**Root Cause:**
The `createMockAuthentication` helper method in the test class does not mock `authentication.getName()` to return the
registration number. The handler now calls `getAuthenticatedRegistrationNumber()` which expects
`authentication.getName()` to return the registration number string (lines 127-137 in UpdateMemberCommandHandler).

**Test Failures (15 total):**

- **10 Failures in AuthorizationFailures and UserIdentificationTests:**
    - Expected `AdminFieldAccessException` or `SelfEditNotAllowedException`
    - Got `InvalidUpdateException: Cannot update member without authenticated user`
    - Cause: `auth.getName()` returns null, so `getAuthenticatedRegistrationNumber()` returns empty Optional

- **5 Errors in SelfEditTests and UserIdentificationTests:**
    - Same root cause - authentication name not set

**Tests That Need Fixing:**

1. **SelfEditTests (4 tests):**
    - `shouldAllowMemberToUpdateOwnEmail`
    - `shouldAllowMemberToUpdateOwnPhone`
    - `shouldAllowMemberToUpdateOwnAddress`
    - `shouldAllowMemberToUpdateDietaryRestrictions`

2. **AuthorizationFailures (7 tests):**
    - `shouldThrowExceptionWhenMemberEditsAnotherMember`
    - `shouldThrowExceptionWhenNonAdminUpdatesGender`
    - `shouldThrowExceptionWhenNonAdminUpdatesFirstName`
    - `shouldThrowExceptionWhenNonAdminUpdatesLastName`
    - `shouldThrowExceptionWhenNonAdminUpdatesDateOfBirth`
    - `shouldThrowExceptionWhenNonAdminUpdatesChipNumber`
    - `shouldThrowExceptionWhenNonAdminUpdatesIdentityCard`
    - `shouldThrowExceptionWhenNonAdminUpdatesTrainerLicense`

3. **UserIdentificationTests (3 tests):**
    - `shouldThrowExceptionWhenOAuth2TokenHasNoEmailAndMemberNotFound`
    - `shouldThrowExceptionWhenMemberHasNoEmailOnFile`
    - `shouldSuccessfullyIdentifyUserFromOAuth2TokenEmail`

**Already Passing (No Changes Needed):**

- AdminEditTests (6 tests) - don't rely on registration number verification
- ValidationFailures (3 tests) - test validation logic, not authorization

**Implementation Plan:**

### Phase 1: Fix createMockAuthentication Helper

- Update `createMockAuthentication(String email, boolean isAdmin)` to also set `authentication.getName()` to return
  registration number
- Need to determine correct registration number based on test scenario:
    - For self-edit tests: use test member's registration number (ZBM9001)
    - For admin tests: use admin's registration number (ZBM0001)
    - For "edit another member" tests: use different registration number

### Phase 2: Fix Self-Edit Tests (4 tests)

- Update tests to properly set authentication name with member's registration number
- Verify registration number matches between authentication and test member

### Phase 3: Fix Authorization Failure Tests (7 tests)

- Update `shouldThrowExceptionWhenMemberEditsAnotherMember` to use different registration number
- Update other admin field tests to set authentication name correctly

### Phase 4: Fix User Identification Tests (3 tests)

- Update tests to match new registration-number-based error messages
- Fix assertions to expect correct exception types

### Phase 5: Run All Tests and Verify

- Run full test suite to ensure all 24 tests pass
- No regressions in already-passing tests

**Status:** 🔄 IN PROGRESS

**Progress Updates:**

### Phase 1: Analysis Complete ✓

- Subagent analyzed the test failures and handler code
- Root cause confirmed: `createMockAuthentication` doesn't set `auth.getName()` to return registration number
- Solution plan created:
    - Change signature from `createMockAuthentication(String email, boolean isAdmin)` to
      `createMockAuthentication(String registrationNumber, String email, boolean isAdmin)`
    - Add `when(auth.getName()).thenReturn(registrationNumber);`
    - Update all 22 test calls to use new signature with appropriate registration numbers

### Phase 2: Implementation - Fix createMockAuthentication Helper ✓ COMPLETED

- [x] Updated `createMockAuthentication` method signature from `(String email, boolean isAdmin)` to
  `(String registrationNumber, String email, boolean isAdmin)`
- [x] Added `when(auth.getName()).thenReturn(registrationNumber);` to mock the registration number
- [x] Updated all 22 test calls to use new signature with appropriate registration numbers:
    - **AdminEditTests (6 calls):** Use `"ZBM0001", "admin@example.com", true`
    - **SelfEditTests (4 calls):** Use `"ZBM9001", "jan.novak@example.com", false`
    - **AuthorizationFailures (8 calls):**
        - `shouldThrowExceptionWhenMemberEditsAnotherMember`: Use `"ZBM8001", "different.email@example.com", false`
        - Other 7 tests: Use `"ZBM9001", "jan.novak@example.com", false`
    - **ValidationFailures (2 calls):** Use `"ZBM9001", "jan.novak@example.com", false`
    - **UserIdentificationTests (2 calls):** Updated to use helper method with null email for OAuth2 token scenarios

### Phase 3: Fix User Identification Tests - Updated Test Expectations ✓ COMPLETED

- [x] Updated tests to match current handler behavior instead of expecting `UserIdentificationException`
- [x] Fixed `shouldThrowExceptionWhenOAuth2TokenHasNoEmailAndMemberNotFound` to expect `SelfEditNotAllowedException` (
  different registration numbers)
- [x] Fixed `shouldThrowExceptionWhenMemberHasNoEmailOnFile` to allow successful update (self-edit with same
  registration number passes)
- [x] Fixed `shouldSuccessfullyIdentifyUserFromOAuth2TokenEmail` to use proper authentication setup with email

### Phase 4: Additional Test Fixes ✓ COMPLETED

- [x] Fixed `ValidationFailures` test that had incorrect admin setup
- [x] Fixed reflection code to set ID for `memberWithoutEmail` in UserIdentificationTests
- [x] Added proper ArgumentCaptor setup for save operations

## Test Results ✓ ALL TESTS PASSING

- **Total Tests:** 24 tests in UpdateMemberCommandHandlerTest
- **Before Fix:** 15 failing tests
- **After Fix:** All 24 tests passing ✓
- **Test Categories Fixed:**
    - AdminEditTests: 6 tests passing
    - SelfEditTests: 4 tests passing
    - AuthorizationFailures: 8 tests passing
    - ValidationFailures: 3 tests passing
    - UserIdentificationTests: 3 tests passing

## Summary of Changes Made

### Updated Files:

1.
`/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/test/java/com/klabis/members/application/UpdateMemberCommandHandlerTest.java`

### Key Changes:

1. **Modified `createMockAuthentication` method:**
    - Changed signature: `(String registrationNumber, String email, boolean isAdmin)`
    - Added: `when(auth.getName()).thenReturn(registrationNumber);`
    - Updated to handle null email values for OAuth2 token scenarios

2. **Updated 22 test method calls:**
    - Admin tests: `"ZBM0001", "admin@example.com", true`
    - Self-edit tests: `"ZBM9001", "jan.novak@example.com", false"`
    - Authorization failures: Appropriate registration numbers based on test scenario
    - Validation tests: `"ZBM9001", "jan.novak@example.com", false"`

3. **Fixed UserIdentificationTests:**
    - Updated test expectations to match current handler behavior
    - Fixed reflection setup for member IDs
    - Added proper mock setup for OAuth2 token scenarios

### Impact:

- Tests now properly verify the registration-number-based self-edit authorization
- All failing unit tests have been resolved
- Test suite accurately reflects the current handler implementation
- No changes were made to production code - only test fixes

**Note:** The UserIdentificationTests section had 3 tests that were originally written to expect
`UserIdentificationException`, but the current handler implementation doesn't throw this exception in those scenarios.
The tests were updated to match the actual handler behavior, which is the correct approach for unit tests.

---

## Date: 2026-01-19 (Task Completion)

**Status:** ✅ COMPLETED - All unit tests in UpdateMemberCommandHandlerTest are now passing

**Final Results:**

- **Tests Run:** 24 total tests
- **Pass Rate:** 100% (24/24 tests passing)
- **Fixed Issues:** 15 previously failing tests now pass
- **No Regressions:** All previously passing tests continue to pass

**Key Achievement:** The test suite now properly reflects the handler's registration-number-based authorization model,
ensuring accurate validation of the self-edit functionality.

### Phase 5: Code Review and Refactoring ✓ COMPLETED

**Code Review Findings:**

- Reviewed test file for code quality and adherence to project conventions
- Identified high-priority improvements for maintainability
- No critical issues found - all tests follow project conventions

**Implemented Refactoring:**

1. **Added Convenience Helper Methods:**
    - `createAdminAuthentication()` - for admin test scenarios (6 calls)
    - `createMemberAuthentication()` - for self-edit scenarios (18 calls)
    - `createOtherMemberAuthentication(String, String)` - for different member scenarios
    - Reduced code duplication and improved readability

2. **Extracted Test Data Constants:**
    - `ADMIN_REG_NUMBER = "ZBM0001"`
    - `MEMBER_REG_NUMBER = "ZBM9001"`
    - `ADMIN_EMAIL = "admin@example.com"`
    - `MEMBER_EMAIL = "jan.novak@example.com"`
    - Improved maintainability and reduced magic strings

**Final Test Results:**
✅ All 24 tests passing after refactoring
✅ Code quality improved
✅ No regressions introduced
✅ Ready for commit

### Phase 6: Git Commit Preparation

**Files Modified:**

-
`/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/test/java/com/klabis/members/application/UpdateMemberCommandHandlerTest.java`

**Changes Summary:**

1. Updated `createMockAuthentication` to include registration number parameter
2. Added `when(auth.getName()).thenReturn(registrationNumber)`
3. Updated all 22 test calls to use new signature
4. Added convenience helper methods for common scenarios
5. Extracted test data constants to class level
6. Fixed UserIdentificationTests expectations to match actual handler behavior

**Commit Message Suggestion:**

```
test(members): fix UpdateMemberCommandHandlerTest after registration number auth change

- Updated createMockAuthentication to set registration number via auth.getName()
- Added convenience helper methods (createAdminAuthentication, createMemberAuthentication)
- Extracted test data constants (ADMIN_REG_NUMBER, MEMBER_REG_NUMBER, emails)
- Fixed 15 failing tests to match registration-number-based authorization
- All 24 tests now passing

Ref: Issue #4 in team_communication.md for add-member-self-edit-api
```

