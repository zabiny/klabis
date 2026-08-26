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
- Every field-level security annotation the hand-written class carried (`@OwnerId`,
  `@OwnerVisible`, `@HasAuthority`, `@HalForms(access=...)`) has a corresponding `x-klabis-*`
  extension on the spec property **before** that hand-written class is deleted — never after. A
  silently-dropped `@HasAuthority(MEMBERS_MANAGE)` on `email`/`active` (`MemberSummaryResponse`) or
  `@HasAuthority(EVENTS_MANAGE)` on `status` (`EventSummaryDto`) is a security regression, not a
  cosmetic diff — see [[feedback-check-annotations-before-unmapping]] precedent from the
  hand-written-to-generated *request* DTO migration, which this repeats for *response* DTOs.
- Category A: add missing `models` entries only; delete the hand-written class once the generated
  one is shape-equivalent AND field-security-equivalent (Decision 0).
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

### Decision 0 (all categories): field-security parity check gates every deletion

Structural shape-equivalence (field names, types, JSON shape) is necessary but not sufficient
before a hand-written class is deleted. A hand-written response DTO commonly carries
security-relevant annotations that have no counterpart in a *field name/type* diff:

| Hand-written annotation | Spec extension it must map from | Where seen in the 33 classes |
|---|---|---|
| `@HasAuthority(Authority.X)` | `x-klabis-authority: X` | `MemberSummaryResponse.email/active`, `EventSummaryDto.status` |
| `@OwnerVisible` | `x-klabis-owner-visible: true` | `RegistrationSummaryDto.registrationTime` |
| `@OwnerId` | `x-klabis-owner-id: true` | `RegistrationSummaryDto.coordinators` |
| `@HalForms(access = READ_ONLY)` | `x-klabis-halforms-access: READ_ONLY` | `EventDto.id/status/cancellationReason/deadlines`, `CalendarItemDto.id/eventId`, `CategoryPresetDto.id` |
| `@HandleAuthorizationDenied(NullDeniedHandler.class)` (class-level) | none needed — `additionalModelTypeAnnotations` applies it to every generated model unconditionally | all |
| `@JsonIgnore` (drops a field from the wire entirely) | property omitted from the spec schema altogether | `RegistrationSummaryDto.coordinators/registeredMemberId` |

**Verification procedure, per class, before deletion (not after):**
1. List every field-level annotation on the hand-written record's components (grep the file for
   `@OwnerId`, `@OwnerVisible`, `@HasAuthority`, `@HalForms`, `@JsonIgnore`, `@JsonInclude` at the
   component level — class-level `@JsonInclude`/`@HandleAuthorizationDenied` are already covered by
   `additionalModelTypeAnnotations` and need no per-field check).
2. For each, confirm the matching `x-klabis-*` extension exists on the corresponding spec property
   (or, for `@JsonIgnore`, confirm the property is absent from the spec schema — never present and
   silently unauthorized).
3. Generate and read the actual generated `.java` source (not just the spec YAML) — `pojo.mustache`
   is the thing that must render the annotation, and a typo in the extension key (`x-klabis-authroity`)
   fails silently (no annotation rendered, no build error) rather than loudly.
4. Only after steps 1–3 pass does the hand-written class get deleted. If a gap is found, fix the
   spec first (add the missing `x-klabis-*` extension) — never delete the hand-written class to
   "match" a spec that hasn't caught up yet.

This mirrors the exact incident recorded in [[feedback-check-annotations-before-unmapping]] from
the request-DTO migration: a hand-written class replaced by a generated one that silently dropped
a `x-klabis-minLength`/`x-klabis-authority` constraint is a quiet security regression, not a build
failure — nothing in `./gradlew build` catches a missing annotation, only a deliberate diff against
the hand-written source does.

### Decision 1 (Category A): treat as a `models`-list gap, not a codegen gap

