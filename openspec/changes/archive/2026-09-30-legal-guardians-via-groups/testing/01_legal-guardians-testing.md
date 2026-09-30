# Legal Guardians via Groups - QA Testing

## Scenarios

### Detail nezletilého (admin ZBM9000)
- [ ] **DET-1**: Detail Matyáše Kratochvíla zobrazuje sekci „Zákonní zástupci“ s Ondřejem Kratochvílem a Lenkou Kratochvílovou (jméno, e-mail, telefon)
- [ ] **DET-2**: Klik na nečlenského zástupce (Lenka) otevře profil zástupce; klik na zástupce-člena (Ondřej) otevře detail člena
- [ ] **DET-3**: Detail Vojtěcha Malého (bez skupiny) – sekce uvádí „bez zákonného zástupce“, akce „Upravit zástupce“ je dostupná, zobrazeno chybějící GUARDIAN v neúplných údajích
- [ ] **DET-4**: Detail dospělého člena nemá sekci zástupců ani akci „Upravit zástupce“ ani „Založit účet“
- [ ] **DET-5**: Detail nezletilého ve skupině má tlačítko „Zákonní zástupci“ vedoucí na detail skupiny

### Úprava zástupců nezletilého (admin)
- [ ] **EDIT-1**: Vojtěchovi nastavit existujícího zástupce (dospělý člen) – po uložení je zástupce vypsán, zmizí chybějící GUARDIAN
- [ ] **EDIT-2**: Přidání druhého zástupce jednomu sourozenci (Adam Dlouhý) nezmění zástupce druhého sourozence (Klára)
- [ ] **EDIT-3**: Nový nečlenský zástupce založený inline v „Upravit zástupce“ je vypsán a dostane login EXTnnnn (další v řadě)
- [ ] **EDIT-4**: Odebrání posledního zástupce → chyba, nic se neuloží
- [ ] **EDIT-5**: Nový zástupce bez telefonu → chyba, nic se neuloží
- [ ] **EDIT-6**: Nový zástupce s e-mailem existujícího zástupce → chyba „e-mail již použit“
- [ ] **EDIT-7**: Picker kandidátů: hledání podle jména, člen s reg. číslem, nečlen s e-mailem, bez nezletilých

### Stránka „Zákonní zástupci“ (admin)
- [ ] **GRP-1**: Položka „Zákonní zástupci“ v sekci Administrace; seznam skupin s názvem, zástupci a počtem nezletilých
- [ ] **GRP-2**: Název skupiny = příjmení zástupců abecedně spojená „ a “ (např. „Kratochvíl a Kratochvílová“)
- [ ] **GRP-3**: Detail skupiny vypisuje zástupce a nezletilé, oba lze otevřít
- [ ] **GRP-4**: Úprava zástupců celé skupiny se promítne všem dětem skupiny
- [ ] **GRP-5**: Úprava zástupců skupiny na množinu jiné existující skupiny → sloučení, původní skupina zmizí
- [ ] **GRP-6**: Žádná akce pro ruční vytvoření/smazání skupiny

### Registrace (admin)
- [ ] **REG-1**: Formulář přepíná sekce podle data narození (nezletilý: sekce zástupců, e-mail/telefon volitelné; dospělý: bez sekce zástupců, e-mail/telefon povinné)
- [ ] **REG-2**: Registrace nezletilého s vybraným existujícím zástupcem (člen) – zástupce vypsán na detailu
- [ ] **REG-3**: Registrace nezletilého se dvěma zástupci (existující + nový nečlen) – oba vypsáni, nový má login EXTnnnn
- [ ] **REG-4**: Registrace nezletilého bez zástupce → chyba, nic nevytvořeno
- [ ] **REG-5**: Registrace nezletilého s novým zástupcem s obsazeným e-mailem → chyba, nic nevytvořeno
- [ ] **REG-6**: Registrace dospělého s převzetím nečlenského zástupce – předvyplnění údajů, po registraci je členem, zůstává zástupcem svých dětí
- [ ] **REG-7**: Sourozenec se stejnými zástupci se při registraci připojí do existující skupiny

