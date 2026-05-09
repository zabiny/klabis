## Context

`events` capability má dnes per-row akci „Synchronizovat" pro ORIS-importované eventy. Bulk operations chybí. ORIS workflow vyžaduje, aby pořadatelé doplňovali detaily akcí postupně po jejich založení s minimálním obsahem (datum + název) — což znamená, že manager musí periodicky kontrolovat ORIS aktualizace ručně, jednu po druhé.

Eventy nemají typovou kategorizaci (event type / level). Aktuální model zná pouze `status` (DRAFT/ACTIVE/FINISHED/CANCELLED), `organizer`, `coordinator`, `categories` (kategoriové výsledky závodu). Filter bar nabízí jen time window (Budoucí/Proběhlé/Vše) + fulltext + Moje přihlášky. Pro členy bez UX rozlišení trénink vs. závod vs. mistrovství je vyhledávání pomalé.

Tento proposal řeší obě věci současně — sdílí target plochu (manager UI) a technicky závisí na stejné Event aggregate. Pokud se rozsah ukáže příliš velký, lze rozdělit:
1. Phase A = N5 (jen bulk sync action).
2. Phase B = N13 (event types capability + filter + ORIS mapping).

Tasks queue je připravený sekvenčně; Phase A může jet samostatně.

## Goals / Non-Goals

**Goals:**
- Manager s `EVENTS:MANAGE` může jediným klikem spustit synchronizaci všech budoucích ORIS-importovaných akcí (DRAFT/ACTIVE).
- Bulk sync je odolný k dílčí chybě — pokračuje pro ostatní, na konci vrací summary.
- Klabis má číselník typů akcí (`event-types`) spravovaný adminem.
- Eventy mohou mít volitelný typ; tabulka akcí umí filtrovat podle typu.
- ORIS import auto-mapuje Level/Discipline na číselníkový typ podle názvu.

**Non-Goals:**
- Asynchronní sync přes message queue (zatím sekvenční synchronní call — KISS, počty akcí v desítkách).
- Real-time progress streaming (Server-Sent Events / WebSocket) — frontend ukáže `loading` overlay + final summary, prozatím dostatečné.
- Strukturované enum typů (např. fixed list `TRAINING/RACE/CHAMPIONSHIP`). Číselník je editovatelný uživateli.
- Multi-language názvy typů (Klabis je jen česky).
- Inheritance / hierarchie typů.

## Decisions

### Decision 1: Bulk sync — synchronní endpoint s aggregate response

Endpoint: `POST /api/events/sync-from-oris/all-upcoming` (přesný path do diskuze; alt. `POST /api/oris-events/sync-all`). Body je prázdný (server si selektuje matching events sám).

Response (HAL+FORMS):
```json
{
  "totalProcessed": 13,
  "successCount": 12,
  "failureCount": 1,
  "results": [
    { "eventId": "...", "name": "Krajský přebor", "status": "synced" },
    { "eventId": "...", "name": "Letní pohár", "status": "failed", "error": "ORIS endpoint returned 404" }
  ]
}
```

Authority gating: `@HasAuthority(EVENTS_MANAGE)`. Idempotence zajištěna přesně tak, jak je u per-row sync (overwrite from ORIS source of truth).

