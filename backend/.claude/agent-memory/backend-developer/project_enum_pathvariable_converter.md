---
name: enum-pathvariable-converter
description: Generated OpenAPI enum @PathVariable needs an explicit Converter<String,Enum> bound by wire value, else Spring falls back to Enum.valueOf (case-sensitive, name != wire value)
metadata:
  type: project
---

Generated OpenAPI param enums (e.g. `SyncEntityTypeParam.EVENTS` with wire value `"events"`) have `@JsonCreator fromValue(String)` for JSON body deserialization only — it does NOT apply to `@PathVariable` binding. Without a registered `Converter<String, TheEnum>`, Spring MVC binds path variables via `Enum.valueOf(name)`, which is case-sensitive and fails whenever the wire value differs from the constant name (`"events"` vs `EVENTS`). The failure surfaces as `MethodArgumentTypeMismatchException` on every request, including valid ones — easy to misdiagnose as a routing or exception-handler bug.

**Fix pattern**: a stateless `Converter<String, GeneratedEnum>` class, `convert()` delegates to `GeneratedEnum.fromValue(source)` (never `valueOf`/`name()`). Annotate `@MvcComponent` (not `@Component`) so it's both auto-registered into the global `mvcConversionService` (Spring Boot auto-detects `Converter` beans with no constructor deps — same mechanism `MemberIdToUuidConverter` and `RegisterNewMemberConverter` rely on) and included in `@WebMvcTest` slice scans via `MvcConfiguration`'s `@ComponentScan` filter. No dependencies in the constructor — a `Converter` bean with sync-module-specific dependencies (see `SyncStateResponseConverter`'s javadoc) previously broke `@WebMvcTest` across the entire project (807 failures) by being pulled into every slice's `mvcConversionService`.

Example: `com.klabis.sync.infrastructure.restapi.StringToSyncEntityTypeParamConverter`, converting `SyncEntityTypeParam` path segments for `SynchronizationController`.

**Why:** discovered while fixing 18 failing `SynchronizationControllerTest` tests (all returning 404 instead of 200/403/409) during `add-bidirectional-sync-engine` Slice 6 cleanup, 2026-09-03.

**How to apply:** whenever adding a new generated OpenAPI enum used as a `@PathVariable`, check whether its wire value matches its constant name; if not (or to be safe generally), add a matching `@MvcComponent` converter alongside the controller rather than relying on Spring's default enum binding.
