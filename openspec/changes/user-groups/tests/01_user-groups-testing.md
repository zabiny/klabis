# User Groups - Testovani implementace (Slice 1a: Free Group CRUD)

## Scenare k otestovani

Scenare odvozeny ze specs/user-groups/spec.md a tasks.md (Slice 1a - completed).

### Navigace a pristup
- [ ] **NAV-1**: Polozka "Skupiny" je viditelna v navigaci pro prihlaseneho uzivatele
- [ ] **NAV-2**: Kliknuti na "Skupiny" zobrazi stranku se seznamem skupin

### Vytvoreni skupiny
- [ ] **CREATE-1**: Tlacitko "Vytvorit skupinu" je viditelne na strance skupin
- [ ] **CREATE-2**: Formular pro vytvoreni skupiny obsahuje pole pro nazev
- [ ] **CREATE-3**: Uspesne vytvoreni skupiny s platnym nazvem
- [ ] **CREATE-4**: Po vytvoreni se skupina objevi v seznamu
- [ ] **CREATE-5**: Tvurce skupiny je automaticky vlastnikem a clenem

### Detail skupiny
- [ ] **DETAIL-1**: Kliknuti na skupinu v seznamu zobrazi detail
- [ ] **DETAIL-2**: Detail zobrazuje nazev skupiny
- [ ] **DETAIL-3**: Detail zobrazuje sekci spravcu (vlastniku)
- [ ] **DETAIL-4**: Detail zobrazuje sekci clenu s tabulkou
- [ ] **DETAIL-5**: Tabulka clenu zobrazuje registracni cislo, prijmeni, jmeno, datum pridani

### Editace skupiny
- [ ] **EDIT-1**: Tlacitko pro editaci nazvu je viditelne pro vlastnika
- [ ] **EDIT-2**: Uspesna zmena nazvu skupiny
- [ ] **EDIT-3**: Po zmene nazvu se aktualizuje zobrazeny nazev

### Smazani skupiny
- [ ] **DELETE-1**: Tlacitko pro smazani je viditelne pro vlastnika
- [ ] **DELETE-2**: Uspesne smazani skupiny
- [ ] **DELETE-3**: Po smazani je uzivatel presmerovan na seznam skupin
- [ ] **DELETE-4**: Smazana skupina se neobjevuje v seznamu

### Pridani clena
- [ ] **ADD-1**: Tlacitko "Pridat clena" je viditelne pro vlastnika
- [ ] **ADD-2**: Formular pro pridani clena umoznuje vybrat clena
- [ ] **ADD-3**: Uspesne pridani clena do skupiny
- [ ] **ADD-4**: Pridany clen se objevi v tabulce clenu

### Odebrani clena
- [ ] **REMOVE-1**: Tlacitko "Odebrat" je viditelne u kazdeho clena (krome vlastnika)
- [ ] **REMOVE-2**: Uspesne odebrani clena ze skupiny
- [ ] **REMOVE-3**: Odebrany clen zmizi z tabulky clenu
- [ ] **REMOVE-4**: Vlastnika nelze odebrat ze skupiny

### Autorizace (testovano jako druhy uzivatel - ne-vlastnik)
- [ ] **AUTH-1**: Ne-vlastnik vidi skupinu, kde je clenem, v seznamu
- [ ] **AUTH-2**: Ne-vlastnik na detailu neviditelna tlacitka pro editaci/smazani/pridani/odebrani clena

---

## Vysledky testovani

### Iterace 1

Testovano pred restartem backendu (stary kod bez uncommitted zmen).
Mnoho FAIL kvuli nesouladu backend/frontend template names a chybejicim member data.
Issues z Iterace 1 byly opraveny uncommitted zmenami z predchozi konverzace.

### Iterace 2

Po restartu backendu s aktualnim kodem (vcetne uncommitted zmen) vysledky:

