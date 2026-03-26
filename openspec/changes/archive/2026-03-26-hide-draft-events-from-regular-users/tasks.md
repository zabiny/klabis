## 1. Hide DRAFT events from event list

- [x] 1.1 Write controller tests: `GET /api/events` as regular user excludes DRAFT events; as manager includes them
- [x] 1.2 Add repository method to find events excluding a specific status (`findByStatusNot`)
- [x] 1.3 Extend service `listEvents` to accept an optional excluded status filter
- [x] 1.4 Implement controller logic: check `EVENTS:MANAGE` authority and pass DRAFT exclusion filter to service

## 2. Hide DRAFT events from status-filtered list

- [x] 2.1 Write controller test: `GET /api/events?status=DRAFT` as regular user returns empty, as manager returns DRAFT events
- [x] 2.2 Implement controller logic: return empty page for `status=DRAFT` when user lacks `EVENTS:MANAGE`

## 3. Hide DRAFT event detail

- [x] 3.1 Write controller tests: `GET /api/events/{id}` for DRAFT event returns 404 as regular user, 200 as manager
- [x] 3.2 Implement controller logic: check `EVENTS:MANAGE` authority on detail endpoint — return 404 for DRAFT events
