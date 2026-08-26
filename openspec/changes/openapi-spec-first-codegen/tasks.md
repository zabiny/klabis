## 1. Baseline snapshot

- [x] 1.1 Run `./gradlew clean compileJava` on current `main` and save the full generated source
      tree (`build/generated/openapi/*/src/main/java`) as a comparison baseline (e.g. a checksummed
      file listing per module, or a saved `find ... -type f` output).
- [x] 1.2 Save the current `docs/openapi/klabis-full.json` checksum as a bundle-equivalence
      baseline for steps 2–3.

## 2. Extract `_shared/responses.yaml`

- [x] 2.1 Create `docs/openapi/spec/_shared/responses.yaml` with the six `responses` entries
      (`BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `UnprocessableEntity`),
      each `$ref`-ing `./problem.yaml#/components/schemas/ProblemDetail`, matching what's in
      `groups.yaml` today.
- [x] 2.2 Repoint the `responses` block in all 9 domain files (`members.yaml`, `events.yaml`,
      `event-types.yaml`, `finance.yaml`, `calendar.yaml`, `membershipfees.yaml`, `groups.yaml`,
      `common.yaml`, `oris.yaml`) to `$ref: './_shared/responses.yaml#/components/responses/<Name>'`
      instead of local definitions. `oris.yaml` only references the 3 it uses
      (`BadRequest`/`Unauthorized`/`Forbidden`). `groups.yaml`'s content is otherwise untouched —
      no split. Deviation from original plan: local `components.responses` blocks were removed
      entirely (not kept as ref-stubs) because ref-stubs collide in `bundle.mjs`'s
      `collectComponents()` — every operation-level `$ref` now points directly at
      `./_shared/responses.yaml#/components/responses/<Name>`.
- [x] 2.3 Run `./gradlew openapiBundle -PopenapiCheck` — must report the spec valid with the same
      operation/schema counts as the 1.2 baseline. Run `./gradlew openapiBundle` and diff the
      output `klabis-full.json` against the 1.2 baseline — must be byte-identical.

## 3. Merge `event-types.yaml` into `events.yaml`

- [x] 3.1 Move `event-types.yaml`'s `paths` and `components.schemas` entries into `events.yaml`,
      preserving the `EventTypes` tag.
- [x] 3.2 Update `klabis.yaml`'s path `$ref`s for the two `event-types` paths
      (`/api/event-types`, `/api/event-types/{id}`) to point at `./events.yaml` instead.
- [x] 3.3 Delete `docs/openapi/spec/event-types.yaml`.
- [x] 3.4 Run `./gradlew openapiBundle -PopenapiCheck` then `./gradlew openapiBundle` — diff
      against the 1.2 baseline, must be byte-identical.

## 4. Gradle codegen wiring

- [x] 4.1 Add a `specFile: String` parameter to `openApiModule(...)` in
      `backend/buildSrc/src/main/kotlin/OpenApiModule.kt`; set `inputSpec` from
      `docs/openapi/spec/$specFile` instead of the shared `klabis-full.json` path.
- [x] 4.2 Remove the `models: List<String>` and `apis: List<String>` parameters from
      `openApiModule(...)`'s signature and from the `globalProperties` block. **Deviation from
      design.md:** the `"models"`/`"apis"` `globalProperties` keys are NOT omitted — they are set to
      `""` (empty string). Confirmed via `--info` log that an omitted key means "generate nothing"
      (`Skipping generation of models/APIs.`); only a present-but-empty value means "generate
      everything". This is unrelated to the withdrawn Decision 5 in
      `openspec/changes/archive/2026-08-25-custom-openapi-codegen/design.md` (a different,
      never-shipped `CodegenConfig`-level mechanism) — confirmed empirically that request DTOs
      reachable only via `requestBody` (e.g. `CreateEventTypeRequest`) DO generate under
      `models=""`/`apis=""`.
- [x] 4.3 Update the 7 unaffected call sites in `backend/build.gradle.kts` to point `specFile` at
      their own file: `members` → `members.yaml`; `finance` → `finance.yaml`; `calendar` →
      `calendar.yaml`; `membershipfees` → `membershipfees.yaml`; `common` → `common.yaml`; `oris` →
      `oris.yaml` (dropped its now unnecessary `apis = listOf("OrisImport")` filter and placeholder
      `models` list — confirmed via `grep tags: oris.yaml` that `OrisImport` remains the only tag).
- [x] 4.4 Merge the `events`/`event-types` call sites into a single `events` call with
      `specFile = "events.yaml"` (dropped the separate `event-types` call entirely).
- [x] 4.5 Collapse the `groupsFamily`/`groupsFree`/`groupsTraining` call sites into a single
      `groups` call: `module = "groups"`, `pkg = "com.klabis.groups.infrastructure.restapi"`,
      `specFile = "groups.yaml"`. Merged the three existing `mappings`/`extraImportMappings` blocks
      into one combined `mapOf(...)` pair — confirmed no key collision.
- [x] 4.6 Confirmed every `openApiModule(...)` call still `dependsOn(openapiBundle)` — unchanged.

**Unplanned work required to make 4.1–4.6 actually compile** (discovered only once `models=""`
started generating everything reachable, not documented in design.md):

- Deleted dead hand-written `backend/src/main/java/com/klabis/oris/OrisEventSummary.java` — the
  `models = listOf("_NoGeneratedModelsForOris")` placeholder had been hiding that this schema was
  always safe to generate (zero hand-written references to the old class existed).
- Converted the hand-written nested record `CancelEventRequest` in `EventController.java` into a
  spec-generated one: added `x-field-extra-annotation:
  com.klabis.common.ui.HalForms(formInputType = "textarea")` to `docs/openapi/spec/events.yaml`'s
  `CancelEventRequest.cancellationReason` property (existing extension mechanism, no template
  change needed) and removed the hand-written record.
- Added a `postProcessAllModels` override to `KlabisSpringCodegen.java` (with two new private
  helpers, `envelopeAndFragmentNames`/`embeddedReferencedClassnames`) that removes every HAL
  envelope schema and its orphaned `allOf`-decomposition fragments from the generated model set —
  `models=""`/`apis=""` generates every schema reachable in the document, including envelope
  schemas that were always meant to stay ungenerated (per the `klabis-api-spec` skill: "Payload and
  envelope are separate schemas") and their synthetic `AllOf<Property>` sub-schemas, none of which
  any hand-written code ever references. Uses a fixed-point reachability pass over the
  `CodegenProperty` graph, seeded with roots from operation request/response content AND from
  envelope `_embedded` blocks (the latter needed because `_embedded` content is assembled at
  runtime by `HalResponseContext`/`HalResponseBodyAdvice`, never as a Java field — see
  `MemberInGroupResponse`, reachable only via
  `EntityModelMembershipFeeGroupResponseWithMembers._embedded.members`). Also strips same-package
  stale `imports` entries left over from openapi-generator's own model-collapse behavior (observed
  on `EntityModelFreeGroupMembershipResponseAllOfLinks`, whose generated record has no field typed
  against its own dangling import). This also incidentally resolved a `MethodEnum` symbol bug
  (`RESOLVE_INLINE_ENUMS` inconsistently naming a shared inline enum across its many
  `HalFormsTemplate.method` occurrences) — the affected `*AllOfTemplates` classes are themselves
  envelope fragments the filter now removes.
- Renamed `EventImportEntryStatus` → `EventSyncEntryStatus` in two hand-written MapStruct
  converters (`BulkImportResultConverter`, `BulkSyncResultConverter`) — `EventSyncEntry.status`/
  `EventImportEntry.status` share one inline enum that `RESOLVE_INLINE_ENUMS` promotes to a single
  class named after whichever occurrence the generator resolves second; dropping `models`/`apis`
  changed which occurrence that is. Left a comment explaining the fragility for future spec
  changes.

## 5. Fix hand-written groups imports

- [x] 5.1 Run `./gradlew clean compileJava` — compile failures found in
      `groups.familygroup.infrastructure.restapi`, `groups.freegroup.infrastructure.restapi`,
      `groups.traininggroup.infrastructure.restapi` for generated types that are no longer
      package-local, as expected.
- [x] 5.2 Added explicit imports (`import com.klabis.groups.infrastructure.restapi.X;`) to every
      file reporting a missing-symbol error: `FamilyGroupController`, `FreeGroupController`,
      `PendingInvitationsController`, `InvitationModelBuilder`, `TrainingGroupController`,
      `MemberFamilyGroupLinkProcessor`, `MemberTrainingGroupLinkProcessor`. The
      `*ExceptionHandler`/`*OpenApiConfig`/`*IdMixin` classes design.md expected to need imports
      turned out not to reference generated types directly — no changes needed there.
- [x] 5.3 Re-ran `./gradlew clean compileJava` — clean build with zero errors, after also fixing an
      unplanned issue below.
- [x] 5.4 Confirmed the three implementation subpackages themselves are unchanged (no class moved,
      renamed, or had its package declaration edited) — only new `import` statements were added.

**Unplanned work — systemic generated-record field-order mismatch (not anticipated by design.md):**
Fixing imports alone left ~40 "incompatible types"/positional-constructor errors spanning 6 modules
(calendar, common, events, finance, groups, membershipfees), not just groups. Root cause: the old
bundle-based codegen (`klabis-full.json`, produced by `bundle.mjs` from JS objects) generated Java
record fields in a DIFFERENT order than direct-YAML codegen (this change) does — verified by
regenerating from the pristine pre-refactor commit and diffing `IcalTokenResponse`'s field order
(`lastSetAt, url` old vs `url, lastSetAt` new, matching the YAML's literal declared property order).
Every hand-written call site using a positional `new GeneratedType(arg1, arg2, ...)` constructor was
at risk — worse, a same-type-adjacent-field swap (e.g. two `String`/`UUID` fields) would have
compiled silently wrong rather than erroring. Per explicit user direction, fixed by converting every
such call site to the record's already-available `@RecordBuilder`-generated builder
(`TypeNameBuilder.builder().field(value)....build()`, named setters, immune to field-order drift) —
22 files, ~45 call sites, backend/src/main/java only. A full sweep (regex over all 208 generated
record type names) confirmed no remaining positional constructor calls, and caught one genuine silent
field-order bug already in `main` before this refactor (`MemberDetailsConverter.guardianToResponse`
had `GuardianDTO`'s `email`/`phone` swapped) plus two incidental fixes (missing imports in the two
`Member*GroupLinkProcessor` classes; a stale `FeeAssignmentResponseSource` enum reference in
`MembershipFeesResponseMapper` corrected to the actual generated `MemberInGroupResponseSource`).

## 6. Verification

- [ ] 6.1 Diff the full generated source tree (`build/generated/openapi/*/src/main/java`) against
      the 1.1 baseline for every module except `groups` — expect zero file or content differences.
      For `groups`, confirm the same set of generated file basenames as the sum of the former
      `groupsFamily`+`groupsFree`+`groupsTraining` output now live under
      `build/generated/openapi/groups/src/main/java/com/klabis/groups/infrastructure/restapi/`.
- [ ] 6.2 Use the `test-runner` agent to run the full backend test suite — expect no failures and
      no test modifications needed.
- [ ] 6.3 Run `./gradlew openapiBundle -PopenapiCheck` one final time from the fully merged state
      to confirm `validateModuleDocuments()` reports no drift across the now-8 domain files.
- [ ] 6.4 Manually spot-check `docs/openapi/spec/klabis.yaml`, `groups.yaml`, and `events.yaml`
      render correctly in Swagger UI/Redoc (or equivalent standalone-open check).
- [ ] 6.5 Grep the codebase for any remaining reference to the old
      `groupsFamily`/`groupsFree`/`groupsTraining` Gradle task names (e.g. in CI config, IDE run
      configurations, or documentation) and update to the merged `openApiGenerateGroups` task name.
