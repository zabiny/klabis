## Context

Klub dnes rozlišuje, kolik který člen doplácí za startovné, ručně mimo systém. Tato logika závisí na „úrovni" člena (mládež, dospělý, závodník, podporovaný reprezentant). Současně nejsou v systému evidovány roční členské poplatky. Cílem changu je zavést **doménu definice členských příspěvků** — úrovně, jejich roční poplatek a sadu pravidel spoluúčasti — a mechanismus, jak si člen úroveň pro daný rok zvolí.

Výpočet konkrétního doplatku za přihlášku na závod je **mimo scope** (závisí na rankingu závodu a ceně startovného z ORIS — řešeno samostatným proposalem). Tento design tedy připravuje doménu tak, aby na ni navazující proposal mohl výpočet jen „dosednout".

**Stávající stav kódu** (relevantní moduly Spring Modulith monolitu):

- `groups` — 3 typy skupin (`traininggroup`, `familygroup`, `freegroup`) sdílejících jednu tabulku `user_groups` s type discriminatorem; persistence přes polymorfní `GroupMemento` + `GroupJdbcRepository`.
- `finance` — agregát `MemberAccount` s append-only ledgerem `Transaction`; aplikační porty `DepositPort` / `ChargePort`; účet vzniká listenerem na `MemberCreatedEvent`.
- `events` — agregát `EventType` (NE enum), referencovaný přes value-object `EventTypeId`.
- `members` — referenční agregát; vzor `register/reconstruct`, memento persistence, HAL+FORMS controller.
- Časově spouštěná logika: `@Scheduled` cron bean (vzor `EventCompletionScheduler`).
- Cross-module komunikace **výhradně přes doménové eventy** (`@ApplicationModuleListener`, Spring Modulith outbox); value-object reference (`MemberId`, `EventTypeId`) napříč moduly povoleny.

**Stakeholdeři:** správce členů (`MEMBERS:ADMIN`) definuje a vypisuje úrovně; člen volí svou úroveň; modul `finance` přijímá generovaný roční poplatek.

## Glosář (ubikvitní jazyk)

Závazné názvy pro implementaci, specifikaci i UI labely. Specs jsou psané anglicky; UI je v češtině.

### Agregáty a entity (modul `membership-fees`)

| EN (kód/spec) | CZ (UI / popis) | Role | Drží |
|---|---|---|---|
| `MembershipFeeLevel` | úroveň členského příspěvku | aggregate root — katalog/šablona | název, roční poplatek, sada pravidel; editovatelný |
| `FeeYearPublication` | vypsání úrovní pro rok | aggregate root | rok platnosti, **jedna uzávěrka voleb**, sada vypsaných úrovní |
| `YearlyMembershipFeeGroup` | roční úroveň / skupina volby | aggregate root — vlastní typ skupiny | snapshot úrovně pro rok + členství (volby členů); stav `EDITABLE`/`FROZEN` |
| `MembershipPaymentRule` | pravidlo spoluúčasti | value object (uvnitř úrovně) | (typ závodu + žebříček) → procento NEBO fixní příplatek |

### Value objekty / identifikátory

| EN | CZ / význam |
|---|---|
| `MembershipFeeLevelId` | identita šablony úrovně (párování loňské→letošní volby) |
| `YearlyMembershipFeeGroupId` | identita roční úrovně |
| `EventTypeId` | (existující) reference na typ závodu v pravidle |
| `Ranking` | žebříček/série závodu — zde jen jako součást klíče pravidla |
| `Money` | (existující) částka v CZK |

### Stavy `YearlyMembershipFeeGroup`

| EN | význam |
|---|---|
| `EDITABLE` | snapshot lze editovat (před prvním doplatkem) |
| `FROZEN` | snapshot zmrazen (po prvním dopočítaném doplatku) |

### Doménové eventy (cross-module)

