> **Související GitHub issue:** [#274 Úrovně členských příspěvků a spoluúčast na startovném](https://github.com/zabiny/klabis/issues/274)
>
> **Závislosti:** vyžaduje doplnění atributu `ranking` na závod a ceny startovného ze synchronizace ORIS — řešeno samostatným follow-up proposalem `events`/ORIS.

## Why

Klub potřebuje rozlišit, jakou částku za závody členové doplácejí z vlastní kapsy — různí členové (mládež, dospělí, závodníci na ranking, podporovaní reprezentanti) mají historicky různé podmínky. Dnes klub tuto logiku řeší ručně mimo systém, což blokuje automatické vyúčtování a vede k chybám. Současně nejsou v systému evidovány ani roční členské příspěvky.

Tento change definuje samotnou **doménu členských příspěvků**: úrovně, pravidla, vypsání pro daný rok a volbu člena. Výpočet doplatku za konkrétní přihlášku na závod je řešen v navazujícím proposalu.

## What Changes

- Zavést koncept **úrovně členského příspěvku** (membership fee level) jako konfigurovatelnou entitu (např. "Základ", "Mládež", "Reprezentace")
- Každá úroveň definuje:
  - **Roční členský poplatek** (částka v Kč za kalendářní rok)
  - **Sadu pravidel pro spoluúčast na startovném** — každé pravidlo říká, kolik člen za daný typ závodu doplácí
- Pravidla pro spoluúčast jsou rozlišena podle **typu závodu** (event-type) a **žebříčku/série** závodu a mohou mít formu:
  - **Procento** ze základní ceny závodu (např. 50 % oblastní, 100 % celostátní)
  - **Fixní příplatek** v Kč
- **Vypsání úrovní pro nadcházející rok**: admin (`MEMBERS:ADMIN`) vypíše sadu úrovní platných pro daný kalendářní rok a stanoví uzávěrku voleb. Vypsáním vznikne pro každou úroveň vypsaná úroveň (`YearlyMembershipFeeGroup` — vlastní typ skupiny modulu, viz Impact) obsahující kompletní snapshot úrovně.
- **Volba úrovně členem**: do uzávěrky si každý člen sám zvolí svou úroveň pro nadcházející rok (= stane se členem odpovídající vypsané úrovně). Volbu může do uzávěrky libovolně měnit. Po uzávěrce je volba zamknuta.
- **Nouzové přiřazení / změna úrovně** může provést správce členů (`MEMBERS:ADMIN`) i po uzávěrce.
- **Sankce za neprovedenou volbu**: pokud se člen do uzávěrky nepřihlásí k úrovni, nesmí se přihlašovat na nové závody a je automaticky odhlášen ze všech závodů s dosud otevřenými přihláškami.
- **Editace pravidel po vypsání úrovně**: admin smí upravovat pravidla a sazby vypsané úrovně, dokud nebyl spočítán první doplatek na základě této úrovně; poté je snapshot zmrazen (oprava = nová úroveň).
- **Roční členský poplatek se promítá do účtu člena** automaticky následující den po uzávěrce voleb (na základě aktuálního členství v Membership Fee skupině).
- **Administrace** — nová sekce v Administraci pro definici úrovní a pravidel, vypsání úrovní pro rok a sledování voleb členů.
- **UI pro člena** — stránka pro volbu úrovně na nadcházející rok; widget na profilu člena s aktuální zvolenou úrovní.

## Capabilities

### New Capabilities
- `membership-fees`: definice úrovní členských příspěvků pro daný kalendářní rok (roční poplatek + pravidla spoluúčasti na startovném podle typu závodu a žebříčku), vypsání úrovní pro nadcházející rok, volba úrovně členem, sankce za neprovedenou volbu, generování ročního členského do účtu člena.

### Modified Capabilities
- `member-accounts`: roční členský poplatek se promítá do účtu člena jako automaticky generovaná položka po uzávěrce voleb.

## Impact

- **Nová doména:** modul `membership-fees` s vlastním agregátem **`YearlyMembershipFeeGroup`** (vypsaná úroveň pro rok).
- **Přiřazení člen ↔ úroveň** je realizováno jako **vlastní typ skupiny v modulu `membership-fees`** — analogicky jako traininggroup/familygroup/freegroup definují svůj agregát. Agregát drží snapshot úrovně pro daný rok i členství (volbu člena). Sdílí pouze doménový building block `common.usergroup`; **vlastní persistenci** (modul `groups` ani `user-groups` spec se nemění). Navenek vystupuje jen pod `membership-fees`, ne v obecném výpisu skupin. Detail viz design.md (D2).
- **Závislost:** `event-types` (pravidlo referencuje typ závodu); `member-accounts` přijímá generované položky.
- **Závislost na follow-up proposalu:** atribut `ranking` na závodu a synchronizace ceny startovného z ORIS — tyto změny jsou předpokladem pro výpočet doplatku, ale ten je out-of-scope tohoto changu.
- **Audit trail:** historie přiřazení úrovní členům (členství v ročních úrovních napříč roky) pro účetní účely.
- **API:** nové HAL+FORMS endpointy pro správu úrovní, vypsání úrovní pro rok, volbu úrovně členem a nouzové přiřazení správcem.
- **Frontend:** nová sekce v Administraci pro definici úrovní a vypsání; UI pro člena pro volbu úrovně na další rok; widget na profilu člena s aktuální úrovní.

## Out of Scope (řešeno v navazujících proposalech)

- Výpočet doplatku za konkrétní přihlášku na závod podle pravidla úrovně (vyžaduje znát ranking závodu a cenu startovného)
- Promítnutí doplatku do `member-accounts` při přihlášce na závod
- Automatický výpočet doplatku při importu přihlášek z ORIS
- Doplnění atributu `ranking` na závod + import ceny startovného z ORIS (samostatný proposal — předpoklad pro výše uvedené)
