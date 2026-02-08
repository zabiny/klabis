# API Prefix Implementation Plan: Adding `/api` to REST Endpoints

## Overview

Add `/api` prefix to all REST API endpoints while keeping OAuth2, frontend routes, and documentation endpoints at root
level.

**Current State**: All endpoints at root level (e.g., `/members`, `/events`, `/calendar-items`)
**Target State**: All REST API endpoints under `/api/*` prefix (e.g., `/api/members`, `/api/events`,
`/api/calendar-items`)

---

## File Inventory & Changes Required

### 1. **Custom Annotation Configuration** (HIGHEST PRIORITY)

#### File: `backend/src/main/java/club/klabis/shared/config/restapi/ApiController.java`

- **Current**: `@RequestMapping` annotation with configurable `path` attribute
- **Change**: Modify annotation to automatically prepend `/api` prefix to all controller paths
- **Implementation**:
    - Create a custom annotation processor or
    - Modify the annotation's `path` parameter handling in `ApisConfiguration`
    - Ensure all controllers using `@ApiController` automatically get `/api` prefix

**Affected Controllers** (14 controllers using `@ApiController`):

```
✓ RootController (path = "/")
✓ MembersApi (path = "/members")
✓ FinanceAccountsController (path = "/member/{memberId}/finance-account")
✓ UserPermissionsApiController
✓ SuspendMemberUseCaseControllers
✓ EventRegistrationsController
✓ EventsController (path = "/events")
✓ CalendarApiController (path = "/calendar-items")
✓ OrisProxyController
✓ OrisEventsController
✓ EventSourcingController (path = "/eventSourcing")
✓ EditOwnInfoUseCaseControllers
✓ RegisterNewMemberController
✓ AdminMemberEditUseCaseControllers
```

---

### 2. **API Configuration** (CORE)

#### File: `backend/src/main/java/club/klabis/shared/config/restapi/ApisConfiguration.java`

- **Current State**:
    - Line 51-65: `apiSecurityFilterChain` - protects all authenticated requests
    - Line 126-162: CORS configuration with `"/**"` pattern

- **Changes Needed**:
    1. Update `apiSecurityFilterChain()` to match `/api/**` pattern instead of general pattern
    2. Update CORS configuration to apply specifically to `/api/**` paths
    3. Ensure `OPTIONS` requests are permitted for CORS preflight

**Current Code (Line 51-57)**:

```java
@Order(AuthorizationServerConfiguration.AFTER_LOGIN_PAGE)
public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
    return http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
```

**New Code Required**:

```java
@Order(AuthorizationServerConfiguration.AFTER_LOGIN_PAGE)
public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
    return http
            .securityMatcher("/api/**")  // Add explicit matcher for /api/** paths
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/api/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
```

---

### 3. **Security Filter Chain Ordering**

#### File: `backend/src/main/java/club/klabis/shared/config/authserver/AuthorizationServerConfiguration.java`

- **Current State**: Lines 29-38 define filter chain order
- **No Changes Required** if implementation handles it correctly
- **Verification Needed**:
    - AuthServer chain (OAuth/OIDC endpoints) at `/oauth/**`, `/oidc/**` should execute before API chain
    - Frontend security chain for static assets should execute before API chain

---

### 4. **Frontend Security Configuration**

#### File: `backend/src/main/java/club/klabis/shared/config/frontend/FrontendSecurityConfiguration.java`

- **Current State**: Lines 22-25 match:
    - `TEXT_HTML` media type
    - `/assets/**` pattern
    - `/auth/callback` pattern

- **Changes Needed**: None required - these routes should remain at root level
- **Verification**: Ensure this filter chain executes before the API security chain (correct order already exists)

---

### 5. **Swagger/OpenAPI Documentation Configuration**

#### File: `backend/src/main/resources/application-springdoc.yml`

- **Current State**:
    - Line 6: `url: /klabis-api-spec.yaml`
    - Line 36: `paths-to-match: - /**`
    - Line 38: `paths-to-exclude: - /actuator/**`

- **Changes Needed**:
    1. Update Springdoc to scan `/api/**` paths
    2. Update `paths-to-match` to properly match `/api/**` pattern
    3. Ensure documentation reflects new API structure

**New Configuration**:

```yaml
springdoc:
  swagger-ui:
    url: /klabis-api-spec.yaml
  api-docs:
    groups:
      enabled: true
    enabled: true
  group-configs:
    - group: members
      display-name: Members API
      packages-to-scan:
        - club.klabis.members
    # ... other groups remain same ...
    - group: all
      display-name: X All endpoints
      paths-to-match:
        - /api/**        # Updated to scan /api/** only
      paths-to-exclude:
        - /actuator/**
    - group: actuator
      display-name: System monitoring endpoints
      paths-to-match:
        - /actuator/**
```

---

### 6. **Swagger Security Configuration**

#### File: `backend/src/main/java/club/klabis/shared/config/springdoc/SpringdocSecurityConfiguration.java`

- **Current State**: Lines 16-24 allow public access to Swagger paths

- **Changes Needed**: Update security matcher patterns

```java
@Bean("swaggerDocChain")
@Order(value = AuthorizationServerConfiguration.AFTER_AUTH_SERVER_SECURITY_ORDER)
public SecurityFilterChain swaggerFilterChain(HttpSecurity http) {
    // Match both old and new paths during migration
    http.securityMatcher(
            "/swagger-ui/*",
            "/v3/api-docs/*",
            "/v3/api-docs",
            "/klabis-api-spec.yaml",
            "/redoc.html")
            .csrf(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(
                    c -> c.anyRequest().permitAll()
            );
    return http.build();
}
```

---

### 7. **Root Navigation Controller**

#### File: `backend/src/main/java/club/klabis/shared/config/hateoas/RootController.java`

- **Current State**:
    - Line 10: `@ApiController(path = "/", ...)`
    - Line 14: Handles both `/` and `/api` paths

- **Impact Analysis**:
    - **If prefix is added at annotation level**: This controller becomes `/api/` instead of just `/`
    - **Decision Required**: Should root navigation remain at `/` or move to `/api/`?

**Option A** (Recommended): Keep root navigation at `/` by excluding RootController from prefix

```java
// RootController remains as-is with path="/"
// Other controllers get /api/ prefix via annotation processor
```

**Option B**: Move root to `/api/` and create separate root endpoint

```java
// Change path to "/api" with prefix, also keep one at "/" for backward compatibility
```

---

### 8. **OAuth2 Authorization Server Configuration** (NO CHANGES NEEDED)

#### File: `backend/src/main/java/club/klabis/shared/config/authserver/AuthorizationServerConfiguration.java`

- **Current State**: Endpoints use `.oauth2AuthorizationServer()` with `server.getEndpointsMatcher()`
- **Change**: None required - OAuth uses Spring Security's built-in path matching
- **Paths Protected**:
    - `/oauth/authorize`
    - `/oauth/token`
    - `/oidc/userinfo`
    - `/.well-known/openid-configuration`

---

### 9. **Application Configuration Files**

#### File: `backend/src/main/resources/application.yml`

- **Current State**: OAuth endpoints defined at lines 22-27
- **Changes Needed**: Update endpoint URIs if needed (likely none if using annotation-based routing)

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        endpoint:
          oidc:
            user-info-uri: /oidc/userinfo    # Keep at root
          authorization-uri: /oauth/authorize  # Keep at root
          token-uri: /oauth/token            # Keep at root
