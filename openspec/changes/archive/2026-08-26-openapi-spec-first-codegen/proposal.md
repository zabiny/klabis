## Why

Each backend `openApiModule(...)` call in `backend/build.gradle.kts` generates from the single
merged `docs/openapi/klabis-full.json` bundle, filtered down to one module's scope via explicit
`models`/`apis` lists. A recent commit expanded those lists to enumerate **every** DTO in each
module, making them large, redundant with the spec itself, and a second place (besides the spec
file) that must be kept in sync whenever a schema is added, renamed, or removed. Since
`docs/openapi/spec/` already splits the API into one hand-written file per domain, codegen can
read directly from each module's own file and drop the enumeration entirely — provided each spec
file maps 1:1 onto its gradle module, which today isn't quite true (`groups.yaml` feeds three
separate gradle modules/packages; `event-types.yaml` and `events.yaml` split one gradle module
across two files).

## What Changes

- Collapse the three `openApiModule(...)` calls that read `groups.yaml` today
  (`groupsFamily`/`groupsFree`/`groupsTraining`, one package each) into a single `groups` call
  generating the whole file into one shared package, `com.klabis.groups.infrastructure.restapi`.
  `groups.yaml` itself is untouched — no split. Hand-written implementation classes
  (`FamilyGroupController`, `FreeGroupController`, `TrainingGroupController`, and their
  postprocessors/mappers/exception handlers) stay in their current subpackages
  (`groups.familygroup.infrastructure.restapi`, `groups.freegroup.infrastructure.restapi`,
  `groups.traininggroup.infrastructure.restapi`) — only their imports change, from
  package-local references to explicit imports of the now-relocated generated types.
- Merge `docs/openapi/spec/event-types.yaml` into `events.yaml` — both already generate into the
  same gradle module (`events`, package `com.klabis.events.infrastructure.restapi`).
- Extract the `components.responses` block (`BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`,
  `Conflict`, `UnprocessableEntity`) — byte-identical across 8 of 9 domain files, and a subset in
  `oris.yaml` — into `docs/openapi/spec/_shared/responses.yaml`, referenced via `$ref` from every
  domain file.
- `components.securitySchemes.KlabisAuth` is **not** extracted — see Rejected Alternative below.
- Update `backend/buildSrc/src/main/kotlin/OpenApiModule.kt` and every `openApiModule(...)` call in
  `backend/build.gradle.kts` so `inputSpec` points at the module's own
  `docs/openapi/spec/<file>.yaml` (new `specFile` parameter) instead of the shared
  `klabis-full.json`, and drop the `models`/`apis` enumeration parameters entirely — each file now
  *is* the module's full generation scope. `schemaMappings`/`extraImportMappings`
  (`EntityModel<X>` redirects) stay per-module, unaffected.
- Resulting module list (8, each 1:1 with its own spec file): `members`, `events` (merged),
  `finance`, `calendar`, `membershipfees`, `groups` (merged from 3), `common`, `oris`.

**Out of scope / unchanged:**
- `docs/openapi/spec/klabis.yaml` stays the root document that `$ref`s path fragments from every
  domain file. `moduleFileNames()`/`validateModuleDocuments()` in
  `tools/openapi-bundle/lib/validate.mjs` derive the module file list automatically from
  `klabis.yaml`'s path refs, so they require no manual update for the split/merge.
- `tools/openapi-bundle/bundle.mjs` and the committed `docs/openapi/klabis-full.json` are
  unchanged — the frontend's `npm run openapi` TypeScript codegen and Swagger UI/Redoc keep reading
  the bundle. Only backend Gradle codegen stops depending on it.
- No REST endpoint, request/response shape, status code, or authorization rule changes.

## No Behavior Change Justification

**Specs reviewed:** `openspec/specs/events/spec.md`, `openspec/specs/event-types/spec.md`,
`openspec/specs/event-categories/spec.md`, `openspec/specs/event-registrations/spec.md`,
`openspec/specs/user-groups/spec.md` — all describe user-observable API/business behavior for
these modules; none reference how the OpenAPI spec is split across files or how Gradle invokes
codegen.

**Why no spec update is needed:** This change reorganizes where hand-written OpenAPI source lives
on disk and how the Gradle codegen tasks read it. The generated Java DTOs, API interfaces, and the
committed `klabis-full.json` (and therefore the actual REST contract) are unchanged — every schema
and path that exists today continues to exist, unmodified, just declared in a different file
layout. This was verified by direct experimentation before this proposal was written: a spike
against the real `tools/openapi-bundle/lib/bundle.mjs` `bundleSpec()` confirmed the `responses`
extraction produces zero conflicts and bundles identically (including a two-module scenario), and
`openapi-generator`'s `inputSpec` resolves relative cross-file `$ref`s (e.g. `./_shared/hal.yaml`)
the same way `bundle.mjs` already does.

## Impact

- **Affected files:** `docs/openapi/spec/groups.yaml` (only `$ref` target for `responses` changes,
  content otherwise untouched), `docs/openapi/spec/events.yaml` (absorbs `event-types.yaml`, which
  is deleted), `docs/openapi/spec/klabis.yaml` (path `$ref`s repointed for the events merge),
  `docs/openapi/spec/_shared/responses.yaml` (new), `backend/build.gradle.kts`,
  `backend/buildSrc/src/main/kotlin/OpenApiModule.kt`, and every hand-written class in
  `groups.familygroup.infrastructure.restapi`, `groups.freegroup.infrastructure.restapi`,
  `groups.traininggroup.infrastructure.restapi` that references a generated groups DTO/API
  interface (import-only changes — see design.md Decision 1).
- **Developer workflow:** adding a new DTO or endpoint to a module no longer requires updating a
  parallel `models`/`apis` list in `build.gradle.kts` — the spec file is the single place to edit.
  Module boundaries in the spec directory now map 1:1 onto gradle codegen tasks, matching the
  mental model developers already use when navigating `docs/openapi/spec/`.
- **Build:** `./gradlew compileJava` (and therefore CI) must be re-verified to produce an identical
  set of generated sources after the change — this is the primary regression risk and should be
  the first verification step in `tasks.md`.

## Rejected Alternative

Extracting `components.securitySchemes.KlabisAuth` into `_shared/security.yaml` alongside
`responses.yaml` was considered and rejected. `security: [{KlabisAuth: [...]}]` in each operation
references the scheme **by name**, not `$ref`, so every standalone spec file must still declare a
local `KlabisAuth` key to remain independently valid (e.g. openable in Swagger UI). A spike
confirmed that `$ref`-ing that key's *value* out to `_shared/security.yaml` causes
`bundle.mjs`'s `collectComponents` (which claims a component name before resolving its
definition) to either produce a false conflict or collapse the bundled result into a
self-referencing `$ref` cycle. This exact approach was already tried once before and reverted —
see the explanatory comment at `docs/openapi/spec/klabis.yaml:188-191`. `KlabisAuth` stays
duplicated verbatim per file, as it is today, protected by `validate.mjs`'s existing
`validateModuleDocuments()` drift check.
