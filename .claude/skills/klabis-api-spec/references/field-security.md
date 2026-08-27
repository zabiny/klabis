# Field & Endpoint Authorization (`x-klabis-*`)

Per-field visibility on response DTOs, per-field write authorization on request DTOs, and
per-operation endpoint authorization. These extensions are what the generator turns into
`@HasAuthority` / `@OwnerVisible` annotations — see the `backend-patterns` skill for how the
backend then enforces them.

# `x-klabis-*` — field-level security

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

# `x-klabis-authority` on an operation — endpoint authorization

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

## `x-klabis-owner-visible` on an operation — ownership authorization

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
