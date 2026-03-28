## 1. MemberFilter and Repository

- [x] 1.1 Create `MemberFilter` value object in domain layer with `activeOnly` flag and factory methods `all()` and `activeOnly()`
- [x] 1.2 Add `findAll(MemberFilter, Pageable)` to `MemberRepository` interface; replace `findAll(Pageable)` and `findAllActive()` with the new signature
- [x] 1.3 Implement `buildCriteriaQuery(MemberFilter)` in `MemberRepositoryAdapter` using Spring Data JDBC Criteria API (`JdbcAggregateTemplate`)
- [x] 1.4 Update all existing callers of the removed `findAll(Pageable)` and `findAllActive()` methods to use `findAll(MemberFilter, Pageable)` or `findAll(MemberFilter)`

## 2. List Endpoint Filtering

- [x] 2.1 Write failing test: non-admin user receives only active members in paginated list (and `totalElements` reflects active-only count)
- [x] 2.2 Inject `CurrentUserData` into `listMembers()` and pass `MemberFilter.activeOnly()` or `MemberFilter.all()` based on `MEMBERS:MANAGE` authority
- [x] 2.3 Write failing test: admin user receives both active and inactive members in list

## 3. Detail Endpoint 404 for Inactive

- [x] 3.1 Write failing test: non-admin user gets 404 when accessing detail of an inactive member
- [x] 3.2 Extend `ManagementService.getMemberAndRecordView()` to throw `MemberNotFoundException` when `!canManageMembers && !member.isActive()`
- [x] 3.3 Write failing test: admin user can still access detail of an inactive member (200 OK)
