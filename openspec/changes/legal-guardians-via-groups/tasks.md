## 1. Iterace: skupina zákonných zástupců místo rodinné skupiny

- [ ] 1.1 Přejmenovat `FamilyGroup` → `LegalGuardianGroup` (balíček `members.legalguardiangroup`, discriminator `LEGAL_GUARDIAN`, CHECK a komentáře ve `V001__initial_schema.sql`) přes JetBrains refactoring; testy zelené beze změny chování
- [ ] 1.2 Doménové testy (red) a implementace: vlastník ≠ člen, zástupce ve více skupinách, dítě max v jedné skupině, jen nezletilí, neprázdná množina zástupců, generovaný název („Novák a Svobodová“)
- [ ] 1.3 Testy (red) a implementace `LegalGuardianGroupService` – jádro find-or-create/merge (D2) a operace `changeGroupGuardians(groupId, guardians)`: úprava na místě, sloučení s existující množinou, smazání prázdné skupiny, odmítnutí prázdné množiny
- [ ] 1.4 Úprava suspension blockeru: `findAll` místo `findOne`, varování pro jediného zástupce nezletilého; testy `ManagementService`
- [ ] 1.5 API spec: `/api/legal-guardian-groups` (list, detail, `PUT …/guardians` s affordancí `setLegalGuardianGroupGuardians`), odstranit create/delete/parents/children, root rel `legalGuardianGroups`, odkaz `legalGuardianGroup` na detailu člena (jen `MEMBERS_MANAGE`); přegenerovat bundle a frontend typy
- [ ] 1.6 Controller testy (red) a implementace: přístup jen `MEMBERS:MANAGE`, detail se zástupci a dětmi a jejich odkazy, nastavení zástupců skupiny včetně sloučení
- [ ] 1.7 Frontend: stránka „Zákonní zástupci“ (seznam, detail, formulář zástupců skupiny), položka menu v Administraci, tlačítko „Zákonní zástupci“ na detailu člena; testy + `npm run build`

## 2. Iterace: nečlenský zákonný zástupce, účet a profil

- [ ] 2.1 `V001`: `common.users.user_name VARCHAR(255)`, nová tabulka `members.legal_guardians`; testy perzistence
- [ ] 2.2 Doménové testy (red) a implementace agregátu `LegalGuardian` (povinný e-mail i telefon, update bez vymazání kontaktu) a aplikační služby pro založení: `User` s username = e-mail, jen `MEMBERS:READ`, odmítnutí kolize e-mailu s uživatelem i s dospělým členem
- [ ] 2.3 `GuardianContactResolver` (člen → údaje člena, nečlen → `LegalGuardian`) s testy
- [ ] 2.4 API spec + controller testy (red) + implementace `GET/PATCH /api/legal-guardians/{userId}` (přístup `MEMBERS:MANAGE` nebo sám zástupce)
- [ ] 2.5 API spec + testy (red) + implementace `GET /api/legal-guardian-options?q=` (nečlenští zástupci ∪ aktivní členové ≥ 18, dedupe dle `UserId`, `MEMBERS:CREATE`/`MEMBERS:MANAGE`); `x-hal-input-type: UserId` options přepojit na tento endpoint
- [ ] 2.6 Token customizer a `KlabisUserDetailsService`: lookup člena podle `UserId`, jména nečlenského zástupce do claimů; testy (člen s username = e-mail je `is_member: true`)
- [ ] 2.7 Root `_links.profile` (člen → detail člena, nečlenský zástupce → profil zástupce); testy `RootController`
- [ ] 2.8 Frontend: stránka profilu zástupce (zobrazení + editace), „Můj profil“ na HomePage z odkazu `profile`, login label „Registrační číslo nebo e-mail“; testy + `npm run build`

## 3. Iterace: zástupci nezletilého ze skupiny (odstranění guardian z člena)

