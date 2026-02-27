## 1. Spec update

- [x] 1.1 Sync delta spec into `openspec/specs/members/spec.md` — nahradit requirements `Member-Editable Fields` a `Admin-Only Fields` novou verzí z delta spec (`/opsx:sync`)

## 2. Domain — Member aggregate

- [x] 2.1 Přidat command `Member.SelfUpdate` s poli: email, phone, address, chipNumber, nationality, bankAccountNumber, identityCard, drivingLicenseGroup, medicalCourse, trainerLicense, dietaryRestrictions, guardian
- [x] 2.2 Přidat `handle(SelfUpdate)` metodu v `Member` — PATCH semantics (null = ponechat stávající hodnotu), validace kontaktu (alespoň 1 email + telefon), mazání birthNumber při změně nationality z CZ
- [x] 2.3 Přidat command `Member.UpdateMemberByAdmin` s poli všech self-edit polí + firstName, lastName, dateOfBirth, gender, birthNumber
- [x] 2.4 Přidat `handle(UpdateMemberByAdmin)` metodu v `Member` — stejná PATCH semantics, validace birthNumber jen pro CZ národnost
- [x] 2.5 Napsat unit testy pro `handle(SelfUpdate)` — pokrýt všechna pole, kontaktní validaci, mazání birthNumber
- [x] 2.6 Napsat unit testy pro `handle(UpdateMemberByAdmin)` — pokrýt admin-only pole, birthNumber validaci

## 3. Application layer — ManagementService

- [x] 3.1 Přidat metodu `updateMember(UUID memberId, Member.SelfUpdate command)` do `ManagementService` rozhraní — vrací `Member`
- [x] 3.2 Přidat metodu `updateMember(UUID memberId, Member.UpdateMemberByAdmin command)` do `ManagementService` rozhraní — vrací `Member`
- [x] 3.3 Implementovat obě metody v `ManagementServiceImpl` — load member, handle command, save, return Member (bez importů z `infrastructure.restapi`)
- [x] 3.4 Odstranit původní `updateMember(UUID, UpdateMemberRequest)` metodu ze service a implementace
- [x] 3.5 Napsat unit testy pro `ManagementServiceImpl` — self-edit a admin-edit cesty

## 4. API — UpdateMemberRequest a controller

- [x] 4.1 Doplnit `UpdateMemberRequest` o chybějící pole: `gender`, `birthNumber` (pokud tam nejsou)
- [x] 4.2 Přesunout self-edit check (ověření, že non-admin edituje jen svůj vlastní záznam) do `MemberController`
- [x] 4.3 V `MemberController.updateMember` sestavit `Member.SelfUpdate` nebo `Member.UpdateMemberByAdmin` command dle oprávnění volajícího (`MEMBERS:UPDATE`), předat do service
- [x] 4.4 Napsat integrační testy pro `MemberController` — self-edit s rozšířenými poli (chipNumber, identityCard, drivingLicenseGroup, medicalCourse, trainerLicense, nationality, guardian), admin edit s gender a birthNumber
- [x] 4.5 Přidat label `BackendCompleted` k issue #4

## 5. Ověření a cleanup

- [x] 5.1 Ověřit, že `ManagementService` a `ManagementServiceImpl` neobsahují žádné importy z `com.klabis.members.infrastructure.restapi`
- [x] 5.2 Spustit všechny testy members modulu a ověřit, že prochází