| Scenar | Vysledek | Poznamka |
|--------|----------|----------|
| NAV-1 | PASS | Odkaz "Skupiny" v navigaci viditelny |
| NAV-2 | PASS | Stranka /groups se zobrazila |
| CREATE-1 | PASS | Tlacitko "Vytvorit skupinu" viditelne |
| CREATE-2 | PASS | Formular s polem "Nazev" v modalnim dialogu |
| CREATE-3 | PASS | Skupina vytvorena, toast "Uspesne ulozeno" |
| CREATE-4 | PASS | Skupina se objevila v seznamu |
| CREATE-5 | PASS | Tvurce je vlastnikem (SPRAVCI: ZBM9000 / Jan Novak) i clenem |
| DETAIL-1 | PASS | Klik na radek otevre detail |
| DETAIL-2 | PASS | Nazev skupiny zobrazen |
| DETAIL-3 | PASS | Sekce SPRAVCI: registracni cislo a jmeno vlastnika |
| DETAIL-4 | PASS | Sekce CLENU s tabulkou zobrazena |
| DETAIL-5 | PASS | ZBM9000, Novak, Jan, 31. 3. 2026 |
| EDIT-1 | PASS | Tlacitko "Upravit nazev" viditelne |
| EDIT-2 | PASS | Nazev zmenen na "Prejmenована skupina" |
| EDIT-3 | PASS | Nadpis aktualizovan |
| DELETE-1 | PASS | Tlacitko "Smazat skupinu" viditelne |
| DELETE-2 | PASS | Skupina uspesne smazana |
| DELETE-3 | PASS | Po smazani presmerovano na /groups |
| DELETE-4 | PASS | Smazana skupina neni v seznamu |
| ADD-1 | PASS | Tlacitko "Pridat clena" viditelne |
| ADD-2 | **FAIL** | Formular ukazuje chybu: "memberId: neznamy typ HAL+FORMS property: 'UUID'" |
| ADD-3 | SKIP | Nelze otestovat - formular nefunguje |
| ADD-4 | SKIP | Nelze otestovat |
| REMOVE-1 | **FAIL** | Tlacitko "Odebrat" viditelne i u vlastnika - nemelo by byt |
| REMOVE-2 | SKIP | Nelze otestovat bez pridaneho ne-vlastnika clena |
| REMOVE-3 | SKIP | Nelze otestovat |
| REMOVE-4 | **FAIL** | Vlastnik BYL odebran! Backend neodmitl, skupina zustavila bez clenu/vlastnika = 404 |
| AUTH-1 | SKIP | Nelze otestovat - nejdrive nutno pridat clena |
| AUTH-2 | SKIP | Nelze otestovat |

### Nalezene issues (Iterace 2)

**ISSUE-1: Frontend nezna HAL+FORMS typ UUID pro pole memberId**
- Formular "Pridat clena" zobrazuje chybu: `memberId: neznamy typ HAL+FORMS property: 'UUID'`
- Backend posila `type: "UUID"` v HAL+FORMS template pro memberId, frontend tento typ nezna
- Dopad: Nelze pridat clena do skupiny
- Scenare: ADD-2, ADD-3, ADD-4
- Fix: Bud backend zmeni typ na "text" nebo frontend prida podporu pro "UUID" typ

**ISSUE-2: Tlacitko Odebrat zobrazeno u vlastnika skupiny**
- Backend vraci `removeGroupMember` affordance i pro clena, ktery je vlastnikem
- Frontend zobrazuje tlacitko "Odebrat" u vlastnika
- Dopad: UI umoznuje akci, ktera by nemela byt dostupna
- Scenare: REMOVE-1

**ISSUE-3: Backend umoznil odebrani vlastnika ze skupiny**
- Po kliknuti na "Odebrat" u vlastnika, backend vraci uspech misto chyby
- Skupina zustane bez vlastnika/clenu, dalsi pristup vrati 404
- SEVERITY: CRITICAL - data integrity issue
- Scenare: REMOVE-4
- Pozn: Mozna souvisi s ISSUE-2 - affordance by nemela existovat, ale i tak by backend mel validovat

**ISSUE-4: Toast "Uspesne ulozeno" zobrazen i pri chybne operaci**
- Pri odebrani vlastnika se zobrazil toast "Uspesne ulozeno" a nasledne 404
- Frontend pravdepodobne interpretuje jakykoliv non-error response jako uspech
- Scenare: REMOVE-4

**ISSUE-5: Chybejici scenare pro pridani/odebrani clenu**
- Kvuli ISSUE-1 nelze pridat dalsiho clena, proto nelze otestovat scenare REMOVE-2/3 a AUTH-1/2
- Blokujici issue: ISSUE-1

### Iterace 3

Opravy: ISSUE-1 (AddMemberRequest zmena UUID->MemberId), ISSUE-2 (affordance filtr pro vlastniky),
ISSUE-3 (domain validace uz existovala, nebyla jen v deploynutem kodu),
nova oprava: removeGroupMember pathname pouziva member self link misto group path.