**Frontend UX:**
- Tlačítko v toolbar nad tabulkou akcí, exposed přes HAL+FORMS affordance v list response.
- Klik → modal s progress (spinner + „Synchronizuji X akcí...").
- Po dokončení modal ukáže summary („12 úspěšně synchronizováno, 1 chyba — Letní pohár: ORIS endpoint returned 404") s tlačítkem „Zavřít".

**Alternative considered:**
- *Asynchronní operace s polling endpoint* — overkill pro desítky eventů, prosté HTTP request stačí (timeout settings v reverse proxy).
- *Per-event SSE streaming* — pěkné UX, ale rozšiřuje complexity. Final summary v modálu postačuje.

### Decision 2: Bulk sync — dílčí chyby pokračují, neselží celá operace

Service iteruje matching events; pro každý zkusí `OrisEventSyncService.sync(event)`; chytí výjimku, zaloguje, přidá do `failed` výsledku. Po skončení všech vrátí aggregate response. Klient vždy dostane 200 OK (pokud autorizace a DB nejsou rozbité); failure count v body.

**Alternative considered:**
- *Při první chybě vrátit 500* — manager by ztratil možnost vidět, kolik dalších eventů by se synchronizovalo. Špatný UX.
- *Transakční rollback při dílčí chybě* — žádný atomic semantic není potřeba (každý event je nezávislý).

### Decision 3: EventType — samostatný aggregate v package `com.klabis.events.eventtype`

```java
public record EventType(
        EventTypeId id,
        String name,        // unique, max 100 chars
        Optional<String> color,  // hex like #aabbcc
        int sortOrder,
        AuditMetadata audit
) implements KlabisAggregateRoot<EventType, EventTypeId> { }
```

Repository `EventTypeRepository` (read by id + list all sorted by `sortOrder`). Mapping přes `EventTypeMemento` (single table `event_types`). Application service `EventTypeManagementService` (CRUD).

**Smazání používaného typu:** `Event.eventTypeId` cizí klíč na `event_types(id)` — `delete` typu projde pouze pokud žádný event ho neodkazuje. Service vyhodí `EventTypeInUseException` → REST 409 Conflict s informativní hláškou.

### Decision 4: Event.eventType — Optional field

```java
private Optional<EventTypeId> eventType;
```

Persistence: nullable column `event_type_id UUID NULL REFERENCES event_types(id)`. Pro existující eventy zůstává null (žádný auto-fill).

UI:
- Form: dropdown „Typ akce" (volitelný), položka „—" pro "neuvedeno".
- Detail: pokud typ je nastavený, zobrazí se v sekci základních informací s color badge.
- Tabulka: nový sloupec „Typ" — color badge + jméno; pokud není set, prázdná buňka.

### Decision 5: ORIS import auto-mapping — case-insensitive lookup podle názvu

Při importu z ORIS:
1. Service načte ORIS event payload.
2. Z payload vyextrahuje typu identifikátor (Level / Discipline — to záleží na ORIS API; do design zatím nech otevřené, implementace si zvolí konkrétní pole).
3. Lookup v `EventTypeRepository` přes `findByNameIgnoreCase(orisLevel)`.
4. Pokud match — `event.eventType = matchedType`.
5. Pokud no match — `event.eventType = empty`. Manager si typ doplní ručně přes form.

Žádný auto-create EventType při importu (admin musí explicitně schválit nový typ). Důvod: ORIS Level/Discipline obsahuje desítky variant a rovnou je všechny vytvořit jako Klabis EventType je nežádoucí.

**Alternative considered:**
- *Auto-create při importu* — záplava jednorázových položek v číselníku.
- *Strukturovaný mapping table (ORIS Level → Klabis EventType)* — overhead pro maintainera; case-insensitive name lookup pokrývá většinu případů.

## Risks / Trade-offs

- **[Risk] Bulk sync trvá příliš dlouho a HTTP timeout** → Mitigation: počet matching events je v desítkách (klubové akce na sezónu). Per-event sync je v ms (jen REST call do ORIS). Worst case ~30s — well within HTTP timeout (60s default Tomcat). Pokud se ukáže problém, přejít na async (samostatný proposal).
- **[Risk] EventType cizí klíč na events.event_type_id ztěžuje cleanup** → Mitigation: `EventTypeInUseException` chrání před orphans. UI "smazat" tlačítko nezobrazí pro typy, které jsou v užívání (HAL affordance conditional).
- **[Risk] ORIS auto-mapping je křehké (změna ORIS Level názvu rozbije lookup)** → Mitigation: žádný error pro no-match, jen ponechá typ empty. Maintainer si doplní typy do číselníku ručně po prvním importu.
- **[Trade-off] Sloučení 2 featur do 1 proposalu** — proposal je delší než ostatní. Pokud implementační agent narazí na problém, lze rozdělit při task queue execution (Phase A=N5, Phase B=N13).

## Migration Plan

**Phase A — Bulk sync (N5):**
1. Backend: nový endpoint + service (delegování na existující per-event sync).
2. Frontend: toolbar tlačítko + modal s progress + summary.
3. E2E test proti `https://api.klabis.otakar.io`.

**Phase B — Event types (N13):**
1. DB migrace: `event_types` table + `event_type_id` na events.
2. Domain + persistence + REST: `EventType` aggregate, `EventTypeManagementService`, controller s CRUD.
3. Event aggregate: přidat optional `eventType`.
4. ORIS auto-mapping helper.
5. Frontend admin stránka „Typy akcí".
6. Frontend event form / detail / table column / filter bar.
7. E2E test.

Migrace dat: žádná (existing events mají `eventType = null`).

Rollback: revert commits per phase. Phase A je zcela samostatná.

## Open Questions

- ~~**DB migrace V001 update vs. V004**~~ — **vyřešeno (2026-04-29):** production stále běží na H2 bez perzistentních dat. In-place update `V001__initial_schema.sql` je OK pro tento proposal i pro 1.4 / 1.9.
- **ORIS Level vs. Discipline** — který field z ORIS Event payload má být zdrojem auto-mapping? Implementační agent si zvolí; rozhodnutí dokumentuje v `developerManual`.
- **Case-insensitive uniqueness na EventType.name** — je v DB constraint (`UNIQUE(LOWER(name))`)? Doplnit v Phase B implementaci.
- **Default sort order položek** — `sortOrder` field; nově vytvořené typy mají `MAX(sortOrder)+1` při create. Potřeba reorder UI? Out of scope, manuálně přes `update`.
