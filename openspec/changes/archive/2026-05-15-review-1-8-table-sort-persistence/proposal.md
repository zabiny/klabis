## Why

Z review aplikace 2026-04-29: uživatelé chtějí, aby si aplikace pamatovala jejich poslední řazení tabulek napříč sessiony. Aktuálně tabulky (akce, členové, skupiny, kalendář) řadí podle defaultního sloupce; pokud si uživatel přepne sort a vrátí se další den, sort je zase v defaultu — musí ho znovu nastavit.

Aktuální chování: sort + filtry jsou perzistovány v URL (spec scénář *Filter state is preserved in the page URL* v `events`). To dává sdílení odkazu se stejnými filtry, ale neukládá per-user preference napříč zařízeními a sessiony.

Tato změna zavádí per-user persistenci řazení tabulek. Cílem je „při návratu na stránku tabulky se mi rovnou zobrazí to řazení, co používám".

## What Changes

### Per-user persistence řazení tabulek

- **Nový cross-cutting requirement** v `non-functional-requirements`:
  - Tabulky aplikace si pamatují posledně použité řazení **per-user**, **per-tabulka**.
  - Když uživatel klikne na sort header, tabulka přeřadí a **uloží sort preference do localStorage** v prohlížeči (key per-tabulka, např. `klabis.table.events.sort`).
  - Když se uživatel vrátí na stránku tabulky (po reloadu, nový tab, druhý den), tabulka se inicializuje s uloženým sortem.
  - Pokud URL obsahuje explicitní sort param (např. sdílený link), URL **vyhrává** nad localStorage (zachová sdílení odkazu jako dosud).
  - Reset preference: tlačítko / akce „Resetovat řazení" v tabulce vyprázdní localStorage entry → návrat k defaultu.

### Scope

- Týká se tabulek v aplikaci kde má sort smysl: `events`, `members`, `groups` (free + family + training), `calendar-items`.
- **Pouze sort**, ne filtry, ne page size, ne column visibility (tyto rozšíření jsou samostatný proposal, viz Open Questions).

### Storage volba

- **localStorage** (per browser, per device). Jednodušší než server-side per-user preferences.
- Trade-off: uživatel na druhém zařízení nezíská stejný sort. To je akceptovatelné — sort je low-stakes preference. Pokud se ukáže jako frustrující, lze migrovat na server-side preference v navazujícím proposalu.

## Capabilities

### New Capabilities

Žádné.

### Modified Capabilities

- `non-functional-requirements`: nový cross-cutting requirement „Tabulky persistují řazení per uživatele".

## Impact

- **Backend kód:** žádné změny.
- **Frontend kód:**
  - Rozšířit `KlabisTable` (nebo wrapper hook `useTableSort`) o:
    - Načítání initial sort state z localStorage při mount.
    - Zápis sort change do localStorage.
    - Logiku „URL > localStorage > default".
  - Per-tabulka identifier (key v localStorage) — typicky pevný string (např. `events`, `members`).
- **Backward compat:** existující URL-based sort se zachová; změna je čistě frontendové „sticky" rozšíření.

## Open Questions

- **Persistovat i další table state (filter, page size, columns)?** Podle review jen sort. Pro filter platí výjimka — filtry mají dnes URL-based persistence (sdílení odkazu). Page size je menší preference, ale relevantní; můžeme rozšířit nebo nechat samostatný proposal. Default: pouze sort.
- **Reset action UX:** kde je "Resetovat řazení"? V kontextovém menu sortable headeru? Default: ano.
- **localStorage quota / corruption:** ošetřit JSON parse error gracefully (pokud uživatel ručně edituje storage).
