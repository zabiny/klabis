## Context

`docs/openapi/spec/` is the hand-written source of truth for the Klabis API. `klabis.yaml` is the
root document; it `$ref`s path fragments from nine domain files (`members.yaml`, `events.yaml`,
`event-types.yaml`, `finance.yaml`, `calendar.yaml`, `membershipfees.yaml`, `groups.yaml`,
`common.yaml`, `oris.yaml`). `tools/openapi-bundle/bundle.mjs` resolves every cross-file `$ref`
(including `./_shared/hal.yaml`, `./_shared/problem.yaml`, `./_shared/pagination.yaml`) into the
single committed `docs/openapi/klabis-full.json`, which the frontend's `npm run openapi` reads for
TypeScript types.

The backend generates Java DTOs/API interfaces from that same `klabis-full.json` via
`openApiModule(...)` (`backend/buildSrc/src/main/kotlin/OpenApiModule.kt`), one Gradle task per
module. Because `inputSpec` is always the *entire* bundle, each call must filter its scope down
with two `globalProperties`: `models` (schema names to generate) and `apis` (tag names to generate
interfaces for). A recent commit expanded `models` to enumerate every DTO per module — these lists
are now large, and every schema add/rename/remove requires updating both the spec file and this
parallel Gradle list.

Two structural mismatches stand between "one spec file, no enumeration" and today's file layout:

1. **`groups.yaml` feeds three Gradle modules.** `FamilyGroupController`,
   `FreeGroupController`/`PendingInvitationsController`, and `TrainingGroupController` are three
   separate aggregate roots, generated into three separate packages
   (`groups.familygroup`/`groups.freegroup`/`groups.traininggroup`) via three separate
   `openApiModule(...)` calls (`groupsFamily`/`groupsFree`/`groupsTraining`), each with its own
   `schemaMappings` — but all three read from the one `groups.yaml` file today, distinguished only
   by the `apis`/`models` filter.
2. **`events.yaml` and `event-types.yaml` are two files feeding one Gradle module.** Both already
   generate into `com.klabis.events.infrastructure.restapi` (module `events` / `event-types`
   respectively) — the split exists in the spec directory but not in the Gradle wiring.

Every domain file is already a standalone OpenAPI document (own `openapi:`/`info:`/
`securitySchemes` — see `event-types.yaml`'s header comment explaining this convention exists
specifically so `$ref` cannot leak file identity: "`$ref` cannot target a document's own info").
This was confirmed by inspection: all nine files carry `openapi: 3.1.0` and a full `info:` block,
not just fragments.

