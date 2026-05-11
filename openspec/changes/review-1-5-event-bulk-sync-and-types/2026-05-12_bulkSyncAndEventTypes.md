# TCF — review-1-5: Bulk ORIS sync + Event Types

This Team Coordination File tracks progress for implementation of the `review-1-5-event-bulk-sync-and-types` proposal.

**Read this file before starting work.** Append a concise summary of your changes and any issues at the bottom when you finish.

## Proposal scope (high level)

- **Phase A (N5)** — Bulk ORIS sync: backend endpoint `POST /api/events/sync-from-oris/all-upcoming` (sync, sequential, partial-failure tolerant, aggregate response) + frontend toolbar action with progress modal and summary.
- **Phase B (N13)** — Event Types catalog: new `event-types` capability (CRUD aggregate + REST + admin UI) + optional `Event.eventType` field + ORIS auto-mapping by name (case-insensitive) + table column + filter.

Reference proposal docs:
- `proposal.md`
- `design.md`
- `tasks.md`
- `specs/events/spec.md`
- `specs/event-types/spec.md`

## Plan

Vertical slices, sequential (Phase A is fully independent and can ship first; Phase B is split into BE-then-FE iterations).

| Iter | Scope | Agent | Status |
|------|-------|-------|--------|
| 1 | A1 — Backend bulk-sync service + endpoint + HAL affordance + integration tests | backend-developer | TODO |
| 2 | A2 — Frontend toolbar action, progress modal, summary view | frontend-developer | TODO |
| 3 | B1+B2+B3 — DB migration, `EventType` aggregate, persistence, REST CRUD controller + tests | backend-developer | TODO |
| 4 | B4 — Wire `eventType` into `Event` aggregate + REST DTO/forms + tests | backend-developer | TODO |
| 5 | B5 — ORIS import auto-mapping helper + tests | backend-developer | TODO |
| 6 | B6 — Frontend admin page for event types | frontend-developer | TODO |
| 7 | B7 — Frontend event form/table column/filter/detail badge + labels | frontend-developer | TODO |
| 8 | Simplify pass + code review + fixes + docs (task 9) | mix | TODO |

After every iteration: tests pass → commit. E2E verification (A3, B8) is optional manual step at the end.

## Notes for subagents

- Always run tests via `developer:test-runner-skill` (do NOT run gradlew/npm directly in parallel).
- Use `backend-patterns` skill for all backend code; use `hal-navigator-patterns` for frontend.
- Mark completed tasks in `tasks.md` as you finish them.
- Do NOT bypass commit signing — use `--no-gpg-sign` only if 1Password agent fails.
- Conventional Commits — scope: `events` for N5, `event-types` (or `events`) for N13.

## Open implementation decisions

- ORIS field for auto-mapping: backend-developer will pick Level or Discipline during iter 5 and document in `developerManual`.
- DB constraint for unique name: implement as `UNIQUE INDEX ON event_types (LOWER(name))` in V001.

## Progress log

(subagents append below)

---

### Iter 2 — Frontend bulk sync toolbar + modal (2026-05-12)

Implemented tasks A2.1–A2.4. All 1392 frontend tests pass.

**Changed files:**
- `frontend/src/localization/labels.ts` — added `templates.syncAllUpcomingFromOris`, `dialogTitles.syncAllUpcomingFromOris`, `bulkSync.*` labels
- `frontend/src/components/events/BulkSyncOrisModal.tsx` — new component: triggers mutation on mount, shows spinner during `isPending`, switches to summary with success/failure counts + failure list on `isSuccess`
- `frontend/src/pages/events/EventsPage.tsx` — wires `syncAllUpcomingFromOris` template to toolbar button + `BulkSyncOrisModal`; invokes `route.refetch` on sync complete
- `frontend/src/components/events/BulkSyncOrisModal.test.tsx` — 6 new tests (progress state, summary state, onClose, mutation trigger)
- `frontend/src/pages/events/EventsPage.test.tsx` — 3 new tests (button visible/absent, modal opens on click) + `BulkSyncOrisModal` mock

**HAL template name:** `syncAllUpcomingFromOris` (derived from method name by Spring HATEOAS)
**No blocked items.**

---
