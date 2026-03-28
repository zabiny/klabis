# Design: Import eventu z ORIS

## Backend

### DB migrace

Přidat sloupec do tabulky `events` v `V001__initial_schema.sql`:

```sql
oris_id INTEGER NULL UNIQUE
```

### Event agregát

Přidat `orisId` (nullable `Integer`) jako interní pole do:
- `Event` — nové private pole `orisId`, getter, rozšíření `Event.create()` o nový factory method `importFromOris(...)`
- `EventMemento` — nový `@Column("oris_id") Integer orisId`, propagace v `from()` a `toEvent()`
- `EventRepository` — nová metoda `existsByOrisId(int orisId)`
- `EventRepositoryAdapter` + `EventJdbcRepository` — implementace `existsByOrisId`

`orisId` se **nevystavuje** v API — není v `EventDto` ani `EventSummaryDto`.

### Import command

Nový record `Event.ImportCommand(int orisId)` — vstup pro import endpoint.

### EventManagementService

Nová metoda `importEventFromOris(int orisId)` na rozhraní i implementaci:
1. Ověří `existsByOrisId` → 409 pokud duplikát
2. Volá `OrisApiClient.getEventDetails(orisId)`
3. Mapuje `EventDetails` → pole pro `Event.createFromOris()`
4. Uloží a vrátí nový `Event`

Mapování:
- `name` ← `EventDetails.name()`
- `eventDate` ← `EventDetails.date()`
- `location` ← `EventDetails.place()`
- `organizer` ← první neprázdný z `org1().abbr()`, `org2().abbr()`, fallback `"---"`
- `websiteUrl` ← `"https://oris.ceskyorientak.cz/Zavod?id={orisId}"`
- `orisId` ← vstupní `orisId`

### Výjimky

- `DuplicateOrisImportException` (409) — v balíčku `events.application`

### EventController

Nový endpoint:
```
POST /api/events/import
Body: { "orisId": 9876 }
Vyžaduje: EVENTS:MANAGE
Odpověď: 201 + Location header na nový event
```

`importFromOris` affordance na seznam eventů — podmíněno přítomností ORIS beanu (kontext obsahuje bean `OrisApiClient`):
```java
// v listEvents():
if (orisApiClient.isPresent()) {
    selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).importEvent(null)));
}
```

`OrisApiClient` injektován jako `Optional<OrisApiClient>` do `EventController`.

### OrisController (nový, v oris modulu)

```
GET /api/oris/events
Vyžaduje: EVENTS:MANAGE
Odpověď: List<{ id, name, date }>
```

Aktivní jen pokud je profil `oris` (`@OrisIntegrationComponent`).

Volá `OrisApiClient.getEventList(OrisEventListFilter.EMPTY.withDateFrom(LocalDate.now()).withDateTo(LocalDate.now().plusYears(1)))`

Response DTO: jednoduchý record `OrisEventSummary(int id, String name, LocalDate date)`.

## Frontend

### Tlačítko Import

V `EventsPage.tsx` přidat `HalFormButton` pro affordance `importFromOris` — ale tato affordance **nevede na HAL form** (je to vlastní overlay s select boxem, ne generický HAL form). Proto místo `HalFormButton` použijeme přímý button s podmíněným renderováním na základě existence `_templates.importFromOris`.

### ImportOrisEventModal komponenta

Nová komponenta `src/components/events/ImportOrisEventModal.tsx`:
- Přijímá `isOpen`, `onClose`, `importHref` (URL z `_templates.importFromOris.target`)
- Při otevření fetchuje `GET /api/oris/events` (autorizovaný fetch)
- Zobrazuje loading → select box s opcemi `"{date} — {name}"` → submit
- Submit: `POST importHref` s `{ orisId: selectedId }`
- 201: zavři modal + redirect na `Location` header
- 409: zobraz "Tento závod již byl importován"
- Jiná chyba: obecná hláška

### Localization

Přidat do `labels.ts`:
- `templates.importFromOris` — label tlačítka
- `dialogs.importFromOris` — titulek modalu
- Chybové hlášky pro 409 a obecnou chybu
