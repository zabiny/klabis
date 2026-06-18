<!--
RESUME POZNÁMKA (pracovní kontext, nepatří do finální specifikace):
Tato změna vznikla z grill-me session "events-refactor-prices-design".
- Kompletní doménový návrh + class diagram + všech 15 rozhodnutí (decisions): events-domain-model.md (v této change složce)
- Tabulka "Rozhodnutí z grill-me session" v tom souboru je zdroj pravdy pro design.md a specs.
- Pro pokračování konverzace v Claude Code: spusť `claude --resume` a vyber session
  "events-refactor-prices-design" (případně `claude --continue` pro poslední session v tomto adresáři).
- Stav workflow: proposal hotov, dále následují artefakty design → specs → tasks
  (`openspec status --change event-registration-pricing`).
-->

## Why

Členové se přihlašují na eventy, ale systém zatím neumí spočítat, kolik daná registrace stojí. Cena se přitom liší podle zvolené kategorie, podle doplňkových služeb (ubytování, doprava, půjčení čipu) a podle členské úrovně (membership tier), která může vstupné zlevnit. Bez výpočtu ceny nelze připravit budoucí rezervaci a vyúčtování plateb za eventy.

## What Changes

- **Kategorie s cenou:** Kategorie eventu přestávají být prostý seznam názvů a stávají se strukturovanými položkami s volitelnou vlastní cenou. Cena kategorie **přepisuje** základní vstupné (`baseEntryFee`) eventu. **BREAKING** — mění tvar pole `categories`.
- **Doplňkové služby:** Event může nabídnout doplňkové služby s vlastní cenou. Tři předdefinované typy (ubytování, doprava, půjčení čipu) jako šablona, plus libovolné vlastní služby. Člen si je při registraci volitelně vybírá.
- **Příspěvek dle membership tier:** Cena za vstupné se modifikuje podle pravidel členské úrovně člena pro rok konání eventu (procentem nebo pevnou částkou). Vychází z existujících `MembershipPaymentRule` v modulu membership-fees.
- **Informativní cena registrace:** Každá registrace nese spočtenou orientační cenu (`reservedPrice`). Je **informativní** — závazná cena vznikne až při budoucím vyúčtování eventu (mimo rozsah této změny).
- **Jedna měna na event:** Všechny ceny na eventu (vstupné, kategorie, služby) musí být ve stejné měně.

## Capabilities

### New Capabilities
- `event-supplementary-services`: Definice doplňkových služeb na eventu (předdefinované i vlastní), jejich ceny a výběr službou členem při registraci.
- `event-registration-pricing`: Výpočet orientační ceny registrace ze základního vstupného / ceny kategorie, příspěvku dle membership tier a zvolených doplňkových služeb.

### Modified Capabilities
- `event-categories`: Kategorie získává volitelnou cenu, která přepisuje základní vstupné eventu.
- `event-registrations`: Registrace nese spočtenou orientační cenu a seznam zvolených doplňkových služeb.
- `events`: Validace jednotné měny napříč cenami eventu; výběr ceny kategorie nad `baseEntryFee`.

## Impact

- **Backend — events modul:** Nový value object `EventCategory` (name + volitelná cena), entita `SupplementaryService` (id, typ, název, cena) + enum `ServiceType`, rozšíření `EventRegistration` o `selectedServiceIds` a `reservedPrice`. Nový application service `RegistrationPricingService`. Persistence (memento), REST + HAL-FORMS afordance pro správu služeb a výběr při registraci.
- **Cross-module hranice:** Nový port z events do membership-fees `MemberFeePricingPort.contribution(memberId, year, eventTypeId, ranking, basePrice)`, který zapouzdřuje aplikaci `MembershipPaymentRule` a vrací výslednou částku za vstupné.
- **Frontend:** Správa služeb v create/edit formuláři eventu, výběr služeb při registraci, zobrazení rozpadu ceny.
- **Migrace dat:** Změna tvaru `categories` (string → struktura) vyžaduje migraci existujících dat.
- **Mimo rozsah:** Skutečná rezervace/blokace plateb a vyúčtování eventu (samostatná navazující změna). `reservedPrice` je zatím jen informativní.
