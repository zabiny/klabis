# Smoke QA Testing — Klabis

Testovací scénáře pokrývají hlavní funkcionality všech komponent projektu.
Testovací uživatelé: Admin `ZBM9000/password`, Člen `ZBM9500/password`.

---

## Scénáře

### AUTH — Autentizace

**AUTH-1** — Login admina
- Akce: Naviguj na `http://localhost:3000`, vyplň `ZBM9000` / `password`, klikni „Přihlásit se"
- Assert: URL se změní na `http://localhost:3000/` (nebo dashboard route), stránka neobsahuje login formulář, navigační menu je viditelné

**AUTH-2** — Odhlášení
- Akce: Po přihlášení klikni tlačítko „Odhlásit"
- Assert: Prohlížeč je přesměrován na login stránku (`https://localhost:8443/login`), navigační menu zmizelo, opětovný pokus o přístup na chráněnou stránku vrátí znovu na login

**AUTH-3** — Login řadového člena
- Akce: Přihlaš se jako `ZBM9500` / `password`
- Assert: Přihlášení proběhne úspěšně, v menu NENÍ sekce „Administrace", nejsou dostupné odkazy na správu členů ani správu událostí

---

### NAV — Navigace

**NAV-1** — Admin — dvousekční menu
- Akce: Přihlaš se jako admin, prohlédni postranní menu
- Assert: Menu obsahuje sekci „Hlavní" (nebo ekvivalent) se standardními položkami (Kalendář, Události, Skupiny…) A sekci „Administrace" se správou členů

**NAV-2** — Člen — omezené menu
- Akce: Přihlaš se jako člen `ZBM9500`, prohlédni menu
- Assert: Menu neobsahuje sekci „Administrace" ani žádné administrativní položky (Správa členů, Oprávnění…)

**NAV-3** — Mobilní navigace
- Akce: Přihlaš se jako admin, zmenši viewport na mobilní rozlišení (375 × 812 px)
- Assert: Spodní navigační lišta je viditelná, zobrazuje pouze každodenní destinace (Kalendář, Události…), administrativní položky v ní NEJSOU

---

### MEMBERS — Správa členů (čtení)

**MEM-1** — Seznam členů
- Akce: Admin → naviguj na seznam členů
- Assert: Tabulka/seznam zobrazuje alespoň jednoho člena, každý řádek obsahuje jméno a registrační číslo (formát XXXYYSS)

**MEM-2** — Detail člena
- Akce: Admin → klikni na člena v seznamu
- Assert: Detail zobrazuje: celé jméno, datum narození, pohlaví, státní příslušnost, adresu; pole jsou čitelně popsána

**MEM-3** — Akční tlačítka na detailu
- Akce: Admin → otevři detail člena
- Assert: Na stránce jsou viditelná tlačítka/odkazy: „Upravit" (editace profilu) a odkaz na správu oprávnění

**MEM-4** — Dialog oprávnění
- Akce: Admin → klikni na odkaz oprávnění v detailu člena
- Assert: Otevře se modální dialog s přepínači (toggle switches) pro všechny role (MEMBERS:MANAGE, EVENTS:MANAGE, GROUPS:TRAINING…), dialog má tlačítka „Uložit" a „Zrušit"

---

### MEMBERS — Aktivní operace

**MEM-W1** — Změna oprávnění
- Akce: Admin → otevři dialog oprávnění u testovacího člena → přepni libovolnou roli (která nemá vliv na přístup ke zbytku testu) → klikni „Uložit"
- Assert: Dialog se zavře, zobrazí se notifikace o úspěchu; znovu otevři dialog — přepínač zůstal ve změněné poloze
- Cleanup: Vrať přepínač zpět do původního stavu

**MEM-W2** — Editace profilu člena
- Akce: Admin → otevři detail člena → klikni „Upravit" → změň telefonní číslo (nebo jinou volitelnou hodnotu) → ulož
- Assert: Po uložení se zobrazí detail s novou hodnotou pole; formulář se zavřel / navigace zpět na detail proběhla

---

### EVENTS — Správa událostí (čtení)

**EVT-1** — Seznam událostí
- Akce: Admin → naviguj na seznam událostí
- Assert: Seznam obsahuje události, každý řádek zobrazuje název, datum a stav (Draft / Active / Finished / Cancelled)

