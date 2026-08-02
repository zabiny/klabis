## Why

Backend codegen (`openApiModule(...)` in `backend/build.gradle.kts`) still needs a hand-maintained
`schemaMappings`/`importMappings`/`models` configuration alongside the OpenAPI spec, even after two
rounds of work (giving every HAL response an `application/json` sibling, then eliminating the
resulting envelope mappings that turned out to be generator-native `List<T>` all along — see
`docs/technicalAnalysis/openapi-generator-list-types.md` and
`docs/technicalAnalysis/openapi-gradle-mappings-review.md`). 33 mapping entries and a per-module
`models` allow-list remain, split across six distinct categories, each with a different root cause.
The target this change works toward: backend codegen driven **entirely** by the spec, with zero
Gradle-side type configuration — every model present in the (HAL-stripped) spec gets generated, and
every generated type resolves correctly with no redirect.

## What Changes

- Domain enum substitutions (8 entries: `Gender`, `DeactivationReason`, `DrivingLicenseGroup`,
  `TrainerLicenseDto_level`, `RefereeLicenseDto_level`, `EventStatus`, `Authority`,
  `UpdateMemberRequest_gender`) — investigate whether the OpenAPI generator/mustache templates can be
  told to reuse an existing Java enum for a `type: string, enum: [...]` schema (e.g. via
  `importMappings` alone without `schemaMappings`, a custom `x-klabis-*` extension read by
  `pojo.mustache`, or a generator config option not currently in use) instead of generating a
  duplicate enum class that then has to be redirected away.
- `SuspensionBlockedWarning` -> `java.lang.Object` (1 entry) — investigate whether the generator can
  express a discriminator-less `oneOf` union as something other than `Object`, or whether the spec
  itself should model `suspendMember`'s 409 body differently (e.g. giving the `oneOf` a discriminator)
  so a real generated type exists and no redirect is needed.
- `Page<T>` pagination mappings (4 entries: `members`, `finance`, `events` — envelope + array-sibling
  pairs) — investigate whether `x-spring-paginated` (or a new extension) can also drive the *response*
  type, the same way it already drives the `Pageable` *parameter*, so a paginated response's
  `application/json` array schema resolves to `Page<T>` without an explicit `schemaMappings` entry.
