## 1. Hide DRAFT events from event list

- [ ] 1.1 Write controller tests: `GET /api/events` as regular user excludes DRAFT events; as manager includes them
- [ ] 1.2 Add repository method to find events excluding a specific status (`findByStatusNot`)
- [ ] 1.3 Extend service `listEvents` to accept an optional excluded status filter
- [ ] 1.4 Implement controller logic: check `EVENTS:MANAGE` authority and pass DRAFT exclusion filter to service

## 2. Hide DRAFT events from status-filtered list

- [ ] 2.1 Write controller test: `GET /api/events?status=DRAFT` as regular user returns empty, as manager returns DRAFT events
- [ ] 2.2 Implement controller logic: return empty page for `status=DRAFT` when user lacks `EVENTS:MANAGE`

## 3. Hide DRAFT event detail

- [ ] 3.1 Write controller tests: `GET /api/events/{id}` for DRAFT event returns 404 as regular user, 200 as manager
- [ ] 3.2 Implement controller logic: check `EVENTS:MANAGE` authority on detail endpoint — return 404 for DRAFT events