**EVT-2** — Akce dle stavu události
- Akce: Admin → prohlédni akce dostupné u událostí různých stavů v seznamu
- Assert: Událost ve stavu Draft/Active má akci „Editovat"; akce „Zrušit" je dostupná pouze pro aktivovatelné stavy; u ORIS událostí je viditelná akce „Synchronizovat"

**EVT-3** — Kategorie na detailu události
- Akce: Admin → otevři detail události, která má přiřazené kategorie
- Assert: Kategorie jsou zobrazeny jako vizuální pills/tags (barevné štítky), ne jako prostý text v seznamu

---

### EVENTS — Aktivní operace

**EVT-W1** — Vytvoření nové události
- Akce: Admin → klikni „Vytvořit událost" → vyplň název (např. „Smoke test závod"), datum konání → ulož
- Assert: Aplikace přejde na detail nové události nebo zpět na seznam; nová událost je v seznamu viditelná ve stavu **Draft**

**EVT-W2** — Editace události
- Akce: Admin → otevři existující událost ve stavu Draft → klikni „Editovat" → změň název → ulož
- Assert: Detail události zobrazuje aktualizovaný název; předchozí název se nikde nezobrazuje

**EVT-W3** — Přidání kategorie k události
- Akce: Admin → otevři detail události → přidej kategorii (např. „M21") → ulož/potvrď
- Assert: Nová kategorie se zobrazí jako pill v sekci kategorií na detailu události

---

### CALENDAR — Kalendář (čtení)

**CAL-1** — Měsíční pohled
- Akce: Admin → naviguj na Kalendář
- Assert: Zobrazuje se aktuální měsíc, položky jsou seřazeny chronologicky, každá položka má alespoň název a datum

**CAL-2** — Event-linked položka je read-only
- Akce: Admin → najdi v kalendáři položku navázanou na událost (event-linked)
- Assert: U položky NEJSOU tlačítka „Editovat" ani „Smazat"; je viditelný odkaz na příslušnou událost

---

### CALENDAR — Aktivní operace

**CAL-W1** — Vytvoření manuální položky
- Akce: Admin → klikni „Přidat položku" → vyplň název (např. „Smoke test schůzka"), datum, volitelný popis → ulož
- Assert: Nová položka se zobrazí v kalendáři ve správném datu; popis je viditelný (nebo rozbalitelný)

**CAL-W2** — Editace manuální položky
- Akce: Admin → klikni „Editovat" u manuální kalendářní položky → změň název → ulož
- Assert: Položka v kalendáři zobrazuje aktualizovaný název

**CAL-W3** — Smazání manuální položky
- Akce: Admin → klikni „Smazat" u manuální kalendářní položky → potvrď smazání
- Assert: Položka zmizí z kalendáře; ostatní položky jsou nedotčeny

---

### GROUPS — Skupiny

**GRP-1** — Seznam skupin
- Akce: Admin → naviguj na seznam skupin
- Assert: Seznam zobrazuje skupiny různých typů (Tréninková, Rodinná, Volná), každá skupina má viditelný název a typ

**GRP-2** — Detail skupiny
- Akce: Admin → klikni na skupinu v seznamu
- Assert: Detail zobrazuje seznam členů skupiny s jejich rolemi (Trenér / Rodič / Vlastník); role jsou vizuálně odlišeny od běžných členů

---

### EVENT-REG — Registrace na události

**REG-1** — Viditelnost tlačítka přihlášení
- Akce: Přihlaš se jako člen `ZBM9500` → otevři detail aktivní události
- Assert: Na stránce je viditelné tlačítko / akce „Přihlásit se na závod" (nebo ekvivalent); neaktivní nebo minulé události toto tlačítko nemají

**REG-W1** — Přihlášení na událost
- Akce: Člen `ZBM9500` → klikni „Přihlásit se" na aktivní události → vyplň požadované údaje (SI číslo, kategorie pokud existují) → potvrď
- Assert: Zobrazí se potvrzení přihlášky; v detailu události (nebo sekci „Moje přihlášky") je člen uveden jako přihlášený; tlačítko „Přihlásit se" zmizelo nebo se změnilo na „Odhlásit se"
