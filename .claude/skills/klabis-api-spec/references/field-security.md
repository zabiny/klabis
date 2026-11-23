# Field & Endpoint Authorization (`x-klabis-*`)

Per-field visibility on response DTOs, per-field write authorization on request DTOs, and
per-operation endpoint authorization. These extensions are what the generator turns into
`@HasAuthority` / `@TargetId` / `@ReadAuthority` annotations — see the
`backend-patterns` skill (`authorization.md`, `field-security.md`) for how `AuthorizationEvaluator`
then enforces them, and ADR-010 for the model (an authority held over everything or over specific
targets) and ADR-013 for why owner access is a self-grant, not an extension.

# `x-klabis-*` — field-level security

On schema properties. Each maps to exactly one existing Java annotation.

(`x-klabis-authority` also works one level up, on an operation, and
`x-klabis-target-id` also works on a path parameter — see
[endpoint authorization](#x-klabis-authority-on-an-operation--endpoint-authorization) below.
`x-klabis-read-authority` and `x-klabis-halforms-access` are property-only and the bundler rejects
them on an operation.)

| extension | value | generates | semantics |
|---|---|---|---|
| `x-klabis-target-id` | `MEMBER` \| `EVENT` | `@TargetId(TargetType.MEMBER)` | Marks the field — or, on an operation's path parameter, the parameter — identifying the target the record/operation is about. Authorities may then be held over just that target. A record has one target; an operation may have one per kind. A record without it has no target. |
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
      x-klabis-authority: [MEMBERS_MANAGE, MEMBERS_VIEW_PROFILE]   # MEMBERS_MANAGE, or VIEW_PROFILE over this member (self-grant, guardians)
```

The former owner extensions (`x-klabis-owner-id`, and its successor for "the target is the caller") no longer exist — `validate.mjs` rejects them as unknown. A member's access to their own data is a self-grant (`SelfRelationshipSource`): list the `SPECIFIC` authority it grants (e.g. `MEMBERS_VIEW_PROFILE`, `MEMBERS_EDIT_PROFILE`) next to the administrator authority and mark the target with `x-klabis-target-id`.

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

A list means "any of": `x-klabis-authority: [MEMBERS_MANAGE, EVENTS_REGISTER_MEMBER]` generates
`@HasAuthority({Authority.MEMBERS_MANAGE, Authority.EVENTS_REGISTER_MEMBER})`. The same affordance and
enforcement code evaluates it, so a HAL template is offered exactly when the call would pass.

`KlabisSpringCodegen` renders the full `@HasAuthority({...})` text into the vendor extension
`x-klabis-authority-annotation` (and `x-klabis-read-authority-annotation` for `@ReadAuthority`), which
the overridden `api.mustache` / `pojo.mustache` print verbatim — the same pattern as
`x-klabis-halforms-annotation`. That happens inside the generator, so the published
`klabis-full.json` carries the spec key alone, not a Java string derived from it.

Do not also annotate the controller. The authority is stated once, in the spec; a second copy in
Java is what this replaces.

This relies on `MethodSecurityAnnotations`, which resolves security annotations across the interface
boundary — Java does not inherit method annotations from an interface, so without it the generated
annotation would compile and silently enforce nothing.

## `x-klabis-target-id` on an operation

`@TargetId` tells `AuthorizationEvaluator.canInvoke` which argument is the target. An authority that may be held over specific targets (`GrantForm.SPECIFIC` in `Authority.java`) needs it — without a target only grants over everything count, so a delegate, a guardian or the owner (self-grant) would be refused.

An operation may identify **several targets of different kinds** (one parameter per `TargetType`, e.g. `MEMBER` and `EVENT`); each listed authority is checked over the target of its own kind. Two parameters of one kind are rejected.

The halves are declared on two different nodes:

```yaml
paths:
  /api/members/{id}:
    patch:
      operationId: updateMember
      x-klabis-authority: [MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]   # -> @HasAuthority on the method
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

`api.mustache` emits `@HasAuthority`, `pathParams.mustache` emits `@TargetId`, and neither can see the other — so **`validate.mjs` is the only thing keeping them together.** It requires an operation listing authorities with `SPECIFIC` to have exactly one `x-klabis-target-id` parameter per kind the authorities are held over, whose type equals that authority's `targetType`. `ALL`-only authorities are target-agnostic and need none.

**`x-klabis-target-id` may sit on a `$ref` parameter shared with operations that need no target.** `MemberIdParam` is used by `getMember`, `updateMember`, `suspendMember` and `resumeMember`; on operations whose authorities are `ALL`-only the `@TargetId` is inert (a grant over everything passes regardless of the target). It is only allowed on a **path** parameter — `pathParams.mustache` is the only parameter template with a branch for it, so anywhere else the key would be silently dropped. `validate.mjs` rejects that too, along with a target-id parameter that `x-spring-paginated` would fold into `Pageable` (`page`/`size`/`sort` are query parameters, so the same check covers them), and `x-klabis-target-id` on an operation itself.

**Owner access is a self-grant.** To let the member themself in, list the `SPECIFIC` authority `SelfRelationshipSource` grants over their own `MemberId` next to the administrator authority; guardians come in through their delegations. To keep the administrator out ("only the member and their guardian"), omit the administrator authority and test that a holder of it is still `403` (see `MemberFeeChoiceGuardianAccessIntegrationTest`) — nothing else in the suite distinguishes the two, so adding an authority later would widen access silently.

**Nothing requires an operation to declare an authority.** A missing `x-klabis-authority` generates
a method without `@HasAuthority` and no check reports it; the endpoint still requires
authentication (`/api/**` is `.authenticated()`), but loses its authority check. When adding an
operation, state the authority deliberately.
