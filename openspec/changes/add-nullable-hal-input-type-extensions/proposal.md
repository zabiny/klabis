## Why

Two property-level declarative capabilities are missing from the spec language, and the workarounds for both collide with OpenAPI composition:

- **HAL-FORMS input type.** For composite fields the backend emits the Java class simple name as the HAL-FORMS property `type`, and the frontend field factory switches on that name. The only way to override it today is `x-field-extra-annotation` carrying a hand-written `@HalForms(formInputType = ...)`, which (a) is stripped by `oneOf`/`allOf` composition, and (b) risks emitting a second `@HalForms` annotation next to `x-klabis-halforms-access` — `@HalForms` is not `@Repeatable`, so that combination is a compile error.
- **JsonNullable (PATCH tri-state).** A nullable `$ref` property can only be spelled `oneOf: [$ref, null]`, and that composition strips *sibling* vendor extensions — so a patch property cannot carry `x-klabis-authority`, owner visibility, or any annotation. Separately, `type: [x, 'null']` always wraps in `JsonNullable`, so a nullable response field cannot be declared without the wrapper leaking into Java code that only wants a value.

Motivating case: `UpdateEventRequest.ranking` needs `JsonNullable` **and** `@HalForms(formInputType = "RankingRequest")` at once — impossible today. The frontend renders a "neznamy typ HAL+FORMS property" warning instead of the ranking fields, which violates the existing requirement `events/spec.md` § Manual Editing Of Ranking And Base Entry Fee (regression from the spec-first cutover renaming the schema `RankingRequest` → `UpdateEventRankingRequest`).

## What Changes

- New property-level extension **`x-hal-input-type`** (free-form string): emits the HAL-FORMS input type via `@HalForms(formInputType = ...)`. The annotation is assembled as a **single** `@HalForms` in `KlabisSpringCodegen` from `x-hal-input-type` + existing `x-klabis-halforms-access` (kept as-is), then rendered verbatim by `pojo.mustache`.
- New property-level extension **`x-klabis-nullable: true|false`**: `true` produces `JsonNullable<T>` without any `oneOf`/type-array composition; `false` produces a plain Java type despite a nullable wire type (removes the JsonNullable leak on nullable response fields). Implemented in `KlabisSpringCodegen` (`isNullable` from vendor extension, no spec-model rewrite).
- `tools/openapi-bundle` consumes both directives: `x-klabis-nullable: true` on a `$ref` property derives the existing `oneOf: [$ref, 'null']` bundle shape (frontend types unchanged); `x-klabis-nullable: false` drops `'null'` from the bundled type array; `x-hal-input-type` is stripped from the bundle like `x-hal-entity-items`.
- `validate.mjs`: allowlist the new keys; forbid `x-klabis-nullable`/`x-hal-input-type` on a property that also uses `oneOf`/`allOf`; warn on redundant usage (both composition and `x-nullable: true`); reject `@HalForms(...)` inside `x-field-extra-annotation` after migration.
- Apply the new syntax to the motivating case: `UpdateEventRequest.ranking` gets `$ref` + `x-klabis-nullable: true` + `x-hal-input-type: RankingRequest` — restores working ranking edit fields (fixes the regression above).
- Final phase: mass refactor of all existing `oneOf: [$ref, null]` (and `oneOf: [$ref, 'null']`) patch properties across module specs to `$ref` + `x-klabis-nullable: true`, verified zero-diff against the previously generated code and bundle.
- Documentation: `docs/openapi/spec/README.md`, `klabis-api-spec` skill references (field-security / validation / patch-bodies as relevant), and the anti-pattern list.

## No Behavior Change Justification

The refactor of existing properties is byte-identical on all three generated surfaces: Java records (same `JsonNullable<T>` wrapper, same annotations), `klabis-full.json` (same `oneOf: [$ref, 'null']` shape the bundle already carries), and frontend types (same `T | null`). The only user-visible change is the **restoration** of ranking edit fields on the event detail form, which reinstates behavior already required by the existing spec — no requirement is added, modified, or removed.

**Specs reviewed:**
- `openspec/specs/events/spec.md` — § Manual Editing Of Ranking And Base Entry Fee already requires manual ranking editing; the `x-hal-input-type` application on `UpdateEventRequest.ranking` restores compliance with it (today it fails: warning instead of fields). No other requirement in the file touches HAL-FORMS property typing or PATCH nullability spelling.
- `openspec/specs/membership-fees/spec.md`, `openspec/specs/non-functional-requirements/spec.md` — mention ranking only as data display; unaffected.

**Why no spec update is needed:**
The change is codegen/tooling: it widens what API authors may *write* in `docs/openapi/spec/` without changing what the API *does*. All wire formats, Java signatures, and frontend types of existing endpoints stay identical (enforced by a zero-diff check in the final refactor phase); the ranking fix returns the endpoint to its spec-mandated behavior.

## Impact

- `backend/buildSrc/src/main/java/com/klabis/openapi/codegen/KlabisSpringCodegen.java` — annotation assembly + `isNullable` handling.
- `backend/src/main/openapi-templates/pojo.mustache` — render the assembled `@HalForms` extension.
- `tools/openapi-bundle/lib/derive.mjs`, `validate.mjs` — consume/strip directives, allowlist + validation rules.
- `docs/openapi/spec/*.yaml` — ranking application + mass `oneOf: [$ref, null]` refactor.
- Skills/docs: `klabis-api-spec` (extension reference + anti-patterns), `docs/openapi/spec/README.md`.
- No runtime Java changes; `HalFormsSupport` already reads `@HalForms.formInputType()`. No frontend code changes; frontend types stay byte-identical.
- Developer workflow: `./gradlew openapiBundle` + `openApiGenerate*` pipeline unchanged; new extensions are validated up front (nothing silent).
