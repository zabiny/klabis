## Why

Klabis v současnosti podporuje registraci a správu členů, ale chybí funkce pro řádné ukončení členství na základě odhlášky. Správci musí manuálně označit členy jako neaktivní bez evidence kdy a proč k ukončení došlo. To vede k nepřehledným záznamům a chybějící audit stopě.

## What Changes

- Přidání možnosti ukončit členství s důvodem (odhláška, přestup, jiný)
- Automatické zaznamenání data a času ukončení
- Evidence důvodu ukončení a volitelné poznámky
- REST API endpoint pro provedení ukončení
- Domain event pro integraci s dalšími moduly (Finance, ORIS, CUS, Groups)

## Capabilities

### New Capabilities
- `member-termination`: Ukončení členství s důvodem, časovým údajem a auditací

### Modified Capabilities
- `members`: Rozšíření Member aggregate o pole pro terminaci (deactivationReason, deactivatedAt)

## Impact

**Affected Code:**
- `Member.java` - přidání fieldů a domain command
- `MemberController.java` - nový endpoint
- `MemberJdbcRepository.java` - uložení nových fieldů
- Database schema V001 - přidání sloupců do `members` tabulky

**API Changes:**
- Nový endpoint: `POST /api/members/{id}/terminate`
- Response DTO rozšířeno o termination informace

**Dependencies:**
- Žádné nové externí závislosti

**Systems:**
- Members bounded context
- Future integrations: Finance (issue #266), ORIS (#267), CUS (#268), Groups (#269)
