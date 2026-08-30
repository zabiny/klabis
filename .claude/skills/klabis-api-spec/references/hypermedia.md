# Responses & Hypermedia

Everything about what an endpoint returns: which media types it declares, how the bundler derives the
HAL envelope from the payload schema, why a payload schema's *name* is part of the wire contract, and
how `x-hal-*` declares the links and affordances that envelope carries.

Envelope shape and the links inside it are one concern — a change to either usually needs the other,
which is why they live in one file. Read this before adding or changing any response.

## Contents

- **Declare the payload; the bundler derives the envelope** — the core rule, and bodyless 201/204
- **What the deriver produces** — collection vs. item, paged vs. unpaged, `_templates`
- **`x-klabis-hal: false`** — the opt-out, and who uses it
- **`x-hal-entity-items`** — array items that are resources in their own right
- **`x-hal-embedded`** — a nested collection assembled at runtime
- **`mappings` for hand-written overrides** — the narrow remaining use
- **A payload schema's name is wire contract** — `_embedded` keys and frontend indexing
- **`x-hal-*`** — declaring links and affordances, what the frontend generates, the MAXIMAL variant

## Declare the payload; the bundler derives the envelope

A response declares its `application/json` payload and stops there:

```yaml
      responses:
        '200':
          description: Member found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/MemberDetailsResponse'
```

`tools/openapi-bundle/lib/derive.mjs` adds the `application/prs.hal-forms+json` content entry and the
`EntityModelMemberDetailsResponse` schema behind it into `klabis-full.json`. It is **the only place
in the repo encoding HAL envelope structure**, and it exists for the frontend's TypeScript types and
Swagger UI alone: the backend never needs envelope types, because Spring HATEOAS builds them at
runtime from the plain DTO (ADR-002). `KlabisSpringCodegen` reads `docs/openapi/spec/<module>.yaml`
directly and never sees an envelope.

So `_links` is never part of a schema you write. A payload declaring one of its own is not just
redundant — the deriver recognises envelopes by *shape*, so it would take that payload for an
already-written envelope and skip it silently. `validate.mjs` reports the case rather than letting it
through.

Two things follow for the generated Java: the record is built from the payload schema exactly as
written, and `produces` gains the hal-forms media type from
`KlabisSpringCodegen.addDerivedHalFormsContentType()` — the Java-side counterpart of the deriver's
own "is this a HAL response" test. **The two encode the same rule on either side of the language
boundary; change one, change the other.**

### Bodyless success responses still need an empty HAL content block

A `201` or `204` with no `content:` at all generates a method whose `produces` lists only
`application/problem+json` (inherited from the error responses). A client that sends
`Accept: application/prs.hal-forms+json` — as the frontend does on every request — then gets **406
Not Acceptable** instead of the success status. Declare the media type with an empty schema:

```yaml
        '204':
          description: Calendar item successfully updated
          content:
            application/prs.hal-forms+json: {}
```

No body is produced; this only pins the negotiated media type. Easy to miss because MockMvc tests
that omit `.accept(...)` pass either way — the gap surfaces only against a real client, or a test that
sets the header.

## What the deriver produces

The rule, applied to every 2xx response with an `application/json` schema:

| payload schema | derived envelope | generated Java |
|---|---|---|
| `$ref: FooResponse` | `EntityModelFooResponse` | `FooResponse` |
| `type: array, items: $ref FooDto` | `CollectionModelEntityModelFooDto` | `List<FooDto>` |
| the same, operation has `x-spring-paginated: true` | `PagedModelEntityModelFooDto` + `page` | `Page<FooDto>` |

Collection items are always themselves wrapped in `EntityModel` — matching
`HalResponseBodyAdvice.wrapCollection`, which does this unconditionally at runtime whether or not any
postprocessor gives the item links.

**Pagination is read from `x-spring-paginated` on the operation**, never inferred from the payload.
An array response without it is a `CollectionModel`, with it a `PagedModel`; there is no third
signal.

Every derived envelope carries `_templates`, uniformly. That property is an `additionalProperties`
map with no named members — it declares only that a template map may appear, which is true of every
HAL-FORMS resource. The contract that actually says *which* templates is `x-hal-templates`, which
`haltypes.mjs` turns into the named union in `halTypes.ts`.

### Envelope shape: composition, not repetition