Hand-written classes today reference the generated types **without an import**: e.g.
`FamilyGroupController implements FamilyGroupsApi` and uses `FamilyGroupResponse` with no
`import` statement, because the generator's `modelPackage`/`apiPackage` for the `groupsFamily`
task is set to the exact same package the hand-written controller lives in
(`com.klabis.groups.familygroup.infrastructure.restapi`). This package-per-aggregate split exists
so cross-module link processors can address each aggregate's types via Modulith named interfaces
(see `OpenApiModule.kt`'s doc comment: "Each module generates into the same package as its
hand-written controller/mapper, because cross-module link processors depend on those packages via
Modulith named interfaces").

## Goals / Non-Goals

**Goals:**
- Every `openApiModule(...)` Gradle task reads `inputSpec` directly from its own
  `docs/openapi/spec/<file>.yaml`, with no `models`/`apis` enumeration.
- Spec-file boundaries map 1:1 onto Gradle codegen module boundaries (one `openApiModule(...)`
  call per spec file).
- No change to any REST endpoint, DTO field shape, or the committed `klabis-full.json` content.
- Reduce the `components.responses` duplication (byte-identical across 8/9 domain files) via a
  shared `_shared/responses.yaml`.

**Non-Goals:**
- Changing `bundle.mjs`, `klabis.yaml`'s role as root document, or the frontend's OpenAPI
  consumption path — these are unaffected and stay exactly as they are.
- Extracting `components.securitySchemes` to `_shared/` — investigated and rejected (Decision 3).
- Splitting `groups.yaml` into multiple spec files — considered and superseded by Decision 1
  below.
- Restructuring hand-written implementation packages
  (`groups.familygroup`/`groups.freegroup`/`groups.traininggroup` stay exactly where they are;
  this change is scoped to generated-code package location only).
- Any change to REST behavior, DTO field shape, authorization, or validation rules.

## Decisions

### Decision 1 — Collapse the three `groups` codegen tasks into one shared-package task; do NOT split `groups.yaml`

Two ways to reach "one spec file : one `openApiModule` call" for `groups.yaml` were considered:

**Option A — split the spec file into three** (`groups-family.yaml`/`groups-free.yaml`/
`groups-training.yaml`), keep three `openApiModule(...)` calls, one package each (original
proposal). Rejected in favor of Option B, on direction from the user: it triples the file count in
`docs/openapi/spec/` for a domain that is one bounded context in the backend (`groups` module),
and re-derives the per-aggregate split that the hand-written package layout already expresses —
duplicating that boundary in the spec directory adds a second place it has to be kept consistent.

**Option B — keep `groups.yaml` as one file, collapse codegen to one shared package (chosen).**
Register a single `openApiModule(module = "groups", pkg =
"com.klabis.groups.infrastructure.restapi", ...)` call that generates the *entire* file — all
three tags (`FamilyGroups`, `Groups`+`Invitations`, `TrainingGroups`) and every schema — into one
new package, replacing the three `groupsFamily`/`groupsFree`/`groupsTraining` calls. This is safe
because `groups.yaml` has zero duplicate schema names across its three tag groups (confirmed via a
name-count scan of `components.schemas`), so merging their generation target into one package
produces no collisions.

**Consequence — hand-written classes need explicit imports.** Since generated types move from each
aggregate's own package (`groups.familygroup.infrastructure.restapi`, etc.) to the new shared
`groups.infrastructure.restapi`, every hand-written class that used a generated type without an
import (because it used to be package-local) now needs one. This is mechanical and compiler-caught
(a missing symbol fails the build, it cannot silently pass), but touches every hand-written file in
the three implementation subpackages that references a generated groups DTO or `*Api` interface —
see the file list gathered during exploration:
`FamilyGroupController`/`FamilyGroupExceptionHandler`/`FamilyGroupOpenApiConfig`/
`FamilyGroupIdMixin`/`MemberFamilyGroupLinkProcessor` (familygroup);
`FreeGroupController`/`PendingInvitationsController`/`FreeGroupExceptionHandler`/
`FreeGroupOpenApiConfig`/`FreeGroupIdMixin`/`InvitationIdMixin`/`InvitationModelBuilder`
(freegroup); `TrainingGroupController`/`TrainingGroupExceptionHandler`/
`TrainingGroupOpenApiConfig`/`TrainingGroupIdMixin`/`MemberTrainingGroupLinkProcessor`
(traininggroup).

**Implementation packages are explicitly NOT moved.** `FamilyGroupController` and friends stay in
`groups.familygroup.infrastructure.restapi` (etc.) exactly where they are today — only the
*generated* code's package changes. This is a deliberate scope limit from the user: consolidating
hand-written implementation packages is a separate, larger decision (affects Modulith named
interfaces, per the `OpenApiModule.kt` comment cited in Context) that this change does not make.

**`schemaMappings`/`extraImportMappings` merge cleanly.** The three existing mapping blocks
(`groupsFamily`: `EntityModelParentResponse`/`EntityModelFamilyGroupMembershipResponse`;
`groupsFree`: `EntityModelOwnerResponse`/`EntityModelFreeGroupMembershipResponse`/
`EntityModelPendingInvitationResponse`; `groupsTraining`: `EntityModelTrainerResponse`/
`EntityModelGroupMembershipResponse`) use disjoint key names — confirmed no overlap — so they
combine into a single `mapOf(...)`/`extraImportMappings` pair for the merged `groups` call with no
conflict.

### Decision 2 — Merge `event-types.yaml` into `events.yaml`

Both files generate into the same package and the same conceptual Gradle module today (`events`
in the module list, `com.klabis.events.infrastructure.restapi` package). Keeping them as two spec
files after the enumeration is dropped would mean the `events` Gradle task needs two `inputSpec`
values, which `GenerateTask` does not support (`inputSpec` is a scalar). Merging is the only
option that preserves "one file per module." This is a genuine two-file merge (unlike groups,
where the file was already single) — `event-types.yaml`'s `EventTypes` tag/schemas move into
`events.yaml`, and `event-types.yaml` is deleted.

### Decision 3 — Extract `components.responses`, but NOT `components.securitySchemes` (rejected alternative)

`components.responses.{BadRequest, Unauthorized, Forbidden, NotFound, Conflict,
UnprocessableEntity}` is byte-identical (verified via `md5sum`) across 8 of the 9 domain files;
`oris.yaml` has a strict subset (`BadRequest`/`Unauthorized`/`Forbidden` only). These are
referenced from `paths` exclusively via `$ref`, so extraction to `_shared/responses.yaml` is
mechanically identical to the existing `_shared/problem.yaml`/`_shared/hal.yaml` pattern. This was
verified experimentally against the real bundler before writing this design: a standalone script
called `bundleSpec()` from `tools/openapi-bundle/lib/bundle.mjs` against two synthetic domain
files both `$ref`-ing a shared `_shared/responses.yaml`, and confirmed zero conflicts and a
correctly resolved bundle.

`components.securitySchemes.KlabisAuth` is also byte-identical across all 9 files (verified via
`md5sum`), but is **not** being extracted. Two independent reasons converge:

1. **OpenAPI semantics.** An operation's `security: [{KlabisAuth: [scope]}]` requirement names the
   scheme by key, not by `$ref`. Every standalone file must therefore still declare a local
   `KlabisAuth` key under `components.securitySchemes` to be independently valid (e.g. to open
   correctly in Swagger UI/Redoc, which the header comments in `event-types.yaml`/`oris.yaml`
   explicitly call out as a goal).
2. **Bundler limitation.** `tools/openapi-bundle/lib/bundle.mjs`'s `collectComponents()` claims a
   component name in the accumulator *before* resolving its (possibly `$ref`-based) definition, to
   short-circuit cyclic refs. Confirmed by direct experiment: `$ref`-ing a `securitySchemes` entry
   out to `_shared/security.yaml` causes the bundler to either report a false
   `conflicts: [{bucket: "securitySchemes", ...}]` (when the referencing file still needs the
   local key present, which it does per point 1) or, if the local key is omitted instead, silently
   drops `securitySchemes` from the bundle entirely, leaving `security: [{KlabisAuth: ...}]`
   pointing at an undefined scheme.

This exact approach — `$ref`-ing `securitySchemes` from `klabis.yaml`'s own root component block —
was already tried once and reverted; the reasoning is preserved as a code comment at
`docs/openapi/spec/klabis.yaml:188-191`: *"Inlined rather than `$ref`'d to `_shared/security.yaml`:
a root component that only `$ref`s another file collapses to a self-referencing
`$ref: #/components/securitySchemes/KlabisAuth` in the bundle... The module files carry the same
definition; `validate.mjs` checks that all ten stay identical."* `KlabisAuth` stays duplicated
verbatim per file, as it is today, protected by the existing `validateModuleDocuments()` drift
check in `tools/openapi-bundle/lib/validate.mjs` — no change needed there, since
`moduleFileNames()` derives its file list from `klabis.yaml`'s `paths` refs automatically, and the
file count only changes by the events/event-types merge (9 → 8 files), not by a groups split.

### Decision 4 — `inputSpec` reads the domain file directly; `models`/`apis` are dropped

`OpenApiModule.kt` gains a `specFile` parameter (path under `docs/openapi/spec/`) that becomes
`inputSpec`, replacing the shared `layout.projectDirectory.file("../docs/openapi/klabis-full.json")`.
`models` and `apis` parameters are removed from the function signature and every call site — the
target file's full `paths`/`components.schemas` content *is* the module's scope, since after
Decisions 1–2 there is no longer a file covering more than one module's generation target.

This was verified as viable before writing this design: `openapi-generator`'s `inputSpec` resolves
relative cross-file `$ref`s (e.g. `./_shared/hal.yaml#/components/schemas/Link`) using the same
mechanism `bundle.mjs` already relies on (both ultimately delegate to standard JSON-pointer/file
resolution against paths relative to the input document) — no separate pre-bundling step is
required for the backend codegen path.

`oris.yaml`'s existing `apis = listOf("OrisImport")` filter — needed today only because the
generator's substring tag-matching would otherwise also pull in `events.yaml`'s `OrisEvents` tag
from the shared bundle — becomes unnecessary once `inputSpec` is scoped to `oris.yaml` alone: no
other tag exists in that file to collide with.

The `doFirst { project.delete(outputDir) }` staleness guard, `templateDir`, `configOptions`,
`typeMappings`, and all other `GenerateTask` configuration in `OpenApiModule.kt` stay unchanged.

### Decision 5 — `openapiBundle` dependency

Today every `openApiModule(...)` task `dependsOn(openapiBundle)` because `inputSpec` reads
`klabis-full.json`, which `openapiBundle` produces. Once `inputSpec` reads the domain file
directly, this `dependsOn` is no longer required for correctness — but it is kept anyway, because
`openapiBundle` also runs `validateSpec`/`validateModuleDocuments`, which is the only place that
catches a domain file drifting from `klabis.yaml` (e.g. a stale path `$ref`, a `securitySchemes`
mismatch). Keeping the dependency means a broken spec still fails the build at the same point it
does today, rather than silently succeeding backend codegen against a domain file the bundle would
have rejected.

## Risks / Trade-offs

- **[Risk]** Every hand-written class in the three groups implementation subpackages that
  currently relies on package-local (import-free) resolution of a generated type will fail to
  compile the moment the generated package changes, until an explicit import is added.
  **Mitigation:** this is compiler-caught, not a silent correctness risk — `./gradlew
  compileJava` after the Gradle-side change enumerates every file needing an import in one pass;
  fix mechanically (IDE "import missing symbol" or manual `import
  com.klabis.groups.infrastructure.restapi.X;`) with no logic change.
- **[Risk]** Generated source drift: with `models`/`apis` removed, a schema present in a domain
  file but never reachable from any of its `paths` (dead/orphaned schema) will now be generated
  where it previously would have been silently excluded (or vice versa — a schema currently
  force-included via `models` that isn't actually reachable from `paths` would stop generating).
  **Mitigation:** run `./gradlew compileJava` before/after and diff the full generated file list
  per module (`build/generated/openapi/<module>/src/main/java`) — any file appearing or
  disappearing needs manual review against the corresponding `models` list entry it replaces. For
  the merged `groups` module specifically, also confirm the generated file *names* are unchanged
  (only their package/directory moved) by comparing basenames before/after.
- **[Risk]** `oris.yaml`'s dropped `apis` filter relies on there being no other tag left in the
  file. **Mitigation:** grep `oris.yaml` for `tags:` before removing the filter to confirm
  `OrisImport` is still the only tag present.
- **[Trade-off]** The three groups aggregates' generated code no longer sits alongside its
  hand-written implementation in the same package — a developer opening
  `groups.traininggroup.infrastructure.restapi` now sees only hand-written classes, with generated
  DTOs one package hop away in `groups.infrastructure.restapi`. Accepted per explicit user
  direction; revisiting the hand-written package layout to match is out of scope for this change
  (see Non-Goals).

## Migration Plan

1. Extract `_shared/responses.yaml`; repoint all nine existing domain files' `$ref`s to it (no
   file split/merge yet). Run `./gradlew openapiBundle -PopenapiCheck` — bundle must be byte-
   identical to the current committed `klabis-full.json` (this step changes only `$ref` targets,
   not resolved content).
2. Merge `event-types.yaml` into `events.yaml`; update `klabis.yaml`'s path `$ref`s; delete
   `event-types.yaml`. Re-run the bundle check — still byte-identical.
3. Update `OpenApiModule.kt` (`specFile` parameter) and every `openApiModule(...)` call site in
   `build.gradle.kts`: repoint `inputSpec` for the 7 untouched-file modules, merge
   `events`/`event-types` into one `events` call, and collapse `groupsFamily`/`groupsFree`/
   `groupsTraining` into one `groups` call (merged `schemaMappings`/`extraImportMappings`,
   `pkg = "com.klabis.groups.infrastructure.restapi"`). Drop `models`/`apis` from every call.
4. `./gradlew clean compileJava`; expect compile failures only in the groups implementation
   subpackages (missing imports for relocated generated types — see Risks). Add the needed
   imports mechanically; no other module should show any compile difference.
5. Diff the generated source tree against a pre-change snapshot for every module *except* groups —
   expect zero content differences. For groups, confirm the same set of generated file basenames
   now live under `build/generated/openapi/groups/...` instead of split across three module
   output directories.
6. Run the full backend test suite via the `test-runner` agent per project convention.

**Rollback:** every step above is independently revertible via `git revert` — nothing here touches
runtime business logic, only the spec source layout, Gradle codegen wiring, and hand-written
import statements, so a broken intermediate state costs a `gradlew clean` and a revert, not a
redeploy.

## Open Questions

- None outstanding — the two technical uncertainties this design depends on (whether
  `bundle.mjs`'s `$ref` resolution is generic enough for a `responses.yaml` extraction, and
  whether `openapi-generator`'s `inputSpec` can read a domain file directly) were both resolved by
  direct experiment before this document was written. The groups package-consolidation approach
  (Decision 1, Option B) was chosen directly by the user over the file-split alternative
  originally proposed.
