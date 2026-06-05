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
| `FeeYearPublication` | vypsání úrovní pro rok | aggregate root | rok platnosti, **jedna uzávěrka voleb**, sada ID vypsaných skupin |
| `MembershipFeeGroup` | roční úroveň / skupina volby | aggregate root — vlastní membership logika | snapshot úrovně pro rok + členství (volby členů); stav `EDITABLE`/`FROZEN` |
| `MembershipPaymentRule` | pravidlo spoluúčasti | value object (uvnitř úrovně) | (typ závodu + žebříček) → procento NEBO fixní příplatek |

### Value objekty / identifikátory

| EN | CZ / význam |
|---|---|
| `MembershipFeeLevelId` | identita šablony úrovně (párování loňské→letošní volby) |
| `MembershipFeeGroupId` | identita roční úrovně |
| `EventTypeId` | (existující) reference na typ závodu v pravidle |
| `Ranking` | žebříček/série závodu — zde jen jako součást klíče pravidla |
| `Money` | (existující) částka v CZK |

### Stavy `MembershipFeeGroup`

| EN | význam |
|---|---|
| `EDITABLE` | snapshot lze editovat (před uzávěrkou voleb) |
| `FROZEN` | snapshot zmrazen (po uzávěrce voleb) |

### Doménové eventy (cross-module)

| Event | Směr | Reakce |
|---|---|---|
| `MemberMissedFeeSelectionEvent` | `membership-fees` → `events` | blokace přihlášek + auto-odhlášení (D5) |
| `MemberFeeSelectionResolvedEvent` | `membership-fees` → `events` | zrušení blokace po nouzovém přiřazení (D9) |

### Pojmy mimo agregáty

| EN | CZ / význam |
|---|---|
| surcharge | doplatek za konkrétní přihlášku (výpočet je out-of-scope) |
| voting deadline | uzávěrka voleb |
| yearly membership fee | roční členský poplatek |

### Sdílené / externí

| Pojem | Zdroj |
|---|---|
| `MemberId` | `members` — cross-module value object reference |
| `ChargePort` | `finance` — port pro zaúčtování ročního poplatku |
| `AllMembersPort` | `members` — port pro získání seznamu všech `MemberId` (včetně suspendovaných) |
| `MEMBERS:ADMIN` | autorita správce členů |

## Goals / Non-Goals

**Goals:**

- Definovat úroveň členského příspěvku (`MembershipFeeLevel`) jako šablonu: roční poplatek + sada pravidel spoluúčasti podle (typ závodu + žebříček).
- Umožnit `MEMBERS:ADMIN` vypsat sadu úrovní pro daný kalendářní rok s uzávěrkou voleb; vypsání vytvoří **zmrazený snapshot** každé úrovně.
- Realizovat přiřazení člen↔úroveň jako **vlastní typ skupiny v modulu `membership-fees`** (`MembershipFeeGroup`) s vlastní membership logikou — člen volbou vstoupí do skupiny.
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

### D2: `membership-fees` zavádí vlastní typ skupiny pro roční evidenci voleb (vlastní aggregate root, vlastní membership logika, vlastní persistence)

**Rozhodnutí (OQ1 vyřešeno):** Modul `membership-fees` zavádí **vlastní aggregate root** `MembershipFeeGroup` pro roční evidenci voleb k jedné úrovni. Agregát nese snapshot úrovně pro daný rok (pravidla, roční částka, rok platnosti, uzávěrka, stav `EDITABLE/FROZEN`) i členství (kteří členové si tuto úroveň zvolili). Owner skupiny = admin, který danou skupinu vytvořil (provenance, neřídí autorizaci).

**Hranice modulů a sdílení:**
- Agregát **nedědí** `groups.common.domain.MemberGroup` — `groups.common.domain` je interní balíček modulu `groups` bez `@NamedInterface`; dědění přes modulové hranice by porušilo Spring Modulith izolaci a `JMoleculesArchitectureTest`.
- `MembershipFeeGroup` implementuje **vlastní membership logiku** (~30 řádků: `Set<MemberId>`, `addMember` / `removeMember` / `hasMember`). Duplikace je malá a zachovává nulovou cross-module závislost.
- **Persistenci si `membership-fees` řeší vlastní** (vlastní memento + repository + tabulky). Sdílená group persistence z `groups/common/infrastructure/jdbc` se **nepoužije**.
- Modul `groups` se **nedotýká**; tabulka `user_groups` zůstává beze změny; `user-groups` spec se nemění.

