# Klabis UI Mockup

Tento adresář obsahuje statické HTML/UI mockupy pro systém Klabis (Membership Management System), které demonstrují
možné uživatelské rozhraní na základě aktuálně dostupných backend API.

## 🔥 NOVINKA: Backend API Integration

Mockup je nyní propojen s reálným Klabis backend API pomocí **OAuth2 Authorization Code** flow!

### Co funguje s backendem:

- ✅ **OAuth2 Authorization Code autentizace** - kompletní OAuth2 flow s autorizačním serverem
- ✅ **Token management** - automatický refresh tokenů
- ✅ **Seznam členů** - načítá reálná data z `GET /api/members`
- ✅ **Paginace a řazení** - podporováno backendem
- ✅ **Error handling** - správné zobrazení chyb z API

### Jak začít:

1. Spusťte backend: `cd klabis-backend && ./gradlew bootRun`
2. Otevřete mockup: https://localhost:8443/mock-login.html
3. Zadejte Client ID (výchozí: mock-web)
4. Budete přesměrováni na přihlašovací stránku autorizačního serveru
5. Přihlašte se uživatelským účtem (admin / admin123)
6. Prohlížejte reálná data z databáze!

**Poznámka:** UI je servírováno automaticky z backendu (Spring Boot složka `src/main/resources/static/`), není potřeba
žádný další server.

**Viz [BACKEND_INTEGRATION.md](BACKEND_INTEGRATION.md) pro detaily.**

---

## Přehled

Mockupy jsou vytvořeny jako moderní webová aplikace s plně responzivním designem a interaktivními prvky pomocí
JavaScriptu. Jsou psány v češtině, což odpovídá cílové skupině uživatelů.

## Soubory

### Hlavní stránky

- **index.html** - Hlavní dashboard s přehledem statistik a rychlých akcí
- **mock-login.html** - 🔥 OAuth2 Authorization Code připojení k backend API
- **members.html** - 🔥 Seznam členů z backend API (paginace, řazení)
- **member-details.html** - Detail člena s kompletními informacemi (TODO: backend)
- **member-form.html** - Formulář pro vytvoření nového člena (TODO: backend)
- **permissions.html** - Správa uživatelských oprávnění (TODO: backend)

### Sdílené soubory

- **styles.css** - Společné styly pro všechny stránky
- **app.js** - Společné JavaScript funkce a utility s API error handling
- **api-client.js** - 🔥 OAuth2 Authorization Code API klient

### Dokumentace

- **README.md** - Tento soubor
- **BACKEND_INTEGRATION.md** - Detailní dokumentace propojení s backendem

## Funkcionality

### 1. Autentizace a Autorizace

#### Login stránka (`mock-login.html`) - 🔥 **AKTUÁLNĚ: OAuth2 Authorization Code**

- **OAuth2 Authorization Code flow** - bezpečný OAuth2 standardní flow
- Backend URL: `https://localhost:8443` (auto-detected)
- Client ID: `mock-web` (z V002 migration)
- Redirect URI: `https://localhost:8443/callback.html`
- Přesměrování na autorizační server pro přihlášení
- Automatická výměna authorization code za access token
- Token refresh při expiraci
- Odhlašování (vymaže token a session)

**Login proces:**

1. Zadáte Client ID (mock-web)
2. Budete přesměrováni na autorizační server
3. Přihlašte se uživatelským jménem a heslem (např. admin / admin123)
4. Autorizační server vás přesměruje zpět s authorization code
5. Callback page vymění authorization code za access token a refresh token
6. Tokeny uloženy v sessionStorage
7. Session uložena v sessionStorage

### 2. Správa Členů

#### Seznam členů (`members.html`) - 🔥 **PROPojENO K BACKEND API**

- **Endpoint**: `GET /api/members`
- **Paginace**: Podporováno backendem (page, size parametry)
- **Řazení**: Podporováno backendem (sort parametr)
    - Pole: firstName, lastName, registrationNumber
    - Směr: asc, desc
- **Loading states**: Spinner během načítání
- **Error handling**: Zobrazení chyb z API (401, 403, 404, etc.)
- Zobrazení stavu člena (aktivní/neaktivní)
- Navigace na detail člena

**API Response Format:**

```json
{
  "_embedded": {
    "members": [...]
  },
  "page": {
    "size": 10,
    "totalElements": 5,
    "totalPages": 1,
    "number": 0
  }
}
```

#### Detail člena (`member-details.html`)

- Kompletní osobní údaje
- Adresa
- Kontaktní údaje
- Rodné číslo (pouze pro české státní příslušníky)
- Dodatečné údaje (číslo čipu, bankovní účet)
- Údaje zákonného zástupce (pro nezletilé)
- Auditní informace (kdo a kdy vytvořil/upravil)

