# Klabis Frontend Mockup

Statický HTML/CSS/JavaScript mockup pro členskou sekci orientačního běhu Klabis.

## Design Concept

**Téma**: Sportovní elegance inspirovaná orientačním během

**Barevná paleta**:
- Lesní zelená (`#2d5a27`, `#4a7c42`) - primary, reference k lesům a mapám
- Oranžová (`#ff6b35`, `#ff8c61`) - accent, reference k kontrolním bodům na mapě
- Krémová (`#faf8f5`) - background
- Šedá (`#e8e4dc`) - borders, secondary elements

**Typografie**:
- **Outfit** - display font pro nadpisy (moderní, geometrický)
- **DM Sans** - body font pro text (čitelný, přátelský)

**Vizuální prvky**:
- Subtilní mapové vzory v pozadí
- Čisté karty s jemnými stíny
- Hover efekty na interaktivních prvcích
- Responzivní design (mobile-first)

## Stránky

| Soubor | Popis |
|--------|-------|
| `index.html` | Dashboard - přehled akcí, novinek, notifikací |
| `calendar.html` | Kalendář - osobní kalendář uživatele |
| `events.html` | Seznam událostí - závody, tréninky, soustředění |
| `event-detail.html` | Detail události s formulářem pro přihlášení |
| `members.html` | Seznam členů s filtry a vyhledáváním |
| `profile.html` | Uživatelský profil s editací osobních údajů |

## Funkcionalita

### Dashboard (`index.html`)
- Statistické karty (přihlášené akce, zůstatek účtu, uzávěrky, zprávy)
- Nadcházející akce s filtry
- Pozvánky do skupin
- Rychlé akce
- Novinky v klubu

### Kalendář (`calendar.html`)
- Měsíční pohled s událostmi
- Filtry typů událostí (závody, tréninky, soustředění, uzávěrky)
- Navigace mezi měsíci
- Legendy pro barvy událostí
- Postranní panel s událostmi v měsíci

### Události (`events.html`)
- Grid karet událostí
- Filtry typů a stavů
- Search
- Stavové badge (uzávěrka, plno, přihlášen)

### Detail události (`event-detail.html`)
- Kompletní informace o události
- Seznam kategorií
- Přihlášení účastníci
- **Formulář pro přihlášení**:
  - Výběr kategorie
  - Číslo čipu SI
  - Společná doprava
  - Společné ubytování
  - Dopňkové služby
  - Interní poznámka
  - Poznámka do ORIS
  - Kalkulace ceny

### Členové (`members.html`)
- Tabulka členů s avatary
- Filtry (skupina, status)
- Řazení
- Search
- Export do CSV
- Tisk
- Paginace

### Profil (`profile.html`)
- Základní informace o uživateli
- Tréninková skupina
- **Formulář pro editaci osobních údajů**:
  - Jméno, příjmení
  - Datum narození, rodné číslo
  - Národnost, číslo OP
  - Adresa
  - Email, telefon
  - Zákonný zástupce
  - Číslo čipu, číslo účtu
  - Řidičský průkaz, diety
- **Nastavení soukromí**:
  - Viditelnost emailu
  - Viditelnost telefonu
  - Viditelnost adresy

## Technologie

- **HTML5** - sémantická struktura
- **Tailwind CSS** (CDN) - utility-first CSS framework
- **Vanilla JavaScript** - interaktivita formulářů, filtry
- **Google Fonts** - Outfit + DM Sans

## Responzivita

- Mobile-first přístup
- Breakpoints: `sm` (640px), `md` (768px), `lg` (1024px)
- Collapsible sidebar na menších obrazovkách
- Grid layout se adaptuje na velikost obrazovky

## Jak spustit

1. Otevřete jakýkoliv soubor v prohlížeči
2. Nebo použijte local server:
   ```bash
   cd frontend-mockup
   python3 -m http.server 8000
   ```
   Pak navštivte `http://localhost:8000`

## Další stránky k implementaci

- `trainings.html` - Týdenní plán tréninků
- `groups.html` - Správa uživatelských skupin
- `news.html` - Novinky a komunikace
- `finances.html` - Přehled financí a plateb
- `member-detail.html` - Detail člena
- `settings.html` - Globální nastavení aplikace

## Poznámky

- Všechny formuláře mají demo odeslání s alertem
- Search a filtry jsou vizuální (nefungují skutečně)
- Data jsou hardcoded pro demo účely
- Ikony jsou SVG inline (Heroicons)
- Avatary používají iniciály uživatelů
