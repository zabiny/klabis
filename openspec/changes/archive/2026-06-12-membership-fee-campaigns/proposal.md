## Why

Správa výběru členských příspěvků potřebuje přísnější kontrolu životního cyklu: systém aktuálně nebrání vytvoření více souběžných kampaní, neumožňuje administrátorovi změnit deadline ani kampaň předčasně ukončit. Zároveň je terminologie ("Vypsání pro rok") nevýstižná a matoucí pro uživatele.

## What Changes

- **Nová validace při zakládání kampaně:** datum deadline musí být v budoucnosti (> dnes); nelze vytvořit kampaň, pokud již existuje jiná aktivní (tj. neuzavřená)
- **Nová operace: změna deadline aktivní kampaně** — administrátor může posunout deadline, ale ne do minulosti (>= dnes)
- **Přejmenování tříd a terminologie** — `FeeYearPublication` → `FeeSelectionCampaign` v celém stacku (backend, frontend, spec); UI label "Vypsání pro rok" → "Kampaň volby členského příspěvku"
- Rok přestává být identitou pro unikátnost — může existovat více kampaní pro stejný rok (sekvenčně)

## Capabilities

### New Capabilities

_(žádné nové bounded contexty — vše spadá do existující capability)_

### Modified Capabilities

- `membership-fees`: Validace datumu při zakládání a editaci kampaně, kontrola jedné aktivní kampaně, změna deadline aktivní kampaně, přejmenování terminologie

## Impact

**Backend:**
- `FeeYearPublication` doménový objekt a vše navázané přejmenovat na `FeeSelectionCampaign`
- Nová metoda `changeDeadline(LocalDate newDeadline, LocalDate today)` s validací
- Validace při vytváření: deadline > today, žádná aktivní kampaň neexistuje
- `isClosed(today)` zůstává datum-based (`today.isAfter(votingDeadline)`)

**Frontend:**
- Přejmenovat stránky `FeeYearPublications*` → `FeeSelectionCampaigns*`
- Aktualizovat `labels.ts` — všechny výskyty "Vypsání pro rok" → "Kampaň volby členského příspěvku"
- Přidat UI akci: "Změnit deadline"

**API:**
- Nová affordance na campaign resource: `changeDeadline`
- Validace chyb: `ActiveCampaignExistsException`, `DeadlineNotInFutureException`
