# @HasAuthority Annotation Guide

## Overview

`@HasAuthority` is a custom Spring Security annotation that provides **type-safe, method-level authorization checking**
for global authorities in Klabis.

It works like `@PreAuthorize` but with key advantages:

- ✅ **Type-safe**: Uses `Authority` enum instead of string literals
- ✅ **IDE support**: Full refactoring and code completion support
- ✅ **No SpEL**: Avoids complex Spring Expression Language strings
- ✅ **Simple**: Best for single-authority checks

## Quick Start

### Basic Usage (Single Method)

```java
@PostMapping("/members")
@HasAuthority(Authority.MEMBERS_CREATE)
public ResponseEntity<?> createMember(@RequestBody MemberRequest request) {
    // Only users with MEMBERS:CREATE authority can access this
    return ResponseEntity.ok("Member created");
}
```

### Class-Level Authorization

```java
@RestController
@RequestMapping("/api/admin")
@HasAuthority(Authority.MEMBERS_PERMISSIONS)
class AdminController {

    @GetMapping
    public ResponseEntity<?> getStats() {
        // All methods require MEMBERS:PERMISSIONS
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<?> updateSettings() {
        return ResponseEntity.ok("Settings updated");
    }
}
```

### Method-Level Overrides Class-Level

```java
@RestController
@RequestMapping("/api/members")
@HasAuthority(Authority.MEMBERS_READ)  // Default for all methods
class MemberController {

    @GetMapping
    public ResponseEntity<?> listMembers() {
        // Requires MEMBERS:READ
        return ResponseEntity.ok(members);
    }

    @PostMapping
    @HasAuthority(Authority.MEMBERS_CREATE)  // Overrides class-level
    public ResponseEntity<?> createMember() {
        // Requires MEMBERS:CREATE instead
        return ResponseEntity.ok("Created");
    }
}
```

## Authorization Flow

```
Request
  ↓
Spring AOP Intercepts Method Call
  ↓
HasAuthorityAspect.checkAuthority() invoked
  ↓
Resolves @HasAuthority annotation
  ↓
Gets Current Authentication from SecurityContext
  ↓
Checks if user has required authority
  ├─ No authority? → AccessDeniedException (403)
  └─ Has authority? → Method executes
```

## Error Handling

When authorization fails, an `AccessDeniedException` is thrown, which Spring Security converts to:

- **HTTP 403 Forbidden** response with problem+json body

Example error response:

```json
{
  "type": "https://api.klabis.example.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access denied. Required authority: MEMBERS:CREATE"
}
```

## Available Authorities

See `Authority` enum for all available authorities:

- `MEMBERS_CREATE` → `"MEMBERS:CREATE"` (Context-specific)
- `MEMBERS_READ` → `"MEMBERS:READ"` (Context-specific)
- `MEMBERS_UPDATE` → `"MEMBERS:UPDATE"` (Context-specific)
- `MEMBERS_DELETE` → `"MEMBERS:DELETE"` (Context-specific)
- `MEMBERS_PERMISSIONS` → `"MEMBERS:PERMISSIONS"` (Global)
- `EVENTS_MANAGE` → `"EVENTS:MANAGE"` (Context-specific)

## Comparison: @HasAuthority vs @PreAuthorize

### @HasAuthority (Recommended for simple checks)

```java
@HasAuthority(Authority.MEMBERS_CREATE)
public ResponseEntity<?> createMember() { ... }
```

**Pros:**

- Type-safe (uses enum)
- Clean, readable syntax
- IDE refactoring support
- No string literals

**Cons:**

- Only for single authority checks
- Cannot combine with boolean logic

### @PreAuthorize (For complex authorization)

```java
@PreAuthorize("hasAuthority('MEMBERS:CREATE')")
public ResponseEntity<?> createMember() { ... }
```

**Pros:**

- Flexible (supports complex logic)
- Can check multiple conditions
- Can access method parameters

**Cons:**

- Uses string literals (not type-safe)
- SpEL syntax is verbose and error-prone
- No IDE refactoring support

### Complex Authorization Example

Use `@PreAuthorize` for complex scenarios:

```java
// User can update their own member OR has admin update authority
@PreAuthorize("(#id == authentication.principal.userId) OR hasAuthority('MEMBERS:UPDATE')")
public ResponseEntity<?> updateMember(@PathVariable UUID id) { ... }

// Admin endpoint with multiple conditions
@PreAuthorize("hasAuthority('MEMBERS:PERMISSIONS') AND hasAuthority('EVENTS:MANAGE')")
public ResponseEntity<?> adminPanel() { ... }
```

## Implementation Details

### HasAuthorityAspect

The `HasAuthorityAspect` class uses AspectJ to:

1. Intercept methods annotated with `@HasAuthority`
2. Resolve the required authority from the annotation
3. Get current authentication from `SecurityContextHolder`
4. Check if user's granted authorities include the required authority
5. Throw `AccessDeniedException` if authorization fails

### How It Works

```java
@Aspect
@Component
public class HasAuthorityAspect {

    @Before("@annotation(HasAuthority) || @within(HasAuthority)")
    public void checkAuthority(JoinPoint joinPoint) {
        // 1. Get required authority from annotation
        Authority requiredAuthority = resolveAuthority(joinPoint);

        // 2. Get current user's authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 3. Check if user has the authority
        if (!hasAuthority(auth, requiredAuthority)) {
            throw new AccessDeniedException("...");
        }
    }
}
```

## Testing

### Unit Test Example

