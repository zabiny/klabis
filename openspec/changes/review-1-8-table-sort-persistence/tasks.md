## 1. useTableSort hook

- [x] 1.1 Create `useTableSort(tableId: string, defaultSort: { column, direction })` hook under `frontend/src/hooks/`
- [x] 1.2 Implement initial state resolution: URL > localStorage > defaultSort
- [x] 1.3 Implement sort change handler: writes to URL AND localStorage
- [x] 1.4 Implement `reset()` function: clears localStorage entry, updates URL to defaultSort
- [x] 1.5 Handle localStorage parse errors gracefully (fall back to default)
- [x] 1.6 Unit tests: cover all precedence cases (URL only, localStorage only, both, neither), reset flow, parse error fallback

## 2. Apply to existing tables — vertical slices

- [x] 2.1 Refactor events list table to use `useTableSort('events', defaultSort)`; verify URL behavior unchanged for shared links
- [x] 2.2 Refactor members list table to use `useTableSort('members', defaultSort)`
- [x] 2.3 Refactor free groups table to use `useTableSort('groups.free', defaultSort)`
- [x] 2.4 Refactor family groups table to use `useTableSort('groups.family', defaultSort)`
- [x] 2.5 Refactor training groups table to use `useTableSort('groups.training', defaultSort)`
- [ ] 2.6 Refactor calendar items table to use `useTableSort('calendar-items', defaultSort)` — SKIPPED: CalendarPage uses custom _embedded rendering, not HalEmbeddedTable
- [x] 2.7 Refactor category presets table to use `useTableSort('category-presets', defaultSort)` (admin)
- [x] 2.8 If proposal 1.5 is merged: refactor event types admin table to use `useTableSort('event-types', defaultSort)`

## 3. Reset action UI

- [x] 3.1 Add "Resetovat řazení" item to the column header context menu (or as a small icon in the header) in `KlabisTable`
- [x] 3.2 Wire it to the hook's `reset()` function
- [x] 3.3 Frontend test: click reset → preference cleared → table renders with default sort

## 4. End-to-end verification

- [ ] 4.1 Browser test: sort events by name → reload → sort persists
- [ ] 4.2 Browser test: sort members by lastName → close tab → reopen → sort persists
- [ ] 4.3 Browser test: paste shared URL `?sort=date,desc` → URL wins over persisted
- [ ] 4.4 Browser test: reset → preference cleared, default sort restored

## 5. Documentation

- [ ] 5.1 Update memory `project_navigation_architecture.md` if relevant (mention the new hook and storage namespace)
- [ ] 5.2 Sync the spec change into `openspec/specs/non-functional-requirements/spec.md` after archiving
