## Context

The spec pipeline has four stages that touch property-level metadata: `validate.mjs` (extension allowlists, `x-klabis-*` prefix only), `KlabisSpringCodegen` (buildSrc, already carries custom logic: `preprocessOpenAPI`, `fromProperty`, `fromResponse`), `pojo.mustache` (renders annotations from `vendorExtensions.x-*` keys, incl. the annotation chain at line ~66), and `derive.mjs` (consumes `x-hal-*` directives into `klabis-full.json` — the single source of frontend types).

Two things a spec author cannot declare today:

1. The HAL-FORMS property `type` for a composite field. Backend emits the Java class simple name (`HalFormsSupport.getTypeFromClass`); the only override is `x-field-extra-annotation` with a hand-written `@HalForms(formInputType = ...)`, which dies inside composition (`oneOf`/`allOf` strip sibling vendor extensions) and, combined with `x-klabis-halforms-access` (which emits `@HalForms(access = ...)` directly in mustache), would emit two `@HalForms` annotations on one record component — not `@Repeatable`, so a compile error.
2. `JsonNullable` without composition. Nullable `$ref` requires `oneOf: [$ref, null]` (strips siblings — patch properties can't carry authorization), and `type: [x, 'null']` always wraps, so nullable response fields get a `JsonNullable` leak.

Motivating case: `UpdateEventRequest.ranking` — today the event-detail update form renders a "neznamy typ" warning instead of the ranking fields (regression from the spec-first cutover renaming the ranking schema).

## Goals / Non-Goals

**Goals:**
- `x-hal-input-type` (free-form string) → a single assembled `@HalForms(formInputType = "...")`, coexisting with `x-klabis-halforms-access`.
- `x-klabis-nullable: true|false` → explicit control of the Java `JsonNullable` wrapper independent of spec composition.
- Bundler consumes both directives so `klabis-full.json` (and therefore frontend types) keep their current shapes for refactored properties.
- Restore working ranking edit fields via the new syntax.
- Mass-refactor existing `oneOf: [$ref, null]` PATCH properties to the new syntax, verified zero-diff.

**Non-Goals:**
- No runtime Java changes (`HalFormsSupport` already reads `formInputType`); no frontend code changes.
- Renaming `x-klabis-halforms-access` (explicitly kept).
- Refactoring nullable `$ref` properties on *responses* (no `JsonNullable` there today; wire format must not change). `x-klabis-nullable: false` is a capability for future use, not part of the mass refactor.
- Supporting `x-klabis-nullable`/`x-hal-input-type` inside `oneOf`/`allOf` compositions — composition stays the forbidden combination; the extensions *replace* it.
- Generalizing the annotation-assembly mechanism beyond the single `@HalForms` annotation.

## Decisions

**D1 — Annotation assembly lives in `KlabisSpringCodegen`, not mustache.** `postProcessModelProperty` combines `x-klbis-halforms-access` + `x-hal-input-type` into one complete annotation string stored in a vendor extension (e.g. `vendorExtensions.x-klabis-halforms-annotation`), and `pojo.mustache` renders it verbatim. Alternative (branching `{{#vendorExtensions…}}` logic in mustache to cover the 2×2 access × input-type matrix) rejected: mustache lacks value-aware conditionals, every future `@HalForms` attribute adds combinatorial branches, and errors surface only as bad output. Assembly in Java can also log/validate conflicting input. Existing `x-klabis-halforms-access` rendering in mustache is removed in favor of the assembled form.

**D2 — `x-klabis-nullable` sets `CodegenProperty.isNullable` in `postProcessModelProperty`.** No spec-model rewrite: the vendor extension is already on the property when the codegen hook runs; setting `isNullable` lets the stock `openApiNullable` machinery wrap `datatypeWithEnum` as usual (`JsonNullable<T>`), including `@Valid` placement. Alternative (`preprocessOpenAPI` rewriting the schema to `$ref` + `nullable: true`) rejected: it depends on stock parser handling of `$ref` siblings, which is exactly the territory where composition semantics already proved lossy. `x-klabis-nullable: false` sets `isNullable = false` (plain `T` despite nullable wire type). Enum-typed and nested-class-typed nullable properties must be exercised in the verification (wrapper shape `JsonNullable<UpdateEventRequestRanking>` etc.).

**D3 — `derive.mjs` consumes both directives; the bundle stays the single truth for frontend types.**
- `x-klabis-nullable: true` on a `$ref` property → bundle emits exactly today's shape, `oneOf: [$ref, 'null']` (frontend types byte-identical).
- `x-klabis-nullable: false` → drops `'null'` from the bundled type array (frontend type narrows — intentional for future response fields).
- `x-hal-input-type` → deleted from the bundle like `x-hal-entity-items` (it is a codegen directive, meaningless to frontend).
The extensions must never appear in `klabis-full.json` — bundling is their consumption point.

**D4 — Naming follows the two existing families.** `x-hal-input-type` sits with the `x-hal-*` directives the deriver consumes; `x-klabis-nullable` sits with the `x-klabis-*` annotation-emitting extensions, so `validate.mjs`'s existing `x-klabis-*` prefix check covers it (`x-hal-*` needs an explicit allowlist entry like its siblings). `x-klabis-halforms-access` is kept unchanged.

**D5 — `validate.mjs` turns every misuse into a build failure (nothing silent):** unknown keys rejected as today; `x-klabis-nullable` or `x-hal-input-type` together with `oneOf`/`allOf` on the same property → error (composition would strip them or double-declare nullability); `x-klabis-nullable: true` where the property is already nullable via `type: [x, 'null']` → error (redundant dual declaration); `@HalForms(...)` inside `x-field-extra-annotation` → error after migration (would duplicate the assembled annotation); `x-hal-input-type` + `x-klabis-halforms-access` → legal, that combination is the point of D1.

**D6 — Zero-diff verification for the mass refactor.** Before refactoring: snapshot generated Java (`build/generated/openapi/**`) and `klabis-full.json` for all modules. After: `diff` must show no change beyond the ranking case (which changes `UpdateEventRequest.java` intentionally: `JsonNullable` kept, annotation added). This is the safety net for the "byte-identical" claim in the proposal.

**D7 — Ranking case is the first application and the acceptance test.** `UpdateEventRequest.ranking` → `$ref` + `x-klabis-nullable: true` + `x-hal-input-type: RankingRequest`; standalone `UpdateEventRankingRequest` schema stays (mapper imports unchanged). Verified by a MockMvc HAL-FORMS test asserting the template property carries `type: "RankingRequest"` (and existing tests that today would see the warning-causing simple name).

## Risks / Trade-offs

- [Stock generator surprises around `isNullable` edge cases — enums, nested classes, `@Valid` rendering] → D6-style snapshot diff per module right after the extension lands, before any spec refactor; exercise one enum and one nested-class nullable property explicitly.
- [`x-klabis-nullable: false` narrows frontend types while wire stays nullable] → documented capability; validate warns when used on a request body (tri-state intent mismatch); today unused.
- [Mass refactor touches every module spec at once] → it is the *last* phase, gated by working extensions + zero-diff snapshots; rollback is git revert of a single phase.
- [Removing `x-klabis-halforms-access` from mustache changes existing output] → the assembled annotation must be byte-equal to what mustache emits today for access-only properties (`@HalForms(access = com.klabis.common.ui.HalForms.Access.X)`); covered by the same snapshot diff (D1/D6).
- [Two families of extensions confuse spec authors] → `klabis-api-spec` skill and `docs/openapi/spec/README.md` get one table: which family, which stage consumes it, what it emits.

## Migration Plan

1. Codegen + mustache: assembled `@HalForms` (D1) — snapshot-diff proves access-only properties unchanged.
2. Codegen: `x-klabis-nullable` (D2) — snapshot-diff proves no-op while no spec uses it.
3. Bundler: consume/strip directives (D3) — bundle unchanged until specs adopt the syntax.
4. Validate rules (D5).
5. Apply to `UpdateEventRequest.ranking` (D7) — intentional diff, MockMvc assertion on the template type.
6. Mass refactor of `oneOf: [$ref, null]` PATCH properties + zero-diff diff (D6); migrate `CancelEventRequest.cancellationReason` textarea to `x-hal-input-type`.
7. Docs/skills update (`klabis-api-spec` references + anti-patterns, `docs/openapi/spec/README.md`).

Each phase independently committable; rollback = revert that phase. Nothing observable changes until phase 5.

## Open Questions

None blocking. (Settled during exploration: keep `x-klabis-halforms-access` name; `x-hal-input-type` takes a free-form string; mass refactor is in scope as the final phase; `x-klabis-nullable` chosen over `x-nullable` for the validate prefix and to avoid the OAS 3.0 `nullable` keyword shadow.)
