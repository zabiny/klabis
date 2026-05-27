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
- Zdrojem hodnoty rankingu je ORIS pole **`Level`** (`EventDetails.level`, dto `Level(int id, String shortName, String nameCZ, String nameEN)`). Konkrétní reprezentace na straně Klabis — zda využít `Level.id`, mapovaný enum, nebo samostatnou entitu — se rozhodne v `design.md` (Level.id sám o sobě pravděpodobně nestačí, např. pro zobrazení/lokalizaci).
- Závod získává atribut **základní cena startovného** (`baseEntryFee`) v Kč.
- Volitelně i informace o **navýšené ceně** za pozdější přihlášku (pokud ORIS poskytuje) — k pozdějšímu rozhodnutí v designu.
- **Synchronizace z ORIS** je rozšířena o stažení rankingu a ceny startovného (při importu i při následné synchronizaci).
- Ranking a cena jsou viditelné v UI — alespoň na detailu akce; ranking případně i v seznamu akcí jako filter/sort.
- Pokud ORIS nevrací ranking nebo cenu (např. tréninkové akce vytvořené ručně), atribut zůstane prázdný; admin může doplnit ručně.

## Capabilities

### Modified Capabilities
- `events`: závod má nový atribut `ranking` (žebříček/série) a `baseEntryFee` (cena startovného); obě hodnoty se synchronizují z ORIS a jsou editovatelné správcem.

## Impact

- **Doménový model:** rozšíření aggregate root závodu o dvě hodnoty.
- **Persistence:** schema migrace (přidání sloupců).
- **ORIS adapter:** rozšířit mapování ORIS → lokální model o nové hodnoty.
- **API:** rozšíření DTO závodu + form template pro editaci.
- **Frontend:** zobrazení rankingu (chip/badge), zobrazení ceny startovného na detailu akce.
- **Otevřené otázky pro design fázi:**
  - Konkrétní reprezentace rankingu na straně Klabis (zdrojem je ORIS `Level`, ale Level.id pravděpodobně nestačí — uvážit ukládání celého snapshotu, mapovaný enum, nebo lookup tabulku)
  - Měna / formát ceny (CZK only, nebo víceměnové?)
  - Zda do tohoto changu zahrnout i navýšenou cenu za pozdní přihlášku
