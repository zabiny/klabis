# PATCH Bodies & the `JsonNullable<T>` Tri-State

A PATCH request distinguishes three states per field: absent, explicit null, and a value.
This is how that is spelled in the spec and what the generator does with it.

# PATCH bodies: nullable properties become `JsonNullable<T>`

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

ageRange:                            # a $ref property — no composition needed
  $ref: '#/components/schemas/AgeRangeRequest'
  x-klabis-nullable: true            # -> JsonNullable<AgeRangeRequest>
```

Both spellings generate the same wrapper. Prefer `x-klabis-nullable` on `$ref` properties: the
legacy `oneOf: [{$ref}, {type: 'null'}]` spelling still works, but composition strips sibling
vendor extensions (below), so the property can carry no `x-klabis-*` / `x-hal-*` keys. The bundle
emits the identical legacy `oneOf` shape either way — `derive.mjs` translates — so frontend types
never notice which spelling the property uses. `x-klabis-nullable: false` is the reverse lever: a
plain `T` despite a nullable wire type, for a nullable response property where the wrapper would
leak into code that only wants a value (unused today).

`nullable: true` and `allOf` + `nullable` both generate the bare type. This is driven by
`openApiNullable = "true"` in `openApiModule(...)` plus the `isNullable` branch in the overridden
`pojo.mustache`; if that template is ever re-forked from upstream, port the branch or every PATCH DTO
silently loses its wrappers.

Only PATCH bodies want this. A nullable response property or POST/PUT body would generate the
wrapper too, forcing an unwrap on every read for a distinction those payloads do not have. Several
response schemas still carry a leftover `nullable: true`; it is inert only because the 3.0 keyword is
ignored, so do not "modernise" them to the 3.1 spelling.

## `oneOf` strips property-level `x-klabis-*`

The generator discards vendor extensions from any property written as a `oneOf`. This is a property
of the composition keyword itself, not of what it contains — a scalar
`oneOf: [{type: string}, {type: 'null'}]` loses them just as a `$ref` branch does, even though the
union spelling `type: ['string', 'null']` keeps them.

For a `$ref` property the dilemma is gone — spell it `$ref` + `x-klabis-nullable: true` and there is
no composition to strip anything. What follows is why the legacy `oneOf` spelling was migrated away
(and why `allOf` is a trap worth naming, because it looks like one):

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

## Consuming the tri-state

`JsonNullable.map` applies the mapper to a *present null* — it branches on presence, not on nullness.
A mapper that dereferences the value turns an intended `400` into an NPE and a `500`:

```java
// forwards the null so the domain's own Assert rejects it
field.map(value -> value == null ? null : convert(value))
```

`orElse(current)` is the "apply patch" primitive: the wrapped value when present (including null),
the fallback when absent.
