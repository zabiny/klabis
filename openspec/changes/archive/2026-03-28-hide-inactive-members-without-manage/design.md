## Context

Currently `MemberController.listMembers()` calls `memberRepository.findAll(pageable)` unconditionally, returning all members regardless of their active status. `ManagementService.getMemberAndRecordView()` also returns any member by ID without checking active status against the caller's authority.

The `MemberRepository` already has `findAllActive()` returning `List<Member>` and `findAll(Pageable)` for paginated access, but no way to combine filtering with pagination.

## Goals / Non-Goals

**Goals:**
- Members without `MEMBERS:MANAGE` authority see only active members in the list
- Members without `MEMBERS:MANAGE` authority receive 404 when requesting detail of an inactive member
- Pagination metadata reflects only visible members (correct totalElements for non-admin users)

**Non-Goals:**
- Changing visibility rules for any other field (email, active status — already handled by field security)
- Affecting any endpoint other than `GET /api/members` and `GET /api/members/{id}`
- Frontend changes (the UI adapts automatically — inactive members simply disappear)

## Decisions

### Conditional repository query in list endpoint

`listMembers()` receives current user's authority via `@CurrentUser CurrentUserData`. Based on `canManage = currentUser.hasAuthority(MEMBERS_MANAGE)` it constructs a `MemberFilter` and passes it to `memberRepository.findAll(filter, pageable)`:
- `canManage = true` → `MemberFilter.all()` (no restriction)
- `canManage = false` → `MemberFilter.activeOnly()`

**Why a Filter object instead of separate `findAllActive(Pageable)` method?** A single `findAll(MemberFilter, Pageable)` keeps the repository interface stable as future filtering needs arise (e.g. filter by name, group). Adding a new `findAllXxx` method per combination leads to interface bloat. The filter object cleanly expresses intent and is extensible.

**Why pass authority to the controller, not push it into the service?** The filtering is a presentation/authorization concern, not a domain concern. The domain layer has no concept of "what the caller is allowed to see" — that belongs in the primary adapter.

### 404 for inactive member detail without MANAGE

`ManagementService.getMemberAndRecordView()` already accepts `canManageMembers: boolean`. Extend the method to throw `MemberNotFoundException` when `!canManageMembers && !member.isActive()`.

Returning 404 (not 403) is intentional: it avoids revealing that an inactive member with the given ID exists. From the caller's perspective, the member is simply not found — consistent with the filtered list.

### MemberFilter value object

Introduce `MemberFilter` in the domain layer with a single optional constraint: `activeOnly`. Replace the existing `findAll(Pageable)` and `findAllActive()` methods in `MemberRepository` with `findAll(MemberFilter, Pageable)` and `findAll(MemberFilter)` respectively.

The JDBC adapter (`MemberRepositoryAdapter`) implements the query using Spring Data JDBC Criteria API — the same pattern as `EventRepositoryAdapter`. A `buildCriteriaQuery(MemberFilter)` method accumulates `Criteria` conditions into a `Query`, which is then executed via `JdbcAggregateTemplate`. This avoids string-concatenated SQL and keeps the query composable as the filter grows.

```
MemberFilter.activeOnly()
    → Criteria.where("active").isTrue()
    → Query.query(combined)
    → jdbcAggregateTemplate.findAll(query.with(pageable), MemberMemento.class)
    → jdbcAggregateTemplate.count(query, MemberMemento.class)   // for totalElements
```

## Risks / Trade-offs

- **Stale links**: If a non-admin user somehow holds a direct link to an inactive member's detail page, they will hit 404. This is the intended behavior but may feel unexpected. → Mitigated: the list never shows inactive members to these users, so direct links are unlikely to be obtained.
- **Pagination counts**: Non-admin users will see lower `totalElements` counts than admins. This is correct but may cause confusion if counts are compared between users. → Acceptable: counts reflect what the user can actually see.

## Migration Plan

No data migration required. The change is purely behavioral — existing data is unaffected. Deployment is safe to roll forward; rollback restores previous (over-permissive) behavior.
