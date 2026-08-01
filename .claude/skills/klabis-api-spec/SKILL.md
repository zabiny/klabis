---
name: klabis-api-spec
description: Authoring the hand-written OpenAPI spec in docs/openapi/spec/ — x-klabis-* field-security and x-hal-* hypermedia extensions, module layout, and the spec-first workflow. Use whenever adding, changing or removing a REST endpoint, request/response field, HAL link or HAL+FORMS template; when writing the API chapter of an OpenSpec design.md; or when migrating a module from code-first to spec-first.
user-invocable: false
version: 0.4.0
---

# Klabis API Spec

`docs/openapi/spec/` is **the** source of truth for the REST API: Java DTOs, API interfaces and
frontend HAL types are all generated from it.

Never edit generated output — `docs/openapi/klabis-full.json`, `build/generated/**`,
`frontend/src/api/klabisApi.d.ts`.

The spec drives real codegen: payload DTOs, the `*Api` interfaces controllers implement, endpoint
authorization, and the frontend HAL types. The Java signature is a consequence of the spec, never the
driver.

```
./gradlew openapiBundle                          # from backend/ — spec/ -> klabis-full.json
./gradlew openapiBundle -PopenapiCheck           # validate only, write nothing
```

### `klabis-full.json` is generated from the spec

`docs/openapi/klabis-full.json` is the published API document — Swagger UI and the frontend's
`klabisApi.d.ts` are built from it. `openapiBundle` produces it from `docs/openapi/spec/`, so there
is a single source of truth end to end.

- **`klabis-full.json` is a gitignored build artifact, not a review target.** Review the spec;
  regenerate the bundle. Never hand-edit it.
- **A schema name in the spec is a wire contract** — `halTypes.ts` indexes into `klabisApi.d.ts` by
  schema name. See "A payload schema's name is wire contract" below; this is why `event-types` keeps
  the `*Dto` suffix.
- Changing the spec requires regenerating **both** the bundle (`./gradlew openapiBundle`) and the
  frontend types (`npm run openapi` from `frontend/`).
- **Never put `@Operation` / `@ApiResponse` / `@Parameter` on a controller** — see below.

#### Why the controllers carry no springdoc annotations

`documentationProvider = "springdoc"` makes the generator emit them onto the `*Api` interface,
straight from the spec. Springdoc reads annotations off the concrete class and Java does not inherit
method annotations from an interface — but the controller `@Override`s a method whose *declaration*
carries them, and springdoc resolves that. So `/v3/api-docs` is spec-derived too, and hand-written
duplicates are pure drift surface.

The generated set is strictly richer than what the controllers used to hold: it also carries
`security`, `tags`, every error response with its description and `@Content` schema, and
`@Parameter(hidden = true)` on the `x-spring-provide-args` arguments. Measured over the whole app
when the 26 controllers were stripped: operations 117 → 117 (none lost), summaries 82 → 117,
descriptions 51 → 111, parameter descriptions 95 → 133.

**The catch is generic `schemaMappings`.** `documentationProvider` renders each response's baseType
into `@Schema(implementation = <baseType>.class)`, and the envelope mappings target *generic* types,
producing `java.util.Collection<…EventTypeDto>.class` — not legal Java (JLS 15.8.2: a class literal
takes a raw type). 27 occurrences across 5 modules. The generator has no hook for it: stock
`api.mustache` emits `{{{baseType}}}` verbatim, Mustache cannot transform a string, and a lambda
would mean subclassing `SpringCodegen`. `openApiModule` therefore erases the type arguments in a
`doLast` over the generated `*Api.java`. That erasure is declared via `inputs.property` — a `doLast`
body is not part of a task's cache fingerprint, so without it a cached output could be restored
having been patched by an older version of the rule.

The exceptions are the 7 files with no generated counterpart, which legitimately keep their
annotations: `OpenApiConfig` (global info + security scheme), `MvcExceptionHandler` and
`GroupsExceptionHandler`, the hand-written `MemberOptionResponse`/`MemberSummaryResponse`, and
`@Hidden` on `ActingUser`/`ActingMember`.

## Layout

```
docs/openapi/spec/
  klabis.yaml          root: info, servers, securitySchemes, $ref per path to module files
  _shared/
    hal.yaml           Link, Links, PageMetadata, HalFormsTemplate(s), HalFormsProperty, HalFormsOptions
    problem.yaml       RFC 7807 ProblemDetail
  members.yaml         one file per module
  event-types.yaml
```

`klabis.yaml` references each path individually, with JSON-pointer escaping (`/` is `~1`):

```yaml
paths:
  /api/members/{id}:
    $ref: './members.yaml#/paths/~1api~1members~1{id}'
```

A module file owns the endpoints of **its own module only**. `/api/members/{memberId}/account` looks
like a members path but belongs to `finance` — it goes in `finance.yaml`. Decide by which controller
serves it, not by the URL prefix.

Reusable parameters and error responses go in the module's own `components`; only genuinely
cross-module building blocks belong in `_shared/`.

