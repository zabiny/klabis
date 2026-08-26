## Context

`MonetaryAmountConverter` already established the target pattern: a MapStruct-generated `Converter<S,T>` bean, configured via the shared `MapstructSpringMapperConfig`, consumed by callers exclusively through Spring's `ConversionService`. This removed a `@WebMvcTest` slice-context dependency on a concrete mapper type.

Three remaining mappers still expose conversions the old way — callers inject the mapper interface directly and invoke named methods:

| Mapper | Externally-called methods | Callers |
|---|---|---|
| `AuthorityMapper` | `toDto(Authority)`, `toDomain(AuthorityDto)`, `toDtoSet`, `toDomainSet` | `PermissionController` |
| `BulkResultMapper` | `toDto(BulkSyncResult)`, `toDto(BulkImportResult)` | `OrisEventController` |
| `MemberMapper` | `toSummaryResponse`, `toDetailsResponse`, `deactivationReasonToDomain`, `toRegisterNewMemberCommand` | `MemberController`, `RegistrationController` |

No mapper today declares `uses = <OtherMapper>` — cross-mapper composition (case "b" from the original discussion, i.e. a conversion consumed by another mapper) does not currently exist in the codebase. The scope is therefore limited to case "a": conversions consumed from outside the mapper (controllers).

## Goals / Non-Goals

**Goals:**
- Every mapper method invoked from outside its own mapper interface is reachable as a `Converter<S,T>` Spring bean.
- Every controller/handler consumes conversions exclusively via `ConversionService` — no controller injects a concrete `XyzMapper` type.
- Collection conversions (`Set<AuthorityDto> -> Set<Authority>`) are handled by `ConversionService`'s built-in element-reuse (`TypeDescriptor.collection(...)`), not by hand-written collection converters.
- Multi-argument external calls (`toRegisterNewMemberCommand(request, registeredBy)`) are normalized to single-argument via a wrapper record, so they fit `Converter<S,T>`.

**Non-Goals:**
- Rewriting internal-only helper methods (`guardianToResponse`, `createPersonalInformation`) as converters — they are never called from outside `MemberMapper`.
- Introducing cross-mapper composition via `uses = ConversionServiceAdapter.class` — not needed since no mapper currently depends on another mapper's output type.
- Changing any DTO shape, field name, or HTTP-observable behavior.

## Decisions

**1. One `Converter<S,T>` interface per externally-called method, not one per mapper.**
Mirrors `MonetaryAmountConverter`. A mapper interface can still declare multiple methods (MapStruct requires the interface to compile), but only the methods actually called from outside get promoted to their own `Converter` interface + generated `...Impl` bean. Internal helper methods stay `default` methods inside the original (now internal-only) mapper interface, OR get folded directly into the `Converter` interface's abstract method if the mapper interface would otherwise be empty (e.g. `AuthorityMapper` has no purely-internal methods left, so it can be deleted once both directions are converters).

**2. Collection conversions rely on `ConversionService` element-reuse — no `Converter<Set<X>, Set<Y>>`.**
Confirmed against Spring Framework reference docs: `GenericConversionService` reuses a registered `Converter<X,Y>` for `Collection<X> -> Collection<Y>` automatically, provided the caller uses the `convert(Object, TypeDescriptor, TypeDescriptor)` overload (or `TypeDescriptor.collection(...)`) so the element type survives generic erasure. `AuthorityMapper.toDtoSet`/`toDomainSet` are dropped; `PermissionController` calls `conversionService.convert(source, TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(AuthorityDto.class)), TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Authority.class)))`.

**3. Multi-argument external methods get a wrapper record, not a `GenericConverter`.**
`toRegisterNewMemberCommand(RegisterMemberRequest request, UserId registeredBy)` becomes `Converter<RegisterMemberRequestWithParameters, RegistrationPort.RegisterNewMember>`, where `RegisterMemberRequestWithParameters` is a new record `(RegisterMemberRequest request, UserId registeredBy)`. This keeps every converter a plain `Converter<S,T>` (single abstract method, MapStruct-friendly) instead of introducing `GenericConverter`/multi-source patterns for a single call site.

**4. Distinct converter class names when a mapper has overloaded external methods.**
`BulkResultMapper.toDto(BulkSyncResult)` and `toDto(BulkImportResult)` both external, both named `toDto`, but different source types. Since each becomes its own `Converter<S,T>` interface, the interface names disambiguate them (e.g. `BulkSyncResultConverter`, `BulkImportResultConverter`) — no naming collision, `ConversionService.convert(source, TargetType.class)` dispatches by source type alone.

**5. Investigate `mapstruct-spring-extensions` automatic reverse-converter generation before hand-writing both directions.**
For fully MapStruct-generated (non-`default`) bidirectional pairs like `AuthorityMapper.toDto`/`toDomain`, `mapstruct-spring-extensions` may be able to derive the reverse `Converter` automatically from the forward mapping's annotations, avoiding a hand-written second interface. To be confirmed during implementation (task-level spike) — if unsupported or unreliable, fall back to writing both directions explicitly, consistent with `MonetaryAmountConverter`.

## Risks / Trade-offs

- **[Risk]** Splitting one mapper into N single-method `Converter` interfaces increases file count and could feel like over-fragmentation for simple mappers (e.g. `AuthorityMapper` today is 4 lines of interface, tomorrow 2 files).
  → **Mitigation**: Accept the trade-off — the goal is uniformity for callers, not minimizing file count. `MonetaryAmountConverter` already set this precedent.