| Event | Směr | Reakce |
|---|---|---|
| `MemberMissedFeeSelectionEvent` | `membership-fees` → `events` | blokace přihlášek + auto-odhlášení (D5) |
| `MemberFeeSelectionResolvedEvent` | `membership-fees` → `events` | zrušení blokace po nouzovém přiřazení (D9) |
| `SurchargeCalculatedEvent` | follow-up `events` → `membership-fees` | `EDITABLE`→`FROZEN` (D4); přichází z follow-up proposalu |

### Pojmy mimo agregáty

| EN | CZ / význam |
|---|---|
| surcharge | doplatek za konkrétní přihlášku (výpočet je out-of-scope) |
| voting deadline | uzávěrka voleb |
| yearly membership fee | roční členský poplatek |

### Sdílené / externí

| Pojem | Zdroj |
|---|---|
| `UserGroup` | `common.usergroup` — sdílený building block pro členství |
| `ChargePort` | `finance` — port pro zaúčtování ročního poplatku |
| `MEMBERS:ADMIN` | autorita správce členů |

## Goals / Non-Goals

**Goals:**

- Definovat úroveň členského příspěvku (`MembershipFeeLevel`) jako šablonu: roční poplatek + sada pravidel spoluúčasti podle (typ závodu + žebříček).
- Umožnit `MEMBERS:ADMIN` vypsat sadu úrovní pro daný kalendářní rok s uzávěrkou voleb; vypsání vytvoří **zmrazený snapshot** každé úrovně.
- Realizovat přiřazení člen↔úroveň jako **vlastní typ skupiny v modulu `membership-fees`** (`YearlyMembershipFeeGroup`) sdílející building block `common.usergroup` — člen volbou vstoupí do skupiny.
- Umožnit členovi zvolit a měnit úroveň do uzávěrky; po uzávěrce zamknout.
- Umožnit `MEMBERS:ADMIN` nouzové přiřazení/změnu i po uzávěrce.
- Sankcionovat neprovedenou volbu (blokace přihlášek + auto-odhlášení) přes doménový event, který si zpracuje modul `events`.
- Vygenerovat roční členský poplatek do účtu člena den po uzávěrce voleb.
- Vystavit HAL+FORMS endpointy pro administraci úrovní, vypsání, volbu člena a nouzové přiřazení.

**Non-Goals:**

- Výpočet doplatku za konkrétní přihlášku na závod (samostatný proposal).
- Atribut `ranking` na závodu a import ceny startovného z ORIS (předpoklad, samostatný proposal).
- Promítnutí doplatku do `member-accounts` při přihlášce.
- Refaktoring stávajících 3 typů skupin.

## Decisions

### D1: Členská úroveň jako šablona vs. vypsaná instance pro rok

**Rozhodnutí:** Oddělit **šablonu úrovně** (`MembershipFeeLevel` — editovatelný katalog) od **vypsané instance pro rok** (snapshot uložený ve skupině). Vypsání pro rok zkopíruje aktuální stav šablony do snapshotu; pozdější editace šablony se do již vypsaného roku nepromítne.

**Proč:** Účetní integrita — jakmile člen zvolí úroveň a začnou se počítat doplatky, podmínky musí zůstat neměnné. Šablona slouží jako pracovní katalog pro přípravu dalšího roku.

**Alternativy:**
- *Jen instance pro rok bez šablony* — admin by každý rok zadával vše znovu; zamítnuto (opakovaná práce, chyby).
- *Jen šablona s verzováním na úrovni řádků* — složitější model historie; zamítnuto ve prospěch celkového snapshotu (KISS).

### D2: `membership-fees` zavádí vlastní typ skupiny pro roční evidenci voleb (vlastní agregát + vlastní persistence, sdílí jen `common.usergroup`)

