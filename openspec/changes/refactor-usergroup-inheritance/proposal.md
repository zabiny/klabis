## Why

`UserGroup` je v současnosti kompozitní building block v `common.usergroup` — každý skupinový agregát (`TrainingGroup`, `FamilyGroup`, `FreeGroup`) ho drží jako `private final` field a deleguje na něj ~13–15 metod. Delegace vytváří zbytečný boilerplate a překladovou vrstvu `MemberId ↔ UserId` na každém vstupu i výstupu. `UserGroup` navíc nepatří do `common` — slouží výhradně skupinovým agregátům v `groups` modulu.

## What Changes

- `UserGroup` se přesune do `groups.common.domain` a přejmenuje na `MemberGroup` — abstraktní základní třída rozšiřující `KlabisAggregateRoot`, celé public API pracuje s `MemberId`
- `GroupMembership` se přesune do `groups.common.domain`, `UserId userId` → `MemberId memberId`
- `TrainingGroup`, `FamilyGroup`, `FreeGroup` přejdou na `extends MemberGroup`, odstraní `private final UserGroup userGroup` a všechny delegační metody (~120 řádků celkem)
- `WithInvitations` a `Invitation` se přesunou do `groups.freegroup.domain`, API přepíše z `UserId` na `MemberId`
- Skupinové výjimky (`MemberAlreadyInGroupException` atd.) se přesunou z `common.usergroup` do `groups.common.domain`
- Handlery skupinových výjimek se přesunou z `common.mvc.MvcExceptionHandler` do nového `GroupsExceptionHandler` v `groups` modulu
- `GroupMemento` zůstane beze změny logiky — upraví se pouze importy (`memberId()` místo `userId()`)
- Celý package `common.usergroup` se odstraní

## No Behavior Change Justification

Změna je čistě strukturální — přesun a přejmenování tříd, náhrada kompozice dědičností při zachování identického chování. Žádná business pravidla se nemění, žádné API endpointy se nemění.

**Specs reviewed:**
- `openspec/specs/groups/spec.md` — neovlivněno; skupinová funkcionalita (vytváření, přiřazování členů, správa trainerů/rodičů) zůstává identická

**Why no spec update is needed:**
Jde o interní restrukturalizaci doménové a infrastrukturní vrstvy. Chování skupin z pohledu uživatele (API kontrakt, HTTP responses, business pravidla) se nemění.

## Impact

- `common.usergroup` — celý package se odstraní (~18 souborů)
- `common.mvc.MvcExceptionHandler` — odebrání 3 skupinových handlerů
- `groups.common.domain` — nový package s `MemberGroup`, `GroupMembership`, výjimkami
- `groups.traininggroup/familygroup/freegroup` — dědičnost místo kompozice (~25 souborů)