- **[Risk]** `ConversionService.convert(Object, Class)` loses generic type info for collections; a careless caller might use the wrong overload and get a runtime `ConverterNotFoundException`.
  → **Mitigation**: Always use `TypeDescriptor.collection(...)` explicitly at every collection call site; cover with a `@WebMvcTest` for `PermissionController` asserting the conversion actually happens (not just that the endpoint returns 200).

- **[Risk]** `@WebMvcTest` slice tests for the 4 affected controllers currently `@Import`/mock the concrete mapper type (e.g. `@Import(MemberMapperImpl.class)` in `BirthNumberAuditControllerTest`) — these need auditing so they don't silently keep importing dead mapper beans after the refactor.
  → **Mitigation**: Grep for `MapperImpl` and `@MockitoBean(... Mapper.class)` across `src/test` as an explicit task; update or remove obsolete test-side wiring.

- **[Risk]** Deleting `AuthorityMapper`/`BulkResultMapper`/`MemberMapper` as standalone interfaces (once fully split into converters) could break `uses = ...` references if any exist elsewhere.
  → **Mitigation**: Already verified — no mapper in the codebase declares `uses = <these mappers>`. Safe to delete once all external methods are migrated, though keeping the original interface as a private/internal-only container (for the `default` helpers) is equally acceptable and lower-risk; final call left to implementation.

- **[Risk, found during MemberMapper slice]** `Converter` beans are visible to every `@WebMvcTest` slice regardless of the test's `controllers` filter (`WebMvcTypeExcludeFilter` always lets them through), so a `Converter` with `uses = SomeInternalMapper.class` drags that mapper's bean requirement into every unrelated slice test app-wide — not just tests for its own controller. This broke `BulkSyncResultConverter`/`BulkImportResultConverter` (declared `uses = BulkResultMapper.class`) for every members-module `@WebMvcTest`, since those tests never imported `BulkResultMapperImpl`.
  → **Fix**: Never use `uses = <OtherMapper>` between a `Converter` and a plain internal `@Mapper`. Each `Converter` interface declares all the nested/helper mapping methods it needs directly on itself (MapStruct resolves same-interface methods without `uses=`). Trivial one-liner helpers (nested enum/value mappings) are cheap to duplicate across two `Converter`s if both need them — cheaper than a shared bean dependency.

- **[Risk, found during MemberMapper slice]** A second, independent failure mode from the same root cause: `@SpringMapperConfig`'s generated `KlabisConversionServiceAdapter` (in `com.klabis.common.mapping`) aggregates every mapper's converted source/target types as static fields/methods in one class, regardless of whether any mapper actually uses cross-mapper `uses=` composition. The moment a `Converter`'s source or target type is a Modulith-module-private `.domain` type (e.g. `members.domain.Member`, `members.domain.DeactivationReason` — as introduced by `MemberSummaryConverter`/`MemberDetailsConverter`/`DeactivationReasonConverter`), the generated adapter class makes `common` illegally depend on that module's internals, failing `ModuleStructureVerificationTest`. This adapter turned out to have zero consumers anywhere in `src/main` — it existed only to support cross-mapper composition (case "b"), which this migration deliberately never uses.
  → **Fix**: Removed the `@SpringMapperConfig(...)` annotation from `MapstructSpringMapperConfig` entirely — it no longer generates `KlabisConversionServiceAdapter`. `MapstructSpringMapperConfig` now only carries `@MapperConfig(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)`. Every `Converter` across all 3 slices already works without the adapter, since none of them relied on cross-mapper composition. This also removes the Decision 5 rationale for having the adapter at all — the "auto-derive reverse converter" spike concluded it doesn't do that anyway.

## Migration Plan

Vertical-slice per mapper (each independently committable/testable, per project convention):
1. `AuthorityMapper` + `PermissionController` (smallest, 1 controller, no multi-arg methods)
2. `BulkResultMapper` + `OrisEventController` (2 converters, no multi-arg methods)
3. `MemberMapper` + `MemberController` + `RegistrationController` (largest — 4 converters, introduces `RegisterMemberRequestWithParameters`)

For each slice: run affected `@WebMvcTest`s (and any relevant integration tests) before moving to the next slice. No feature flag or rollback mechanism needed — this is a same-behavior internal refactor; a bad slice is simply reverted via git if tests fail.

## Open Questions

- Does `mapstruct-spring-extensions` actually support auto-deriving the reverse `Converter` for a fully-generated mapping pair? (Decision 5 — resolve during `AuthorityMapper` slice, first in the migration order specifically to answer this early.)
  **Resolved (no):** inspected `mapstruct-spring-extensions:2.0.0` on the classpath — the library only ships `ConverterRegistrationConfigurationGenerator`, which is what `MapstructSpringMapperConfig`'s `@SpringMapperConfig` already uses to generate the `KlabisConversionServiceAdapter` bean (an aggregator that lets other MapStruct mappers reuse converters via the adapter instead of `uses = ...`). There is no annotation or processor that derives a reverse `Converter<T,S>` from a forward `Converter<S,T>` declaration. Both directions of `AuthorityMapper.toDto`/`toDomain` were hand-written as two separate `Converter` interfaces (`AuthorityToDtoConverter`, `AuthorityToDomainConverter`), consistent with `MonetaryAmountConverter`.
- Should the now-converter-only mapper interfaces (`AuthorityMapper`, `BulkResultMapper`) be deleted entirely, or kept as a documentation-only grouping? Leaning delete for `AuthorityMapper` (no leftover internal methods); `MemberMapper` must stay (holds `guardianToResponse`/`createPersonalInformation` helpers).
