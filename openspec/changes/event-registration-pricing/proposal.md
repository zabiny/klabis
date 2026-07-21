## Why

Členové se přihlašují na eventy, ale systém zatím neumí spočítat, kolik daná registrace stojí. Cena se přitom liší podle zvolené kategorie, podle doplňkových služeb (ubytování, doprava, půjčení čipu) a podle členské úrovně (membership tier), která může vstupné zlevnit. Bez výpočtu ceny nelze připravit budoucí rezervaci a vyúčtování plateb za eventy.

## What Changes

- **Kategorie s cenou:** Kategorie eventu přestávají být prostý seznam názvů a stávají se strukturovanými položkami s volitelnou vlastní cenou. Cena kategorie **přepisuje** základní vstupné (`baseEntryFee`) eventu. **BREAKING** — mění tvar pole `categories`.
- **Doplňkové služby:** Event může nabídnout doplňkové služby s vlastní cenou a názvem. Člen si je při registraci volitelně vybírá. Tři obvyklé služby (ubytování, doprava, půjčení čipu) nabízí UI jako předvyplněné názvy; doména typ služby nerozlišuje. Služba má stabilní ID, takže přejmenování ani úprava ceny nerozváže výběr existujících registrací.
- **Příspěvek dle membership tier:** Cena za vstupné se modifikuje podle pravidel členské úrovně, kterou má člen v době konání eventu (procentem nebo pevnou částkou). Vychází z existujících `MembershipPaymentRule` v modulu membership-fees. Events předává datum konání a membership-fees samo určí, do kterého fee období spadá.
- **Informativní cena registrace:** Cena registrace se počítá **při čtení** a je **informativní** — nikam se neukládá. Závazná cena vznikne až při budoucím vyúčtování eventu (mimo rozsah této změny).
- **Jedna měna na event:** Všechny ceny na eventu (vstupné, kategorie, služby) musí být ve stejné měně.

## Capabilities

### New Capabilities
- `event-supplementary-services`: Definice doplňkových služeb na eventu (název + cena), jejich správa a výběr členem při registraci.
- `event-registration-pricing`: Výpočet orientační ceny registrace ze základního vstupného / ceny kategorie, příspěvku dle membership tier a zvolených doplňkových služeb.

### Modified Capabilities
- `event-categories`: Kategorie získává volitelnou cenu, která přepisuje základní vstupné eventu.
- `event-registrations`: Registrace nese seznam zvolených doplňkových služeb; cena se k registraci dopočítává při čtení.
- `events`: Validace jednotné měny napříč cenami eventu; výběr ceny kategorie nad `baseEntryFee`.

## Impact

- **Backend — events modul:** Nový value object `EventCategory` (name + volitelná cena), entita `SupplementaryService` (id, volitelný `orisId`, název, cena) + `SupplementaryServiceId`, rozšíření `EventRegistration` o `selectedServiceIds`. Nový application service `RegistrationPricingService` počítající cenu on-the-fly. Persistence (memento), REST + HAL-FORMS afordance pro správu služeb a výběr při registraci.
- **Cross-module hranice:** Nový port z events do membership-fees `MemberFeePricingPort`, který zapouzdřuje aplikaci `MembershipPaymentRule` a vrací výslednou částku za vstupné.
- **Sjednocení `Money` (napříč moduly):** Dnešní duplicitní `events.domain.Money` a `finance.domain.Money` se slučují do sdíleného `com.klabis.common.domain.Money`. Odstraňuje duplicitu i mapování na hranici portu a zabraňuje tomu, aby moduly pracující s cenou musely závist na `finance`. Dotýká se `finance`, `membershipfees` i `events` — čistě náhrada importů, bez změny chování a SQL schématu.
- **Frontend:** Správa služeb v create/edit formuláři eventu, výběr služeb při registraci, zobrazení rozpadu ceny.
- **Migrace dat:** Změna tvaru `categories` (string → struktura) vyžaduje migraci existujících dat.
- **Mimo rozsah:** Skutečná rezervace/blokace plateb a vyúčtování eventu (samostatná navazující změna). Cena registrace je zatím jen informativní a neukládá se.
