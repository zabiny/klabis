## Context

Tři group aggregáty (FreeGroup, FamilyGroup, TrainingGroup) sdílí jednu tabulku `user_groups` s type discriminatorem (memory `project_user_groups_persistence.md`, 2026-04-17). Persistence používá Spring Data JDBC s `JdbcAggregateTemplate`, agregáty se mapují přes shared `GroupMemento` (`backend/src/main/java/com/klabis/groups/common/infrastructure/jdbc/GroupMemento.java`) — v něm jsou 3 statické factory methods (`fromFreeGroup`, `fromFamilyGroup`, `fromTrainingGroup`) a 3 reverse methods.

**Bug se projevuje napříč všemi 3 group typy** — uživatel potvrdil HTTP 500 i pro rodinnou skupinu. To znamená, že root cause je v **shared persistence layer**, ne ve FreeGroup-specific cestě.

Symptom (reprodukce 2026-04-29):
- **UI:** dialog „Vytvořit skupinu" → vyplnit název → Odeslat → response status 500, alert „HTTP 500 ()".
- **Network:** `POST /api/groups` (resp. proxy přes nginx → Spring), 500.
- **Backend log (H2 lokální):**
  ```
  DataIntegrityViolationException: PreparedStatementCallback;
  SQL [INSERT INTO "user_groups" ("age_range_max", "age_range_min", "created_at", "created_by",
                                  "id", "modified_at", "modified_by", "name", "type", "version")
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)];
  Check constraint invalid: "chk_user_groups_type: "
  SQL statement: INSERT INTO "user_groups" (...) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) [23514-240]
  with root cause
  org.h2.jdbc.JdbcSQLNonTransientConnectionException: Databáze byla ukončena
  The database has been closed [90098-240]
  ```

Statická analýza:
- `chk_user_groups_type` (V001 migrace) = `type IN ('FREE','TRAINING','FAMILY')`.
- `FreeGroup.TYPE_DISCRIMINATOR = "FREE"`.
- `GroupMemento.fromFreeGroup` nastavuje `memento.type = "FREE"` přes `initCommon`.
- Žádný code path neukazuje na špatnou hodnotu `type`.

Indícia z chybové hlášky `Check constraint invalid: "chk_user_groups_type: "` (s prázdným řetězcem za dvojtečkou) plus root cause `Databáze byla ukončena` naznačují, že H2 nestihl validaci dokončit (zavřená session). To je ale **lokální only**; produkce s PostgreSQL chybu reprodukuje také, takže musí jít o jiný root cause na jiné vrstvě (nebo se chybová hláška jen různě převléká).

## Goals / Non-Goals

**Goals:**
- Deterministicky identifikovat skutečný root cause HTTP 500 v create endpointech pro **všechny 3 typy** skupin (FreeGroup, FamilyGroup, TrainingGroup) — společný symptom napříč 3 typy ukazuje na shared persistence layer.
- Opravit kód tak, aby vytvoření kterékoli skupiny přes UI uspělo (HTTP 201).
- Pokrýt happy path integration testy na PostgreSQL TestContainer (production parity) pro každý ze 3 typů zvlášť.
- Pokrýt hraniční případy: prázdný název (400 validation), velmi dlouhý název (≥ 200 znaků — DB constraint `VARCHAR(200)`).

**Non-Goals:**
- Refactor sjednocené persistence pro tři group aggregáty (`GroupMemento` orchestrace 3 typů). Pokud diagnostika ukáže designový problém v shared mementu, otevřeme samostatný proposal.
- Změna spec pro `user-groups`. Bug je čistá implementační regrese.
- Změna UI dialogu „Vytvořit skupinu" — funguje, jen backend zatím selže.

## Decisions

### Decision 1: Diagnostika před fixem (TDD red phase)

Než napíšeme změnu kódu, nejdřív získáme deterministický stack trace **z PostgreSQL prostředí** (production-parity TestContainer). To eliminuje H2-specific artefakty z chybové hlášky a nasměruje fix na skutečný problém.

Konkrétně:
1. Napsat integration test `GroupCreationIntegrationTest` s `@SpringBootTest`, profile `test` (PostgreSQL TestContainer), který volá REST endpointy pro **každý ze 3 typů** group: `POST /api/groups` (FreeGroup), `POST /api/family-groups`, `POST /api/training-groups` s validním JSON payloadem.
2. Spustit testy → sledovat skutečné stack trace z PostgreSQL JDBC driveru. PostgreSQL chybové hlášky jsou na rozdíl od H2 jednoznačné (`ERROR: new row for relation "user_groups" violates check constraint "chk_user_groups_type"` nebo cokoli jiného).
3. Pokud testy procházejí na PostgreSQL ale failují na H2 → bug je H2-specific a vznikne otázka, jak je možné, že produkce s PostgreSQL také padá (možná environment state).
4. Pokud testy failují i na PostgreSQL → root cause je v aplikační vrstvě (nejspíše shared persistence — `GroupMemento`, `GroupJdbcRepository`, isNew handling).

