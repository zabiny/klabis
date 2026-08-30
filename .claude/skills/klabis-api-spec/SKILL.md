---
name: klabis-api-spec
description: Authoring the hand-written OpenAPI spec in docs/openapi/spec/ — x-klabis-* field-security and x-hal-* hypermedia extensions, module layout, and the spec-first workflow. Use whenever adding, changing or removing a REST endpoint, request/response field, HAL link or HAL+FORMS template; when writing the API chapter of an OpenSpec design.md; or when migrating a module from code-first to spec-first.
user-invocable: false
version: 0.7.0
---

# Klabis API Spec

`docs/openapi/spec/` is **the** source of truth for the REST API: Java DTOs, the `*Api` interfaces
controllers implement, endpoint authorization, and the frontend HAL types are all generated from it.
The Java signature is a consequence of the spec, never the driver.

Never edit generated output — `docs/openapi/klabis-full.json`, `build/generated/**`,
`frontend/src/api/klabisApi.d.ts`.

```
./gradlew openapiBundle                  # from backend/ — spec/ -> klabis-full.json
./gradlew openapiBundle -PopenapiCheck   # validate only, write nothing
npm run openapi                          # from frontend/ — regenerates klabisApi.d.ts + halTypes.ts
```

Changing the spec requires regenerating **both** the bundle and the frontend types.

`docs/openapi/spec/README.md` documents the pipeline and directory layout for anyone browsing the
repo; it is the reference for those mechanics rather than something restated here.

## Which reference to read

Load the file matching what you are changing — each is authoritative for its area, and the rules
differ enough between them that generalising from one to another produces broken spec.

| Working on | Read |
|---|---|
| Any response: media types, the derived HAL envelope, `_embedded` keys, `x-hal-entity-items`/`x-hal-embedded`, `x-hal-links`/`x-hal-templates` | `references/hypermedia.md` |
| Hiding/masking a response field, authorizing a request field, endpoint authorization (`x-klabis-authority`, `x-klabis-owner-visible`) | `references/field-security.md` |
| Constraints on request fields, cross-field rules | `references/validation.md` |
| A PATCH endpoint (the `JsonNullable<T>` tri-state) | `references/patch-bodies.md` |
| Registering a new module for codegen, or migrating one to spec-first | `references/adding-a-module.md` |

How the backend then *consumes* what the spec generates — controllers implementing `*Api`,
postprocessors, `HalResponseContext` — is the `backend-patterns` skill.

## Core rule: DTOs carry wire types

API DTOs are transport records mirroring the JSON payload. `string`/`format: uuid`, never a
`MemberId` object. Conversion to domain types belongs in the mapper or the controller.

The spec describes JSON — a domain type is not derivable from it, and a generated DTO cannot invent
one.

## Core rule: declare the payload, not the envelope

A response declares its `application/json` payload and nothing else. The bundler
(`tools/openapi-bundle/lib/derive.mjs`) derives the `application/prs.hal-forms+json` content entry
and, behind it, an `allOf` composing the payload with a shared `EntityModel` / `CollectionModel` /
`PagedModel` base model (defined once in `_shared/hal.yaml`, hoisted into `klabis-full.json` by the
bundler). That bundle is what the frontend types and Swagger UI are generated from. It is the only
place in the repo encoding envelope structure; the backend codegen reads the module YAML directly
and never sees one.

**HAL is the default** — every 2xx response with an `application/json` schema gets an envelope.
`x-klabis-hal: false` on the **operation** is the sole opt-out (6 operations: the pre-auth password
endpoints, `getMySchedule`, `listOrisEvents`).

Three facts the payload cannot state get an extension:

| extension | where | states |
|---|---|---|
| `x-klabis-hal: false` | operation | this endpoint is not hypermedia |
| `x-hal-entity-items: true` | an array **property** | its items are independently addressable — each gets its own `EntityModel` wrapper, and the generated Java property becomes `List<EntityModel<Item>>` |
| `x-hal-embedded: {items, suffix}` | a **response** | a nested collection the controller assembles at runtime via `HalResponseContext.embed`, which is not derivable from the payload |

Everything else — collection vs. item, paged vs. unpaged (`x-spring-paginated`), the `_embedded` key
— is derived. `references/hypermedia.md` has the rules in full.

## Layout

