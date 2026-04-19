# Implementation status as of 2026-04-19 05:45 CEST

Hodnocení je odvozeno výhradně z OpenSpec specifikací v `openspec/specs/` (archiv ignorován).
Issues s tématy, která ve specifikacích vůbec nefigurují (Finance, Web/CMS, CUS/ORIS integrace, Notifications, doprava, ubytování), jsou označeny jako **TODO**, protože neexistuje žádná implementovaná spec.

---

## Skupiny: Omezení ukončení členství vedoucím skupiny
ISSUE ID: #269
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `user-groups` obsahuje requirement "Warning on Last Owner Deactivation" a spec `members` scénáře "Suspension blocked when member is last owner of a training/family/free group" — při suspend člena, který je posledním vlastníkem skupiny, systém blokuje akci a vyžaduje určení nástupce.

---

## CUS: Odstranění neaktivních členů z exportu
ISSUE ID: #268
Milestone: core
Implementation status: **TODO**

---

## ORIS: Označení neaktivního člena v ORIS
ISSUE ID: #267
Milestone: core
Implementation status: **TODO**

---

## Finance: Kontrola dluhů před ukončením členství
ISSUE ID: #266
Milestone: core
Implementation status: **TODO**

---

## chci mít editovatelnou informaci u clena jaky ma typ clenskeho prispevku
ISSUE ID: #126
Milestone: core
Implementation status: **TODO**

---

## chci vedet kam jsem prihlasen
ISSUE ID: #88
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Spec `event-registrations` definuje "View Own Registration" (detail vlastní přihlášky k akci). Chybí však agregovaný přehled všech akcí, na které je člen přihlášen napříč systémem.

---

## Chci se prihlasit do kategorie mimo soutez (mimo vekovou kategorii)
ISSUE ID: #87
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Spec `event-registrations` podporuje volbu kategorie při registraci z libovolné vypsané kategorie akce. Speciální příznak "mimo soutěž" ale specifikován není.

---

## chci se zapsat na dopravu na akci
ISSUE ID: #86
Milestone: core
Implementation status: **TODO**

---

## chci se zapsat na ubytovani na akci
ISSUE ID: #85
Milestone: core
Implementation status: **TODO**

---

## Chci mít k dispozici seznam příhlášených na společnou dopravu
ISSUE ID: #84
Milestone: core
Implementation status: **TODO**

---

## Chci definovat vedoucího akce (a jeho zástupce)
ISSUE ID: #83
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Spec `events` definuje pole coordinator (přiřaditelný člen klubu) na akci. Zástupce vedoucího akce specifikován není.

---

## Možnost strhávání/připisování částky za/na akci
ISSUE ID: #82
Milestone: core
Implementation status: **TODO**

---

## Chci mít přehled akcí na cca 10 dní dopředu
ISSUE ID: #81
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `calendar-items` definuje kalendář s uživatelsky volitelným rozsahem (default current month, max 366 dní), který zahrnuje automaticky generované event-date a registration-deadline položky.

---

## Chci zadávat cenu akce
ISSUE ID: #80
Milestone: core
Implementation status: **TODO**

---

## Chci si vybrat které oddílové oblečení si chci objednat
ISSUE ID: #79
Milestone: core
Implementation status: **TODO**

---

## Chci mít přehled o členských příspěvcích. kdo zaplatil / nezaplatil
ISSUE ID: #78
Milestone: core
Implementation status: **TODO**

---

## Možnost strhávání/připisování částky členovi
ISSUE ID: #77
Milestone: core
Implementation status: **TODO**

---

## postezovat si kolik mi bylo strzeno penez za akci
ISSUE ID: #76
Milestone: core
Implementation status: **TODO**

---

## chci ostatnim dat vedet kdy kde je sraz
ISSUE ID: #75
Milestone: core
Implementation status: **TODO**

---

## Potřebuji definovat pro které skupiny je akce určená
ISSUE ID: #74
Milestone: core
Implementation status: **TODO**

---

