## 1. Codegen module scaffolding

- [x] 1.1 Create `backend/buildSrc/` (if not already present) with a Gradle Java/Kotlin source
      set, add `openapi-generator` as a `compileOnly`/`implementation` dependency at the version
      pinned in `backend/build.gradle.kts` (7.18.0)
- [x] 1.2 Add empty `KlabisSpringCodegen extends SpringCodegen` class (package
      `com.klabis.openapi.codegen` or similar under `buildSrc/src/main/java`), overriding only
      `getName()` to return `"klabis-spring"`
- [x] 1.3 Register it via `buildSrc/src/main/resources/META-INF/services/
      org.openapitools.codegen.CodegenConfig`
- [x] 1.4 Verify `generatorName.set("klabis-spring")` is resolvable from
      `backend/build.gradle.kts` by pointing one *unused* trial `openApiModule(...)`-like
      `GenerateTask` at it and confirming it runs (produces stock-equivalent output, since no
      overrides exist yet) — remove the trial task once confirmed

## 2. `HalEnvelopeDetector` — Shape 1 (single entity)

- [x] 2.1 Write failing unit test: given a `Schema` tree equivalent to
      `EntityModelEventDtoWithRegistrations` (`allOf: [$ref EventDto, {_links, _templates,
      _embedded}]`), `HalEnvelopeDetector.detect(...)` returns an unwrap result targeting
      `EventDto`
- [x] 2.2 Write failing unit test: given `EntityModelPaymentRuleResponse` (`allOf: [$ref
      PaymentRuleResponse, {_links, _templates}]`, no `_embedded`), detector unwraps to
      `PaymentRuleResponse`
- [x] 2.3 Write failing negative unit test: an `allOf` schema whose second member has a property
      *not* in `{_links, _templates, _embedded}` (e.g. a genuine multi-parent composition) is
      NOT detected as Shape 1
- [x] 2.4 Implement Shape 1 detection to make 2.1–2.3 pass
- [x] 2.5 Refactor: extract shared "is this an inline object with properties ⊆ given set" helper
      for reuse by Shape 2

## 3. `HalEnvelopeDetector` — Shape 2 (collection)

- [x] 3.1 Write failing unit test: given `PagedModelEntityModelMemberSummaryResponse`'s shape
      (`type: object`, `_embedded.memberSummaryResponseList: array[$ref
      EntityModelMemberSummaryResponse]`, `_links`, `page: PageMetadata`), detector unwraps to
      `Page<T>` where `T` is the *inner* payload (resolving through the nested
      `EntityModelMemberSummaryResponse` via Shape 1, not left as the envelope type)
- [x] 3.2 Write failing unit test: same shape without a `page`-shaped property → detector
      unwraps to `List<T>` instead of `Page<T>`
- [x] 3.3 Write failing negative unit test: a plain `object` schema with an array property and
      `_links` but where the array item is NOT a `$ref` (inline object) — NOT detected as Shape 2
- [x] 3.4 Write failing negative unit test: a schema with exactly one property that happens to be
      named `_embedded` but is not itself `{single array-of-$ref property}`-shaped — NOT detected
- [x] 3.5 Implement Shape 2 detection (including the nested-unwrap composition with Shape 1 from
      3.1) to make 3.1–3.4 pass

## 4. `handleMethodResponse()` override

- [x] 4.1 Write failing test at the `CodegenOperation` level (using a minimal in-memory OpenAPI
      `Operation`/`ApiResponse` fixture, not a full spec file): an operation whose response
      schema matches Shape 1 produces `op.returnType`/`op.returnBaseType` equal to the unwrapped
      payload type, with no explicit `schemaMappings` entry
- [x] 4.2 Same for Shape 2 on an `x-spring-paginated: true` operation → `op.returnContainer ==
      "Page"`, `op.returnType == "org.springframework.data.domain.Page<X>"`, `op.isArray == false`
- [x] 4.3 Same for Shape 2 without `x-spring-paginated` → `op.isArray == true` and a non-null
      `op.returnContainer` (stock behavior; the value is `"array"` rather than `"list"` because
      the unwrap re-wraps the payload in a synthesized `type: array` schema — both take the same
      `DefaultCodegen` branch, and `returnContainer` reaches no springdoc-rendered output)
