## Why

The `custom-openapi-codegen` change (archived 2026-08-25) closed with three
follow-ups recorded in its `design.md`, each raised by the closing quality review and
deferred because it would have reached beyond that change's diff while its acceptance
bar was byte-for-byte identical generated output. They are collected here.

The first is the substantive one: `postProcessOperationsWithModels` removes HAL envelope
imports *after* they are added, by matching an import's simple name against a schema name.
That is the only name-based rule in a generator whose design principle is matching by shape
(design.md Decision 3), and it leaves a latent hazard — a model import sharing a simple name
with an envelope schema would be dropped silently.

## What Changes

- **Override `DefaultCodegen.getContent()`** so an envelope import is never added, replacing
  the after-the-fact cleanup in `postProcessOperationsWithModels`. `getContent` (protected,
  sources line 7864) walks every media type of the raw response — including the HAL variant —
  and imports each one's type; it is the single origin of every leftover envelope import.
  With it overridden, `postProcessOperationsWithModels` is deleted.
- **Use `ModelUtils.isArraySchema`** in `HalEnvelopeDetector.asSingleArrayOfRefProperty`
  instead of `"array".equals(schema.getType())`. The rest of the pipeline already routes every
  schema-level array test through that helper, which also handles the OpenAPI 3.1
  `type: ["array"]` spelling. Latent today (the bundle emits scalar `type` strings), but an
  `_embedded` array written in list form would silently stop being detected.
- **Extract a shared test fixture builder** for the HAL envelope shapes built by hand in 22
  places across the four `buildSrc` test files. The detector's rules are structural, so those
  fixtures *are* the specification of the rules; a builder keeps them from drifting as rules
  change. Negative-case fixtures stay inline — deviating from the canonical shape is precisely
  what they test.

## No Behavior Change Justification

**Specs reviewed:** all 19 under `openspec/specs/`. Verified by search that none mentions
codegen, the generator, `schemaMappings`, or `buildSrc` — they describe API contracts and
business behavior, which are defined by `docs/openapi/spec/*.yaml` (untouched here) and
enforced by the generated code's *shape*, not by how that shape is produced.

**Why no spec update is needed:** this changes only the Java class hierarchy of the build-time
code generator and its unit tests. The acceptance bar is the same one the parent change used and
verified: generated Java sources must stay byte-for-byte identical across all 11 modules. Any
generated-code diff is a regression, not an intended outcome. The third item touches test files
only.

## Impact

- **`backend/buildSrc/.../KlabisSpringCodegen.java`** — gains a `getContent` override, loses
  `postProcessOperationsWithModels`.
- **`backend/buildSrc/.../HalEnvelopeDetector.java`** — one array test swapped for the shared helper.
- **`backend/buildSrc/src/test/.../`** — new fixture builder; the four existing test files use it.
- **No impact** on `backend/build.gradle.kts`, on generated output, on any REST endpoint's wire
  contract, on `docs/openapi/klabis-full.json`, or on `frontend/`.
