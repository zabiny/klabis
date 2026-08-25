## 1. Spike: property-level unwrap hook (blocks Phase 6 only)

- [ ] 1.1 Read `openapi-generator-7.18.0-sources.jar` for `DefaultCodegen.fromProperty(...)` and
      `postProcessModelProperty(...)` — confirm call order, whether the property's `Schema` can be
      rewritten before `super`'s own type/import resolution runs, and whether it receives the
      `schemas` map needed to resolve a `$ref`.
- [ ] 1.2 Build a throwaway fixture schema (array of `$ref` to `allOf[inline-payload, {_links}]`,
      mirroring `EntityModelTrainerResponse`) and a unit test proving the chosen hook rewrites it to
      `List<Payload>` with no `EntityModel` in the resolved type.
- [ ] 1.3 Widen `HalEnvelopeDetector.detectShape1` to package-private (or extract its structural
      check into a method reusable from the new property-level path) so both call sites share one
      rule instead of duplicating it.

## 2. Category A — `calendar` module (smallest, proves the recipe)

- [ ] 2.1 Add `CalendarItemDto`, `IcalTokenResponse` to the `calendar` module's `models` list in
      `backend/build.gradle.kts`.
- [ ] 2.2 Generate, diff the generated records against `CalendarItemDto.java` and the nested
      `IcalTokenResponse` in `IcalTokenController.java` (field names/types, `@HalForms` ↔
      `x-klabis-halforms-access` parity).
- [ ] 2.3 Delete both hand-written declarations; confirm `CalendarController`,
      `IcalTokenController`, and their postprocessors compile unchanged (same package, same simple
      name).
- [ ] 2.4 Run `calendar` module's backend tests via the test-runner agent — no assertion changes
      expected.

## 3. Category A — `common`, `finance`, `members` modules

- [ ] 3.1 Add `PermissionsResponse` to `common`'s `models` list; generate, diff, delete
      hand-written class, fix `PermissionController` import if needed.
- [ ] 3.2 Add `MemberAccountResource` to `finance`'s `models` list; move its `fromBalance(...)`
      mapping logic out of the record into a MapStruct mapper or a plain static helper in
      `MemberAccountController`; generate, diff, delete hand-written class.
- [ ] 3.3 Add `x-klabis-relation: {collectionRelation: transactions, itemRelation: transaction}` to
      `TransactionResource` in `finance.yaml`; extend `pojo.mustache` with the `x-klabis-relation`
      render rule (Decision 2) — first real use, verify the annotation compiles and the `_embedded`
      key is unchanged.
- [ ] 3.4 Add `TransactionResource` to `finance`'s `models` list; move its `from(...)` mapping logic
      to a mapper; generate, diff, delete hand-written class.
- [ ] 3.5 Add `MemberSummaryResponse`, `MemberOptionResponse` to `members`'s `models` list;
      generate, diff, delete hand-written classes.
- [ ] 3.6 Add `SuspensionBlockedWarning`, `OutstandingDebtWarning`, `LastOwnerWarning`,
      `AffectedGroup` to `members`'s `models` list; correct the stale comment in `members.yaml`
      (lines ~815-816) that still references the removed `schemaMapping` mechanism; generate, diff,
      delete hand-written classes (including the nested ones in `MembersExceptionHandler`).
- [ ] 3.7 Run `common`, `finance`, `members` module tests via the test-runner agent.

## 4. Category A+B — `events` module

- [ ] 4.1 Add `EventDto`, `RankingDto`, `EntryFeeDto`, `EventCategoryDto`, `EventSummaryDto`,
      `CategoryPresetDto` to `events`'s `models` list; generate, diff each against its hand-written
      counterpart (note `RankingDto`/`EntryFeeDto`/`EventCategoryDto` generate as top-level records,
      not nested inside `EventDto` — this is expected, see design.md Decision 1).
- [ ] 4.2 Fix every reference to the nested form (`EventDto.RankingDto` etc.) across `events`
      module source to the top-level generated name.
- [ ] 4.3 Delete `EventDto.java`, `EventSummaryDto.java`, `CategoryPresetDto.java`.
- [ ] 4.4 Add `x-klabis-relation` to `RegistrationSummaryDto`
      (`collectionRelation: registrationDtoList`) and `AccommodationListItemDto`
      (`collectionRelation: accommodationList`) in `events.yaml`; add both to `events`'s `models`
      list; generate, diff, delete hand-written classes.
- [ ] 4.5 Add an integration test (or confirm an existing one covers) `GET /api/events/{id}` still
      returns `_embedded.registrationDtoList` unchanged — the runtime-embed path itself is
      untouched by this change, but `EventDto` moving to generated code is the highest-risk point
      for a silent regression there.
- [ ] 4.6 Run `events` module tests via the test-runner agent.

## 5. Category A — `membershipfees` module

