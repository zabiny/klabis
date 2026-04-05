## Scenarios

### Vytvoření family group s rodiči

- Admin (MEMBERS:MANAGE) vytvoří family group → zadá název, rodiče (min 1) a volitelně další členy
- Tvůrce skupiny se NESTÁVÁ automaticky rodičem — rodiče se zadávají explicitně
- Rodiče se automaticky stávají i členy skupiny
- Exclusive membership platí i pro rodiče — rodič nesmí být v jiné family group

### Správa rodičů

- Rodič může přidat dalšího rodiče (= owner + member)
- Přidání rodiče automaticky přidá i členství ve skupině
- Odebrání rodiče odebere člena ze skupiny úplně (ne jen degradace na běžného člena)
- Nelze odebrat posledního rodiče — min 1 rodič musí vždy existovat

### Přejmenování owners → parents (jen FamilyGroup kontext)

- REST API: `/api/family-groups/{id}/parents` místo `/owners`
- Response: pole `parents` místo `owners`
- Frontend labels: "Rodiče" místo "Vlastníci"
- Doménový model `UserGroup.owners` zůstává beze změny — parent je sémantická vrstva nad owner

## Why

Aktuální model používá generický pojem "owner" pro všechny typy skupin. V kontextu family group je přirozený termín "rodič" (parent). Navíc se aktuálně tvůrce skupiny automaticky stává ownerem, což nedává smysl — family group typicky vytváří admin, který není rodičem.

## What Changes

- `FamilyGroup.CreateFamilyGroup` command: nahrazení `owner: MemberId` za `parents: Set<MemberId>` (min 1)
- REST API endpoint pro vytvoření: request přijímá `parentIds` (povinné, min 1) + `memberIds` (volitelné)
- REST API pro správu rodičů: `/parents` místo `/owners`
- Přidání parenta = `addOwner` + `addMember` v doméně
- Odebrání parenta = `removeOwner` + `removeMember` v doméně
- Response DTO: `parents` pole místo `owners`
- Frontend: labels, typy, API volání

## Capabilities

### Modified Capabilities

- `user-groups`: Family group creation vyžaduje explicitní rodiče místo automatického owner assignment. API terminologie owners → parents jen v kontextu family groups.

## Impact

- Backend: `FamilyGroup`, `GroupManagementPort`, `GroupManagementService`, `FamilyGroupController`, response DTOs, request DTOs, testy
- Frontend: `FamilyGroupDetailPage`, `FamilyGroupsPage`, labels
- OpenSpec: aktualizace spec.md — terminologie owners → parents pro family groups
- Žádné DB migrace — `user_group_owners` tabulka zůstává (parent = owner interně)
- Cross-module API (`LastOwnershipChecker`, `FamilyGroupProvider`) beze změny
