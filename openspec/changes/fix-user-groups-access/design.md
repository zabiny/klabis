## Context

Skupiny (tréninková, rodinná, volná) mají detail stránku přístupnou správcům (GROUPS:TRAINING, MEMBERS:MANAGE) a vlastníkům skupin. Běžní členové skupin však detail zobrazit nemohou — backend vrací HTTP 403, protože autorizační pravidla nezahrnují případ "člen skupiny".

Zároveň navigační položka "Administrace → Rodinné skupiny" je zobrazena všem přihlášeným uživatelům bez ohledu na roli — přičemž by měla být viditelná pouze uživatelům s MEMBERS:MANAGE.

## Goals / Non-Goals

**Goals:**
- Členové skupiny (všechny tři typy) mohou číst detail skupiny, jejíž jsou členy
- Navigační položka "Rodinné skupiny" v sekci Administrace je podmíněna rolí MEMBERS:MANAGE

**Non-Goals:**
- Členové skupiny nemohou spravovat skupinu (přidávat/odebírat členy, mazat skupinu) — správa zůstává vyhrazena správcům a vlastníkům
- Změna autorizačních pravidel pro jiné operace než čtení detailu

## Decisions

### Backend: rozšíření autorizace pro čtení detailu skupiny

Aktuálně je přístup k detailu skupiny povolen pouze na základě role (GROUPS:TRAINING / MEMBERS:MANAGE) nebo vlastnictví (owner). Je potřeba přidat podmínku: uživatel je členem dané skupiny.

Rozhodnutí: přidat kontrolu členství do autorizační vrstvy (Spring Security / `@PreAuthorize` nebo dedikovaná autorizační komponenta) pro GET endpoint každého typu skupiny.

Alternativa — veřejný přístup ke všem skupinám pro přihlášené uživatele: zamítnuto, protože by odhalilo seznam členů skupin, ke kterým uživatel nepatří.

### Frontend: HAL-link řízené zobrazení navigační položky

Frontend zobrazuje navigační položky podmíněně na základě přítomnosti HAL linků vrácených backendem (RootController). Položka "Rodinné skupiny" by měla být vrácena jako HAL link pouze uživatelům s MEMBERS:MANAGE.

Rozhodnutí: podmínit HAL link na rodinné skupiny v RootController rolí MEMBERS:MANAGE (stejný vzor jako ostatní admin položky).

## Risks / Trade-offs

- [Risk] Přidání membership check do autorizace zvyšuje počet DB dotazů při každém GET skupiny → Mitigation: dotaz na členství je jednoduchý lookup podle userId + groupId, zanedbatelný dopad na výkon
- [Risk] Různé typy skupin mají různé datové modely členství → Mitigation: ověřit pro každý typ skupiny zvlášť (TrainingGroup, FamilyGroup, FreeGroup)
