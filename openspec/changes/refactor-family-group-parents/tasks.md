## 1. Domain — CreateFamilyGroup command refaktoring

- [x] 1.1 Změnit `CreateFamilyGroup(name, owner, initialMembers)` na `CreateFamilyGroup(name, parents: Set<MemberId>, initialMembers: Set<MemberId>)` s validací min 1 parent
- [x] 1.2 Upravit `FamilyGroup.create()` — parents se nastaví jako owners + přidají jako members
- [x] 1.3 Přidat doménové metody `addParent`/`removeParent` do FamilyGroup — delegují na addOwner+addMember / removeOwner+removeMember
- [x] 1.4 Aktualizovat testy v FamilyGroupTest pro nový CreateFamilyGroup command a parent metody

## 2. Application service — parent management s MEMBERS:MANAGE autorizací

- [x] 2.1 Upravit `GroupManagementService.createFamilyGroup()` — validovat exclusive membership pro všechny parents, přidat parents jako owners+members
- [x] 2.2 Přidat `addParentToFamilyGroup(UserGroupId, MemberId)` metodu — vyžaduje MEMBERS:MANAGE (ne owner check), přidá parent jako owner+member
- [x] 2.3 Přidat `removeParentFromFamilyGroup(UserGroupId, MemberId)` metodu — vyžaduje MEMBERS:MANAGE, odebere parent z owners i members
- [x] 2.4 Odstranit staré owner-based metody pro family group kontext (addOwnerToGroup/removeOwnerFromGroup se přestanou používat pro family groups)
- [x] 2.5 Aktualizovat GroupManagementPort rozhraní s novými metodami

## 3. REST API — přejmenování owners na parents

- [x] 3.1 Upravit `CreateFamilyGroupRequest` — přidat `parentIds` (povinné, min 1), odebrat automatické přiřazení authenticated user jako owner
- [x] 3.2 Přejmenovat endpointy: `POST /parents` místo `/owners`, `DELETE /parents/{id}` místo `/owners/{id}`
- [x] 3.3 Upravit response DTO — `parents` pole místo `owners` ve FamilyGroupResponse
- [x] 3.4 Změnit autorizaci parent management endpointů na MEMBERS:MANAGE (bez owner check)
- [x] 3.5 Upravit HAL affordance — `addFamilyGroupParent`/`removeFamilyGroupParent` jen pro uživatele s MEMBERS:MANAGE
- [x] 3.6 Aktualizovat controller testy pro nové endpointy, request/response formáty a autorizaci

## 4. Frontend — parent terminologie a create flow

- [ ] 4.1 Upravit FamilyGroupsPage create modal — přidat `parentIds` pole místo automatického owner
- [ ] 4.2 Upravit FamilyGroupDetailPage — sekce "Rodiče" místo "Vlastníci", nové HAL template názvy
- [ ] 4.3 Aktualizovat labels v localization — přejmenování owner-related labels na parent-related pro family group kontext

## 5. OpenSpec — sync delta specs

- [ ] 5.1 Sync delta spec do hlavního `openspec/specs/user-groups/spec.md`