**Component names are global once bundled, even though they are defined per module.** Files are
namespaced; `components.parameters` / `components.responses` / `components.schemas` entries are not —
bundling flattens them all into one namespace. Two modules defining the same name with different
content is a hard bundle failure (the message names the same file twice, which reads as nonsense
until you know it is a cross-file collision):

```yaml
# members.yaml            # finance.yaml
MemberIdParam:            MemberIdParam:
  name: id                  name: memberId      # same name, different shape -> bundle fails
```

Prefix module-owned components rather than reaching for the generic name: `AccountMemberIdParam`,
not `MemberIdParam`. This applies to `responses` too — a shared error response whose `description`
wording differs from another module's copy of the same name collides the same way, so match the
existing wording exactly or pick a distinct name.

A name collision can also surface far from its cause: if a colliding parameter component resolves to
the *other* module's definition, `x-klabis-owner-visible: <name>` then fails with "not declared on
that operation" even though the operation's own file looks correct. Check for a duplicate component
name before believing the error at face value.

## Validation lives in the spec too

Bean-validation constraints are generated from standard OpenAPI keywords, so they belong in the spec
alongside the types:

| spec | generates |
|---|---|
| `required: [firstName, …]` | `@NotNull` (not `@NotBlank` — see below) |
| `x-klabis-not-blank: true` | `@NotBlank` (Klabis extension; schema properties only) |
| `x-klabis-past: true` | `@Past` (Klabis extension; schema properties only) |
| `x-klabis-url: true` | `@URL` (Klabis extension; schema properties only) |
| `x-klabis-class-constraint: <FQN>` | that annotation on the record itself — cross-field rules |
| `maxLength` / `minLength` | `@Size(max=…, min=…)` |
| `pattern` | `@Pattern(regexp=…)` |
| `format: email` | `@Email` |
| `minimum` / `maximum` | `@Min` / `@Max` |
| `type: [x, 'null']` | `JsonNullable<X>` — PATCH tri-state, see below |

A schema that omits them produces a DTO that accepts anything — the failure shows up as a controller
test expecting `400` and getting `200`, or as missing entries under `fieldErrors`. When migrating a
module, transcribe the Jakarta annotations off the hand-written record; springdoc reports most of
them in `klabis-codefirst.json` already.

**`required` is not `@NotBlank`.** In OpenAPI `required` only means the key must be present, so it
generates `@NotNull` — which accepts `""`. Adding `minLength: 1` gets you `@Size(min = 1)`, which
rejects `""` but still accepts `"   "`. OpenAPI has no standard keyword meaning "not blank".

Klabis therefore has its own: **`x-klabis-not-blank: true`** on the property, emitted as `@NotBlank`
by the overridden `pojo.mustache`. Use it wherever the hand-written record had `@NotBlank`; the field
also keeps the redundant `@NotNull` from `required`, which is harmless.

**`x-klabis-past: true`** works the same way for `@Past`, which OpenAPI likewise cannot express
(`format: date` says nothing about the range), and **`x-klabis-url: true`** for `@URL`. All three
live in `PROPERTY_ONLY_CONSTRAINT_EXTENSIONS` in `validate.mjs`; adding a fourth constraint of this
kind means one entry there plus one branch in `pojo.mustache`.

**Do not pair `x-klabis-url` with `format: uri`.** That format makes the generator emit
`java.net.URI`, and Hibernate's `@URL` constrains `CharSequence` — the combination changes the Java
type out from under the mapper and the constraint silently never applies. Leave the property a plain
string and let the extension carry the validation.

**Check the schema is actually generated before converting a `pattern` hack to it.** Not every
schema in the spec has a generated counterpart: one that is mapped away via `schemaMappings`, or
simply absent from a module's `models` allow-list, is documented for the frontend but produces no
Java. There the extension emits nothing and the validation still lives in a hand-written annotation,
so swapping `pattern: '^(?!\s*$).+'` for `x-klabis-not-blank` is a pure regression.
`find backend/build/generated/openapi -name '<Schema>.java'` settles it.

### Cross-field rules: `x-klabis-class-constraint`

A rule spanning two properties ("these deadlines must be non-decreasing") has no OpenAPI keyword, and
the `@AssertTrue` accessor that used to express it cannot survive migration — a generated record has
no method bodies. Write a class-level Bean Validation constraint instead and name it on the schema:

```yaml
UpdateEventRequest:
  type: object
  x-klabis-class-constraint: com.klabis.events.infrastructure.restapi.DeadlinesOrdered
```

The value is a fully-qualified annotation name **without** the leading `@`; `pojo.mustache` renders it
above the record. Unlike `additionalModelTypeAnnotations` — which applies to every model in the task —
this is keyed on the individual schema.

The annotation is rendered with **no argument list**, so all its members must have defaults; a
constraint needing a mandatory attribute means extending the template block to carry arguments.

Two things the validator must handle, both learned from `DeadlinesOrderedValidator`:
- **It must be `public`.** Hibernate's default factory instantiates validators reflectively and
  rejects a package-private class with `HV000064`.
