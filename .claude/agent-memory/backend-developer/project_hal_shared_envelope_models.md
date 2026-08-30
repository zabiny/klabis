---
name: hal-shared-envelope-models
description: Bundler HAL deriver composes derived envelopes from shared EntityModel/CollectionModel/PagedModel base models in _shared/hal.yaml via allOf
metadata:
  type: project
---

The OpenAPI bundler's HAL deriver (`tools/openapi-bundle/lib/derive.mjs`) produces derived
envelope schemas as `allOf` compositions of shared base models, not inline `_links`/`_templates`.

**Why:** DRY — the `{_links, _templates, page}` structure was repeated in every derived
`EntityModelX`/`CollectionModelX`/`PagedModelX` schema in the generated `klabis-full.json`.

**How to apply:**
- Base models `EntityModel`, `CollectionModel`, `PagedModel` are defined once in
  `docs/openapi/spec/_shared/hal.yaml`. `PagedModel` = `allOf: [CollectionModel, {page}]`.
- Derived shapes:
  - item: `EntityModelFoo` = `allOf: [Foo, {$ref EntityModel}]`
  - collection: `CollectionModelEntityModelFoo` = `allOf: [{$ref CollectionModel}, {_embedded: {fooList: [...]}}]`
  - paged: same but `{$ref PagedModel}`
  - `x-hal-embedded`: `allOf: [Foo, {$ref EntityModel}, {_embedded: {...}}]`
- No module YAML references the base models. `bundleSpec` (`bundle.mjs`) hoists them from
  `rootDir/_shared/hal.yaml` before `deriveHalEnvelopes` runs. If the reader stub in
  `bundle.test.mjs` has no `_shared/hal.yaml` entry, `ensureEnvelopeBaseModels` is a no-op.
- `isEnvelopeShaped()` recognises three shapes: new `allOf` with `{$ref .../EntityModel|CollectionModel|PagedModel}`
  member; legacy `allOf` with inline `{properties: {_links}}` member; flat `{properties: {_links|_embedded}}`.
- `validate.mjs` `missingEnvelopeBaseErrors` fails the bundle if a derived envelope references a
  base model that was not hoisted.
- Hand-written marker types `EntityModelRootModel` / `EntityModelDashboardModel` in `common.yaml`
  (schemaMappings-bound to `RootModel`/`DashboardModel`) keep the OLD flat `{_links}` shape and are
  the only envelope kind backend codegen sees. They were deliberately NOT migrated.
- `klabis-full.json` is gitignored (removed from git in PR #312, "generate on demand"). `npm run openapi`
  regenerates bundle + `klabisApi.d.ts` + `halTypes.ts`. `halTypes.ts` is unaffected — it references
  `components['schemas'][name]` via `&`, shape-agnostic.
