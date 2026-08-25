## Context

`custom-openapi-codegen` (`openspec/changes/archive/2026-08-25-custom-openapi-codegen/design.md`)
introduced `KlabisSpringCodegen extends SpringCodegen` and `HalEnvelopeDetector`
(`backend/buildSrc/src/main/java/com/klabis/openapi/codegen/`), which resolve HAL envelope
schemas to their payload type structurally, at the level of an **operation's return type**:

- **Shape 1** — `allOf[$ref, {_links,_templates,_embedded}]` → unwraps to the `$ref` payload.
- **Shape 2** — `{_links, _embedded: {exactly one array-of-$ref property}}` → unwraps to
  `List<Payload>`/`Page<Payload>` (container decided by `x-spring-paginated` on the operation, not
  by the envelope's own `page` property).

That migration deliberately left every module's `models` list unchanged (design.md's Decision 5:
tag-scoped model discovery was investigated and withdrawn — `DefaultGenerator` reads `models` in a
private method outside any `CodegenConfig` hook, so there is nothing to override). Only request
DTOs are on any `models` list today. `pojo.mustache` already renders field-level annotations from
`x-klabis-owner-id` / `x-klabis-owner-visible` / `x-klabis-authority` / `x-klabis-halforms-access` /
`x-klabis-not-blank` / `x-klabis-past` / `x-klabis-url` / `x-klabis-class-constraint`, and
`additionalModelTypeAnnotations` (`OpenApiModule.kt`) already stamps `@RecordBuilder`,
`@JsonInclude(NON_NULL)`, `@HandleAuthorizationDenied(NullDeniedHandler.class)` onto every
generated model in a module, free of charge.

33 response DTO classes across 8 modules remain hand-written. Reading each one against its spec
schema and the two shape detectors above sorts them into three groups, not one:

**Category A — nothing blocks generation.** The class is a flat record (or a nested one whose
outer schema is already named in the spec), carries no `@Relation`, and is simply absent from its
module's `models` list. Confirmed examples: `EventDto` (+ `RankingDto`, `EntryFeeDto`,
`EventCategoryDto` — already named schemas in `events.yaml` with correct
`x-klabis-halforms-access`), `EventSummaryDto`, `CategoryPresetDto`, `CalendarItemDto`,
`IcalTokenResponse`, `PermissionsResponse`, `MemberSummaryResponse`, `MemberOptionResponse`,
`SuspensionBlockedWarning` (+ `OutstandingDebtWarning`, `LastOwnerWarning`, `AffectedGroup`),
`MemberAccountResource`, `FeeSelectionCampaignResponse`, `MembershipFeeTierResponse`,
`MembershipFeeTierSummaryResponse`, `PaymentRuleResponse`, `MemberFeeChoiceResponse`,
`MemberFeeHistoryResponse` (+ `AssignmentResponse`), `MemberFeeSummaryResponse` (+
`CurrentGroupResponse`), and — corrected during this exploration — **`MembershipFeeGroupResponse`**.
Its `members` collection is not a Java property at all: `getFeeGroup` embeds it at runtime via
`HalResponseContext.embed(...)`, assembled by `HalResponseBodyAdvice` under `_embedded.members`,
exactly like `EventDto`'s `registrationDtoList`. The schema itself
(`membershipfees.yaml` `MembershipFeeGroupResponse`, line ~1629) is a flat object with `memberCount`,
not `members`. No Category C work is needed for it.

**Category B — generator has no `@Relation` render path.** `pojo.mustache` has no hook for Spring
HATEOAS's `@Relation(collectionRelation=..., itemRelation=...)`, which several response DTOs carry
to name their `_embedded` collection key. The spec already documents, in comments, which class
needs which relation name (`groups.yaml:41-47`, `finance.yaml:478`, `membershipfees.yaml:718,1608`,
`events.yaml:1747`) — this category formalizes that comment into a real vendor extension.
Confirmed examples: `RegistrationSummaryDto`, `AccommodationListItemDto`, `TransactionResource`,
`GroupSummaryResponse`, `PendingInvitationResponse`, `FamilyGroupSummaryResponse`,
`TrainingGroupSummaryResponse`.

