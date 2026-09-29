## Why

Zákonný zástupce nezletilého (rodič) nemusí mít plnohodnotný členský účet. Rodinná skupina dnes vyžaduje, aby byl rodič `Member`, což brání dalšímu kroku (správa zákonných zástupců mimo členskou základnu). Rodičem má být libovolný uživatel systému (`User`), děti zůstávají členy.

## What Changes

- Rodič rodinné skupiny je identifikován `UserId` (ne `MemberId`); rodičem může být uživatel bez členského profilu.
- Děti zůstávají identifikovány `MemberId` (vždy plnohodnotní členové); doménové API pro děti přijímá `MemberId`.
- Rodič je nadále automaticky členem skupiny (chování beze změny), vnitřně je celá skupina na `UserId` (`MemberId` a `UserId` sdílejí stejné UUID).
- REST API: rodič se zadává/vrací jako `userId` (vytvoření skupiny, přidání a odebrání rodiče, `ParentResponse`); děti dál `memberId`. Výběr rodiče v UI zatím nabízí členy (výběr nečlenských uživatelů přijde s dalším krokem).
- Přístup k detailu skupiny se ověřuje podle `userId` z tokenu (rodič bez členského profilu vidí svou skupinu); rodič již nemá odkaz `member` (odkaz `user` na základní detaily uživatele přijde v další fázi).
- Pravidlo „jeden člen / uživatel v maximálně jedné rodinné skupině“ platí nad `UserId`.
- **BREAKING** (API): pole `parent` / `memberId` u rodičů se mění na `userId`; cesta pro odebrání rodiče používá `{userId}`.
- Databáze: sloupec `groups.user_group_owners.member_id` se přejmenuje na `owner_id` přímo v `V001` (sdílená tabulka všech typů skupin; žádný perzistentní stav, nová migrace není potřeba).

## Capabilities

### New Capabilities
<!-- žádné -->

### Modified Capabilities
- `user-groups`: rodič rodinné skupiny je libovolný uživatel (nemusí být člen); přidání/odebrání rodiče podle uživatele; výlučnost rodinné skupiny a přístup k detailu podle uživatele.

## Impact

- Backend: `groups` modul (`FamilyGroup`, `FamilyGroupFilter`, management service, `FamilyGroupController`, `FamilyGroupRepositoryAdapter`, `GroupOwnerMemento`, `GroupJdbcRepository`), `FamilyGroupSuspensionBlockersAdapter`, `MemberFamilyGroupLinkProcessor`, úprava `V001__initial_schema.sql`.
- API spec: `docs/openapi/spec/groups.yaml` (+ přegenerování bundle a frontend typů); frontend formuláře rodinné skupiny.
- Beze změny: Free/Training skupiny (zůstávají `MemberId`), `MemberGroup` (jeden typový parametr).
