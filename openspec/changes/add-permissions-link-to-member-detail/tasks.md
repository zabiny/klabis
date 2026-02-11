# Implementation Tasks

## 1. Create MemberPermissionsLinkProcessor (TDD)

- [ ] 1.1 Create test class MemberPermissionsLinkProcessorTest in backend/src/test/java/com/klabis/members/management/
- [ ] 1.2 Write test: shouldAddPermissionsLinkWhenUserHasMembersPermissionsAuthority()
    - Mock SecurityContext with MEMBERS:PERMISSIONS authority
    - Create EntityModel with MemberDetailsResponse (id field = userId)
    - Call processor.process()
    - Assert permissions link is present with correct href (using response.id()) and rel
- [ ] 1.3 Write test: shouldNotAddPermissionsLinkWhenUserLacksMembersPermissionsAuthority()
    - Mock SecurityContext without MEMBERS:PERMISSIONS authority
    - Create EntityModel with MemberDetailsResponse
    - Call processor.process()
    - Assert permissions link is NOT present
- [ ] 1.4 Write test: shouldNotAddPermissionsLinkWhenUserIsUnauthenticated()
    - Mock SecurityContext with null authentication
    - Create EntityModel with MemberDetailsResponse
    - Call processor.process()
    - Assert permissions link is NOT present
- [ ] 1.5 Write test: shouldHandleNullIdGracefully()
    - Create EntityModel with MemberDetailsResponse where id is null
    - Call processor.process()
    - Assert no exception thrown, no link added
- [ ] 1.6 Create MemberPermissionsLinkProcessor class in backend/src/main/java/com/klabis/members/management/
    - Implement RepresentationModelProcessor<EntityModel<MemberDetailsResponse>>
    - Add @Component annotation
    - Make class package-private
    - Add class Javadoc explaining cross-module concern
- [ ] 1.7 Implement process() method
    - Extract id (= userId) from EntityModel content via response.id()
    - Return model unchanged if id is null
    - Call hasMembersPermissionsAuthority()
    - If true, add permissions link using linkTo(methodOn(PermissionController.class).getUserPermissions(id)).withRel("permissions")
- [ ] 1.8 Implement hasMembersPermissionsAuthority() helper method
    - Get Authentication from SecurityContextHolder
    - Return false if null or not authenticated
    - Check authorities stream for "MEMBERS:PERMISSIONS"
- [ ] 1.9 Run tests to verify implementation passes all test cases

## 2. Integration Tests

- [ ] 2.1 Create integration test method in GetMemberApiTest: shouldIncludePermissionsLinkWhenUserHasMembersPermissionsAuthority()
    - Setup: Create member via test data
    - Setup: Mock user with MEMBERS:PERMISSIONS authority
    - Execute: GET /api/members/{id}
    - Assert: HTTP 200 OK
    - Assert: Response contains _links.permissions pointing to /api/users/{id}/permissions (id = userId)
    - Assert: permissions link has correct rel="permissions"
- [ ] 2.2 Create integration test method in GetMemberApiTest: shouldNotIncludePermissionsLinkWhenUserLacksMembersPermissionsAuthority()
    - Setup: Create member via test data
    - Setup: Mock user with MEMBERS:READ authority only
    - Execute: GET /api/members/{id}
    - Assert: HTTP 200 OK
    - Assert: Response does NOT contain _links.permissions
    - Assert: Response contains other expected links (self, collection, edit if applicable)
- [ ] 2.3 Run integration tests to verify HATEOAS link conditional logic works end-to-end

## 3. Verify Existing Tests

- [ ] 3.1 Run complete test suite: ./gradlew test
- [ ] 3.2 Verify all tests pass (target: >80% coverage, 100% for domain logic)

## 4. Manual Verification (Optional)

- [ ] 4.1 Start backend server: BOOTSTRAP_ADMIN_USERNAME='admin' BOOTSTRAP_ADMIN_PASSWORD='admin123' OAUTH2_CLIENT_SECRET='test-secret-123' ./gradlew bootRun
- [ ] 4.2 Authenticate as admin user (has MEMBERS:PERMISSIONS)
- [ ] 4.3 GET /api/members/{id} and verify permissions link is present
- [ ] 4.4 Authenticate as regular member (lacks MEMBERS:PERMISSIONS)
- [ ] 4.5 GET /api/members/{id} and verify permissions link is absent
- [ ] 4.6 Verify id field is used in permissions link URL
