## Why

Z review aplikace 2026-04-29 vyplynuly dvě nové funkce nad oblastí akcí, které dohromady reprezentují organizační must-have před sezónou:

1. **N5 — Hromadná synchronizace všech budoucích ORIS-importovaných akcí.** Aktuálně existuje pouze per-row akce „Synchronizovat", která vyžaduje, aby manager klikl tlačítko pro každou akci zvlášť. V praxi pořadatelé zakládají akce v ORIS jen s datem a názvem; detaily (uzávěrky, kategorie, lokace, web, koordinátor) doplňují postupně během následujících týdnů. Klabis by měl umět **jediným tlačítkem** synchronizovat všechny budoucí ORIS-importované akce ve stavu DRAFT/ACTIVE — to je pro správce klíčové při přípravě sezóny.
2. **N13 — Editovatelný číselník typů akcí + filtrování podle typu.** Eventy aktuálně nemají typové kategorie (např. „Klubový závod", „Pohárový závod", „Trénink", „Mistrovství", „Závod žebříčku" …). Členové klubu chtějí filtrovat seznam akcí podle typu, aby si rychle našli relevantní akce. Manager s `EVENTS:MANAGE` autoritou bude číselník typů spravovat (CRUD jako u Category Presets).

Společný kontext: obě funkce rozšiřují existující `events` capability o nové operace — bulk action a nový atribut `type` + nová admin capability pro číselník. Sloučení do jedné change je ale méně přirozené než u 1.4 (úpravy existujících scénářů). Tady jde o **dvě paralelní featury**, které mohou být doručeny v libovolném pořadí. Přesto je sloučím, protože:
- Obě řeší organizační rutinu manageru s `EVENTS:MANAGE`.
- Obě se dotýkají stejné `events` table view (filter bar, action toolbar).
- Sdílí frontend tlačítko UX (přidání nové action do detail/list).

Pokud se ukáže, že rozsah je příliš velký, lze ho rozdělit při implementaci na dvě fáze (N5 first, N13 second) — to popisuje task queue.

## What Changes

### N5 — Bulk ORIS sync

- **Modifikace** *Row-Level Management Actions in Events Table* v `events`: přidat globální (toolbar-level, ne per-row) akci „**Synchronizovat z ORIS všechny budoucí**" pro uživatele s `EVENTS:MANAGE`.
- **Chování:**
  - Akce se vykoná pouze nad eventy splňujícími: `status IN (DRAFT, ACTIVE)` AND `eventDate >= today` AND ORIS-imported (má `orisEventId`).
  - Server zpracuje sekvenčně všechny matching events, pro každý udělá totéž co per-row sync.
  - **Při dílčí chybě** (jeden event selže): pokračuje s ostatními, na konci vrátí summary (počet úspěšných, počet selhaných, seznam chybných eventů).
  - Frontend zobrazí progress UI a po dokončení summary („Synchronizováno: 12 ✓, 1 chyba: Akce X — důvod Y").
- **Idempotence:** synchronizace stejných dat dvakrát nemění výsledek (jako per-row sync).

### N13 — Číselník typů akcí + filtrování

- **Nová capability** `event-types` (analogie `category-presets`):
  - CRUD nad `EventType { id, name, color (optional, hex), sortOrder }` přes `/api/event-types` endpointy.
  - Spravuje uživatel s `EVENTS:MANAGE` (stejný authority gating jako pro category presets).
  - Operace: list, create, update, delete (s ochranou — nelze smazat typ používaný akcí).
- **Modifikace** Event aggregate (`events`):
  - Přidat optional `eventType: EventTypeId` field na `Event`.
  - Create/Update event form přidává volitelné dropdown „Typ akce" (z číselníku).
- **Modifikace** *Events Table Display*:
  - Volitelný sloupec „Typ" zobrazující název typu, badge ve barvě typu (pokud je nastavená).
  - Filter bar: nový filtr „Typ akce" (multi-select dropdown).
- **ORIS import:**
  - ORIS má vlastní typologii akcí (`Discipline`, `Level`); navrhujeme automaticky **mapovat ORIS Level/Discipline na číselníkový typ** podle názvu (case-insensitive lookup); pokud není match, ponechá `eventType = null`. Manager může později ručně přiřadit typ.
- **Spec update:** přidat scénář v `events` o filtrování podle typu, scénář o zobrazení sloupce.

## Capabilities

### New Capabilities

- `event-types` — CRUD číselník typů akcí spravovaný uživateli s `EVENTS:MANAGE`. Endpoint base `/api/event-types`. Slouží jako lookup pro `Event.eventType`.

### Modified Capabilities

- `events`:
  - *Row-Level Management Actions in Events Table* — přidat global toolbar action „Synchronizovat všechny budoucí z ORIS" (N5).
  - *Events Table Display* / *Events Table View* — sloupec „Typ", filtr „Typ akce" (N13).
  - *Get Event Detail* — zobrazení typu akce (N13).
  - Vznik / úprava akce — povinné/volitelné pole „Typ akce" (volitelné).

## Impact

- **Backend kód:**
  - **N5:** nový endpoint `POST /api/events/sync-all-oris` (nebo `/api/oris-events/sync-all`); service iteruje matching events, deleguje na existující per-event sync; aggregate response s success/error rozpisem.
  - **N13:**
    - Nový module / package `com.klabis.events.eventtype` s aggregate `EventType`, repository, controller, application service.
    - Nová DB tabulka `event_types(id, name, color, sort_order, audit fields)`.
    - Migrace: přidat sloupec `event_type_id UUID NULL REFERENCES event_types(id)` na `events` tabulku.
    - ORIS auto-mapping: helper service, která lookupuje EventType podle názvu (case-insensitive) při importu.
    - REST: CRUD endpointy pro `event_types`, plus rozšíření Event create/update DTO o `eventTypeId`.
  - Migrace: do V001 (nebo V004 pokud je třeba samostatná migrace, viz Open Questions).
- **Frontend kód:**
  - **N5:** tlačítko v toolbar `EventsListPage` ovládáno HAL+FORMS affordance; loading state s progress; výsledný dialog se summary.
  - **N13:**
    - Nová admin stránka „Typy akcí" v sekci ADMINISTRACE (vedle „Šablony kategorií") — CRUD list/edit.
    - Event create/update form: dropdown „Typ akce" (z číselníku).
    - Events table: sloupec „Typ" s color badge.
    - Filter bar: multi-select „Typ akce".
- **Lokalizace:** doplnit `src/localization/labels.ts`.
- **Dokumentace:** `developerManual` aktualizovat o ORIS Level/Discipline → EventType mapping.
