> **Související GitHub issue:** [#275 Events: atribut ranking (žebříček) a import ceny startovného z ORIS](https://github.com/zabiny/klabis/issues/275)
>
> **Předpoklad pro:** [#274 Úrovně členských příspěvků](https://github.com/zabiny/klabis/issues/274) — pravidla úrovní pracují s kombinací (typ závodu + ranking) a procentem ze základní ceny závodu.

## Why

Pro výpočet spoluúčasti člena na startovném (definované úrovněmi členských příspěvků) musí systém znát:

1. Do jakého **žebříčku/série** závod patří (oblastní, žebříček B, celostátní, ...) — pravidla úrovní rozlišují podle této hodnoty.
2. Jaká je **základní cena startovného** závodu — procentuální pravidla počítají doplatek z této ceny.

Obě hodnoty pochází z ORIS a dnes nejsou v lokálním modelu závodu evidovány. Tento change je předpoklad pro výpočet doplatků v rámci `membership-fees`.

## What Changes

- Závod získává nový atribut **ranking** (žebříček/série) — samostatný atribut nezávislý na typu závodu, hodnoty odpovídají kategoriím v ORIS (např. *Oblastní žebříček*, *Žebříček B*, *Český pohár*, *bez žebříčku*).
- Zdrojem hodnoty rankingu je ORIS pole **`Level`** (`EventDetails.level()`, dto `Level(int id, String shortName, String nameCZ, String nameEN)` — ověřeno, oris-client `0.1.0` ho vystavuje; ORIS má 22 hodnot: MČR, ŽA, ŽB, OŽ, ČP, …). Klabis ukládá `Level.id` jako klíč pro matchování pravidel úrovní **plus denormalizovaný snapshot** `shortName`/`nameCZ` pro zobrazení a lokalizaci (bez lookup tabulky a bez mapovaného enumu — viz `design.md`).
  - **Poznámka:** ORIS `Level` (žebříček/série) je něco jiného než ORIS pole `Ranking`/`RankingKoef` (0/1 flag + koeficient, zda se výsledek počítá do celostátního hodnocení). Klabis pracuje s **`Level`**, ORIS `Ranking`/`RankingKoef` se neimportuje.
- Závod získává atribut **základní cena startovného** (`baseEntryFee`) jako částku s měnou (Money). Měna se synchronizuje z ORIS (`EventDetails.currency()`, typicky CZK).
  - **ORIS nemá jednu cenu na závod** — cena je vždy per-kategorie (`EventClass.fee()`, ověřeno; např. MČR má D21=650 Kč, M16=320 Kč, varianty s 0 Kč). Jako reprezentativní základní cenu Klabis bere **maximum přes všechny kategorie** (`MAX(EventClass.fee)`), což odpovídá ceně hlavní dospělé kategorie a automaticky ignoruje zlevněné/nulové varianty.
- **Navýšená cena** za pozdější přihlášku (`ManualFeeEntryDate2/3`) **není** součástí tohoto changu — pro výpočet doplatků úrovní stačí základní cena; lze přidat později samostatným changem.
- **Synchronizace z ORIS** je rozšířena o stažení rankingu (`Level`), základní ceny startovného (`MAX(EventClass.fee)`) a měny (`currency()`) — při importu i při následné synchronizaci.
- Ranking a cena jsou viditelné pouze na **detailu akce**. Seznam akcí se nemění (ani ranking, ani cena se v seznamu nezobrazují).
- Pokud ORIS nevrací ranking nebo cenu (např. tréninkové akce vytvořené ručně), atribut zůstane prázdný; admin může doplnit ručně.

## Capabilities

### Modified Capabilities
- `events`: závod má nový atribut `ranking` (žebříček/série) a `baseEntryFee` (cena startovného); obě hodnoty se synchronizují z ORIS a jsou editovatelné správcem.

## Impact

- **Doménový model:** rozšíření aggregate root závodu o dvě hodnoty.
- **Persistence:** schema migrace (přidání sloupců).
- **ORIS adapter:** rozšířit mapování ORIS → lokální model o nové hodnoty.
- **API:** rozšíření DTO závodu + form template pro editaci.
- **Frontend:** zobrazení rankingu (chip/badge) a ceny startovného na detailu akce; seznam akcí beze změny.
- **Vyřešené otázky (rozhodnuto před designem):**
  - Reprezentace rankingu → `Level.id` + denormalizovaný snapshot (`shortName`, `nameCZ`), bez lookup tabulky/enumu.
  - Cena → `MAX(EventClass.fee)` přes kategorie, ukládána jako Money (částka + měna z ORIS).
  - Navýšená cena za pozdní přihlášku → mimo rozsah tohoto changu.
  - Zobrazení v seznamu akcí → seznam se nemění; ranking i cena jsou pouze na detailu akce.
