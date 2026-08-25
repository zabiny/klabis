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

- [ ] 4.1 Add `EventDto`, `RankingDto`, `EntryFeeDto`, `EventCategoryDto`, `EventSummaryDto`,
      `CategoryPresetDto` to `events`'s `models` list; generate, diff each against its hand-written
      counterpart (note `RankingDto`/`EntryFeeDto`/`EventCategoryDto` generate as top-level records,
      not nested inside `EventDto` — this is expected, see design.md Decision 1).
- [ ] 4.2 Fix every reference to the nested form (`EventDto.RankingDto` etc.) across `events`
      module source to the top-level generated name.
- [ ] 4.2a Field-security check (design.md Decision 0): `EventDto`/`EventSummaryDto` both mark
      `id`/`status`/`cancellationReason`/`deadlines` `@HalForms(access = READ_ONLY)`, and
      `EventSummaryDto.status` additionally carries `@HasAuthority(Authority.EVENTS_MANAGE)` +
      `@HandleAuthorizationDenied` — confirm `events.yaml`'s `EventDto`/`EventSummaryDto` schemas
      carry `x-klabis-halforms-access: READ_ONLY` on all four properties and
      `x-klabis-authority: EVENTS_MANAGE` on `EventSummaryDto.status`, and that the generated Java
      renders both.
- [ ] 4.3 Delete `EventDto.java`, `EventSummaryDto.java`, `CategoryPresetDto.java` — only after 4.2a
      passes.
- [ ] 4.4 Add `x-klabis-relation` to `RegistrationSummaryDto`
      (`collectionRelation: registrationDtoList`) and `AccommodationListItemDto`
      (`collectionRelation: accommodationList`) in `events.yaml`; add both to `events`'s `models`
      list; generate, diff.
- [ ] 4.4a Field-security check (design.md Decision 0): `RegistrationSummaryDto.registrationTime`
      carries `@OwnerVisible` + `@HasAuthority(Authority.EVENTS_REGISTRATIONS)`;
      `.coordinators`/`.registeredMemberId` carry `@JsonIgnore` (never serialized) and
      `.coordinators` also carries `@OwnerId`. Confirm `x-klabis-owner-visible: true` +
      `x-klabis-authority: EVENTS_REGISTRATIONS` on `registrationTime`, `x-klabis-owner-id: true` on
      `coordinators`, and that both `coordinators`/`registeredMemberId` are either absent from the
      spec response schema or otherwise never rendered into the generated wire payload — a
      `@JsonIgnore`'d field silently reappearing on the wire is exactly the kind of regression this
      check exists to catch.
- [ ] 4.4b Delete `RegistrationSummaryDto.java`, `AccommodationListItemDto.java` — only after 4.4a
      passes.
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
- [ ] 5.2a Field-security check (design.md Decision 0): grep all nine hand-written classes for
      `@OwnerId`/`@OwnerVisible`/`@HasAuthority`/`@HalForms`/`@JsonIgnore` at the component level
      (none expected on any of the nine as of this exploration — `membershipfees` response DTOs
      carry no field-level security today) and confirm the generated spec/Java doesn't silently
      introduce or drop any such annotation relative to the hand-written source.
- [ ] 5.3 Delete all nine hand-written classes — only after 5.2a passes; fix imports in
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
      their modules' `models` lists; generate, diff.
- [ ] 6.2a Field-security check (design.md Decision 0): none of these three carry field-level
      security annotations today — confirm that stays true in the generated form, then delete the
      three hand-written classes.
- [ ] 6.3 Implement Category C's property-level unwrap in `KlabisSpringCodegen` using the hook
      confirmed in Phase 1; unit-test it against `FamilyGroupResponse.parents`/`.members` and
      `GroupResponse.owners`/`.members`/`.pendingInvitations` fixtures (all already reference named
      item schemas — `OwnerResponse`, `ParentResponse`, `FreeGroupMembershipResponse`,
      `FamilyGroupMembershipResponse` — no spec promotion needed for this phase).
- [ ] 6.4 Add `FamilyGroupResponse`, `ParentResponse`, `FamilyGroupMembershipResponse`,
      `GroupResponse`, `OwnerResponse`, `FreeGroupMembershipResponse` to the two modules' `models`
      lists; generate, diff each property's resolved type is `List<Payload>` (never
      `List<EntityModel<Payload>>`).
- [ ] 6.4a Field-security check (design.md Decision 0): none of these six carry field-level security
      annotations today — confirm that stays true in the generated form.
- [ ] 6.4b Delete the six hand-written classes — only after 6.4a passes; fix imports in
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
- [ ] 7.3a Field-security check (design.md Decision 0): none of these four carry field-level
      security annotations today — confirm that stays true in the generated form.
- [ ] 7.4 Delete the five hand-written classes — only after 7.3a passes; fix imports in
      `TrainingGroupController` and its postprocessors (per-item
      `EntityModel.of(new TrainerResponse(...))` calls stay — only the payload type is now
      generated).
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
- [ ] 8.4 Final field-security audit (design.md Decision 0): re-diff the generated sources for
      `MemberSummaryResponse`, `EventSummaryDto`, `RegistrationSummaryDto` — the three classes
      confirmed to carry `@HasAuthority`/`@OwnerVisible`/`@OwnerId`/`@JsonIgnore` — against the git
      history of the deleted hand-written files (`git show <sha>:<path>` for each), field by field,
      as an independent double-check before this change is considered done. This is the one
      irreversible-if-missed step in the whole migration: a dropped authorization annotation ships
      silently, with no failing test to catch it.
