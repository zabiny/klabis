# REST Infrastructure Adapter

The spec-first REST layer: generated `*Api` interfaces, HAL/HATEOAS wiring, postprocessors,
and DTO<->domain conversion. `docs/openapi/spec/*.yaml` is the source of truth for everything
the generator emits — see the `klabis-api-spec` skill for authoring that spec.


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

Writing any of these on the controller is either drift (a hand-written `@Operation` that no longer matches the spec) or a silent break (see the HV000151 note below). **To change an endpoint's URL, authority, status codes, or validation, edit the spec YAML and regenerate — never patch the controller.** The `klabis-api-spec` skill covers the spec authoring side (`x-klabis-*` extensions, module layout).

The generated interface also exposes path constants — `MembersApi.PATH_GET_MEMBER` = `"/api/members/{id}"` — for anything that needs the literal path (security config, tests) rather than re-typing it.

The controller keeps only: `@PrimaryAdapter`, `@RestController`, a `@RequestMapping(produces = …)` default, `@ExposesResourceFor(Aggregate.class)`, and `@Override` on each method. Note there is no `value=`/`path=` on the class-level `@RequestMapping` — the full path comes from the interface.

## `@HasAuthority` — declared in the spec, not the controller

`@HasAuthority(Authority.X)` is the type-safe alternative to `@PreAuthorize("hasAuthority('X:Y')")` for **single-authority global checks**, enforced by `HasAuthorityMethodInterceptor` (AuthorizationAdvisor); failure throws `AccessDeniedException` → 403.

For spec'd endpoints you do not write it — set `x-klabis-authority: MEMBERS_READ` on the operation and the generator emits the annotation onto the interface method. Omitting the extension means "any authenticated caller" (per the `/api/**` `.authenticated()` rule), which is a deliberate choice worth a comment in the YAML rather than an accident.

Reach for a hand-written `@PreAuthorize` on the override only when you need boolean logic, parameter access, or context-specific rules that a single authority cannot express — that is the one authorization concern the spec cannot carry.

## Field-Level Authorization on Controller Methods

Field-level authorization on the request DTO (`@HasAuthority`, `@OwnerVisible` on `JsonNullable<T>` components) is enforced by `RequestBodyFieldAuthorizationAdvice`. Single command path — no role-based branching in the controller. On a generated DTO these come from the spec's `x-klabis-authority` / `x-klabis-owner-visible` field extensions; the `@OwnerId` marker on the path variable is declared in the spec too.

```java
@Override
public ResponseEntity<Void> updateMember(@PathVariable UUID id,
                                         UpdateMemberRequest request,
                                         @ActingUser CurrentUserData currentUser) {
    MemberId memberId = new MemberId(id);  // Convert UUID → type-safe ID at boundary
    managementService.updateMember(memberId, UpdateMemberRequestMapper.toCommand(request, currentUser.userId()));
    return ResponseEntity.noContent().build();
}
```

Note the override repeats `@PathVariable` here but carries no `@RequestBody` on `request` — that asymmetry is deliberate and explained under "Which annotations belong on the override".

## HATEOAS — Controllers Return Plain DTOs; HalResponseBodyAdvice Wraps Them

Controller methods return the plain JSON-payload type from the generated API interface (`ResponseEntity<SomeResponse>` / `ResponseEntity<Page<SomeResponse>>`) — the generator does not produce `EntityModel`/`PagedModel` return types. Hypermedia wrapping happens **after** the controller returns, via `HalResponseBodyAdvice` (a `ResponseBodyAdvice` in `com.klabis.common.ui`), driven by a request-scoped `HalResponseContext` that the controller populates with the domain object(s) behind the DTO.

**Controller — return the plain DTO, stash the domain object(s) in `HalResponseContext` before returning:**

```java
@Override
public ResponseEntity<MemberDetailsResponse> getMember(@PathVariable UUID id, @ActingUser CurrentUserData currentUser) {
    Member member = managementService.getMemberAndRecordView(new MemberId(id), currentUser.userId(), ...);

    HalResponseContext.setDomain(member);          // must run after everything that can throw
    return ResponseEntity.ok(conversionService.convert(member, MemberDetailsResponse.class));
}
```

For a collection, use `setDomainList` — same order as the DTO content, paired 1:1 by index. This works for both a `Page<Dto>` (wrapped into `PagedModel`) and a plain `List<Dto>`/`Collection<Dto>` (wrapped into `CollectionModel`); a size mismatch between the two lists fails fast with `IllegalStateException` rather than silently pairing the wrong domain object with a DTO:

```java
@Override
public ResponseEntity<Page<MemberSummaryResponse>> listMembers(..., @ParameterObject Pageable pageable, ...) {
    Page<Member> memberPage = memberRepository.findAll(filter, pageable);

    HalResponseContext.setDomainList(memberPage.getContent());
    return ResponseEntity.ok(memberPage.map(member -> conversionService.convert(member, MemberSummaryResponse.class)));
}
```

**A second, independently-shaped collection alongside a single-item payload** goes through `HalResponseContext.embed(collection, ItemType.class)`, which renders it under `_embedded` next to the item's `_links`/`_templates`. It lives on the controller rather than in a postprocessor because the data usually needs a port the controller already holds, and because the payload's postprocessor is often shared with a *list* endpoint that has no such collection. One collection per response — a second call replaces the first. Current callers: `EventController` (registrations on an event) and `MembershipFeeGroupController` (members in a group).

```java
HalResponseContext.setDomain(group);
HalResponseContext.embed(buildGroupMembers(group), MemberInGroupResponse.class);
return ResponseEntity.ok(conversionService.convert(group, MembershipFeeGroupResponse.class));
```

**Always call `HalResponseContext.set*` last, after any code that can throw.** If the controller throws afterwards, `MvcExceptionHandler` returns a `ProblemDetail`; `HalResponseBodyAdvice` detects that and clears the context instead of wrapping the error body, but only if nothing between `set*` and the exception can leave stale context data for a *different* concern.

**What the advice does, automatically, with no controller involvement:**
- Single DTO → wraps it in `EntityModelWithDomain<T, D>` and runs it through every `RepresentationModelProcessor` bean — including `ModelWithDomainPostprocessor<Dto, Aggregate>` postprocessors.
- `Page<Dto>` → runs it through `PagedResourcesAssembler`, pairing each DTO with its domain object via `HalResponseContext`'s stashed list, then derives the **self link directly from the current request's path and query parameters** (no `klabisLinkTo` call needed for the self link — the controller method already ran and passed authorization for exactly this request).
- `Collection<Dto>` (a plain `List`, no paging) → same pairing, wrapped into a `CollectionModel` with a self link built the same way.
- Anything a postprocessor registered via `HalResponseContext.embed` is rendered into `_embedded` **after** the processors run, so it lands beside the links and templates they added.
- Non-HAL content types (e.g. `MemberOptionResponse` served as plain `application/json`) are left untouched — the advice checks `selectedContentType` and only wraps `HAL_JSON`/`HAL_FORMS_JSON` responses.
- A `ProblemDetail` error body is never wrapped, and the context is cleared so nothing leaks into a later request on the same thread pool.
- A body that is already a `RepresentationModel` is passed through untouched: without a `HalResponseContext` entry the advice does nothing.

**Postprocessor — extend `ModelWithDomainPostprocessor<T, D>`, which receives the DTO-shaped `EntityModel<T>` and the domain aggregate `D`:**

```java
@MvcComponent
class MemberDetailsPostprocessor extends ModelWithDomainPostprocessor<MemberDetailsResponse, Member> {

    @Override
    public void process(EntityModel<MemberDetailsResponse> dtoModel, Member member) {
        klabisLinkTo(methodOn(MembersApi.class).getMember(member.getId().uuid(), null))
            .map(link -> {
                var self = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(member.getId().uuid(), null, null)));
                if (member.isActive()) {
                    self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).suspendMember(member.getId().uuid(), null, null)));
                } else {
                    self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).resumeMember(member.getId().uuid(), null)));
                }
                return self;
            })
            .ifPresent(dtoModel::add);
    }
}
```

**Collection-level affordances (not per item) go on the `PagedModel` itself**, in a plain `RepresentationModelProcessor<PagedModel<EntityModel<Dto>>>` — the self link already exists (built by the advice), this processor only adds affordances that point at *other* endpoints:

```java
@MvcComponent
class MemberListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<MemberSummaryResponse>>> {

    @Override
    public PagedModel<EntityModel<MemberSummaryResponse>> process(PagedModel<EntityModel<MemberSummaryResponse>> pagedModel) {
        pagedModel.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(null, null, null)))
                .andAffordances(klabisAfford(methodOn(RegistrationApi.class).registerMember(null, null))));
        return pagedModel;
    }
}
```

**Why this pattern:**
- Controllers return the exact type the OpenAPI-generated API interface requires — no `EntityModel`/`PagedModel` in the method signature, so the generated interface can be implemented directly.
- State-driven affordances read from the real aggregate (`member.isActive()`), so hypermedia stays a function of domain state rather than of what the DTO happens to carry. Spring HATEOAS's own `HandlerMethodReturnValueHandler` only fires for return values that are already a `RepresentationModel`, which is why the advice invokes the postprocessors instead.
- The self link for a collection is built once, generically, by the advice for every paginated endpoint — no `klabisLinkTo(methodOn(...).listMembers(...))` call re-deriving the current request in the controller.
- Non-aggregate-backed responses (pure projections like `MemberOptionResponse`, served as plain JSON) are naturally skipped — no `HalResponseContext` entry means the advice passes the body through unchanged.