## Potrebuji zalozit akci
ISSUE ID: #73
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `events` "Create Event" — uživatel s EVENTS:MANAGE může založit akci (název, datum, organizer code + volitelné location, web, coordinator, deadline, kategorie). Akce vzniká v DRAFT statusu.

---

## Potřebuji zadávat informace o akcí
ISSUE ID: #72
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `events` "Update Event" pokrývá editaci DRAFT/ACTIVE akcí včetně inline editace z detail page. Spec `event-categories` pokrývá správu kategorií.

---

## zrušit (či po nějako dobu upravovat) již provedené platby
ISSUE ID: #71
Milestone: core
Implementation status: **TODO**

---

## Chci vědět, kolik, kdy, jak a kam mám platit (členské příspěvky, za akce, vybavení, ...)
ISSUE ID: #70
Milestone: core
Implementation status: **TODO**

---

## Chci vědět, kolik, kdy a jak mám platit (za akce, vybavení, ...)
ISSUE ID: #69
Milestone: core
Implementation status: **TODO**

---

## Chci sledovat stav svých financí
ISSUE ID: #68
Milestone: core
Implementation status: **TODO**

---

## Chci kontaktovat vedoucího akce
ISSUE ID: #67
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Spec `events` ukazuje coordinator jako klikatelný odkaz na jeho profil členské detailové stránky; kontaktní údaje jsou viditelné v profilu. Dedikovaný "kontakt na vedoucího" flow ale není.

---

## Chci mít možnost viditelně škrtnout akci se zveřejněním důvodu zrušení
ISSUE ID: #66
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Spec `events` definuje cancel akci (DRAFT i ACTIVE → CANCELLED) se zachováním registrací pro záznamy. Zveřejnění důvodu zrušení (textový reason) však specifikován není.

---

## chci změnit info o tréninku
ISSUE ID: #65
Milestone: core
Implementation status: **TODO**

---

## Chci mít možnost zneviditelnit/smazat akci
ISSUE ID: #64
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Spec `events` podporuje cancel akce. Hard delete/zneviditelnění mimo DRAFT ale definované není.

---

## chci vedet co kdy poradame a jak se mohu zapojit
ISSUE ID: #63
Milestone: core
Implementation status: **TODO**

---

## Chci zjistit informace o trénincích (tento/další týden)
ISSUE ID: #62
Milestone: core
Implementation status: **TODO**

---

## Chci mít možnost převést finance jinému členu
ISSUE ID: #61
Milestone: core
Implementation status: **TODO**

---

## Chci řešit finance jako řádný člen, (ale bez členských příspevků)
ISSUE ID: #60
Milestone: core
Implementation status: **TODO**

---

## Chci sledovat dění v klubu (novinky)
ISSUE ID: #59
Milestone: core
Implementation status: **TODO**

---

## informovat cleny o deni v oddile
ISSUE ID: #58
Milestone: core
Implementation status: **TODO**

---

## Chci zadávat veřejně dostupné novinky
ISSUE ID: #57
Milestone: core
Implementation status: **TODO**

---

## Chci kontakt na mé trenéry
ISSUE ID: #56
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Training Group Info on Member Profile" — každý člen vidí na profilu název své tréninkové skupiny + jméno a kontakt trenéra/trenérů.

---

## Chci za členy skupiny rešit finance
ISSUE ID: #55
Milestone: core
Implementation status: **TODO**

---

## Pro členy své skupiny chci řešit přihlášky
ISSUE ID: #54
Milestone: core
Implementation status: **TODO**

---

## Chci mít možnost editovat osobní údaje členů skupiny (čip, adresa, telefon, apod)
ISSUE ID: #53
Milestone: core
Implementation status: **TODO**

---

## Chci mapy a informace o pevných areálech kontrol
ISSUE ID: #52
Milestone: core
Implementation status: **TODO**

---

## dozvedet se co je OB klub SK Brno Žabovřesky
ISSUE ID: #51
Milestone: core
Implementation status: **TODO**

---

## kontakt na X
ISSUE ID: #50
Milestone: core
Implementation status: **TODO**

---

## jak se stat clenem
ISSUE ID: #49
Milestone: core
Implementation status: **TODO**

