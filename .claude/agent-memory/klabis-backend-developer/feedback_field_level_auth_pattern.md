---
name: field-level-authorization-with-records
description: How to implement field-level authorization on Java records using Spring Security AuthorizationAdvisorProxyFactory
type: feedback
---

# Field-Level Authorization Pattern for Java Records

## Pattern Overview

Records are final — CGLIB cannot subclass them. `AuthorizationAdvisorProxyFactory` sets `proxyTargetClass=false` for final classes, falling back to JDK interface proxy. This means the record MUST implement an interface carrying the `@PreAuthorize` annotations.

## Required Structure

```java
// 1. Interface carries @PreAuthorize + @HandleAuthorizationDenied
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
interface MyDtoView {

    @JsonProperty
    String publicField();

    @JsonProperty
    @PreAuthorize("hasAuthority('MY:READ')")
    String hiddenField();      // null → disappears from JSON (NullDeniedHandler)

    @JsonProperty
    @PreAuthorize("hasAuthority('MY:READ')")
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)
    String maskedField();      // "***" when denied
}

// 2. Record implements the interface
record MyDto(String publicField, String hiddenField, String maskedField)
    implements MyDtoView {}

// 3. Controller proxies via AuthorizationProxyFactory (auto-configured by @EnableMethodSecurity)
@GetMapping("...")
MyDtoView getDto() {
    MyDto dto = new MyDto("public", "secret", "sensitive");
    return (MyDtoView) proxyFactory.proxy(dto);
}
```

## Key Requirements

- `@JsonProperty` on ALL interface methods — JDK proxy doesn't follow Bean naming convention, Jackson won't discover properties without explicit annotation
- `@JsonInclude(NON_NULL)` on the interface (not the record) — Jackson serializes based on interface type when that's the declared return type
- `NullDeniedHandler` and `MaskDeniedHandler` MUST be Spring `@Component` beans — `@HandleAuthorizationDenied` resolves handler via Spring context
- `AuthorizationProxyFactory` bean is auto-registered by `@EnableMethodSecurity` — do NOT declare a duplicate `@Bean`

## WebMvcTest Requirements

```java
@WebMvcTest(controllers = MyController.class)
@Import({NullDeniedHandler.class, MaskDeniedHandler.class})  // handlers not auto-scanned in web slice
class MyControllerTest {
    @MockitoBean UserService userService;          // SecurityConfiguration dependency
    @MockitoBean UserDetailsService userDetailsService;  // SecurityConfiguration dependency
}
```

**Why:** `@WebMvcTest` only scans web-layer beans. Security handler components need explicit `@Import`.

## Infrastructure Location

- `NullDeniedHandler` — `com.klabis.common.security`
- `MaskDeniedHandler` — `com.klabis.common.security`
- `PreAuthorizePropertyFilter` — `com.klabis.common.security` (evaluates @PreAuthorize expressions)
- `HalFormsAuthorizationPostProcessor` — `com.klabis.common.hateoas` (BPP wrapping HAL template builder)
- `AuthorizationProxyFactory` — auto-configured, just `@Autowired`

## HAL+FORMS Template Property Filtering (Phase 2 — automatic via Jackson)

The same `@PreAuthorize` annotations on interface methods that control JSON field visibility also
automatically filter `_templates.*.properties` in HAL+FORMS responses. No need for `klabisAfford()`.

### How it works

`HalFormsAuthorizationPostProcessor` wraps the `halFormsTemplateBuilder` bean (a Spring HATEOAS internal)
with a CGLIB proxy via `ProxyFactory`. The proxy intercepts `findTemplates()`, temporarily replaces
each affordance model's `input` field (via reflection on `AffordanceModel.input`) with an
`AuthorizingInputPayloadMetadata` that filters `stream()` by `PreAuthorizePropertyFilter`.

```java
// In controller — standard afford() works automatically:
model.add(klabisLinkTo(methodOn(MyController.class).get(id)).withSelfRel()
    .andAffordance(afford(methodOn(MyController.class).update(id, null))));
```

### Registration in @WebMvcTest

`HateoasConfiguration` declares the BPP as `static @Bean` (important for BPP lifecycle). It creates
`PreAuthorizePropertyFilter` directly (not injected) to avoid `UnsatisfiedDependencyException`
in `@WebMvcTest` contexts where security components may not be present.

```java
@Bean
static BeanPostProcessor halFormsAuthorizationPostProcessor() {
    return new HalFormsAuthorizationPostProcessor(new PreAuthorizePropertyFilter());
}
```

Tests that use `@WebMvcTest` with `@Import(HateoasConfiguration.class)` get the filtering for free.

## What Doesn't Work

- Annotating record component directly (no Spring AOP interception on the record itself)
- Using `@AuthorizeReturnObject` on repositories returning records (records are final, CGLIB fails)
- Registering a duplicate `authorizationProxyFactory` bean (causes `BeanDefinitionOverrideException`)
- Injecting `PreAuthorizePropertyFilter` as a @Bean parameter in the static BPP factory method — causes
  `UnsatisfiedDependencyException` in @WebMvcTest contexts
