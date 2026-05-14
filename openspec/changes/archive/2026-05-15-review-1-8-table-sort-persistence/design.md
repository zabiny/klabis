## Context

Cross-cutting frontend change. Žádný backend touch. Existing behaviour: sort + filter state je v URL (TanStack Router / React Router query params). Sdílení odkazu funguje out of the box.

## Goals / Non-Goals

**Goals:**
- Sort preference per-user (per-browser via localStorage), per-tabulka.
- URL > localStorage > default precedence.
- Reset action.

**Non-Goals:**
- Server-side preferences synced across devices.
- Filter / page-size / column-visibility persistence (samostatný proposal).
- Migration existing URL-based sort to localStorage (URL je stále zdroj pravdy pokud je v něm sort param).

## Decisions

### Decision 1: Hook `useTableSort(tableId, defaultSort)`

Centralizovaný hook, který:
1. Při mount: pokud URL `?sort=...` je přítomný, použije ho jako initial state. Jinak pokud localStorage `klabis.table.{tableId}.sort` má hodnotu, použije ji. Jinak `defaultSort`.
2. Při změně sort (uživatel klikne header): updatuje URL (současné chování) **a** zapíše do localStorage.
3. Při reset: vyprázdní localStorage entry, vrátí URL na default sort, refresh state.

Hook integruje s existing TanStack Query / React Router infrastrukturou — používá stejné mechanismy, jen přidá localStorage layer.

### Decision 2: localStorage key naming convention

Single root namespace `klabis.table.<tableId>.sort` se string value `"<column>,<direction>"` (např. `"name,asc"`). Stejný formát jako URL param — žádný JSON parsing.

`tableId` je pevný string per use site:
- `events`
- `members`
- `groups.free`
- `groups.family`
- `groups.training`
- `calendar-items`
- `event-types` (po deployi 1.5)
- `category-presets`
- `event.{eventId}.registrations` — možná, ale per-event je problém (storage roste neomezeně). Pro registration list spíš nepersistovat zatím (samostatný design).

### Decision 3: URL > localStorage precedence

Když uživatel otevře sdílený odkaz `?sort=date,desc`, ten override jeho lokální preferenci. Důvod: explicit user intent (klikl na sdílený link) vyhrává nad implicit preferencí.

Jakmile uživatel klikne na sort header, URL se updatuje **a** localStorage se updatuje na novou hodnotu. Tj. interakce přepíše obě vrstvy.

### Decision 4: Reset action

V column header context menu (klik pravým tlačítkem nebo dropdown) přidat položku „Resetovat řazení". Klik:
1. Vyprázdní localStorage entry.
2. Updatuje URL na default sort.
3. Re-fetch data.

Alternativně, méně viditelné: klik na již sortovaný header třikrát = asc → desc → reset (default). Toto je standardní pattern v TanStack Table; pokud `KlabisTable` ho má, využít. Default: ponechat current toggle, přidat explicit "Resetovat" v menu.

## Risks / Trade-offs

- **[Risk] localStorage corruption / quota** → Mitigation: try/catch při parsing; fallback na default sort při error; quota neexistuje v praxi pro pár key-value paírů.
- **[Risk] Uživatel přepne sort, otevře nový tab — nový tab dostane uloženou preferenci, ale URL nemá sort. Konfuze?** → Mitigation: nový tab dostane URL bez sort, hook detekuje, použije localStorage. Pokud uživatel pak klikne header, URL se update. Konzistentní behavior.
- **[Trade-off] Per-browser, ne per-user-account** — uživatel na pracovním PC vs. domácím PC má jiné preference. Akceptovatelné pro low-stakes preference. Pokud se ukáže problém, server-side migration.

## Migration Plan

1. Implementace `useTableSort` hooku.
2. Refactor existing tables (events, members, groups, calendar) na nový hook.
3. Přidat reset action UI.
4. Frontend tests + smoke test po deployi.
