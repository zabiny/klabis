# Responses & Hypermedia

Everything about what an endpoint returns: which media types it declares, how the HAL envelope
relates to the plain payload schema, why a payload schema's *name* is part of the wire contract,
and how `x-hal-*` declares the links and affordances that envelope carries.

Envelope shape and the links inside it are one concern — a change to either usually needs the other,
which is why they live in one file. Read this before adding or changing any response.

## Contents

- **Payload and envelope are separate schemas** — the core split, and bodyless 201/204 responses
- **Declare both `application/json` and a HAL type** — the default and its four exceptions
- **Structural unwrapping** — how the generator resolves envelope -> payload without `schemaMappings`
- **`mappings` for hand-written overrides** — the narrow remaining use
- **A payload schema's name is wire contract** — `_embedded` keys and frontend indexing
- **`x-hal-*`** — declaring links and affordances, what the frontend generates, the MAXIMAL variant

## Payload and envelope are separate schemas

`_links` is not part of a DTO — Spring HATEOAS adds it when the controller wraps the payload in an
`EntityModel`. Model that split, or the generated Java record grows a bogus `links` component:

```yaml
EntityModelMemberDetailsResponse:      # envelope; never generated into Java
  allOf:
    - $ref: '#/components/schemas/MemberDetailsResponse'
    - type: object
      properties:
        _links:
          $ref: './_shared/hal.yaml#/components/schemas/Links'

MemberDetailsResponse:                 # payload; this is what becomes a record
  type: object
  properties: …
```

Same rule for `_embedded` and `page` on collections — they belong to `PagedModel*` /
`CollectionModel*`, not to the item.

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

## HAL envelopes are unwrapped structurally — no `schemaMappings`, no separate codegen bundle

`KlabisSpringCodegen` (`backend/buildSrc/`) resolves a response's `EntityModel*` / `PagedModel*` /
`CollectionModel*` HAL envelope schema down to its real payload type by inspecting the schema's
*shape*, not its name — see `openspec/changes/custom-openapi-codegen/design.md` for the full
rationale and `HalEnvelopeDetector` for the two shapes it matches:

- **Shape 1 — single entity:** `allOf` of exactly two members, the first a `$ref`, the second an
  inline object whose properties are a subset of `{_links, _templates, _embedded}`. Unwraps to the
  `$ref` target.
- **Shape 2 — collection:** a plain object with a `_links` property and exactly one
  `_embedded.<name>: array[$ref]`-shaped property. Unwraps to the array item's payload type (composed
  through a nested Shape 1, if the item is itself an envelope), as `List<T>` or — when the operation
  declares `x-spring-paginated: true` — `Page<T>`.

This means **the generator needs no separate `application/json` sibling to resolve the payload
type** — it reads the HAL envelope schema directly. The single codegen-only bundle
(`bundleSpecForCodegen` / `bundle.mjs --strip-hal`) this used to require no longer exists — backend
codegen reads `docs/openapi/klabis-full.json` directly, the exact same bundle the frontend consumes.

| response shape | `application/json` schema (optional, real content negotiation) | generated Java |
|---|---|---|
| single resource (`EntityModelFooResponse`) | `$ref: FooResponse` | `FooResponse` |
| collection (`CollectionModelEntityModelFooDto`) | `type: array, items: $ref FooDto` | `List<FooDto>` |
| paged (`PagedModelEntityModelFooResponse`) | named array schema, e.g. `FooResponseList` | `Page<FooResponse>` |

Pagination is read from `x-spring-paginated: true` on the **operation**, independent of which content
type resolves the payload — an operation serving only `application/json` still gets `Page<T>`, and a
paginated operation's `application/json` sibling (even a bare array with no `page`/`_links`) still
resolves to `Page<T>`, never `List<T>`.

### Declare both `application/json` and a HAL type — the default for body-carrying responses

Even though codegen does not need it, a body-carrying 2xx response normally declares **both** a HAL
content type and an `application/json` sibling: the plain-JSON option is a real content-negotiation
choice for callers that do not want hypermedia, not a codegen workaround. See "Bodyless success
responses" above for why a bare `{}` entry still matters for `produces` on 201/204.

Four situations legitimately depart from this, and recognising them matters — "fixing" one by adding
a sibling breaks it:

- **Pure navigation** (`GET /api`, `GET /api/dashboard`) — the response is only `_links`, so a plain
  JSON sibling would be an empty object. These declare `application/hal+json` *and*
  `application/prs.hal-forms+json` instead: two HAL variants, no payload.
