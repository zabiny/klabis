## 1. Backend: Training Group Member Access

- [x] 1.1 Write failing test: training group member can GET training group detail (currently 403)
- [x] 1.2 Extend authorization for training group detail endpoint to allow access for group members
- [x] 1.3 Write failing test: non-member cannot GET training group detail
- [x] 1.4 Verify non-member access is still denied

## 2. Backend: Family Group Member Access

- [x] 2.1 Write failing test: family group member can GET family group detail (currently 403)
- [x] 2.2 Extend authorization for family group detail endpoint to allow access for group members
- [x] 2.3 Write failing test: non-member cannot GET family group detail
- [x] 2.4 Verify non-member access is still denied

## 3. Backend: Free Group Member Access

- [x] 3.1 Write failing test: free group member (non-owner) can GET free group detail (currently 403)
- [x] 3.2 Extend authorization for free group detail endpoint to allow access for group members
- [x] 3.3 Verify non-member access is still denied

## 4. Backend: Family Groups Navigation HAL Link

- [x] 4.1 Write failing test: RootController returns family groups link only for MEMBERS:MANAGE users
- [x] 4.2 Condition family groups HAL link in RootController on MEMBERS:MANAGE permission
- [x] 4.3 Write test: user without MEMBERS:MANAGE does not receive family groups link

## 5. Frontend: Family Groups Navigation Visibility

- [x] 5.1 Verify frontend hides "Rodinné skupiny" navigation item when HAL link is absent (should work via existing HAL-link-driven navigation pattern — verify and add test if needed)