**Alternative considered:** Pustit produkční backend s `--debug` logem a vyčíst stack trace z deploye — ale není bezpečné měnit produkční logging level kvůli debugging session, a TestContainer dosahuje stejného výsledku.

### Decision 2: Pokud bug je v `GroupMemento` mappingu, ověřit konzistenci `Persistable.isNew()` napříč 3 typy skupin

Memory `project_user_groups_persistence.md` (2026-04-17) zaznamenává, že 3 agregáty (FreeGroup, TrainingGroup, FamilyGroup) sdílejí `GroupMemento` přes type discriminator. Common gotcha shared mementem:
- `Persistable.isNew()` rozhoduje o `INSERT` vs. `UPDATE` v Spring Data JDBC.
- `GroupMemento.isNew = (group.getAuditMetadata() == null)` — depends on whether `AuditMetadata` was already set (existing group) nebo not (new group).
- Pokud creator pipeline někde přidá `AuditMetadata` před `repository.save()` (např. `@CreatedBy` populating before persist), `isNew` bude `false` a JdbcAggregateTemplate udělá UPDATE proti neexistujícímu řádku → optimistic lock failure (jiná chyba), nebo dovede k prapodivné chybě v case Spring Data 3.x dělá pre-validation INSERT pro potvrzení.

Prověřit, kdy přesně se `AuditMetadata` populuje (je to v `KlabisAggregateRoot.create(...)` nebo až v `GroupMemento` po insertu?). Pokud v `create`, isNew == false → INSERT validuje, ale Spring Data může poslat divnou kombinaci.

**Action:** Krok 1 v tasks.md je prozkoumat tuto hypotézu spuštěním testu a ladit stack trace.

### Decision 3: Test uchovat i po opravě jako regression guard

Integration test `GroupCreationIntegrationTest` zůstává v repo i po opravě jako:
- Happy path REST + persistence testy pro create endpointy všech 3 group typů.
- Sanity check, že sjednocená group persistence funguje pro každý typ.
- Coverage pro hraniční případy (prázdný název, příliš dlouhý název, duplicitní název pro stejného uživatele).

## Risks / Trade-offs

- **[Risk] Root cause se nedokáže reprodukovat v testu** → Mitigation: pokud `FreeGroupCreationIntegrationTest` projde proti PostgreSQL TestContaineru, znamená to, že bug je environment-specific (production database state, sekvence migrací, cached connection pool). Druhý krok: získat backend log z `api.klabis.otakar.io` (`tail` na log nebo dotaz operátora) pro přesný stack trace.
- **[Risk] Bug je v shared mementu pro 3 group typy a oprava rozbije Training/FamilyGroup** → Mitigation: TestContainer test pokrývá všechny 3 group create flows (rozšířit oblast testů, ne jen FreeGroup). Smoke test po oprávě: `Tréninkové skupiny` a `Rodinné skupiny` přes UI.
- **[Trade-off] PostgreSQL TestContainer je pomalejší než H2** → Mitigation: stejný engine už používá `test` profile s `testcontainers`; přidání jednoho dalšího `@SpringBootTest` testu nemá významný dopad na build time.

## Migration Plan

1. **Diagnostika:** Napsat `FreeGroupCreationIntegrationTest`, spustit, získat stack trace.
2. **Fix:** Implementovat opravu na základě skutečného root cause; iterovat dokud test neprojde.
3. **Verifikace:** Lokálně ověřit přes UI (`runLocalEnvironment.sh` → /groups → Vytvořit skupinu).
4. **Deploy:** Push do produkce, ověřit přes browser test proti `https://api.klabis.otakar.io` (login → /groups → Vytvořit skupinu „X" → očekávat HTTP 201, řádek se objeví v seznamu).
5. **Rollback:** revert commitu; UI funkce zůstane rozbitá, ale ostatní funkčnost není dotčena.

## Open Questions

- **Existuje rozdíl v migrations mezi production DB a TestContainer/H2?** Ověřit `flyway_schema_history` v produkci (potřeba operator access). Pokud byly aplikované migrace v jiném pořadí, může to být příčinou state issue.
- **Kdy přesně se v create flow populuje `AuditMetadata`?** Před `save()` nebo až po? Stack trace poskytne odpověď.
