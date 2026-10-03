## Context

Method security today is `@HasAuthority(Authority)` enforced by the custom advisor `HasAuthorityMethodInterceptor`, plus `@OwnerVisible` / `@OwnerId` for the "target is me" case. `@PreAuthorize` is evaluated only on response/request record components (field security). `HalFormsSupport.isMethodAuthorized` re-implements a subset of the interceptor's logic (only `@HasAuthority` and `@OwnerVisible`) to decide whether an affordance is emitted, so the two can disagree.

Controllers implement generated `*Api` interfaces; security annotations come from the hand-written OpenAPI spec (`x-klabis-authority`, `x-klabis-owner-visible`), so annotation lookup crosses the interface boundary (`MethodSecurityAnnotations`).

`Authority.Scope` has `GLOBAL` and `CONTEXT_SPECIFIC`; `AuthorizationPolicy.checkGlobalAuthorityNotGrantedViaGroup` is a placeholder for future group delegation and treats every non-global authority as delegable. That is too broad (`MEMBERS_MANAGE` is `CONTEXT_SPECIFIC`), so a new scope is introduced.

All three group aggregates (free, training, legal guardian) extend `MemberGroup` and share the `groups.user_groups` / `user_group_owners` / `user_group_members` tables via a type discriminator.

## Goals / Non-Goals

**Goals:**
- Declare relationship-aware authorization on controller methods with an annotation.
- One evaluator decides both the call and the visibility of affordances / links.
- Groups delegate authorities to their owners over their members.
- Proof of concept: `MEMBER:EDIT_DETAILS` on the member update endpoint.

**Non-Goals:**
- Editing the delegated set of an existing group (immutable for now).
- Delegation of `GLOBAL` authorities.
- Event-scoped relations (`eventId` target types); the model must allow them, but no use case is implemented.
- Replacing `@OwnerVisible` (stays for the "target is me" case and field security).
- Frontend permission editor; the frontend only displays the set in invitations and chooses it at free group creation.

## Decisions

### D1: `@HasAuthority` is a repeatable meta-annotation over `@PreAuthorize`

`@HasAuthority(value, target)` is meta-annotated with `@PreAuthorize` using Spring Security's annotation template placeholders, so it participates in the standard method-security chain (`@EnableMethodSecurity` is already active). `target` is an optional SpEL string (e.g. `"#id"`) resolved against method arguments.

Repeated `@HasAuthority` (`@HasAuthority.List`) combine with OR. `@PreAuthorize` is not `@Repeatable`, so the container cannot be expressed as a single template expression; a dedicated `AuthorizationManager` handles `@HasAuthority` / `@HasAuthority.List` (replacing `HasAuthorityMethodInterceptor`), while plain `@PreAuthorize` keeps the standard manager.

Alternatives: keep the custom advisor and only add `target` (smaller change, but perpetuates the second evaluation path); rely on hand-written `@PreAuthorize` SpEL for OR (generator and spec would have to carry SpEL).

`target` as SpEL depends on parameter names matching between the generated `*Api` method and its implementation. Names are read from the annotated interface method; the build keeps `-parameters`.

### D2: One evaluator, two callers

```mermaid
flowchart TD
    M[Method security: AuthorizationManager] --> E[AuthorityEvaluator]
    H[HalFormsSupport afford and link] --> E
    E --> G[Global authority on Authentication]
    E --> R[RelationshipAuthorityProvider SPI]
    R --> Q[Groups: delegated authorities per owner]
```

`AuthorityEvaluator.isGranted(authentication, authority, target)`:
1. true if the user holds the authority globally;
2. otherwise, if a target is given, true if any `RelationshipAuthorityProvider` reports the user holds the authority over the target.

`HalFormsSupport` builds the SpEL evaluation context from the dummy invocation's method and arguments (the same arguments it gets from `methodOn(...)`), then calls the same evaluator for every `@HasAuthority`, OR-ing them. Its static per-`Method` cache stores only parsed annotation metadata, never results. Plain `@PreAuthorize` on controller methods is evaluated by HAL through the standard `PreAuthorizeAuthorizationManager`, so affordance and call stay in agreement for both.

`@OwnerVisible` keeps working as an additional OR alternative; the evaluator is called first, ownership second.

### D3: The relationship SPI is implemented once, in the groups shared kernel

`RelationshipAuthorityProvider` (in `common.security`) is implemented in `common.groups` infrastructure with one query over the unified tables: the user is in `user_group_owners` of a group that lists the authority in `user_group_authorities` and has the target in `user_group_members`. It therefore covers all three group types without per-module code. A user's owned-group delegations are loaded once per request (`authority → set of member ids`) and kept in `HalResponseContext`; rendering a member list performs one query, not one per row.

Alternative: ask each module's repository separately (N modules, N queries) — rejected.

### D4: Delegated authorities live on the group, immutable

`MemberGroup` gains `delegatedAuthorities` (a set of `Authority`), set in the constructor and without mutators. Invariants: every entry has the new `Scope.MEMBER`, meaning an authority evaluated over a member target and safe to delegate through groups (initially only `MEMBER_EDIT_DETAILS`). `AuthorizationPolicy` allows group delegation only for this scope.

- Free group: chosen in the create request; shown in the invitation response; not editable afterwards. Existing groups have an empty set.
- Training group: predefined empty set.
- Legal guardian group: predefined set `{MEMBER_EDIT_DETAILS}`. Existing legal guardian groups are migrated to it (schema is edited in V001 per project rules; data bootstrap sets it for example data).