**Autorizace** operací na skupině jde výhradně přes `@HasAuthority(MEMBERS_ADMIN)`, ne přes ownership skupiny.

**Navenek** typ vystupuje pouze pod `membership-fees` (API/UI = „volba členského příspěvku"), neobjevuje se v obecném výpisu skupin.

**Proč:** Čisté modulové hranice mají přednost před sdílením ~30 řádků doménové logiky. Modul `membership-fees` je plně autonomní bez závislosti na interních balíčcích `groups`.

**Historie přiřazení napříč roky** (audit trail pro účetnictví): pro každý rok existuje samostatný `MembershipFeeGroup`, takže členství v ročních úrovních = kompletní historie volby člena.

**Alternativy:**
- *Dědit `groups.common.domain.MemberGroup`* — porušuje Spring Modulith hranice; zamítnuto (2026-06-05).
- *Přidat `@NamedInterface` na `groups.common.domain`* — otevírá interní doménové třídy `groups` ostatním modulům; zamítnuto (netypická těsná vazba).
- *Přesunout `MemberGroup` do `common`* — `common` je sdílené jádro, ne místo pro groups-specific logiku; zamítnuto.
- *Pole v agregátu `Member`* — přiřazení nepatří do member agregátu; zamítnuto.

### D3: Snapshot pravidel spoluúčasti

**Rozhodnutí:** Pravidlo = `(EventTypeId, rankingShortName) → hodnota`, kde hodnota je buď **procento ze základní ceny** (0–100+ %), nebo **fixní příplatek v Kč**. Snapshot drží kompletní sadu pravidel; reference na typ závodu přes `EventTypeId` (value-object, žádný runtime coupling).

**Proč:** Kombinace (typ + žebříček) je business klíč pravidla dle zadání. Procento i fix pokrývají oba modely klubu.

**Typ `rankingShortName`:** volný `String` — bez FK na tabulku rankingů, která zatím neexistuje. `shortName` je přirozený unikátní klíč rankingu (ORIS i interní závody). Referenční integrita se doplní FK migrací až vznikne ranking tabulka (follow-up proposal). Pravidlo s nevalidním `shortName` se jen nikdy nezapáruje se závody — bezpečné selhání bez runtime chyby.

**Pozn.:** Tabulka rankingů (se záznamy pro ORIS i interní závody) je prerekvizita pro výpočet doplatku, ale nikoliv pro tento proposal — zde `rankingShortName` slouží jen jako klíč pravidla.

### D4: Editovatelnost vypsané úrovně do uzávěrky voleb

**Rozhodnutí:** Snapshot vypsané úrovně lze editovat do uzávěrky voleb (`votingDeadline`). Po uzávěrce se snapshot automaticky přepne do stavu `FROZEN` — pravidla jsou od té chvíle závazná pro všechny členy, kteří si úroveň zvolili. Stav snapshotu: `EDITABLE` → `FROZEN`. Oprava zmrazené úrovně = founded nové úrovně.

Listener na `SurchargeCalculatedEvent` se **nezavádí** — producer tohoto eventu (follow-up proposal) zatím neexistuje; mrtvý listener bez producera je zbytečný kód. Až follow-up proposal vznikne, lze zmrazení přes event doplnit.

**Proč:** Uzávěrka je přirozená a bezpečná hranice — po ní jsou pravidla závazná bez ohledu na to, zda follow-up proposal existuje. Závislost na `SurchargeCalculatedEvent` jako primárním spouštěči by znamenala, že snapshot zůstává editovatelný i po uzávěrce, dokud nedorazí první doplatek z dosud neimplementovaného proposalu.

### D5: Sankce za neprovedenou volbu přes doménový event

**Rozhodnutí:** Den po uzávěrce scheduler v `membership-fees` zjistí členy bez volby a publikuje `MemberMissedFeeSelectionEvent` pro každého z nich. Modul `events` na něj reaguje: blokace nových přihlášek + auto-odhlášení z akcí s otevřenými přihláškami. `membership-fees` **neví** o registracích na závody — drží jen hranici modulu.

**Jak scheduler zjistí členy bez volby:** Zavolá port `AllMembersPort` exponovaný modulem `members` pro seznam všech `MemberId` (včetně suspendovaných), odečte členy přítomné v některé `MembershipFeeGroup` daného roku — zbytek jsou členové bez volby.

**Suspendovaní členové se sankcionují záměrně** — reaktivovaný člen bez přiřazené úrovně by jinak mohl okamžitě registrovat závody. Sankce zajistí, že admin musí po reaktivaci provést nouzové přiřazení úrovně, než se člen může přihlašovat.

**Proč:** Respektuje pravidlo „cross-module jen přes eventy" pro výstupní komunikaci; `AllMembersPort` je standardní aplikační port (povolená hexagonální závislost). Non-Goal: žádná přímá závislost na `event-registrations`.

**Alternativy:**
- *Přímé volání služby `events`* — zamítnuto (porušuje hranice modulů).
- *`membership-fees` si udržuje vlastní evidenci členů (opt-in seznam)* — zbytečná duplikace dat z `members`; zamítnuto.
- *Jeden `VotingDeadlinePassedEvent`, `members` identifikuje své členy bez volby* — vyžaduje, aby `members` znal stav voleb; zamítnuto (porušuje hranice).
- *Vyloučit suspendované členy ze sankcionování* — reaktivovaný člen by mohl registrovat závody bez přiřazené úrovně; zamítnuto.

### D6: Generování ročního poplatku přes `@Scheduled` + `ChargePort`

**Rozhodnutí:** Cron bean v `membership-fees` se spustí den po uzávěrce voleb, projde členství ve skupinách vypsaných úrovní pro daný rok a pro každého člena zavolá `finance.ChargePort.charge(...)` s roční částkou ze snapshotu. Idempotence: záznam, že poplatek pro (člen, rok) už byl vygenerován, brání dvojímu zaúčtování.

Scheduler při zpracování `FeeYearPublication` zaznamená čas provedení operací na samotné `FeeYearPublication` jako pole `Instant deadlineProcessedAt`. Pole je `null` dokud scheduler neproběhne; nenulová hodnota slouží jako audit záznam (kdy byly eventy odeslány a poplatek zaúčtován). Zmrazení skupin a sankcionování jsou přirozeně idempotentní — opakované spuštění scheduleru (restart serveru) operace bezpečně přeskočí nebo zopakuje bez vedlejších efektů.

**Proč:** Konzistentní se vzorem `EventCompletionScheduler`; `finance` už má `ChargePort`. Uzávěrka je per vypsaný rok, ne pevné datum — scheduler vybírá vypsání, jejichž „den po uzávěrce" = dnes.

**Pozn.:** Modified capability `member-accounts` = pouze tato generovaná položka.

### D7: Jedna uzávěrka voleb per vypsaný rok (OQ2 vyřešeno)

**Rozhodnutí:** Uzávěrka voleb je **jedna pro celé vypsání roku**, společná pro všechny úrovně daného roku. Patří tedy logicky na **vypsání roku jako celek**, ne na jednotlivý `MembershipFeeGroup`.

**Modelový důsledek:** zavádí se koncept **`FeeYearPublication`** (vypsání úrovní pro rok) jako vlastník: rok platnosti + uzávěrka voleb + sada vypsaných úrovní (`MembershipFeeGroup`) pro tento rok. Volba člena směřuje na konkrétní `MembershipFeeGroup`, ale termín a zamčení voleb řídí nadřazené `FeeYearPublication`.

**Proč:** Jediný okamžik uzávěrky → jednoduchá sankční logika (D5) i generování ročního poplatku (D6) se spouští jednou pro celý rok. Per-úrovňové uzávěrky by zkomplikovaly „kdo nezvolil včas".

**Alternativy:** uzávěrka per úroveň — zamítnuto (zbytečná složitost sankcí a schedulingu).

### D8: Loňská volba jako předvyplněný default (OQ3 vyřešeno)

**Rozhodnutí:** Při volbě úrovně na rok N se členovi nabídne jeho **volba z roku N-1 jako předvyplněný default** (pokud existuje odpovídající úroveň ve vypsání roku N). Jde o UX pomůcku — člen jen potvrdí, nebo změní. Default se **neuplatní automaticky**: dokud člen aktivně nepotvrdí, považuje se za „nezvoleno" a vztahuje se na něj sankce (D5). Předvyplnění není volba.

**Modelový předpoklad:** dotaz „úroveň člena v roce N-1" — náš model ho splňuje přirozeně (členství v `MembershipFeeGroup` daného roku). Párování loňské úrovně na letošní vypsání se dělá podle identity šablony (`MembershipFeeLevel` v katalogu), ze které obě vypsané úrovně vznikly; pokud loňská úroveň letos není vypsána, default se nenabídne.

**Proč:** Většina členů zůstává rok co rok na stejné úrovni → snižuje tření a chybovost.

**Pozn.:** Aby „nepotvrzený default" nebyl tichý souhlas (a nezablokoval sankci, která je záměrná), volba musí být vždy explicitní akce člena. Default jen předvyplní formulář.

### D9: Nouzové přiřazení po sankci jen odblokuje přihlašování (OQ4 vyřešeno)

**Rozhodnutí:** Když `MEMBERS:ADMIN` po uzávěrce nouzově přiřadí úroveň členovi, který propásl volbu a byl sankcionován, **automaticky se pouze odblokují budoucí přihlášky**. Dříve zrušené (auto-odhlášené) přihlášky se **automaticky neobnovují** — případné znovupřihlášení je manuální akce.

**Proč:** Auto-obnovení je křehké — mezi odhlášením a nouzovým přiřazením se mohla zaplnit kapacita, posunout termín nebo uzavřít přihlášky závodu. Tichá reaktivace by vedla k nekonzistentním stavům. Odblokování budoucích přihlášek je bezpečné a pokrývá praktickou potřebu.

**Hranice modulu:** odblokování řeší modul `events` jako reakci na zrušení sankce — `membership-fees` po nouzovém přiřazení publikuje `MemberFeeSelectionResolvedEvent` a `events` na něj zruší blokaci. `membership-fees` nezná konkrétní přihlášky.

**Alternativy:** auto-obnovení zrušených přihlášek — zamítnuto (křehké, nekonzistentní stavy).

## Risks / Trade-offs

- **[Vlastní persistence skupiny místo sdílené `groups`]** → duplikuje část mechanismu členství a historie není v jedné tabulce s ostatními skupinami. Mitigace: vlastní `FeeGroupMembership` value object (memberId, joinedAt, AssignmentSource); per rok samostatný agregát `MembershipFeeGroup` → historie = členství v ročních úrovních. Budoucí sjednocení persistence je možné, ne nutné.
- **[Sankce auto-odhlášení je destruktivní]** → omylem nezvolená úroveň odhlásí člena ze závodů; nouzové přiřazení dle D9 jen odblokuje budoucí přihlášky, zrušené neobnovuje. Mitigace: nouzové přiřazení `MEMBERS:ADMIN`; event zpracovat až den po uzávěrce; modul `events` loguje dotčené (auto-odhlášené) přihlášky, aby šlo provést **manuální** znovupřihlášení.
- **[Idempotence ročního poplatku]** → opakovaný běh cronu nebo restart během běhu by zaúčtoval dvakrát. Mitigace: unikátní marker (člen, rok) v `membership-fees` před voláním `ChargePort`; transakční hranice per člen.
- **[`rankingShortName` bez referenční integrity]** → pravidlo s nevalidním `shortName` se tiše nezapáruje se závody. Mitigace: FK na ranking tabulku se doplní migrací až vznikne follow-up ranking proposal.

## Migration Plan

- Aditivní změna: nové tabulky modulu `membership-fees` se přidají do **V001** (dle projektové konvence — domain DDL do V001). Tabulky: katalog šablon úrovní, pravidla spoluúčasti, `FeeYearPublication`, `MembershipFeeGroup` se snapshotem a členstvím, idempotency marker ročního poplatku `(member_id, year)`. Žádná změna tabulky `user_groups`, žádná migrace existujících dat.
- Nasazení po částech: (1) katalog šablon úrovní + doména/persistence, (2) vypsání roku + vypsané úrovně se snapshotem, (3) volba člena (členství), (4) scheduler ročního poplatku, (5) sankční event + konzument v `events`, (6) frontend.
- Rollback: žádná migrace stávajících dat → rollback = odebrání nového kódu a tabulek; moduly `groups` a `finance` nedotčeny.

## Open Questions

_Všechny otevřené otázky (OQ1–OQ4) byly vyřešeny a promítnuty do rozhodnutí D2, D7, D8, D9._
