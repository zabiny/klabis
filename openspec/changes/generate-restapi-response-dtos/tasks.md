## 1. Spike: property-level unwrap hook (blocks Phase 6 only)

- [x] 1.1 Read `openapi-generator-7.18.0-sources.jar` for `DefaultCodegen.fromProperty(...)` and
      `postProcessModelProperty(...)` — confirm call order, whether the property's `Schema` can be
      rewritten before `super`'s own type/import resolution runs, and whether it receives the
      `schemas` map needed to resolve a `$ref`.
- [x] 1.2 Build a throwaway fixture schema (array of `$ref` to `allOf[inline-payload, {_links}]`,
      mirroring `EntityModelTrainerResponse`) and a unit test proving the chosen hook rewrites it to
      `List<Payload>` with no `EntityModel` in the resolved type.
- [x] 1.3 Widen `HalEnvelopeDetector.detectShape1` to package-private (or extract its structural
      check into a method reusable from the new property-level path) so both call sites share one
      rule instead of duplicating it.

## 2. Category A — `calendar` module (smallest, proves the recipe)

- [x] 2.1 Add `CalendarItemDto`, `IcalTokenResponse` to the `calendar` module's `models` list in
      `backend/build.gradle.kts`.
- [x] 2.2 Generate, diff the generated records against `CalendarItemDto.java` and the nested
      `IcalTokenResponse` in `IcalTokenController.java` (field names/types, `@HalForms` ↔
      `x-klabis-halforms-access` parity).
- [x] 2.2a Field-security check (design.md Decision 0): list every field-level annotation on both
      hand-written records (`@HalForms` on `CalendarItemDto.id`/`.eventId`; none on
      `IcalTokenResponse`), confirm each has a matching `x-klabis-*` extension in the generated
      spec/Java, confirm none is silently missing. Gate 2.3 on this passing.
- [x] 2.3 Delete both hand-written declarations; confirm `CalendarController`,
      `IcalTokenController`, and their postprocessors compile unchanged (same package, same simple
      name).
- [x] 2.4 Run `calendar` module's backend tests via the test-runner agent — no assertion changes
      expected.

## 3. Category A — `common`, `finance`, `members` modules

- [x] 3.1 Add `PermissionsResponse` to `common`'s `models` list; generate, diff, delete
      hand-written class, fix `PermissionController` import if needed.
- [x] 3.2 Add `MemberAccountResource` to `finance`'s `models` list; move its `fromBalance(...)`
      mapping logic out of the record into a MapStruct mapper or a plain static helper in
      `MemberAccountController`; generate, diff, delete hand-written class.
- [x] 3.3 Add `x-klabis-relation: {collectionRelation: transactions, itemRelation: transaction}` to
      `TransactionResource` in `finance.yaml`; extend `pojo.mustache` with the `x-klabis-relation`
      render rule (Decision 2) — first real use, verify the annotation compiles and the `_embedded`
      key is unchanged.
- [x] 3.4 Add `TransactionResource` to `finance`'s `models` list; move its `from(...)` mapping logic
      to a mapper; generate, diff, delete hand-written class.
- [x] 3.5 Add `MemberSummaryResponse`, `MemberOptionResponse` to `members`'s `models` list;
      generate, diff.
- [x] 3.5a Field-security check (design.md Decision 0): `MemberSummaryResponse.email`/`.active`
      carry `@HasAuthority(Authority.MEMBERS_MANAGE)` — confirm `members.yaml`'s
      `MemberSummaryResponse` has `x-klabis-authority: MEMBERS_MANAGE` on both properties and the
      generated Java actually renders `@HasAuthority`, not just that the spec has the extension.
- [x] 3.5b Delete `MemberSummaryResponse.java`/`MemberOptionResponse.java` — only after 3.5a passes.
- [x] 3.6 Add `SuspensionBlockedWarning`, `OutstandingDebtWarning`, `LastOwnerWarning`,
      `AffectedGroup` to `members`'s `models` list; correct the stale comment in `members.yaml`
      (lines ~815-816) that still references the removed `schemaMapping` mechanism; generate, diff,
      delete hand-written classes (including the nested ones in `MembersExceptionHandler`) — none of
      these four carry field-level security annotations today, so Decision 0's check here is
      confirming that absence stays true in the generated form, not adding anything.
- [x] 3.7 Run `common`, `finance`, `members` module tests via the test-runner agent.

## 4. Category A+B — `events` module

