# Design Decisions (ADRs)

This file records non-obvious architectural choices for the Klabis backend as Architecture Decision Records (ADRs). Read it before proposing significant architectural changes — the reasoning behind existing decisions is documented here. Add a new `ADR-NNN` section for any similarly-sized decision introduced in the future.

## ADR-001: Cross-module ports live in `<module>.application`, consume primary ports only

**Status:** Accepted

**Context:**

The Modulith exposes module functionality to other modules through jMolecules ports (`@PrimaryPort` / `@SecondaryPort`). Before this decision the convention was inconsistent: some cross-module ports lived in the module root package, others in `<module>.application`, and at least one consumer reached past a module's public API straight into a foreign repository. `KlabisUserDetailsService` (authorizationserver) injected `com.klabis.common.users.domain.UserPermissionsRepository` directly — a secondary port — carrying a standing `// TODO` acknowledging it should not depend on another module's repository.

This inconsistency made dependency rules unpredictable, let new modules couple to foreign repositories, and weakened the named-interface boundaries that Spring Modulith can verify.

**Decision:**

1. Cross-module ports (ports consumed across Modulith module boundaries) live in the `<module>.application` package, exposed via `@org.springframework.modulith.NamedInterface("application")` declared in that package's `package-info.java`. The module root package is NOT used for cross-module ports.

   ```java
   // com/klabis/<module>/application/package-info.java
   @org.springframework.modulith.NamedInterface("application")
   package com.klabis.<module>.application;
   ```

   `finance/application/package-info.java` is the reference. As part of this change `EventDataProvider`, `EventScheduleQuery`, and `MemberFinancialStatePort` moved from their module root packages into the respective `<module>.application` packages.

2. A module consumes another module's **primary port**, never a foreign repository or any other secondary port. The motivating example: `KlabisUserDetailsService` now consumes `com.klabis.common.users.application.PermissionService` (a primary port) instead of `UserPermissionsRepository` (a secondary port), removing the standing `// TODO`.

**Consequences:**

- Uniform, predictable dependency rules across the Modulith: cross-module collaboration always flows through a primary port published in `<module>.application`.
- Future modules cannot quietly depend on a foreign repository; the secondary-port/persistence layer of a module stays private to that module.
- The Modulith `ModuleStructureVerificationTest` enforces the named-interface boundary, so violations fail the build rather than being caught only in review.
- Slightly more indirection: a module that only needs read data must still expose (or reuse) a primary port rather than letting consumers reach into its repository.

**References:** OpenSpec change `unify-cross-module-ports`.

## ADR-002: Controllers return plain DTOs; `HalResponseBodyAdvice` wraps them into HAL models

**Status:** Accepted

**Context:**

The backend is migrating from code-first (springdoc) to spec-first OpenAPI generation: `docs/openapi/spec/` becomes the source of truth, and both Java DTOs and frontend types are generated from it. The `openapi-generator` `spring` generator produces Java **interfaces** for controllers to implement, with return types derived strictly from the OpenAPI response schemas — plain payload DTOs (records), never `EntityModel<T>`/`PagedModel<EntityModel<T>>`.

This conflicted with the project's existing HATEOAS convention (see the `backend-patterns` skill before this change): controllers built `EntityModel`/`PagedModel` directly — via `entityModelWithDomain(dto, domain)` for single items, or `PagedResourcesAssembler` plus an inline `klabisLinkTo(...).withSelfRel().andAffordances(...)` call for collections — and returned that wrapped type from the method signature.

Three ways to reconcile this with the generator were evaluated and rejected:

1. **`hateoas=true` generator option.** Decompiling `openapi-generator` showed this makes generated model classes `extend RepresentationModel<T>` — a single line in `pojo.mustache`. Records cannot extend a class, so this is incompatible with the project's record-based DTOs, and would additionally break `FieldSecurityBeanSerializerModifier` (reads annotations off record accessors only), `@RecordBuilder`, and MapStruct mapping.
2. **`schemaMappings` to a custom HAL wrapper type.** No generic parameter support in the generator's schema-mapping mechanism — cannot express `EntityModel<GeneratedDto>` this way without hand-writing every wrapper permutation.
3. **`responseWrapper` config option.** Verified experimentally: `responseType.mustache` places the configured wrapper **outside** `ResponseEntity`, not inside — it is designed for `CompletableFuture<ResponseEntity<T>>`, not `ResponseEntity<Wrapper<T>>`. Produces the wrong nesting (`EntityModel<ResponseEntity<T>>`) and cannot be corrected via configuration.

