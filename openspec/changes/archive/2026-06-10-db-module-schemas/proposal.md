## Why

Všechny doménové tabulky jsou aktuálně v jediném `public` schématu. S rostoucím počtem tabulek (30+) je stále obtížnější poznat, který modul tabulku vlastní, bez čtení kódu nebo komentářů v migračním souboru.

## What Changes

- V SQL migraci (V001) přidat `CREATE SCHEMA` pro každý doménový modul a přesunout tabulky do příslušného schématu
- Upravit cross-schema FK syntaxi na `schema.table` formát
- V 26 Java souborech přidat atribut `schema` do `@Table` anotací Spring Data JDBC
- Aktualizovat test SQL soubory (~7 souborů) o schema prefix

Infrastrukturní tabulky Spring Authorization Serveru (V002) a Spring Modulith (V003) zůstávají v `public` — jsou spravovány frameworky a nelze je jednoduše přesunout.

## No Behavior Change Justification

Struktura DB schémat je čistě infrastrukturní detail, neviditelný pro uživatele ani API konzumenty. API odpovědi, business pravidla ani autorizace se nemění.

**Specs reviewed:**
- `openspec/specs/members/spec.md` — neovlivněno, API zůstává beze změny
- `openspec/specs/events/spec.md` — neovlivněno
- `openspec/specs/calendar-items/spec.md` — neovlivněno
- `openspec/specs/user-groups/spec.md` — neovlivněno
- `openspec/specs/member-accounts/spec.md` — neovlivněno
- `openspec/specs/membership-fees/spec.md` — neovlivněno
- `openspec/specs/users/spec.md` — neovlivněno

**Why no spec update is needed:** Přesun tabulek do pojmenovaných schémat mění pouze interní organizaci databáze. Žádná funkční logika, API endpoint ani business pravidlo se nemění.

## Impact

- `backend/src/main/resources/db/migration/V001__initial_schema.sql` — hlavní změna
- 26 Java Memento tříd ve všech doménových modulech (`@Table` anotace)
- Test SQL soubory v `backend/src/test/resources/`
- H2 a PostgreSQL — obě databáze schémata podporují; H2 `MODE=PostgreSQL` je kompatibilní
