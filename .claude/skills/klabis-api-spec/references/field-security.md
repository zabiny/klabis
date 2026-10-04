# Field & Endpoint Authorization (`x-klabis-*`)

Per-field visibility on response DTOs, per-field write authorization on request DTOs, and
per-operation endpoint authorization. These extensions are what the generator turns into
`@HasAuthority` / `@TargetId` / `@OwnerVisible` / `@ReadAuthority` annotations — see the
`backend-patterns` skill (`authorization.md`, `field-security.md`) for how `AuthorizationEvaluator`
then enforces them, and ADR-010 for the model (an authority held over everything or over specific
targets).

# `x-klabis-*` — field-level security

On schema properties. Each maps to exactly one existing Java annotation.

(`x-klabis-authority` and `x-klabis-owner-visible` also work one level up, on an operation, and
`x-klabis-target-id` also works on a path parameter — see
[endpoint authorization](#x-klabis-authority-on-an-operation--endpoint-authorization) below.
`x-klabis-read-authority` and `x-klabis-halforms-access` are property-only and the bundler rejects
them on an operation.)

| extension | value | generates | semantics |
|---|---|---|---|
| `x-klabis-target-id` | `MEMBER` \| `EVENT` | `@TargetId(TargetType.MEMBER)` | Marks the field — or, on an operation's path parameter, the parameter — identifying the target the record/operation is about. Authorities may then be held over just that target, and `x-klabis-owner-visible` compares it with the caller. On a record without it, the single UUID-convertible field is taken as a member target for owner-visible fields. |
| `x-klabis-owner-visible` | `true` | `@OwnerVisible` | Visible/permitted when the target is the caller, even without the authority (OR semantics with `x-klabis-authority`; **alone = owner-only**). |
| `x-klabis-authority` | `MEMBERS_MANAGE` or `[MEMBERS_MANAGE, EVENTS_MANAGE]` | `@HasAuthority({Authority.MEMBERS_MANAGE, …})` | Any one of the listed authorities suffices — held over everything, or over the target when there is one. Each must be a constant of `Authority.java`. |
| `x-klabis-read-authority` | e.g. `[MEMBERS_READ]` | `@ReadAuthority({Authority.MEMBERS_READ})` | **Request schemas only.** A user holding one of these but not allowed to change the field sees it `readOnly` in the HAL-FORMS template instead of not at all. |
| `x-klabis-halforms-access` | `READ_ONLY` \| `NONE` \| `READ_WRITE` \| `DEFAULT` | `@HalForms(access = …)` | Controls `readOnly` in HAL+FORMS `_templates`. |

```yaml
MemberDetailsResponse:
  type: object
  properties:
    id:
      type: string
      format: uuid
      x-klabis-target-id: MEMBER
    dateOfBirth:
      type: string
      format: date
      x-klabis-authority: MEMBERS_MANAGE
      x-klabis-owner-visible: true   # MEMBERS_MANAGE holders OR the member themselves
```

`x-klabis-owner-id: true` no longer exists — `validate.mjs` rejects it; use `x-klabis-target-id: MEMBER`.

Enforcement lives in `FieldSecurityBeanSerializerModifier`, which works **only on records**; the
decision for each annotated accessor is made by `AuthorizationEvaluator`. A denied field is omitted
or masked per `@HandleAuthorizationDenied`.

# `x-klabis-authority` on an operation — endpoint authorization

The authority an endpoint requires belongs in the spec, not on the controller. On an operation it
generates `@HasAuthority` on the **generated interface method**:

```yaml
paths:
  /api/members/{id}:
    get:
      operationId: getMember
      x-klabis-authority: MEMBERS_READ    # -> @HasAuthority({Authority.MEMBERS_READ}) on MembersApi.getMember
```

A list means "any of": `x-klabis-authority: [MEMBERS_MANAGE, EVENTS_REGISTRATIONS]` generates
`@HasAuthority({Authority.MEMBERS_MANAGE, Authority.EVENTS_REGISTRATIONS})`. The same affordance and
enforcement code evaluates it, so a HAL template is offered exactly when the call would pass.

The overridden `api.mustache` reads this key directly and emits the annotation above the method —
the same way `pojo.mustache` reads it off a schema property. Nothing rewrites it, so the published
`klabis-full.json` carries the spec key alone, not a Java string derived from it.

Do not also annotate the controller. The authority is stated once, in the spec; a second copy in
Java is what this replaces.

This relies on `MethodSecurityAnnotations`, which resolves security annotations across the interface
boundary — Java does not inherit method annotations from an interface, so without it the generated
annotation would compile and silently enforce nothing.

## `x-klabis-target-id` and `x-klabis-owner-visible` on an operation

`@TargetId` tells `AuthorizationEvaluator.canInvoke` which argument is the target. Two things need it:

- `@OwnerVisible` — it compares the target with the caller; without a `@TargetId` parameter it
  denies rather than resolving ownership, silently dropping the owner-or-authority semantics the
  endpoint advertises.
- an authority that may be held over specific targets (`GrantForm.SPECIFIC` in `Authority.java`) —
  without a target only grants over everything count, so a delegate would be refused.

The halves are declared on two different nodes:

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
      x-klabis-target-id: MEMBER         # -> @TargetId(TargetType.MEMBER) on the parameter
```

`api.mustache` emits `@HasAuthority`/`@OwnerVisible`, `pathParams.mustache` emits `@TargetId`, and
neither can see the other — so **`validate.mjs` is the only thing keeping them together.** It requires:

- an operation declaring `x-klabis-owner-visible` to have exactly one `x-klabis-target-id` parameter
  (zero denies; two would silently resolve against whichever came first);
- an operation listing an authority with `SPECIFIC` to have exactly one `x-klabis-target-id`
  parameter whose type equals that authority's `targetType`. `ALL`-only authorities are
  target-agnostic and need none.

**`x-klabis-target-id` may sit on a `$ref` parameter shared with operations that need no target.**
`MemberIdParam` is used by `getMember`, `updateMember`, `suspendMember` and `resumeMember`, but only
`updateMember` declares `x-klabis-owner-visible`; on the others the `@TargetId` is inert while their
authorities are `ALL`-only (a grant over everything passes regardless of the target). It is only
allowed on a **path** parameter — `pathParams.mustache` is the only parameter template with a branch
for it, so anywhere else the key would be silently dropped. `validate.mjs` rejects that too, along
with a target-id parameter that `x-spring-paginated` would fold into `Pageable` (`page`/`size`/`sort`
are query parameters, so the same check covers them), and `x-klabis-target-id` on an operation itself.

Combined with `x-klabis-authority`, owner-visible reproduces the OR semantics used everywhere else in
the codebase (MANAGE authority OR ownership) — see `FieldLevelAuthorizationTest` /
`HasAuthorityMethodInterceptorTargetTest` for the enforcement tests.

**Declared alone, it means owner-only.** The OR is with whatever authority is declared, so with none
declared there is nothing to OR against: `AuthorizationEvaluator.isAllowed` finds no authority in
an empty list, leaving ownership the sole path to `proceed()`. A lone `x-klabis-owner-visible`
therefore *narrows* access to the owner rather than widening it, and is the right way to model "only the member themselves, no MANAGE alternative"
— `MemberFeeChoice`'s and `MemberFeeSummary`'s 5 operations use exactly this. Do not reach for an
imperative controller check for that case.

Such operations need a test asserting that a caller holding the module's MANAGE authority is still
`403` (see `MemberFeeChoiceControllerTest`). Nothing else in the suite distinguishes owner-only from
owner-OR-MANAGE, so pairing an authority in later would widen access silently.

**Nothing requires an operation to declare an authority.** A missing `x-klabis-authority` generates
a method without `@HasAuthority` and no check reports it; the endpoint still requires
authentication (`/api/**` is `.authenticated()`), but loses its authority check. When adding an
operation, state the authority deliberately.
