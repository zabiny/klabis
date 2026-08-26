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

- [ ] 3.1 Move `event-types.yaml`'s `paths` and `components.schemas` entries into `events.yaml`,
      preserving the `EventTypes` tag.
- [ ] 3.2 Update `klabis.yaml`'s path `$ref`s for the two `event-types` paths
      (`/api/event-types`, `/api/event-types/{id}`) to point at `./events.yaml` instead.
- [ ] 3.3 Delete `docs/openapi/spec/event-types.yaml`.
- [ ] 3.4 Run `./gradlew openapiBundle -PopenapiCheck` then `./gradlew openapiBundle` — diff
      against the 1.2 baseline, must be byte-identical.

## 4. Gradle codegen wiring

- [ ] 4.1 Add a `specFile: String` parameter to `openApiModule(...)` in
      `backend/buildSrc/src/main/kotlin/OpenApiModule.kt`; set `inputSpec` from
      `docs/openapi/spec/$specFile` instead of the shared `klabis-full.json` path.
- [ ] 4.2 Remove the `models: List<String>` and `apis: List<String>` parameters from
      `openApiModule(...)`'s signature and from the `globalProperties` block (the `"models"`/`"apis"`
      keys in `globalProperties` are dropped entirely — every tag/schema in the target file is now
      generated).
- [ ] 4.3 Update the 7 unaffected call sites in `backend/build.gradle.kts` to point `specFile` at
      their own file: `members` → `members.yaml`; `finance` → `finance.yaml`; `calendar` →
      `calendar.yaml`; `membershipfees` → `membershipfees.yaml`; `common` → `common.yaml`; `oris` →
      `oris.yaml` (drop its now unnecessary `apis = listOf("OrisImport")` filter and placeholder
      `models` list — confirm via `grep tags: oris.yaml` that `OrisImport` remains the only tag
      first).
- [ ] 4.4 Merge the `events`/`event-types` call sites into a single `events` call with
      `specFile = "events.yaml"` (drop the separate `event-types` call entirely).
- [ ] 4.5 Collapse the `groupsFamily`/`groupsFree`/`groupsTraining` call sites into a single
      `groups` call: `module = "groups"`, `pkg = "com.klabis.groups.infrastructure.restapi"`,
      `specFile = "groups.yaml"`. Merge the three existing `mappings`/`extraImportMappings` blocks
      (`EntityModelParentResponse`/`EntityModelFamilyGroupMembershipResponse`,
      `EntityModelOwnerResponse`/`EntityModelFreeGroupMembershipResponse`/
      `EntityModelPendingInvitationResponse`, `EntityModelTrainerResponse`/
      `EntityModelGroupMembershipResponse`) into one combined `mapOf(...)` pair — confirm no key
      collision before merging (already verified disjoint during design).
- [ ] 4.6 Confirm every `openApiModule(...)` call still `dependsOn(openapiBundle)` (kept
      deliberately — see design.md Decision 5 — for the spec validation side effect, not for
      `inputSpec` correctness).

## 5. Fix hand-written groups imports

- [ ] 5.1 Run `./gradlew clean compileJava` — expect compile failures only in
      `groups.familygroup.infrastructure.restapi`, `groups.freegroup.infrastructure.restapi`,
      `groups.traininggroup.infrastructure.restapi` for generated types that are no longer
      package-local.
- [ ] 5.2 Add explicit imports (`import com.klabis.groups.infrastructure.restapi.X;`) to every
      file reporting a missing-symbol error. Do not change any logic, method signature, or
      behavior — this is import-only. Expected files (confirm against the actual compiler output,
      this list is from static inspection during design):
      `FamilyGroupController`, `FamilyGroupExceptionHandler`, `FamilyGroupOpenApiConfig`,
      `FamilyGroupIdMixin`, `MemberFamilyGroupLinkProcessor`, `FreeGroupController`,
      `PendingInvitationsController`, `FreeGroupExceptionHandler`, `FreeGroupOpenApiConfig`,
      `FreeGroupIdMixin`, `InvitationIdMixin`, `InvitationModelBuilder`, `TrainingGroupController`,
      `TrainingGroupExceptionHandler`, `TrainingGroupOpenApiConfig`, `TrainingGroupIdMixin`,
      `MemberTrainingGroupLinkProcessor`.
- [ ] 5.3 Re-run `./gradlew clean compileJava` — expect a clean build with zero errors.
- [ ] 5.4 Confirm the three implementation subpackages themselves are unchanged (no class moved,
      renamed, or had its package declaration edited) — only new `import` statements were added.

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
