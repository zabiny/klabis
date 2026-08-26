# Field-Level Authorization

Per-field visibility on response DTOs and per-field write authorization on request DTOs,
enforced during Jackson serialization and by a request-body advice respectively.


Filter individual response fields and HAL+FORMS template properties based on the authenticated user's authorities. Implemented via a custom Jackson 3 `ValueSerializerModifier` — annotations go directly on record components, no interface needed.

## Pattern: Annotated Record (no interface)

Security annotations are placed directly on record components. `FieldSecurityBeanSerializerModifier` (extends `ValueSerializerModifier`) evaluates them during Jackson serialization. This avoids the need for a separate interface — records are final so Spring Security's `AuthorizationAdvisorProxyFactory` (JDK proxy) would require an interface, which is unnecessary boilerplate. Module registered via `@JacksonComponent` on `FieldSecurityJacksonModule`.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)  // default: field hidden
record MemberDetailResponse(
    String firstName,  // always visible — no security annotation

    @PreAuthorize("hasAuthority('MEMBERS:MANAGE')")
    String birthNumber,  // hidden for unauthorized users

    @HasAuthority(Authority.MEMBERS_MANAGE)
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)  // per-field override
    String bankAccountNumber  // masked as "***" for unauthorized users
) {}
```

Controller returns a plain record — no proxy call needed. Field security applies during Jackson serialization regardless of when in the response pipeline the DTO gets wrapped into `EntityModel` (see the HATEOAS section above):

```java
@Override
public ResponseEntity<MemberDetailResponse> getMember(@PathVariable UUID id, @ActingUser CurrentUserData currentUser) {
    Member member = managementService.getMember(new MemberId(id));
    HalResponseContext.setDomain(member);
    return ResponseEntity.ok(conversionService.convert(member, MemberDetailResponse.class));
}
```

## Ownership-Based Field Authorization (@OwnerVisible)

Fields can be made accessible to the data owner using `@OwnerVisible`. Uses OR semantics with authority annotations:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
record MemberDetailResponse(
    @OwnerId MemberId id,                         // owner identifier
    String firstName,                              // always visible
    @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
    String birthNumber,                            // visible to admin OR owner
    @OwnerVisible
    String email,                                  // visible only to owner
    @HasAuthority(Authority.MEMBERS_MANAGE)
    String suspensionNote                          // visible only to admin
) {}
```

Owner discovery: single field whose type converts to UUID via `ConversionService` is used automatically. If ambiguous, annotate with `@OwnerId`. `OwnershipResolver` compares with `KlabisJwtAuthenticationToken.getMemberIdUuid()`.

In collections (`GET /members`), each item is evaluated independently — owner sees more on their own record.

## Key rules

- `OwnershipResolver` is lazy-resolved from `ApplicationContext` — eager injection causes `No ServletContext set` startup error
- `@JsonInclude(NON_NULL)` on the record — denied fields (handled by `NullDeniedHandler`) disappear from JSON
- Class-level `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)` sets default deny behavior
- Per-field override with `@HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)` for masked fields
- Both `@PreAuthorize` (SpEL) and `@HasAuthority` (type-safe) annotations are supported on record components
- `@OwnerVisible` adds ownership-based access with OR semantics
- No interface, no proxy — `FieldSecurityBeanSerializerModifier` handles everything during serialization

## Field-Level Authorization on Request DTOs (PATCH)

`JsonNullable<T>` components with `@PreAuthorize`, `@HasAuthority`, or `@OwnerVisible` annotations are enforced by `RequestBodyFieldAuthorizationAdvice`. Only present fields are checked — absent (undefined) fields are skipped. An explicit `null` counts as present, so it is still authorized. For `@OwnerVisible`, owner ID is read from the controller method's `@OwnerId @PathVariable` parameter.

```java
record UpdateMemberRequest(
    JsonNullable<String> email,  // no annotation — anyone can update

    @HasAuthority(Authority.MEMBERS_MANAGE)
    JsonNullable<String> birthNumber,  // only admin can update — 403 if unauthorized

    @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
    JsonNullable<String> chipNumber  // admin OR owner can update
) {}
```

The `@OwnerId` path variable and the method-level authorization are declared on the **generated interface** (emitted from the spec's `x-klabis-authority` / owner-visible extensions), which is where `RequestBodyFieldAuthorizationAdvice` reads them:

```java
// generated <X>Api interface — not hand-written
@RequestMapping(method = RequestMethod.PATCH, value = MembersApi.PATH_UPDATE_MEMBER, ...)
@HasAuthority(Authority.MEMBERS_MANAGE)
@OwnerVisible
ResponseEntity<Void> updateMember(@PathVariable @OwnerId UUID id, @RequestBody UpdateMemberRequest request);
```

The controller's override carries only `@Override` and the parameter names.

If an unauthorized user sends a present `JsonNullable` for a protected field, `FieldAuthorizationException` is thrown → HTTP 403.

## Available denied handlers (`com.klabis.common.security.fieldsecurity`)

| Handler | Behavior | Use case |
|---|---|---|
| `NullDeniedHandler` | Field absent from JSON | Default — hide sensitive fields |
| `MaskDeniedHandler` | Field shows `"***"` | Show field existence without value |

## HAL+FORMS template filtering

`klabisAfford()` automatically filters HAL+FORMS template properties based on the same `@PreAuthorize` / `@HasAuthority` annotations on record component accessors. If user lacks authority for a field, the property is excluded from the PATCH template. No extra configuration needed.

## Reference implementation

- Serializer: `com.klabis.common.security.fieldsecurity.FieldSecurityBeanSerializerModifier`, `SecuredBeanPropertyWriter`
- Request auth: `com.klabis.common.security.fieldsecurity.RequestBodyFieldAuthorizationAdvice`
- Method auth: `com.klabis.common.security.HasAuthorityMethodInterceptor`
- Ownership: `OwnershipResolver`, `DefaultOwnershipResolver`, `@OwnerVisible`, `@OwnerId`
- Handlers: `com.klabis.common.security.fieldsecurity.NullDeniedHandler`, `MaskDeniedHandler`
- Test: `com.klabis.common.security.fieldsecurity.FieldLevelAuthorizationTest`
- HAL+FORMS filtering: `com.klabis.common.ui.HalFormsSupport` (`isPropertyAuthorized`)
