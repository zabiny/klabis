## Why

`custom-openapi-codegen` (archived 2026-08-25) taught the OpenAPI generator to resolve HAL
envelope schemas structurally, removing `--strip-hal`, the `doLast` regex patch, and per-envelope
`schemaMappings`. It deliberately left every module's `models` list unchanged in scope — only
request DTOs are generated today. 33 response DTO classes across 8 modules (`calendar`, `common`,
`events`, `finance`, `groups` ×3, `membershipfees`, `members`) remain hand-written, some for a
real structural reason and some simply because nobody has added them to a `models` list since the
migration landed. Response DTOs drifting out of sync with the spec (missing `x-klabis-*`
annotations, stale HAL comments referencing a mapping mechanism that no longer exists) is a live
maintenance risk this change removes at the source.

## What Changes

- Add 20 already-generatable response DTOs (flat objects, no `_embedded`, no `@Relation`) to their
  module's `models` list in `backend/build.gradle.kts`, deleting the hand-written class once the
  generated one is verified equivalent in shape (Category A).
- Add a new `x-klabis-relation` schema-level vendor extension (`collectionRelation`,
  optional `itemRelation`) and render it in `pojo.mustache` as
  `@org.springframework.hateoas.server.core.Relation(...)`, then generate the 7 DTOs that need a
  non-default HAL collection relation name (Category B).
- Extend `KlabisSpringCodegen`/`HalEnvelopeDetector` with a property-level unwrap: a model property
  shaped as `array of $ref-to-allOf[payload, {_links}]` generates as `List<Payload>` (no
  `EntityModel` wrapper — HATEOAS adds `_links` per item at runtime via the existing
  `RepresentationModelProcessor` postprocessors, unchanged). Promote the inline payload schemas this
  uncovers (`TrainerResponse`, `GroupMembershipResponse`, `ParentResponse`,
  `FamilyGroupMembershipResponse`, `OwnerResponse`, `FreeGroupMembershipResponse`) to named spec
  schemas so they have something to generate from (Category C).
- Delete the 33 hand-written response DTO classes once their generated replacement is verified;
  update every controller/mapper/postprocessor that references them by (unchanged) type name.
- Correct a stale spec comment (`members.yaml` `SuspensionBlockedWarning`) referencing a removed
  `schemaMapping` mechanism that no longer describes reality.

## No Behavior Change Justification

**Specs reviewed:** `openspec/specs/members/spec.md`, `openspec/specs/events/spec.md`,
`openspec/specs/groups/spec.md`, `openspec/specs/membershipfees/spec.md`,
`openspec/specs/finance/spec.md`, `openspec/specs/calendar/spec.md` — all unaffected. Every response
DTO's JSON shape, field names, HAL `_links`/`_embedded` keys, and field-level authorization
behavior stay identical; only the *Java source* of each record changes from hand-written to
generated. `custom-openapi-codegen`'s own acceptance bar (byte-for-byte or import-order-only
identical generated output vs. hand-written) applies here per-class.

**Why no spec update is needed:** No REST endpoint, status code, request/response field, HAL link,
or authorization rule changes. This is the same category of change as `custom-openapi-codegen`
itself (already precedent as `spec-free`) and the five `refactor(openapi)` commits preceding it on
this branch.

## Impact

- `backend/build.gradle.kts` — `models` lists grow for `calendar`, `common`, `events`, `finance`,
  `members`, `membershipfees`, `groupsFamily`, `groupsFree`, `groupsTraining`.
- `backend/buildSrc/src/main/java/com/klabis/openapi/codegen/` — `HalEnvelopeDetector` gains a
  property-level unwrap path; `KlabisSpringCodegen` gains the extension point that invokes it
  (exact hook TBD in design.md).
- `backend/src/main/openapi-templates/pojo.mustache` — renders `x-klabis-relation`.
- `docs/openapi/spec/*.yaml` — new named schemas for previously-inline item payloads; new
  `x-klabis-relation` extension on 7 schemas; one stale comment corrected.
- 33 hand-written `.java` files deleted across `calendar`, `common`, `events`, `finance`, `members`,
  `membershipfees`, `groups/{familygroup,freegroup,traininggroup}` `infrastructure.restapi`
  packages; their controllers/mappers/postprocessors updated to import the generated type instead
  (package and simple name unchanged, so most call sites need no edit).
- No frontend impact — `openapi-typescript` codegen and the wire contract are untouched.