- [x] 4.4 Write failing test confirming pagination is independent of the response representation
      (design.md Decision 2): an `x-spring-paginated: true` operation whose response declares
      ONLY `application/json` (no HAL envelope to detect) still produces `Page<X>`, and an
      operation declaring both media types resolves the payload from the first content entry
      while `op.produces` still lists both
- [x] 4.5 Implement `handleMethodResponse()` override (delegating to `super` after schema
      rewrite, per design.md Decision 1) to make 4.1–4.4 pass
- [x] 4.6 Write failing test confirming a response schema matching neither shape (e.g. a bare
      `RegisterMemberRequest`-style request-response, or an explicitly `mappings`-overridden
      schema) is untouched and produces the same `op.returnType` as stock `SpringCodegen` would
- [x] 4.7 Confirm 4.6 passes without additional code (should already hold from the `super`
      delegation fallback in 4.5); if not, fix the fallback path

## 5. Tag-scoped model/API discovery — DROPPED (out of scope)

Cut after investigation: the generator offers no extension point for this. Model filtering lives
entirely in `DefaultGenerator` — `modelKeys()` (private, line 646 of the 7.18.0 sources) reads the
`models` global property and is called from `generateModels()`. `KlabisSpringCodegen` is a
`CodegenConfig`, not a generator, so it cannot reach that decision; the Gradle plugin instantiates
`DefaultGenerator` directly. Design.md's Decision 5 ("`KlabisSpringCodegen` overrides model/schema
discovery") assumed a hook that does not exist.

The stock `generateRecursiveDependentModels` global property was tried as a substitute and
rejected: it walks model *properties*, so it never reaches a request DTO referenced only from an
operation's `requestBody`. Measured on `event-types` with `models` cut to a single root —
`EventTypeDto` was generated, `CreateEventTypeRequest`/`UpdateEventTypeRequest` were not.

Reaching this would mean subclassing `DefaultGenerator` and replacing the Gradle plugin's task,
a far larger maintenance surface than the `models` lists it would remove. The other three
compensating mechanisms (`--strip-hal`, the `doLast` patch, per-envelope `mappings`) still go
away, which is where the bulk of the value is. Per-module `models` lists stay.

Note: task 5.2's goal holds for free — `DefaultGenerator` already skips any schema present in
`schemaMapping()`, at both of its generation sites. No code was ever needed for it.

## 6. Migrate `membershipfees` module (first real module, per design.md's migration plan)

- [x] 6.1 Capture current generated output for `membershipfees` (stock `spring` generator +
      `--strip-hal` + regex patch) as a baseline snapshot for diffing
- [x] 6.2 Switch `membershipfees`'s `openApiModule(...)` call to
      `generatorName = "klabis-spring"`, remove its `models` list, remove envelope-unwrap
      `mappings` entries (`EntityModelPaymentRuleResponse` etc.), keep the hand-written
      nested-class override (`PaymentRuleResponse` →
      `MembershipFeeTierResponse.PaymentRuleResponse`)