```java
@Test
void shouldAllowAccessWithRequiredAuthority() {
    // Given
    Authentication auth = createAuthentication("user1", Authority.MEMBERS_CREATE.getValue());
    SecurityContextHolder.getContext().setAuthentication(auth);

    // When & Then
    String result = testService.methodWithAuthority();
    assertThat(result).isEqualTo("success");
}
```

See `HasAuthorityAspectTest` for comprehensive test examples.

### Integration Test Example

```java
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Test
    @WithMockUser(authorities = {"MEMBERS:CREATE"})
    void shouldCreateMemberWhenAuthorized() throws Exception {
        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"MEMBERS:READ"})
    void shouldReturn403WhenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson))
                .andExpect(status().isForbidden());
    }
}
```

## Best Practices

### ✅ Do

1. **Use enum type reference** - Always use `Authority.MEMBERS_CREATE` not `"MEMBERS:CREATE"`
2. **Use at controller level** - Apply to REST endpoints
3. **Document required authorities** - Add Javadoc mentioning required authority
4. **Use for global authorities** - Apply to endpoints checking user's global permissions
5. **Test authorization** - Write tests for both authorized and unauthorized scenarios

Example:

```java
/**
 * Creates a new member.
 *
 * Requires authority: {@link Authority#MEMBERS_CREATE}
 *
 * @param request the member creation request
 * @return 201 Created with the created member resource
 */
@PostMapping("/members")
@HasAuthority(Authority.MEMBERS_CREATE)
public ResponseEntity<?> createMember(@RequestBody MemberRequest request) {
    // Implementation
}
```

### ❌ Don't

1. **Don't use for complex logic** - Use `@PreAuthorize` instead
2. **Don't apply to service layer** - Keep authorization at controller level
3. **Don't check context-specific permissions** - That's a business logic concern
4. **Don't forget to test** - Always test authorization scenarios
5. **Don't mix with @Secured** - Stick to one approach

## FAQ

### Q: Can I use @HasAuthority on service methods?

**A:** Technically yes, but it's not recommended. Authorization is typically handled at the controller/adapter layer.
Service methods should focus on business logic, and authorization decisions should be made before calling services.

### Q: What happens when authentication is null?

**A:** `AccessDeniedException` is thrown immediately. The user must be authenticated to access methods annotated with
`@HasAuthority`.

### Q: Can I combine @HasAuthority with other annotations?

**A:** Yes. You can use `@HasAuthority` together with other Spring Security annotations like `@PreAuthorize`:

```java
@HasAuthority(Authority.MEMBERS_CREATE)
@PreAuthorize("#id == authentication.principal.userId")
public ResponseEntity<?> createMember(@PathVariable UUID id) { ... }
```

### Q: Does @HasAuthority check context-specific permissions?

**A:** No. `@HasAuthority` only checks if the user has the required **global** authority. For context-specific
authorization (e.g., "can this user edit this specific member?"), implement that logic in your service layer or use more
complex `@PreAuthorize` expressions.

### Q: How do I add new authorities?

**A:** Add new constants to the `Authority` enum:

```java
public enum Authority {
    NEW_AUTHORITY("NEW:AUTHORITY", Scope.GLOBAL),
    // ...
}
```

Then use it in your annotations:

```java
@HasAuthority(Authority.NEW_AUTHORITY)
public ResponseEntity<?> someEndpoint() { ... }
```

## Field-Level Usage on Record Components

`@HasAuthority` can be placed directly on record components to control field visibility in responses and field-level authorization in PATCH requests.

### Response Field Filtering

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
record MemberDetailResponse(
    String firstName,

    @HasAuthority(Authority.MEMBERS_MANAGE)
    String birthNumber,  // hidden from users without MEMBERS:MANAGE

    @HasAuthority(Authority.MEMBERS_MANAGE)
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
    String bankAccountNumber  // shows "***" for unauthorized users
) {}
```

`FieldSecurityBeanSerializerModifier` evaluates `@HasAuthority` on record component accessors during Jackson serialization. No interface or proxy is needed.

### PATCH Request Field Authorization

```java
record UpdateMemberRequest(
    PatchField<String> email,  // no restriction

    @HasAuthority(Authority.MEMBERS_MANAGE)
    PatchField<String> birthNumber  // 403 if unauthorized user provides this field
) {}
```

`RequestBodyFieldAuthorizationAdvice` checks `@HasAuthority` on provided `PatchField` components. Non-provided fields are skipped.

### Combining with @OwnerVisible

`@HasAuthority` can be combined with `@OwnerVisible` for OR semantics — the field/method is accessible if the user has the authority **OR** is the data owner:

```java
record MemberDetailResponse(
    @OwnerId MemberId id,

    @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
    String birthNumber  // visible to admin OR the member themselves
) {}
```

On methods:
```java
@HasAuthority(Authority.MEMBERS_MANAGE)
@OwnerVisible
ResponseEntity<Void> updateMember(@PathVariable @OwnerId UUID id, ...) { ... }
```

### How annotations propagate on records

`@HasAuthority` has `@Target({TYPE, METHOD})`. When placed on a record component, Java propagates it to the accessor method (per JLS §8.10.1). The field-level security infrastructure reads annotations via `RecordComponent.getAccessor().getAnnotation(HasAuthority.class)`.

## See Also

- `Authority` - Enum of all available authorities
- `HasAuthority` - The annotation definition
- `HasAuthorityMethodInterceptor` - AuthorizationAdvisor for method-level security
- `FieldSecurityBeanSerializerModifier` - Jackson serializer for response field filtering
- `RequestBodyFieldAuthorizationAdvice` - Request body field authorization
- `FieldLevelAuthorizationTest` - Comprehensive test examples
- `HasAuthorityExamples` - Usage examples
- `SecurityConfiguration` - Spring Security configuration
