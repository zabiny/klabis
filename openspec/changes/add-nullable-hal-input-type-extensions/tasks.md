## 1. Preparation

- [ ] 1.1 Snapshot generated output for zero-diff verification: `./gradlew openapiBundle openApiGenerateEvents openApiGenerateMembers openApiGenerateCommon openApiGenerateFinance openApiGenerateGroups openApiGenerateMembershipfees openApiGenerateCalendar openApiGenerateOris`, copy `build/generated/openapi/**` and `docs/openapi/klabis-full.json` to a scratch location (`/mnt/ramdisk/klabis`)
- [ ] 1.2 Record today's HAL-FORMS template for `GET /api/events/{id}` update affordance (MockMvc test output or manual curl) as the before-picture for the ranking case

## 2. Assembled @HalForms annotation (x-hal-input-type)

- [ ] 2.1 In `KlabisSpringCodegen.postProcessModelProperty`: assemble a single `@HalForms(...)` string from `x-klabis-halforms-access` (access) + `x-hal-input-type` (formInputType) into `vendorExtensions.x-klabis-halforms-annotation`; both absent → no extension emitted
- [ ] 2.2 Update `pojo.mustache`: replace the direct `x-klabis-halforms-access` rendering with the assembled `x-klabis-halforms-annotation`; output must stay byte-equal for access-only properties (`@HalForms(access = com.klabis.common.ui.HalForms.Access.X)`)
- [ ] 2.3 Regenerate all modules and diff against the 1.1 snapshot — expect zero changes
- [ ] 2.4 Add a codegen-level test: property with only `x-hal-input-type`, only `x-klabis-halforms-access`, and both — one `@HalForms` each time, no duplicate annotation

## 3. x-klabis-nullable in codegen

- [ ] 3.1 In `KlabisSpringCodegen.postProcessModelProperty`: read `x-klabis-nullable` (boolean) and set `CodegenProperty.isNullable` accordingly; absent → stock behavior
- [ ] 3.2 Regenerate all modules and diff against the snapshot — expect zero changes (no spec uses it yet)
- [ ] 3.3 Codegen-level tests: `x-klabis-nullable: true` on a `$ref` property yields `JsonNullable<T>` (with correct `@Valid`), including an enum-typed and a nested-class-typed property; `false` on `type: [x, 'null']` yields plain `T`

## 4. Bundler consumption (derive.mjs) + validation (validate.mjs)

- [ ] 4.1 `derive.mjs`: `x-klabis-nullable: true` on a `$ref` property → bundle emits `oneOf: [$ref, 'null']`; `false` → drop `'null'` from the type array; delete the directive from the bundle output
- [ ] 4.2 `derive.mjs`: strip `x-hal-input-type` from bundle output (same as `x-hal-entity-items`)
- [ ] 4.3 `validate.mjs`: allowlist `x-hal-input-type` (x-hal family) and cover `x-klabis-nullable` in the `x-klabis-*` rules; error on either key combined with `oneOf`/`allOf` on the same property; error on `x-klabis-nullable: true` where `type` already contains `'null'`; error on `@HalForms(...)` inside `x-field-extra-annotation`
- [ ] 4.4 Unit-test the bundler transformations and the new validation errors (existing validate tests keep passing)

## 5. Apply to the ranking case (acceptance)

- [ ] 5.1 `events.yaml`: `UpdateEventRequest.ranking` → `$ref: UpdateEventRankingRequest` + `x-klabis-nullable: true` + `x-hal-input-type: RankingRequest` (standalone schema stays; inlined-type experiment discarded)
- [ ] 5.2 Regenerate; diff shows only `UpdateEventRequest.java` gaining the annotation with `JsonNullable<UpdateEventRankingRequest>` intact; bundle/FE types unchanged
- [ ] 5.3 MockMvc HAL-FORMS test: update template's `ranking` property carries `type: "RankingRequest"` (replaces the before-picture from 1.2); run existing event controller tests

## 6. Mass refactor to the new syntax

- [ ] 6.1 Script-driven rewrite across module specs: every request-body property spelled `oneOf: [$ref, 'null']` → `$ref` + `x-klabis-nullable: true` (list occurrences first: `grep -rn "oneOf:" docs/openapi/spec/`)
- [ ] 6.2 Migrate `CancelEventRequest.cancellationReason` from `x-field-extra-annotation` `@HalForms(formInputType = "textarea")` to `x-hal-input-type: textarea`
- [ ] 6.3 Regenerate all modules; diff against the 1.1 snapshot must show zero changes
- [ ] 6.4 Run the full backend test suite (test-runner agent, sequential) — no test modification beyond the 5.3 assertion change

## 7. Documentation

- [ ] 7.1 Update `klabis-api-spec` skill: extension reference table (family, emitting stage, consumption point), patch-bodies and validation references, anti-pattern entries (`@HalForms` via `x-field-extra-annotation`; nullable `$ref` via `oneOf`)
- [ ] 7.2 Update `docs/openapi/spec/README.md` pipeline description; verify `openspec validate --strict` passes for this change