- [ ] 5.1 Add `FeeSelectionCampaignResponse`, `MembershipFeeTierResponse`,
      `MembershipFeeTierSummaryResponse`, `PaymentRuleResponse`, `MemberFeeChoiceResponse`,
      `MemberFeeHistoryResponse`, `AssignmentResponse`, `MemberFeeSummaryResponse`,
      `CurrentGroupResponse`, `MembershipFeeGroupResponse` to `membershipfees`'s `models` list.
- [ ] 5.2 Generate, diff each against its hand-written counterpart; confirm
      `MembershipFeeGroupResponse` generates as a flat record (no `members` property — it stays a
      runtime `_embedded` addition, same as `EventDto`).
- [ ] 5.3 Delete all nine hand-written classes; fix imports in
      `MembershipFeeTierController`/`MembershipFeeGroupController`/`FeeSelectionCampaignController`/
      `MemberFeeChoiceController`/`MemberFeeSummaryController` and their mappers/postprocessors.
- [ ] 5.4 Add/confirm an integration test that `GET /api/membership-fee-groups/{id}` still returns
      `_embedded.members` unchanged.
- [ ] 5.5 Run `membershipfees` module tests via the test-runner agent.

## 6. Category B+C — `groupsFamily`, `groupsFree` modules (named item schemas already exist)

- [ ] 6.1 Add `x-klabis-relation` to `FamilyGroupSummaryResponse`
      (`collectionRelation: familyGroupSummaryResponseList`), `GroupSummaryResponse`
      (`collectionRelation: groupSummaryResponseList`), `PendingInvitationResponse`
      (`collectionRelation: pendingInvitationResponseList`) in `groups.yaml`.
- [ ] 6.2 Add `FamilyGroupSummaryResponse`, `GroupSummaryResponse`, `PendingInvitationResponse` to
      their modules' `models` lists; generate, diff, delete hand-written classes.
- [ ] 6.3 Implement Category C's property-level unwrap in `KlabisSpringCodegen` using the hook
      confirmed in Phase 1; unit-test it against `FamilyGroupResponse.parents`/`.members` and
      `GroupResponse.owners`/`.members`/`.pendingInvitations` fixtures (all already reference named
      item schemas — `OwnerResponse`, `ParentResponse`, `FreeGroupMembershipResponse`,
      `FamilyGroupMembershipResponse` — no spec promotion needed for this phase).
- [ ] 6.4 Add `FamilyGroupResponse`, `ParentResponse`, `FamilyGroupMembershipResponse`,
      `GroupResponse`, `OwnerResponse`, `FreeGroupMembershipResponse` to the two modules' `models`
      lists; generate, diff each property's resolved type is `List<Payload>` (never
      `List<EntityModel<Payload>>`); delete the six hand-written classes; fix imports in
      `FamilyGroupController`/`FreeGroupController`/`PendingInvitationsController` and their
      postprocessors/model builders (`InvitationModelBuilder` stays — it still constructs
      `EntityModel<PendingInvitationResponse>` at runtime, only the payload type is now generated).
- [ ] 6.5 Run `groupsFamily`, `groupsFree` module tests via the test-runner agent.

## 7. Category B+C — `groupsTraining` module (needs inline-to-named schema promotion)

- [ ] 7.1 Promote `EntityModelTrainerResponse`'s inline `{memberId}` half to a named `TrainerResponse`
      schema in `groups.yaml`; same for `EntityModelGroupMembershipResponse`'s `{memberId,
      joinedAt}` half → `GroupMembershipResponse`.
- [ ] 7.2 Add `x-klabis-relation` to `TrainingGroupSummaryResponse`
      (`collectionRelation: trainingGroupSummaryResponseList`); add it to `groupsTraining`'s
      `models` list; generate, diff, delete hand-written class.
- [ ] 7.3 Add `TrainingGroupResponse`, `TrainerResponse`, `GroupMembershipResponse`,
      `AgeRangeResponse` to `groupsTraining`'s `models` list; generate; confirm `trainers`/`members`
      resolve to `List<TrainerResponse>`/`List<GroupMembershipResponse>` and `ageRange` resolves as
      a plain `$ref` property (Category A, confirmed no array unwrap needed for it).
- [ ] 7.4 Delete the five hand-written classes; fix imports in `TrainingGroupController` and its
      postprocessors (per-item `EntityModel.of(new TrainerResponse(...))` calls stay — only the
      payload type is now generated).
- [ ] 7.5 Run `groupsTraining` module tests via the test-runner agent.

## 8. Closeout

- [ ] 8.1 Run the full backend test suite (unit + Modulith integration) via the test-runner agent.
- [ ] 8.2 Grep `backend/src/main/java` for any remaining hand-written class in an
      `infrastructure.restapi` package that duplicates a generated model name — confirm zero
      matches.
- [ ] 8.3 Code review (per project convention) before committing: verify no `@Schema`/`@Relation`/
      `x-klabis-*` semantic drift between the deleted hand-written classes and their generated
      replacements, and that `docs/openapi/klabis-full.json` was regenerated and committed alongside
      the spec changes.
