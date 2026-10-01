# REST Controller

Spec-first controllers implementing generated `*Api` interfaces, PATCH handling and current-user
parameters. `docs/openapi/spec/*.yaml` is the source of truth for everything the generator emits —
see the `klabis-api-spec` skill for authoring that spec. Response hypermedia is in `hateoas.md`,
DTO↔domain conversion in `dto-mapping.md`.

## Contents
- Spec-First: the Controller Implements a Generated `*Api` Interface
- `@HasAuthority` — declared in the spec
- PATCH controller method / PATCH endpoints (prefill + overlay)
- Which annotations belong on the override (HV000151)
- Current User Parameters (`@ActingUser` / `@ActingMember`)
- `@MvcComponent`

## Spec-First: the Controller Implements a Generated `*Api` Interface

The REST layer is **spec-first**. `docs/openapi/spec/*.yaml` is the source of truth; the build generates, per module, an `*Api` interface plus the request/response DTOs into `backend/build/generated/openapi/<module>/`. A controller's job is to `implements <X>Api` and supply method bodies — nothing else. The only controllers outside this rule are ones serving no spec'd endpoint at all (e.g. `PwaDisabledController`).

```java
@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(Member.class)
public class MemberController implements MembersApi {

    private final ManagementPort managementService;
    private final ConversionService conversionService;
    // constructor injection …

    @Override
    public ResponseEntity<MemberDetailsResponse> getMember(@PathVariable UUID id,
                                                           @ActingUser CurrentUserData currentUser) { … }
}
```

**What is generated onto the interface — so it must NOT be written on the controller:**

| Concern | Generated from | Spec source |
|---|---|---|
| `@RequestMapping(method=…, value=PATH_…, produces=…)` | path + operation | the path item itself |
| `@HasAuthority(Authority.X)` | `x-klabis-authority: MEMBERS_READ` | per-operation |
| `@Operation`, `@Parameter`, `@ApiResponse`, `@Tag`, `@SecurityRequirement` | `documentationProvider=springdoc` | summary/description/responses |
| `@RequestBody`, `@RequestParam`, `@PathVariable`, `@Valid`, Bean Validation | parameter + schema definitions | parameters/requestBody |

Writing any of these on the controller is either drift (a hand-written `@Operation` that no longer matches the spec) or a silent break (see "Bean Validation is all-or-nothing" below). **To change an endpoint's URL, authority, status codes, or validation, edit the spec YAML and regenerate — never patch the controller.** The `klabis-api-spec` skill covers the spec authoring side (`x-klabis-*` extensions, module layout).

The generated interface also exposes path constants — `MembersApi.PATH_GET_MEMBER` = `"/api/members/{id}"` — for anything that needs the literal path (security config, tests) rather than re-typing it.

The controller keeps only: `@PrimaryAdapter`, `@RestController`, a `@RequestMapping(produces = …)` default, `@ExposesResourceFor(Aggregate.class)`, and `@Override` on each method. Note there is no `value=`/`path=` on the class-level `@RequestMapping` — the full path comes from the interface.

## `@HasAuthority` — declared in the spec, not the controller

`@HasAuthority(Authority.X)` is the type-safe alternative to `@PreAuthorize("hasAuthority('X:Y')")` for **single-authority global checks**, enforced by `HasAuthorityMethodInterceptor` (AuthorizationAdvisor); failure throws `AccessDeniedException` → 403. The interceptor's pointcut only considers classes under `com.klabis.*` — a bean from any other package is never secured by `@HasAuthority`/`@OwnerVisible`.

For spec'd endpoints do not write it by hand — set `x-klabis-authority: MEMBERS_READ` on the operation and the generator emits the annotation onto the interface method. Omitting the extension means "any authenticated caller" (per the `/api/**` `.authenticated()` rule), which is a deliberate choice worth a comment in the YAML rather than an accident.

Reach for a hand-written `@PreAuthorize` on the override only for boolean logic, parameter access, or context-specific rules that a single authority cannot express — that is the one authorization concern the spec cannot carry.

