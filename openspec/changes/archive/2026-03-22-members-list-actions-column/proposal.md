## Why

Seznam členů (GET /api/members) zobrazuje pouze 3 sloupce (Reg. číslo, Příjmení, Jméno). Admin nemá přehled o stavu členů ani nemůže provádět rychlé akce přímo ze seznamu — musí proklikávat do detailu každého člena. Pencil design již definuje rozšířený admin pohled s 8 sloupci včetně sloupce Akce s kontextovými ikonami.

## What Changes

- Rozšíření `MemberSummaryResponse` o pole `email` a `active` (viditelné jen pro MEMBERS:MANAGE přes field-level security)
- Přidání HATEOAS affordances na jednotlivé summary items (suspend/resume/update templates + permissions link)
- Rozšíření `MemberPermissionsLinkProcessor` pro práci s `MemberSummaryResponse` (nejen detail)
- Frontend: přidání sloupců E-mail, Stav a Akce do tabulky členů
- Sloupec Akce zobrazuje ikony podmíněně podle přítomnosti HAL templates/links:
  - `pencil` (editace) — navigace na detail, závisí na `_templates.default`
  - `shield` (oprávnění) — otevře PermissionsDialog, závisí na `_links.permissions`
  - `user-x` (suspend) — otevře HalFormButton overlay, závisí na `_templates.suspendMember`
  - `user-check` (resume) — otevře HalFormButton overlay, závisí na `_templates.resumeMember`

## Capabilities

### New Capabilities

_Žádné nové capabilities — jde o rozšíření existující._

### Modified Capabilities

- `members`: Rozšíření member summary response o nová pole (email, active) a HATEOAS affordances na summary items

## Impact

- **Backend API**: GET /api/members response se rozšíří o pole `email`, `active` a HAL affordances/links na každém summary itemu. Zpětně kompatibilní (přidání polí).
- **Backend kód**: `MemberSummaryResponse`, `MemberMapper`, `MemberController.listMembers()`, `MemberPermissionsLinkProcessor`
- **Frontend**: `MembersPage.tsx` — nové sloupce a akční ikony
- **Testy**: Rozšíření `MemberControllerApiTest` o nová pole a affordances
