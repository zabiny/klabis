# Proposal

## Why

Členská základna klubu je dnes vedená dvakrát: v ORISu, kde je zdrojem pravdy pro celý český orientační sport, a v Klabisu, kam ji musí administrátor přepsat ručně. Každý nový člen se tak zakládá dvakrát a každá změna adresy, telefonu nebo čipu se musí provést na obou místech — nebo se neprovede nikde a data se rozejdou.

Klub už má na tenhle druh problému hotový nástroj: generický synchronizační engine (`com.klabis.sync`, ADR-005), který právě podruhé prokázal svůj návrh na disciplínách. Napojit na něj členy znamená především napsat adaptér, ne stavět integraci od nuly.

## What Changes

- Zavést `MemberSyncAdapter` — pull-only adaptér se schopností zakládat lokální stranu (`SyncCapabilities.pullOnlyCreating()`), který čte ORIS `getClubUserList` a udržuje osobní údaje člena v souladu.
- Zavést `MemberDiscoveryJob` — plánovaná úloha, která projde seznam členů klubu v ORISu a každého dosud nespárovaného zaeviduje. Stejný vzor jako `DisciplineDiscoveryJob`: členy nikdo nejmenuje jednotlivě, systém si je najde sám.
- Nový člen z ORISu vzniká **přes `RegistrationPort.registerMember()`**, tedy úplně stejnou cestou jako při ručním založení administrátorem. Vzniká tím i uživatelský účet, finanční účet a zařazení do tréninkové skupiny podle věku — všechny obvyklé důsledky registrace.
- Změna člena v ORISu se promítne **přes `ManagementPort.updateMember()`**, tedy stejnou cestou jako když člena upraví administrátor.
- **Registrační číslo se přebírá z ORISu**, negeneruje se. ORIS je jeho vlastníkem; formát `ZBM9000` z něj ostatně pochází. Vyžaduje to rozšířit `RegisterNewMember` o volitelné registrační číslo — dnes ho vždy generuje service vrstva.
- Synchronizují se **jen údaje, které ORIS skutečně vlastní**: jméno, datum narození, pohlaví, státní příslušnost, rodné číslo, e-mail, telefon, adresa a číslo SI čipu. Pole, která ORIS nezná (průkaz, řidičské oprávnění, licence, zákonný zástupce, bankovní účet, dietní omezení), zůstávají plně v režii Klabisu a synchronizace se jich nesmí dotknout.
- **Členství se nesynchronizuje.** ORIS vede členství jako časový interval (`memberFrom`/`memberTo`/`valid`), Klabis jako pozastavení s důvodem a odpovědnou osobou — nejsou to tytéž pojmy a mechanicky se nepřevádějí. Místo toho se členové, kteří v ORISu neplatí, vůbec neimportují.
- Zavést správu klubového klíče k ORISu. Dnes ho Klabis nemá nikde; `oris-client` ho vědomě nespravuje a nechává na konzumující aplikaci. Konkrétně:
  - Administrátor klíč nastaví a systém umí odpovědět, **zda je nastaven** — ale nikdy nevrátí jeho hodnotu, ani zamaskovanou. Jednou zadaný klíč se dá jen přepsat, ne přečíst.
  - Klíč se zatím drží jen v paměti běžící aplikace, takže ho restart zapomene a administrátor ho musí zadat znovu. Rozhraní se ale navrhne tak, aby za ním šlo později vyměnit trvalé šifrované úložiště bez zásahu do volajících.
  - Operace vůči ORISu, které klíč vyžadují, se schovají za vlastní rozhraní, které klíč **samo doplňuje** — volající o něm vůbec neví. Není-li klíč nastaven, volání skončí srozumitelnou chybou místo odmítnutí z ORISu. Dnes takovou operací je jen načtení členů klubu; rozhraní počítá s tím, že další přibydou (například zápis přihlášek). Zbývající volání ORISu, která klíč nepotřebují, zůstávají beze změny.
- Import se **nespouští při aktivním profilu `example-data`**, aby si nesrazil hlavu s ukázkovými členy (`ZBM9000`, `ZBM9500`).

```mermaid
flowchart TD
    A[MemberDiscoveryJob] --> M{klubový klíč nastaven?}
    M -->|ne| N[nedělat nic a zalogovat]
    M -->|ano| B[ORIS: seznam členů klubu]
    B --> C{platný člen?}
    C -->|ne| D[přeskočit]
    C -->|ano| E{už spárován?}
    E -->|ano| F[engine udržuje v souladu]
    E -->|ne| G[pullAndEnroll]
    G --> H[MemberSyncAdapter.createLocal]
    H --> I[registerMember]
    I --> J[Member + User + finanční účet + tréninková skupina]
    F --> K[MemberSyncAdapter.applyToLocal]
    K --> L[updateMember]
```

## Capabilities

### New Capabilities
- `member-synchronization`: průběžné udržování členů Klabisu v souladu s jejich protějšky v ORISu — automatické zavedení nového člena, promítnutí změny osobních údajů a viditelnost stavu synchronizace u konkrétního člena.
- `oris-club-key`: nastavení klubového klíče k ORISu administrátorem a zjištění, zda už je nastaven, aniž by systém kdy prozradil jeho hodnotu.

### Modified Capabilities
- `members`: registrace člena může nově převzít registrační číslo zvenčí místo toho, aby si ho systém vždy vygeneroval sám.

## Impact

- **Backend members**: `RegistrationPort.RegisterNewMember` dostává volitelné registrační číslo; `RegistrationService` ho generuje jen když nepřijde. Doménový příkaz `Member.RegisterMember` už registrační číslo přijímá, takže se nemění.
- **Backend sync**: nová hodnota `SyncEntityType.MEMBER`; nový `MemberSyncAdapter`, `MemberProjection` a `MemberDiscoveryJob` v novém balíčku `com.klabis.members.infrastructure.orissync`.
- **Citlivé údaje**: `MemberSyncAdapter` jako první adaptér deklaruje `containsSensitiveData` — projekce nese rodné číslo. Šifrování projekcí v úložišti už engine má (`EncryptedString`, stejně jako `members.birth_number`), takže jde o nastavení příznaku, ne o novou infrastrukturu.
- **Audit**: synchronizace čte rodná čísla bez zápisu do `VIEW_BIRTH_NUMBER` auditu. Vědomě přijato — spec `data-synchronization` už detail synchronizace zpřístupňuje jen držiteli oprávnění k synchronizaci.
- **Klubový klíč**: nový port v doméně s in-memory adaptérem (klíč nepřežije restart) a dvě REST operace — nastavit klíč a zjistit, zda je nastaven. Čtecí operace vrací výhradně ano/ne, nikdy hodnotu. Nové rozhraní zastřešující klíčem chráněná volání ORISu, s vlastní výjimkou pro nenastavený klíč.
- **Konfigurace**: cron výrazy pro discovery job.
- **Databáze**: žádná změna schématu členů. Párování drží engine ve svých vlastních záznamech, stejně jako u událostí a disciplín (ADR-005).
- **Mimo rozsah**: zápis změn z Klabisu do ORISu. ORIS pro členy zatím nenabízí zapisovací API. Až ho nabídne, směr se otočí — administrátor založí člena v Klabisu a ten se propíše do ORISu.
- **Přijaté omezení**: člen, který už v Klabisu existuje a zároveň je v ORISu, se automaticky nespáruje — engine dnes umí jen „založit nový", ne „napojit na tenhle existující". V produkci to skončí odmítnutím kvůli jedinečnosti registračního čísla a záznamem v logu. Přijato vědomě pro první nasazení.