The derived schemas do not restate `_links`/`_templates`/`page` — they compose three shared base
models from `_shared/hal.yaml` via `allOf`:

| base model | carries |
|---|---|
| `EntityModel` | `_links`, `_templates` |
| `CollectionModel` | `_links`, `_templates` |
| `PagedModel` | `allOf: [CollectionModel, {page}]` |

So `EntityModelFooResponse` is `allOf: [FooResponse, EntityModel]`;
`CollectionModelEntityModelFooDto` is `allOf: [CollectionModel, {_embedded: {fooDtoList: [...]}}]`;
an `x-hal-embedded` response adds a third member, `allOf: [Foo, EntityModel, {_embedded: {...}}]`.
No module YAML references the three base models — `bundleSpec` hoists them from `_shared/hal.yaml`
before the deriver runs, and `validate.mjs` fails the bundle if a derived envelope references a base
model that was not hoisted.

The two hand-written marker types (`EntityModelRootModel`, `EntityModelDashboardModel` in
`common.yaml`, `schemaMappings`-bound to `RootModel`/`DashboardModel`) keep their older flat
`{_links}` shape and are the one envelope kind the backend codegen *does* see. Leave them as they
are.

## `x-klabis-hal: false` — the opt-out

HAL is the default. The marker goes on the **operation** and is the only way out:

```yaml
  get:
    operationId: listOrisEvents
    x-klabis-hal: false
```

Six operations use it, and they are coherent rather than arbitrary: `getMySchedule`
(`text/calendar`), the four pre-auth password endpoints in `common.yaml` (called without a token,
outside hypermedia navigation), and `listOrisEvents` (an external ORIS passthrough). All six opt out
wholesale — per-response granularity does not exist, because no endpoint has needed it.

