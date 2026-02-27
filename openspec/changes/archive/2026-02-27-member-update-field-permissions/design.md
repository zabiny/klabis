## Context

Endpoint `PATCH /api/members/{id}` implementuje dual-authorization model: člen může editovat svůj vlastní záznam (self-edit), admin může editovat libovolného člena. Aktuální implementace (`ManagementServiceImpl`) však chybně klasifikuje pole jako admin-only, přestože je specifikace (issue #4) řadí mezi self-edit.

Zároveň spec v `specs/members/spec.md` obsahuje neúplnou definici admin-only polí — chybí `gender` a `birthNumber`.

Tato změna opravuje spec a dokumentuje správné rozdělení polí, které se pak promítne do implementace.

## Goals / Non-Goals

**Goals:**
- Sjednotit specifikaci s issue #4 — jasně definovat, která pole patří do self-edit a která do admin-only
- Doplnit chybějící admin-only pole (`gender`, `birthNumber`) do spec

**Non-Goals:**
- Refactoring aplikačních služeb a kontrolerů (to řeší `tasks/refactor-member-application-services.md`)
- Změna validačních pravidel jednotlivých polí
- Nové endpointy nebo změna HTTP metod

## Decisions

### Rozdělení polí

```
PATCH /api/members/{id}
         │
         ├─ Self-edit (člen edituje svůj vlastní záznam)
         │   email, phone, address, chipNumber, nationality,
         │   bankAccountNumber, identityCard, drivingLicenseGroup,
         │   medicalCourse, trainerLicense, dietaryRestrictions,
         │   guardian (email + phone)
         │
         └─ Admin-only (vyžaduje MEMBERS:UPDATE)
             firstName, lastName, dateOfBirth, gender, birthNumber
```

**Proč jsou admin-only pole právě tato?**

Pole `firstName`, `lastName`, `dateOfBirth` a `gender` jsou identifikační údaje člena, jejichž změna může mít právní nebo evidenční dopad (evidence v ORIS, CUS). Proto je spravuje výhradně admin.

`birthNumber` je GDPR-citlivý identifikátor (šifrován v DB), jehož editace musí být auditovatelná a omezená.

**Proč nationality patří do self-edit?**

Národnost člen zná sám, je běžně editovatelná i v jiných systémech a nemá přímý právní dopad na identitu. Podmíněná vazba na `birthNumber` (pouze pro CZ) je vynucována doménovým pravidlem — pokud člen změní národnost z CZ, `birthNumber` se vymaže.

### Chování při self-edit s admin-only poli v requestu

Admin-only pole přítomná v PATCH requestu od non-admina jsou **tiše ignorována** (ne 403). Toto chování zachováme — je dokumentováno v existující spec a frontend se na něj spoléhá.

## Risks / Trade-offs

- **Risk:** Rozšíření self-edit polí může umožnit členům měnit data, která jsou synchronizována do ORIS/CUS (adresa, email, telefon, číslo čipu). → Mitigation: ORIS/CUS synchronizace (issue #264, #265) musí být navržena tak, aby reagovala na změny přes doménové události, nikoliv batch.
- **Risk:** `nationality` jako self-edit pole v kombinaci s mazáním `birthNumber` může překvapit adminy. → Mitigation: zdokumentovat v API (OpenAPI description).
