# Doklady a licence — rozhodčí licence a stupně licencí

Implementace `RefereeLicense` jako nové domain entity a přidání `TrainerLevel` enumu do `TrainerLicense`.
Sekce "Doklady a licence" v member detail stránce pak zobrazí stupeň trenérské licence (T1/T2/T3)
a novou záložku pro rozhodčí licenci (R1/R2/R3).

`MedicalCourse` žádné backend změny nevyžaduje — frontend zobrazí "Ano/Ne" badge na základě
`completionDate != null`.

---

## 1. Domain — TrainerLevel enum

- [x] 1.1 Vytvořit `TrainerLevel` enum v `com.klabis.members.domain` s hodnotami `T1`, `T2`, `T3`
  - Soubor: `backend/src/main/java/com/klabis/members/domain/TrainerLevel.java`
- [x] 1.2 Přidat field `level` (typ `TrainerLevel`, required) do `TrainerLicense` value objectu
  - Odstranit field `licenseNumber` — nahrazen enumerovaným stupněm
  - Aktualizovat validaci v compact constructoru (level nesmí být null, `ExpiringDocument` delegace se zruší nebo přizpůsobí)
  - Aktualizovat factory metodu `of(TrainerLevel level, LocalDate validityDate)`
  - Soubor: `backend/src/main/java/com/klabis/members/domain/TrainerLicense.java`
- [x] 1.3 Napsat unit testy pro `TrainerLicense` s novým `level` fieldem
  - Soubor: `backend/src/test/java/com/klabis/members/domain/TrainerLicenseTest.java`

## 2. Domain — RefereeLicense value object

- [x] 2.1 Vytvořit `RefereeLevel` enum v `com.klabis.members.domain` s hodnotami `R1`, `R2`, `R3`
  - Soubor: `backend/src/main/java/com/klabis/members/domain/RefereeLevel.java`
- [x] 2.2 Vytvořit `RefereeLicense` value object analogicky ke `TrainerLicense`
  - Fields: `level` (RefereeLevel, required), `validityDate` (LocalDate, required)
  - Validace: level nesmí být null, validityDate nesmí být null
  - Factory metoda: `of(RefereeLevel level, LocalDate validityDate)`
  - Soubor: `backend/src/main/java/com/klabis/members/domain/RefereeLicense.java`
- [x] 2.3 Napsat unit testy pro `RefereeLicense`
  - Soubor: `backend/src/test/java/com/klabis/members/domain/RefereeLicenseTest.java`

## 3. Domain — Member aggregate

- [x] 3.1 Přidat field `refereeLicense` (typ `RefereeLicense`, nullable) do `Member` aggregate
  - Soubor: `backend/src/main/java/com/klabis/members/domain/Member.java`
- [x] 3.2 Přidat `refereeLicense` do `Member.SelfUpdate` command recordu (admin-only pole — viz poznámka)
- [x] 3.3 Přidat `refereeLicense` do `Member.UpdateMemberByAdmin` command recordu
- [x] 3.4 Přidat `refereeLicense` zpracování do `handle(SelfUpdate)` a `handle(UpdateMemberByAdmin)` metod
- [x] 3.5 Přidat `getRefereeLicense()` getter
- [x] 3.6 Aktualizovat `register()` factory metodu a `reconstruct()` factory metodu (přidat `refereeLicense` parametr)

> Poznámka k SelfUpdate: `refereeLicense` je admin-only pole (stejně jako `trainerLicense`).
> `SelfUpdate` command ho obsahuje technicky, ale `UpdateMemberRequestMapper.toSelfUpdateCommand()`
> ho naplní jako `null` — tedy self-update ho nemůže měnit.

## 4. Persistence — MemberMemento a DB schema

- [x] 4.1 Přidat sloupce do migration skriptu `V001__initial_schema.sql` v tabulce `members`:
  - `trainer_license_level VARCHAR(10)` — nový sloupec (nahrazuje `trainer_license_number`)
  - `referee_license_level VARCHAR(10)` — nový sloupec
  - `referee_license_validity_date DATE` — nový sloupec
  - Smazat sloupec `trainer_license_number` (data budou ztracena — dev prostředí, H2 se restartuje)
  - Soubor: `backend/src/main/resources/db/migration/V001__initial_schema.sql`
