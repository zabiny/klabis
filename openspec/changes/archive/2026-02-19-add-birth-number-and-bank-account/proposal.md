## Why

Issue #3 (Zakládání nových členů oddílu) vyžaduje dva chybějící atributy pro kompletní správu členských dat:

1. **Rodné číslo** - povinné pro české členy, slouží jako identifikátor v ORIS/ČUS systémech a umožňuje automatické odvození data narození a pohlaví
2. **Číslo bankovního účtu** - volitelné pole pro snadnější proplácení cestovních náhrad a dokladů bez nutnosti používat hotovost

Obě pole jsou GDPR-citlivá a vyžadují šifrování v databázi. Rodné číslo musí být podmíněně dostupné pouze pro českou národnost.

## What Changes

- Přidání value objectu `BirthNumber` s validací formátu rodného čísla (RRMMDD/XXXX nebo RRMMDDXXXX)
- Přidání pole `birthNumber` do `Member` agregátu s podmíněnou validací (pouze CZ národnost)
- Přidání pole `bankAccountNumber` do `Member` agregátu (volitelné, validace IBAN formátu)
- Konfigurace Jasypt šifrování pro sloupec `birth_number` v databázi (GDPR compliance)
- Aktualizace databázového schématu (V001__initial_schema.sql):
  - `birth_number VARCHAR(255)` (šifrováno)
  - `bank_account_number VARCHAR(50)` (nešifrováno)
- Rozšíření `RegisterMemberRequest` a `UpdateMemberRequest` o nová pole
- Rozšíření `MemberDetailsResponse` o nová pole
- Podmíněné zobrazení rodného čísla v UI (pouze při CZ národnosti)
- Automatické odvození data narození a pohlaví z rodného čísla (volitelné, pro validaci konzistence)

## Capabilities

### New Capabilities

- `birth-number-management`: Správa rodného čísla pro české členy včetně validace formátu, šifrování, a podmíněné dostupnosti
- `bank-account-management`: Správa čísla bankovního účtu pro členy (volitelné pole s IBAN validací)

### Modified Capabilities

- `member-registration`: Přidání rodného čísla a čísla bankovního účtu do procesu registrace nového člena
- `member-profile-update`: Rozšíření možnosti editace profilu o rodné číslo (pouze admin) a číslo bankovního účtu

## Impact

**Affected Code:**
- `Member.java` - nová pole birthNumber, bankAccountNumber
- `PersonalInformation.java` - možná úprava pro konzistenci s rodným číslem
- `MemberMemento.java` - mapování nových sloupců
- `RegisterMemberRequest.java` - nová pole
- `UpdateMemberRequest.java` - nová pole
- `MemberDetailsResponse.java` - nová pole
- `RegistrationService.java` - validace a zpracování nových polí
- `ManagementService.java` - update logika pro nová pole

**Database:**
- V001__initial_schema.sql - přidání 2 sloupců do tabulky `members`
- Konfigurace Jasypt pro šifrování

**Security/GDPR:**
- Šifrování rodného čísla pomocí Jasypt (AES-256)
- Audit trail pro přístup k rodným číslům
- Podmíněné zobrazení citlivých dat

**API:**
- GET /api/members/{id} - vrací nová pole
- POST /api/members - přijímá nová pole
- PATCH /api/members/{id} - přijímá nová pole