A fourth option — permanently forking the schema to carry both a plain DTO and a HAL envelope, verified only at the boundary — was rejected earlier in the same investigation as adding a permanent dual-mode maintenance burden for no runtime benefit.

**Decision:**

Controllers return the plain DTO type the generated API interface requires (`ResponseEntity<SomeResponse>` / `ResponseEntity<Page<SomeResponse>>`). Before returning, a controller stashes the domain object(s) backing the DTO in a request-scoped `HalResponseContext` (`com.klabis.common.ui`):

```java
HalResponseContext.setDomain(member);                    // single item
HalResponseContext.setDomainList(memberPage.getContent()); // Page content, same order
```

`HalResponseBodyAdvice`, a `ResponseBodyAdvice<Object>` registered as `@MvcComponent @ControllerAdvice`, runs in `beforeBodyWrite` and:

- Passes through unchanged anything that is not a HAL/HAL-FORMS media type (checked via `MediaTypes.HAL_JSON`/`HAL_FORMS_JSON` against `selectedContentType`), or any `ProblemDetail` error body — clearing the context in both cases so nothing leaks into a later request on the same thread.
- For a single DTO with a stashed domain object: wraps it in `EntityModelWithDomain<T, D>` (the same class the pre-existing postprocessor pattern already used) and runs it through every `RepresentationModelProcessor` bean via `RepresentationModelProcessorInvoker` — so existing `ModelWithDomainPostprocessor<Dto, Aggregate>` postprocessors require no changes.
- For a `Page<Dto>` with a stashed domain list: runs it through `PagedResourcesAssembler`, pairing each DTO with its domain object by index, then derives the **collection self link directly from the current request's path and parameters** (`HttpServletRequest.getRequestURI()` + a sorted `getParameterMap()`, decoded and re-encoded together to avoid double-encoding) — no `klabisLinkTo` authorization re-check is needed for the self link itself, since the controller method already ran and passed authorization for exactly this request. Collection-level affordances to *other* endpoints (which remain authorization-sensitive) are added separately by a plain `RepresentationModelProcessor<PagedModel<EntityModel<Dto>>>` (e.g. `MemberListPostprocessor`), not by the advice.

Why this works without an ordering conflict: Spring HATEOAS's own postprocessing (`RepresentationModelProcessorHandlerMethodReturnValueHandler`) only recognizes return values that are already a `RepresentationModel`. A plain DTO never satisfies that check, so HATEOAS skips postprocessing entirely for these endpoints — `HalResponseBodyAdvice` is the only thing invoking postprocessors for them, not something racing or double-running against the framework's own mechanism.

**Consequences:**

- A module can adopt spec-first OpenAPI generation for its controllers without abandoning the existing HATEOAS postprocessor pattern — `ModelWithDomainPostprocessor` subclasses are reused unchanged.
- Two request-scoped context slots (`setDomain`/`setDomainList`) are a new implicit contract: a controller **must** call them after all code that can throw, or a subsequent exception leaves stale context data that the `ProblemDetail` guard must clear defensively rather than the controller guaranteeing correctness by construction.
- Collection self-link construction moved out of controllers entirely and became generic (path + parameters from the request), removing a `klabisLinkTo(methodOn(...).listMembers(pageable, q, status, null))` call that had to be kept in sync with the method's own parameter list by hand.
- Not yet a project-wide requirement: as of this ADR, only two `MemberController` endpoints (`getMember`, `listMembers`) use this pattern; the rest of the codebase still returns `EntityModel`/`PagedModel` directly. Migrating the remaining modules to spec-first generation (and this pattern) is a separate, module-by-module decision.
- No dedicated abstraction yet exists for the collection-level `RepresentationModelProcessor<PagedModel<EntityModel<Dto>>>` postprocessor (unlike `ModelWithDomainPostprocessor` for single items) — `MemberListPostprocessor` is a one-off; a shared base class is worth introducing once a second module needs the same shape.

**References:** PR #299 (`worktree-hal-response-advice`), `backend-patterns` skill.

## ADR-003: HAL affordances resolve against the generated `*Api` interface, not the controller

**Status:** Accepted

**Context:**

After the spec-first migration (ADR-002), every REST controller implements a generated `*Api` interface that carries `@RequestMapping`, `@RequestBody`, `@HasAuthority` and `@OwnerId`. Affordances, however, still recorded the **controller** method: `klabisAfford(methodOn(MemberController.class).updateMember(...))`.

`HalFormsSupport.modifyAffordanceForHalForms` walks the parameters of whatever method `methodOn(...)` recorded, looking for `@RequestBody`. Java does not inherit parameter annotations from an interface, so the lookup only succeeds when the override happens to repeat the annotation. When it does not, the method returns the affordance untouched and `HalFormsInputPayloadMetadata` never runs — the code that supplies inline `options`, `@HalForms(access = …)` and the `readOnly` flag.

