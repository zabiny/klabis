## Why

Frontend potřebuje odkaz na endpoint pro správu oprávnění uživatele přímo z Member detail response. Aktuálně klient musí znát URL strukturu (`/api/users/{userId}/permissions`), což porušuje HATEOAS princip a ztěžuje navigaci. Link umožní frontendu dynamicky zjistit, zda má uživatel právo upravovat oprávnění.

## What Changes

- Vytvořit `MemberPermissionsLinkProcessor` (RepresentationModelProcessor) pro přidání conditional permissions link
- Link se zobrazí pouze uživatelům s `MEMBERS:PERMISSIONS` authority
- Použít `member.id` (který je UserId) přímo v processoru pro vytvoření permissions linku
- **Neměnit** DTOs - userId je redundantní, protože member.id už je UserId (1:1 relace)

## Capabilities

### New Capabilities
<!-- Žádné nové capability - jedná se o HATEOAS link enrichment existující functionality -->

### Modified Capabilities
- `member-management`: Přidání userId do Member detail response a HATEOAS link na user permissions

## Impact

**Affected Files:**
- New: `backend/src/main/java/com/klabis/members/management/MemberPermissionsLinkProcessor.java`

**API Changes:**
- Member detail response (`GET /api/members/{id}`) obsahuje conditional `_links.permissions` odkaz na `GET /api/users/{userId}/permissions`
- **Žádné změny polí** - používá se existující `id` field (který je UserId)

**Dependencies:**
- Members modul závisí na Users modul (PermissionController) - již existující závislost
- PermissionController je již public, není třeba měnit viditelnost

**Testing:**
- Unit test pro MemberPermissionsLinkProcessor
- Integration test pro Member API s/bez MEMBERS:PERMISSIONS authority
