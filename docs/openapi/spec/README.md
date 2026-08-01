# Hand-written OpenAPI spec

This directory is **the** source of truth for the Klabis REST API. Java DTOs, API interfaces and
frontend types are all generated from it.

## Pipeline

```
docs/openapi/spec/  ──npm run openapi (bundles, then generates)──▶  docs/openapi/klabis-full.json  ──▶  frontend types
        │
        └──────────openApiGenerate<Module>──────────▶  backend DTOs + *Api interfaces
```

- `docs/openapi/spec/` — hand-written spec; edit here
- `docs/openapi/klabis-full.json` — gitignored build artifact, generated; never hand-edit
- `docs/openapi/generated/klabis-codefirst.json` — springdoc dump of what the running app serves
  (gitignored). Nothing depends on it; it exists only for ad-hoc comparison against the spec. Since
  the generator runs with `documentationProvider=springdoc`, the annotations springdoc introspects
  are themselves generated from the spec, so the two documents now differ only in the Actuator paths
  springdoc auto-discovers.

**Change an endpoint by editing the spec, not the Java.** A generated DTO or `*Api` interface edited
by hand is overwritten on the next build.

## Layout

```
klabis.yaml          root document: info, servers, securitySchemes, $ref to module specs
_shared/
  hal.yaml           HAL and HAL-FORMS building blocks (media-type level, not Klabis-specific)
  problem.yaml       RFC 7807 ProblemDetail
  pagination.yaml    generic PageParam/SizeParam — see note below on why `sort` is not here
<module>.yaml        one file per module, added as it is migrated
```

Module specs are pulled in from `klabis.yaml` via `$ref`. Shared components are hoisted into the
bundle on demand — a schema nobody references does not end up in the output.

### Every module file is a standalone OpenAPI document

Each `<module>.yaml` carries its own `openapi`, `info` and `servers`, so it opens directly in Swagger
UI or Redoc without being bundled first:

```bash
npx @redocly/cli preview-docs docs/openapi/spec/members.yaml
```

Serve it from this directory — its `./_shared/*.yaml` refs are relative, so a module file moved out
of here stops resolving.

That standalone-ness costs three keys duplicated from `klabis.yaml`: `openapi`, `info.version` and
`components.securitySchemes`. The last one is not optional decoration — operations carry
`security: [KlabisAuth: [...]]`, and a security requirement naming a scheme the document does not
define is an error, not a warning.

The bundler **ignores all three**: modules are pulled in through a `#/paths/...` pointer, so nothing
outside `paths` and the referenced `components` is ever read. A module that drifted from the root
would therefore keep bundling cleanly and only mislead whoever opened that one file — so
`validateModuleDocuments` (validate.mjs) pins each to the root's value. Change `info.version` in
`klabis.yaml` and the bundle fails until every module matches.

`securitySchemes` is inlined in `klabis.yaml` rather than shared from `_shared/`, because a root
component that only `$ref`s another file collapses to a self-referencing
`$ref: '#/components/securitySchemes/KlabisAuth'` in the bundle — a ref into `#/components/` is
localized, not expanded.

`_shared/*.yaml` are fragments, not modules: they hold `components` only and are never opened on
their own, so they carry no header. The check derives its module list from the files `klabis.yaml`
routes a path to, so anything it does not route to — `_shared/`, or a scratch file left in this
directory — is simply not a module and is not checked.

## Commands

Run from `backend/`:

| command | what it does |
|---|---|
| `./gradlew openapiBundle` | validates the spec and writes `docs/openapi/klabis-full.json` |
| `./gradlew openapiBundle -PopenapiCheck` | validates only, writes nothing (what CI runs) |
| `./gradlew openapiBundle -PopenapiOut=<path>` | writes the bundle elsewhere |

Or directly from `tools/openapi-bundle/`: `node bundle.mjs [--check] [--out <path>]`.

`drift.mjs` is still there for comparing the spec against a springdoc dump, but it needs
`docs/openapi/generated/klabis-codefirst.json`, which only exists after `./gradlew generateOpenApiDocs`.