---

## Chci mít přehled pravidelnych tréninků
ISSUE ID: #48
Milestone: core
Implementation status: **TODO**

---

## Chci mít přehled o slevách poskytovaných oddílu (mně)
ISSUE ID: #47
Milestone: core
Implementation status: **TODO**

---

## informace o zavodech poradanych ZBM
ISSUE ID: #46
Milestone: core
Implementation status: **TODO**

---

## Rodiče/Prarodiče rádi sledují své ratolesti (fotky, ...)
ISSUE ID: #45
Milestone: core
Implementation status: **TODO**

---

## Sponzoři se chtějí vidět na stránkách
ISSUE ID: #44
Milestone: core
Implementation status: **TODO**

---

## Chci vidět, že oddíl žije pro volbu kam se přihlásím
ISSUE ID: #43
Milestone: core
Implementation status: **TODO**

---

## Potřebuji znát fakturační údaje oddílu
ISSUE ID: #42
Milestone: core
Implementation status: **TODO**

---

## odkazy na CSOS, ORIS, BZL,..
ISSUE ID: #41
Milestone: core
Implementation status: **TODO**

---

## Chci informace pro nováčky, co a jak v oddíle chodí
ISSUE ID: #40
Milestone: core
Implementation status: **TODO**

---

## Chci číst oddílový časopis
ISSUE ID: #39
Milestone: core
Implementation status: **TODO**

---

## Chci sdílet fotky z akcí (momentálně přihlašovací údaje na rajče/instagram)
ISSUE ID: #38
Milestone: core
Implementation status: **TODO**

---

## Chci kontaktovat vedoucího akce, ...
ISSUE ID: #37
Milestone: core
Implementation status: **IN_PROGRESS**
Details: Viz #67 — kontakty přes profilové stránky koordinátora/členů jsou dostupné skrze spec `events` + `members`, ale dedikovaný kontaktní flow ne.

---

## Chci kontaktovat vedení
ISSUE ID: #36
Milestone: core
Implementation status: **TODO**

---

## Chci kontaktovat trenéra/trenéry, ...
ISSUE ID: #35
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Training Group Info on Member Profile" garantuje viditelnost jména a kontaktu trenérů na profilu člena jeho tréninkové skupiny.

---

## Jako vedouci akce chci odhlasovat lidi z akce
ISSUE ID: #34
Milestone: core
Implementation status: **TODO**

---

## chci prihlasovat lidi na akce (pripadne i po terminu)
ISSUE ID: #33
Milestone: core
Implementation status: **TODO**

---

## Chci zaznamenat absenci drive prihlaseneho cloveka
ISSUE ID: #32
Milestone: core
Implementation status: **TODO**

---

## Chci mít možnost kdykoli před termínem se odhlásit
ISSUE ID: #31
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `event-registrations` "Unregister from Event" — registrovaný člen se odhlásí před datem akce i před deadlinem (pokud je deadline nastaven). Po deadlinu/datu akce je odhlášení odmítnuto.

---

## Chci mít k dispozici seznam příhlášených na akci
ISSUE ID: #30
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `event-registrations` "List Event Registrations" — seznam přihlášených s jménem, příjmením, kategorií a časem přihlášky pro akce mimo DRAFT.

---

## Chci, aby vedoucí akce mohl evidovat účast na akci (soustředění, trénink,....)
ISSUE ID: #29
Milestone: core
Implementation status: **TODO**

---

## chci si vybrat jake notifikace mi budou chodit emailem a/nebo do feedu zmen na strankach
ISSUE ID: #28
Milestone: core
Implementation status: **TODO**

---

## Chci notifikaci pri zapornem zustatku na financnim uctu
ISSUE ID: #27
Milestone: core
Implementation status: **TODO**

---

## Chci se prihlasit na akci
ISSUE ID: #26
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `event-registrations` "Register for Event" — člen se přihlásí na ACTIVE akci s otevřenými registracemi, zadá SI card, volí kategorii (pokud je nastavena).

---

