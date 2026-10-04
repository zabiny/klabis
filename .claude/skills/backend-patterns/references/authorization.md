# Authorization Model

Who may do what, and how code asks. Rationale: ADR-010 in `docs/design-decisions.md`. Field-level
specifics (hiding/masking response fields, PATCH request fields) are in `field-security.md`; the
spec extensions that generate the annotations are in the `klabis-api-spec` skill.

## Contents
- Grants: over everything vs. over specific targets
- The permission snapshot
- `AuthorizationEvaluator` — the only place that decides
- Annotations: `@HasAuthority`, `@TargetId`, `@OwnerVisible`, `@ReadAuthority`
- Imperative checks in application code
- Adding a relationship-based permission (`RelationshipSource`)
- Testing

## Grants: over everything vs. over specific targets

An authority is held either **over everything** of its target type (today's "global" permission,
including targets created later) or **over specific targets** (derived from a relationship).
`Authority` declares which forms are allowed:

```java
MEMBERS_MANAGE("MEMBERS:MANAGE", TargetType.MEMBER, GrantForm.ALL),
EVENTS_MANAGE("EVENTS:MANAGE", TargetType.EVENT, GrantForm.ALL),
SYNC_MANAGE("SYNC:MANAGE", TargetType.NONE, GrantForm.ALL),
```

- `targetType` (`common.authorization.TargetType`): `MEMBER`, `EVENT`, or `NONE` (not about a target).
- `grantForms` (`GrantForm`): `ALL` = assignable in the permissions dialog (`Authority.assignableAuthorities()`);
  `SPECIFIC` = may come from a relationship (`Authority.delegatable()`). An `{ALL}`-only authority is
  an administrator authority and can never be delegated.

A target is a `TargetRef(TargetType type, UUID id)` (`TargetRef.member(id)`, `TargetRef.event(id)`).

## The permission snapshot

`AuthorizationSnapshot` is the immutable set of a user's grants for one request:
`overAll` (from `PermissionService` + standard authorities) and `overTargets` (union of all
`RelationshipSource` beans). The snapshot is carried by the authentication: the resource server builds a new
`KlabisJwtAuthenticationToken` per request, and the token holds a memoized supplier that calls
`AuthorizationSnapshotLoader.loadFor(userId)` on the first authorization question. Every later decision of
the request reads the same instance — one permission set per request, no request-scoped bean.
`AuthorizationSnapshotProvider.current()` just reads the snapshot from the current authentication.

- Access tokens of users carry **no** `authorities` claim. `KlabisJwtAuthenticationToken.getAuthorities()`
  returns the snapshot's `overAll`, so a grant or revocation applies on the next request.
- `client_credentials` tokens (no `user_id`) keep their scope-derived authorities; the provider turns
  them into a snapshot with no targeted grants (`AuthorizationSnapshot.ofGrantedAuthorities`).
- Without an authenticated user (event listeners, scheduled jobs) the snapshot is empty: the evaluator
  denies. Do not authorize in listeners.

## `AuthorizationEvaluator` — the only place that decides

Every decision goes through `com.klabis.common.authorization.AuthorizationEvaluator`:

| Method | Used by |
|---|---|
| `canInvoke(method, targetClass, args)` | `HasAuthorityMethodInterceptor`, `HalFormsSupport` (`klabisAfford*`, `klabisLinkTo`) |
| `canReadField(accessor, record)` | `SecuredBeanPropertyWriter` (response fields) |
| `canWriteField(accessor, target)` | `RequestBodyFieldAuthorizationAdvice` |
| `requestFieldAccess(payloadType, property, target)` → `FieldAccess` (`WRITE` / `READ` / `NONE`) | HAL-FORMS property visibility / `readOnly` (`HalFormsSupport`) |
| `describeRequirement(method, class)` / `describeRequirement(accessor)` | the "Required: …" text of every denial — never compose it from annotations in the caller |
| `isGuarded(method, class)` (static) | the pointcut of `HasAuthorityMethodInterceptor` |
| `toTarget(type, rawId)` | converts a raw id (URI variable, argument) to a `TargetRef` through the same `ConversionService` as `canInvoke`; `null` when not convertible |
| `has(authority)` / `has(authority, target)` / `isSelf(target)` | application code (controllers, postprocessors) |

Rule for an element guarded by authorities `[A1..An]`, an optional target `t` and optional `@OwnerVisible`:

```
allowed ⇔ ∃ Ai: (t present ? has(Ai, t) : hasOverAll(Ai))   ∨   (ownerVisible ∧ isSelf(t))
```

Because affordances and enforcement share this code path, an offered template or link is always one
the user may invoke — never compute "can the user do X" separately for a link.

`AuthorizationArchitectureTest` (ArchUnit) fails the build when code outside `common.authorization`
/ `common.security` reads `@HasAuthority`, `@OwnerVisible`, `@TargetId`, `@ReadAuthority`,
or `Authentication.getAuthorities()`. *Applying* the annotations is fine; *reading* them is not.

## Annotations

On generated `*Api` interfaces and DTOs these come from the spec (`klabis-api-spec`) — never write
them by hand on generated code or on a controller override.