- [x] 4.1 Add `EventDto`, `RankingDto`, `EntryFeeDto`, `EventCategoryDto`, `EventSummaryDto`,
      `CategoryPresetDto` to `events`'s `models` list; generate, diff each against its hand-written
      counterpart (note `RankingDto`/`EntryFeeDto`/`EventCategoryDto` generate as top-level records,
      not nested inside `EventDto` — this is expected, see design.md Decision 1).
- [x] 4.2 Fix every reference to the nested form (`EventDto.RankingDto` etc.) across `events`
      module source to the top-level generated name.
- [x] 4.2a Field-security check (design.md Decision 0): `EventDto`/`EventSummaryDto` both mark
      `id`/`status`/`cancellationReason`/`deadlines` `@HalForms(access = READ_ONLY)`, and
      `EventSummaryDto.status` additionally carries `@HasAuthority(Authority.EVENTS_MANAGE)` +
      `@HandleAuthorizationDenied` — confirm `events.yaml`'s `EventDto`/`EventSummaryDto` schemas
      carry `x-klabis-halforms-access: READ_ONLY` on all four properties and
      `x-klabis-authority: EVENTS_MANAGE` on `EventSummaryDto.status`, and that the generated Java
      renders both. **PASS** — all four properties render `@HalForms(access = READ_ONLY)` on both
      generated records; `EventSummaryDto.status` additionally renders `@HasAuthority(EVENTS_MANAGE)`.
- [x] 4.3 Delete `EventDto.java`, `EventSummaryDto.java`, `CategoryPresetDto.java` — only after 4.2a
      passes.
- [x] 4.4 Add `x-klabis-relation` to `AccommodationListItemDto` (`collectionRelation:
      accommodationList`) in `events.yaml`; add it to `events`'s `models` list; generate, diff.
      **`RegistrationSummaryDto` NOT migrated — see 4.4a.** `x-klabis-relation:
      {collectionRelation: registrationDtoList}` was added to its schema for future use, but the
      class stays hand-written; do not add it to the `models` list until the gaps below are fixed.
