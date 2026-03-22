---
name: field-level-authorization-with-records
description: How to implement field-level authorization on Java records using a custom Jackson BeanSerializerModifier (no interface needed)
type: feedback
---

# Field-Level Authorization Pattern for Java Records

## Pattern Overview

Field-level authorization is implemented via `FieldSecurityBeanSerializerModifier`, a Jackson `BeanSerializerModifier` that wraps `BeanPropertyWriter` instances for record components annotated with `@PreAuthorize` or `@HasAuthority`. Authorization is evaluated during serialization — no interface or JDK proxy is required.

## Required Structure

```java
// Record with security annotations directly on components
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
record MyDto(
    String publicField,

    @PreAuthorize("hasAuthority('MY:READ')")
    String hiddenField,           // skipped entirely when denied

    @PreAuthorize("hasAuthority('MY:READ')")
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
    String maskedField,           // "***" when denied

    @HasAuthority(Authority.MEMBERS_MANAGE)
    String authorityHiddenField,  // skipped entirely when denied

    @HasAuthority(Authority.MEMBERS_MANAGE)
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
    String authorityMaskedField   // "***" when denied
) {}

// Controller — plain EntityModel, no proxying needed
@GetMapping("...")
EntityModel<MyDto> getDto() {
    return EntityModel.of(new MyDto("public", "secret", "sensitive", "auth-secret", "auth-masked"));
}
```

## Key Requirements

- `@JsonInclude(NON_NULL)` on the record — denied fields with `NullDeniedHandler` are skipped (field not written at all via `SecuredBeanPropertyWriter`)
- `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)` on the record class sets the default deny handler for all secured components
- Component-level `@HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)` overrides the class default
- `FieldSecurityJacksonModule` registered via `@JsonComponent` — auto-discovered in both `@WebMvcTest` and `@SpringBootTest`

## Infrastructure Location

- `FieldSecurityBeanSerializerModifier` — `com.klabis.common.security.fieldsecurity`
- `SecuredBeanPropertyWriter` — `com.klabis.common.security.fieldsecurity`
- `FieldSecurityJacksonModule` — `com.klabis.common.security.fieldsecurity`
- `NullDeniedHandler` — `com.klabis.common.security.fieldsecurity`
- `MaskDeniedHandler` — `com.klabis.common.security.fieldsecurity`
- `SecuritySpelEvaluator` — `com.klabis.common.security.fieldsecurity` (evaluates @PreAuthorize SpEL)

## How it works

`FieldSecurityBeanSerializerModifier.changeProperties()` is called by Jackson before serializing any record type. For each `BeanPropertyWriter` whose corresponding record component has `@PreAuthorize` or `@HasAuthority`, it wraps the writer in `SecuredBeanPropertyWriter`.

`SecuredBeanPropertyWriter.serializeAsField()` evaluates the security expression at serialization time:
- Authorized → delegates to wrapped writer (normal serialization)
- Denied + `MaskDeniedHandler` → writes `"***"` as field value
- Denied + `NullDeniedHandler` or no handler → skips the field entirely (writes nothing)

## HAL+FORMS Template Property Filtering

The `HalFormsSupport` already inspects record component accessor methods for `@PreAuthorize` and `@HasAuthority` when building HAL+FORMS `_templates`. No additional changes needed — both response JSON filtering and template property filtering derive from the same annotations.

## WebMvcTest Requirements

Standard `@WebMvcTest` setup — no special imports needed for field security:

```java
@WebMvcTest(controllers = MyController.class)
class MyControllerTest {
    @MockitoBean UserService userService;
    @MockitoBean UserDetailsService userDetailsService;
}
```

`@JsonComponent`-annotated modules are auto-discovered by `@WebMvcTest`.

## What Doesn't Work (old approach — replaced)

The old approach required records to implement an interface with `@JsonProperty` + `@PreAuthorize` methods, and controllers to wrap DTOs with `AuthorizationAdvisorProxyFactory.proxy()`. This is no longer used:
- `ResponseBodyFieldAuthorizationAdvice` is retained as a no-op but no longer proxies anything
- Do NOT use `AuthorizationAdvisorProxyFactory` for response DTO field filtering
