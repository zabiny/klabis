## 0. Baseline

- [x] 0.1 Capture the current generated output for all 11 modules as the parity reference
      (`./gradlew compileJava`, then copy `backend/build/generated/openapi/`). Every task below
      is verified against it: the only acceptable diff is the `@Generated` annotation. Anything
      else is a regression, per proposal.md's acceptance bar.
      Baseline stored at `backend/build/generated-openapi-baseline/` (93 files, all 11 modules).

## 1. `isArraySchema` in the detector (smallest slice first)

- [x] 1.1 Write a failing unit test: an `_embedded` block whose array property is spelled
      `type: ["array"]` (the OpenAPI 3.1 list form) is detected as Shape 2. It fails today
      because `asSingleArrayOfRefProperty` compares `getType()` against the string `"array"`.
- [x] 1.2 Replace that comparison with `ModelUtils.isArraySchema(...)`. Note it is a superset —
      it also matches `ArraySchema` instances and `types`-only schemas — so it cannot stop
      matching a shape that matches today.
- [x] 1.3 Run the buildSrc unit tests; regenerate and diff against 0.1. Expect no change: the
      current bundle emits scalar `type` strings, so this is robustness, not a live fix.
      24/24 buildSrc tests pass; regenerated output diffs clean vs baseline (only `@Generated`
      timestamp line differs across all 93 files).

## 2. `getContent()` override replaces the name-based import cleanup

- [ ] 2.1 Write a failing test at the `getContent` level: a response whose content map holds
      both `application/json` (bare payload) and `application/prs.hal-forms+json` (envelope)
      contributes NO import for the envelope type to the `imports` set it is handed.
- [ ] 2.2 Override `getContent(Content, Set<String>, String)` (protected, `DefaultCodegen`
      sources line 7864) to map each media type's schema through the existing
      `resolveRef` + `HalEnvelopeDetector.detect` + `unwrappedResponseSchema` pipeline before
      delegating to `super`, so the envelope type is never imported in the first place.
- [ ] 2.3 Delete `postProcessOperationsWithModels` and its `KlabisSpringCodegenPostProcessOperationsTest`
      counterpart — or repoint that test at the new hook if its aggregation-flow coverage is
      still worth keeping (it is what caught the promoted-`$ref` bug originally).
- [ ] 2.4 Regenerate and diff against 0.1 for all 11 modules. Pay attention to `groupsFamily`,
      `groupsFree`, `groupsTraining` (single-item envelopes with no `application/json` sibling)
      and `event-types` (the promoted `_embedded` `$ref` case) — those exercise the paths where
      an envelope import could survive.
- [ ] 2.5 Confirm `CodegenResponse.content` is not rendered anywhere: verified during planning
      that no template under `backend/src/main/openapi-templates/` reads `{{#content}}` — re-check
      after the change, since `getContent`'s return value populates that field.
- [ ] 2.6 Run the full backend test suite.

## 3. Shared test fixture builder

- [ ] 3.1 Add a package-private builder under `buildSrc/src/test/java/com/klabis/openapi/codegen/`
      covering the three shapes rebuilt by hand across the suite: Shape 1
      (`allOf[$ref X, {_links, ...}]`), Shape 2 (`{_embedded: {<name>: array[$ref]}, _links}`,
      with an optional `page` property), and the `OpenAPI` + `Components` assembly that
      `KlabisSpringCodegenHandleMethodResponseTest` already extracted as `openApiWithSchemas`.
- [ ] 3.2 Migrate the positive-case fixtures in all four test files onto the builder.
- [ ] 3.3 Leave the negative-case fixtures hand-built and add a comment saying why: they exist to
      deviate from the canonical shape, and routing them through a builder would hide which
      property makes them invalid.
- [ ] 3.4 Run the buildSrc unit tests — same count, same outcomes. No production code changes in
      this section, so no regeneration is needed.

## 4. Close out

- [ ] 4.1 Full backend test suite plus a from-scratch regeneration (`rm -rf build/generated/openapi`)
      diffed against 0.1 across all 11 modules.
- [ ] 4.2 Update the `KlabisSpringCodegen` class Javadoc: it lists four overrides, and the set
      changes here (`postProcessOperationsWithModels` out, `getContent` in). The note that
      model/API discovery is not this class's concern stays.
- [ ] 4.3 Remove the "Known follow-ups" section from the archived
      `openspec/changes/archive/2026-08-25-custom-openapi-codegen/design.md`, or annotate it with
      a pointer to this change — whichever the project prefers for archived documents.