```
docs/openapi/spec/
  klabis.yaml          root: info, servers, securitySchemes, $ref per path to module files
  _shared/
    hal.yaml           Link, Links, PageMetadata, HalFormsTemplate(s), HalFormsProperty, HalFormsOptions,
                       and the EntityModel / CollectionModel / PagedModel envelope base models the deriver composes
    problem.yaml       RFC 7807 ProblemDetail
    pagination.yaml    generic PageParam/SizeParam
    responses.yaml     shared error responses (BadRequest, Unauthorized, Forbidden, NotFound, …)
  <module>.yaml        one file per module: members, events, groups, finance,
                       membershipfees, calendar, common, oris
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

## Why the controllers carry no springdoc annotations

`documentationProvider = "springdoc"` makes the generator emit `@Operation` / `@ApiResponse` /
`@Parameter` onto the `*Api` interface, straight from the spec. Springdoc reads annotations off the
concrete class and Java does not inherit method annotations from an interface — but the controller
`@Override`s a method whose *declaration* carries them, and springdoc resolves that. So
`/v3/api-docs` is spec-derived too, and a hand-written duplicate on a controller is pure drift
surface.

The generated set is strictly richer than what the controllers used to hold: it also carries
`security`, `tags`, every error response with its description and `@Content` schema, and
`@Parameter(hidden = true)` on the `x-spring-provide-args` arguments. Measured over the whole app
when the 26 controllers were stripped: operations 117 → 117 (none lost), summaries 82 → 117,
descriptions 51 → 111, parameter descriptions 95 → 133.

**The catch is a generic return type in `@Schema(implementation = ...)`.** `documentationProvider`
renders each response's baseType into `@Schema(implementation = <baseType>.class)`, and a paginated
response's real type is `Page<X>` — not legal Java there (JLS 15.8.2: a class literal takes a raw
type). Same problem for a `schemaMappings` target of `java.lang.Object` (springdoc would render it as
`"type": "string"`, worse than nothing). `KlabisSpringCodegen.fromResponse()` (`backend/buildSrc/`)
handles both by leaving that response's `baseType` unset, so `api.mustache`'s
`{{#baseType}}...{{/baseType}}` never opens the `content = {...}` block for it — no post-process
patch needed.

The exceptions are the 7 files with no generated counterpart, which legitimately keep their
annotations: `OpenApiConfig` (global info + security scheme), `MvcExceptionHandler` and
`GroupsExceptionHandler`, the hand-written `MemberOptionResponse`/`MemberSummaryResponse`, and
`@Hidden` on `ActingUser`/`ActingMember`.

## Anti-patterns

- Editing generated output: `build/generated/**`, `docs/openapi/klabis-full.json`,
  `frontend/src/api/klabisApi.d.ts`
- Adding an endpoint by writing the controller method first
- Putting a domain type in a DTO
- Putting another module's endpoints in a module file because the URL prefix matches
- Writing a *derived* envelope schema by hand — a per-payload `EntityModelFoo` / `PagedModelEntityModelFoo`
  / `CollectionModelEntityModelFoo` in a module spec, or a `mappings` entry to unwrap one. A module
  spec declares only the payload and the bundler derives the envelope. (The three *unsuffixed* base
  models `EntityModel` / `CollectionModel` / `PagedModel` in `_shared/hal.yaml` are the exception —
  they are hand-written on purpose and the deriver composes every envelope from them.) A `mappings`
  entry is only for the hand-written-override cases listed above (nested classes, domain enums,
  cross-module types, the two `common` marker records, the `java.lang.Object` fallback)
- Declaring an `application/prs.hal-forms+json` content entry — the deriver adds it, and an entry
  already present makes the deriver skip that response entirely (the coexistence guarantee that let
  the modules migrate one at a time). Bodyless `201`/`204` responses are the exception: they have no
  `application/json` sibling to derive from, so they declare the media type with an empty schema
  themselves
- Adding a `schemaMappings` entry for a `List<T>` collection response "just in case" — an unmapped
  `type: array` schema (named or inline) always generates `List<T>` directly with no wrapper class,
  confirmed against the pinned generator version; a mapping here is pure ceremony. Only `Page<T>`
  (pagination metadata an array can't carry) needs one, and even then only a **named** array schema.
- Keeping a controller's return type as `Collection<T>` and adding a `schemaMappings` entry to match
  it, instead of migrating the controller to `List<T>` — there is no generator-native way to produce
  `Collection<T>` from an array schema, only `List<T>`, so the mapping exists purely to preserve an
  incidental signature choice. Change the controller instead (usually a one-line type change, since
  the underlying value is already a `List`)
- Leaving a `MockMvc` test asserting `$._links`/`$._templates` without an explicit
  `.accept(MediaTypes.HAL_FORMS_JSON)` — every HAL response also serves its bare `application/json`
  payload, so content negotiation resolves to the payload and the assertions fail even though the
  endpoint still serves HAL-FORMS correctly to a client that asks for it
- Forgetting `x-klabis-hal: false` on an endpoint that is not hypermedia — HAL is the default, so the
  bundle grows an envelope and a hal-forms media type the controller never serves
- Declaring a payload schema with a `_links` or `_embedded` property of its own. The deriver
  recognises envelopes by shape, so it would take the payload for an already-written envelope and
  skip it silently; `validate.mjs` reports this rather than letting it pass
- Renaming a payload schema for tidiness — the `_embedded` key defaults to
  `uncapitalize(schemaName) + "List"`, so the rename changes a JSON key on the wire
- Writing `@Relation(collectionRelation = ...)` on a payload class instead of `x-klabis-relation` in
  the spec — `pojo.mustache` emits the annotation from the extension, and the deriver reads the same
  value for the bundle's `_embedded` key, so declaring it once is what keeps spec and runtime in step
- Reusing a generic component name (`MemberIdParam`) across module files — component names are one
  global namespace after bundling; prefix them per module
- Leaving the path on a class-level `@RequestMapping` after the controller starts implementing a
  generated `*Api` — the path doubles and every endpoint 404s
- Writing a `201`/`204` with no `content:` block — the endpoint then answers 406 to any client that
  sends `Accept: application/prs.hal-forms+json`
- Giving an operation a multi-word `tags:` value — the generator drops it silently and the `*Api`
  interface never appears
- Mapping any schema onto `java.util.List<...>`; `List` is a reserved container name in the
  generator's type system and the mapping is dropped silently, producing `ResponseEntity<>` with no
  diagnostic
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
