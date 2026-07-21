## Why

Členové se přihlašují na eventy, ale systém zatím neumí spočítat, kolik daná registrace stojí. Cena se přitom liší podle zvolené kategorie, podle doplňkových služeb (ubytování, doprava, půjčení čipu) a podle členské úrovně (membership tier), která může vstupné zlevnit. Bez výpočtu ceny nelze připravit budoucí rezervaci a vyúčtování plateb za eventy.

> **Závislost:** Tato změna navazuje na change **`event-category-identity`**, který se implementuje **před** ní. Ten dává kategoriím stabilní identitu a volitelnou cenu; pricing tuto cenu už jen konzumuje.

## What Changes

- **Cena kategorie se promítne do ceny registrace:** Volitelná cena kategorie (zavedená changem `event-category-identity`) **přepisuje** základní vstupné (`baseEntryFee`) eventu. Tvar kategorií se v této změně už nemění — pouze se jejich cena poprvé konzumuje.
- **Doplňkové služby:** Event může nabídnout doplňkové služby s vlastní cenou a názvem. Člen si je při registraci volitelně vybírá. Tři obvyklé služby (ubytování, doprava, půjčení čipu) nabízí UI jako předvyplněné názvy; doména typ služby nerozlišuje. Služba má stabilní ID, takže přejmenování ani úprava ceny nerozváže výběr existujících registrací.
- **Příspěvek dle membership tier:** Cena za vstupné se modifikuje podle pravidel členské úrovně, kterou má člen v době konání eventu (procentem nebo pevnou částkou). Vychází z existujících `MembershipPaymentRule` v modulu membership-fees. Events předává datum konání a membership-fees samo určí, do kterého fee období spadá.
- **Informativní cena registrace:** Cena registrace se počítá **při čtení** a je **informativní** — nikam se neukládá. Závazná cena vznikne až při budoucím vyúčtování eventu (mimo rozsah této změny).
- **Jedna měna na event:** Všechny ceny na eventu (vstupné, kategorie, služby) musí být ve stejné měně.

## Capabilities

### New Capabilities
- `event-supplementary-services`: Definice doplňkových služeb na eventu (název + cena), jejich správa a výběr členem při registraci.
- `event-registration-pricing`: Výpočet orientační ceny registrace ze základního vstupného / ceny kategorie, příspěvku dle membership tier a zvolených doplňkových služeb.

### Modified Capabilities
- `event-registrations`: Registrace nese seznam zvolených doplňkových služeb; cena se k registraci dopočítává při čtení.
- `events`: Validace jednotné měny napříč všemi cenami eventu (vstupné, kategorie, služby).

> `event-categories` **není** modifikovaná — cenu kategorie i její chování specifikuje `event-category-identity`. Tato změna ji pouze používá jako vstup výpočtu.

## Impact

- **Backend — events modul:** Nová entita `SupplementaryService` (id, volitelný `orisId`, název, cena) + `SupplementaryServiceId`, rozšíření `EventRegistration` o `selectedServiceIds`. Nový application service `RegistrationPricingService` počítající cenu on-the-fly. Persistence (memento), REST + HAL-FORMS afordance pro správu služeb a výběr při registraci. Kategorie se nemění — jejich tvar dodává `event-category-identity`.
- **Cross-module hranice:** Nový port z events do membership-fees `MemberFeePricingPort`, který zapouzdřuje aplikaci `MembershipPaymentRule` a vrací výslednou částku za vstupné.
- **Sjednocení `Money` (napříč moduly):** Dnešní duplicitní `events.domain.Money` a `finance.domain.Money` se slučují do sdíleného `com.klabis.common.domain.Money`. Odstraňuje duplicitu i mapování na hranici portu a zabraňuje tomu, aby moduly pracující s cenou musely závist na `finance`. Dotýká se `finance`, `membershipfees` i `events` — čistě náhrada importů, bez změny chování a SQL schématu.
- **Frontend:** Správa služeb v create/edit formuláři eventu, výběr služeb při registraci, zobrazení rozpadu ceny.
- **Migrace dat:** Žádná. Změna je čistě aditivní (nové služby, nová pole na registraci); migraci kategorií vlastní `event-category-identity`.
- **Pořadí implementace:** Až po dokončení `event-category-identity`.
- **Mimo rozsah:** Skutečná rezervace/blokace plateb a vyúčtování eventu (samostatná navazující změna). Cena registrace je zatím jen informativní a neukládá se. Tvar a migrace kategorií.