- [x] 4.4a Field-security check (design.md Decision 0) — **FAILED for `RegistrationSummaryDto`,
      blocking its migration; `AccommodationListItemDto` passed and was migrated.**
      Two real gaps found when the generated `RegistrationSummaryDto` was wired in and the module's
      tests were run (not caught by spec/Java diffing alone — only surfaced at test time):
      1. **Functional regression (the one this check exists to catch):** the hand-written class's
         `@JsonIgnore @OwnerId Set<MemberId> coordinators` serves double duty — it is dropped from
         the wire, but its `@OwnerId` also tells `OwnershipResolver` which field represents
         "owner" for the sibling `@OwnerVisible Instant registrationTime`. There is no
         `x-klabis-*` extension (nor any codegen mechanism) to keep a property in the generated
         Java for ownership resolution while excluding it from JSON — the design's Decision 0
         checklist for `@JsonIgnore` only checks "absent from wire," not "absent from wire but
         needed for owner resolution." With `coordinators` gone entirely, `registrationTime` is
         silently hidden even from event coordinators (proven by two failing tests:
         `EventRegistrationControllerTest$RegistrationTimePrivacyTests.eventCoordinatorSeesRegistrationTime`,
         `EventControllerTest$GetEventTests.registrationTimeVisibleForSecondCoordinator`).
      2. **Wire-shape drift:** the hand-written `category` field carries
         `@JsonInclude(JsonInclude.Include.ALWAYS)`, overriding the class-level `NON_NULL` default
         so `category: null` is always present in the JSON (proven by
         `EventRegistrationControllerTest$ListRegistrationsTests.shouldReturnNullCategoryWhenOrphaned`
         failing with the property missing entirely). No `x-klabis-*` extension exists for a
         per-property `@JsonInclude` override; `pojo.mustache` has no render path for it.
      Both gaps required a codegen/spec-extension addition beyond this change's scope as originally
      planned — resolved in a follow-up session by reusing `x-field-extra-annotation`, a stock
      openapi-generator vendor extension that was already wired into `pathParams.mustache` for path
      parameters but had no render clause in `pojo.mustache` for model properties (confirmed against
      the stock `JavaSpring/pojo.mustache` in `openapi-generator-7.18.0.jar`, which does have this
      exact hook at the field-annotation position — Klabis's fork had simply never carried it over).
      Added one more `{{#vendorExtensions.x-field-extra-annotation}}@{{{.}}} {{/...}}` clause to
      `pojo.mustache`'s field-annotation chain (documented inline). `events.yaml`'s
      `RegistrationSummaryDto` schema now declares:
      - `coordinators` (added back to the schema, `type: array, items: {type: string, format: uuid}`)
        with `x-klabis-owner-id: true` + `x-field-extra-annotation:
        com.fasterxml.jackson.annotation.JsonIgnore` — renders as
        `@OwnerId @JsonIgnore List<UUID> coordinators` in the generated Java: present for
        `OwnershipResolver`, absent from the wire.
      - `registeredMemberId` (added back, `type: string, format: uuid`) with the same
        `x-field-extra-annotation: JsonIgnore` (no `x-klabis-owner-id` — it has no ownership role,
        only used by `RegistrationRecordTransactionLinkProcessor` at runtime) — renders as
        `@JsonIgnore UUID registeredMemberId`.
      - `category` gained `x-field-extra-annotation:
        com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)`,
        overriding the class-level `NON_NULL` default per-property, matching the hand-written class.
      `RegistrationSummaryDto` was added to the `events` module's `models` list in
      `build.gradle.kts`. `RegistrationDtoMapper` and `RegistrationRecordTransactionLinkProcessor`
      (and their tests) were updated for the generated record's field types (`List<UUID>` instead of
      `Set<MemberId>` for `coordinators`, `UUID` instead of `MemberId` for `registeredMemberId`,
      alphabetized component order per the bundler's key-sorting).
      All three previously-failing tests now pass:
      `EventRegistrationControllerTest$RegistrationTimePrivacyTests.eventCoordinatorSeesRegistrationTime`,
      `EventControllerTest$GetEventTests.registrationTimeVisibleForSecondCoordinator`,
      `EventRegistrationControllerTest$ListRegistrationsTests.shouldReturnNullCategoryWhenOrphaned`.
      Full `events` module suite: 803/803 passed.
- [x] 4.4b Delete `AccommodationListItemDto.java` — after its 4.4a check passed (no field-level
      security annotations on it at all, migration is clean). `RegistrationSummaryDto.java` is now
      **also deleted** — see the 4.4a follow-up above; the gap that blocked it is resolved.
- [x] 4.5 `_embedded.registrationDtoList` regression coverage already exists and passes:
      `EventControllerTest$GetEventTests` (embed/empty/registrationTime-visibility cases) and
      `EventRegistrationE2ETest` both assert `$._embedded.registrationDtoList[...]` shape after the
      migration — no new test needed.
- [x] 4.6 Ran `events` module tests via the test-runner skill: 803/803 passed (post-4.4a/4.4b
      `RegistrationSummaryDto` migration; 616/616 was the earlier partial-migration checkpoint).

## 5. Category A — `membershipfees` module

- [x] 5.1 Add `FeeSelectionCampaignResponse`, `MembershipFeeTierResponse`,
      `MembershipFeeTierSummaryResponse`, `PaymentRuleResponse`, `MemberFeeChoiceResponse`,
      `MemberFeeHistoryResponse`, `AssignmentResponse`, `MemberFeeSummaryResponse`,
      `CurrentGroupResponse`, `MembershipFeeGroupResponse` to `membershipfees`'s `models` list.
      **Correction found during migration:** the spec's nested-assignment schema is named
      `FeeAssignmentResponse`, not `AssignmentResponse` (the hand-written nested class name) — added
      under its real spec name. Also required three inline-enum-promoted schemas not listed in this
      task (`PaymentRuleResponse_ruleType`, `MembershipFeeGroupResponse_status`,
      `FeeAssignmentResponse_source`) — `resolveInlineEnums` promotes each enum property to its own
      named model, and the generator fails to compile without them on the `models` list (same
      `EventImportEntry_status` precedent as the `events` module). Also required adding
      `MemberInGroupResponse` to the `models` list — see 5.2 correction below.
- [x] 5.2 Generate, diff each against its hand-written counterpart; confirm
      `MembershipFeeGroupResponse` generates as a flat record (no `members` property — it stays a
      runtime `_embedded` addition, same as `EventDto`).
      **Correction found during migration:** the hand-written `MembershipFeeGroupResponse` has a
      nested `MemberInGroupResponse` record carrying `@Relation(collectionRelation = "members")` —
      this is Category B behavior (a non-default `@Relation`), not pure Category A as this task
      assumed. Spring HATEOAS's `HalModelBuilder.embed(...)` (called from
      `HalResponseBodyAdvice.applyEmbeddeds`) derives the `_embedded` key from this annotation, so
      it is load-bearing, not cosmetic — without it the key would fall back to the
      class-name-derived default (`memberInGroupResponseList`) instead of `members`. Added
      `x-klabis-relation: {collectionRelation: members}` to `MemberInGroupResponse` in
      `membershipfees.yaml` (pojo.mustache's render rule from Decision 2/task 3.3 already covers
      this — no template change needed) before generating. Generated Java correctly renders
      `@org.springframework.hateoas.server.core.Relation(collectionRelation = "members")`. All
      eleven generated records (nine listed classes + `FeeAssignmentResponse` under its real name +
      `MemberInGroupResponse`) are shape-equivalent to their hand-written counterparts. Enum fields
      (`PaymentRuleResponse.ruleType`, `MembershipFeeGroupResponse.status`,
      `FeeAssignmentResponse.source`/`MemberInGroupResponse.source`) generate as real enum types
      instead of the hand-written `String`, but serialize to the identical wire strings via
      `@JsonValue` — no wire-shape change.
