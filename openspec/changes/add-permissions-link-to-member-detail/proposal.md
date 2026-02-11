## Why

Frontend potřebuje odkaz na endpoint pro správu oprávnění uživatele přímo z Member detail response. Aktuálně klient musí znát URL strukturu (`/api/users/{userId}/permissions`), což porušuje HATEOAS princip a ztěžuje navigaci. Link umožní frontendu dynamicky zjistit, zda má uživatel právo upravovat oprávnění.

## What Changes

- Přidat `userId` field do `MemberDetailsDTO` (application layer)
- Přidat `userId` field do `MemberDetailsResponse` (presentation layer)
- Aktualizovat `ManagementService.mapToMemberDetailsDTO()` pro zahrnutí userId
- Vytvořit `MemberPermissionsLinkProcessor` (RepresentationModelProcessor) pro přidání conditional permissions link
- Link se zobrazí pouze uživatelům s `MEMBERS:PERMISSIONS` authority

## Capabilities

### New Capabilities
<!-- Žádné nové capability - jedná se o HATEOAS link enrichment existující functionality -->

### Modified Capabilities
- `member-management`: Přidání userId do Member detail response a HATEOAS link na user permissions

## Impact

**Affected Files:**
- `backend/src/main/java/com/klabis/members/management/MemberDetailsDTO.java` - přidat userId field
- `backend/src/main/java/com/klabis/members/management/MemberDetailsResponse.java` - přidat userId field
- `backend/src/main/java/com/klabis/members/management/ManagementService.java` - mapovat userId v DTO factory
- `backend/src/main/java/com/klabis/members/management/MemberController.java` - aktualizovat DTO mapping
- New: `backend/src/main/java/com/klabis/members/management/MemberPermissionsLinkProcessor.java`

**API Changes:**
- Member detail response (`GET /api/members/{id}`) obsahuje nový field `userId` (non-breaking, additive)
- Member detail response obsahuje conditional `_links.permissions` odkaz na `GET /api/users/{userId}/permissions`

**Dependencies:**
- Members modul závisí na Users modul (PermissionController) - již existující závislost
- PermissionController je již public, není třeba měnit viditelnost

**Testing:**
- Unit test pro MemberPermissionsLinkProcessor
- Integration test pro Member API s/bez MEMBERS:PERMISSIONS authority
- Aktualizace existujících testů mockujících MemberDetailsDTO