**Rozhodnutí (OQ1 vyřešeno):** Stejně jako submoduly `traininggroup` / `familygroup` / `freegroup` definují každý svůj agregát skupiny, zavede modul `membership-fees` **vlastní typ skupiny** pro **roční evidenci voleb k jedné úrovni** — agregát `YearlyMembershipFeeGroup`. Ten nese snapshot úrovně pro daný rok (pravidla, roční částka, rok platnosti, uzávěrka, stav `EDITABLE/FROZEN`) **i členství** (kteří členové si tuto úroveň zvolili).

**Hranice modulů a sdílení:**
- Agregát používá sdílený doménový building block **`com.klabis.common.usergroup.UserGroup`** (jméno, vlastníci, členové, `addMember` / `removeMember` / `hasMember`) — stejně jako ho používají 3 existující typy skupin. Tím získá konzistentní sémantiku členství.
- **Persistenci si `membership-fees` řeší vlastní** (vlastní memento + repository + tabulky). Sdílená group persistence z `groups/common/infrastructure/jdbc` (`GroupMemento`, `GroupJdbcRepository`) se **nepoužije** — je privátní pro modul `groups` a její mapping je hard-coded na 3 konkrétní typy; její otevření by znamenalo refaktoring modulu `groups`, který je mimo scope.
- Modul `groups` se tedy **nedotýká**; tabulka `user_groups` zůstává beze změny; `user-groups` spec se nemění.