From `frontend/`, `npm run openapi` bundles the spec first (equivalent to `node bundle.mjs`), then
regenerates both `src/api/klabisApi.d.ts` (schemas, via openapi-typescript) and
`src/api/halTypes.ts` (link/template relations, via `tools/openapi-bundle/haltypes.mjs`). A
standalone `./gradlew openapiBundle` run is only needed to inspect the bundle directly or when
driving backend codegen manually — `npm run openapi` doesn't need it as a separate step.

## Klabis extensions

Field-level authorization, on schema properties:

| extension | generates |
|---|---|
| `x-klabis-owner-id: true` | `@OwnerId` |
| `x-klabis-owner-visible: true` | `@OwnerVisible` |
| `x-klabis-authority: MEMBERS_MANAGE` | `@HasAuthority(Authority.MEMBERS_MANAGE)` |
| `x-klabis-halforms-access: READ_ONLY` | `@HalForms(access = READ_ONLY)` |

Endpoint authorization uses the same three keys one level up: `x-klabis-authority` and
`x-klabis-owner-visible: true` on the **operation**, `x-klabis-owner-id: true` on one of its **path
parameters**. The owner-id parameter may be a shared `$ref` — `@OwnerId` is inert unless the method
is also `@OwnerVisible` — but an operation declaring `x-klabis-owner-visible` must have exactly one,
which `validate.mjs` enforces during bundling: `@OwnerVisible` without `@OwnerId` denies instead of
resolving ownership.

Hypermedia, on **response objects** (not on schemas — links belong to the representation):

- `x-hal-links` — link relations the response may carry
- `x-hal-templates` — HAL-FORMS templates the response may carry

Both describe the **maximal variant**. `klabisLinkTo` returns `Optional` and `klabisAfford` returns an
empty list when the user lacks authorization, so any link or template may be absent at runtime, and the
same endpoint returns different `_links`/`_templates` per user. Conditions are not expressible here —
put them in `description`.

Extension values are validated during bundling: `x-klabis-authority` must be a constant of
`Authority.java`, and `operation:` inside `x-hal-*` must match an existing `operationId`.

`x-klabis-*` keys are consumed by the overridden templates in `backend/src/main/openapi-templates/`
— `pojo.mustache` for schema properties, `api.mustache` for operations, `pathParams.mustache` for
parameters. The bundler passes them through untouched, so the published contract states each rule
once, in the spec's own vocabulary, rather than alongside a Java string derived from it.
**`api.mustache` and `pathParams.mustache` are forks of the stock `JavaSpring` templates** and
nothing detects drift: on a generator upgrade, diff each against its new stock version and port the
Klabis branch across. Each file opens with a note saying so.

> **Never put a field-authorization extension on a property that uses `oneOf`/`allOf`.** The
> generator drops property-level vendor extensions from composed schemas, and an `allOf` also
> generates a bare type instead of `JsonNullable<T>` — which
> `RequestBodyFieldAuthorizationAdvice` skips outright. Either way the field silently stops being
> authorization-checked. Inline the type instead (see `UpdateMemberRequest.gender` in
> `members.yaml`), and add a `schemaMappings` entry for the generated `<Parent>_<property>` name if
> it needs to stay a domain type. `PatchRequestWrapperArchitectureTest` guards this.

## Rules

- API DTOs carry **wire types**, not domain types — `string`/`format: uuid`, never a `MemberId` object.
  Conversion to domain types belongs in the mapper or controller.
- Never hand-edit `docs/openapi/klabis-full.json` — it is generated and gitignored.
- Reference operations by `operation: <operationId>`, not by escaped `operationRef` pointers.
- **Nullable properties use the OpenAPI 3.1 union spelling**, `type: [string, 'null']`, never the
  3.0 `nullable: true`. These documents declare `openapi: 3.1.0`; `nullable: true` is a 3.0 keyword
  that a 3.1-aware tool (Redocly, `openApiNullable`) does not recognize as nullable at all. For a
  `$ref` that needs to be nullable, `allOf: [{$ref: ...}, nullable: true]` doesn't compose the way
  it looks like it should — use `oneOf: [{$ref: ...}, {type: 'null'}]` instead (see
  `MemberFeeSummaryResponse.currentGroup` in `membershipfees.yaml`), and re-read the `oneOf`/`allOf`
  warning above first if the property carries a field-authorization extension.
