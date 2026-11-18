# Tasks

## 1. Navigace a routy

- [ ] 1.1 Test `useRootNavigation`: ze tří linků vznikne jedna položka „Nastavení akcí" v sekci `admin`, bez linků žádná; implementovat a ověřit spuštěním testu
- [ ] 1.2 Přidat vnořené routy `/event-settings/*` a přesměrování `/disciplines`, `/event-types`, `/category-presets` v `App.tsx`; ověřit testem routování

## 2. Stránka se záložkami

- [ ] 2.1 Test layoutu `EventSettingsPage`: záložky podle dostupných linků, aktivní záložka z URL, přesměrování z nedostupné záložky, `ForbiddenPage` bez záložek
- [ ] 2.2 Implementovat layout se záložkami (`TabButton` + `NavLink`, `<Outlet/>`) a lokalizační texty v `labels.ts`; ověřit testem z 2.1
- [ ] 2.3 Dedikovaná stránka disciplín přes `HalPageGate` zachovávající dnešní vzhled; ověřit testem a ručně na `http://localhost:3000`

## 3. Úklid a ověření

- [ ] 3.1 Odstranit nepoužívané ikony a labely tří původních položek (`Layout.tsx`, `labels.ts`) a ověřit `npm run build`
- [ ] 3.2 Aktualizovat E2E scénáře navigace (položka „Nastavení akcí", záložky, staré adresy) a spustit `npm run test:e2e`
- [ ] 3.3 Ručně ověřit na `:3000` jako ZBM9000 (vidí záložky) a ZBM9500 (nevidí položku, přímé otevření je odepřeno); code review před commitem

## Workflow follow-up

- Po schválení změnu archivovat (`openspec archive`) a ověřit, že hlavní specifikace `application-navigation`, `disciplines` a nová `event-settings` odpovídají.