**Navenek** typ vystupuje pouze pod `membership-fees` (API/UI = „volba členského příspěvku"), neobjevuje se v obecném výpisu skupin.

**Proč:** Respektuje vzor „každý typ skupiny = vlastní agregát ve svém submodulu", drží celou doménu příspěvků (bohatý snapshot, sazby, lifecycle) na jednom místě a nezatěžuje modul `groups`. Sdílením `common.usergroup` se neduplikuje doménová logika členství, jen persistence.

**Historie přiřazení napříč roky** (audit trail pro účetnictví): pro každý rok existuje samostatný `YearlyMembershipFeeGroup`, takže členství v ročních úrovních = kompletní historie volby člena.

**Dopad na dřívější rozhodnutí:** Upřesňuje dřívější „přiřazení = nový typ user-group" — typ skupiny ano, ale **ve vlastním modulu s vlastní persistencí**, ne fyzicky sdílený s `groups`.

**Trade-off:** persistence členství se částečně duplikuje (vlastní tabulky místo sdílené `user_groups`) a historie není v jedné tabulce s ostatními skupinami. Akceptováno výměnou za čisté hranice modulů a menší rozsah. Případné sjednocení persistence (refaktoring `groups/common` na obecnou group infra) je možný budoucí krok.

**Alternativy:**
- *Sdílet `groups/common` persistenci jako 4. typ* — vyžadovalo by refaktoring modulu `groups` (named interface + generický mapping snapshotu); zamítnuto pro rozsah.
- *Vše ve `membership-fees` bez `common.usergroup`* — duplikovalo by i doménovou logiku členství; zamítnuto.
- *Pole v agregátu `Member`* — zamítnuto dříve (přiřazení nepatří do member agregátu).

### D3: Snapshot pravidel spoluúčasti

**Rozhodnutí:** Pravidlo = `(EventTypeId, ranking) → hodnota`, kde hodnota je buď **procento ze základní ceny** (0–100+ %), nebo **fixní příplatek v Kč**. Snapshot drží kompletní sadu pravidel; reference na typ závodu přes `EventTypeId` (value-object, žádný runtime coupling).

**Proč:** Kombinace (typ + žebříček) je business klíč pravidla dle zadání. Procento i fix pokrývají oba modely klubu.

**Pozn.:** `ranking` jako doménový pojem zde existuje pouze jako součást business klíče pravidla. Samotný atribut na závodu doplní follow-up proposal; tento change neukládá ranking k závodu.

### D4: Editovatelnost vypsané úrovně do prvního dopočítaného doplatku

**Rozhodnutí:** Snapshot vypsané úrovně lze editovat, dokud na jeho základě nebyl spočítán první doplatek. Stav snapshotu: `EDITABLE` → `FROZEN`. Přechod do `FROZEN` spustí příchod prvního výpočtu doplatku (`SurchargeCalculatedEvent` z follow-up proposalu). Do té doby (a dokud follow-up neexistuje) zůstává `EDITABLE`; oprava zmrazené úrovně = založení nové úrovně.

**Proč:** Umožní opravit chybu v sazbě před prvním reálným dopadem, pak garantuje neměnnost.

### D5: Sankce za neprovedenou volbu přes doménový event

**Rozhodnutí:** Den po uzávěrce `membership-fees` zjistí členy bez volby a publikuje `MemberMissedFeeSelectionEvent`. Modul `events` na něj reaguje: blokace nových přihlášek + auto-odhlášení z akcí s otevřenými přihláškami. `membership-fees` **neví** o registracích na závody — drží jen hranici modulu.

**Proč:** Respektuje pravidlo „cross-module jen přes eventy" a Non-Goal (žádná závislost na event-registrations).

**Alternativy:** přímé volání služby `events` — zamítnuto (porušuje hranice modulů).

### D6: Generování ročního poplatku přes `@Scheduled` + `ChargePort`

**Rozhodnutí:** Cron bean v `membership-fees` se spustí den po uzávěrce voleb, projde členství ve skupinách vypsaných úrovní pro daný rok a pro každého člena zavolá `finance.ChargePort.charge(...)` s roční částkou ze snapshotu. Idempotence: záznam, že poplatek pro (člen, rok) už byl vygenerován, brání dvojímu zaúčtování.

**Proč:** Konzistentní se vzorem `EventCompletionScheduler`; `finance` už má `ChargePort`. Uzávěrka je per vypsaný rok, ne pevné datum — scheduler vybírá vypsání, jejichž „den po uzávěrce" = dnes.

**Pozn.:** Modified capability `member-accounts` = pouze tato generovaná položka.

### D7: Jedna uzávěrka voleb per vypsaný rok (OQ2 vyřešeno)

**Rozhodnutí:** Uzávěrka voleb je **jedna pro celé vypsání roku**, společná pro všechny úrovně daného roku. Patří tedy logicky na **vypsání roku jako celek**, ne na jednotlivý `YearlyMembershipFeeGroup`.

**Modelový důsledek:** zavádí se koncept **`FeeYearPublication`** (vypsání úrovní pro rok) jako vlastník: rok platnosti + uzávěrka voleb + sada vypsaných úrovní (`YearlyMembershipFeeGroup`) pro tento rok. Volba člena směřuje na konkrétní `YearlyMembershipFeeGroup`, ale termín a zamčení voleb řídí nadřazené `FeeYearPublication`.

**Proč:** Jediný okamžik uzávěrky → jednoduchá sankční logika (D5) i generování ročního poplatku (D6) se spouští jednou pro celý rok. Per-úrovňové uzávěrky by zkomplikovaly „kdo nezvolil včas".

**Alternativy:** uzávěrka per úroveň — zamítnuto (zbytečná složitost sankcí a schedulingu).

### D8: Loňská volba jako předvyplněný default (OQ3 vyřešeno)

**Rozhodnutí:** Při volbě úrovně na rok N se členovi nabídne jeho **volba z roku N-1 jako předvyplněný default** (pokud existuje odpovídající úroveň ve vypsání roku N). Jde o UX pomůcku — člen jen potvrdí, nebo změní. Default se **neuplatní automaticky**: dokud člen aktivně nepotvrdí, považuje se za „nezvoleno" a vztahuje se na něj sankce (D5). Předvyplnění není volba.

**Modelový předpoklad:** dotaz „úroveň člena v roce N-1" — náš model ho splňuje přirozeně (členství v `YearlyMembershipFeeGroup` daného roku). Párování loňské úrovně na letošní vypsání se dělá podle identity šablony (`MembershipFeeLevel` v katalogu), ze které obě vypsané úrovně vznikly; pokud loňská úroveň letos není vypsána, default se nenabídne.

**Proč:** Většina členů zůstává rok co rok na stejné úrovni → snižuje tření a chybovost.

**Pozn.:** Aby „nepotvrzený default" nebyl tichý souhlas (a nezablokoval sankci, která je záměrná), volba musí být vždy explicitní akce člena. Default jen předvyplní formulář.

### D9: Nouzové přiřazení po sankci jen odblokuje přihlašování (OQ4 vyřešeno)

**Rozhodnutí:** Když `MEMBERS:ADMIN` po uzávěrce nouzově přiřadí úroveň členovi, který propásl volbu a byl sankcionován, **automaticky se pouze odblokují budoucí přihlášky**. Dříve zrušené (auto-odhlášené) přihlášky se **automaticky neobnovují** — případné znovupřihlášení je manuální akce.

**Proč:** Auto-obnovení je křehké — mezi odhlášením a nouzovým přiřazením se mohla zaplnit kapacita, posunout termín nebo uzavřít přihlášky závodu. Tichá reaktivace by vedla k nekonzistentním stavům. Odblokování budoucích přihlášek je bezpečné a pokrývá praktickou potřebu.

**Hranice modulu:** odblokování řeší modul `events` jako reakci na zrušení sankce — `membership-fees` po nouzovém přiřazení publikuje `MemberFeeSelectionResolvedEvent` a `events` na něj zruší blokaci. `membership-fees` nezná konkrétní přihlášky.

**Alternativy:** auto-obnovení zrušených přihlášek — zamítnuto (křehké, nekonzistentní stavy).

## Risks / Trade-offs

- **[Vlastní persistence skupiny místo sdílené `groups`]** → duplikuje část mechanismu členství a historie není v jedné tabulce s ostatními skupinami. Mitigace: sdílet doménový building block `common.usergroup` (žádná duplikace doménové logiky); per rok samostatný agregát `YearlyMembershipFeeGroup` → historie = členství v ročních úrovních. Budoucí sjednocení persistence je možné, ne nutné.
- **[Sankce auto-odhlášení je destruktivní]** → omylem nezvolená úroveň odhlásí člena ze závodů; nouzové přiřazení dle D9 jen odblokuje budoucí přihlášky, zrušené neobnovuje. Mitigace: nouzové přiřazení `MEMBERS:ADMIN`; event zpracovat až den po uzávěrce; modul `events` loguje dotčené (auto-odhlášené) přihlášky, aby šlo provést **manuální** znovupřihlášení.
- **[Idempotence ročního poplatku]** → opakovaný běh cronu nebo restart během běhu by zaúčtoval dvakrát. Mitigace: unikátní marker (člen, rok) před voláním `ChargePort`; transakční hranice per člen.
- **[Závislost na neexistujícím follow-up]** → přechod snapshotu `EDITABLE→FROZEN` závisí na eventu, který zatím nikdo nepublikuje. Mitigace: design počítá s tím, že do dodání follow-up zůstávají snapshoty `EDITABLE`; žádná funkční regrese.

## Migration Plan

- Aditivní změna: nové vlastní tabulky modulu `membership-fees` (katalog šablon úrovní; vypsání roku `FeeYearPublication` s uzávěrkou; vypsané úrovně `YearlyMembershipFeeGroup` se snapshotem pravidel a členstvím). Žádná změna tabulky `user_groups`, žádná migrace existujících dat.
- Nasazení po částech: (1) katalog šablon úrovní + doména/persistence, (2) vypsání roku + vypsané úrovně se snapshotem, (3) volba člena (členství), (4) scheduler ročního poplatku, (5) sankční event + konzument v `events`, (6) frontend.
- Rollback: žádná migrace stávajících dat → rollback = odebrání nového kódu a tabulek; moduly `groups` a `finance` nedotčeny.

## Open Questions

_Všechny otevřené otázky (OQ1–OQ4) byly vyřešeny a promítnuty do rozhodnutí D2, D7, D8, D9._