No `KlabisSpringCodegen`/template change. Per class: add to the module's `models` list in
`build.gradle.kts`, run the generate task, diff the generated record against the hand-written one
(field names, types, `x-klabis-*`-derived annotations per Decision 0's checklist), fix any spec gap
(e.g. a property missing `x-klabis-halforms-access` that the hand-written version had as
`@HalForms`), only then delete the hand-written class, and update its (unchanged) import in every
controller/mapper/postprocessor that referenced it.

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

### Decision 3 (Category C): property-level unwrap, no `EntityModel<T>` in generated Java (SUPERSEDED for `FamilyGroupResponse`/`GroupResponse` — see Decision 3a below)

**Status: this section describes the originally-designed mechanism, which turned out to conflict
with real controller code once wired in (tasks.md 6.3's blocker). It still applies as-is to any
future property-level unwrap where the controller does NOT need per-item `EntityModel<X>` — e.g. if
`TrainingGroupResponse` (Phase 7) turns out not to need per-item links after all. For
`FamilyGroupResponse`/`GroupResponse`, Decision 3a is what actually shipped.**

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

**Field-security check applies here too:** `TrainerResponse`/`GroupMembershipResponse`/
`ParentResponse`/`FamilyGroupMembershipResponse`/`OwnerResponse`/`FreeGroupMembershipResponse` are
plain value carriers today with no `@OwnerVisible`/`@HasAuthority` — Decision 0's checklist still
applies (confirm none is silently introduced as a gap, i.e. confirm the *absence* of any
field-security annotation on the hand-written class before treating the promoted spec schema as
equivalent, not just its presence).

### Decision 3a (supersedes 3 for `FamilyGroupResponse`/`GroupResponse`): `application/json` sibling + `schemaMappings` onto a real generic type

tasks.md 6.3 found the real blocker Decision 3 did not anticipate: `FamilyGroupController` and
`FreeGroupController` build each collection item as `EntityModel.of(new ParentResponse(...))`
**on purpose** — every item carries its own conditionally-present `_links`/affordances (a `member`
link, plus an owner/manager-gated self+DELETE affordance), which the frontend
(`FamilyGroupDetailPage.tsx`/`GroupDetailPage.tsx`) reads directly to render per-row remove buttons
and compute mutation URLs. Rewriting the property to bare `List<Payload>` (Decision 3's mechanism)
makes the controller fail to compile — `EntityModel<T>` does not implement `T`, Java generics are
invariant — and there is no existing runtime mechanism that attaches per-item HAL links to a nested
collection property the way `HalResponseBodyAdvice`/postprocessors do for a top-level
`EntityModel`/`PagedModel`. Decision 3's own text acknowledged this risk in the abstract
("HAL linking is exclusively Spring HATEOAS's runtime concern") but the concrete controllers already
depend on `EntityModel<Payload>` being the field's own static type, not something added later.

**The fix actually used, confirmed against real generated output (not just the design):**

1. **Response-level:** add an `application/json` content entry alongside
   `application/prs.hal-forms+json` on `getFamilyGroup`/`getGroup`, pointing directly at the bare
   payload schema (`FamilyGroupResponse`/`GroupResponse`, not their `EntityModelX` envelope). The
   bundler's alphabetical content-type sort places `application/json` first, and
   `KlabisSpringCodegen#resolveResponseSchema` reads the first content entry to feed
   `HalEnvelopeDetector` — confirmed by inspecting the generated `FamilyGroupsApi`/`GroupsApi`
   springdoc `@Schema` annotations: both content entries now correctly name the bare payload class,
   proving response-level Shape 1 detection is fully suppressed for these two responses (their own
   top level has no `allOf`/`_links` shape to detect in the first place — this is a no-op, not a
   new unwrap path).
2. **Property-level:** for each `EntityModelX` envelope schema still referenced from a
   `parents`/`members`/`owners`/`pendingInvitations` array property, add a `schemaMappings` entry
   in `build.gradle.kts` redirecting the envelope schema name directly onto the literal string
   `org.springframework.hateoas.EntityModel<X>` — a real, legal Java generic type, not a name the
   generator needs to understand structurally. A paired `extraImportMappings` entry
   (`"org.springframework.hateoas.EntityModel<X>" to "org.springframework.hateoas.EntityModel"`)
   makes the emitted `import` line resolve correctly, since `importMapping` is a plain string-keyed
   map — the key does not need to be a legal Java identifier, only to match exactly the string that
   ends up in the property's `complexType`/`baseType`.
3. **`KlabisSpringCodegen.fromProperty` needed one guard added:** Decision 3's `fromProperty`
   override runs its structural Shape 1-item detection unconditionally, which rewrites the array
   item's `$ref` from `EntityModelX` to the bare payload's `$ref` *before* `super.fromProperty` ever
   runs — so `schemaMapping`'s substitution (step 2) never fires, since by the time `super` resolves
   the item schema, its `$ref` no longer names `EntityModelX` at all. Confirmed empirically: without
   the guard, `FamilyGroupResponse.parents` generated as `List<@Valid ParentResponse>` (Decision 3's
   default), not `List<EntityModel<ParentResponse>>`. The fix: `fromProperty` now checks whether the
   array item's `$ref` schema name is already a key in `schemaMapping()` — if so, it steps aside
   entirely and delegates the *original*, unrewritten property to `super`, letting the stock
   generator's own `schemaMapping` substitution take over. This makes the two mechanisms mutually
   exclusive per schema: a schema absent from `schemaMapping` gets Decision 3's default
   strip-to-bare-payload behavior; a schema explicitly `schemaMappings`-redirected gets whatever
   Java type string that mapping names, verbatim, including a generic one.

**Generated output actually produced** (verified, not assumed):

```java
public record FamilyGroupResponse(
    @Valid UUID id,
    @Valid List<org.springframework.hateoas.EntityModel<FamilyGroupMembershipResponse>> members,
    String name,
    @Valid List<org.springframework.hateoas.EntityModel<ParentResponse>> parents
) { }
```

Field-for-field equivalent to the hand-written class except: `UUID id` instead of `FamilyGroupId id`
(DTOs carry wire types — klabis-api-spec's existing rule, the hand-written class had drifted from
it), and alphabetized constructor-parameter order (an existing, already-documented generator
behavior — see "Anti-patterns" in the klabis-api-spec skill on constructing generated records
positionally). Both controllers needed only mechanical fixes: `group.getId()` → `group.getId().uuid()`,
and reordering positional constructor arguments to match the alphabetized field order — zero change
to the per-item `EntityModel.of(...)`/`klabisLinkTo`/`klabisAfford` logic itself.

**Why this is not "just Decision 3 with extra steps":** Decision 3's `fromProperty` rewrite is a
structural default with no spec-visible opt-out — every Shape 1-item property gets stripped to bare
`List<Payload>` unconditionally. Decision 3a is an explicit, per-schema override
(`schemaMappings`) layered on top, only for the specific `EntityModelX` schemas whose controller
genuinely needs the item type to stay `EntityModel<X>`. `TrainingGroupResponse` (Phase 7) is
expected to use Decision 3's original default, if its own controller turns out not to need per-item
links the same way — that determination has not been made yet (Phase 7 is unscoped from this
session).

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
