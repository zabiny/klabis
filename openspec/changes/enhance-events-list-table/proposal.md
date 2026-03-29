## Why

Tabulka závodů zobrazuje jen základní přehled (datum, název, místo, pořadatel, status). Členové klubu potřebují vidět důležité informace přímo v seznamu — odkaz na web závodu, datum uzávěrky přihlášek, koordinátora a možnost přihlášení — bez nutnosti proklikávat se na detail každého závodu.

## What Changes

- Přidání nového doménového pole `registrationDeadline` (volitelné datum) na Event — pokud je nastavené a uplynulo, přihlášky se automaticky uzavřou (i když je závod ACTIVE a datum v budoucnosti)
- Rozšíření list API o: `websiteUrl`, `registrationDeadline`, `_links.coordinator`, affordance pro registraci/odregistraci na self linku
- Sloupec `status` v tabulce viditelný pouze pro uživatele s `EVENTS:MANAGE` oprávněním (field-level security přes `@HasAuthority`)
- Nové sloupce v tabulce závodů: externí URL (klikatelná ikona), datum uzávěrky přihlášek, jméno koordinátora (proklik na detail člena), akce přihlášení/odhlášení
- Úprava formulářů pro vytvoření a editaci události — přidání pole `registrationDeadline`
- Zobrazení `registrationDeadline` na detailu události (read-only řádek + editovatelné pole v edit módu)
- Mapování `registrationDeadline` z ORIS importu (pole `EntryDate1` → `registrationDeadline`)

## Capabilities

### New Capabilities

_(žádné nové capability — rozšiřujeme existující)_

### Modified Capabilities

- `events`: Přidání pole `registrationDeadline` s doménovou logikou uzavření přihlášek. Rozšíření list API response o `websiteUrl`, `registrationDeadline`, `_links.coordinator`, affordance pro registraci. Field-level security pro `status` pole.
- `event-registrations`: Úprava podmínky otevřených přihlášek — kromě stávající logiky (ACTIVE + budoucí datum) zohlednit i `registrationDeadline`.

## Impact

- **Backend domain**: `Event` aggregate — nové pole `registrationDeadline`, úprava `areRegistrationsOpen()` logiky
- **ORIS integration**: `EventManagementServiceImpl.importEventFromOris()` — mapování `EntryDate1` na `registrationDeadline`
- **Backend API**: `EventSummaryDto` — přidání polí a linků, `EventController` — affordance na list endpointu, `@HasAuthority` na `status` pole
- **Backend persistence**: DB migrace pro sloupec `registration_deadline`
- **Frontend**: `EventsPage.tsx` — nové sloupce tabulky, zobrazení koordinátora z HATEOAS linku, inline akce registrace; `EventDetailPage.tsx` — zobrazení registrationDeadline
- **Specifikace**: Delta spec pro `events` a `event-registrations`
