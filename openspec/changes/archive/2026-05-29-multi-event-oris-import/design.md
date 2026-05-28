## Context

ORIS import dnes běží přes `POST /api/events/import` s tělem `{orisId}`, vrací `201 Created` + `Location` na nový event. Service `OrisEventImportService.importEventFromOris(int orisId)` fetchne ORIS detail, vytvoří `Event` v DRAFT a uloží; při duplicitě hází `DuplicateOrisImportException` (409).

Projekt už má hotový vzor pro hromadné zpracování s tolerancí částečného selhání: `OrisEventController.syncAllUpcomingFromOris()` → `BulkSyncResult { totalProcessed, successCount, failureCount, results[] }`, vrací vždy `200` a per-event status `synced|failed`. Frontend `BulkSyncOrisModal` tento výsledek zobrazuje. Multi-event import navrhujeme přesně podle tohoto vzoru, aby byl kód i UX konzistentní.

Seznam dostupných ORIS závodů poskytuje `GET /api/oris/events?region=...` → `OrisEventSummary[]`.

## Goals / Non-Goals

**Goals:**
- Dávkový import více ORIS závodů jedním požadavkem, per-závod výsledek, žádný all-or-nothing.
- Znovupoužít stávající `importEventFromOris` na jednotlivé závody (žádná duplikace import logiky).
- UX konzistentní s `BulkSyncOrisModal` a s nakresleným Pencil designem (multi-select → výsledkový panel).
- Single-event import zůstává funkční jako výběr jednoho.

**Non-Goals:**
- Plně automatická periodická synchronizace.
- "Vybrat vše v období" / rychlé filtry.
- Změna ORIS klienta nebo datového modelu `Event`.
- Odstranění stávajícího `POST /api/events/import` (zůstává; batch je samostatný endpoint).

## Decisions

**1. Nový dávkový endpoint `POST /api/events/import-batch` místo cyklu na frontendu.**
Tělo `{ "orisIds": [int, ...] }`, návrat `200` + `BulkImportResult`. Zpracování sekvenční, každý závod ve vlastní transakci přes existující `importEventFromOris`; výjimka jednoho se zachytí a zapíše jako `failed`, pokračuje se dál.
- *Proč ne cyklus N requestů z FE:* hůř se řeší atomická per-request transakce, progres a 409/4xx mapování; server-side dávka je jeden zdroj pravdy a kopíruje zavedený bulk-sync pattern.

**2. Výsledkový DTO podle `BulkSyncResult`.**
`BulkImportResult { totalProcessed, successCount, failureCount, results: [{ orisId, name, date, status: imported|failed, error? }] }`. Pole `name`/`date` se plní z ORIS detailu (u úspěchu z importovaného eventu, u selhání z ORIS summary, pokud je k dispozici), aby výsledkový panel mohl ukázat "$DATUM $NÁZEV" i u chyb.
- *Proč ne sdílet doslova `BulkSyncResult`:* sync klíčuje `eventId` (existující event), import klíčuje `orisId` (ještě neexistuje) — samostatný record je čistší než ohýbat jeden pro obě role.

**3. Per-závod transakce, ne jedna velká.**
Každý import je `@Transactional` na úrovni `importEventFromOris`; dávková metoda transakci nedrží. Selhání jednoho tak nerollbackne už uložené.

**4. HAL+FORMS affordance `importEventsBatch` na `GET /api/events`.**
Vedle stávajícího `importEvent` (ponecháme kvůli zpětné kompatibilitě a single-flow). Frontend preferuje `importEventsBatch`, pokud je přítomna. Affordance se přidává jen při aktivní ORIS integraci a EVENTS:MANAGE — stejně jako dnes.

**5. Frontend: rozšířit `ImportOrisEventModal` + `useOrisEventImport` na multi-select.**
Stav výběru `selectedIds: Set<number>`, checkboxy v seznamu, "Vybrat vše" (tristate), submit přes batch endpoint, po dokončení přepnutí na výsledkový panel (per-závod ✓/✗), tlačítko Hotovo zavře a refreshne seznam. Vizuál podle Pencil overlayů (`Events - OrisImport*Overlay`). Pro nižší desítky závodů: scroll se seznamem fixní výšky + počítadlo + lišta vybraných.

## Risks / Trade-offs

- **Dlouhá dávka blokuje request** (desítky závodů × ORIS latence) → import je řádově jednotky/desítky závodů; sekvenční zpracování je v rámci <500ms cíle hraniční, ale akce je explicitní správcovská a UI ukazuje průběh. Zůstává synchronní (stejně jako bulk-sync). Asynchronní zpracování je mimo rozsah.
- **Dva import endpointy** (`import` + `import-batch`) → drobná duplicita afordancí; zmírněno tím, že batch je tenký wrapper nad stejnou service metodou. Pozdější sloučení do jednoho (vždy seznam) je možný follow-up.
- **Per-závod `name/date` u chyb** závisí na dostupnosti ORIS summary → když chybí, zobrazí se aspoň `orisId`.

## Open Questions

Žádné — API tvar (batch endpoint), transakční chování (částečný úspěch) i UX po importu (výsledkový panel) jsou rozhodnuté.