| Annotation | Spec extension | Meaning |
|---|---|---|
| `@HasAuthority({A, B})` (`common.users`) | `x-klabis-authority: A` or `[A, B]` | any one of the listed authorities suffices |
| `@TargetId(TargetType.MEMBER)` (`common.authorization`) | `x-klabis-target-id: MEMBER` | the path parameter / record component identifying the target |
| `@OwnerVisible` (`common.security.fieldsecurity`) | `x-klabis-owner-visible: true` | also allowed when the target is the caller; alone = owner-only |
| `@ReadAuthority({A})` (`common.security.fieldsecurity`) | `x-klabis-read-authority: [A]` | request field only: HAL-FORMS shows it read-only to holders of `A` who may not change it |

```java
// generated MembersApi — not hand-written
@HasAuthority({ Authority.MEMBERS_MANAGE })
@OwnerVisible
ResponseEntity<Void> updateMember(@TargetId(TargetType.MEMBER) @PathVariable("id") UUID id,
                                  @RequestBody UpdateMemberRequest request);
```

Without a `@TargetId` parameter an authority must be held over everything. `@OwnerVisible` without
`@TargetId` denies (nothing to compare with the caller) — `validate.mjs` rejects that in the spec.

`MethodSecurityAnnotations` resolves these annotations across the interface boundary; the
interceptor's pointcut only considers classes under `com.klabis.*`.

A hand-written Spring `@PreAuthorize("hasAuthority(...)")` on a **method** sees only grants over everything
(through `getAuthorities()`). Use it only for logic the annotations cannot express, and never for an
authority that may be held over a target. On a field or record component `@PreAuthorize` is not supported.

## Imperative checks in application code

When a decision cannot be declared (a flag that changes a query, a conditional link in a
postprocessor, a rule combining domain data), inject `AuthorizationEvaluator`:

```java
boolean canManage = authorizationEvaluator.has(Authority.MEMBERS_MANAGE);                      // over everything
boolean canEdit   = authorizationEvaluator.has(Authority.MEMBERS_MANAGE, TargetRef.member(id)); // over this member
boolean self      = authorizationEvaluator.isSelf(TargetRef.member(id));
```

- `CurrentUserData` (`@ActingUser`) identifies the caller (`userId`, `memberId`) — it carries no
  authorities. Never read `Authentication.getAuthorities()` / `SecurityContextHolder` for a decision.
- Prefer `has(authority, target)` whenever the question is about a specific member or event, so a
  future targeted grant works without touching the code.
- In plain unit tests of a hand-built component pass `SecurityContextAuthorizationEvaluator.create()`
  (test support in `com.klabis.common`): it decides over the authentication in the `SecurityContextHolder`.

## Adding a relationship-based permission (`RelationshipSource`)

1. Add `GrantForm.SPECIFIC` to the authority in `Authority.java` (and the right `targetType`).
2. Implement `RelationshipSource` in the owning module's `infrastructure`, querying the module's own
   repository (ADR-001 — never another module's repository):

   ```java
   @Component   // illustrative — no source exists yet; the first one arrives with rebac-3
   class GuardianRelationshipSource implements RelationshipSource {
       @Override
       public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
           // one query returning every grant of the user
       }
   }
   ```

   Return all grants of the user at once — the snapshot calls each source once per request. Grants
   for authorities without `SPECIFIC`, or with a mismatched target type, are silently discarded.
3. In the spec, put `x-klabis-target-id: <TYPE>` on the path parameter of every operation listing the
   authority — `validate.mjs` requires exactly one, with the authority's `targetType`.
4. Response/request records whose fields the authority guards need the target component marked
   `x-klabis-target-id` too.

## Testing

- `@WithKlabisMockUser(authorities = {...})` installs grants over everything; add
  `targetGrants = @TargetGrant(authority = Authority.X, type = TargetType.MEMBER, ids = {"<uuid>"})`
  for grants over specific targets. The token carries a fixed snapshot, so `@WebMvcTest`s need no database;
  slices get the evaluator and provider from `KlabisWebMvcSliceConfiguration`.
- Tokens built by hand: `KlabisAuthenticationFactory.createAuthenticationToken(jwtParams[, snapshot])` —
  never construct a `Jwt` and a converter in a test.
- Integration tests with real tokens use the real provider and real sources.
- Ownership tests need `@WithKlabisMockUser(memberId = "...")`; `isSelf` compares with the token's user id and member id.
- `HalFormsSupportInstanceTestExecutionListener` binds the static `HalFormsSupport` instance to the
  running test's context; a plain unit test (no Spring extension) gets one from `PlainHalFormsSupportExtension` (auto-detected) deciding over the
  authentication in the `SecurityContextHolder`. `klabisAfford*` / `klabisLinkTo` fail fast (`IllegalStateException`) when no instance is bound.

## Reference implementation

- Model: `common.authorization` — `AuthorizationSnapshot`, `AuthorizationSnapshotLoader`, `AuthorizationSnapshotProvider`,
  `AuthorizationEvaluator`, `RelationshipSource`, `TargetRef`, `TargetType`, `GrantForm`, `TargetId`
- Authorities: `common.users.Authority`, `common.users.HasAuthority`
- Enforcement: `common.security.HasAuthorityMethodInterceptor`, `common.ui.HalFormsSupport`
- Token: `common.security.KlabisJwtAuthenticationConverter`, `KlabisJwtAuthenticationToken` (carries the snapshot)
- Tests: `AuthorizationArchitectureTest`, `HasAuthorityMethodInterceptorTargetTest`,
  `AffordanceAuthorizationTest`, `PermissionChangesTakeEffectIntegrationTest`
