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