**Category C — generator has no property-level HAL unwrap.** `HalEnvelopeDetector` only inspects
the schema selected as an *operation's response*. Several response DTOs have a plain top-level
property (not the whole response) shaped as `array of $ref to allOf[payload, {_links}]` —
confirmed on `TrainingGroupResponse.trainers`/`.members` (`groups.yaml:1745-1752`), where
`EntityModelTrainerResponse` = `allOf[{type:object, properties:{memberId}}, {type:object,
properties:{_links}}]` with an **inline** (unnamed) payload half. No detector today reaches
inside a property's schema at all. Confirmed examples: `TrainingGroupResponse` (+ `TrainerResponse`,
`GroupMembershipResponse`), `FamilyGroupResponse` (+ `ParentResponse`,
`FamilyGroupMembershipResponse`), `GroupResponse` (+ `OwnerResponse`,
`FreeGroupMembershipResponse`). `AgeRangeResponse` is a plain `$ref` property (not an array) and
sorts into Category A instead.

The real runtime shape (verified against a live `GET /api/training-groups/{id}` response) has
`trainers`/`members` as **top-level array properties** on the object, each item independently
carrying its own `_links` — not nested under `_embedded`. This is explicitly not the same shape
`_embedded` is for.

## Goals / Non-Goals

**Goals:**
- Every response DTO in an `infrastructure.restapi` package is generated from the spec, the same
  way every request DTO already is.
- Category A: add missing `models` entries only; delete the hand-written class once the generated
  one is shape-equivalent.
- Category B: introduce `x-klabis-relation` and its `pojo.mustache` render rule.
- Category C: extend `KlabisSpringCodegen`/`HalEnvelopeDetector` to unwrap a **property**'s schema,
  not just an operation's response schema, producing a bare `List<Payload>` — never
  `List<EntityModel<Payload>>`. The generator's job stops at the plain Java type; HAL `_links` per
  item is added at request time by the existing `RepresentationModelProcessor` postprocessors
  (`FreeGroupController.buildOwnerModel`, `TrainingGroupController`'s per-item `EntityModel.of(...)`
  calls, etc.), which are themselves untouched by this change.
- Promote the inline payload schemas Category C uncovers (e.g. `EntityModelTrainerResponse`'s
  `{memberId}` half) to named spec schemas, so there is something for the generator to name and
  reuse.
- Zero change to any endpoint's wire contract, status code, `_links`/`_embedded` structure, or
  authorization behavior.

**Non-Goals:**
- Runtime `_embedded` composition (`HalResponseContext.embed`) is unaffected — `EventDto` and
  `MembershipFeeGroupResponse` need no codegen changes for it; it stays exactly as
  `custom-openapi-codegen` left it (out of scope there too).
- No change to `models`/`apis` global-property mechanics — `custom-openapi-codegen` Decision 5
  already established there is no generator extension point for automatic model discovery. Every
  module's `models` list stays an explicit enumeration.
- No change to the frontend TypeScript codegen or the published API contract.
- Not attempting a generic, reusable-outside-Klabis HAL-property unwrap. Scoped to Klabis's own
  `_links`-per-item convention, same framing as the original design.

## Decisions

### Decision 1 (Category A): treat as a `models`-list gap, not a codegen gap

No `KlabisSpringCodegen`/template change. Per class: add to the module's `models` list in
`build.gradle.kts`, run the generate task, diff the generated record against the hand-written one
(field names, types, `x-klabis-*`-derived annotations), fix any spec gap (e.g. a property missing
`x-klabis-halforms-access` that the hand-written version had as `@HalForms`), delete the
hand-written class, update its (unchanged) import in every controller/mapper/postprocessor that
referenced it.