## chci zjisti telefon / adresu na jineho clena
ISSUE ID: #25
Milestone: core
Implementation status: **COMPLETED**
Details: Spec `members` "Member Detail" — autentizovaný člen s MEMBERS:READ vidí kompletní detail (osobní údaje, adresu, kontakty) aktivních členů.

---

## Pozastaveni clenstvi v oddíle na základě obdržené odhlášky
ISSUE ID: #14
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `members` "Membership Suspension" — admin s MEMBERS:UPDATE může suspendovat členství s důvodem (ODHLASKA, PRESTUP, OTHER), kontroluje vlastnictví skupin. Tiket je label `BackendCompleted`.

---

## Clen: editace svych osobnich udaju
ISSUE ID: #4
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `members` "Member Update" — člen edituje vlastní profil (email, telefon, adresa, čip, národnost, bankovní účet, opatrovník, doklady, licence, dietní omezení). Tiket je label `BackendCompleted`.

---

## Zakládání nových členů oddílu
ISSUE ID: #3
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `members` "Member Registration Flow" + "Registration Number Generation" + "Welcome Email on Registration" — kompletní registrační flow s generováním čísla XXXYYSS a uvítacím emailem. Tiket je label `BackendCompleted`.

---

## feat(cus): CUS synchronizace dat členů
ISSUE ID: #265
Milestone: MVP
Implementation status: **TODO**

---

## Zruseni pozvanky do skupiny
ISSUE ID: #241
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `user-groups` "Free Group Invitation System" definuje vytvoření, accept a reject pozvánky. Explicitní zrušení pozvánky majitelem skupiny (cancel pending) ale specifikováno není.

---

## Pri zalození noveho clena klubu chci zvolit skupiny do kterych bude prirazen
ISSUE ID: #240
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `user-groups` "Training Group Membership Assignment" — při registraci nového člena se automaticky přiřadí do training group podle věku. Manuální volba skupin v registračním formuláři specifikována není.

---

## chci synchronizovat členy s ČUS
ISSUE ID: #235
Milestone: MVP
Implementation status: **TODO**

---

## chci definovat pravidelné tréninky
ISSUE ID: #234
Milestone: MVP
Implementation status: **TODO**

---

## Zrušit své členství ve skupině
ISSUE ID: #233
Milestone: MVP
Implementation status: **TODO**

---

## Odebrat členy ze skupiny
ISSUE ID: #232
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Training Group Member Management" (remove člen s GROUPS:TRAINING, mimo trenéra) + "Free Group Member Management" (owner odebere člena) + "Add and Remove Child Members of a Family Group" (MEMBERS:MANAGE odebere dítě).

---

## Pozvat nové členy skupiny
ISSUE ID: #231
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Free Group Invitation System" — owner pozve člena, invitation se objeví v seznamu pozvánek příjemce, který může accept/reject.

---

## Chci aby skupina lidí měla možnost platit společně (např. rodiny)
ISSUE ID: #128
Milestone: MVP
Implementation status: **TODO**

---

## Chci automatický import plateb členů klubu = připsání částky členovi
ISSUE ID: #127
Milestone: MVP
Implementation status: **TODO**

---

## Chci hromadně změnit typu oddílového příspěvku ostatním členům
ISSUE ID: #125
Milestone: MVP
Implementation status: **TODO**

---

## Chci potvrzení o členských příspěvcích, ... (pro pojišťovny)
ISSUE ID: #124
Milestone: MVP
Implementation status: **TODO**

---

## Chci export dat z tabulkovych prehledu do CSV
ISSUE ID: #123
Milestone: MVP
Implementation status: **TODO**

---

## Chci nahrávat mapové teorie a další dokumenty na web
ISSUE ID: #122
Milestone: MVP
Implementation status: **TODO**

---

## chci něcojakowiki (pokyny k poradani, info o treninkovych skupinach,..)
ISSUE ID: #121
Milestone: MVP
Implementation status: **TODO**

---

## Při vstupu do skupiny chci potvrdit oprávnění které skupina vyžaduje
ISSUE ID: #120
Milestone: MVP
Implementation status: **TODO**

---

