# Implementation Tasks

## 1. Extend DTOs with userId (TDD)

- [ ] 1.1 Update existing tests that mock/create MemberDetailsDTO to include userId parameter
- [ ] 1.2 Add `UUID userId` field to MemberDetailsDTO record after `id` field
- [ ] 1.3 Update canonical constructor Javadoc in MemberDetailsDTO to document userId parameter
- [ ] 1.4 Update existing tests that mock/create MemberDetailsResponse to include userId parameter
- [ ] 1.5 Add `UUID userId` field to MemberDetailsResponse record after `id` field
- [ ] 1.6 Update Javadoc in MemberDetailsResponse to document userId field
- [ ] 1.7 Run existing tests to verify DTOs compile and tests pass

## 2. Update ManagementService mapping

- [ ] 2.1 Open ManagementService.mapToMemberDetailsDTO() method
- [ ] 2.2 Add `member.getId().uuid()` as second parameter to MemberDetailsDTO constructor call (after member.getId().uuid())
- [ ] 2.3 Run ManagementService tests to verify mapping works correctly

## 3. Update MemberController mapping

- [ ] 3.1 Open MemberController.mapToResponse() method
- [ ] 3.2 Add `dto.userId()` as second parameter to MemberDetailsResponse constructor call (after dto.id())
- [ ] 3.3 Run MemberController tests to verify response mapping works correctly

## 4. Create MemberPermissionsLinkProcessor (TDD)

- [ ] 4.1 Create test class MemberPermissionsLinkProcessorTest in backend/src/test/java/com/klabis/members/management/
- [ ] 4.2 Write test: shouldAddPermissionsLinkWhenUserHasMembersPermissionsAuthority()
    - Mock SecurityContext with MEMBERS:PERMISSIONS authority
    - Create EntityModel with MemberDetailsResponse containing userId
    - Call processor.process()
    - Assert permissions link is present with correct href and rel
- [ ] 4.3 Write test: shouldNotAddPermissionsLinkWhenUserLacksMembersPermissionsAuthority()
    - Mock SecurityContext without MEMBERS:PERMISSIONS authority
    - Create EntityModel with MemberDetailsResponse containing userId
    - Call processor.process()
    - Assert permissions link is NOT present
- [ ] 4.4 Write test: shouldNotAddPermissionsLinkWhenUserIsUnauthenticated()
    - Mock SecurityContext with null authentication
    - Create EntityModel with MemberDetailsResponse containing userId
    - Call processor.process()
    - Assert permissions link is NOT present
- [ ] 4.5 Write test: shouldHandleNullUserIdGracefully()
    - Create EntityModel with MemberDetailsResponse where userId is null
    - Call processor.process()
    - Assert no exception thrown, no link added
- [ ] 4.6 Create MemberPermissionsLinkProcessor class in backend/src/main/java/com/klabis/members/management/
    - Implement RepresentationModelProcessor<EntityModel<MemberDetailsResponse>>
    - Add @Component annotation
    - Make class package-private
    - Add class Javadoc explaining cross-module concern
- [ ] 4.7 Implement process() method
    - Extract userId from EntityModel content
    - Return model unchanged if userId is null
    - Call hasMembersPermissionsAuthority()
    - If true, add permissions link using linkTo(methodOn(PermissionController.class).getUserPermissions(userId)).withRel("permissions")
- [ ] 4.8 Implement hasMembersPermissionsAuthority() helper method
    - Get Authentication from SecurityContextHolder
    - Return false if null or not authenticated
    - Check authorities stream for "MEMBERS:PERMISSIONS"
- [ ] 4.9 Run tests to verify implementation passes all test cases

## 5. Integration Tests

- [ ] 5.1 Create integration test method in GetMemberApiTest: shouldIncludePermissionsLinkWhenUserHasMembersPermissionsAuthority()
    - Setup: Create member via test data
    - Setup: Mock user with MEMBERS:PERMISSIONS authority
    - Execute: GET /api/members/{id}
    - Assert: HTTP 200 OK
    - Assert: Response contains _links.permissions pointing to /api/users/{userId}/permissions
    - Assert: permissions link has correct rel="permissions"
- [ ] 5.2 Create integration test method in GetMemberApiTest: shouldNotIncludePermissionsLinkWhenUserLacksMembersPermissionsAuthority()
    - Setup: Create member via test data
    - Setup: Mock user with MEMBERS:READ authority only
    - Execute: GET /api/members/{id}
    - Assert: HTTP 200 OK
    - Assert: Response does NOT contain _links.permissions
    - Assert: Response contains other expected links (self, collection, edit if applicable)
- [ ] 5.3 Run integration tests to verify HATEOAS link conditional logic works end-to-end

## 6. Verify Existing Tests

- [ ] 6.1 Run complete test suite: ./gradlew test
- [ ] 6.2 Fix any broken tests caused by DTO signature changes
- [ ] 6.3 Verify all tests pass (target: >80% coverage, 100% for domain logic)

## 7. Manual Verification (Optional)

- [ ] 7.1 Start backend server: BOOTSTRAP_ADMIN_USERNAME='admin' BOOTSTRAP_ADMIN_PASSWORD='admin123' OAUTH2_CLIENT_SECRET='test-secret-123' ./gradlew bootRun
- [ ] 7.2 Authenticate as admin user (has MEMBERS:PERMISSIONS)
- [ ] 7.3 GET /api/members/{id} and verify permissions link is present
- [ ] 7.4 Authenticate as regular member (lacks MEMBERS:PERMISSIONS)
- [ ] 7.5 GET /api/members/{id} and verify permissions link is absent
- [ ] 7.6 Verify userId field is present in response JSON
