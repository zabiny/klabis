# Bulk ORIS sync + Event Types — QA Testing

## Scenarios

### Event Types catalog (admin page)
- [x] **CAT-1**: Admin sees "Typy akcí" link under ADMINISTRACE in the sidebar
- [x] **CAT-2**: Admin opens Event Types page, sees the list (may be empty initially)
- [x] **CAT-3**: Admin creates type "Pohárový závod" with color #ffaa00 → appears in list
- [x] **CAT-4**: Admin creates type "Trénink" with color #00aaff → appears in list
- [x] **CAT-5**: Admin attempts to create "trénink" (case-insensitive duplicate) → rejected with conflict error
- [x] **CAT-6**: Admin renames an event type → name updates
- [x] **CAT-7**: Admin deletes an unused event type → removed from catalog
- [ ] **CAT-8**: Regular member (ZBM9500) does NOT see "Typy akcí" in navigation

### Event Type assignment on Event
- [x] **EVT-1**: Admin opens an event create form → "Typ akce" dropdown contains the catalog types
- [ ] **EVT-2**: Admin creates an event with type "Pohárový závod" → event saved with type (not tested in UI)
- [ ] **EVT-3**: Event detail page shows the type badge with color
- [ ] **EVT-4**: Events list shows "Typ" column with badge for the new event
- [x] **EVT-5**: Admin updates an event to change its type → updated value shown (via API; UI blocked by Issue #2)
- [ ] **EVT-6**: Admin updates an event to clear its type (select empty) → no badge shown
- [ ] **EVT-7**: Admin attempts to delete a type that is in use → 409 with affected event names

### Events list type filter
- [x] **FLT-1**: Filter bar contains "Typ akce" multi-select
- [x] **FLT-2**: Selecting one type filters list to matching events (verified via API)
- [ ] **FLT-3**: Selecting multiple types shows OR-results
- [ ] **FLT-4**: Clearing filter shows all events again
- [ ] **FLT-5**: URL persists selected type ids as repeated `eventTypeId` params

### Bulk ORIS sync
- [x] **SYNC-1**: Admin sees "Synchronizovat všechny budoucí z ORIS" toolbar button on events list
- [ ] **SYNC-2**: Member without EVENTS:MANAGE does NOT see the bulk sync button
- [ ] **SYNC-3**: Clicking opens modal with progress spinner
- [ ] **SYNC-4**: After completion, modal shows summary "X úspěšně synchronizováno, Y chyb"
- [ ] **SYNC-5**: After completion, events list is refreshed

---

## Results

### Iteration 1 (initial run)
| Scenario | Result | Note |
|----------|--------|------|
| CAT-1 | PASS | sidebar item visible |
| CAT-2 | PASS | empty list rendered |
| CAT-3 | PASS | row added with #ffaa00 + sortOrder=0 |
| CAT-4 | PASS | row added with #00aaff + sortOrder=1 |
| CAT-5 | PASS | inline alert "Event type with name 'trénink' already exists" — text in EN |
| CAT-6 | PASS | rename ok |
| CAT-7 | PASS | TempType created and deleted via confirm dialog |
| EVT-1 | PASS | dropdown contains Pohárový závod + Trénink + "—" |
| FLT-1 | PASS | multi-select listbox rendered |
| SYNC-1 | FAIL (iter1) | affordance missing — fixed in iter2 by enabling `oris` profile |
| Rest | SKIP | blocked by issues #1 and #2 |

### Iteration 2 (after backend restart with `oris` profile)
| Scenario | Result | Note |
|----------|--------|------|
| SYNC-1 | PASS | Tlačítko "Synchronizovat všechny budoucí z ORIS" viditelné |
| EVT-5 | PASS via API | PATCH /api/events/{id} with full payload + eventTypeId returns 204 |
| FLT-2 | PASS via API | GET /api/events?eventTypeId=X returns matching event |
| CAT-8, EVT-2..7, FLT-3..5, SYNC-2..5 | NOT EXECUTED | session interrupted at stop-condition limit |

---

## Issues found

### Issue #1 — `oris` Spring profile must be active for bulk-sync affordance — FIXED CONFIG
- **Root cause**: `runLocalEnvironment.sh` did not include the `oris` profile in `SPRING_PROFILES_ACTIVE`. Without it, `OrisEventImportPort` is not registered as a bean, so `orisIntegrationActive` is false in `EventController`, and the `syncAllUpcomingFromOris` affordance is never attached to the events list response.
- **Impact**: SYNC-1..SYNC-5 all blocked.
- **Fix applied**: added `oris` to the default profile list in `runLocalEnvironment.sh` (line 58).
- **Not a code bug** — the production app must always run with the `oris` profile to enable ORIS integration. The script just had a local dev gap. The backend code is correct.

### Issue #2 — Edit modal sends partial PATCH but backend validates all fields as required
- **Symptom**: Opening Upravit modal on an event, changing only the type and submitting yields `Method 'PATCH' is not supported.` (in iter 1) or `Invalid request content. Event organizer is required, eventDate ...` (iter 2 with fresh bytecode).
- **Root cause**: Frontend's HAL-driven update modal posts only changed fields (`{eventTypeId: 'X'}`) as a PATCH body. The backend `EventController.updateEvent` is annotated `@PatchMapping` but its `UpdateEventRequest` Bean Validation requires `name`, `organizer`, `eventDate`, etc. → 400 because the partial body fails validation, not because PATCH is unsupported.
- **Verification**: with a manual full payload (all required fields + eventTypeId), `PATCH /api/events/{id}` returns 204 No Content.
- **Impact**: Blocks UI flow for changing event type on existing events (EVT-2..EVT-7 via UI).
- **Affected component**: either backend (relax validation for PATCH semantics — partial update) OR frontend (always send full event payload when editing). Backend is closer to spec — PATCH means partial — so the right fix is to make the backend accept partial bodies (validate only fields that are present).
- **Note**: The `Method 'PATCH' is not supported.` message reported in iter 1 came from a stale backend running pre-PATCH bytecode at the time. The persistent issue is the validation requirement, not the HTTP method.

### Issue #3 — CAT-5 duplicate-name error is in English
- "Event type with name 'trénink' already exists" — should be Czech to match rest of UI.
- Low priority.

---

## Suggested follow-up

1. Backend: make `UpdateEventRequest` partial-update friendly (mark all fields optional; validate only when present). This matches `@PatchMapping` semantics.
2. Backend: localize `EventTypeNameAlreadyExistsException` message (or have the exception handler return a localized response).
3. Frontend: continue UI verification — when issue #2 is fixed, retest EVT-2..EVT-7 + SYNC-3..5 + CAT-8 + FLT-3..5 in a fresh QA session.