The failure is silent. No exception, no failing link assertion; the `_templates` entry is still present, still points at the right URL, still has the right name. Only its `properties` come back wrong. Three modules had shipped in that state:

| Endpoint | Symptom |
|---|---|
| `PUT /api/users/{id}/permissions` | `authorities` — the only field the endpoint exists to change — rendered `readOnly: true` |
| `POST …/account/transactions` (deposit, charge, reverse) | every field including the required `amount` rendered `readOnly: true` |
| `POST /api/calendar-items`, `PATCH …/{id}` | all four fields rendered `readOnly: true` |

**Decision:**

`methodOn(...)` takes the generated interface. Spring HATEOAS supports this directly: `DummyInvocationUtils.getProxyWithInterceptor` branches on `type.isInterface()`, and `WebMvcLinkBuilder.linkTo` resolves the path from `method.getDeclaringClass()`, which for an interface proxy is the interface itself.

Consequently `@RequestBody` is removed from the overrides — the interface becomes the single declaration site. `@Parameter` / `@Operation` / `@ApiResponse` stay on the controller, because springdoc scans the concrete class and `klabis-full.json` would otherwise lose its descriptions. That asymmetry is deliberate and documented in the `backend-patterns` skill, since "remove the annotations the interface already has" is the intuitive but wrong generalization.

Bean Validation forces the removal to be all-or-nothing. Hibernate Validator rejects an override that *redefines* the parameter constraint configuration of the method it overrides (`ConstraintDeclarationException: HV000151`) and compares the parameter list as a whole, so dropping `@RequestBody` while leaving `@NotNull` on a sibling parameter fails **at request time**. `@Valid` is exempt — it is a cascade marker, not a constraint.

**Consequences:**

- A controller override can no longer silently drop HAL-FORMS input metadata; the annotation it would have to forget now lives somewhere it cannot forget it.
- `HalFormsSupport.METHOD_AUTH_CACHE` is keyed on `Method` alone, sound only while callers derive `targetClass` from `method.getDeclaringClass()`. That class is now the interface rather than the controller — still self-derived, so the invariant holds, but the authorization outcome is asserted explicitly in `AffordanceAuthorizationTest` rather than assumed.
- Two guards enforce this going forward: `AffordanceRoutingArchitectureTest` fails the build if any production call site passes a `*Controller.class` to `methodOn(...)` (source-level, because ArchUnit sees the call but not the class literal argument), and `AffordanceAuthorizationTest.InterfaceRoutedInputMetadata` exercises the mechanism end to end against an override that deliberately omits `@RequestBody`.
- Every `Link` in a response body now goes through `klabisLinkTo`. The one call that had bypassed the authorization check — the `event` rel on `EventController`'s accommodation list — was switched over: reaching that list requires `EVENTS:REGISTRATIONS` or being the coordinator, while reading the event needs `EVENTS:READ`, and neither implies the other, so the link was being offered to callers who would get 403 by following it. Plain `linkTo` remains correct for `Location` headers, which must be returned regardless of whether the caller may `GET` the target.
- Test-local controllers in `AffordanceAuthorizationTest`, `FieldLevelAuthorizationTest` and `HalFormsExpectationsTest` deliberately have no interface — they exercise `HalFormsSupport` directly and are excluded from the architecture guard, which only scans `src/main/java`.

**References:** `openspec/changes/affordance-interface-routing/`, `backend-patterns` skill.

## ADR-004: HAL envelope structure lives solely in the bundler

**Status:** Accepted

**Context:**

Under ADR-002 controllers return plain DTOs and Spring HATEOAS builds the HAL envelope at runtime, so the backend never needs an envelope *type*. The envelopes exist in the OpenAPI document for one audience only: the frontend's generated TypeScript types and Swagger UI.

Nevertheless the hand-written specs in `docs/openapi/spec/` spelled every envelope out — an `EntityModel*` wrapper around each payload, a `CollectionModel*`/`PagedModel*` around that, a bare-array `*List` sibling, and a second `application/prs.hal-forms+json` content entry per operation. 56 envelope schemas and 105 hal-forms content entries across 9 module files, all mechanically derivable from the payload plus two facts stated elsewhere: whether the response is a collection (`x-spring-paginated`) and what the `_embedded` key is called (`x-klabis-relation`).