Because the set is fixed at creation there is no escalation path: nobody can grant themselves rights later, and invitees see the delegation before accepting (consent). Training and legal guardian members are assigned without invitation; their sets are predefined by the system, not by a user.

### D5: `MEMBER:EDIT_DETAILS` and the member update endpoint

New `Authority.MEMBER_EDIT_DETAILS("MEMBER:EDIT_DETAILS", CONTEXT_SPECIFIC)`. `updateMember` is declared with `@HasAuthority(MEMBERS_MANAGE)` and `@HasAuthority(value = MEMBER_EDIT_DETAILS, target = "#id")` (OR), in addition to the existing `@OwnerVisible`. The spec extension `x-klabis-authority` accepts a list of entries `{authority, target}`; the generator templates emit repeated annotations.

`OwnProfileEditRule` currently blocks every non-admin from editing a minor. It narrows to: blocked only when the target member is the acting user and is a minor and the user lacks `MEMBERS_MANAGE`. A guardian acting on another member (holding `MEMBER_EDIT_DETAILS` over them) is never blocked by it. The rule is shared by the "Upravit" affordance and the update itself, as before.

Field-level security is unchanged: admin-only fields stay admin-only (`MEMBERS_MANAGE`); `MEMBER_EDIT_DETAILS` grants the member-editable field set only.

### D6: `MEMBER:EDIT_DETAILS` is not assignable as a global permission

Global permission assignment (`MEMBERS:PERMISSIONS` dialog) lists only `GLOBAL`-scope authorities plus those already assignable; `MEMBER_EDIT_DETAILS` is `MEMBER` and arises only from group delegation, so the dialog does not offer it. Resolving authorities in the JWT is unchanged: the token carries only global authorities.

## Domain model

```mermaid
classDiagram
    class MemberGroup {
        name
        owners
        members
        delegatedAuthorities
    }
    class FreeGroup
    class TrainingGroup
    class LegalGuardianGroup
    class Invitation
    class Authority {
        value
        scope
    }
    MemberGroup <|-- FreeGroup
    MemberGroup <|-- TrainingGroup
    MemberGroup <|-- LegalGuardianGroup
    FreeGroup "1" --> "*" Invitation
    MemberGroup "*" --> "*" Authority : delegates
```

| Element | Change |
|---|---|
| `MemberGroup.delegatedAuthorities` | added; immutable set of `MEMBER` authorities, validated on construction |
| `FreeGroup` creation | accepts the set; the set is shown on invitations |
| `TrainingGroup`, `LegalGuardianGroup` | constructed with predefined sets |
| `Authority.MEMBER_EDIT_DETAILS` | added, `MEMBER`; new `Authority.Scope.MEMBER` |
| `groups.user_group_authorities(user_group_id, authority)` | new table in V001, FK to `user_groups` with cascade delete |

## API changes

No new endpoints. Changes to existing contracts (`docs/openapi/spec/groups.yaml`, `members.yaml`; bundle and frontend types regenerated):

| Operation | Change |
|---|---|
| `PATCH /api/members/{id}` (`updateMember`) | security: `MEMBERS_MANAGE` or `MEMBER_EDIT_DETAILS` over `{id}` (plus owner-visible as before). HAL: `member` detail `_templates.updateMember` appears for any of these callers |
| `POST /api/groups` (`createGroup`) | request body gains optional `delegatedAuthorities` (array of authority codes; only delegable values accepted, else 422); HAL-FORMS property with options listing the delegable authorities |
| `GET /api/groups/{id}` (`getGroup`) | response gains `delegatedAuthorities` |
| `GET /api/invitations/pending` | each `PendingInvitationResponse` gains `delegatedAuthorities` |
| `GET /api/training-groups/{id}`, legal guardian group detail | response gains `delegatedAuthorities` (read-only) |

Links and affordance names are unchanged; the difference is who sees them. A guardian viewing a minor sees `_templates.updateMember` on the minor's member detail and the member's row in group views; a user without the relation does not.

## Risks / Trade-offs

- [SpEL `target` depends on parameter names across the `*Api` interface and controller] → startup/test check that every `@HasAuthority(target=…)` resolves; WebMvc tests for the PoC endpoint (403 and 200 paths).
- [HAL and method security drifting again] → both call `AuthorityEvaluator`; a shared test asserts the same user/target matrix against the call and the affordance.
- [Per-request cache holds stale delegations within one request] → acceptable; request scoped.
- [Free group creator delegates rights over invitees] → consent via invitation display; immutable set; only `MEMBER` authorities delegable.
- [Replacing `HasAuthorityMethodInterceptor` touches every secured endpoint] → keep behaviour-preserving tests first (existing 403/200 slice tests), swap manager, run the full suite once.
- [Generator template change for repeated annotations] → covered by the zero-diff baseline check on untouched operations.

## Migration Plan

Schema is edited in V001 (project rule, H2 resets). New table gets the predefined sets for existing legal guardian groups through the bootstrap/example-data path. No data migration for free groups (empty set). Rollback: revert the change; no persistent data depends on it.

## Open Questions

None. Free group creation offers every `MEMBER` authority (today only `MEMBER:EDIT_DETAILS`); training groups delegate nothing.

## Glossary

- **Target**: the object (member, event, …) an authority is evaluated against.
- **Delegated authority**: an authority a group grants to its owners over the group's members.
- **Relationship-based authorization (ReBAC)**: deciding access from the relation between the acting user and the target (here: owner of a group containing the target).
- **Global authority**: an authority held by the user directly, valid for any target.
