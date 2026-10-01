## Context

Úprava člena (`PATCH /api/members/{id}`) je povolena uživateli s MEMBERS:MANAGE nebo „vlastníkovi“ záznamu (`x-klabis-owner-visible`, porovnání userId z cesty s přihlášeným uživatelem). Mechanismus nezná věk, proto nezletilý dnes smí upravovat sám sebe. Affordance „Upravit“ se přidává v `MemberSelfLinkSupport` přes `klabisAfford(updateMember)`.

## Goals / Non-Goals

**Goals:**
- Nezletilý bez MEMBERS:MANAGE nevidí akci „Upravit“ na vlastním detailu a backend jeho úpravu odmítne.
- Dospělí a administrátoři beze změny.

**Non-Goals:**
- Oprávnění zákonného zástupce upravovat dítě (řeší se až s úrovní oprávnění přes skupiny).
- Změna dalších akcí nad vlastním profilem nezletilého (např. „Založit účet“ je už jen pro MEMBERS:MANAGE).

## Decisions

- **D1: `x-klabis-owner-visible` na `updateMember` zůstává.** Dospělý si dál upravuje svůj profil přes stejný mechanismus. Pravidlo „nezletilý ne“ se přidává jako samostatná doménová kontrola, ne jako rozšíření generického mechanismu o věk.
- **D2: Affordance.** `MemberSelfLinkSupport` nepřidá `updateMember`, pokud je člen nezletilý a přihlášený uživatel nemá MEMBERS:MANAGE. Detail tak neobsahuje šablonu úpravy a UI akci nezobrazí (HAL-FORMS řízené UI, bez změny frontendu).
- **D3: Odmítnutí na backendu.** `updateMember` v `MemberController` (nebo aplikační službě) před úpravou ověří: pokud je cílový člen nezletilý a volající není admin (bez MEMBERS:MANAGE), vyhodí `InsufficientAuthorityException` (403). Skutečné datum „je nezletilý“ se počítá k dnešku, proto po 18. narozeninách vlastní úprava opět funguje bez zásahu.
- **D4: Rozhodnutí podle věku, ne podle stavu účtu.** Pravidlo platí pro každého nezletilého, i pro toho, kdo má aktivní účet.

## Risks / Trade-offs

- Dvě místa (affordance a kontrola při úpravě) musí používat stejné pravidlo; sdílí se jedna metoda, aby se nerozešla.
- Nezletilý nemůže opravit ani vlastní kontakt. To je záměr požadavku, admin to udělá za něj.