**Nested classes** (`EventDto.RankingDto`/`EntryFeeDto`/`EventCategoryDto`,
`SuspensionBlockedWarning`'s `OutstandingDebtWarning`/`LastOwnerWarning`/`AffectedGroup`,
`MemberFeeHistoryResponse.AssignmentResponse`, `MemberFeeSummaryResponse.CurrentGroupResponse`) are
already named top-level schemas in the spec (never inline), so the generator emits them as
top-level Java records, not nested ones. This is a structural difference from the hand-written form
(nested `record RankingDto(...)` inside `EventDto` vs. a sibling top-level `RankingDto` record) but
not a behavioral one — same package, same field shape, same JSON. Call sites referencing the
nested form (`EventDto.RankingDto`) need a mechanical import fix to the top-level name
(`RankingDto`).

### Decision 2 (Category B): `x-klabis-relation` schema extension

```yaml
GroupSummaryResponse:
  type: object
  x-klabis-relation:
    collectionRelation: groupSummaryResponseList
  properties: ...
```

`pojo.mustache` gains one more annotation clause, alongside the existing `x-klabis-class-constraint`
block (which is also schema-level, not property-level — same rendering position applies):

```mustache
{{#vendorExtensions.x-klabis-relation}}@org.springframework.hateoas.server.core.Relation(collectionRelation = "{{collectionRelation}}"{{#itemRelation}}, itemRelation = "{{itemRelation}}"{{/itemRelation}})
{{/vendorExtensions.x-klabis-relation}}
```

Declared on **every** Category B schema, even where the desired relation name equals Spring
HATEOAS's own default (`decapitalize(ClassName) + "List"`) — e.g. `GroupSummaryResponse` and
`PendingInvitationResponse`, which today carry an explicit `@Relation` whose value already matches
the default. Relying on "the default happens to match" is a silent trap the moment the class is
renamed; an explicit `x-klabis-relation` makes the `_embedded` key a spec-visible contract instead
of an accident of Java naming, and costs one YAML block per schema.

### Decision 3 (Category C): property-level unwrap, no `EntityModel<T>` in generated Java

