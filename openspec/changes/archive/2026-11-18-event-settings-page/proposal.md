## Why

Menu Administrace dnes obsahuje tři samostatné položky, které spolu souvisí a nastavují akce: Disciplíny, Typy akcí a Šablony kategorií. Menu je kvůli nim dlouhé a související číselníky jsou roztříštěné. Podobně jako u synchronizace je přehlednější jedna stránka se záložkami.

## What Changes

- Nová stránka „Nastavení akcí" s třemi záložkami: Disciplíny, Typy akcí, Šablony kategorií. Každá záložka má vlastní adresu pod `/event-settings` (`/event-settings/disciplines`, `/event-settings/event-types`, `/event-settings/category-presets`).
- V menu Administrace se tři položky nahradí jednou položkou „Nastavení akcí". Zobrazí se uživatelům s oprávněním EVENTS:MANAGE.
- Dosavadní adresy `/event-types`, `/category-presets` a `/disciplines` přesměrují na odpovídající záložku, aby fungovaly záložky a odkazy uživatelů.
- Obsah záložek (seznamy, formuláře, oprávnění) se nemění.
- Backend se nemění; záložky se zobrazí podle toho, které odkazy uživatel v kořenovém API dostane.

## Capabilities

### New Capabilities

- `event-settings`: stránka „Nastavení akcí" se záložkami Disciplíny, Typy akcí a Šablony kategorií, přístup podle oprávnění a zachování záložky v adrese.

### Modified Capabilities

- `application-navigation`: sekce Administrace obsahuje jednu položku „Nastavení akcí" místo tří samostatných položek.
- `disciplines`: číselník disciplín se otevírá ze záložky stránky Nastavení akcí, ne z vlastní položky menu.

Specifikace `event-types` a `category-presets` se neupravují: jejich scénáře (správa číselníku, položka „Typy akcí" chybí v navigaci pro uživatele bez EVENTS:MANAGE) zůstávají platné, jen se obsah otevírá v záložce.

## Impact

- Frontend: `App.tsx` (vnořené routy a přesměrování), nová stránka a layout se záložkami, `Layout.tsx` a `useRootNavigation.ts` (sloučení tří položek do jedné), `EventTypesPage`, `CategoryPresetsPage`, stránka disciplín (dnes `GenericHalPage`), lokalizace v `labels.ts`.
- Testy: unit testy stránky a navigace, E2E scénáře navigace.
- Specifikace: `application-navigation`, `disciplines`, nová `event-settings`.
- Backend a API kontrakt beze změny.
