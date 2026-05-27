## Context

Modul `finance` modeluje historii transakcí na účtu člena. Doménová entita `Transaction` už dnes nese `recordedBy: UserId` (kdo transakci zaznamenal). Tento údaj se persistuje (`TransactionMemento.recordedByUserId`) a v HAL+FORMS API se serializuje jako ploché pole `recordedBy: UUID` v `TransactionResource`.

Frontend stránka `MemberAccountManagePage` zobrazuje seznam transakcí (komponenta `TransactionsTable` v `FinancesPage.tsx`), ale UUID pořizovatele ignoruje — uživatel tedy nemá v UI šanci zjistit, kdo transakci pořídil. Stránka má též nadpis "Účet člena", který je z pohledu uživatele méně srozumitelný než "Finance" (sjednoceno s názvem v hlavním menu, viz spec `member-accounts`).

## Goals / Non-Goals

**Goals:**
- Sjednotit H1 nadpis stránky finančního účtu na "Finance".
- V seznamu transakcí zobrazit jméno uživatele, který transakci zaznamenal.
- API návrh držet v duchu HATEOAS — pořizovatele exponovat jako HAL link, ne jako embedded duplikát ani plochý string.
- Znovupoužít existující frontend komponentu pro vykreslení jména člena.

**Non-Goals:**
- Měnit doménový model `Transaction` (recordedBy už existuje).
- Měnit breadcrumb, položku v hlavním menu, ani title v prohlížeči — pouze H1.
- Filtrovat/řadit transakce podle pořizovatele.
- Řešit situaci "smazaný uživatel" jinak než zobrazením pomlčky ("—").
- Z jména pořizovatele dělat klikatelný odkaz na profil člena.

## Decisions

### D1: Pořizovatel je vystaven jako HAL link, ne embedded a ne ploché jméno

`TransactionResource` v `_links` doplní `recordedBy` s href na `/api/members/{id}`. Stávající ploché pole `recordedBy: UUID` zůstává — slouží jako stabilní identifikátor pro klienta a backwards-compat se stávajícími klienty/testy.

**Proč ne embedded resource?** Embedded by znamenalo, že server při každém čtení historie transakcí musí načíst i členy. Pro stránku s desítkami transakcí je to N+1 dotazů navíc. HAL link je laciný a nechává volbu na klientovi, kdy (a zda) jméno fetchne.

**Proč ne `recordedByName: String`?** Bylo by jednodušší implementačně, ale rozbíjí HATEOAS princip — klient by neměl konstruovat URL k profilu pořizovatele, server má vést navigaci.

**Alternativy:**
- Embedded full member resource → zamítnuto kvůli N+1.
- Jen ploché jméno → zamítnuto kvůli HATEOAS konzistenci napříč API.

### D2: Frontend si jméno dotahuje paralelně přes `useQuery` per unikátní link

V `TransactionsTable` se vyextrahují unikátní `recordedBy.href` přes všechny transakce na stránce a pro každý se spustí `useQuery` (cache klíč = href). Díky React Query cachi se opakující se pořizovatelé fetchnou jen jednou. Komponenta zobrazení jména (`MemberName`) už tento pattern v projektu používá — znovupoužijeme ji.

**Proč ne jeden batch endpoint?** Komplikovalo by API pro marginální výkon na stránce s ~20 transakcemi.

### D3: Fallback pro chybějící/neznámého pořizovatele → "—"

Pokud `_links.recordedBy` chybí (legacy data) nebo fetch jména selže 404 (smazaný člen, nedohledatelný uživatel), buňka tabulky zobrazí pomlčku. Žádný "Systém" label — projekt nepoužívá systémové importy.

### D4: Změna titulku je pouze textová; bez i18n klíče navíc

Aktuální projekt centralizuje labely v `src/localization/labels.ts`. Existující klíč pro nadpis stránky se přepíše z "Účet člena" na "Finance". Pokud klíč neexistuje a nadpis je v JSX literál, refaktor na použití `labels.ts` je v rámci tohoto change vítaný, ale ne povinný.

## Risks / Trade-offs

- **Riziko:** Per-transaction fetch členů může na stránce s mnoha unikátními pořizovateli (např. střídá se více finance managerů) zatěžovat backend.
  → Mitigace: React Query cache deduplikuje shodné href; pokud se v praxi ukáže problém, lze do `member-accounts` API přidat embedded `recordedBy` jako optimalizaci pozdější změnou.

- **Riziko:** Backend musí pro každý člen ověřit autorizaci čtení (viditelnost profilu) — pro nezvládnutý 403 by se v UI ukázala pomlčka, což působí jako bug.
  → Mitigace: viditelnost jména pořizovatele transakce je odvozená od práva vidět transakci. Pokud uživatel transakci vidí, smí vidět i jméno pořizovatele. Endpoint `/api/members/{id}` musí pro tento případ vrátit minimálně jméno; pokud to dnes neumí, je to scope budoucí změny (zatím akceptujeme "—").