### Profil nečlenského zástupce
- [ ] **PROF-1**: Admin otevře profil zástupce – zobrazen login EXTnnnn, jméno, e-mail, telefon
- [ ] **PROF-2**: Admin změní telefon zástupce – nový telefon je na profilu i v sekci zástupců u dětí
- [ ] **PROF-3**: Vymazání e-mailu/telefonu na profilu → chyba, nic se neuloží
- [ ] **PROF-4**: Běžný člen (ZBM9500) nemá přístup k profilu cizího zástupce

### „Založit účet“ (admin)
- [ ] **ACT-1**: Nezletilý s vlastním e-mailem a neaktivním účtem má akci „Založit účet“; po potvrzení se zobrazí potvrzení odeslání
- [ ] **ACT-2**: Nezletilý bez vlastního e-mailu akci nemá

### Oprávnění a navigace (člen ZBM9500)
- [ ] **AUTH-1**: Běžný člen nevidí položku „Zákonní zástupci“ v menu
- [ ] **AUTH-2**: Běžný člen na detailu cizího nezletilého nevidí sekci zástupců, „Upravit zástupce“ ani „Založit účet“
- [ ] **AUTH-3**: „Můj profil“ na HomePage vede na detail vlastního člena
- [ ] **AUTH-4**: Bootstrap admin (`admin`) nevidí „Můj profil“