- [ ] 3.1 Testy (red) a implementace `LegalGuardianGroupService.setGuardiansOf(minor, guardians)` nad jádrem D2 (sourozenec se nemění, přesun do existující množiny, úprava na místě u jediného dítěte, odmítnutí dospělého)
- [ ] 3.2 Doménová služba `MemberCompleteness` (testy red: pravidla nezletilý/dospělý, kontakty kteréhokoli zástupce, `GUARDIAN` bez skupiny, výjimka změny data narození dospělý → nezletilý); `Member.missingData`/edit validace na ni přepojit
- [ ] 3.3 Odstranit `GuardianInformation` z `Member`, `MemberMemento`, `MemberCreatedEvent` (`isMinor()` z data narození), `UpdateMemberRequest`, konvertorů a sloupce `guardian_*` z `V001`; `MemberActivationContactVerifier` dočasně jen vlastní e-mail
- [ ] 3.4 API spec: `legalGuardians[]` na detailu člena (`MEMBERS_MANAGE` + owner-visible, odkazy `member`/`legalGuardian`), `PUT /api/members/{id}/legal-guardians` (`setMemberLegalGuardians`, jen nezletilý, položka `{userId}` nebo nový zástupce); přegenerovat bundle a typy
- [ ] 3.5 Controller testy (red) a implementace: detail nezletilého se zástupci, dospělý bez zástupců, nastavení zástupců včetně inline založení nového zástupce v jedné transakci, poslední zástupce nelze odebrat
- [ ] 3.6 Seznam členů: štítek „Neúplné údaje“ čte materializovaný příznak (`data_incomplete`), detail počítá živě; testy
- [ ] 3.7 Frontend: sekce „Zákonní zástupci“ na detailu nezletilého, komponenta „vybrat nebo založit zástupce“ (pole objektů HAL-FORMS) + integrační test, odstranit guardian pole z editace; testy + `npm run build`

## 4. Iterace: registrace nezletilý / dospělý

- [ ] 4.1 API spec `RegisterMemberRequest`: `guardian` → `legalGuardians[]`, `legalGuardianUserId` pro převzetí zástupce; přegenerovat bundle a typy
- [ ] 4.2 Testy (red) a implementace `RegistrationService`: nezletilý vyžaduje ≥ 1 zástupce (existující/nový) a zavolá `setGuardiansOf` v jedné transakci; dospělý se zástupci odmítnut; dospělý vyžaduje vlastní e-mail a telefon; chyba při zakládání zástupce nic nevytvoří
- [ ] 4.3 Testy (red) a implementace převzetí nečlenského zástupce při registraci dospělého (stejný `UserId`, smazání `LegalGuardian`, skupiny beze změny, login e-mailem funguje a `is_member: true`)
- [ ] 4.4 Frontend registrace: sekce podle data narození, volitelné vlastní kontakty nezletilého, zástupci (výběr/založení), převzetí zástupce s předvyplněním; testy + `npm run build`

## 5. Iterace: aktivace účtů

- [ ] 5.1 Testy (red) a implementace pravidel `MemberActivationContactVerifier`: nečlenský zástupce svým e-mailem, dospělý člen vlastním e-mailem, nezletilý nikdy; login name může být e-mail
- [ ] 5.2 API spec + testy (red) + implementace `POST /api/members/{id}/account-activation` (`sendMemberAccountActivation`): jen `MEMBERS:MANAGE`, nezletilý s vlastním e-mailem a účtem čekajícím na aktivaci; odkaz na e-mail dítěte
- [ ] 5.3 Frontend: akce „Založit účet“ na detailu nezletilého s potvrzením; formulář aktivace „Registrační číslo nebo e-mail“; testy + `npm run build`

## 6. Iterace: dovršení 18 let

- [ ] 6.1 Testy (red) a implementace `MinorAgeOutJob` (denně): členové s 18. narozeninami dnes → přepočet a uložení `data_incomplete`, publikace `MinorAgedOutEvent`
- [ ] 6.2 Testy (red) a implementace listeneru v `legalguardiangroup`: odebrání dítěte, smazání skupiny bez dětí, idempotence

## 7. Example data a dokončení

- [ ] 7.1 `example-data`: nečlenský zástupce se dvěma sourozenci, člen-zástupce s dítětem, dítě se dvěma zástupci (člen + nečlen), nezletilý z ORIS bez zástupce
- [ ] 7.2 Zapsat ADR (`docs/design-decisions.md`) k modelu zástupců přes skupiny a login e-mailem
- [ ] 7.3 Plný backend test suite sekvenčně s `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true` (test-runner agent), frontend testy a `npm run build`
- [ ] 7.4 QA testování scénářů ze specifikací přes Playwright (registrace nezletilého, úprava zástupců, stránka skupin, login zástupce, „Založit účet“)
- [ ] 7.5 Přidat label `BackendCompleted` na issues #332, #333 a #334