Two responses are skipped without any marker, and neither is an opt-out worth imitating: a `2xx`
whose only content is `text/csv` or `text/calendar` has no `application/json` schema to derive from,
and non-2xx responses are never enveloped (error bodies are not hypermedia resources — this is why
`suspendMember`'s plain-JSON `409` warning does not grow an envelope).

### Bodyless 201/204 declare the media type themselves

They have no `application/json` sibling, so the deriver has nothing to work from — see the empty
content block above.

## `x-hal-entity-items: true` — array items that are resources

An array property whose items are independently addressable — each carrying its own `_links` and
possibly affordances — is an API design choice, not something derivable. Mark the **array**:

```yaml
    GroupResponse:
      properties:
        owners:
          type: array
          x-hal-entity-items: true
          items:
            $ref: '#/components/schemas/OwnerResponse'
```

The deriver emits `EntityModelOwnerResponse` and retargets `items.$ref` at it;
`KlabisSpringCodegen.fromProperty` reads the same marker off the module YAML and resolves the
property to `List<EntityModel<OwnerResponse>>`. No `schemaMappings` entry is involved — though
an explicit per-module mapping on the item `$ref` still wins over the marker, which is what lets a
module override the resolution when it has to.

The marker sits on the array rather than beside the `$ref` deliberately: a `$ref` sibling is ignored
outright in OpenAPI 3.0 and honoured inconsistently by 3.1 tooling, so each of
`openapi-typescript`, openapi-generator and `haltypes.mjs` would have to be trusted separately.
`validate.mjs` restricts it to `type: array` with a `$ref` items schema; a singular HAL-wrapped
property is refused until a real case appears.

All 7 uses are in `groups.yaml` — the `parents`/`members`/`owners`/`pendingInvitations`/`trainers`
collections of the three group responses, whose rows carry per-row affordances like
`removeGroupOwner` and `cancelInvitation`.

## `x-hal-embedded` — a nested collection assembled at runtime

Some responses carry a nested collection in `_embedded` that is a different *shape* of data, not a
different serialisation of the payload: the controller assembles it at runtime via
`HalResponseContext.embed` (see the `backend-patterns` skill). The deriver cannot infer it from the
`application/json` payload, so the response declares it:

```yaml
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EventDto'
          x-hal-embedded:
            items: RegistrationSummaryDto
            suffix: WithRegistrations
```

- `items` names the **row payload**. The `_embedded` key comes from that schema's own
  `x-klabis-relation.collectionRelation` (or the default), never restated here — one value, so spec
  and runtime cannot drift.
- `suffix` (PascalCase) disambiguates the derived name — here `EntityModelEventDtoWithRegistrations`
  — from the plain `EntityModelEventDto` the same payload would get if returned bare elsewhere.
  `MembershipFeeGroupResponse` is exactly that case: `listGroupsForYear` returns it bare while
  `getFeeGroup` embeds its members.

Two operations use it: `getEvent` and `getFeeGroup`.

## `mappings` is now only for hand-written overrides the generator cannot derive from spec structure

Envelopes need **zero** `mappings`/`extraImportMappings` entries — a module spec contains none for
the generator to trip over, and `x-hal-entity-items` carries the one case that used to need them. A
`mappings` entry in `openApiModule(...)` is only for a case the generator genuinely cannot infer from
the spec alone:

- **A nested Java class.** `PaymentRuleResponse` → `MembershipFeeTierResponse.PaymentRuleResponse` —
  the schema name matches a top-level class name that isn't the one actually used.
- **A domain enum redirect.** `Authority` → `com.klabis.common.users.Authority` — without the
  mapping the generator synthesizes its own duplicate enum class instead of reusing the domain one.
- **A cross-module application type.** `BulkSyncResult` → `com.klabis.events.application.BulkSyncResult`
  — the target package differs from the module's own `restapi` package.
- **A marker type with no payload of its own.** `EntityModelRootModel` → `common.ui.RootModel`,
  `EntityModelDashboardModel` → `common.ui.DashboardModel`. These are the only two envelopes still
  written by hand, and they have to be: `GET /api` and `GET /api/dashboard` are pure navigation,
  their response is nothing but `_links`, so there is no `application/json` payload for the deriver
  to build from. Adding one collapses the generated return type to `ResponseEntity<Void>`. Their
  `mappings` entries are what keep the generator from emitting them as Java classes.
- **The `java.lang.Object` fallback.** `SuspensionBlockedWarning` → `java.lang.Object` — a
  discriminator-less `oneOf` union with no single Java type to stand for it. `KlabisSpringCodegen`
  suppresses the resulting (illegal) `@Schema(implementation = java.lang.Object.class)` doc block the
  same way it suppresses one for `Page<T>`.

**`type: array` — named or inline — always generates `List<T>` directly, with no wrapper class and
no mapping needed**, confirmed by generating with the actual pinned generator version (7.18.0):
see `docs/technicalAnalysis/openapi-generator-list-types.md`.

**A controller returning `Collection<T>` for a non-paginated list should be migrated to `List<T>`
instead of kept mapped.** There is no generator-native way to produce `Collection<T>` from an array
schema (only `List<T>`, always), so the choice is between a `schemaMappings` entry that exists purely
to preserve an incidental `Collection<T>` signature, or changing the controller's declared return type
to `List<T>` — prefer the latter. The underlying value is normally already a `List` (`.stream()...
.toList()`), so this is usually a one-line signature change, not a behavior change;
`HalResponseBodyAdvice.wrapCollection` branches on `instanceof Collection<?>`, which `List` satisfies
identically. `event-types`' `EventTypeController.listEventTypes` is the reference example.

**`List` still cannot be a `schemaMappings` target.** It remains a reserved container name in the
generator's type system, dropped *silently*, producing `ResponseEntity<>` with no diagnostic — this
is a non-issue now that array schemas are left unmapped rather than redirected onto anything, but
worth knowing if a future case seems to need remapping a `List<T>` response onto something else.

### `frontend/src/api/halTypes.ts` must type off the HAL envelope, not the `application/json` payload

`haltypes.mjs` builds each `*Resource` type by picking a schema out of the response's `content` map.
Because the bundler alphabetizes content-type keys, `"application/json"` always sorts ahead of
`"application/prs.hal-forms+json"` — so without an explicit preference, every response would retype
`*Resource` off the *bare* payload/array instead of the derived envelope, dropping
`_embedded`/`page` from the generated type even though the wire response still has them.
`haltypes.mjs` explicitly prefers `application/prs.hal-forms+json`/`application/hal+json` over any
other content type (falling back only if neither is present) — this is handled centrally, so nothing
extra is required per-endpoint. But if a `*Resource` type ever looks wrong, this is the first thing
to suspect, and the fix belongs in `haltypes.mjs`, not in per-endpoint content ordering (ordering in
the YAML doesn't survive bundling anyway — keys are re-sorted).

### A test asserting on `_links` needs an explicit `Accept` header

Every HAL response offers both the bare `application/json` payload and HAL-FORMS, so a `MockMvc` test
asserting `$._links...`/`$._templates...` without `.accept(...)` can negotiate to the payload and
fail, even though the endpoint serves HAL-FORMS correctly to the real frontend (which always sends
`Accept: application/prs.hal-forms+json`). State the header:

```java
mockMvc.perform(get("/api/users/{id}/permissions", id).accept(MediaTypes.HAL_FORMS_JSON))
    .andExpect(jsonPath("$._links.self.href").exists());
```

## A payload schema's name is wire contract

Spring HATEOAS derives the `_embedded` key of a collection from the **payload class name** at
runtime, and that class name comes from the schema name in the spec:

```
schema EventTypeDto  ->  record EventTypeDto  ->  "_embedded": { "eventTypeDtoList": [...] }
```

So renaming a payload schema renames a JSON key that clients read literally — a breaking change, not
a rename. The deriver reproduces that rule (`uncapitalize(schemaName) + "List"`), which is purely
lexical: `EventSummaryDto` becomes `eventSummaryDtoList`, suffix and all.

**`x-klabis-relation` overrides the default**, on the payload schema:

```yaml
    TransactionResource:
      x-klabis-relation:
        collectionRelation: transactions      # not "transactionResourceList"
        itemRelation: transaction
```

One declaration feeds both paths — `pojo.mustache` emits `@Relation` onto the record from it, and the
deriver reads the same value for the bundle's `_embedded` key — so the runtime response and the
published document cannot disagree. Never hand-write the `@Relation` annotation on a DTO instead;
that reintroduces the drift this replaced.

Two guards, both worth knowing because each fails at a different moment:

- **`halTypes.ts` indexes into `klabisApi.d.ts`** by schema name
  (`components['schemas']['EntityModelEventTypeDto']`). A name that does not exist there is a `tsc`
  error — loud, immediate. Since the bundler owns `klabis-full.json`, that name comes from the spec,
  so the fix is to correct the schema name there and regenerate — never to edit `klabisApi.d.ts`.
- **The `_embedded` key itself is not checked by anything.** Spec and runtime now share one
  declaration, so they cannot drift from each other — but a wrong `x-klabis-relation` is wrong in
  both at once, and the frontend types then name a property no response contains: `undefined` at
  runtime, an empty list in the UI, no error anywhere.

If a rename looks harmless because the tests stayed green, check whether a test was edited in the
same change. Adjusting a `$._embedded.*List` JSON path *is* the breakage surfacing — not the fix.

## `x-hal-*` — declaring links and affordances

On the **response object**, not on the schema — links describe the representation, not the payload.

```yaml
responses:
  '200':
    content:
      application/json:
        schema: { $ref: '#/components/schemas/MemberDetailsResponse' }
    x-hal-links:
      self: { description: This member }
      collection: { operation: listMembers, description: Back to the member list }
      permissions: { description: 'Requires MEMBERS:PERMISSIONS' }
    x-hal-templates:
      updateMember: { operation: updateMember }
      suspend: { operation: suspendMember, description: Present only while the member is active }
```

Reference operations by `operation: <operationId>` — never `operationRef` with escaped slashes.
The bundler validates that the target exists.

### The template key must be the runtime affordance name — never `default`

`klabisAfford` builds every affordance through Spring HATEOAS's `afford()`, which names the
HAL-FORMS template **after the controller method** (`updateMember`, `editSnapshot`,
`changeDeadline`, …). It never emits a template called `default`. An older convention keyed the
primary update/create affordance as `x-hal-templates.default`; `haltypes.mjs` copied that key
verbatim into `halTypes.ts`, so `*Hal._templates` then declared a `default` rel the wire response
never carries and omitted the real one. Every `default` key was renamed to its operation name —
match that: **the key equals the `operation:` value** for the primary affordance just as for every
other. If a `_templates.<name>` read on the frontend has no matching key in the generated `*Hal`,
that is the spec being wrong, not the read.

### What the frontend gets from them

`npm run openapi` generates `frontend/src/api/halTypes.ts` from these declarations — per operation a
`*Rels` constant, a `*Hal` interface and a `*Resource` type (`<payload schema> & <*Hal>`, i.e. the
`EntityModel…`/`CollectionModel…`/`PagedModel…` schema from `klabisApi.d.ts` intersected with the
typed `_links`/`_templates`):

```ts
import {GetMemberRels, type GetMemberResource} from '@/api';   // re-exported from the api barrel
GetMemberRels.links[0]        // 'account' — typed, not a bare string literal
```

`halTypes.ts` is re-exported from `frontend/src/api/index.ts`, so pages import `Get*Resource` /
`List*Resource` / `*Hal` / `*Rels` from `@/api`, not a deep `@/api/halTypes` path.

**Type the page's HAL response with the `*Resource` type**, passed to `useHalPageData<T>()`:

```ts
const { resourceData } = useHalPageData<GetMemberResource>();
// resourceData.firstName        — typed from MemberDetailsResponse
// resourceData._embedded?.…     — typed row lists
// resourceData._templates?.suspend — typed rel (GetMemberHal keys)
```

`useHalPageData<T = HalResponse>` no longer constrains `T` (generated `*Resource` types are closed
objects that cannot satisfy `HalResponse`'s index signatures); its body reads the envelope through a
single internal cast to the structural `HalEnvelope` type in `types.ts`. Do **not** intersect
`*Resource` with `HalResponse` at the call site (`T & HalResponse` collapses every payload field to
`unknown`) — pass the bare `*Resource`.

For a collection row whose per-row `_links`/`_templates` are added at runtime (not in the schema),
extend the schema type: `components['schemas']['EntityModelFooDto'] & { _templates?: Record<string,
HalFormsTemplate> }`. For a detail payload whose array field carries such rows, override it:
`Omit<GetFooResource, 'members'> & { members?: FooMemberRow[] }`.

Use those constants instead of string literals so renaming a relation in the spec breaks the build
rather than silently breaking at runtime.

**Consume the generated values, don't retype them.** A local map of the same string literals typed
as `Record<..., GetMemberLinkRel>` looks safe but is not: it only asserts that each value is *some*
valid rel, so renaming one in the spec still type-checks. Index into `GetMemberRels.links` (an
`as const` tuple) or type the response with `GetMemberHal`.

Worth verifying whenever you wire up a new page: rename a rel in `halTypes.ts` by hand, confirm
`npx tsc --noEmit -p tsconfig.app.json` fails, then regenerate.

`frontend/src/api/types.ts` stays hand-written: it describes the HAL-FORMS **media type**
(`Link`, `HalResponse`, `HalFormsTemplate`, …), which is defined by the standard, not by Klabis.

### These describe the MAXIMAL variant

`klabisLinkTo` returns `Optional` and `klabisAfford` returns an empty list when the caller lacks
authorization, so **any link or template may be absent at runtime** and the same endpoint returns
different `_links`/`_templates` per user.

Consequences:

- Generated TS types make every rel optional. The value is the union of rel names, not a presence
  guarantee.
- Conditions — authorization, entity state — are **not expressible** in `x-hal-*`. Put them in
  `description` and in the OpenSpec design.md prose.
- Never write a spec implying a link is always present.

### Linking to an operation that does not exist in the spec yet

`operation:` is validated against the bundled `operationId`s, so a link pointing at an operation the
spec does not (yet) declare fails the bundle:

```
/paths/.../x-hal-links/event: operation "getEvent" does not match any operationId
```

`operation` is optional — a descriptor carrying only `description` validates. Document the link now,
leave a note, and attach `operation:` when the target module lands. Do not delete the rel (the link
exists at runtime and the frontend types should know about it), and do not relax the validator.

The spec moves first.

1. Edit `docs/openapi/spec/<module>.yaml`
2. `./gradlew compileJava` — regenerates `docs/openapi/klabis-full.json` (via `openapiBundle`, which
   every `openApiGenerate<Module>` task now depends on directly) and then the DTOs/`*Api`, failing on
   whatever no longer matches. This is the real check — a plain `./gradlew openapiBundle` only
   validates the spec syntactically; it does not run the Java generator.
3. Fix the controller against the regenerated interface
4. `cd frontend && npm run openapi`
5. `npx tsc --noEmit -p tsconfig.app.json` — catches schema-name mismatches in `halTypes.ts`

`./gradlew openapiBundle` validates extensions and refs without generating anything; useful for a
fast syntax check, but passing it does not mean the code compiles.

If step 3 shows the spec is wrong, go back to step 1. **Never adjust the spec to match existing Java
just to silence a mismatch** — that reintroduces code-first through the back door. The exception is a
genuine spec bug, which is a spec change like any other.