## Při vytvoření skupiny chci nadefinovat sadu oprávnění platných pro mou skupinu
ISSUE ID: #119
Milestone: MVP
Implementation status: **TODO**

---

## Své skupině mohu nastavit další vedoucí
ISSUE ID: #118
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Group Owner Management" — pro training groups přidání dalšího trenéra (GROUPS:TRAINING), pro free groups promote člena na co-ownera, pro family groups přidání dalšího parent (MEMBERS:MANAGE).

---

## Jsem vedoucím skupinky pro své svěřence
ISSUE ID: #117
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Create Training Group" + "Group Owner Management" — training group má trenéry (referovaní jako "trainers" v UI). Trenéři jsou spravováni uživateli s GROUPS:TRAINING permission.

---

## Chci vědět, do které tréninkové skupiny patřím a kdo jsou trenéři
ISSUE ID: #116
Milestone: MVP
Implementation status: **COMPLETED**
Details: Spec `user-groups` "Training Group Info on Member Profile" — profil člena zobrazuje název tréninkové skupiny a jména + kontakty trenérů.

---

## U zavodu chci mit prehled vysledku clenu klubu
ISSUE ID: #115
Milestone: MVP
Implementation status: **TODO**

---

## Chci se z detailu akce dostat na prehled povinnosti vedoucího akce (odkaz na wiki)
ISSUE ID: #114
Milestone: MVP
Implementation status: **TODO**

---

## Automaticky vytvaret a synchronizovat (a tagovat pro ktere skupiny, termin prihlasek -1D,..) akce z ORIS k diskuzi jak a co implementovat
ISSUE ID: #113
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `events` "ORIS Import Includes Registration Deadline" + "ORIS Import Tolerates Missing Location" + spec `event-categories` "Sync Event from ORIS" — manuální import jednotlivých akcí z ORIS včetně kategorií a deadlinu je implementován. Automatická hromadná synchronizace a tagování pro skupiny specifikovány nejsou.

---

## Chci mít možnost přepsat informace v importovaných akcích (vcetne doplnkovych sluzeb)
ISSUE ID: #112
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `events` "Update Event" umožňuje editaci ORIS-importovaných akcí (DRAFT/ACTIVE). Doplňkové služby ale specifikovány nejsou.

---

## Chci (uzivatelsky definovatelny?) export kalendare do externich aplikaci (google calendar, Outlook, etc)
ISSUE ID: #111
Milestone: MVP
Implementation status: **TODO**

---

## Chci kalendar kde uvidim terminy prihlasek, akce skupin jejichz jsem clenem
ISSUE ID: #110
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `calendar-items` "Automatic Synchronization from Events" — kalendář automaticky obsahuje event-date položky i registration-deadline položky ("Přihlášky - {event name}"). Filtrace podle členství ve skupinách ale specifikována není.

---

## Chci mit moznost filtrovat v kalendari podle ruznych kriterii
ISSUE ID: #109
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `calendar-items` podporuje sortování a navigaci po měsících. Filtrace podle kritérií (skupiny, typ, atd.) specifikována není.

---

## Chci zadávat dotaci oddílu akci po skupinách (může se lišit)
ISSUE ID: #108
Milestone: MVP
Implementation status: **TODO**

---

## Chci zadávat slevy na akci pro členy/skupiny (za pomoc/práci)
ISSUE ID: #107
Milestone: MVP
Implementation status: **TODO**

---

## Potřebují čísla OP/pasů na ubytování
ISSUE ID: #106
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `members` "Identity Card, Licenses, and Supplementary Fields" eviduje ID card s validity date na úrovni člena. Propojení s ubytováním na akci specifikováno není.

---

## Chci mít k dispozici seznam ubytovaných na akci včetně adres a dalších údajů
ISSUE ID: #105
Milestone: MVP
Implementation status: **TODO**

---

## Pro dva a více víkendových závodů (spojených) mít společnou evidenci ubytování
ISSUE ID: #104
Milestone: MVP
Implementation status: **TODO**

---