## PATCH controller method

Field-level write authorization on the request DTO is enforced before the controller runs — see `field-security.md`. The controller has a single command path, no role-based branching:

```java
@Override
public ResponseEntity<Void> updateMember(@PathVariable UUID id,
                                         UpdateMemberRequest request,
                                         @ActingUser CurrentUserData currentUser) {
    MemberId memberId = new MemberId(id);  // Convert UUID → type-safe ID at boundary
    var prefilled = managementService.prefilledUpdateCommand(memberId);
    var command = UpdateMemberRequestMapper.toCommand(request, prefilled, currentUser.userId());
    managementService.updateMember(memberId, command);
    return ResponseEntity.noContent().build();
}
```

Note the override repeats `@PathVariable` here but carries no `@RequestBody` on `request` — that asymmetry is deliberate and explained under "Which annotations belong on the override". The prefill + overlay step is the PATCH pattern below.

## PATCH endpoints — `JsonNullable` stays in the adapter, never reaches the domain

A PATCH request has three states per field: **undefined** (absent → leave alone), **present-null** (`"x": null` → clear), **present-value** (set). The generated request DTO carries every field as `org.openapitools.jackson.nullable.JsonNullable<T>` to preserve that distinction on the wire (see the `PatchRequestWrapperArchitectureTest` guard, and `field-security.md` for why the wrapper also gates field-level auth).

**That `JsonNullable` must not leak past the mapper.** The domain update command (`Member.UpdateMember`, `UpdateTrainingGroupCommand`, …) holds plain domain types only — no `JsonNullable`, no framework import. The three-state merge is resolved in `infrastructure/restapi` by the **prefill + overlay** pattern:

1. **Prefill** — the application port exposes a baseline command built from current aggregate state:

   ```java
   // ManagementPort
   Member.UpdateMember prefilledUpdateCommand(MemberId memberId);   // = Member.UpdateMember.from(load(id))
   ```

   The aggregate owns the `from(aggregate)` factory: every field set to its current value, so applying the command unchanged is a no-op.

2. **Overlay** — the mapper takes the prefilled command as the baseline and writes only the fields the request actually carries, via the `@RecordBuilder` copy-builder:

   ```java
   static Member.UpdateMember toCommand(UpdateMemberRequest request, Member.UpdateMember prefilled, UserId updatedBy) {
       var b = MemberUpdateMemberBuilder.builder(prefilled).updatedBy(updatedBy);
       overlay(request.email(), v -> b.email(v == null ? null : EmailAddress.of(v)));      // clearable: present-null clears
       overlay(request.chipNumber(), b::chipNumber);
       overlayValue(request.firstName(), b::firstName);                                     // no cleared state: present-null retains
       // … one line per field
       return b.build();
   }

   /** present (incl. present-null) → apply; undefined → leave the baseline value. */
   private static <T> void overlay(JsonNullable<T> field, Consumer<T> apply) {
       if (field.isPresent()) apply.accept(field.get());
   }

   /** For fields with no cleared state: only a present non-null value overrides the baseline. */
   private static <T> void overlayValue(JsonNullable<T> field, Consumer<T> apply) {
       field.ifPresent(v -> { if (v != null) apply.accept(v); });
   }
   ```

3. **Apply** — the domain method applies the full snapshot unconditionally (`this.email = command.email()`), with no per-field "was it set?" branching. Validation runs over the resolved end-state values.

**Consequences:**
- This mapper is **not** a `Converter<S,T>` — it is two inputs (request + prefilled baseline) plus merge semantics. Keep it a hand-written package-private class with static `toCommand`; the controller calls it directly, not through `ConversionService`.
- An empty PATCH body (`{}`) is a legitimate no-op that returns 204 — there is no "must contain at least one field" rule in the domain any more.
- `RequestBodyFieldAuthorizationAdvice` is unaffected — it inspects the `JsonNullable` components of the **request DTO**, before the mapper runs.
- `@WebMvcTest` for a PATCH controller must stub **both** `prefilledUpdateCommand(...)` (return `Aggregate.UpdateCmd.from(stubAggregate())`) and the update method; without the first, the mapper hits `builder(null)` → NPE before the endpoint logic runs.
- Domain unit tests build the command from the baseline too: `MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member)).email(newValue).build()` — never the bare `builder()`, which would leave required fields null.

