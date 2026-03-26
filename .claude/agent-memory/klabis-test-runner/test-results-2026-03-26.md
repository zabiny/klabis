---
name: Backend Test Suite Run (2026-03-26)
description: Full backend test suite execution results showing 1523/1524 tests passing
type: project
---

## Test Execution Summary

- **Date**: 2026-03-26
- **Test Suite**: Full backend test suite (`./gradlew test`)
- **Location**: `/home/davca/Documents/Devel/klabis/.claude/worktrees/spring-boot-4-upgrade/backend`
- **Total Tests**: 1524
- **Passed**: 1523 (99.93%)
- **Failed**: 1
- **Skipped**: 8

## Failure Details

### Test: `UpdateMemberApiTest$UpdateMemberTests$AuthorizationTests#shouldReturn401WhenUnauthenticated`

**Error**: `jakarta.servlet.ServletException: Request processing failed: java.lang.IllegalStateException: Expected KlabisJwtAuthenticationToken, got: null principal (token type: Authentication$MockitoMock$lcHjLXqE)`

**Root Cause**: The `CurrentUserArgumentResolver` expects a `KlabisJwtAuthenticationToken` but received a Mockito mock instead of a real token. This occurs when testing unauthenticated requests via MockMvc.

**Location**: `/backend/src/main/java/com/klabis/members/infrastructure/authorizationserver/CurrentUserArgumentResolver.java:70`

**Test Location**: `/backend/src/test/java/com/klabis/members/infrastructure/restapi/UpdateMemberApiTest.java:657`

**Stack Trace Key Points**:
1. Test attempts to make an unauthenticated request expecting 401 response
2. Request passes through Spring Security filters
3. Controller method with `@CurrentUser` annotation triggers argument resolution
4. `CurrentUserArgumentResolver.getAuthentication()` receives `Authentication$MockitoMock$lcHjLXqE` instead of `KlabisJwtAuthenticationToken`
5. `IllegalStateException` thrown because resolver cannot cast mock to expected type

## Notes

- This is a test isolation/mocking issue in the authorization test setup
- 1523 other tests pass, indicating no widespread issues
- The failure appears to be related to how the test framework provides authentication context in MockMvc
