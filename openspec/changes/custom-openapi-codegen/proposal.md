## Why

Adding a new endpoint or DTO to an existing module's OpenAPI spec today requires a matching
`build.gradle.kts` edit: the schema must be added to that module's `models` list (or the tag to
its `apis` list), and if the endpoint returns a HAL envelope (`EntityModel<T>`, `PagedModel<T>`,
`CollectionModel<T>`), a `schemaMappings`/`extraImportMappings` pair must be hand-written to
unwrap it to the real payload type — otherwise the generator either produces nothing for that
schema or leaves the HAL envelope as the controller's declared return type. This coupling was
discovered while diagnosing a regression where the `members` module's `models`/`mappings` were
emptied out, breaking `MemberSummaryResponse` generation and the `Page<T>` return type of
`listMembers`. The stock `openapi-generator` SpringCodegen has no concept of "HAL envelope,
unwrap to payload" — `x-spring-paginated` only affects the method parameter (adds `Pageable`),
never the return type — so the project compensates with two more mechanisms: a `--strip-hal`
bundle pre-process step (`tools/openapi-bundle`) that produces a second, codegen-only spec
bundle, and a `doLast` regex post-process patch on generated `*Api.java` files that removes an
illegal `@Schema(implementation = Page<X>.class)` class literal springdoc emits for
generic-typed mappings. Four moving parts (whitelist, mapping, pre-process, post-process) all
exist to compensate for the same missing capability: the generator cannot resolve a HAL envelope
schema to its real payload type on its own.

## What Changes

- Add a custom OpenAPI generator, `KlabisSpringCodegen`, as a `CodegenConfig` subclass of
  `org.openapitools.codegen.languages.SpringCodegen`, built inside `backend/buildSrc/` and
  registered via `META-INF/services/org.openapitools.codegen.CodegenConfig` so `generatorName`
  can be set to `"klabis-spring"` in `openApiModule(...)`.
- Override `handleMethodResponse()` to structurally detect two HAL envelope shapes and unwrap
  them to the real payload/`Page<T>`/`List<T>` return type automatically, without an explicit
  `schemaMappings` entry:
  - **Shape 1 — single entity (`EntityModel<T>`):** an `allOf` schema whose non-`$ref` member's
    properties are a subset of `{_links, _templates, _embedded}` → unwrap to the `$ref`'d type.
  - **Shape 2 — collection (`PagedModel<T>`/`CollectionModel<T>`):** a plain `object` schema
    with an `_embedded.<name>` property that is an array of `$ref`, plus `_links` → unwrap to
    `List<T>`; if a `page` property (`PageMetadata`) is also present → unwrap to `Page<T>`
    instead. The `page` property's presence is a more direct signal than the operation-level
    `x-spring-paginated` extension and replaces its role in return-type resolution (the
    extension can stay for the `Pageable` parameter wiring, which is a separate, already-correct
    mechanism).
- Because the generator resolves the correct return type internally, it never emits the illegal
  `@Schema(implementation = Page<X>.class)` in the first place — remove the `doLast` regex patch
  block from `openApiModule(...)` in `backend/build.gradle.kts`.
- Because the generator can compute the return type from `application/json` content directly
  while still using the full response's content-type set (HAL/HAL+FORMS included) for the
  `produces` clause, remove the `--strip-hal` flag, `stripHalForCodegen()` in
  `tools/openapi-bundle/lib/stripHal.mjs`, and the separate `bundleSpecForCodegen` Gradle task —
  generate directly from the same `docs/openapi/klabis-full.json` the frontend already consumes.
- Replace each module's explicit `models`/`apis` whitelist with tag-scoped auto-discovery: given
  a module's `apis` tag list, generate every schema reachable from those tagged operations,
  removing the need to add each new request/response DTO by name. (The `apis` tag list itself
  stays — a module boundary still has to name which tags belong to it — but no separate `models`
  enumeration is needed once schemas are discovered by reachability.)
- Keep explicit `mappings` **only** for genuine hand-written Java DTO overrides that are not
  inferable from spec structure: nested classes (e.g. `PaymentRuleResponse` →
  `MembershipFeeTierResponse.PaymentRuleResponse`), domain enum redirection (e.g. `EventStatus`
  → `com.klabis.events.domain.EventStatus`), and cross-module application-layer types (e.g.
  `BulkSyncResult`). These stay because the spec has no way to express "this Java type already
  exists, nested, at this path" — that's a fact about hand-written source, not something
  structurally derivable from OpenAPI.