## HATEOAS Rules (NON-NEGOTIABLE)

Use `klabisLinkTo()` (returns `Optional<WebMvcLinkBuilder>`) and `klabisAfford()` — not standard Spring HATEOAS helpers.

- **`methodOn(...)` takes the generated `*Api` interface, never the controller class.** Write
  `methodOn(MembersApi.class)`, not `methodOn(MemberController.class)`. Java does not inherit
  parameter annotations from an interface, so an affordance recorded against the implementation only
  finds `@RequestBody` if that override happens to repeat it. When it does not, `HalFormsSupport`
  silently skips `HalFormsInputPayloadMetadata` and the `_templates` entry comes back with every
  field `readOnly: true` — no error, no failing link assertion, just a form the UI cannot submit.
  `AffordanceRoutingArchitectureTest` fails the build if a controller class reaches `methodOn`.
- Links (`withSelfRel()`, `withRel()`) — ONLY for GET endpoints
- Affordances (`klabisAfford()`) — ONLY for POST/PUT/PATCH/DELETE endpoints
- POST/PUT/PATCH/DELETE return 204 No Content or 201 Created with Location header — no response body
- `klabisAfford` handles authorization internally — do not duplicate authorization checks

### Which annotations belong on the override

The interface is the declaration site for everything the framework reads. The override carries the
method body and nothing else.

| Annotation | Where it belongs | Why |
|---|---|---|
| `@RequestBody`, `@RequestParam`, `@PathVariable` | interface only | Spring MVC and `HalFormsSupport` both read them from there |
| `@NotNull`, `@Size`, `@Pattern`, … | interface only | see the HV000151 note below |
| `@Valid` | either | a cascade marker, not a constraint — repeating it is legal |
| `@Parameter`, `@Operation`, `@ApiResponse` | interface only, and generated | the generator emits them from the spec (`documentationProvider=springdoc`); a hand-written copy on the controller only drifts |

**Bean Validation is all-or-nothing.** Hibernate Validator rejects an override that *redefines* the
parameter constraint configuration of the method it overrides (`ConstraintDeclarationException:
HV000151`), and it compares the parameter list as a whole. So removing `@RequestBody` from a method
whose sibling parameter still carries `@NotNull` produces a signature that differs from the
interface's and fails **at request time**, not at compile time. Either the override declares the
interface's full constraint set, or none of it. Prefer none.

## Root Navigation Postprocessors

Root navigation (`/api`) is **NOT** an aggregate-backed endpoint — `RootModel` is just a marker for the entry point and there is no domain object to piggy-back. Use a plain `RepresentationModelProcessor<EntityModel<RootModel>>`. Place the class at the end of the file containing the referenced controller, annotated `@MvcComponent`:

```java
@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembersApi.class).listMembers(Pageable.unpaged(), null))
            .ifPresent(link -> model.add(link.withRel("members")));
        return model;
    }
}
```

Same HATEOAS rules apply — no affordances to POST endpoints.

## Choosing the postprocessor type

| Situation | Use |
|---|---|
| Controller loads an aggregate and returns its detail/summary | `ModelWithDomainPostprocessor<Dto, Aggregate>` — controller calls `HalResponseContext.setDomain(aggregate)` before returning the plain DTO |
| Collection-level affordances to other endpoints | Plain `RepresentationModelProcessor<PagedModel<EntityModel<Dto>>>` — the self link itself is built by `HalResponseBodyAdvice`; this processor only adds affordances |
| Root navigation (`RootModel`) | Plain `RepresentationModelProcessor<EntityModel<RootModel>>` — no domain involved |
| Cross-module link enrichment where consuming module knows only the DTO's marker interface and the publishing controller does not expose the aggregate | Plain `RepresentationModelProcessor<EntityModel<MarkerInterface>>` |

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

## DTO ↔ Domain Mapping: `Converter<S,T>` + `ConversionService`

**Canonical pattern for any mapper conversion a controller or exception handler needs to call.** Controllers/handlers never inject a concrete `XyzMapper` type directly — they inject Spring's `ConversionService` and call `conversionService.convert(source, Target.class)`. Each externally-called conversion is its own MapStruct-generated `Converter<S,T>` bean, one interface per conversion (not one per mapper). `MonetaryAmountConverter` (`members.infrastructure.restapi`) is the original precedent; `MemberSummaryConverter`/`MemberDetailsConverter`/`DeactivationReasonConverter` and `BulkSyncResultConverter`/`BulkImportResultConverter` (`events.infrastructure.restapi`) follow the same shape:

```java
@Mapper(config = MapstructSpringMapperConfig.class)
interface MonetaryAmountConverter extends Converter<com.klabis.members.MonetaryAmount, MonetaryAmount> {

    @Override
    MonetaryAmount convert(com.klabis.members.MonetaryAmount source);
}
```

```java
// Controller
private final ConversionService conversionService;

ResponseEntity<MemberDetailsResponse> getMember(...) {
    Member member = managementService.getMemberAndRecordView(...);
    return ResponseEntity.ok(conversionService.convert(member, MemberDetailsResponse.class));
}
```

**Why:** `Converter` beans are auto-discovered by `@WebMvcTest` slices — `WebMvcTypeExcludeFilter` always lets them through regardless of the test's `controllers = {...}` filter — so a test never needs to `@MockitoBean`/`@Import` a mapper just because the controller under test happens to call it. This removed the slice-context coupling that direct mapper injection caused.

**Multi-argument conversions** (e.g. a mapper method that took `(request, currentUserId)`) get a wrapper record so the conversion stays a plain `Converter<S,T>`: `RegisterMemberRequestWithParameters(RegisterMemberRequest request, UserId registeredBy)`, converted via `conversionService.convert(new RegisterMemberRequestWithParameters(request, currentUserId), RegistrationPort.RegisterNewMember.class)`. When the construction logic has real branching (conditional nested objects, several domain factory calls) rather than a field-to-field mapping, write the `Converter` as a plain `@Component implements Converter<S,T>` instead of a MapStruct `@Mapper` — see `RegisterNewMemberConverter`.

**Collection conversions** rely on `ConversionService` element-reuse — never write a hand-rolled `Converter<Set<X>, Set<Y>>`. Use the `TypeDescriptor` overload so the element type survives generic erasure:

```java
conversionService.convert(source,
    TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(AuthorityDto.class)),
    TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Authority.class)));
```

### Pitfall: never `uses = <OtherMapper>` on a `Converter`

A `Converter`'s `@Mapper` annotation must never declare `uses = SomeInternalMapper.class` to reach nested/shared mapping logic. Because `Converter` beans are visible to **every** `@WebMvcTest` slice app-wide (not just tests for their own controller — see "Why" above), a `uses=` dependency on a plain, non-`Converter` `@Mapper` forces every unrelated slice in the whole app to additionally provide that mapper's generated `...Impl` bean (`@Import(SomeInternalMapperImpl.class)`), or the slice fails with `NoSuchBeanDefinitionException` at `mvcConversionService` construction time — a failure with no connection to the controller actually under test.

**Fix:** declare nested/shared mapping methods directly on the `Converter` interface that needs them (MapStruct resolves same-interface methods without `uses=`). If two `Converter`s need the same trivial one-liner (a nested enum mapping, a `@ValueMapping`), duplicate it — cheap, and it removes the cross-bean dependency:

```java
@Mapper(config = MapstructSpringMapperConfig.class)
interface BulkSyncResultConverter extends Converter<com.klabis.events.application.BulkSyncResult, BulkSyncResult> {

    @Override
    BulkSyncResult convert(com.klabis.events.application.BulkSyncResult source);

    @Mapping(target = "eventId", source = "eventId.value")
    EventSyncEntry toDto(com.klabis.events.application.BulkSyncResult.EventSyncEntry entry);

    @ValueMapping(source = "SYNCED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkSyncResult.SyncStatus status);
}
```

## `@MvcComponent` and `@WebMvcTest`

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

**Consequences for tests:**
- `@WebMvcTest` auto-loads `MvcConfiguration`, which then scans **all `com.klabis.**` packages** and picks up every `@MvcComponent` bean — cross-package, cross-module.
- **Do NOT** list postprocessors or `@MvcComponent` beans in `@WebMvcTest(controllers = {...})` or `@Import({...})` — it is redundant. They are discovered automatically.
- If a postprocessor's constructor depends on a non-MVC bean (e.g. a JDBC `SomeRepository`), the test must provide it via `@MockitoBean SomeRepository someRepository;`. Do NOT work around this with `@Lazy` on the constructor parameter — `@Lazy` only defers resolution, it doesn't supply the missing bean at runtime.

**Consequences for production code:**
- `@MvcComponent` is the correct annotation for anything in `infrastructure/restapi/` — controllers, postprocessors (`ModelWithDomainPostprocessor`, plain `RepresentationModelProcessor`), Jackson modules, HAL helpers.
- Cross-module postprocessors (e.g. a `groups.familygroup` postprocessor enriching a `Member` response) live in the consuming module and still just need `@MvcComponent`; the central scan finds them regardless of package.