## Which annotations belong on the override

The interface is the declaration site for everything the framework reads. The override carries the
method body and nothing else.

| Annotation | Where it belongs | Why |
|---|---|---|
| `@RequestBody`, `@RequestParam`, `@PathVariable` | interface only | Spring MVC and `HalFormsSupport` both read them from there |
| `@NotNull`, `@Size`, `@Pattern`, … | interface only | see "Bean Validation is all-or-nothing" below |
| `@Valid` | either | a cascade marker, not a constraint — repeating it is legal |
| `@Parameter`, `@Operation`, `@ApiResponse` | interface only, and generated | the generator emits them from the spec (`documentationProvider=springdoc`); a hand-written copy on the controller only drifts |

**Bean Validation is all-or-nothing.** Hibernate Validator rejects an override that *redefines* the
parameter constraint configuration of the method it overrides (`ConstraintDeclarationException:
HV000151`), and it compares the parameter list as a whole. So removing `@RequestBody` from a method
whose sibling parameter still carries `@NotNull` produces a signature that differs from the
interface's and fails **at request time**, not at compile time. Either the override declares the
interface's full constraint set, or none of it. Prefer none.

## Current User Parameters (`@ActingUser` / `@ActingMember`)

`CurrentUserArgumentResolver` resolves two annotations in controller method parameters:

**`@ActingUser CurrentUserData`** — resolves the authenticated user from the JWT token. Falls back gracefully when no member is associated with the user (e.g., admin-only users):

```java
@Override
public ResponseEntity<MemberDetailsResponse> getMyProfile(@ActingUser CurrentUserData currentUser) {
    // currentUser is resolved from the authenticated JWT token
}
```

Both annotations are declared on the generated interface (the spec marks the parameter), so the override just repeats the parameter — see the annotation table above.

**`@ActingMember MemberId`** — resolves the authenticated user's `MemberId` from the JWT `memberIdUuid` claim. Throws `MemberProfileRequiredException` (HTTP 403) if the user has no member profile. Use this instead of manually calling `requireMemberProfile(currentUser)`:

```java
@Override
public ResponseEntity<Void> inviteMember(@PathVariable UUID id,
                                         @ActingMember MemberId actingMember,
                                         InviteRequest request) {
    // actingMember is guaranteed to be a member — throws 403 otherwise
}
```

Use `@ActingUser` when the endpoint is accessible to non-member users (admins). Use `@ActingMember` when the endpoint requires a member profile.

## `@MvcComponent`

`@MvcComponent` (`com.klabis.common.mvc.MvcComponent`) is a project-specific marker for presentation-layer beans (postprocessors, link processors, MVC helpers). It is meta-annotated `@Component`, but it is NOT a generic alias — `MvcConfiguration` wires it up via a targeted component scan:

```java
@ComponentScan(
    basePackages = "com.klabis",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = MvcComponent.class),
    useDefaultFilters = false
)
@Configuration
class MvcConfiguration implements WebMvcConfigurer { ... }
```

Test-slice consequences (global scan, `@WithPostprocessors`) are in `testing-guide.md`, "Controller Tests".

**Consequences for production code:**
- `@MvcComponent` is the correct annotation for presentation-layer beans in `infrastructure/restapi/` — postprocessors (`ModelWithDomainPostprocessor`, plain `RepresentationModelProcessor`), Jackson modules, HAL helpers. Controllers keep `@RestController`.
- Cross-module postprocessors (e.g. a `groups.traininggroup` postprocessor enriching a `Member` response) live in the consuming module and still just need `@MvcComponent`; the central scan finds them regardless of package.
