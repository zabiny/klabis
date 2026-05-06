## Why

Vytvoření skupiny — **volné (FreeGroup) i rodinné (FamilyGroup), pravděpodobně i tréninkové (TrainingGroup)** — přes UI selhává s HTTP 500. Reprodukováno proti `https://api.klabis.otakar.io` 2026-04-29 pro volnou skupinu (klik na „Vytvořit skupinu" v `/groups`, vyplnění názvu, Odeslat → dialog „HTTP 500 ()"); pro rodinnou skupinu uživatel reportoval stejný symptom. Tím je celá funkcionalita správy skupin v UI nepoužitelná — uživatel nemůže založit žádnou skupinu, navázat na ni invitation flow, ani spravovat členství.

Bug je platný i v lokálním prostředí (H2): backend log uvádí `DataIntegrityViolationException` s textem `Check constraint invalid: "chk_user_groups_type: "` na INSERT do tabulky `user_groups`. Protichůdný signál — `chk_user_groups_type` definuje `type IN ('FREE','TRAINING','FAMILY')` a `FreeGroup.TYPE_DISCRIMINATOR = "FREE"` se s tím shoduje — naznačuje, že skutečná příčina je jinde a chybová hláška je matoucí (in-flight H2 shutdown, sekundární constraint, kolize s jiným řádkem, problém v shared mappingu pro 3 typy skupin). Společný symptom napříč více typy skupin **silně směřuje na sdílený `GroupMemento` / `GroupJdbcRepository` / unified persistence layer** — viz memory `project_user_groups_persistence.md` (sjednocení 3 agregátů do jedné tabulky `user_groups` s type discriminator).

## What Changes

- **Diagnostika je první krok:** task queue začíná reprodukcí v controlovaném prostředí (PostgreSQL TestContainer pro production parity) s plným server-side stack tracem a zaznamenáním celého insert payloadu. Cíl je deterministicky identifikovat root cause sdílený přes všechny 3 typy skupin.
- **Implementace fixu** se odvodí od skutečného root cause; možné kandidáty (do prozkoumání):
  1. JdbcAggregateTemplate save vs. `Persistable.isNew()` inconsistency po sjednocení 3 agregátů přes shared `GroupMemento` — pokud `AuditMetadata` se inicializuje příliš brzy, `isNew()` vrátí `false` a Spring Data udělá UPDATE místo INSERT.
  2. Mapped collection cascade — `MappedCollection`y `owners`, `members`, `invitations` v shared mementu mohou kolidovat při INSERT pro typy, které některou kolekci nepoužívají (např. FamilyGroup nemá invitations).
  3. NULL hodnota v některém non-null sloupci (např. `name`, `created_by` po nějaké transformaci).
  4. Production migration state inconsistency vs. local H2.
- **Acceptance:** úspěšné založení **FreeGroup, FamilyGroup i TrainingGroup** přes UI (production-deployed) vrátí HTTP 201, skupina se objeví v seznamu, vlastník/rodič/trenér je nastaven na přihlášeného uživatele.
- **Test:** přidat integration test `GroupCreationIntegrationTest` (PostgreSQL TestContainer) pokrývající create flow pro všechny tři group typy přes REST API. Testy musí selhat na současném `main` brachu před opravou (red), projít po opravě (green).

## Capabilities

### New Capabilities

Žádné. Spec `user-groups` již popisuje vznik skupin všech tří typů.

### Modified Capabilities

Žádné. Bug je čistá implementační regrese — spec-level chování zůstává stejné, jen aktuálně nefunguje. Pokud diagnostika odhalí spec-level problém (např. chybějící validaci), vrátíme se a delta spec doplníme.

## Impact

- **Backend kód:**
  - `com.klabis.groups.common.infrastructure.jdbc.GroupMemento` (shared memento + 3 mapping methods).
  - `com.klabis.groups.common.infrastructure.jdbc.GroupJdbcRepository` (shared Spring Data repository).
  - 3 adaptéry: `FreeGroupRepositoryAdapter`, `FamilyGroupRepositoryAdapter`, `TrainingGroupRepositoryAdapter`.
  - 3 application services: `FreeGroupManagementService`, `FamilyGroupManagementService`, `TrainingGroupManagementService`.
  - Případně migration `V001__initial_schema.sql` (pokud constraint).
- **Production impact:** P1 — všechny 3 typy skupin jsou v UI nepoužitelné. Po opravě deploy do produkce.
- **Závislosti:** žádné; commit nezasahuje do API kontraktu.
