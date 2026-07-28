---
name: klabis-api-spec
description: Authoring the hand-written OpenAPI spec in docs/openapi/spec/ — x-klabis-* field-security and x-hal-* hypermedia extensions, module layout, and the spec-first workflow. Use whenever adding, changing or removing a REST endpoint, request/response field, HAL link or HAL+FORMS template; when writing the API chapter of an OpenSpec design.md; or when migrating a module from code-first to spec-first.
user-invocable: false
version: 0.1.0
---

# Klabis API Spec

`docs/openapi/spec/` is becoming **the** source of truth for the REST API: Java DTOs, API interfaces
and frontend HAL types will all be generated from it.

Never edit generated output — `docs/openapi/klabis-full.json` (bundle artifact),
`build/generated/**`, `frontend/src/api/klabisApi.d.ts`.

## Migration status

**The migration is in progress.** Only `members` is migrated so far; every other module is still
code-first, and nothing is generated from the spec yet.

```
./gradlew openapiDriftCheck                      # from backend/ — what is still unmigrated
./gradlew openapiDriftCheck -PopenapiModule=/api/members
```

Until a module appears in `docs/openapi/spec/`, treat it as code-first: change the Java controller
and use the `backend-patterns` skill. Do not hand-edit `klabis-full.json` for it either — regenerate
with `./gradlew generateOpenApiDocs`.

## Layout

```
docs/openapi/spec/
  klabis.yaml          root: info, servers, securitySchemes, $ref per path to module files
  _shared/
    hal.yaml           Link, Links, PageMetadata, HalFormsTemplate, HalFormsProperty
    problem.yaml       RFC 7807 ProblemDetail
  members.yaml         one file per module
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

## Core rule: DTOs carry wire types

API DTOs are transport records mirroring the JSON payload. `string`/`format: uuid`, never a
`MemberId` object. Conversion to domain types belongs in the mapper or the controller.

The spec describes JSON — a domain type is not derivable from it, and a generated DTO cannot invent
one.

## `x-klabis-*` — field-level security

On schema properties. Each maps to exactly one existing Java annotation.

| extension | value | generates | semantics |
|---|---|---|---|
| `x-klabis-owner-id` | `true` | `@OwnerId` | Marks the field holding the owner's ID, used to evaluate `x-klabis-owner-visible`. Without it, the single UUID-convertible field is used. |
| `x-klabis-owner-visible` | `true` | `@OwnerVisible` | Visible to the owner even without the authority (OR semantics with `x-klabis-authority`). |
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

## Workflow for an API change

The spec moves first. The Java signature is a consequence, never the driver.

1. Edit `docs/openapi/spec/<module>.yaml`
2. `./gradlew openapiBundle` — validates extensions and refs
3. `./gradlew openapiDriftCheck -PopenapiModule=/api/<module>` — confirms spec and implementation agree
4. Adjust the controller/DTO to match
5. `cd frontend && npm run openapi`

If step 4 shows the spec is wrong, go back to step 1. **Never adjust the spec to match existing Java
just to silence the drift check** — that reintroduces code-first through the back door. The exception
is a genuine spec bug, which is a spec change like any other.

Once codegen lands (a later migration phase), steps 3–4 become `./gradlew compileJava` plus fixing
whatever no longer compiles.

## Migrating a module

1. `./gradlew openapiDriftCheck -PopenapiModule=/api/<module>` — the list of operations to write
2. Read the module's operations out of `docs/openapi/generated/klabis-codefirst.json` — it is the
   ground truth for parameters, request bodies and status codes
3. Write `<module>.yaml`; add one `$ref` per path to `klabis.yaml`
4. Add `x-hal-links` / `x-hal-templates` by reading the controller's postprocessors and
   `RepresentationModelProcessor` implementations — springdoc cannot see them, so the drift check
   will not catch a missing one
5. Re-run the drift check until the module reports `mismatched: 0`

Expect the springdoc output to be wrong in places (it does not know about `@JsonValue` mixins, for
one). Where it disagrees with the actual wire format, the spec follows the **wire**, and the
mismatch is documented rather than mirrored.

## Anti-patterns

- Editing generated output: `build/generated/**`, `docs/openapi/klabis-full.json`,
  `frontend/src/api/klabisApi.d.ts`
- Adding an endpoint by writing the controller method first
- Putting a domain type in a DTO
- Putting another module's endpoints in a module file because the URL prefix matches
- Inventing a new `x-klabis-*` extension: each must map to an annotation the generator can reliably
  emit. Propose it, don't add it ad hoc — the bundler rejects unknown ones.