- [x] 6.3 Generate and diff against the 6.1 baseline; resolve any non-import-order difference as
      a bug in `KlabisSpringCodegen` (per proposal.md's acceptance bar), not by adjusting the
      module's config
      — surfaced three real generator bugs, all fixed in `KlabisSpringCodegen`: (a) the detector
      was handed the raw `{$ref: ...}` pointer instead of the resolved schema, so it never matched
      anything (`resolveIfRef`); (b) `fromOperation()` builds each `CodegenResponse` from the
      still-enveloped schema *before* `handleMethodResponse()` runs, so `@Schema(implementation =
      ...)` named the never-generated envelope class (`fromResponse` override); (c) the same
      ordering left an import of that envelope class behind, which failed to compile
      (`postProcessOperationsWithModels` override). (b) and (c) are exactly what `--strip-hal` and
      the `doLast` regex patch mask today — removing them in section 8 depends on these fixes.
- [x] 6.4 Run `membershipfees` module's backend tests (via test-runner agent); confirm all pass
      unmodified
- [x] 6.5 Confirm the nested-class mapping (`PaymentRuleResponse`) still resolves correctly —
      the one case Shape 1/Shape 2 must NOT auto-unwrap (it's a top-level schema, not an
      envelope)

## 7. Migrate remaining modules (one vertical slice per module)

- [ ] 7.1 Migrate `events` — includes Shape 1 (`EntityModelEventDtoWithRegistrations`), Shape 2
      paginated (`PagedModelEntityModelEventSummaryDto`/`EventSummaryDtoList`), and hand-written
      overrides (`EventStatus` domain enum, `BulkSyncResult`/`BulkImportResult` application
      types) that must keep working unchanged; baseline-diff and run tests as in 6.1/6.3/6.4
- [ ] 7.2 Migrate `members` — includes Shape 2 paginated (`PagedModelEntityModelMemberSummaryResponse`)
      and confirms this migration also fixes the currently-broken `MemberSummaryResponse`
      duplicate-class regression from the empty `models`/`mappings` (this module's real-world
      motivating bug); baseline-diff and run tests
- [ ] 7.3 Migrate `finance` — includes Shape 2 paginated with a named-array
      `application/json` sibling (`TransactionResourcePage`) alongside the envelope
      (`PagedModelEntityModelTransactionResource`) resolving to the same `Page<T>`; baseline-diff
      and run tests
- [ ] 7.4 Migrate `groupsFamily`, `groupsFree`, `groupsTraining` (three `openApiModule()`
      registrations against one spec file — package routing stays manual per design.md's
      Non-Goals); each includes Shape 1 envelopes with per-item `_links` that have no
      `application/json` sibling; baseline-diff and run tests for all three
- [ ] 7.5 Migrate `common` — includes hand-written overrides (`PermissionsResponse`, `Authority`
      domain enum, `RootModel`/`DashboardModel` marker types) that must keep working unchanged;
      baseline-diff and run tests
- [ ] 7.6 Migrate `oris` — confirms the `_NoGeneratedModelsForOris` placeholder can be deleted
      and the module still generates only `OrisImportApi` with no models; baseline-diff and run
      tests
- [ ] 7.7 Migrate `event-types` and `calendar` (no envelope/pagination complexity — confirms the
      simple case stays simple); baseline-diff and run tests

## 8. Remove obsolete mechanisms

- [ ] 8.1 Remove the `doLast` regex-patch block from `openApiModule(...)` in
      `backend/build.gradle.kts`
- [ ] 8.2 Remove `--strip-hal` CLI flag and `stripHalForCodegen()` from
      `tools/openapi-bundle/lib/stripHal.mjs` (and the file itself if nothing else uses it),
      remove its test file `tools/openapi-bundle/test/stripHal.test.mjs`
- [ ] 8.3 Remove the `bundleSpecForCodegen` Gradle task; point each `openApiModule(...)`'s
      `inputSpec` directly at `docs/openapi/klabis-full.json` (produced by the existing
      `openapiBundle` task, unchanged)
- [ ] 8.4 Remove now-empty `models` parameter from `openApiModule(...)`'s function signature in
      `build.gradle.kts` (or confirm it is fully unused and delete it) and update its KDoc comment
- [ ] 8.5 Run the full backend test suite (unit + Modulith integration) once more after all
      removals, confirming zero regressions

## 9. Documentation

- [ ] 9.1 Update the `klabis-api-spec` skill to describe the new zero-mapping default for HAL
      envelopes and clarify when a `mappings` entry is still required (hand-written overrides
      only)
- [ ] 9.2 Add/update a header comment in `backend/buildSrc/` pointing to
      `openspec/changes/custom-openapi-codegen/design.md` for the shape-detection rationale, per
      the same "vendor fork, diff on upgrade" convention already used in `api.mustache`
- [ ] 9.3 Update `docs/openapi/spec/README.md` (or wherever the spec-first workflow is
      documented) to remove references to `--strip-hal`/`bundleSpecForCodegen` and describe the
      single-bundle flow
