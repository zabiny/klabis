## Context

Systém aktuálně definuje tři separátní write autority pro členy: `MEMBERS:CREATE`, `MEMBERS:UPDATE`, `MEMBERS:DELETE`. Tyto tři autority jsou vždy přiřazovány a odebírány dohromady — neexistuje reálný use case, kdy by uživatel potřeboval pouze jednu z nich bez ostatních. Výsledkem je zbytečná složitost v správci oprávnění (3 checkboxy místo 1) a v kódu (3 `@HasAuthority` varianty místo 1).

## Goals / Non-Goals

**Goals:**
- Sloučit `MEMBERS:CREATE`, `MEMBERS:UPDATE`, `MEMBERS:DELETE` do jedné autority `MEMBERS:MANAGE`
- Migrovat stávající uložená oprávnění uživatelů
- Zachovat `MEMBERS:READ` a `MEMBERS:PERMISSIONS` beze změny
- Zjednodušit frontend PermissionsDialog

**Non-Goals:**
- Změna chování autorizace (kdo má přístup k čemu, se nezmění — jen konsolidace)
- Změna `MEMBERS:READ` nebo `MEMBERS:PERMISSIONS`
- Přidání nových operací nad členy

## Decisions

### Název nové autority: MEMBERS:MANAGE

`MEMBERS:MANAGE` je konzistentní s existující `EVENTS:MANAGE` a `CALENDAR:MANAGE`. Alternativy jako `MEMBERS:WRITE` nebo `MEMBERS:ADMIN` jsou méně konzistentní s ostatními autoritami v systému.

### Databázová migrace: UPDATE SQL v Flyway

Existující oprávnění uložená v tabulce budou migrována SQL skriptem. Přístup UPDATE je bezpečný — neexistují produkční data (aplikace je ve vývoji na H2), ale migrace zajistí správnost i při budoucím přechodu na PostgreSQL.

Migrace provede:
1. Přejmenování hodnot `MEMBERS:CREATE`, `MEMBERS:UPDATE`, `MEMBERS:DELETE` → `MEMBERS:MANAGE` v tabulce oprávnění
2. Deduplikaci (pokud měl uživatel kombinaci těchto tří, výsledkem je jeden `MEMBERS:MANAGE`)

### Admin bootstrap

Admin uživatel dostane `MEMBERS:MANAGE` místo `MEMBERS:CREATE`. Tato změna se projeví automaticky po restartu (H2 reset).

## Risks / Trade-offs

- **[Risk] Breaking change v JWT claims** → Existující vydané tokeny obsahují staré authority hodnoty. Mitigation: Aplikace je ve vývoji, žádné live session nebudou narušeny. H2 reset při restartu zajistí čisté tokeny.
- **[Risk] Zapomenuté reference** → Testy a kód mohou obsahovat hardcoded stringy místo enum konstant. Mitigation: Grep prohledá celý codebase před implementací.

## Migration Plan

1. Aktualizovat `Authority` enum — odebrat 3 konstanty, přidat `MEMBERS_MANAGE`
2. Aktualizovat Flyway migraci V001 — přejmenovat hodnoty v tabulce user_permissions
3. Aktualizovat backend kód (controllers, bootstrap, testy)
4. Aktualizovat frontend (PERMISSION_LABELS, PermissionsDialog)
5. Spustit testy

Rollback: Revert commitů, H2 reset smaže migrovaná data (v dev prostředí).
