## Why

Importovat akce z ORIS lze dnes jen po jedné — pro správce, který na začátku sezóny zakládá desítky závodů, je to zdlouhavé. Hromadný výběr a import v jedné dávce tuto rutinu výrazně zkrátí. (GitHub issue #281)

## What Changes

- Dialog "Importovat z ORIS" umožní vybrat **více závodů současně** (checkboxy v seznamu) místo jednoho.
- Po potvrzení se vybrané závody naimportují v jedné dávce. Import je **per-závod** — selhání jednoho (např. už existuje) nezastaví ostatní (žádný all-or-nothing).
- Uživatel po dokončení uvidí **souhrn výsledku** s indikátorem úspěch/chyba pro každý závod.
- Backend dostane nový dávkový endpoint, který přijme seznam ORIS ID a vrátí per-závod výsledek (úspěch/chyba), konzistentně s existujícím bulk-sync chováním.
- Existující import jediného závodu zůstává funkční jako hraniční případ (výběr právě jednoho).

## Capabilities

### New Capabilities
<!-- žádné nové capability — jde o rozšíření existujícího events bounded contextu -->

### Modified Capabilities
- `events`: Požadavek na ORIS import se rozšiřuje ze single-event na multi-event dávkový import s per-závod výsledkem a tolerancí částečného selhání. Stávající single-event scénáře zůstávají platné jako výběr jednoho závodu.

## Impact

- **Backend** (`com.klabis.events`): nový dávkový import endpoint (`POST /api/events/import-batch`) v `OrisEventController`, rozšíření `OrisEventImportPort` / `OrisEventImportService` o dávkovou metodu vracející per-závod výsledek. Znovupoužije existující `importEventFromOris` na jednotlivé závody. HAL+FORMS affordance na `GET /api/events` (`importEventsBatch`).
- **Frontend** (`frontend/src/`): `ImportOrisEventModal` → multi-select s checkboxy a výsledkovým panelem; hook `useOrisEventImport` rozšířen o dávkový submit; napojení v `EventsPage`.
- **Spec**: `openspec/specs/events/spec.md` — ORIS import requirement rozšířen o dávkový import.
- **Bez dopadu** na ORIS klienta (znovu se použije stávající single-event import opakovaně) a na datový model `Event`.
