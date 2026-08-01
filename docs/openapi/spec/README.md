# Hand-written OpenAPI spec

This directory is **the** source of truth for the Klabis REST API. Java DTOs, API interfaces and
frontend types are all generated from it.

## Pipeline

```
docs/openapi/spec/  ──openapiBundle──▶  docs/openapi/klabis-full.json  ──npm run openapi──▶  frontend types
        │
        └──────────openApiGenerate<Module>──────────▶  backend DTOs + *Api interfaces
```

- `docs/openapi/spec/` — hand-written spec; edit here
- `docs/openapi/klabis-full.json` — committed bundle, generated; never hand-edit
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
<module>.yaml        one file per module, added as it is migrated
```

Module specs are pulled in from `klabis.yaml` via `$ref`. Shared components are hoisted into the
bundle on demand — a schema nobody references does not end up in the output.

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

From `frontend/`, `npm run openapi` regenerates both `src/api/klabisApi.d.ts` (schemas, via
openapi-typescript) and `src/api/halTypes.ts` (link/template relations, via
`tools/openapi-bundle/haltypes.mjs`).

## Klabis extensions

Field-level authorization, on schema properties:

| extension | generates |
|---|---|
| `x-klabis-owner-id: true` | `@OwnerId` |
| `x-klabis-owner-visible: true` | `@OwnerVisible` |
| `x-klabis-authority: MEMBERS_MANAGE` | `@HasAuthority(Authority.MEMBERS_MANAGE)` |
| `x-klabis-halforms-access: READ_ONLY` | `@HalForms(access = READ_ONLY)` |

Hypermedia, on **response objects** (not on schemas — links belong to the representation):

- `x-hal-links` — link relations the response may carry
- `x-hal-templates` — HAL-FORMS templates the response may carry

Both describe the **maximal variant**. `klabisLinkTo` returns `Optional` and `klabisAfford` returns an
empty list when the user lacks authorization, so any link or template may be absent at runtime, and the
same endpoint returns different `_links`/`_templates` per user. Conditions are not expressible here —
put them in `description`.

Extension values are validated during bundling: `x-klabis-authority` must be a constant of
`Authority.java`, and `operation:` inside `x-hal-*` must match an existing `operationId`.

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
- Never hand-edit `docs/openapi/klabis-full.json` — it is generated.
- Reference operations by `operation: <operationId>`, not by escaped `operationRef` pointers.