- [x] 5.2a Field-security check (design.md Decision 0): grepped all nine hand-written classes for
      `@OwnerId`/`@OwnerVisible`/`@HasAuthority`/`@HalForms`/`@JsonIgnore` at the component level —
      **confirmed none present on any of the nine**, and the generated Java introduces none either
      (only `@RecordBuilder`/`@JsonInclude(NON_NULL)`/`@HandleAuthorizationDenied(NullDeniedHandler)`
      from `additionalModelTypeAnnotations`, applied uniformly to every generated model, matching
      the hand-written classes' behavior). PASS.
- [x] 5.3 Deleted all nine hand-written classes (via `git rm`) — after 5.2a passed. Their static
      factory methods (`.from(...)`/`.of(...)`) had no home on the generated records, so their
      mapping logic was moved to a new `MembershipFeesResponseMapper` (plain static-method final
      class, mirroring the existing `MembershipFeesRequestMapper` convention) in
      `com.klabis.membershipfees.infrastructure.restapi`. Fixed call sites in
      `MembershipFeeTierController`, `FeeSelectionCampaignController`, `MemberFeeChoiceController`,
      `MemberFeeSummaryController`, `MembershipFeeGroupController` (including the
      `MembershipFeeGroupResponse.MemberInGroupResponse` nested-type reference, now the top-level
      generated `MemberInGroupResponse`). No postprocessor referenced the deleted classes by
      constructor. `./gradlew compileJava` succeeds.
- [x] 5.4 `_embedded.members` regression coverage already exists and passes unchanged:
      `MembershipFeeGroupControllerTest$GetGroupTests.shouldReturnEmbeddedMembersWithDetails` and
      `.shouldNotEmbedMembersWhenGroupHasNoMembers` assert the full `_embedded.members[...]` shape
      (memberId/firstName/lastName/registrationNumber/source/joinedAt) and the empty-collection
      case — no new test needed.
- [x] 5.5 Ran `membershipfees` module tests via the test-runner skill: 616/616 passed.

## 6. Category B+C — `groupsFamily`, `groupsFree` modules (named item schemas already exist)

- [x] 6.1 Add `x-klabis-relation` to `FamilyGroupSummaryResponse`
      (`collectionRelation: familyGroupSummaryResponseList`), `GroupSummaryResponse`
      (`collectionRelation: groupSummaryResponseList`), `PendingInvitationResponse`
      (`collectionRelation: pendingInvitationResponseList`) in `groups.yaml`.
- [x] 6.2 Add `FamilyGroupSummaryResponse`, `GroupSummaryResponse`, `PendingInvitationResponse` to
      their modules' `models` lists; generate, diff.
- [x] 6.2a Field-security check (design.md Decision 0): none of these three carry field-level
      security annotations today — confirmed that stays true in the generated form, then deleted the
      three hand-written classes. Call sites fixed (`FamilyGroupController.toSummaryResponse`,
      `FreeGroupController.listGroups`, `PendingInvitationsController`, `InvitationModelBuilder`) —
      domain ID types generate as raw `UUID` (`.uuid()`/`.value()` needed at call sites) and generated
      record field order is alphabetized, not declaration order.

- [x] 6.3 **UNBLOCKED — resolved via Decision 3a (design.md).** The blocker recorded in the previous
      session (Category C's `fromProperty` strip-to-bare-payload rewrite is incompatible with
      `FamilyGroupController`/`FreeGroupController`'s genuine need for `EntityModel<X>` items
      carrying per-item conditional `_links`/affordances) is real, but has a different fix than "new
      architecture piece": redirect the specific `EntityModelX` envelope schemas onto the real
      generic Java type `org.springframework.hateoas.EntityModel<X>` via `schemaMappings` +
      `extraImportMappings` (paired, since `importMapping` is a plain string-keyed map and the key
      can be any string, including one with `<...>`), and add a response-level `application/json`
      sibling on `getFamilyGroup`/`getGroup` so response-level envelope detection is a no-op for
      these two responses (their own top level has no `allOf`/`_links` shape to unwrap in the first
      place). One `KlabisSpringCodegen.fromProperty` guard was needed: skip the Category C
      strip-rewrite entirely when the array item's `$ref` schema name is already a key in
      `schemaMapping()`, since otherwise the rewrite runs first and the envelope `$ref` is gone by
      the time `super.fromProperty` would consult `schemaMapping`. Verified empirically at every
      step — see design.md Decision 3a for the full mechanism and the actual generated
      `FamilyGroupResponse` source.
- [x] 6.4 `FamilyGroupResponse`, `ParentResponse`, `FamilyGroupMembershipResponse` added to
      `groupsFamily`'s `models` list; `GroupResponse`, `OwnerResponse`, `FreeGroupMembershipResponse`
      added to `groupsFree`'s `models` list (`PendingInvitationResponse` was already there from
      Phase 6.2). `EntityModelParentResponse`/`EntityModelFamilyGroupMembershipResponse` (groupsFamily)
      and `EntityModelOwnerResponse`/`EntityModelFreeGroupMembershipResponse`/
      `EntityModelPendingInvitationResponse` (groupsFree — the third one was missed in the original
      plan and found only once `GroupResponse` was actually generated and diffed) redirected via
      `mappings`/`extraImportMappings` in `build.gradle.kts`. Generated output diffed field-for-field
      against the hand-written classes: identical except `UUID id` (generated) vs. `FamilyGroupId
      id`/`FreeGroupId id` (hand-written — the hand-written classes had drifted from
      klabis-api-spec's "DTOs carry wire types" rule) and alphabetized constructor-parameter order.
      Six hand-written classes deleted (`git rm`): `FamilyGroupResponse.java`, `ParentResponse.java`,
      `FamilyGroupMembershipResponse.java`, `GroupResponse.java`, `OwnerResponse.java`,
      `FreeGroupMembershipResponse.java`. `FamilyGroupController`/`FreeGroupController` fixed:
      `group.getId()` → `group.getId().uuid()` in both `toFamilyGroupResponse`/`toGroupResponse`, and
      positional constructor arguments reordered to match each generated record's alphabetized field
      order (`FamilyGroupMembershipResponse(joinedAt, memberId)`,
      `FreeGroupMembershipResponse(joinedAt, memberId)`,
      `FamilyGroupResponse(id, members, name, parents)`,
      `GroupResponse(id, members, name, owners, pendingInvitations)`) — per-item
      `EntityModel.of(...)`/`klabisLinkTo`/`klabisAfford` construction logic itself is untouched.
      `./gradlew compileJava` succeeds.
- [x] 6.4a Field-security check (design.md Decision 0): grepped all six hand-written classes for
      `@OwnerId`/`@OwnerVisible`/`@HasAuthority`/`@HalForms`/`@JsonIgnore` at the component level —
      confirmed none present, matching design.md's prediction. Generated Java introduces none either
      beyond the uniform `additionalModelTypeAnnotations` (`@RecordBuilder`/`@JsonInclude(NON_NULL)`/
      `@HandleAuthorizationDenied(NullDeniedHandler)`), same as every other generated model. PASS.
- [x] 6.4b The six hand-written classes deleted — see 6.4 (done together with the models-list wiring
      in this session, since the field-security check in 6.4a is a pure confirmation of an already
      well-known absence, not new information requiring a separate gate).
- [x] 6.5 Re-ran `groupsFamily`, `groupsFree` module tests via the test-runner skill after the
      Category C wiring in 6.3/6.4 above (superseding the earlier Category-B-only checkpoint):
      616/616 passed. `./gradlew compileJava` (full backend) and `./gradlew :buildSrc:test` (36/36)
      also green.

## 7. Category B+C — `groupsTraining` module (needs inline-to-named schema promotion)

- [x] 7.1 Promoted `EntityModelTrainerResponse`'s inline `{memberId}` half to a named `TrainerResponse`
      schema in `groups.yaml`; same for `EntityModelGroupMembershipResponse`'s `{memberId,
      joinedAt}` half → `GroupMembershipResponse`.
- [x] 7.2 Added `x-klabis-relation` to `TrainingGroupSummaryResponse`
      (`collectionRelation: trainingGroupSummaryResponseList`); added it to `groupsTraining`'s
      `models` list; generated, diffed — shape-equivalent (generated fields boxed `Integer` vs.
      hand-written primitive `int`, cosmetic only). Deleted hand-written class.
- [x] 7.3 Added `TrainingGroupResponse`, `TrainerResponse`, `GroupMembershipResponse`,
      `AgeRangeResponse` to `groupsTraining`'s `models` list. **Used Decision 3a (not Decision 3's
      original default), matching groupsFamily/groupsFree**: `TrainingGroupController` builds each
      trainer/member item as `EntityModel.of(new TrainerResponse(...))` on purpose (conditional
      per-item `member`/`self`+DELETE affordances), so the array items had to stay
      `EntityModel<X>`, not bare `List<X>`. Added an `application/json` content sibling on
      `getTrainingGroup`'s 200 response pointing at bare `TrainingGroupResponse`, plus
      `mappings`/`extraImportMappings` in `build.gradle.kts` redirecting
      `EntityModelTrainerResponse`/`EntityModelGroupMembershipResponse` onto
      `EntityModel<TrainerResponse>`/`EntityModel<GroupMembershipResponse>`. Generated output
      confirmed (read the actual `.java`, not assumed): `trainers` resolves to
      `List<EntityModel<TrainerResponse>>`, `members` to
      `List<EntityModel<GroupMembershipResponse>>`, and `ageRange` resolves as a plain
      `AgeRangeResponse` field (Category A — confirmed no array unwrap needed for it, and
      `isMappedEnvelopeItem` is not confused by a non-array `$ref` property).
- [x] 7.3a Field-security check (design.md Decision 0): grepped all five hand-written classes for
      `@OwnerId`/`@OwnerVisible`/`@HasAuthority`/`@HalForms`/`@JsonIgnore` at the component level —
      confirmed none present. Generated Java introduces none either beyond the uniform
      `additionalModelTypeAnnotations`. PASS.
- [x] 7.4 Deleted the five hand-written classes (`git rm`) — after 7.3a passed. `TrainingGroupController`
      fixed: `group.getId()` → `group.getId().uuid()` in `toTrainingGroupResponse`/
      `buildLimitedGroupResponse`/`toSummaryResponse`; positional constructor arguments reordered to
      match each generated record's alphabetized field order
      (`AgeRangeResponse(maxAge, minAge)`, `GroupMembershipResponse(joinedAt, memberId)`,
      `TrainingGroupResponse(ageRange, id, members, name, trainers)`,
      `TrainingGroupSummaryResponse(id, maxAge, memberCount, minAge, name)`) — per-item
      `EntityModel.of(...)`/`klabisLinkTo`/`klabisAfford` construction logic itself untouched.
      `./gradlew compileJava` (full backend) succeeds.
- [x] 7.5 Ran `groupsTraining`-related tests (`TrainingGroupManagementServiceTest`, `AgeRangeTest`,
      `TrainingGroupTest`, `TrainingGroupPersistenceTest`, `TrainingGroupControllerTest`) via the
      test-runner skill: 616/616 passed.

## 8. Closeout

- [ ] 8.1 Run the full backend test suite (unit + Modulith integration) via the test-runner agent.
- [ ] 8.2 Grep `backend/src/main/java` for any remaining hand-written class in an
      `infrastructure.restapi` package that duplicates a generated model name — confirm zero
      matches.
- [ ] 8.3 Code review (per project convention) before committing: verify no `@Schema`/`@Relation`/
      `x-klabis-*` semantic drift between the deleted hand-written classes and their generated
      replacements, and that `docs/openapi/klabis-full.json` was regenerated and committed alongside
      the spec changes.
- [ ] 8.4 Final field-security audit (design.md Decision 0): re-diff the generated sources for
      `MemberSummaryResponse`, `EventSummaryDto`, `RegistrationSummaryDto` — the three classes
      confirmed to carry `@HasAuthority`/`@OwnerVisible`/`@OwnerId`/`@JsonIgnore` — against the git
      history of the deleted hand-written files (`git show <sha>:<path>` for each), field by field,
      as an independent double-check before this change is considered done. This is the one
      irreversible-if-missed step in the whole migration: a dropped authorization annotation ships
      silently, with no failing test to catch it.
