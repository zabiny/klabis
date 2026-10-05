---
name: group-delegated-authorities
description: How free group delegation is wired (FreeGroup set at creation, CSV column, FreeGroupRelationshipSource, DelegatedAuthority wire enum) and pitfalls hit while building it
metadata:
  type: project
---

Free group delegation (rebac-3 group 3): `FreeGroup.CreateFreeGroup(name, creator, Set<Authority>)` (2-arg ctor kept), validated in the aggregate constructor via `FreeGroup.delegatableAuthorities()` (SPECIFIC + targetType MEMBER) -> `InvalidDelegatedAuthorityException` (400 via BusinessRuleViolationException handler).

- Stored in `groups.user_groups.delegated_authorities` (CSV of `Authority.getValue()`, NULL = none) edited directly in V001 (no new migration, project rule); `GroupMemento.withDelegatedAuthorities/delegatedAuthorities()`.
- `FreeGroup.reconstruct` has a 6-arg overload without the set (delegates with `Set.of()`) so ~35 test call sites stay unchanged; production adapter uses the 7-arg one.
- API enum is a dedicated `DelegatedAuthority` schema in groups.yaml (generated wire enum, not the domain Authority); mapping in `DelegatedAuthorities` helper (freegroup.infrastructure.restapi). HAL-FORMS options come from `klabisAffordWithOptions` on the createGroup affordance in `GroupListPostprocessor` (domain-derived), `x-hal-input-type: Authority` so the frontend localises.
- `FreeGroupRelationshipSource` reuses `FreeGroupFilter.withOwnerOrMemberIs` + in-memory `isOwner` (no new `ownerIs` filter).

**Pitfalls**
- Creating a TrainingGroup auto-enrols every member whose age fits, so an explicit `addMemberToTrainingGroup` in an integration test throws `MemberAlreadyInGroupException`.
- Member JWT claim for acting member is `member_id` (not `memberIdUuid`) in hand-built integration tokens; user id == member id, activate with `user.activateWithPassword`.
- HAL-FORMS omits `required` when false -> assert `.required` isEmpty, not false.
- `developer:test-runner-skill` does not exist in this environment; run `/opt/gradle/bin/gradle --offline` (wrapper cannot download). Gradle output is huge without `--console=plain`.
