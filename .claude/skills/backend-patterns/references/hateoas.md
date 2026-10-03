# HATEOAS & HAL-FORMS

How responses get `_links`, `_templates` and `_embedded`: `HalResponseContext`,
`HalResponseBodyAdvice`, postprocessors, `klabisLinkTo`/`klabisAfford`/`klabisAffordWithOptions`.

## Contents
- Controllers Return Plain DTOs; HalResponseBodyAdvice Wraps Them
- HATEOAS Rules (NON-NEGOTIABLE)
- HAL-FORMS options
- Root Navigation Postprocessors
- Choosing the postprocessor type

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

Use `klabisLinkTo()` (returns `Optional<WebMvcLinkBuilder>`), `klabisAfford()` and — when a property
needs a non-default option set — `klabisAffordWithOptions()`; never the standard Spring HATEOAS helpers.

- **`methodOn(...)` takes the generated `*Api` interface, never the controller class.** Write
  `methodOn(MembersApi.class)`, not `methodOn(MemberController.class)`. Java does not inherit
  parameter annotations from an interface, so an affordance recorded against the implementation only
  finds `@RequestBody` if that override happens to repeat it. When it does not, `HalFormsSupport`
  silently skips `HalFormsInputPayloadMetadata` and the `_templates` entry comes back with every
  field `readOnly: true` — no error, no failing link assertion, just a form the UI cannot submit.
  `AffordanceRoutingArchitectureTest` fails the build if a controller class reaches `methodOn`.
  See `rest-controller.md`, "Which annotations belong on the override".
- Links (`withSelfRel()`, `withRel()`) — ONLY for GET endpoints
- Affordances (`klabisAfford()`) — ONLY for POST/PUT/PATCH/DELETE endpoints
- POST/PUT/PATCH/DELETE return 204 No Content or 201 Created with Location header — no response body
- `klabisAfford` handles authorization internally — do not duplicate authorization checks

## HAL-FORMS options

Enum-typed request properties get their options **automatically** — never hand-write them.
`EnumOptionsAutoConfiguration` (ADR-007) scans records in `*.infrastructure.restapi` and registers
every enum property (also inside `JsonNullable`/`Optional`/collections) via
`HalFormsConfiguration.withOptions(payloadType, property, …)`. Values are the `@JsonValue` strings
in enum declaration order, serialized as value-only `options.inline: ["A","B"]`; the frontend
localises them (see `x-hal-input-type` in `klabis-api-spec`).

Use explicit options only when the offered set is **not** "all enum constants" — a filtered subset
or data-driven values. The single entry point is `klabisAffordWithOptions`:

```java
klabisAffordWithOptions(
        methodOn(PermissionsApi.class).updatePermissions(id, null),
        Map.of("authorities", HalFormsOptionsDef.values(assignableAuthorities)));
```

| `HalFormsOptionsDef` | Wire format | Use for |
|---|---|---|
| `values(Collection<String>)` | `inline: ["A","B"]` | enum subset — frontend translates the values |
| `new Inline(List<HalFormsInlineOption>)` | `inline: [{value, prompt}]` | server-provided labels (e.g. tier names) |
| `remote(methodOn(...))` | `link: {href}` | large or lazily loaded sets (member pickers) |

Rules:
- Explicit options override the auto-registered ones for that property; an **empty** `values`/`Inline`
  list falls back to them (it does not mean "no options").
- Options are bound to the one affordance built in that call — they never leak into other templates.
- Data needed for the options (ports, repositories) may be injected straight into the postprocessor —
  register the dependency in the owning module's `*WebMvcMockitoBeans` (see `testing-guide.md`). Real examples: `PermissionController` (assignable
  authorities), `SyncStatePostprocessor` (`SynchronizationPort.supportedResolutions`),
  `MembershipFeeTierController` (`Inline`).

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
