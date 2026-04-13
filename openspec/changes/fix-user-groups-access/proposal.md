## Why

Členové skupin (tréninková, rodinná, volná) nemohou zobrazit detail skupiny, jejíž jsou členy — dostávají HTTP 403. Zároveň uživatelé bez role MEMBERS:MANAGE vidí v navigaci položku "Administrace → Rodinné skupiny", ke které nemají přístup. Obě chyby způsobují matoucí UX a nesoulad s definovaným chováním ve specifikaci.

## What Changes

- Členové skupiny (bez ohledu na typ skupiny) mohou zobrazit detail skupiny, jejíž jsou členy (čtení)
- Navigační položka "Administrace → Rodinné skupiny" je podmíněna rolí MEMBERS:MANAGE

## Capabilities

### New Capabilities

*(žádné — jde o opravy stávajícího chování)*

### Modified Capabilities

- `user-groups`: Přidání požadavku, že každý člen skupiny má právo číst detail své skupiny (platí pro všechny typy: tréninková, rodinná, volná). Přidání požadavku na podmíněné zobrazení navigační položky rodinných skupin dle role MEMBERS:MANAGE.

## Impact

- Backend: autorizační pravidla pro čtení detailu skupiny (všechny tři typy)
- Frontend: podmíněné zobrazení navigační položky "Rodinné skupiny" v sekci Administrace