- **Re-anchor the violation on the property** via `addPropertyNode`, or the 400 response stops naming
  the offending field and just reports a class-level error.

Reading the value usually means reflection over the record component, since the same rule tends to
apply to both a POST and a PATCH request — and on the PATCH side it arrives wrapped in
`JsonNullable<T>`.

Two limits:
- **Schema properties only.** Only `pojo.mustache` has a branch for it, so the generator would drop
  the extension on a `parameters` entry; `validate.mjs` rejects it there rather than letting it pass as
  a silent no-op. For a constrained `@RequestParam` use `pattern: '^(?!\s*$).+'` instead — see the
  `validatePasswordSetupToken` `token` parameter in `common.yaml`.
- **The custom message is still lost.** `@NotBlank(message = "…")` texts do not survive; assertions
  must expect the Bean Validation default (`"must not be blank"`).

**Validation messages become the Bean Validation defaults** (`"size must be between 1 and 100"`).
OpenAPI cannot express a custom message, so hand-written `@NotBlank(message = "…")` texts are lost on
migration. Update the assertions to the default text rather than contriving a way to keep the old
one — the constraint still fires, only the wording changes.

## PATCH bodies: nullable properties become `JsonNullable<T>`

A PATCH body needs three states, not two: *absent* (leave the field alone), *present and null* (clear
it), and *present with a value*. A plain Java field cannot express that — `null` would mean both
"untouched" and "clear". Marking the property nullable generates `JsonNullable<T>`, which can.

**Use the OpenAPI 3.1 spelling.** The specs are `3.1.0`, where the 3.0 `nullable: true` keyword is
**silently ignored** — the property stays non-nullable, the wrapper never appears, and nothing warns
(`skipValidateSpec` is on):

```yaml
name:
  type: ['string', 'null']          # -> JsonNullable<String>
  maxLength: 100

ageRange:                            # a $ref property needs the oneOf form
  oneOf:
    - $ref: '#/components/schemas/AgeRangeRequest'
    - type: 'null'                   # -> JsonNullable<AgeRangeRequest>
```

`nullable: true` and `allOf` + `nullable` both generate the bare type. This is driven by
`openApiNullable = "true"` in `openApiModule(...)` plus the `isNullable` branch in the overridden
`pojo.mustache`; if that template is ever re-forked from upstream, port the branch or every PATCH DTO
silently loses its wrappers.

Only PATCH bodies want this. A nullable response property or POST/PUT body would generate the
wrapper too, forcing an unwrap on every read for a distinction those payloads do not have. Several
response schemas still carry a leftover `nullable: true`; it is inert only because the 3.0 keyword is
ignored, so do not "modernise" them to the 3.1 spelling.

### `oneOf` strips property-level `x-klabis-*`

The generator discards vendor extensions from any property written as a `oneOf`. This is a property
of the composition keyword itself, not of what it contains — a scalar
`oneOf: [{type: string}, {type: 'null'}]` loses them just as a `$ref` branch does, even though the
union spelling `type: ['string', 'null']` keeps them.

That is only a dilemma for `$ref` properties, since a scalar can always use the union spelling
instead. **Neither composition keyword is a way out**, and `allOf` is a trap worth naming, because
it looks like one:

```yaml
gender:
  allOf:                             # keeps x-klabis-authority — but generates a bare Gender
    - $ref: '#/components/schemas/Gender'
  x-klabis-authority: MEMBERS_MANAGE
```

`allOf` does keep the extension, but it costs the `JsonNullable` wrapper — and
`RequestBodyFieldAuthorizationAdvice` **skips every component that is not `JsonNullable`-typed**, so
the `@HasAuthority` it emits is never evaluated. Keeping the annotation while losing the wrapper
protects nothing. This shipped: `UpdateMemberRequest.gender` was written exactly this way, and
`MEMBERS:MANAGE` went unenforced on it until 2026-07-31.

**Inline the type instead**, so the property keeps both:

```yaml
gender:
  type: ['string', 'null']
  enum: ['MALE', 'FEMALE', null]
  x-klabis-authority: MEMBERS_MANAGE
```

Inlining loses the schema name that `schemaMappings` keys on, so add an entry for the generated
`<Parent>_<property>` name (`UpdateMemberRequest_gender` → `com.klabis.members.domain.Gender`) to
keep the domain type. `PatchRequestWrapperArchitectureTest` pins both halves: every component is
wrapped, and the exact set of components carrying an authority.

### Consuming the tri-state

`JsonNullable.map` applies the mapper to a *present null* — it branches on presence, not on nullness.
A mapper that dereferences the value turns an intended `400` into an NPE and a `500`:

```java
// forwards the null so the domain's own Assert rejects it
field.map(value -> value == null ? null : convert(value))
```

`orElse(current)` is the "apply patch" primitive: the wrapped value when present (including null),
the fallback when absent.

## Core rule: DTOs carry wire types

API DTOs are transport records mirroring the JSON payload. `string`/`format: uuid`, never a
`MemberId` object. Conversion to domain types belongs in the mapper or the controller.

