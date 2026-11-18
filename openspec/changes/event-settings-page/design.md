# Design

## Context

Menu se skládá z linků kořenového API (`useRootNavigation`). Tři stávající položky odpovídají linkům `disciplines`, `event-types` a `category-presets`, každý gated na backendu přes `klabisLinkTo`. `EventTypesPage` a `CategoryPresetsPage` mají vlastní routy, disciplíny nemají dedikovanou stránku a renderuje je catch-all `GenericHalPage`. `HalRouteProvider` načítá HAL zdroj podle aktuální cesty, proto je každá záložka vlastní zdroj. Stránka synchronizace používá `?tab=`, ale její záložky sdílejí jeden zdroj. Důvody změny: viz proposal.md.

## Goals / Non-Goals

**Goals:**
- Jedna položka menu a jedna stránka se záložkami, každá záložka s vlastní adresou.
- Zachovat stávající obsah záložek i oprávnění beze změny backendu.

**Non-Goals:**
- Změna obsahu číselníků, API nebo oprávnění.
- Řešení administrace na mobilu (zůstává dokumentovaná mezera v `application-navigation`).

## Decisions

1. **Vnořené routy** `/event-settings/disciplines`, `/event-settings/event-types`, `/event-settings/category-presets` pod společným layoutem se záložkami (`<Outlet/>`). Důvod: každá záložka má vlastní HAL zdroj, který `HalRouteProvider` bere z cesty. Alternativa `?tab=` odmítnuta: zdroj by se musel předávat přes `routeLink` a přepisovat tři stránky.
2. **Záložky z kořenových linků.** Layout čte linky `disciplines`, `event-types`, `category-presets` z `useRootNavigation`; záložka se zobrazí, jen když link existuje. Backend se nemění a autorizace zůstává v jednom místě (backend).
3. **Jedna položka menu** se v `useRootNavigation` odvozuje: pokud existuje aspoň jeden ze tří linků, vznikne položka „Nastavení akcí" v sekci `admin` s cílem na první dostupnou záložku. Tři původní položky se z menu odstraní. Alternativa „nový root link `eventSettings` z backendu" odmítnuta: přidala by endpoint bez vlastního obsahu.
4. **Přesměrování starých adres** (`/event-types`, `/category-presets`, `/disciplines`) na odpovídající záložku přes `<Navigate replace>`.
5. **Disciplíny dostanou dedikovanou stránku** (tabulka přes `HalPageGate`), protože se renderují uvnitř layoutu; dnešní vzhled z `GenericHalPage` se zachová.
6. **Záložky použijí existující `TabButton`** z `components/UI`, přepínání přes `NavLink` (aktivní záložka z URL).

## Risks / Trade-offs

- [Rozcházení stavu „záložka v URL, bez oprávnění"] → při přímém otevření nedostupné záložky layout přesměruje na první dostupnou, bez dostupných záložek zobrazí `ForbiddenPage` (už existuje).
- [Přepisování `useRootNavigation` může rozbít pořadí položek] → pořadí zachovat podle prvního z linků; pokrýt testem.
- [Staré záložky a odkazy v e-mailech] → přesměrování z bodu 4.