- Cross-package application-layer type mappings (4 entries: `BulkSyncResult`/`BulkImportResult`,
  envelope + bare-schema pairs in `events`) — investigate whether these types can move into
  `infrastructure.restapi` (matching every other generated DTO's location) without violating hexagonal
  boundaries, removing the package mismatch that requires the mapping.
- Envelope mappings for operations with no `application/json` sibling by design (7 entries: `getEvent`,
  `getFeeGroup`, the three "get single group" endpoints in `groups`, and the `Root`/`Dashboard` marker
  types) — investigate whether `haltypes.mjs`'s envelope-preference fix (already applied) removes the
  reason these operations skip the `application/json` sibling in the first place (their original reason
  for skipping it was to avoid the frontend `*Resource` type losing `_embedded`/nested-`_links` typing
  — that specific failure mode was fixed at the tooling level for a different reason; re-verify whether
  adding the sibling is now actually safe for these operations too).
- Nested Java class mappings (2 entries: `PaymentRuleResponse` inside `MembershipFeeTierResponse`,
  `OrisEventSummary` inside `OrisController`) — a preliminary investigation (moving both to top-level
  files and letting the generator produce them fresh, adding them to `models`) surfaced two blockers
  that make this harder than "just move the class":
  1. Both schemas mark response-only optional fields with the OpenAPI 3.1 `type: [x, 'null']` spelling
     (correctly describing "may be absent from the source system," e.g. ORIS location/organizer, or
     "exactly one of these two fields is set"), but that spelling is indistinguishable to the generator
     from a PATCH tri-state field — `openApiNullable=true` wraps every such property in
     `JsonNullable<T>`, which is meaningless ceremony for a value that is never "absent," only
     "present-and-possibly-null." There is currently no bare "nullable, no tri-state" keyword available
     for a 3.1 response property (per the `klabis-api-spec` skill: 3.0's `nullable: true` is silently
     ignored, and there is no dedicated response-only nullable extension).
  2. `PaymentRuleResponse.ruleType`'s inline `enum: [PERCENTAGE, FIXED_AMOUNT]` gets promoted to its
     own schema by `resolveInlineEnums` and would generate a brand-new Java enum, replacing the
     hand-written `String ruleType` — adopting it touches every call site that currently passes string
     literals, and *not* adopting it just reintroduces a `schemaMappings` entry for the promoted
     sub-schema, defeating the purpose.
  This category needs either a spec-level solution (a dedicated "nullable response property, no
  tri-state" extension analogous to `x-klabis-not-blank`) before the Java-class move is worth doing, or
  a decision to keep both as permanent, documented hand-written exceptions (the same status as
  `MemberAccountResource`/`TransactionResource` in `finance`) — to be resolved during design.
- Remove the per-module `models = listOf(...)` allow-list entirely, so the generator produces every
  model present in the bundled, `--strip-hal`-postprocessed spec used as codegen input, instead of
  requiring each model to be explicitly enumerated per module. Today an empty or incomplete `models`
  list is a known footgun (an empty list generates *everything*, per existing comments in
  `build.gradle.kts` and the `klabis-api-spec` skill) — removing the allow-list removes the need to
  keep it in sync with the spec at all, at the cost of needing the models generator config option
  either omitted or handled differently (likely `apis`-only filtering combined with per-module
  package targeting already does the necessary scoping, since `apis` already restricts which tags'
  *interfaces* get generated per module — needs verification that the equivalent model scoping falls
  out correctly).
- No spec (`docs/openapi/spec/*.yaml`) changes are anticipated for most of the above — this is
  fundamentally a codegen-configuration and possibly generator-template change. The
  `SuspensionBlockedWarning` and nested-class categories are the two items that may require a spec
  change (a discriminator on an existing `oneOf`; a new nullable-response extension), and if so they
  change only the OpenAPI document's shape or vendor extensions for those bodies, not any
  client-observable contract (the wire response is unaffected either way — only the generated Java
  type improves).

## No Behavior Change Justification

**Specs reviewed:** `openspec/specs/members/spec.md`, `openspec/specs/events/spec.md`,
`openspec/specs/event-types/spec.md`, `openspec/specs/member-accounts/spec.md`,
`openspec/specs/membership-fees/spec.md`, `openspec/specs/user-groups/spec.md`,
`openspec/specs/dashboard/spec.md`, `openspec/specs/application-navigation/spec.md`,
`openspec/specs/users/spec.md` — all unaffected, since none describe OpenAPI codegen internals,
Gradle build configuration, or Java class layout. They describe user-facing capabilities (what an
endpoint does, what fields a response carries, what business rules apply), none of which change here.

**Why no spec update is needed:** This is purely a change to how backend Java DTOs/API interfaces are
*generated* from the existing spec — the wire contract (request/response JSON shapes, status codes,
HAL links/templates, authorization) is defined by `docs/openapi/spec/*.yaml` and is untouched. Every
prior round of this same effort (the `application/json`-sibling round and the `schemaMappings`
-elimination round, both completed and merged) changed only `backend/build.gradle.kts` and generated
Java, verified end-to-end each time via the full backend test suite (3200 tests, 0 failures) and a
frontend `npm run openapi && tsc --noEmit` no-op. This change continues that same pattern: the
generator configuration and possibly its mustache templates or vendor extensions change; the API's
observable behavior does not. Even the "possible spec change" items (a `oneOf` discriminator, a new
nullable-response extension) only affect the OpenAPI document's internal shape/annotations, not any
field, status code, or business rule a client would observe.

## Impact

- `backend/build.gradle.kts` — every `openApiModule(...)` block's `mappings`/`extraImportMappings`/
  `models` arguments, and possibly the `openApiModule` function signature itself if `models` is removed
  as a parameter entirely.
- `backend/src/main/openapi-templates/*.mustache` — possibly, if the enum-substitution, `oneOf`-union,
  or nullable-response-property investigations require a template change (parallel to the existing
  forked `pojo.mustache`/`api.mustache` overrides for `x-klabis-*` extensions and `JsonNullable`).
- `tools/openapi-bundle/lib/stripHal.mjs` / `bundle.mjs` — possibly, if the `Page<T>` investigation
  needs the bundler to annotate paginated responses more explicitly for the generator to pick up
  without a `schemaMappings` entry.
- `tools/openapi-bundle/lib/validate.mjs` — possibly, if a new `x-klabis-*` extension (nullable
  response property) is introduced, following the existing pattern for
  `x-klabis-not-blank`/`x-klabis-past`/`x-klabis-url`.
- `com.klabis.membershipfees.infrastructure.restapi.MembershipFeeTierResponse.PaymentRuleResponse` and
  `com.klabis.oris.OrisController.OrisEventSummary` may end up staying permanent hand-written
  exceptions (documented alongside `MemberAccountResource`/`TransactionResource`) rather than being
  migrated to generated classes, depending on the outcome of the nullable-response-property
  investigation.
- Developer workflow: registering a new spec-first module currently requires knowing which
  `schemaMappings` entries to add and keeping `models` in sync with the spec (`klabis-api-spec` skill,
  "Registering a module for codegen"). If this change succeeds, that step disappears — a new module
  needs only `pkg`/`apis`, nothing else. The skill would need a follow-up update once implemented.