The spec describes JSON — a domain type is not derivable from it, and a generated DTO cannot invent
one.

## `x-klabis-*` — field-level security

On schema properties. Each maps to exactly one existing Java annotation.

(`x-klabis-authority` and `x-klabis-owner-visible` also work one level up, on an operation, and
`x-klabis-owner-id` also works on a path parameter — see
[endpoint authorization](#x-klabis-authority-on-an-operation--endpoint-authorization) below. The
remaining one is property-only and the bundler rejects it on an operation.)

| extension | value | generates | semantics |
|---|---|---|---|
| `x-klabis-owner-id` | `true` | `@OwnerId` | Marks the field — or, on an operation's path parameter, the parameter — holding the owner's ID, used to evaluate `x-klabis-owner-visible`. On a property without it, the single UUID-convertible field is used. |
| `x-klabis-owner-visible` | `true` | `@OwnerVisible` | Visible/permitted to the owner even without the authority (OR semantics with `x-klabis-authority`; **alone = owner-only**). |
| `x-klabis-authority` | e.g. `MEMBERS_MANAGE` | `@HasAuthority(Authority.MEMBERS_MANAGE)` | Requires the authority. Must be a constant of `Authority.java`. |
| `x-klabis-halforms-access` | `READ_ONLY` \| `NONE` \| `READ_WRITE` \| `DEFAULT` | `@HalForms(access = …)` | Controls `readOnly` in HAL+FORMS `_templates`. |

```yaml
MemberDetailsResponse:
  type: object
  properties:
    id:
      type: string
      format: uuid
      x-klabis-owner-id: true
    dateOfBirth:
      type: string
      format: date
      x-klabis-authority: MEMBERS_MANAGE
      x-klabis-owner-visible: true   # admins OR the member themselves
```

Enforcement lives in `FieldSecurityBeanSerializerModifier`, which works **only on records** and reads
annotations off the accessor method. A denied field is omitted or masked per
`@HandleAuthorizationDenied`.

## `x-klabis-authority` on an operation — endpoint authorization

The authority an endpoint requires belongs in the spec, not on the controller. On an operation it
generates `@HasAuthority` on the **generated interface method**:

```yaml
paths:
  /api/members/{id}:
    get:
      operationId: getMember
      x-klabis-authority: MEMBERS_READ    # -> @HasAuthority(Authority.MEMBERS_READ) on MembersApi.getMember
```

The overridden `api.mustache` reads this key directly and emits the annotation above the method —
the same way `pojo.mustache` reads it off a schema property. Nothing rewrites it, so the published
`klabis-full.json` carries the spec key alone, not a Java string derived from it.

Do not also annotate the controller. The authority is stated once, in the spec; a second copy in
Java is what this replaces.

This relies on `MethodSecurityAnnotations`, which resolves security annotations across the interface
boundary — Java does not inherit method annotations from an interface, so without it the generated
annotation would compile and silently enforce nothing.

### `x-klabis-owner-visible` on an operation — ownership authorization

`@OwnerVisible` and `@OwnerId` are a pair: `HasAuthorityMethodInterceptor.checkOwnership()` scans the
method's parameters for the one carrying `@OwnerId` to know whose ownership to check.
`@OwnerVisible` on a method without a matching `@OwnerId` parameter enforces nothing — it denies
rather than resolving ownership, silently dropping the owner-or-authority semantics the endpoint
advertises. The two halves are declared on two different nodes:

```yaml
paths:
  /api/members/{id}:
    patch:
      operationId: updateMember
      x-klabis-authority: MEMBERS_MANAGE
      x-klabis-owner-visible: true       # -> @OwnerVisible on the method
      parameters:
        - $ref: '#/components/parameters/MemberIdParam'

components:
  parameters:
    MemberIdParam:
      name: id
      in: path
      required: true
      x-klabis-owner-id: true            # -> @OwnerId on the parameter
```

`api.mustache` emits `@OwnerVisible`, `pathParams.mustache` emits `@OwnerId`, and neither can see
the other — so **`validate.mjs` is the only thing keeping the pair together.** It requires an
operation declaring `x-klabis-owner-visible` to have exactly one parameter marked
`x-klabis-owner-id` (zero denies; two would silently resolve against whichever came first).

**`x-klabis-owner-id` may sit on a `$ref` parameter shared with operations that never opt into
ownership.** `MemberIdParam` is used by `getMember`, `updateMember`, `suspendMember` and
`resumeMember`, but only `updateMember` declares `x-klabis-owner-visible`; the other three get an
inert `@OwnerId`. That is harmless because nothing reads `@OwnerId` on its own — both
`checkOwnership()` and `RequestBodyFieldAuthorizationAdvice` only consult it for a method or field
already marked `@OwnerVisible`. `@OwnerId` on a parameter is only allowed on a **path** parameter —
`pathParams.mustache` is the only parameter template with a branch for it, so anywhere else the key
would be silently dropped. `validate.mjs` rejects that too, along with an owner-id parameter that
`x-spring-paginated` would fold into `Pageable` (`page`/`size`/`sort` are query parameters, so the
same check covers them).

Combined with `x-klabis-authority`, this reproduces the OR semantics used everywhere else in the
codebase (MANAGE authority OR ownership) — see `FieldLevelAuthorizationTest` /
`MemberControllerApiTest` for the enforcement tests.

**Declared alone, it means owner-only.** The OR is with whatever authority is declared, so with none
declared there is nothing to OR against: `HasAuthorityMethodInterceptor.invoke()` computes
`authorityGranted` as `requiredAuthority != null && hasAuthority(...)`, leaving ownership the sole
path to `proceed()`. A lone `x-klabis-owner-visible` therefore *narrows* access to the owner rather
than widening it, and is the right way to model "only the member themselves, no MANAGE alternative"
— `MemberFeeChoice`'s and `MemberFeeSummary`'s 5 operations use exactly this. Do not reach for an
imperative controller check for that case.

Such operations need a test asserting that a caller holding the module's MANAGE authority is still
`403` (see `MemberFeeChoiceControllerTest`). Nothing else in the suite distinguishes owner-only from
owner-OR-MANAGE, so pairing an authority in later would widen access silently.

**Nothing requires an operation to declare an authority.** A missing `x-klabis-authority` generates
a method without `@HasAuthority` and no check reports it; the endpoint still requires
authentication (`/api/**` is `.authenticated()`), but loses its authority check. When adding an
operation, state the authority deliberately.

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

### The response must reference the envelope — always

A response references the `EntityModel*` / `PagedModel*` / `CollectionModel*` schema, never the bare
payload or a bare array. The envelope is what actually goes on the wire; the plain Java signature is
arranged separately, by mapping the envelope schema onto an existing type in `build.gradle.kts`:

| envelope schema | `schemaMappings` target | controller returns |
|---|---|---|
| `EntityModelFooResponse` | `…restapi.FooResponse` | `FooResponse` |
| `PagedModelEntityModelFooResponse` | `org.springframework.data.domain.Page<…FooResponse>` | `Page<FooResponse>` |
| `CollectionModelEntityModelFooDto` | `java.util.Collection<…FooDto>` | `Collection<FooDto>` |

It is tempting to reference a plain `type: array` instead, because that yields a `List<T>` signature
directly. Don't: the spec then advertises a bare JSON array while the endpoint actually returns a HAL
object with `_embedded`. Nothing fails at build time — envelopes are never generated into Java — so
the lie surfaces only as frontend types describing a response shape that never occurs.

**`List` cannot be a `schemaMappings` target.** `List` is a reserved container name in the
generator's type system: a mapping onto `java.util.List<...>` is dropped *silently*, and the method
generates as `ResponseEntity<>` — a syntax error with no diagnostic pointing at the mapping. Use
`java.util.Collection<...>`. Both the advice and the postprocessors only iterate the value, so
nothing downstream cares.

Generic mappings also need a matching `importMappings` entry carrying the raw type only
(`java.util.Collection`, `org.springframework.data.domain.Page`) — the import statement must not
repeat the type arguments.

### A payload schema's name is wire contract

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

## `x-hal-*` — hypermedia

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
2. `./gradlew compileJava` — regenerates DTOs and `*Api`, then fails on whatever no longer matches.
   This is the real check: the generator runs off `bundleSpecForCodegen`, not off `openapiBundle`.
3. Fix the controller against the regenerated interface
4. `cd frontend && npm run openapi`
5. `npx tsc --noEmit -p tsconfig.app.json` — catches schema-name mismatches in `halTypes.ts`

`./gradlew openapiBundle` validates extensions and refs without generating anything; useful for a
fast syntax check, but passing it does not mean the code compiles.

If step 3 shows the spec is wrong, go back to step 1. **Never adjust the spec to match existing Java
just to silence a mismatch** — that reintroduces code-first through the back door. The exception is a
genuine spec bug, which is a spec change like any other.

## Registering a module for codegen

Writing the YAML generates nothing on its own. Each module gets its own codegen task, registered with
`openApiModule(...)` in `backend/build.gradle.kts`:

```kotlin
openApiModule(
    module = "event-types",                                  // -> build/generated/openapi/event-types
    pkg = "com.klabis.events.infrastructure.restapi",         // same package as the controller
    apis = listOf("EventTypes"),                              // OpenAPI tags
    models = listOf("EventTypeDto", "CreateEventTypeRequest", "UpdateEventTypeRequest"),
    mappings = mapOf(
        "EntityModelEventTypeDto" to "com.klabis.events.infrastructure.restapi.EventTypeDto",
        "CollectionModelEntityModelEventTypeDto" to "java.util.Collection<…EventTypeDto>"
    ),
    extraImportMappings = mapOf("CollectionModelEntityModelEventTypeDto" to "java.util.Collection")
)
```

One task **per module**, not one shared task: `modelPackage`/`apiPackage` are scalars and
`schemaMappings` is global per task, so a single task could never let two modules each define their
own `AddressRequest`.

`pkg` must be the package the hand-written controller already lives in — cross-module link processors
reach these types through Modulith named interfaces.

**`apis` must list the tags explicitly.** The underlying `apis` global property generates *every*
tag when given an empty string, which would emit every other module's `*Api.java` into this module's
package. Forgetting a tag is safe by comparison: the interface is simply not generated and
`implements XApi` fails to compile.

**Tags must be single words.** A tag containing a space (`Calendar Feed Token`, `Event
Registrations`, `My Profile`) is silently dropped: the build succeeds, no warning is printed, and the
interface simply never appears. Watch for a trailing space too — `"Members "` is not `"Members"`.
Existing controllers carry several multi-word `@Tag` names, so when migrating one, give the spec a
single-word tag (`IcalToken`, not `Calendar Feed Token`) and use that same string in `apis`. The tag
is spec-side only, so renaming it changes neither the wire nor `klabis-full.json`, which takes its
tags from `@Tag` on the controller.

**The generator never deletes.** It only writes, so a schema you rename or drop leaves its old record
behind in `build/generated/openapi/<module>/`— and since that directory is on `sourceSets.main`, the
ghost keeps compiling. Local builds stay green while a clean CI build fails. `openApiModule` handles
this with `doFirst { delete(outputDir) }`; keep it when touching that function.

## Adding a module

Every existing module is already spec-first; this is the recipe for a genuinely new one, and the
reference for how the existing ones are put together.

1. `./gradlew generateOpenApiDocs` then read `docs/openapi/generated/klabis-codefirst.json` — only
   useful if the endpoints already exist in Java; for a new module, skip to step 3
2. That dump is the ground truth for parameters, request bodies and status codes
3. Write `<module>.yaml`; add one `$ref` per path to `klabis.yaml`. Name payload schemas after the
   existing Java DTO classes — see "A payload schema's name is wire contract"
4. Add `x-hal-links` / `x-hal-templates` by reading the controller's postprocessors and
   `RepresentationModelProcessor` implementations — springdoc cannot see them, so the drift check
   will not catch a missing one
5. Transcribe each `@HasAuthority` off the controller into `x-klabis-authority` on the matching
   operation, then delete it from the controller. Compare the generated `*Api` interface against the
   controller as it was — an authority that silently changes or disappears here is not something the
   tests will necessarily catch
   Authorization is not always an annotation. A controller may enforce it **imperatively** — a
   private `checkXxxAccess()` throwing `AccessDeniedException`, typically "owner OR MANAGE
   authority". That is the `x-klabis-authority` + `x-klabis-owner-visible` pair; move it into the
   spec and delete the helper. A helper that permits *only* the caller themselves, with no authority
   alternative, is `x-klabis-owner-visible` on its own — declaring it alone does not widen access
   (see that extension's section). Read each method body before concluding an endpoint is
   unprotected, because an imperative check is invisible both to reflection and to the drift check.
6. Register the module with `openApiModule(...)` (above), then `./gradlew compileJava`
7. Rework the controller: implement the generated `*Api`, return plain payloads, and register the
   domain objects with `HalResponseContext` (below).
   **Strip the path from the class-level `@RequestMapping`.** Generated interface methods carry the
   full absolute path, so a controller that still declares `@RequestMapping(value = "/api/foo")`
   makes Spring concatenate the two into `/api/foo/api/foo` and every endpoint 404s. Keep the
   annotation for `produces` only: `@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)`,
   as `MemberAccountController` and `EventTypeController` do.
8. Re-run the drift check until the module reports `mismatched: 0`
9. `cd frontend && npm run openapi`, then `npx tsc --noEmit -p tsconfig.app.json`

### Returning plain payloads

A generated interface method returns the payload, not a `RepresentationModel`. The controller stores
the domain object(s) in `HalResponseContext`; `HalResponseBodyAdvice` picks them back up, builds the
`EntityModel`/`PagedModel`/`CollectionModel`, and runs the existing postprocessors:

```java
HalResponseContext.setDomain(eventType);          // single
HalResponseContext.setDomainList(eventTypes);     // collection or page — paired 1:1 by index
```

Links and affordances that belonged to the **collection itself** cannot be built in the method any
more (there is no model to add them to). They move into a
`RepresentationModelProcessor<CollectionModel<EntityModel<T>>>`; the advice contributes the self
link, the processor contributes affordances via `klabisAfford*` so they stay authorization-sensitive.

Controllers that still build their own models are untouched — without an entry in
`HalResponseContext` the advice passes the body through unchanged.

### Request bodies bound to domain types

Some controllers deserialize straight into a domain command
(`@RequestBody EventType.CreateEventType`). That violates "DTOs carry wire types" and cannot survive
migration: generate a `CreateFooRequest` DTO from the spec and add a mapper to the domain command.
Keep the domain record — the domain and its service still use it, it just stops being the
deserialization target.

**The drift check compares schemas by name, not by content.** Two schemas called
`RegisterMemberRequest` match even when their properties differ wildly. After the check goes green,
diff the module's schemas property-by-property against `klabis-codefirst.json` — that is where
transcription mistakes actually surface.

Expect the springdoc output to be wrong in places — it does not know about `@JsonValue` mixins, and
it introspects Java types rather than the wire, so a wrapper like `JsonNullable<T>` can surface as an
object with the wrapper's own fields rather than as the value it serializes to. Where it disagrees
with the actual wire format, the spec follows the **wire**, and the discrepancy is documented in the
spec rather than mirrored.

### A newly annotated method can fail a link/affordance unit test

Moving authorization into the spec makes it **discoverable by reflection** for the first time. That
can break a passing unit test without any behaviour changing, and the failure looks alarming — a HAL
link silently disappears.

The cause is in `HalFormsSupport`: every `klabisLinkTo` / `klabisAfford*` guard reads
`INSTANCE != null && !INSTANCE.isMethodAuthorized(...)`. `INSTANCE` is a static set by
`@PostConstruct`, so in a plain unit test with no Spring context it is null and **authorization is
skipped entirely**. Such a test passes without ever exercising the check. Once the target method
carries `@HasAuthority` / `@OwnerVisible`, a leftover `INSTANCE` from another test class's context in
the same fork activates the real check — and `isMethodAuthorized` returns `false` unless an
`OwnershipResolver` is actually available, since the ownership branch falls through to `return false`
when `ownershipResolverProvider.getIfAvailable()` is null.

Symptom: the test passes standalone and fails when run after any `@SpringBootTest` in the same fork.

Fix the test, not the assertion — and verify the production behaviour separately (the module's
MockMvc controller test with `@WithKlabisMockUser` is the real evidence, since it exercises genuine
authentication). Either wire a real `OwnershipResolver` into a `HalFormsSupport` and set `INSTANCE`
for the test's duration (`AccountRootLinkProcessorTest` and `AccountMemberDetailLinkProcessorTest`
do this, and must restore the previous value afterwards or they leak the same problem onward), or use
the `@WebMvcTest` + `@Import(HalFormsSupport.class)` + `@WithKlabisMockUser` slice that
`AffordanceAuthorizationTest` uses. Give the resolver the real UUID-comparison semantics; one that
returns `true` unconditionally makes the test assert nothing.

## Anti-patterns

- Editing generated output: `build/generated/**`, `docs/openapi/klabis-full.json`,
  `frontend/src/api/klabisApi.d.ts`
- Adding an endpoint by writing the controller method first
- Putting a domain type in a DTO
- Putting another module's endpoints in a module file because the URL prefix matches
- Pointing a response at a bare `type: array` or the raw payload instead of the `EntityModel*` /
  `PagedModel*` / `CollectionModel*` envelope, to get a nicer Java signature
- Renaming a payload schema for tidiness — it renames the `_embedded` key on the wire
- Deriving the `_embedded` key from the class name without checking the payload class for
  `@Relation(collectionRelation = ...)`, which overrides it
- Reusing a generic component name (`MemberIdParam`) across module files — component names are one
  global namespace after bundling; prefix them per module
- Leaving the path on a class-level `@RequestMapping` after the controller starts implementing a
  generated `*Api` — the path doubles and every endpoint 404s
- Writing a `201`/`204` with no `content:` block — the endpoint then answers 406 to any client that
  sends `Accept: application/prs.hal-forms+json`
- Giving an operation a multi-word `tags:` value — the generator drops it silently and the `*Api`
  interface never appears
- Mapping an envelope schema onto `java.util.List<...>`; the generator drops it silently
- Concluding an endpoint needs no authority because the controller has no annotation — check the
  method body for an imperative `checkXxxAccess()` first
- Relaxing an assertion in a link/affordance unit test that started failing after authorization moved
  into the spec — the test was passing only because `HalFormsSupport.INSTANCE` was null; wire it a
  real `OwnershipResolver` instead
- Writing `@HasAuthority` on a controller method — the authority belongs in `x-klabis-authority`,
  stated once
- Writing `@Operation` / `@ApiResponse` / `@Parameter` on a controller — the generator emits them
  onto the `*Api` interface from the spec, and a hand-written copy only drifts from it
- Hand-writing `x-operation-extra-annotation` to inject `@HasAuthority` — `api.mustache` emits that
  from `x-klabis-authority` itself, and a hand-written copy would emit the annotation twice
- Inventing a new `x-klabis-*` extension: each must map to an annotation the generator can reliably
  emit. Propose it, don't add it ad hoc — `validate.mjs` rejects any `x-klabis-*` key missing from
  `KNOWN_KLABIS_EXTENSIONS`, and emission needs a matching branch in `pojo.mustache`.
  **First check the generator cannot already do it**, and check that against *stock* templates: an
  `x-klabis-patch-field` extension was written and then removed once it turned out `openApiNullable`
  did the job, the earlier "it doesn't work" verdict having come from testing with our own
  `pojo.mustache` — which ignored the flag the generator was setting correctly all along. Disable
  `templateDir` before concluding a generator feature is broken.
- Reaching for a raw `x-field-extra-annotation` on a schema property to inject an annotation. The
  generator only honours it on *parameters*; on a model property the overridden `pojo.mustache`
  decides, and an unlisted extension is silently dropped — so the annotation never appears and any
  standard keyword you removed to "replace" is lost too.
- Naming a fresh top-level `enum:` schema and listing it in `models` — the generator writes a
  well-formed Java file with no constants and no body, and every reference fails to compile. An
  enum only works when `schemaMappings` points it at an existing domain enum. With no such enum,
  use `type: string`; the domain's own `valueOf`/`switch` still rejects bad values.
- Stripping `@RequestBody` off a generated-interface override because the interface already
  declares it. `HalFormsSupport` looks for it on the *concrete* method resolved via
  `methodOn(Controller.class)`, and Java does not inherit parameter annotations across an
  interface boundary — unlike `@HasAuthority`, nothing bridges this. Binding and authorization
  still work, so it compiles and passes; the inline options from `klabisAffordWith*Options`
  silently vanish from `_templates` while `properties[]` and `target` stay correct.
- Gating a `RepresentationModelProcessor<CollectionModel<EntityModel<X>>>` on a request attribute
  *for fear of cross-contamination*. Spring HATEOAS's `RepresentationModelProcessorInvoker` resolves
  the full generic signature, so a processor typed on `EntityModel<X>` does **not** run for another
  endpoint's `CollectionModel<EntityModel<Y>>` — verified directly against the invoker. Most of the
  collection processors here are correctly ungated. Gate only when the processor needs data the model
  cannot carry: `EventRegistrationController.RegistrationListPostprocessor` reads `eventId` from a
  request attribute because an *empty* registration list has no item to recover it from, which is a
  different problem from type dispatch.
- Injecting a new port into an `@MvcComponent` postprocessor — the scan is global, not scoped to a
  slice's `controllers=`, so every unrelated `@WebMvcTest` breaks unless `WithPostprocessors` also
  mocks it. Compute port-derived data in the controller, which already holds the port, and pass it
  through a request attribute.
- Putting `x-klabis-authority` / `x-klabis-owner-visible` on a property that uses `oneOf` or
  `allOf`. Composition strips property-level vendor extensions, and `allOf` additionally yields a
  bare type rather than `JsonNullable<T>` — which `RequestBodyFieldAuthorizationAdvice` skips, so
  the field silently stops being authorization-checked. Inline the type
  (`UpdateMemberRequest.gender`) and map `<Parent>_<property>` in `schemaMappings` if it must stay a
  domain type.
- Choosing a tag that is a substring of another module's tag. The generator's `apis` filter matches
  by substring, so registering `ORIS` also pulls in `OrisEvents`' operations from another module.
  Pick tags that are not prefixes of one another (`OrisImport`, not `ORIS`).
- Passing an empty `models` list to exclude all schemas. Like `apis`, an empty models property makes
  the generator emit **every** schema in the bundled document, not none. Use a single nonexistent
  placeholder name to keep the property non-empty while matching nothing.
- Forgetting that a generated `*Api` interface is class-level `@Validated`. A constrained
  `@RequestParam` is then rejected by AOP's `MethodValidationInterceptor` **before** MVC argument
  resolution, throwing `ConstraintViolationException` rather than the
  `HandlerMethodValidationException` the exception handler covers — a 500 where a 400 is intended.
  No hand-written controller hits this, so it first appears when a module goes spec-first.
- Writing `nullable: true` on a PATCH property. It is the OpenAPI 3.0 keyword and these specs are
  3.1, so it is silently ignored: no `JsonNullable`, no warning, and the endpoint quietly loses the
  ability to distinguish "absent" from "clear this field". Use `type: [x, 'null']`.
- Writing a PATCH property with `oneOf` or `allOf` when it also carries `x-klabis-authority` or
  `x-klabis-owner-visible`. `oneOf` strips the extension (scalar ones included); `allOf` keeps it
  but generates a bare type, and `RequestBodyFieldAuthorizationAdvice` skips every component that is
  not `JsonNullable`, so the annotation is never evaluated. Either way the check silently stops
  running for the one caller it exists for — the owner. Inline the type and map
  `<Parent>_<property>` in `schemaMappings`; see the `gender` section above.
- Marking a response property or a POST/PUT body property nullable. There is no tri-state to express
  and the `JsonNullable` wrapper leaks into code that only ever wants a value.
- Mapping a `JsonNullable` with a converter that dereferences the value. `map` runs on a *present
  null* too, so `field.map(this::convert)` NPEs into a 500 where the domain would have answered 400.
- Assuming a generated record's constructor parameters follow the order you wrote them in the spec.
  The bundler **alphabetizes `properties` keys**, so a spec written `(minAge, maxAge)` generates
  `AgeRangeRequest(maxAge, minAge)`. Positional construction then silently swaps the values — it
  compiles and only fails on an assertion, if one happens to cover it. Construct generated records
  via their `RecordBuilder`, never positionally.