The redundancy was paid twice. Authors wrote the `_embedded` key in two places and kept them in sync by hand — nothing checked agreement, and a mismatch produced frontend types naming a property no response contains, with no error at any stage. And `KlabisSpringCodegen` spent `HalEnvelopeDetector` (233 lines) plus `EnvelopeUnwrap` and four test classes structurally re-deriving the payload back out of those envelopes so the generated Java records would be payloads again. The information made a full round trip and came back unchanged.

**Decision:**

Module specs declare only the `application/json` payload. `tools/openapi-bundle/lib/derive.mjs` derives the hal-forms content entry and the `EntityModel`/`CollectionModel`/`PagedModel` schemas into `docs/openapi/klabis-full.json`, and is the only place in the repo encoding envelope structure.

HAL is the **default**: every 2xx response carrying an `application/json` schema is enveloped. Three facts are not derivable from a payload and are declared:

- `x-klabis-hal: false` on an **operation** — the sole opt-out. Opt-in would mean 105 markers and one silent failure per forgotten marker; opt-out means 6 (`getMySchedule`, the four pre-auth password endpoints, `listOrisEvents`).
- `x-hal-entity-items: true` on an array **property** — its items are independently addressable resources. The deriver emits `EntityModel<Item>` for them and `KlabisSpringCodegen.fromProperty` resolves the property to `List<EntityModel<Item>>`, replacing 7 `schemaMappings` + 7 `extraImportMappings` entries. An explicit per-module `schemaMappings` entry on the item `$ref` still wins.
- `x-hal-embedded: {items, suffix}` on a **response** — a nested collection the controller assembles at runtime via `HalResponseContext.embed`, which cannot be inferred from the payload. The `_embedded` key comes from the item payload's own `x-klabis-relation`, never restated on the marker.

**The codegen reads `docs/openapi/spec/<module>.yaml` directly, not the bundle.** Deriving in the bundler and reading the module YAML in the generator are two halves of the same decision: the alternative — derive for both, so the generator sees the document the frontend sees — means the generator immediately unwrapping what the bundler just wrapped, which is precisely the round trip the 233 lines of detection existed to perform. The Java side is better served by the source, where the envelope never appears.

`HalEnvelopeDetector` and `EnvelopeUnwrap` are deleted along with their tests; `KlabisSpringCodegen` went from ~950 to 419 lines, losing the `getContent` override and `postProcessAllModels`/`envelopeAndFragmentNames` with them.

Two hand-written envelopes remain, both in `common.yaml`: `EntityModelRootModel` and `EntityModelDashboardModel`. `GET /api` and `GET /api/dashboard` are pure navigation whose response is nothing but `_links`, so they have no `application/json` payload for the deriver to build from — and giving them one collapses the generated return type to `ResponseEntity<Void>`. `common`'s `schemaMappings` keeps both from being emitted as Java classes.

**Consequences:**

- Authoring a HAL endpoint drops from "payload schema + 2-3 envelope schemas + 2 content entries" to "payload schema + 1 content entry". Source specs lost 54 envelope schemas, 18 bare-array `*List` aliases and 41 hal-forms content entries — about 820 lines net.
- The `_embedded` key is declared once and feeds both paths — `pojo.mustache` emits `@Relation` from it, the deriver writes the bundle key from it — so the document and the runtime response cannot disagree. That is structural, not a test.
- Complexity moved rather than disappearing: ~250 lines of Java detection became a comparable amount of JS derivation. The win is that HAL knowledge collapsed from three places to one, not a smaller diff.
- The "is this a HAL response" rule now exists on both sides of a language boundary — `derive.mjs`'s `forEachHalResponse` decides which responses get an envelope, and `KlabisSpringCodegen.isHalResponse`/`isHalOptedOut` decide which get the media type in their `produces` clause. They must stay in agreement; a divergence makes an endpoint answer 406 to the `Accept` header the frontend sends.
- `derive.mjs` is a no-op on already-enveloped input — a response with a hal-forms entry, or a schema already envelope-shaped, is left alone. This let the modules migrate one at a time, and is also what protects the two `common` marker schemas. The cost is that a genuine payload declaring its own `_links` would be mistaken for an envelope and skipped silently; `validate.mjs` reports that case explicitly.
- `_templates` is emitted on every derived envelope, uniformly. The property is an `additionalProperties` map with no named members, so it only declares that a template map may appear — true of every HAL-FORMS resource. The contract that says *which* templates is `x-hal-templates`, which `haltypes.mjs` turns into the named union in `halTypes.ts`.

**References:** OpenSpec change `derive-hal-envelopes-in-bundler`, ADR-002 (why the backend needs no envelope types at all), `klabis-api-spec` skill.
