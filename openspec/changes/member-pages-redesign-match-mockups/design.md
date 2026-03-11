## Context

Frontend zobrazuje member detail, registraci a edit formuláře v 1-sloupcovém layoutu, který neodpovídá schváleným Pencil mockupům (`pencil/klabis-members.pen`). Mockupy definují:

- **2-sloupcový layout** pro detail a edit: vlevo osobní/kontakt/adresa, vpravo doplňkové/doklady
- **3 view varianty** pro detail stránku podle role a vztahu k profilu
- **Action bar dole** pro edit a registraci (místo aktuálního umístění nahoře)
- **Akční tlačítka s ikonami** v admin detailu

Navíc backend aktuálně vrací v `GET /api/members/{id}` PATCH template se všemi poli `UpdateMemberRequest` bez ohledu na oprávnění volajícího. Aplikační vrstva (`updateMember`) teprve při PATCHi rozhodne, která pole se smí změnit. Výsledkem je, že frontend musí sám odhadovat, jaký "pohled" zobrazit.

### Mockup reference

| Varianta | Pencil node | Popis |
|---|---|---|
| Members - Details (Member) | `xmQFI` | Cizí člen — pouze kontakt + adresa, žádné akce |
| Members - SelfDetails (Member) | `7Q3Kh` | Vlastní profil — plná data, tlačítka "Členské příspěvky" + "Upravit profil" |
| Members - Details (Admin) | `kVhxA` | Admin detail — plná data, 4 akční tlačítka s ikonami |
| Members - AdminEdit (Admin) | `P5n4i` | Admin editace — 2 sloupce, admin badge, tlačítka dole |
| Members - SelfEdit (Member) | `LdRj4` | Vlastní editace — osobní pole read-only, kontakt+adresa editovatelné |
| Members - Registration (Admin) | `NIoA4` | 2 sloupce, tlačítka dole (Zrušit + Registrovat člena) |

Pencil soubor: `pencil/klabis-members.pen`. Implementující agent načte detaily přes Pencil MCP:
- `get_screenshot(nodeId)` — vizuální náhled každé varianty
- `batch_get(nodeIds, readDepth=4)` — detaily layoutu a komponent

## Goals / Non-Goals

**Goals:**
- Backend vrátí PATCH template pouze s poli dostupnými pro volajícího uživatele
- Frontend zobrazí sekce a pole pouze na základě obsahu template a response — žádná logika detekce role na frontendu
- Přesná shoda vizuálního výstupu s Pencil mockupy pro všechny view varianty
- 2-sloupcový layout pro detail a edit stránky
- Action bar dole pro edit a registraci

**Non-Goals:**
- Změny v autorizaci PATCH operace samotné (ta zůstává jak je)
- Změny v routování
- MembersPage (seznam)
- PermissionsDialog implementace
- MembershipFees funkcionalita (tlačítko může být placeholder)

## Decisions

### Backend: podmíněný PATCH template podle oprávnění

`MemberController.getMember()` přidá do response PATCH template (affordance) podmíněně:

| Volající | Template pole |
|---|---|
| Má MEMBERS:MANAGE | Všechna pole `UpdateMemberRequest` |
| Je vlastníkem profilu (memberId z JWT = id z path) | `email`, `phone`, `address`, `dietaryRestrictions` |
| Cizí člen bez MEMBERS:MANAGE | Template se nepřidá vůbec |

Implementace: `getMember()` dostane `@CurrentUser CurrentUserData` a na základě `currentUser.hasAuthority(MEMBERS_MANAGE)` a porovnání `currentUser.memberId` s `id` z path zvolí affordanci. Vytvoří se dvě affordance metody nebo jeden overloaded helper, který vrátí správně ořezaný `UpdateMemberRequest`.

Alternativa zvažována: nový endpoint pro "co smím editovat" — zamítnuto jako over-engineering, HAL template je správné místo.

### Frontend: HAL-driven rendering bez detekce role

Frontend zobrazuje sekci pouze pokud existuje odpovídající data v response. Edit formulář zobrazuje pouze pole z template. Žádný `if (isAdmin)` na frontendu.

View varianty se přirozeně odvodí:
- **Cizí člen**: `_templates.default` neexistuje → žádné edit tlačítko; osobní data nejsou v response (backend je nevrátí) → sekce Osobní údaje se nezobrazí
- **Vlastní profil**: template s omezenými poli → edit tlačítko + omezené formulářové sekce
- **Admin**: template se všemi poli → edit tlačítko + plné formulářové sekce + admin badge

> **Poznámka**: Backend aktuálně vrací všechna osobní data i cizímu členovi (MemberDetailsResponse je plný). Filtrování dat v response je samostatná věc — tato změna řeší pouze template pole. Frontend pro "cizí člen" view skryje sekce na základě absence template (ne absence dat).

### 2-sloupcový layout

CSS grid `grid grid-cols-1 lg:grid-cols-2 gap-10` pro obsah detailu a editaci:
- **Levý sloupec:** Osobní údaje, Kontakt, Adresa
- **Pravý sloupec:** Doplňkové informace, Doklady a licence

Pro registraci: pravý sloupec obsahuje pouze Doplňkové informace (bez dokladů — dle mockupu `NIoA4`).

### Action bar pozice

Edit a registrace: action bar `<div class="flex justify-end gap-3">` přesunut na konec obsahu. Detail stránka: akce v hlavičce (dle mockupů).

## Risks / Trade-offs

- **[Risk] Backend nefiltruje data response pro cizího člena** → Frontend zobrazí jen kontakt+adresu na základě absence edit template, ale data v response jsou plná. Citlivá data (rodné číslo) jsou already masked. Mitigation: Toto je known limitation, oddělená issue od layout redesignu.
- **[Risk] `currentUser.memberId` nemusí odpovídat `id` z path** → Member ID a User ID jsou různé typy, ale v 1:1 vztahu. Porovnání přes `currentUser.memberId().uuid().equals(id)`. Ověřit v testech.
- **[Risk] Playwright validace** → Vyžaduje běžící backend + přihlášené uživatele. Implementující agent použije dev server na `localhost:3000`.