## Potřebuji co nejdříve vědět počty účastníků kvůli ubytování (nezávazný zájem)
ISSUE ID: #103
Milestone: MVP
Implementation status: **TODO**

---

## Pri prihlasovani si chci zvolit doplnkove sluzby (vypsane v ORIS popr. "offline v systemu")
ISSUE ID: #102
Milestone: MVP
Implementation status: **TODO**

---

## Chceme zpetnou synchronizaci prihlasek z ORIS do systemu (lide prihlasujici se primo v Oris popr. dohlaseni pres email po terminu)
ISSUE ID: #101
Milestone: MVP
Implementation status: **TODO**

---

## Chci si pripravit seznam zavodu kterych se chci zucastnit jeste pred tim nez se na tyto zavody otevrou prihlasky (ale uz jsou vypsane terminy v ORIS)
ISSUE ID: #100
Milestone: MVP
Implementation status: **TODO**

---

## Chci vedet koho mam dohlasit rucne (pred terminem prihlasek)
ISSUE ID: #99
Milestone: MVP
Implementation status: **TODO**

---

## Synchronizace prihlasek vybrane s ORIS udalosti na "tlacitko"
ISSUE ID: #98
Milestone: MVP
Implementation status: **TODO**

---

## Chci na akci prihlasit nekoho neregistrovaneho v klubu na interni akci ("+1") tak aby byl zapocitan do poctu (doprava, ubytovani, apod)
ISSUE ID: #97
Milestone: MVP
Implementation status: **TODO**

---

## Chci mít možnost po uzaverce prihlasek zadat absenci
ISSUE ID: #96
Milestone: MVP
Implementation status: **TODO**

---

## Chci přehled/statistiku o počtu účastí na akcí/jednotlivých typech akcích/...
ISSUE ID: #95
Milestone: MVP
Implementation status: **TODO**

---

## Potřebují přehled účasti na akcích (pro určení slev na členských prispevcich, apod)
ISSUE ID: #94
Milestone: MVP
Implementation status: **TODO**

---

## Chci se prihlasit jako vypomoc na akci/poradani/apod
ISSUE ID: #93
Milestone: MVP
Implementation status: **TODO**

---

## Chci mít možnost kdykoli před termínem (i mezi termíny) změnit příhlášku
ISSUE ID: #92
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `event-registrations` podporuje register i unregister před deadlinem, takže změna přihlášky je možná přes unregister + re-register. Explicitní "edit registration" (např. změna kategorie bez re-registrace) specifikována není.

---

## Chci být informován o nových akcích z ORISu (abych zkontroloval přiřazení ke skupině)
ISSUE ID: #91
Milestone: MVP
Implementation status: **TODO**

---

## Chci být informován o změnách importovane akce (napr. z ORIS) po prvotnim importu (včetně těch, které jsem přepsal)
ISSUE ID: #90
Milestone: MVP
Implementation status: **TODO**

---

## Chci nepropasnout zadny deadline prihlasek
ISSUE ID: #89
Milestone: MVP
Implementation status: **IN_PROGRESS**
Details: Spec `calendar-items` "Automatic Synchronization from Events" — pro každou publikovanou akci s deadlinem vzniká automaticky položka "Přihlášky - {event name}" dne deadlinu v kalendáři. Aktivní email notifikace specifikována není.

---

## Chci si řídit, kdo má přístup k informacím o mně
ISSUE ID: #24
Milestone: MVP
Implementation status: **TODO**

---

## Chci vědět, na které žebříčky mám právo (z ORISu).
ISSUE ID: #10
Milestone: MVP
Implementation status: **TODO**

---

## natahovat aktuální licence členů z ORISu do systému
ISSUE ID: #7
Milestone: MVP
Implementation status: **TODO**

---

## Chci synchronizaci údajů clenu k jednotlivým agenturám (ORIS, ČUS)
ISSUE ID: #6
Milestone: MVP
Implementation status: **TODO**

---

## Vytvoreni noveho (hostujicho) clena z ORIS (prestupy, hostujici clen)
ISSUE ID: #5
Milestone: MVP
Implementation status: **TODO**
</content>
</invoke>