- [x] 4.2 Aktualizovat `MemberMemento`:
  - Přejmenovat `trainerLicenseNumber` field → `trainerLicenseLevel` (typ `TrainerLevel`)
  - Přidat `refereeLicenseLevel` field (typ `RefereeLevel`)
  - Přidat `refereeLicenseValidityDate` field (typ `LocalDate`)
  - Aktualizovat `copyTrainerLicense()` — ukládat `level` místo `licenseNumber`
  - Přidat `copyRefereeLicense()` metodu
  - Aktualizovat `toMember()` — rekonstruovat `TrainerLicense` z `level` + `validityDate`; rekonstruovat `RefereeLicense`
  - Aktualizovat `Member.reconstruct()` volání (přidat `refereeLicense` argument)
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/jdbc/MemberMemento.java`
- [x] 4.3 Napsat nebo aktualizovat repository integration test
  - Ověřit round-trip uložení a načtení `TrainerLicense` s `level` fieldem
  - Ověřit round-trip uložení a načtení `RefereeLicense`
  - Soubor: `backend/src/test/java/com/klabis/members/infrastructure/jdbc/MemberRepositoryAdapterTest.java` (nebo analogický)

## 5. REST API — DTOs a mapper

- [x] 5.1 Aktualizovat `TrainerLicenseDto`:
  - Přejmenovat field `licenseNumber` → `level` (typ `TrainerLevel`)
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/restapi/TrainerLicenseDto.java`
- [x] 5.2 Vytvořit `RefereeLicenseDto`:
  - Fields: `level` (RefereeLevel), `validityDate` (LocalDate)
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/restapi/RefereeLicenseDto.java`
- [x] 5.3 Přidat `refereeLicense` field do `MemberDetailsResponse`
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/restapi/MemberDetailsResponse.java`
- [x] 5.4 Přidat `refereeLicense` field (admin-only) do `UpdateMemberRequest`
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/restapi/UpdateMemberRequest.java`
- [x] 5.5 Aktualizovat `MemberMapper`:
  - Aktualizovat `trainerLicenseToDto()` — mapovat `level` místo `licenseNumber`
  - Přidat `refereeLicenseToDto()` metodu
  - Přidat `refereeLicense` do `toDetailsResponseInternal()` mapping
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/restapi/MemberMapper.java`
- [x] 5.6 Aktualizovat `UpdateMemberRequestMapper`:
  - Aktualizovat `toTrainerLicense()` — mapovat z `TrainerLicenseDto.level`
  - Přidat `toRefereeLicense()` helper metodu
  - Přidat `refereeLicense` do `toAdminCommand()` volání
  - Soubor: `backend/src/main/java/com/klabis/members/infrastructure/restapi/UpdateMemberRequestMapper.java`
- [x] 5.7 Napsat nebo aktualizovat controller integration test pro `GET /api/members/{id}`:
  - Ověřit `trainerLicense.level` ve response (místo `licenseNumber`)
  - Ověřit `refereeLicense` ve response
  - Soubor: `backend/src/test/java/com/klabis/members/infrastructure/restapi/MemberControllerApiTest.java`

## 6. Frontend — aktualizace API typů a MemberDetailPage

- [x] 6.1 Vygenerovat nové API typy ze Swagger schématu
  - Příkaz: `npm run generate-api` (nebo ekvivalent dle `frontend/CLAUDE.md`)
  - Soubor: `frontend/src/api/klabisApi.d.ts`
- [x] 6.2 Aktualizovat `MemberDetailPage.tsx` — sekce "Doklady a licence":
  - `trainerLicense`: zobrazit badge s `level` (T1/T2/T3) místo `licenseNumber`
  - `medicalCourse`: zobrazit "Ano" badge (zelený) pokud `completionDate != null`, jinak "Ne" (šedý) + datum platnosti pokud existuje
  - Přidat `refereeLicense` sekci: badge s `level` (R1/R2/R3) + datum platnosti
  - Soubor: `frontend/src/pages/members/MemberDetailPage.tsx`
- [x] 6.3 Aktualizovat `KlabisFieldsFactory.tsx` — form pole pro edit mód:
  - `TrainerLicenseDto`: nahradit text input pro `licenseNumber` dropdown selectem pro `level` (T1/T2/T3)
  - Přidat case `RefereeLicenseDto` s dropdown pro `level` (R1/R2/R3) + date picker pro `validityDate`
  - Soubor: `frontend/src/components/KlabisFieldsFactory.tsx`

## 7. Validace

- [x] 7.1 Spustit backend testy přes `test-runner` agenta
- [x] 7.2 Spustit frontend testy: `npm run test`
- [x] 7.3 Ověřit manuálně v prohlížeči: přihlásit se jako admin, otevřít detail člena, zkontrolovat sekci "Doklady a licence"

---

## Success Criteria

- `GET /api/members/{id}` vrací `trainerLicense.level` (enum T1/T2/T3) a `refereeLicense` (nullable)
- `PATCH /api/members/{id}` (admin) akceptuje `trainerLicense.level` a `refereeLicense.level` + `refereeLicense.validityDate`
- Databáze ukládá a načítá oba typy licencí bez ztráty dat
- Frontend zobrazí stupeň trenérské licence jako badge, zdravotní kurz jako Ano/Ne, rozhodčí licenci jako badge se stupněm
- Všechny existující testy prochází (žádná regrese)

## Odhad složitosti

**Střední** (1–2 dny). Změna je rozsáhlá (14+ souborů), ale vzory jsou zavedené — `RefereeLicense`
kopíruje strukturu `TrainerLicense`, persistence layer je plochý memento bez vnořených entit.

Největší riziko: breakující změna `TrainerLicense` (odebrání `licenseNumber`) vyžaduje
aktualizaci existujících testů, které testují ukládání/čtení `TrainerLicense`.