- Explicitly **out of scope** (investigated, does not block the "zero `build.gradle.kts` touch
  for a new endpoint/DTO in an existing module" goal, and not simplified by this change):
  - Multi-package module routing — the `groups` module spans three Java packages from one spec
    file and needs three `openApiModule()` registrations; this is an architectural decision
    about package layout, not a generator limitation.
  - Tag substring-collision in the `apis` filter (e.g. `"ORIS"` matching `"OrisEvents"`) — an
    independently fixable generator matching quirk, unrelated to envelope unwrapping.
  - `_embedded` blocks assembled at runtime via `HalResponseContext.embed(...)` for secondary
    collections beyond the primary payload — already correctly handled once Shape 1 unwraps the
    primary payload type; no additional mechanism needed.
  - Frontend codegen (`npm run openapi` / `openapi-typescript`) — a different, non-Java tool that
    needs the full HAL envelope for `_links`/`_templates` typing and has no equivalent manual
    mapping burden. Unaffected by this change.

## No Behavior Change Justification

**Specs reviewed:** `members/spec.md`, `events/spec.md`, `event-types/spec.md`,
`event-categories/spec.md`, `event-registrations/spec.md`, `membership-fees/spec.md`,
`member-accounts/spec.md`, `user-groups/spec.md`, `category-presets/spec.md`,
`calendar-items/spec.md`, `users/spec.md`, `users-authentication/spec.md`,
`application-navigation/spec.md`, `dashboard/spec.md`, `member-permissions-dialog/spec.md`,
`membership-fee-campaign-manual-close/spec.md`, `server-configuration/spec.md`,
`email-service/spec.md`, `non-functional-requirements/spec.md` — none of these describe the
OpenAPI code generation mechanism; they describe API request/response contracts and business
behavior, all of which are defined by `docs/openapi/spec/*.yaml` (unchanged by this proposal)
and enforced by the generated code's *shape*, not by how that shape was produced.

**Why no spec update is needed:** this change targets exclusively the Java class hierarchy of
the code generator and the Gradle/Node tooling that invokes it. The two structural detection
rules (Shape 1, Shape 2) are designed to reproduce, byte-for-byte, the same generated return
types the existing explicit `schemaMappings` produce today — this is a mechanical replacement of
"list every envelope→payload mapping by hand" with "derive the same mapping from the envelope
schema's own shape." No HTTP request/response contract, status code, validation rule,
authorization check, or HAL/HAL+FORMS wire format changes. The acceptance bar for this refactor
is that the generated Java sources for every existing module are unchanged (or differ only in
immaterial ways such as import ordering) after switching generators — any generated-code diff
beyond that is a regression, not an intended outcome.

## Impact

- **New code:** a `KlabisSpringCodegen` Java class (new Gradle-buildable module, e.g.
  `backend/openapi-codegen/` or a `buildSrc` addition) plus its SPI registration file.
- **`backend/build.gradle.kts`:** `openApiModule(...)` changes `generatorName` to
  `"klabis-spring"`, drops the `doLast` regex patch, drops per-module `models` lists (or reduces
  them to the tag list already present via `apis`), and shrinks `mappings`/`extraImportMappings`
  to the hand-written-override subset only.
- **`tools/openapi-bundle`:** removes `--strip-hal` CLI flag, `stripHalForCodegen()` in
  `lib/stripHal.mjs` (and its test file), and the `bundleSpecForCodegen` Gradle task; backend
  codegen input becomes `docs/openapi/klabis-full.json` directly.
- **Developer workflow:** adding a new endpoint/DTO to an existing module's `.yaml` file and
  running `./gradlew openapiBundle` + a build is sufficient — no `build.gradle.kts` edit needed
  unless the new schema needs a hand-written-override mapping.
- **No impact** on `frontend/`, on any REST endpoint's wire contract, or on
  `docs/openapi/klabis-full.json`'s own content (still produced by the existing `bundle.mjs`
  unchanged).
