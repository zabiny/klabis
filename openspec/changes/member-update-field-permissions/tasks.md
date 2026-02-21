## 1. Spec update

- [ ] 1.1 Sync delta spec into `openspec/specs/members/spec.md` — nahradit requirements `Member-Editable Fields` a `Admin-Only Fields` novou verzí z delta spec (`/opsx:sync`)

## 2. Domain — Member aggregate

- [ ] 2.1 Přidat command `Member.SelfUpdate` s poli: email, phone, address, chipNumber, nationality, bankAccountNumber, identityCard, drivingLicenseGroup, medicalCourse, trainerLicense, dietaryRestrictions, guardian
- [ ] 2.2 Přidat `handle(SelfUpdate)` metodu v `Member` — PATCH semantics (null = ponechat stávající hodnotu), validace kontaktu (alespoň 1 email + telefon), mazání birthNumber při změně nationality z CZ
- [ ] 2.3 Přidat command `Member.UpdateMemberByAdmin` s poli všech self-edit polí + firstName, lastName, dateOfBirth, gender, birthNumber
- [ ] 2.4 Přidat `handle(UpdateMemberByAdmin)` metodu v `Member` — stejná PATCH semantics, validace birthNumber jen pro CZ národnost
- [ ] 2.5 Napsat unit testy pro `handle(SelfUpdate)` — pokrýt všechna pole, kontaktní validaci, mazání birthNumber
- [ ] 2.6 Napsat unit testy pro `handle(UpdateMemberByAdmin)` — pokrýt admin-only pole, birthNumber validaci

## 3. Application layer — ManagementService

- [ ] 3.1 Přidat metodu `updateMember(UUID memberId, Member.SelfUpdate command)` do `ManagementService` rozhraní — vrací `Member`
- [ ] 3.2 Přidat metodu `updateMember(UUID memberId, Member.UpdateMemberByAdmin command)` do `ManagementService` rozhraní — vrací `Member`
- [ ] 3.3 Implementovat obě metody v `ManagementServiceImpl` — load member, handle command, save, return Member (bez importů z `infrastructure.restapi`)
- [ ] 3.4 Odstranit původní `updateMember(UUID, UpdateMemberRequest)` metodu ze service a implementace
- [ ] 3.5 Napsat unit testy pro `ManagementServiceImpl` — self-edit a admin-edit cesty

## 4. API — UpdateMemberRequest a controller

- [ ] 4.1 Doplnit `UpdateMemberRequest` o chybějící pole: `gender`, `birthNumber` (pokud tam nejsou)
- [ ] 4.2 Přesunout self-edit check (ověření, že non-admin edituje jen svůj vlastní záznam) do `MemberController`
- [ ] 4.3 V `MemberController.updateMember` sestavit `Member.SelfUpdate` nebo `Member.UpdateMemberByAdmin` command dle oprávnění volajícího (`MEMBERS:UPDATE`), předat do service
- [ ] 4.4 Napsat integrační testy pro `MemberController` — self-edit s rozšířenými poli (chipNumber, identityCard, drivingLicenseGroup, medicalCourse, trainerLicense, nationality, guardian), admin edit s gender a birthNumber
- [ ] 4.5 Přidat label `BackendCompleted` k issue #4

## 5. Ověření a cleanup

- [ ] 5.1 Ověřit, že `ManagementService` a `ManagementServiceImpl` neobsahují žádné importy z `com.klabis.members.infrastructure.restapi`
- [ ] 5.2 Spustit všechny testy members modulu a ověřit, že prochází