**Extension point.** `HalEnvelopeDetector.detect(schema, schemas)` is a pure function already
independent of any per-operation state — it takes a `Schema` and the document's schema map. It
already composes with itself for Shape 2's array items (detecting a nested Shape 1 inside a Shape 2
collection). The new path reuses the *same* `detect(...)` entry point against a **property**
schema, invoked from a new override rather than from `handleMethodResponse`/`fromResponse`
(which only ever see an operation's response schema).

`DefaultCodegen.fromProperty(String, Schema, boolean, boolean)` is the stock generator's per-model,
per-property resolution method — the direct analogue of `handleMethodResponse` for a response,
and (per `SpringCodegen`'s existing overrides of unrelated `fromProperty` behavior elsewhere in the
stock generator) a supported, `protected`/overridable extension point. `KlabisSpringCodegen`
overrides it the same way it overrides `handleMethodResponse`: detect an unwrap target on the raw
property schema, rewrite the schema to a bare `array of Payload` when found, delegate to `super`.
This needs verification against the `openapi-generator-7.18.0-sources.jar` (mirroring how
`custom-openapi-codegen` verified `handleMethodResponse`'s exact call site and timing) before
implementation — see Open Questions.

**Detected shape** (new, not Shape 1/2 — call it **Shape 1-item** since it is Shape 1's own
`allOf[payload, {_links,...}]` pattern, just reached from a property's `items` instead of a
response's top level):

```
property.type == "array", AND
property.items is a $ref, AND
the $ref resolves to: allOf with exactly 2 members, member[0] a $ref OR inline object of
  domain properties, member[1] inline object whose properties ⊆ {_links, _templates, _embedded}
  → generate property as `List<Payload>`, where Payload is member[0] resolved/named
```

This is deliberately *not* a new top-level "Shape 3" for whole responses — Category C never
touches an operation's return type, only a model's own property list. `GroupResponse`,
`FamilyGroupResponse`, `TrainingGroupResponse` themselves are ordinary flat models once each
`List<EntityModel<X>>`-shaped property inside them resolves to `List<X>`; no operation-level
override was needed for `getGroup`/`getFamilyGroup`/`getTrainingGroup` beyond what already exists
(their envelope is already Shape 1, already unwrapped).

**Spec changes required first:** promote each inline payload half to a named schema, matching the
convention already used elsewhere in `groups.yaml` (`OwnerResponse`, `ParentResponse`,
`FreeGroupMembershipResponse`, `PendingInvitationResponse` are *already* named — only the
`traininggroup` package's `EntityModelTrainerResponse`/`EntityModelGroupMembershipResponse` still
inline their payload half). Example:

```yaml
# before
EntityModelTrainerResponse:
  allOf:
    - type: object
      properties:
        memberId: {type: string, format: uuid}
    - type: object
      properties:
        _links: {$ref: './_shared/hal.yaml#/components/schemas/Links'}

# after
EntityModelTrainerResponse:
  allOf:
    - $ref: '#/components/schemas/TrainerResponse'
    - type: object
      properties:
        _links: {$ref: './_shared/hal.yaml#/components/schemas/Links'}

TrainerResponse:
  type: object
  properties:
    memberId: {type: string, format: uuid}
```

**Why the generator must not emit `List<EntityModel<Payload>>`:** confirmed with the user during
exploration — the generator's job is the plain wire payload; HAL linking is exclusively Spring
HATEOAS's runtime concern (`RepresentationModelProcessor`, `EntityModel.of(...)` calls already
present in every affected controller). Emitting `EntityModel<T>` as a generated field type would
require the generator to know about `org.springframework.hateoas.EntityModel` as a model-property
container — a capability the stock generator has no notion of and this design does not introduce.

## Risks / Trade-offs

- **[Risk] `fromProperty`'s exact signature/timing across `openapi-generator` versions is
  unverified as of this design** — `custom-openapi-codegen` needed to read generator sources to
  pin down `handleMethodResponse`'s exact behavior; the same investigation is needed for
  `fromProperty` before Category C is implementable. → Mitigation: spike task in Phase 3 (below)
  confirms the hook before any module's spec is touched; if `fromProperty` turns out unsuitable,
  the fallback is a `postProcessModelProperty` hook (also stock, coarser-grained, called after
  `fromProperty` per generated model) — evaluate both, prefer whichever needs the least
  re-derivation of stock behavior, same principle as Decision 1 in the archived design.
- **[Risk] Shape 1-item detection is a second copy of Shape 1's structural rule**, now applied in
  two independent call sites (response-level via `detect`, property-level via the new override).
  → Mitigation: implement Category C's detection as a thin wrapper around the *existing*
  `HalEnvelopeDetector.detectShape1` (already a private static method — widen its visibility
  rather than duplicating its `allOf`/`{_links,_templates,_embedded}` check), so the two paths
  cannot drift independently.
- **[Risk] Deleting a hand-written class before its generated replacement is verified compiles
  breaks the build for every downstream reference simultaneously** (33 classes, most referenced
  from ≥2 files: controller + postprocessor + mapper). → Mitigation: per-class migration order is
  add-to-`models` → generate → diff → delete-hand-written → fix imports → run module's tests,
  never batched; matches `custom-openapi-codegen`'s own per-module parity-check discipline.
- **[Trade-off] Category A's nested-to-top-level record promotion (`EventDto.RankingDto` →
  `RankingDto`) is a small but real Java API shape change for any code (including tests) importing
  the nested name.** Accepted: `RankingDto`/`EntryFeeDto`/`EventCategoryDto` are
  `infrastructure.restapi`-internal types with no external consumer; grep confirms all references
  are within the `events` module's own source tree.
- **[Trade-off] `x-klabis-relation` declared unconditionally (Decision 2) is one more line per
  Category B schema even where Spring HATEOAS's default would already match.** Accepted per
  Decision 2's rationale — explicitness over an accidental-default trap.

## Migration Plan

Vertical slices, one module end-to-end per phase (per this project's refactoring convention:
prefer one feature/module fully done over one mechanism applied everywhere) — each phase
independently committable and testable, matching `custom-openapi-codegen`'s own per-module parity
discipline:

1. **Spike: confirm the `fromProperty`/`postProcessModelProperty` hook** against
   `openapi-generator-7.18.0-sources.jar` (Category C only). No spec/module change yet — a
   throwaway fixture schema is enough to prove the override fires and rewrites the property's type
   before `super` resolves it. Blocks Phase 5 only; Phases 2–4 (Category A/B modules) do not depend
   on it.
2. **`calendar`** (Category A only: `CalendarItemDto`, `IcalTokenResponse`) — smallest module,
   proves the Category A recipe end-to-end (add to `models`, generate, diff, delete, fix imports,
   test) before touching anything larger.
3. **`common`, `finance`, `members`** (Category A: `PermissionsResponse`, `MemberAccountResource`,
   `MemberSummaryResponse`, `MemberOptionResponse`, `SuspensionBlockedWarning` + its 3 nested
   schemas). `finance` also carries one Category B class (`TransactionResource`) — do it in the
   same phase since the module is small and it is already mid-migration.
4. **`events`** (Category A: `EventDto`+3 nested, `EventSummaryDto`, `CategoryPresetDto`; Category
   B: `RegistrationSummaryDto`, `AccommodationListItemDto`). Largest Category A/B module; the
   `EventDto`/`_embedded.registrationDtoList` runtime-embed interaction is the one place worth an
   explicit regression test (`GET /api/events/{id}` still returns `_embedded.registrationDtoList`
   unchanged) even though no code in the embed path itself changes.
5. **`membershipfees`** (Category A: `FeeSelectionCampaignResponse`, `MembershipFeeTierResponse`,
   `MembershipFeeTierSummaryResponse`, `PaymentRuleResponse`, `MemberFeeChoiceResponse`,
   `MemberFeeHistoryResponse`+nested, `MemberFeeSummaryResponse`+nested,
   `MembershipFeeGroupResponse`). Same `_embedded.members` runtime-embed regression-test note as
   `events` applies to `getFeeGroup`.
6. **`groupsFamily`, `groupsFree`, `groupsTraining`** (Category B: `GroupSummaryResponse`,
   `PendingInvitationResponse`, `FamilyGroupSummaryResponse`, `TrainingGroupSummaryResponse`;
   Category C: all three `*Response` root records plus their promoted item schemas). Last because
   it is the only phase depending on Phase 1's spike and carries the highest structural risk
   (spec schema promotion + new codegen hook, exercised together for the first time). Do
   `groupsFamily`/`groupsFree` (whose item schemas are already named) before `groupsTraining`
   (which additionally needs the inline-to-named promotion), so the new codegen path is proven
   against a smaller diff first.
7. Full backend test suite (unit + Modulith integration) after the final module — no test should
   need a behavioral change, only import fixes for renamed/moved types.

**Rollback:** each phase is a single module's `models`/`mappings`/spec diff plus its own deleted
`.java` files — reverting one phase (restore the hand-written classes, shrink `models` back) does
not affect any other already-migrated module, same isolation property `custom-openapi-codegen`
relied on.

## Open Questions

- **Exact `fromProperty` vs. `postProcessModelProperty` choice** — resolved by Phase 1's spike, not
  before. Whichever fires early enough to rewrite the property's `Schema` before `super`'s own
  type/import resolution runs (same requirement `handleMethodResponse`'s override satisfied) wins.
- **`IcalTokenResponse` target location** — today a package-private `record` nested inside
  `IcalTokenController.java`. The generator emits into `modelPackage`
  (`com.klabis.calendar.infrastructure.restapi`) as a top-level class, same package, so the
  controller's nested declaration is simply deleted and its (already-matching) simple name
  resolves to the generated one — no import needed since same-package types don't require one, but
  confirm no other file relied on `IcalTokenController.IcalTokenResponse`'s qualified name.
- **Whether `MembershipFeeGroupResponse`'s Category A reclassification (from an originally-assumed
  Category C) needs a fresh read of `membershipfees.yaml` beyond what this design already quotes**
  — resolved during exploration (see Context); no further open item here, listed for traceability
  only.
