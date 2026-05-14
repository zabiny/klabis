# TCF: review-1-8-table-sort-persistence

## Goal
Implement per-user (per-browser via localStorage) sort persistence for application data tables.
URL > localStorage > defaultSort precedence. Reset action clears localStorage entry.

## Scope (frontend-only)
- New hook `useTableSort(tableId, defaultSort)` in `frontend/src/hooks/`
- Wire into existing tables: events, members, groups.free, groups.family, groups.training, calendar-items, category-presets, event-types
- Reset action ("Resetovat řazení") in column header context menu in `KlabisTable`
- Tests (unit + integration where applicable)

## Existing assets
- `frontend/src/hooks/usePersistedState.ts` — generic localStorage wrapper (good base, JSON-based)
- `frontend/src/hooks/useDefaultSearchParam.ts` — URL default applier
- `KlabisTable` component family

## Decisions
- localStorage key: `klabis.table.<tableId>.sort`, string `"column,direction"` (per design)
- Try/catch for parse errors → fallback to default
- URL with `?sort=` always wins; user click updates both URL and localStorage

## Iterations
1. Implement `useTableSort` hook + tests
2. Wire to tables (vertical slices, one or grouped)
3. Add reset UI in KlabisTable
4. Browser E2E smoke test, simplify review, fixes, commit

## Subagent log

### Iteration 1 — 2026-05-15: useTableSort hook + tests

Created `frontend/src/hooks/useTableSort.ts` and `frontend/src/hooks/useTableSort.test.tsx`.

**Hook API:** `useTableSort(tableId: string, defaultSort: SortState)` returns `{ sort, setSort, reset }`.

**Behavior implemented:**
- Initial state: URL `?sort=col,dir` wins, then localStorage `klabis.table.<tableId>.sort`, then defaultSort.
- `setSort`: writes `"col,dir"` plain string (not JSON) to localStorage AND URL `?sort` param simultaneously.
- `reset`: removes localStorage entry, removes `?sort` from URL, restores defaultSort.
- Parse errors and invalid values (missing comma, bad direction, empty column) silently fall back to default.

**Tests:** 16 unit tests covering all precedence cases, invalid/corrupt storage, setSort writes, reset, and tableId isolation.

**Full test suite:** 1451/1451 passed — no regressions.

**Open issues:** None. Hook is ready for iteration 2 (wiring into KlabisTableWithQuery and table consumers).

---

### Iteration 2 — 2026-05-15: Wire useTableSort into KlabisTableWithQuery + consumer call sites

**Approach:** Split `KlabisTableWithQuery` into three internal components: `KlabisTableCore` (shared fetch/pagination logic, receives sort as props), `KlabisTableWithSortPersistence` (calls `useTableSort`, rendered when `tableId` + `defaultOrderBy` both present), and `KlabisTableWithLocalSort` (plain `useState`, existing behaviour). The outer `KlabisTableWithQuery` dispatches to one of the two variants. This avoids conditional hook calls while keeping backward compat fully intact.

**tableId added to `HalEmbeddedTable` props** and forwarded to `KlabisTableWithQuery`.

**Call sites wired (7/8):**
- `events` → `EventsPage.tsx`
- `members` → `MembersPage.tsx`
- `groups.free` → `GroupsPage.tsx`
- `groups.family` → `FamilyGroupsPage.tsx`
- `groups.training` → `TrainingGroupsPage.tsx`
- `category-presets` → `CategoryPresetsPage.tsx`
- `event-types` → `EventTypesPage.tsx`

**Skipped:** `calendar-items` — `CalendarPage` uses a hand-rolled `_embedded` extraction loop, not `HalEmbeddedTable`. Wiring requires a separate refactor not in scope for this iteration.

**Tests:** 1 new test added to `KlabisTableWithQuery.test.tsx` (sort change writes `klabis.table.<tableId>.sort` to localStorage). Total: 1452/1452 passed (was 1451).

---

### Iteration 3 — 2026-05-15: Reset action UI in KlabisTable

**Approach:** Option A — small `RotateCcw` (lucide-react) icon button rendered inline next to the sort direction indicator on the currently sorted column header. Visible only when `onSortReset` prop is defined AND a column is actively sorted.

**Files changed:**
- `frontend/src/components/KlabisTable/types.ts` — added `onSortReset?: () => void` to `KlabisTableProps`
- `frontend/src/components/KlabisTable/KlabisTable.tsx` — destructures `onSortReset`, renders `RotateCcw` icon button with `title="Resetovat řazení"` and `e.stopPropagation()` to avoid toggling sort
- `frontend/src/components/KlabisTable/KlabisTableWithQuery.tsx` — added `onSortReset` to `KlabisTableCoreProps`; `KlabisTableWithSortPersistence` passes `reset` from `useTableSort`; `KlabisTableWithLocalSort` passes nothing (undefined → button hidden)

**Tests:** +6 tests (4 in `KlabisTable.test.tsx`, 2 in `KlabisTableWithQuery.test.tsx`). Total: 1458/1458 passed (was 1452).

**Blockers:** None.

---

### Simplify pass — 2026-05-15

**Reviewed:** `useTableSort` hook, `KlabisTableWithQuery` (Core / WithSortPersistence / WithLocalSort split), `KlabisTable`, `HalEmbeddedTable`, page wire-ups.

**Findings:**

- **Three-component split in `KlabisTableWithQuery`** — justified. `useTableSort` (hook with `useSearchParams`) cannot be called conditionally, so dispatching to two sibling components is the correct React pattern. No collapse warranted.
- **`useTableSort` parse logic** — clean and minimal. `lastIndexOf(',')` correctly handles column names containing commas. Not over-engineered.
- **Dead props / unused code** — none found.
- **`SortState` type duplication** — `useTableSort.ts` and `types.ts` each independently defined an identical `SortState` interface. Applied: removed the definition from `types.ts` and replaced it with an import + re-export from `useTableSort`. `KlabisTableWithQuery` already imported from the hook; no other consumers. One source of truth now.

**Simplification applied:** `types.ts` — remove duplicate `SortState` definition; import and re-export from `../../hooks/useTableSort`.

**Tests after simplify pass:** 1458/1458 passed — no regressions.

**Browser E2E deferred to verify-change phase.**

---

### Review findings — 2026-05-15: Apply 3 high-priority fixes

**Fix 1 (high) — EventsPage timeWindow sort isolation:** Changed `tableId="events"` to `` tableId={`events.${filterValue.timeWindow}`} `` in `EventsPage.tsx`. Each time window (`upcoming`, `past`, etc.) now has its own localStorage key, so persisted sort from one window no longer bleeds into another. The `key={timeWindow}` remount prop was kept (still prevents stale pagination).

**Fix 2 (low-medium) — Remove non-null assertion in KlabisTableWithSortPersistence:** Extracted a new `KlabisTableWithSortPersistenceProps` interface that overrides `defaultOrderBy` as `string` (non-optional). The dispatcher guard `tableId && defaultOrderBy` already enforces this at runtime; the type now reflects it. Removed `defaultOrderBy!`.

**Fix 3 (high) — Reset test gap:** Extended the existing reset test to also assert that the fetch after reset uses the default sort (`sort=name%2Casc`), not only that localStorage is cleared. This verifies the table actually reverts to the default sort visually/functionally, not just in storage.

**Tests:** 1458/1458 passed — no regressions.
