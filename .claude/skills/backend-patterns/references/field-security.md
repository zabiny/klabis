# Field-Level Authorization

Per-field visibility on response DTOs and per-field write authorization on request DTOs,
enforced during Jackson serialization and by a request-body advice respectively. Every decision is
made by `AuthorizationEvaluator` over the request's permission snapshot — see `authorization.md` for
the model (grants over everything vs. over targets, the evaluator rule).

Filter individual response fields and HAL+FORMS template properties based on the authenticated user's grants. Implemented via a custom Jackson 3 `ValueSerializerModifier` — annotations go directly on record components, no interface needed.

On generated request/response DTOs these annotations are emitted from the spec's `x-klabis-authority` / `x-klabis-owner-visible` / `x-klabis-target-id` / `x-klabis-read-authority` field extensions (see `klabis-api-spec`) — never add them by hand to generated code. The records below show the resulting shape.

## Pattern: Annotated Record (no interface)

Security annotations are placed directly on record components. `FieldSecurityBeanSerializerModifier` (extends `ValueSerializerModifier`) wraps secured properties in `SecuredBeanPropertyWriter`, which asks `AuthorizationEvaluator.canReadField(accessor, record)` during serialization. Records are final, so a Spring Security proxy would require an interface — unnecessary boilerplate. Module registered via `@JacksonComponent` on `FieldSecurityJacksonModule`.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)  // default: field hidden
record MemberDetailResponse(
    String firstName,  // always visible — no security annotation

    @HasAuthority({Authority.MEMBERS_MANAGE})
    String birthNumber,  // hidden for unauthorized users

    @HasAuthority({Authority.MEMBERS_MANAGE})
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)  // per-field override
    String bankAccountNumber  // masked as "***" for unauthorized users
) {}
```

Controller returns a plain record — no proxy call needed. Field security applies during Jackson serialization regardless of when in the response pipeline the DTO gets wrapped into `EntityModel` (see `hateoas.md`):

```java
@Override
public ResponseEntity<MemberDetailResponse> getMember(@PathVariable UUID id, @ActingUser CurrentUserData currentUser) {
    Member member = managementService.getMember(new MemberId(id));
    HalResponseContext.setDomain(member);
    return ResponseEntity.ok(conversionService.convert(member, MemberDetailResponse.class));
}
```

## Target and ownership (`@TargetId`, `@OwnerVisible`)

The record supplies the target its fields are about through the component marked `@TargetId(TargetType.X)`. An authority listed in `@HasAuthority` then passes when it is held over everything **or** over that target. `@OwnerVisible` adds "the target is the caller" with OR semantics:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
record MemberDetailResponse(
    @TargetId(TargetType.MEMBER) UUID id,         // the member the record is about
    String firstName,                              // always visible
    @HasAuthority({Authority.MEMBERS_MANAGE}) @OwnerVisible
    String birthNumber,                            // MEMBERS_MANAGE over this member OR the member themself
    @OwnerVisible
    String email,                                  // visible only to the member themself
    @HasAuthority({Authority.MEMBERS_MANAGE})
    String suspensionNote                          // MEMBERS_MANAGE over this member
) {}
```

- Without `@TargetId`, when some component is `@OwnerVisible`, the single component convertible to UUID via `ConversionService` is taken as a member target. If ambiguous, mark it with `@TargetId` (spec: `x-klabis-target-id: MEMBER`).
- A `@TargetId` component holding a **collection** of ids is a set of owners, not one target: authorities must then be held over everything, and an `@OwnerVisible` field is visible to any of the owners.
- `isSelf` compares a member target with the user id and member id of `KlabisJwtAuthenticationToken` (`token.isSelf(id)`, used only inside the evaluator).

In collections (`GET /members`), each item is evaluated independently — owner sees more on their own record.

## Key rules

- `@JsonInclude(NON_NULL)` on the record — denied fields (handled by `NullDeniedHandler`) disappear from JSON
- Class-level `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)` sets default deny behavior
- Per-field override with `@HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)` for masked fields
- `@HasAuthority({...})` lists authorities of which any one suffices; SpEL `@PreAuthorize` on a field or record component is **not** supported — express the rule with `@HasAuthority`
- `@OwnerVisible` adds ownership-based access with OR semantics
- No interface, no proxy — `FieldSecurityBeanSerializerModifier` handles everything during serialization
- Never read these annotations yourself — `AuthorizationArchitectureTest` fails the build; ask the evaluator