```

#### File: `backend/src/main/resources/application-hateoas.yml`

- **Current State**: Framework strategy for HATEOAS URL detection
- **Changes Needed**: None - should work with `/api/**` paths transparently
- **Verification**: Test HATEOAS link generation includes proper `/api/` prefix

---

## Implementation Strategy

### Approach 1: Annotation Processor (RECOMMENDED)

1. Create a custom `@ApiController` annotation processor
2. Processor intercepts controller registration
3. Automatically prepends `/api` to all controller paths
4. Allows RootController to remain unaffected or be configured separately

**Pros**:

- Minimal code changes
- Centralized control
- Easy to add exceptions (e.g., RootController)

**Cons**:

- Requires custom processor implementation

---

### Approach 2: Spring Security Matcher (ALTERNATIVE)

1. Modify `ApisConfiguration.apiSecurityFilterChain()`
2. Add explicit `.securityMatcher("/api/**")`
3. Update controller paths manually (one by one)
4. Keep root navigation at `/`

**Pros**:

- Uses standard Spring features
- No custom code needed

**Cons**:

- More boilerplate changes
- Manual updates to each controller

---

## Migration Checklist

### Phase 1: Configuration Setup

- [ ] Choose implementation approach (Approach 1 or 2)
- [ ] Update `ApisConfiguration.java` security matcher
- [ ] Update `SpringdocSecurityConfiguration.java` patterns
- [ ] Update `application-springdoc.yml` paths
- [ ] Verify OAuth endpoints remain unchanged

### Phase 2: Controller Updates (if using Approach 2)

- [ ] Update MembersApi path from `/members` to `/api/members`
- [ ] Update EventsController path from `/events` to `/api/events`
- [ ] Update CalendarApiController path from `/calendar-items` to `/api/calendar-items`
- [ ] Update FinanceAccountsController path
- [ ] Update all other API controllers
- [ ] Verify RootController handling (keep at `/` or move to `/api`)

### Phase 3: Testing

- [ ] Test API endpoints at new `/api/**` paths
- [ ] Test OAuth endpoints still work at root `/oauth/**`
- [ ] Test frontend routes at `/auth/callback`
- [ ] Test HATEOAS links include `/api/` prefix
- [ ] Test Swagger/OpenAPI documentation
- [ ] Test CORS preflight requests

### Phase 4: Documentation Updates

- [ ] Update API documentation URLs
- [ ] Update client libraries/SDKs
- [ ] Update frontend API client configuration
- [ ] Update external service integrations

---

## Backward Compatibility (Optional)

If gradual migration is needed:

1. Keep both endpoints active temporarily:
    - Old: `/members`, `/events`, etc.
    - New: `/api/members`, `/api/events`, etc.

2. Use Spring's request mapping to support both:
   ```java
   @GetMapping(value = {"/members", "/api/members"})
   public List<Member> getMembers() { ... }
   ```

3. Deprecate old paths after clients migrate
4. Remove old paths in future release

---

## Security Considerations

### Access Control

- OAuth endpoints remain public (with appropriate OAuth2 security)
- API endpoints protected by JWT validation
- Frontend routes allowed for public (HTML requests)

### CORS

- API endpoints at `/api/**` should allow CORS from registered origins
- OAuth endpoints already have CORS configuration
- Document CORS requirements for clients

### HATEOAS Links

- All hypermedia links must include `/api/` prefix
- Verify `forward-headers-strategy: framework` works correctly
- Test with HAL Explorer

---

## Related Configuration Files Summary

| File                                  | Purpose               | Changes                     |
|---------------------------------------|-----------------------|-----------------------------|
| ApiController.java                    | Annotation definition | Add prefix logic            |
| ApisConfiguration.java                | Security for APIs     | Update matcher to `/api/**` |
| SpringdocSecurityConfiguration.java   | Swagger security      | Keep as-is (docs at root)   |
| application-springdoc.yml             | API doc scanning      | Update paths to `/api/**`   |
| AuthorizationServerConfiguration.java | OAuth endpoints       | No change needed            |
| FrontendSecurityConfiguration.java    | Frontend routes       | No change needed            |
| RootController.java                   | Root navigation       | Decide on location          |
| application.yml                       | OAuth endpoint URIs   | Verify no changes needed    |
| application-hateoas.yml               | HATEOAS settings      | No change needed            |

---

## Notes

1. **RootController Special Case**: Currently handles both `/` and `/api` - decision needed on final location
2. **HATEOAS Framework Strategy**: The `forward-headers-strategy: framework` setting is important for proper URL
   generation
3. **OAuth Order**: Authorization server chain must execute before API security chain (already configured correctly)
4. **Springdoc Groups**: OpenAPI groups should include only `/api/**` paths in "All" group to avoid duplication
5. **CORS Preflight**: `OPTIONS` requests to `/api/**` must be permitted for CORS to work

---

## Recommended Next Steps

1. **Approve approach** (Annotation Processor vs Manual Updates)
2. **Decide on RootController** location (keep at `/` or move to `/api`)
3. **Implement core changes** (ApisConfiguration + SpringdocSecurityConfiguration)
4. **Test OAuth and frontend routes** to ensure they're not affected
5. **Update controllers** according to chosen approach
6. **Comprehensive testing** with API clients and HAL Explorer
7. **Update documentation** and client integrations

