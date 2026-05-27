> **Související GitHub issue:** [#274 Úrovně členských příspěvků a spoluúčast na startovném](https://github.com/zabiny/klabis/issues/274)

## Why

Klub potřebuje rozlišit, jakou částku za závody členové doplácejí z vlastní kapsy — různí členové (mládež, dospělí, závodníci na ranking, podporovaní reprezentanti) mají historicky různé podmínky. Dnes klub tuto logiku řeší ručně mimo systém, což blokuje automatické generování vyúčtování na účtu člena a vede k chybám. Současně nejsou v systému evidovány ani roční členské příspěvky.

## What Changes

- Zavést koncept **úrovně členského příspěvku** (membership fee level) jako konfigurovatelnou entitu (např. "Základ", "Mládež", "Reprezentace")
- Každá úroveň definuje:
  - **Roční členský poplatek** (částka v Kč za kalendářní rok)
  - **Sadu pravidel pro spoluúčast na startovném** — každé pravidlo říká, kolik člen za daný typ závodu doplácí
- Pravidla pro spoluúčast jsou rozlišena podle **typu závodu** (event-type) a **žebříčku/série** závodu a mohou mít formu:
  - **Procento** ze základní ceny závodu (např. 50 % oblastní, 100 % celostátní)
  - **Fixní příplatek** v Kč
- **Přiřazení úrovně členovi je realizováno přes nový typ uživatelské skupiny** (`user-groups`): při vypsání úrovně pro daný rok vznikne skupina, která obsahuje kompletní snapshot nastavení (roční poplatek, pravidla, rok platnosti); volba úrovně členem = vstup do dané skupiny. Historie přiřazení = historie členství ve skupinách napříč roky.
- Procento se vždy počítá ze **základní ceny závodu v ORIS** (navýšené startovné za pozdní přihlášku platí člen v plné výši nad rámec pravidla)
- Při přihlášení člena na závod systém spočítá doplatek člena podle pravidla aplikovatelného na úroveň + typ/žebříček závodu a promítne ho do `member-accounts`
- **Administrace úrovní příspěvků** — nová sekce v Administraci pro definici úrovní a pravidel
- **Volba úrovně pro nadcházející rok členem** — do administrativně určeného data si každý člen sám volí svou úroveň pro následující kalendářní rok; nouzově může přiřazení provést správce členů (`MEMBERS:ADMIN`)
- **Sankce za neprovedenou volbu**: pokud se člen do uzávěrky nepřihlásí k úrovni, nesmí se přihlašovat na nové závody a je automaticky odhlášen ze všech závodů s dosud otevřenými přihláškami
- **Editace pravidel po vypsání úrovně**: admin smí upravovat pravidla a sazby dokud nebyl spočítán první doplatek na základě této úrovně; poté je snapshot zmrazen (oprava = nová úroveň)
- **Změna volby členem**: člen smí libovolně měnit svou volbu úrovně do uzávěrky voleb; po uzávěrce je volba zamknuta na celý rok platnosti
- **Generování ročního členského**: položka ročního členského poplatku vznikne na účtu člena automaticky následující den po uzávěrce voleb (na základě aktuálního členství v Membership Fee skupině)

## Capabilities

### New Capabilities
- `membership-fee-levels`: definice úrovní členských příspěvků pro daný kalendářní rok (roční poplatek + pravidla spoluúčasti na startovném podle typu závodu a žebříčku), vypsání úrovní pro nadcházející rok, volba úrovně členem, sankce za neprovedenou volbu

### Modified Capabilities
- `user-groups`: přidat nový typ skupiny **Membership Fee Group** (vedle Training / Family / Free) — drží snapshot úrovně pro daný rok; členství reprezentuje přiřazení člena k úrovni
- `events`: závod má nový samostatný atribut **ranking** (žebříček/série) nezávislý na typu závodu, synchronizovaný z ORIS; pravidla úrovně se vyhodnocují podle kombinace (typ závodu + ranking)
- `event-registrations`: při vytvoření přihlášky se eviduje a propočte doplatek člena podle jeho aktuální úrovně příspěvku
- `member-accounts`: roční členský poplatek a doplatky za závody se promítají do účtu člena jako automatické položky

## Impact

- **Nová doména:** `membership-fee-levels` jako agregát (lokace modulu se rozhodne v designu); přiřazení člen ↔ úroveň je řešeno přes nový typ skupiny v `user-groups`
- **Závislost:** `events` musí poskytnout typ závodu a žebříček; `member-accounts` musí umět přijmout automaticky generované položky; `user-groups` musí umožnit přidat čtvrtý typ skupiny s vlastním lifecycle
- **Integrace s ORIS:** základní cena závodu musí být dostupná v lokálním modelu závodu (synchronizace z ORIS)
- **Audit trail:** historie přiřazení úrovní členům (pro účetní účely a dohledání minulých spoluúčastí)
- **API:** nové HAL+FORMS endpointy pro správu úrovní, pro volbu úrovně členem a pro nouzové přiřazení správcem
- **Frontend:** nová sekce v Administraci pro definici úrovní a pravidel; UI pro člena pro volbu úrovně na další rok; widget na profilu člena s aktuální úrovní; informace o doplatku v dialogu přihlášení na závod
- **Budoucí rozšíření (mimo tento change):** automatický výpočet doplatku i při importu přihlášek z ORIS