## Field-Level Authorization on Request DTOs (PATCH)

`JsonNullable<T>` components with `@HasAuthority`, or `@OwnerVisible` are enforced by `RequestBodyFieldAuthorizationAdvice` via `AuthorizationEvaluator.canWriteField`. Only present fields are checked — absent (undefined) fields are skipped. An explicit `null` counts as present, so it is still authorized. The target is read from the handler method's `@TargetId @PathVariable` parameter.

```java
record UpdateMemberRequest(
    JsonNullable<String> email,  // no annotation — anyone can update

    @HasAuthority({Authority.MEMBERS_MANAGE})
    JsonNullable<String> birthNumber,  // only MEMBERS_MANAGE — 403 otherwise

    @HasAuthority({Authority.MEMBERS_MANAGE}) @OwnerVisible
    JsonNullable<String> chipNumber,  // MEMBERS_MANAGE OR the member themself

    @HasAuthority({Authority.MEMBERS_MANAGE}) @ReadAuthority({Authority.MEMBERS_READ})
    JsonNullable<String> registrationNote  // writable by MEMBERS_MANAGE, shown read-only to MEMBERS_READ
) {}
```

The `@TargetId` path variable and the method-level authorization are declared on the **generated interface** (emitted from the spec's `x-klabis-authority` / `x-klabis-owner-visible` / `x-klabis-target-id` extensions), which is where the evaluator reads them:

```java
// generated <X>Api interface — not hand-written
@RequestMapping(method = RequestMethod.PATCH, value = MembersApi.PATH_UPDATE_MEMBER, ...)
@HasAuthority({Authority.MEMBERS_MANAGE})
@OwnerVisible
ResponseEntity<Void> updateMember(@TargetId(TargetType.MEMBER) @PathVariable UUID id, @RequestBody UpdateMemberRequest request);
```

The controller's override carries only `@Override` and the parameter names — but `@PathVariable` must stay on the override's parameter, because the advice resolves the path-variable name from the concrete handler method.

If an unauthorized user sends a present `JsonNullable` for a protected field, `FieldAuthorizationException` is thrown → HTTP 403.

## Available denied handlers (`com.klabis.common.security.fieldsecurity`)

| Handler | Behavior | Use case |
|---|---|---|
| `NullDeniedHandler` | Field absent from JSON | Default — hide sensitive fields |
| `MaskDeniedHandler` | Field shows `"***"` | Show field existence without value |

## HAL+FORMS template filtering

`klabisAfford()` decides each request-record property through the same evaluator, using the target of the afforded invocation (its `@TargetId` argument):

- `canWriteField` → the property is offered as editable;
- otherwise `canReadRequestField` (a `@ReadAuthority` authority is held) → the property is offered `readOnly`;
- otherwise the property is omitted from the template.

No extra configuration needed. The template itself is offered only when `canInvoke` passes for the afforded method.

## Reference implementation

- Serializer: `com.klabis.common.security.fieldsecurity.FieldSecurityBeanSerializerModifier`, `SecuredBeanPropertyWriter`
- Request auth: `com.klabis.common.security.fieldsecurity.RequestBodyFieldAuthorizationAdvice`
- Decisions: `com.klabis.common.authorization.AuthorizationEvaluator` (`canReadField`, `canWriteField`, `canReadRequestField`)
- Annotations: `@HasAuthority`, `@TargetId`, `@OwnerVisible`, `@ReadAuthority`
- Handlers: `com.klabis.common.security.fieldsecurity.NullDeniedHandler`, `MaskDeniedHandler`
- Tests: `FieldLevelAuthorizationTest`, `TargetedFieldAuthorizationTest`, `HalFormsReadOnlyPropertiesTest`
- HAL+FORMS filtering: `com.klabis.common.ui.HalFormsSupport` (`FieldAuthorization`)
