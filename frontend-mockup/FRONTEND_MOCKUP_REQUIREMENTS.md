# Požadavky na Frontend Mockup - Klabis

Dokument vytvořen z GitHub issues pro milníky **Core** a **MVP**.

---

## Obsah

1. [Správa členů](#správa-členů)
2. [Uživatelské skupiny](#uživatelské-skupiny)
3. [Události a přihlášky](#události-a-přihlášky)
4. [Pravidelné tréninky](#pravidelné-tréninky)
5. [Financování](#financování)
6. [Kalendář](#kalendář)
7. [Notifikace](#notifikace)
8. [Profil a nastavení](#profil-a-nastavení)
9. [Veřejný web](#veřejný-web)

---

## Správa členů

### Založení nového člena (#3, #5)
- Formulář s osobními údaji:
  - Jméno, příjmení (povinné)
  - Registrační číslo (automaticky generované nebo ručně pro přestupy #262)
  - Číslo čipu (nepovinné)
  - Datum narození (povinné)
  - Rodné číslo (povinné pro ČR)
  - Národnost (povinné)
  - Pohlaví (povinné, odvozeno z rodného čísla)
  - Adresa (ulice, PSČ, město)
  - Email (alespoň jeden povinný)
  - Telefon (alespoň jeden povinný)
  - Zákonný zástupce (pro nezletilé)
  - Číslo bankovního účtu (nepovinné)

- Výběr tréninkové skupiny při založení (#240)
- Výběr rodinné skupiny při založení (#240)
- Import z ORIS (#5)

### Editace osobních údajů (#4)
- Člen edituje své údaje
- Validace: alespoň jeden email a telefon
- Synchronizace s ORIS/CUS

### Seznam členů
- Tabulka členů s filtry (#25)
- Zobrazení kontaktních údajů dle oprávnění
- Detail člena s kompletními údaji

### Pozastavení členství (#14)
- Zrušení členství v oddíle
- Kontrola finančního zůstatku
- Synchronizace s ORIS/CUS

---

## Uživatelské skupiny

### Vytvoření skupiny (#119)
- Jednoznačný název skupiny
- Popis skupiny
- Emailová adresa skupiny
- Oprávnění skupiny (tabulka vedoucí/člen):
  - Vidět finance členů
  - Upravovat finance členů
  - Vidět přihlášky a omluvenky
  - Editovat přihlášky a omluvenky
  - Vidět osobní info
  - Editovat osobní info
  - Pozvat do skupiny
  - Psát skupinové novinky bez schválení

### Správa skupiny
- Vedoucí může pozvat nové členy (#231)
- Vedoucí může odebrat členy (#232)
- Člen může zrušit své členství (#233)
- Zrušení pozvánky do skupiny (#241)
- Nastavení dalších vedoucích (#118)

### Přijetí pozvánky do skupiny (#120)
- Zobrazení názvu skupiny, seznamu vedoucích
- Seznam oprávnění skupiny
- Možnost přijmout/odmítnout s důvodem

### Tréninkové skupiny
- Trenér je vedoucím příslušné skupiny (#117)
- Člen vidí svou tréninkovou skupinu (#116)
- Kontakty na trenéry (#56)

---

## Události a přihlášky

### Typy událostí
- Závody
- Tréninky
- Tábory/Soustředění
- Další akce

### Informace o události
- Název
- Datum konání
- Uzávěrka přihlášek
- Místo
- Popis
- Cena (předběžná)
- Skupiny pro které je určena (#74)
- Vedoucí akce (#83)

### Přihlášení na akci (#26)
- Výběr kategorie
- Číslo čipu (editovatelné)
- Společná doprava (ano/ne) (#86)
- Společné ubytování (ano/ne) (#85)
- Dopňkové služby z ORIS (#102)
- Interní poznámka
- Poznámka do ORIS
- Přihlášení nečlena klubu "+1" (#97)

### Správa přihlášek
- Odhlášení z akce (#31)
- Změna přihlášky (#92)
- Absence po uzávěrce (#96)
- Vedoucí odhlašuje účastníky (#34)
- Seznam přihlášených (#30)
- Seznam na dopravu (#84)
- Seznam na ubytování (#105)

### Synchronizace s ORIS
- Synchronizace na tlačítko (#98)
- Zpětná synchronizace (#101)
- Notifikace o změnách (#90)

---

## Pravidelné tréninky (#234)

### Funkce pro trenéra
- Vytvářet a editovat pravidelné tréninky
- Definovat tréninky alespoň 3 týdny dopředu
- Automatické vytváření tréninků na další týdny dle vzoru

### Údaje o tréninku
- Datum
- Název
- Čas
- Místo
- Skupiny pro které je určen

### Zobrazení
- Týdenní plán pro moji skupinu (#48)
- Filtrace dle tréninkových skupin

---

## Financování

### Přehled financí
- Stav finančního účtu (#68)
- Zůstatek rodinné skupiny (#128)
- Historie plateb
- Rezervace za přihlášené akce

### Členské příspěvky
- Typ členského příspěvku (#126)
- Přehled o zaplacených/nezaplatených (#78)
- Potvrzení o příspěvcích (#124)
- Automatický import plateb (#127)

### Platby za akce
- Cena akce (#80)
- Slevy pro členy/skupiny (#107)
- Dotace oddílu (#108)
- Postezování částky za akci (#76)
- Převod financí jinému členu (#61)

### Hromadné operace
- Hromadná změna typu příspěvku (#125)
- Strhávání/připisování částek (#77, #82)

---

## Kalendář

### Osobní kalendář (#110)
- Závody na které jsem přihlášen (termin konání)
- Termíny přihlášek pro události mých skupin
- Termíny konání událostí mých skupin (tréninky)

### Filtry
- Filtrovat podle různých kritérií (#109)
- Zapínání/vypínání kategorií

### Přehled akcí
- Přehled akcí na 10 dní dopředu (#81)
- Export kalendáře (Google, Outlook) (#111)

---

## Notifikace

### Typy notifikací
- Notifikace při záporném zůstatku (#27)
- Notifikace o změnách v ORIS (#90)
- Notifikace o nových akcích (#91)
- Notifikace o uzávěrkách přihlášek (#89)

### Nastavení notifikací (#28)
- Výběr jaké notifikace chci emailem
- Výběr jaké notifikace chci ve feedu

---

## Profil a nastavení

### Osobní údaje (#4)
- Editace osobních údajů
- Číslo OP a platnost
- Diety
- Skupina řidičského oprávnění
- Zdravotnický kurz + platnost
- Trenérská licence + platnost

### Viditelnost údajů (#24)
- Kdo má přístup k informacím o mně
- Nikdo/Všichni/Členové skupin/Konkrétní členové
- Adresa, email, telefon - nastavení viditelnosti

### ORIS integrace
- Licence/žebříčky z ORISu (#10, #7)
- Automatické načítání licencí

---

## Veřejný web

### Informace o klubu
- Co je orientační běh (#51)
- Jak se stát členem (#49)
- Kontakty na vedení (#50)
- Odkazy na CSOS, ORIS, BZL (#41)
- Fakturační údaje (#42)
- Sponzoři (#44)

### Pro veřejnost
- Areály pevných kontrol (#52)
- Mapy a informace
- Informace o závodech (#46)
- Život oddílu (#43)

### Pro členy
- Informace pro nováčky (#40)
- Oddílový časopis (#39)
- Fotky z akcí (#38)
- Novinky a dění v klubu (#59, #58)
- Wiki (pokyny, info o skupinách) (#121)

---

## Stránky aplikace

### Hlavní navigace
1. **Dashboard** - přehled akcí, novinek, notifikací
2. **Kalendář** - osobní kalendář uživatele
3. **Členové** - seznam a správa členů
4. **Tréninky** - pravidelné tréninky (týdenní plán)
5. **Události** - závody, tábory, akce
6. **Skupiny** - uživatelské skupiny
7. **Novinky** - aktuality a komunikace
8. **Finance** - přehled financí a plateb
9. **Profil** - uživatelský profil a nastavení
10. **Administrace** - nastavení systému (jen admin)

### Podstránky
- Detail události
- Detail člena
- Detail skupiny
- Přihláška na akci
- Editace osobních údajů
- Nastavení notifikací

---

## UI/UX Poznámky

### Design
- Moderní, čistý design
- Responzivní (mobile-friendly)
- Použití Tailwind CSS pro styling
- Temný/světlý mód (volitelně)

### Informace
- Jasné vizuální rozlišení rolí a oprávnění
- Indikátory stavů (uzávěrka, plno, volno, atd.)
- Notifikace a zprávy

### Formuláře
- Validace vstupů
- Nápověda a tooltips
- Clear error messages
- Progress indikátory (přihlášky)

### Tabulky
- Filtry a vyhledávání
- Řazení
- Export do CSV (#123)
- Tisk

---

## Role a oprávnění

### Role
- **Administrátor** - plná správa systému
- **Vedoucí skupiny** - správa skupiny
- **Trenér** - správa tréninků
- **Přihlašovatel** - správa přihlášek na závody
- **Finančník** - správa financí
- **Organizátor akce** - správa akcí
- **Vedoucí akce** - vedení akce
- **Člen klubu** - základní práva
- **Hostující člen** - omezená práva
- **Veřejnost** - pouze veřejný web
