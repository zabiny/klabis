## 1. Domain Layer — Enforce Restriction

- [x] 1.1 Write failing test: `FreeGroup.addMember()` throws `DirectMemberAdditionNotAllowedException`
- [x] 1.2 Create `DirectMemberAdditionNotAllowedException` in domain
- [x] 1.3 Override `addMember()` in `UserGroup` to check `instanceof WithInvitations` and throw the exception
- [x] 1.4 Refactor if needed — verify all domain tests pass

## 2. API Layer — Remove Affordance and Map Exception

- [x] 2.1 Write failing test: `GET /api/groups/{id}` response for free group does NOT contain `addGroupMember` template when user is owner
- [x] 2.2 Remove `addGroupMember` affordance from `GroupController` HATEOAS builder for `WithInvitations` groups
- [x] 2.3 Map `DirectMemberAdditionNotAllowedException` to HTTP 422 in exception handler
- [x] 2.4 Write test: `POST /api/groups/{id}/members` for free group returns 422
- [x] 2.5 Refactor if needed — verify all controller tests pass