#### Formulář člena (`member-form.html`)

- **Povinná pole**:
    - Jméno, příjmení
    - Datum narození
    - Státní příslušnost
    - Pohlaví
    - Adresa (ulice, město, PSČ, stát)
    - Alespoň jeden e-mail a jeden telefon
- **Podmíněná pole**:
    - Rodné číslo - pouze při české státní příslušnosti
    - Údaje zákonného zástupce - automaticky pro osoby mladší 18 let
- **Volitelná pole**:
    - Číslo čipu
    - Číslo bankovního účtu
- Validace v reálném čase
- Welcome email po úspěšném vytvoření

### 3. Správa Oprávnění (`permissions.html`)

#### Dostupná oprávnění

- `MEMBERS:CREATE` - Vytvářet nové členy
- `MEMBERS:READ` - Prohlížet členy
- `MEMBERS:UPDATE` - Upravovat členy
- `MEMBERS:DELETE` - Mazat členy
- `MEMBERS:PERMISSIONS` - Spravovat oprávnění uživatelů

#### Funkce

- Zobrazení všech uživatelů systému
- Editace oprávnění přes modal okno
- **Business Rule - Lockout Prevention**:
    - Systém brání odebrání `MEMBERS:PERMISSIONS` od posledního uživatele, který ho má
    - Varování před uzamčením správy oprávnění
    - Validace před uložením změn
- Auditní logování změn oprávnění

### 4. HATEOAS a Hypermedia

Všechny API odpovědi používají HAL+FORMS formát:

- `_links` pro navigaci (self, collection, edit)
- `_templates` pro dostupné akce
- Affordances pro základní CRUD operace

## Technické detaily

### Styly

- Moderní CSS s CSS variables pro theming
- Flexbox a CSS Grid pro layout
- Responzivní design (mobile-first)
- Barvy: Primary indigo (#4F46E5), success green, warning orange, danger red

### JavaScript

- Vanilla JavaScript bez frameworků
- localStorage pro simulaci session
- Interaktivní komponenty:
    - Paginace
    - Řazení tabulek
    - Filtry a vyhledávání
    - Modální okna
    - Form validation
    - Toast notifikace

### Validace

- Klientská validace formulářů
- Real-time feedback
- Vizualizace chyb
- Czech birth number (rodné číslo) validace
- Email a telefon validace

## Mapování na OpenSpec specifikace

### Auth Specification

- ✅ OAuth2 autentizace (Spring Authorization Server)
- ✅ JWT access token (15 min TTL)
- ✅ Refresh token (30 day TTL)
- ✅ Role-Based Access Control (ROLE_ADMIN, ROLE_MEMBER)
- ✅ Authority-based autorizace (MEMBERS:CREATE, atd.)
- ✅ Bootstrap admin user (admin)
- ✅ Audit trail (created_by, modified_by)

### Members Specification

- ✅ Member Creation API s HAL+FORMS
- ✅ Povinné osobní údaje
- ✅ Generování registračního čísla (ZBM formát)
- ✅ Podmíněné pole "rodné číslo" pro českou národnost
- ✅ Kontaktní informace (alespoň 1 email + 1 telefon)
- ✅ Zákonný zástupce pro nezletilé (< 18 let)
- ✅ Volitelná pole (chip number, bank account)
- ✅ Welcome email
- ✅ List All Members s paginací a řazením
- ✅ Get Member by ID s kompletními detaily
- ✅ ISO-8601 date serialization

### User Permissions Specification

- ✅ Get User Permissions API
- ✅ Update User Permissions API
- ✅ Prevent Permission Management Lockout
- ✅ Permission Change Audit Trail
- ✅ Valid Authorities (MEMBERS:CREATE, READ, UPDATE, DELETE, PERMISSIONS)

## Použití

1. Otevřete `mock-login.html` v prohlížeči
2. Přihlaste se pomocí demo údajů (viz login stránka)
3. Prozkoumejte jednotlivé stránky a funkce

## Poznámky

- Toto jsou **statické mockupy** - data jsou uložena v JavaScriptu
- V reálné aplikaci by volaly backend API
- Všechny interakce jsou simulované
- UX/UI je připraveno pro integraci se Spring Boot backendem

## Budoucí rozšíření

Možné doplňkové funkce pro další verze:

- Editace existujících členů
- Deaktivace/aktivace členů
- Upload profilových fotek
- Export dat (CSV, PDF)
- Pokročilé filtry a reporting
- Historie změn člena
- Multi-language support (EN/CZ)
- Dark mode
- Pokročilé auditní logy

## Licence

Tento UI mockup je součástí projektu Klabis a slouží jako referenční implementace pro frontend vývoj.
