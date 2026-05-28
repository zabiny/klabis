## 1. Backend — dávkový import (TDD)

- [x] 1.1 Přidat `BulkImportResult` record (`totalProcessed`, `successCount`, `failureCount`, `results: [{orisId, name, date, status, error}]`) do `com.klabis.events`
- [x] 1.2 Rozšířit `OrisEventImportPort` o `BulkImportResult importEventsFromOris(List<Integer> orisIds)`
- [x] 1.3 Napsat test pro `OrisEventImportService.importEventsFromOris`: import více závodů, jeden selže (duplicate) → ostatní úspěšné, failureCount=1, per-závod status správně
- [x] 1.4 Implementovat `importEventsFromOris` v `OrisEventImportService` — sekvenční volání existujícího `importEventFromOris` na jednotlivé orisIds, zachycení výjimky → `failed` se zprávou, pokračování dál
- [x] 1.5 Přidat `POST /api/events/import-batch` do `OrisEventController` (`@HasAuthority(EVENTS_MANAGE)`, tělo `{orisIds:[...]}`, návrat 200 + `BulkImportResult`)
- [x] 1.6 `@WebMvcTest` pro batch endpoint: 200 s per-závod výsledkem, validace neprázdného seznamu, autorizace EVENTS:MANAGE
- [x] 1.7 Přidat HAL+FORMS affordance `importEventsBatch` na self link `GET /api/events` (jen při aktivní ORIS integraci + EVENTS:MANAGE), vedle stávajícího `importEvent`; ověřit v testu listEvents

## 2. Frontend — multi-select dialog a výsledkový panel

- [x] 2.1 Rozšířit `frontend/src/api/orisEvents.ts` / typy o batch import (request `{orisIds:number[]}`, `BulkImportResult` typ)
- [x] 2.2 Rozšířit hook `useOrisEventImport` o výběr více závodů (`selectedIds: Set<number>`, toggle, vybrat vše/tristate) a dávkový submit na `importEventsBatch` affordance
- [x] 2.3 Přepracovat `ImportOrisEventModal` na multi-select: checkbox seznam závodů (datum, organizátor, název), "Vybrat vše", footer se souhrnem výběru a počtem; vizuál dle Pencil `Events - OrisImportMultiOverlay`
- [x] 2.4 Pro nižší desítky závodů: scrollovatelný seznam fixní výšky + počítadlo "zobrazeno X z Y" + lišta vybraných (dle `Events - OrisImportLargeListOverlay`)
- [x] 2.5 Výsledkový panel po dokončení importu: per-závod ✓/✗ se zprávou, souhrn "Naimportováno N z M", tlačítko Hotovo → zavře a refreshne seznam (dle `Events - OrisImportResultOverlay`)
- [x] 2.6 Stavy loading (seznam) a empty (žádné závody v oblasti) dle `Events - OrisImportLoadingOverlay` / `Events - OrisImportEmptyOverlay`
- [x] 2.7 Napojit v `EventsPage` — preferovat `importEventsBatch` affordance; zachovat zobrazení tlačítka jen při dostupné affordanci
- [x] 2.8 Frontend testy: render checkbox seznamu, vybrat vše, disabled submit při 0 výběru, zobrazení výsledkového panelu s per-závod stavem

## 3. Spec sync a uzavření

- [x] 3.1 Synchronizovat delta spec do `openspec/specs/events/spec.md` (přidat requirement Multi-Event ORIS Import)
- [ ] 3.2 Ověřit success criteria úkolu `review-2-02` a uzavřít GitHub issue #281 (`gh issue close 281 --repo zabiny/klabis --reason completed`)
