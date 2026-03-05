## Why

Specifikace (issue #4) definuje, která pole může člen klubu sám editovat, ale aktuální implementace tato pravidla porušuje — blokuje členovi přístup k polím, která by mohl měnit (číslo čipu, OP, bankovní účet, řidičák apod.), a naopak dovoluje adminům měnit pole (gender, birthNumber), která v existujících specs nejsou pokryta. Spec v `openspec/specs/members/spec.md` obsahuje neúplné a nesprávné rozdělení polí mezi self-edit a admin-edit.

## What Changes

- **Oprava Requirement: Member-Editable Fields** — rozšíření seznamu polí dostupných pro self-edit dle issue #4
- **Oprava Requirement: Admin-Only Fields** — doplnění všech admin-only polí (gender, birthNumber) a přidání jasného odůvodnění proč jsou admin-only
- **Oprava scénářů** — stávající scénáře pro "Member attempts to update admin-only fields" jsou nekonzistentní s novou definicí

## Capabilities

### New Capabilities

- žádné nové capability

### Modified Capabilities

- `members`: Oprava požadavků na rozdělení editovatelných polí mezi self-edit a admin-edit v rámci `PATCH /api/members/{id}`

## Impact

- `openspec/specs/members/spec.md` — aktualizace Requirement: Member-Editable Fields a Requirement: Admin-Only Fields
- `ManagementServiceImpl` — field-level security se musí shodovat s novou spec (viz task `tasks/refactor-member-application-services.md`)
- `UpdateMemberRequest` — doplnění chybějících admin-only polí (gender, birthNumber)