### Přihlášení nečlenského zástupce
- [ ] **LOGIN-1**: Zástupce (EXT0001) požádá o aktivaci loginem + svým e-mailem → aktivační odkaz odeslán; nastaví heslo a přihlásí se loginem EXT0001
- [ ] **LOGIN-2**: Přihlášený zástupce vidí seznam členů v menu a „Můj profil“ vede na jeho profil zástupce
- [ ] **LOGIN-3**: Zástupce si na svém profilu změní e-mail a dál se přihlašuje loginem EXT0001

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| GRP-1 | FAIL | #1: seznam má jen Název + Počet nezletilých; `LegalGuardianGroupSummaryResponse` neobsahuje zástupce (BE) a FE nemá sloupec. Navíc řazení `sort=name,asc` nefunguje (Dlouhá, Svobodová, Kratochvíl…) |
| GRP-2 | PASS | „Kratochvíl a Kratochvílová“ |
| GRP-6 | PASS | seznam ani detail nemá vytvořit/smazat |
| PROF-1 | PASS | EXT0002, e-mail, telefon |
| PROF-2 | PASS | telefon změněn na profilu i na detailu Matyáše |
| DET-1 | PASS | oba zástupci s e-mailem a telefonem |
| DET-2 | PASS | Lenka → profil zástupce, Ondřej → detail člena |
| DET-4 | PASS | Ondřej (dospělý) bez sekce a akcí |
| DET-5 | PASS | tlačítko „Zákonní zástupci“ → detail skupiny |
| GRP-3 | FAIL | #2: řádek nezletilého v detailu skupiny nejde otevřít, přestože API vrací `minors[]._links.member` (FE) |
| PROF-3 | FAIL | #3: vymazání telefonu/e-mailu → FE pošle `PATCH {phone:null}` / `{email:null}`, BE vrátí 204 a hodnotu tiše ponechá; žádná chyba se neukáže (spec: chyba „e-mail a telefon jsou povinné“). Pravděpodobně chybí `required` v HAL-FORMS šabloně a null se bere jako „beze změny“ (BE) |
| DET-3 | PASS | „Bez zákonného zástupce“, „Upravit zástupce“, v neúplných „zákonný zástupce“ |
| EDIT-7 | FAIL | #4: picker volá neexpandovanou URI šablonu `/api/legal-guardian-options%7B?q,kind}` → 400, combobox zůstane prázdný; nelze vybrat existujícího zástupce (FE). Navíc popisek pole je nepřeložený `legalGuardians` |
| EDIT-5 | PASS | bez telefonu formulář neodešle (HTML required na poli Telefon*) |
| EDIT-3 | PASS | Karel Malý vypsán, profil EXT0003 |
| EDIT-1 | PARTIAL | po nastavení zástupce zmizí „zákonný zástupce“ z neúplných; výběr existujícího člena blokuje #4 |
| EDIT-6 | PASS | chyba „E-mail … already belongs to an existing legal guardian…“ (anglicky), nic neuloženo |
| EDIT-4 | PASS | „Odebrat“ u jediného zástupce disabled; po „Zrušit výběr“ chyba, nic neuloženo (technická hláška „must be given either by userId…“) |
| REG-1 | PASS | nezletilý: sekce zástupců + „e-mail/telefon volitelné“; dospělý: E-mail*/Telefon* + sekce převzetí zástupce (combobox převzetí blokuje #4) |
| REG-4 | PASS | „Guardian is required for minors“, nic nevytvořeno |
| REG-3 | PARTIAL | registrace s novým nečlenským zástupcem OK (Test Bezzástupce ZBM1500 / Jana Bezzástupcová); výběr existujícího blokuje #4 |
| REG-5 | PASS | chyba „e-mail already belongs…“, nic nevytvořeno |
| ACT-2 | PASS | Matyáš (bez e-mailu) akci nemá |
| ACT-1 | PASS | po doplnění e-mailu akce „Založit účet“, potvrzení „Aktivační odkaz byl odeslán na e-mail člena.“, e-mail v logu |
| REG-2, REG-6, REG-7, EDIT-2, GRP-4, GRP-5 | SKIP | blokováno #4 (nelze vybrat existujícího zástupce) |
| AUTH-1 | PASS | ZBM9500 nemá „Zákonní zástupci“ v menu |
| AUTH-3 | PASS | „Můj profil“ → /members/{vlastní id} |
| PROF-4 | PASS | ZBM9500 na profilu Lenky → HTTP 403 |
| AUTH-2 | PASS | ZBM9500 na detailu Matyáše: jen kontakt a adresa, žádná sekce zástupců ani akce |
| AUTH-4 | PASS | bootstrap `admin` nemá „Můj profil“ |
| LOGIN-1..3 | SKIP | dle zadání uživatele (ruční test) |

#### Issues (iteration 1)
1. **GRP-1** (BE+FE) – seznam skupin nezobrazuje zástupce; `sort=name,asc` neřadí.
2. **GRP-3** (FE) – nezletilý v detailu skupiny není klikatelný.
3. **PROF-3** (BE+FE) – vymazání e-mailu/telefonu na profilu zástupce tiše ignorováno (204), žádná chyba.
4. **EDIT-7** (FE) – options link `{?q,kind}` se neexpanduje → 400; blokuje výběr existujícího zástupce (EDIT-1/2, GRP-4/5, REG-2/6/7). Nepřeložený popisek `legalGuardians`.

### Iteration 2
| Scenario | Result | Note |
|----------|--------|------|
| GRP-1 | PASS | sloupec „Zástupci“, řazení podle názvu OK |
| GRP-2 | PASS | |
| GRP-6 | PASS | |
| GRP-3 | PASS | nezletilý je odkaz na detail člena |
| EDIT-7 | FAIL | #5: kandidáti se načtou, ale combobox ukazuje jen jméno – chybí reg. číslo (člen) / e-mail (nečlen) a nelze vyhledávat podle jména; API `registrationNumber`/`email` vrací (FE) |
| EDIT-1 | PASS | Vojtěch ← Pavel Dvořák (člen), GUARDIAN zmizel z neúplných |
| EDIT-2 | PASS | Adam: Ivana + Pavel (nová skupina „Dlouhá a Dvořák“), Klára zůstává ve „Dlouhá“ jen s Ivanou |
| GRP-5 | PASS* | skupina „Dvořák“ sloučena do „Dlouhá a Dvořák“ (2 nezletilí) — ale #7: po odeslání zůstane stránka na smazané skupině a ukáže „HTTP 404 (Not Found)“ (FE) |
| GRP-4 | PASS | přidán Tomáš Král → „Dlouhá a Dvořák a Král“, oba nezletilí ve skupině |
| GRP-4/5 | FAIL | #6: „Upravit zástupce“ na detailu skupiny otevře prázdný seznam – aktuální zástupci nejsou předvyplnění (na detailu člena předvyplnění jsou); admin musí zadat všechny znovu |
| PROF-3 | PASS | vymazání telefonu → „phone: nesmí být prázdná“, nic neuloženo |
| PROF-2 | PASS | Ivana: nový telefon na profilu i v detailu skupiny |
| REG-2 | PASS | Tereza Kratochvílová (ZBM1900) s vybranými Ondřejem + Lenkou |
| REG-7 | PASS | Tereza se připojila do existující skupiny „Kratochvíl a Kratochvílová“ (2 nezletilí) |
| REG-6 | PASS | převzetí nabízí jen nečleny (Ivana, Lenka); předvyplněno jméno/e-mail/telefon; Lenka je členem ZBM8501 se stejným userId a v detailu skupiny vede na detail člena |
| ostatní | SKIP | nezměněné oblasti (PASS v iteraci 1) – přetestuje se v iteraci 3 |

#### Issues (iteration 2)
5. **EDIT-7** (FE) – picker kandidátů ukazuje jen jméno, chybí reg. číslo / e-mail a vyhledávání podle jména.
6. **GRP-4/5** (FE) – formulář „Upravit zástupce“ na detailu skupiny není předvyplněn aktuálními zástupci.
7. **GRP-5** (FE) – po sloučení skupiny zůstane stránka na smazané skupině s „HTTP 404 (Not Found)“.

### Iteration 3
| Scenario | Result | Note |
|----------|--------|------|
| GRP-1 | PASS | čistá data: Dlouhá / Kratochvíl a Kratochvílová / Svobodová se zástupci a počty |
| EDIT-7 | PASS | #5 opraveno: „Jméno (ZBMnnnn)“ / „Jméno (e-mail)“, hledání „kratoch“ bez diakritiky najde Lenku + Ondřeje |
| GRP-4/5 | FAIL | #6 přetrvává: „Upravit zástupce“ na detailu skupiny „Dlouhá“ otevře prázdný seznam (GET …/guardians vrátil 200 s Ivanou); na detailu nezletilého (Sofie) předvyplnění funguje |
| GRP-5 | PASS | #7 opraveno: „Dlouhá“ → Eva Svobodová sloučeno do „Svobodová“ (3 nezletilí), toast „Skupina byla sloučena…“, přesměrování na seznam |
| EDIT-1 (prefill) | PASS | detail Sofie: Eva Svobodová předvybraná, tlačítko „Zrušit výběr“ |
| DET-1 | PASS | Matyáš: Ondřej + Lenka s e-mailem a telefonem |
| DET-2 | PASS | Lenka → profil zástupce; Ondřej → detail člena |
| DET-3 | PASS | Vojtěch: „Bez zákonného zástupce“, „Upravit zástupce“, chybí „zákonný zástupce“ |
| DET-4 | PASS | Ondřej (dospělý): žádná sekce ani akce zástupců |
| DET-5 | PASS | tlačítko „Zákonní zástupci“ na detailu Matyáše |
| PROF-1 | PASS | Lenka EXT0002 |
| PROF-3 | PASS | vymazání e-mailu → „email: nesmí mít hodnotu Null“ |
| EDIT-4 | PASS | prázdný seznam → „legalGuardians: velikost musí ležet v rozsahu 1 až …“ |
| EDIT-6 | PASS | e-mail Ivany → „already belongs to an existing legal guardian…“ (anglicky, follow-up) |
| EDIT-5 | PASS | bez telefonu se formulář neodešle (HTML required) |
| EDIT-3 | PASS | Vojtěch ← nový Petr Malý, profil EXT0003; následně nabízen v pickeru |
| EDIT-7 | PASS | picker nenabízí nezletilé |
| REG-1 | PASS | nezletilý: sekce zástupců + „e-mail a telefon volitelné“ |
| REG-4 | PASS | „Guardian is required for minors“ |
| REG-5 | PASS | nový zástupce s e-mailem Petra Malého → chyba, nic nevytvořeno |
| REG-3 | PASS | Ema Testová ZBM1500: Jan Novák (vybraný) + Hana Testová (nová, EXT0005). Pozn.: EXT0004 spotřebován neúspěšným pokusem (chybějící rodné číslo) – mezera v sekvenci, číslo se nepoužije znovu |
| ACT-1 | PASS | Ema po doplnění e-mailu: „Založit účet“ → „Aktivační odkaz byl odeslán na e-mail člena.“ |
| ACT-2 | PASS | Matyáš (bez e-mailu) akci nemá |
| AUTH-1 | PASS | ZBM9500 bez „Zákonní zástupci“ v menu |
| AUTH-2 | PASS | ZBM9500 na detailu Matyáše: jen kontakt + adresa |
| AUTH-3 | PASS | „Můj profil“ → vlastní detail |
| PROF-4 | PASS | ZBM9500 na profilu Lenky → 403 |
| AUTH-4 | PASS | bootstrap `admin` bez „Můj profil“ |
| LOGIN-* | SKIP | dle zadání (ruční test) |
| REG-2 | PASS | Ota Test s vybranými Hanou Testovou (nečlen) + Janem Novákem (člen) |
| REG-7 | PASS | Ota se připojil k Emě do skupiny „Novák a Testová“ (2 nezletilí) |
| GRP-2 | PASS | „Novák a Testová“, „Malý“ |
| REG-1 (dospělý) | PASS | E-mail*/Telefon*, sekce převzetí nabízí jen nečleny |
| REG-6 | PASS | převzetí Petra Malého: předvyplněno jméno/e-mail/telefon; členem ZBM8501 se stejným userId, na detailu Vojtěcha vede na detail člena |
| GRP-4/5 (#6 retest) | PASS | po opravě fe-qa3 (sdílený query key → cache přebíjela resourceData): „Upravit zástupce“ skupiny „Novák a Testová“ předvyplněn Janem + Hanou |
| GRP-4 | PASS | přidán Pavel Dvořák → „Dvořák a Novák a Testová“, oba nezletilí (Ema, Ota) |
| EDIT-2 | PASS | Emě přidána Eva Svobodová → nová skupina se 4 zástupci; Ota zůstává ve „Dvořák a Novák a Testová“ |

#### Issues (iteration 3)
6. ~~**GRP-4/5** (FE) – stále: formulář na detailu skupiny není předvyplněn (`prefillFromTarget={false}` + `resourceData={legalGuardians:[{userId}]}` nestačí).~~ Opraveno v `useHalFormData` (ignoruje cache při `prefillFromTarget=false`), ověřeno.

**Výsledek: všechny scénáře (kromě LOGIN-*, ruční test) PASS.**

### Regrese po opravách z code review
| Scenario | Result | Note |
|----------|--------|------|
| GRP-4/5 prefill | PASS | „Dvořák a Novák a Testová“: 3 zástupci předvybraní, „Zrušit výběr“ u každého |
| EDIT-7 | PASS | hledání „svobodova“ → jen „Eva Svobodová (ZBM9500)“ |
| GRP-5 | PASS | přidána Eva → sloučeno do „Dvořák a Novák a Svobodová a Testová“ (2 nezletilí), toast + přesměrování |