- **Non-hypermedia endpoints** (`/api/auth/password-setup/*`, `/api/oris/events`) — plain JSON only.
  The auth endpoints are called before login, where hypermedia has nothing to offer; `oris` proxies an
  external API.
- **Non-JSON feeds** (`/ical/my-schedule.ics` → `text/calendar`).
- **Endpoints whose schema is an envelope carrying `_embedded`** (`GET /api/events/{id}`,
  `GET /api/membership-fee-groups/{id}`, schemas like `EntityModelEventDtoWithRegistrations`). There
  is no plain payload sibling to declare, because the `_embedded` collection is a different *shape* of
  data rather than a different serialisation of the same payload.

  **Treat that last group as debt, not a pattern to copy.** Declaring the envelope as the schema
  predates structural unwrapping and keeps these two endpoints outside it. Do not spell a new endpoint
  this way — model the payload and let the advice assemble `_embedded` at runtime (the backend side is
  `HalResponseContext.embed`, see the `backend-patterns` skill).

## `mappings` is now only for hand-written overrides the generator cannot derive from spec structure

Envelope→payload redirection needs **zero** `mappings`/`extraImportMappings` entries — the structural
detection above handles every `EntityModel*`/`PagedModel*`/`CollectionModel*` schema in the spec, by
shape, with no naming convention to follow. A `mappings` entry in `openApiModule(...)` is only for a
case the generator genuinely cannot infer from the spec alone:

- **A nested Java class.** `PaymentRuleResponse` → `MembershipFeeTierResponse.PaymentRuleResponse` —
  the schema name matches a top-level class name that isn't the one actually used.
- **A domain enum redirect.** `Authority` → `com.klabis.common.users.Authority` — without the
  mapping the generator synthesizes its own duplicate enum class instead of reusing the domain one.
- **A cross-module application type.** `BulkSyncResult` → `com.klabis.events.application.BulkSyncResult`
  — the target package differs from the module's own `restapi` package.
- **A marker type with no payload of its own.** `EntityModelRootModel` → `common.ui.RootModel`,
  `EntityModelDashboardModel` → `common.ui.DashboardModel` — shaped as
  `{type: object, properties: {_links}}` (no `allOf`, no `_embedded`), which
  `HalEnvelopeDetector` **deliberately does not match** — see "Payload and envelope are separate
  schemas" below for why these two are legitimately envelope-only.
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

**`models` still needs every payload schema listed explicitly, per module.** Structural envelope
unwrapping is unrelated to *discovery* — a proposal to have the generator discover a module's schemas
by tag reachability was investigated and withdrawn (design.md Decision 5: the generator exposes no
hook for it; model filtering lives entirely in `DefaultGenerator`, outside any `CodegenConfig`
override point). Adding a new request/response DTO to an already-migrated module still means adding
its schema name to that module's `models` list in `openApiModule(...)`, exactly as before.

### `frontend/src/api/halTypes.ts` must still type off the HAL envelope, not an `application/json` sibling

`haltypes.mjs` builds each `*Resource` type by picking a schema out of the response's `content` map.
Because the bundler alphabetizes content-type keys, `"application/json"` always sorts ahead of
`"application/prs.hal-forms+json"` — so without an explicit preference, declaring both content types
would silently retype `*Resource` off the *bare* payload/array instead of the envelope, dropping
`_embedded`/`page` from the generated type even though the wire response still has them.
`haltypes.mjs` explicitly prefers `application/prs.hal-forms+json`/`application/hal+json` over any
other content type (falling back only if neither is present) — this is handled centrally, so nothing
extra is required per-endpoint. But if a `*Resource` type ever looks wrong, this is the first thing
to suspect, and the fix belongs in `haltypes.mjs`, not in per-endpoint content ordering (ordering in
the YAML doesn't survive bundling anyway — keys are re-sorted).

### A response already declaring more than one HAL-ish content type may reject a third

Adding `application/json` to a response that already lists **two** content types pointing at the same
schema (`application/hal+json` *and* `application/prs.hal-forms+json` — the `common` module's
`rootNavigation`/`dashboard` are the only endpoints in the spec that do this) can make the generator's
return-type resolution give up and collapse the method to `ResponseEntity<Void>`, silently breaking
the controller's `@Override`. Confirmed empirically (generating with two vs. three content types on
the same schema) — there's no known clean fix, so **skip the `application/json` sibling** for a
response in this situation and leave a comment explaining why, the same way `common.yaml` does for
`rootNavigation`/`dashboard`. This is rare: it only arises for a marker-type response with no real
payload of its own, which is the same shape that makes the sibling low-value anyway.

