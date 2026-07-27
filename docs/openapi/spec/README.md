# Hand-written OpenAPI spec

This directory is being established as **the** source of truth for the Klabis REST API. Java DTOs,
API interfaces and frontend HAL types will all be generated from it.

## Current state: migration in progress

The API is **still code-first**. Nothing here is generated from yet.

- `docs/openapi/klabis-full.json` — committed artifact, currently still produced by springdoc
- `docs/openapi/generated/klabis-codefirst.json` — springdoc output (gitignored), input for the drift check
- `docs/openapi/spec/` — hand-written spec, currently only a skeleton

Run `./gradlew openapiDriftCheck` (from `backend/`) to see which operations are not migrated yet.

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
| `./gradlew openapiBundle` | validates the spec and reports its size (does not write) |
| `./gradlew openapiBundle -PopenapiOut=docs/openapi/klabis-full.json` | validates and writes the bundle |
| `./gradlew openapiDriftCheck` | compares springdoc output against this spec |
| `./gradlew openapiDriftCheck -PopenapiModule=/api/members` | same, restricted to one module |

Or directly from `tools/openapi-bundle/`: `node bundle.mjs --check`, `node drift.mjs`.

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

## Rules

- API DTOs carry **wire types**, not domain types — `string`/`format: uuid`, never a `MemberId` object.
  Conversion to domain types belongs in the mapper or controller.
- Never hand-edit `docs/openapi/klabis-full.json` — it is generated.
- Reference operations by `operation: <operationId>`, not by escaped `operationRef` pointers.