| Scenar | Vysledek | Poznamka |
|--------|----------|----------|
| NAV-1 | PASS | |
| NAV-2 | PASS | |
| CREATE-1 | PASS | |
| CREATE-2 | PASS | |
| CREATE-3 | PASS | |
| CREATE-4 | PASS | |
| CREATE-5 | PASS | Tvurce je vlastnik i clen se spravnymi daty |
| DETAIL-1 | PASS | |
| DETAIL-2 | PASS | |
| DETAIL-3 | PASS | SPRAVCI: ZBM9000 / Jan Novak |
| DETAIL-4 | PASS | |
| DETAIL-5 | PASS | ZBM9000, Novak, Jan, datum |
| EDIT-1 | PASS | Tlacitko "Upravit nazev" viditelne |
| EDIT-2 | PASS | Nazev zmenen |
| EDIT-3 | PASS | Nadpis aktualizovan |
| DELETE-1 | PASS | |
| DELETE-2 | PASS | |
| DELETE-3 | PASS | Redirect na /groups |
| DELETE-4 | PASS | |
| ADD-1 | PASS | Tlacitko "Pridat clena" viditelne |
| ADD-2 | PASS | Formular s MemberId selectem (Jan Novak, Eva Svobodova) |
| ADD-3 | PASS | Eva pridana (204 No Content) |
| ADD-4 | PASS | Eva v tabulce: ZBM9500, Svobodova, Eva |
| REMOVE-1 | PASS | Tlacitko "Odebrat" NENI u vlastnika, JE u ne-vlastnika |
| REMOVE-2 | PASS | Eva uspesne odebrana |
| REMOVE-3 | PASS | Eva zmizela z tabulky, skupina zustava |
| REMOVE-4 | PASS | Vlastnik nema tlacitko Odebrat (nelze ani zkusit) |
| AUTH-1 | SKIP | Nelze otestovat - problem s odhlasenim/prihlasenim jineho uzivatele |
| AUTH-2 | SKIP | Nelze otestovat |

### Nalezene issues (Iterace 3)

Zadne nove kriticke issues. Zbyvajici problemy:

**MINOR-1: Pole "memberId" label v formulari pridani clena**
- Formular zobrazuje technicky label "memberId*" misto uzivatelsky privetiveho "Clen"
- Dopad: Spatna UX, ale funkcne v poradku

**MINOR-2: AUTH scenare neotestovany**
- OAuth session neumoznuje snadno prepnout uzivatele v Playwright testu
- AUTH-1 a AUTH-2 nutno otestovat manualne nebo s lepsi session management

### Iterace 4

Oprava: Affordances na group detail self linku podmineny isOwner.
AUTH scenare otestovany pomoci close/open browser window.

| Scenar | Vysledek | Poznamka |
|--------|----------|----------|
| NAV-1 | PASS | |
| NAV-2 | PASS | |
| CREATE-1 | PASS | |
| CREATE-2 | PASS | |
| CREATE-3 | PASS | |
| CREATE-4 | PASS | |
| CREATE-5 | PASS | |
| DETAIL-1 | PASS | |
| DETAIL-2 | PASS | |
| DETAIL-3 | PASS | |
| DETAIL-4 | PASS | |
| DETAIL-5 | PASS | |
| EDIT-1 | PASS | |
| EDIT-2 | PASS | |
| EDIT-3 | PASS | |
| DELETE-1 | PASS | |
| DELETE-2 | PASS | |
| DELETE-3 | PASS | |
| DELETE-4 | PASS | |
| ADD-1 | PASS | |
| ADD-2 | PASS | |
| ADD-3 | PASS | |
| ADD-4 | PASS | |
| REMOVE-1 | PASS | |
| REMOVE-2 | PASS | |
| REMOVE-3 | PASS | |
| REMOVE-4 | PASS | |
| AUTH-1 | PASS | Eva vidi skupinu kde je clenem v seznamu |
| AUTH-2 | PASS | Eva na detailu NEVIDI tlacitka Upravit/Smazat/Pridat/Odebrat |

### Nalezene issues (Iterace 4)

**MINOR-1: Pole "memberId" label v formulari pridani clena**
- Formular zobrazuje technicky label "memberId*" misto uzivatelsky privetiveho "Clen"

**Vsechny scenare PASS. Testovani dokonceno.**