### A test without an explicit `Accept` header may start asserting against the wrong content type

Content negotiation for a response now has a plain `application/json` option alongside HAL-FORMS. A
`MockMvc` test asserting `$._links...`/`$._templates...` that omits `.accept(...)` can start resolving
to the bare payload instead, failing those assertions even though the endpoint still serves HAL-FORMS
correctly to the real frontend (which always sends `Accept: application/prs.hal-forms+json`). Fix by
adding the header explicitly:

```java
mockMvc.perform(get("/api/users/{id}/permissions", id).accept(MediaTypes.HAL_FORMS_JSON))
    .andExpect(jsonPath("$._links.self.href").exists());
```

Same root cause as "Bodyless success responses" above (a test without `.accept(...)` "passes either
way" until the set of producible media types changes) — check for this whenever migrating a module
whose controller tests assert on `_links`/`_templates`.

## A payload schema's name is wire contract

Spring HATEOAS derives the `_embedded` key of a collection from the **payload class name** at
runtime, and that class name comes from the schema name in the spec:

```
schema EventTypeDto  ->  record EventTypeDto  ->  "_embedded": { "eventTypeDtoList": [...] }
```

So renaming a payload schema renames a JSON key that clients read literally — a breaking change, not
a rename. The envelope schema must spell the same key:

```yaml
CollectionModelEntityModelEventTypeDto:
  type: object
  properties:
    _embedded:
      type: object
      properties:
        eventTypeDtoList:          # <camelCase payload class name> + "List"
          type: array
          items:
            $ref: '#/components/schemas/EntityModelEventTypeDto'
```

**`@Relation` overrides that default.** The class-name rule above only holds when the payload class
carries no `@Relation`. When it does, the annotation wins and the spec must spell the annotation's
value:

```java
@Relation(collectionRelation = "transactions", itemRelation = "transaction")
public record TransactionResource(...) { }
```
```yaml
_embedded:
  type: object
  properties:
    transactions:              # from @Relation, NOT "transactionResourceList"
```

So before writing an envelope, grep the payload class for `@Relation` rather than deriving the key
from its name. Applying the default to a class that overrides it produces a key no response ever
contains, and — per the second guard below — nothing reports it.

Two guards, both worth knowing because each fails at a different moment:

- **`halTypes.ts` indexes into `klabisApi.d.ts`** by schema name
  (`components['schemas']['EntityModelEventTypeDto']`). A name that does not exist there is a `tsc`
  error — loud, immediate. Since the bundler owns `klabis-full.json`, that name comes from the spec,
  so the fix is to correct the schema name there and regenerate — never to edit `klabisApi.d.ts`.
- **The `_embedded` key is not checked by anything.** Getting it wrong in the envelope produces
  frontend types naming a property no response ever contains: `undefined` at runtime, an empty list
  in the UI, no error anywhere.

If a rename looks harmless because the tests stayed green, check whether a test was edited in the
same change. Adjusting a `$._embedded.*List` JSON path *is* the breakage surfacing — not the fix.

## `x-hal-*` — declaring links and affordances

On the **response object**, not on the schema — links describe the representation, not the payload.

```yaml
responses:
  '200':
    content:
      application/prs.hal-forms+json:
        schema: { $ref: '#/components/schemas/EntityModelMemberDetailsResponse' }
    x-hal-links:
      self: { description: This member }
      collection: { operation: listMembers, description: Back to the member list }
      permissions: { description: 'Requires MEMBERS:PERMISSIONS' }
    x-hal-templates:
      default: { operation: updateMember }
      suspend: { operation: suspendMember, description: Present only while the member is active }
```

Reference operations by `operation: <operationId>` — never `operationRef` with escaped slashes.
The bundler validates that the target exists.

### What the frontend gets from them

`npm run openapi` generates `frontend/src/api/halTypes.ts` from these declarations — per operation a
`*Rels` constant, a `*Hal` interface and a `*Resource` type intersecting the payload schema:

```ts
import {GetMemberRels} from '@/api/halTypes';
GetMemberRels.links[0]        // 'account' — typed, not a bare string literal
```

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
