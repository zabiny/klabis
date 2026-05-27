## Why

Při hromadných akcích (závody, soustředění) musí správce financí ručně účtovat startovné nebo vracet peníze členům — dnes musí pro každého registrovaného člena přejít na jeho finanční účet, najít ho a zaúčtovat transakci samostatně. Když je seznam přihlášených otevřený, je přirozené místo, kde správce financí vidí přesně ty členy, kterých se transakce týká. Akce přímo v řádku seznamu přihlášených zkracuje čas potřebný k zaúčtování startovného pro celou akci a snižuje riziko chyby (špatný člen, špatná akce).

## What Changes

- V seznamu přihlášených na událost přibyde u každého řádku akce „Vložit/Strhnout" — otevře sjednocený transakční dialog (deposit/charge) pro účet daného člena.
- Akce SHALL být viditelná jen uživatelům s autoritou `FINANCE:MANAGE` (stejné pravidlo jako jinde pro recording transakcí).
- Dialog je tentýž sjednocený transakční dialog už existující v capability `member-accounts` (znovu použitý komponent) — předvyplní identitu člena a aktuální zůstatek.
- Po úspěšném zaúčtování se uživatel vrátí na seznam přihlášených (žádná navigace pryč).
- Note v transakci SHOULD být přednastavená na název události (např. „Startovné: Mistrovství ČR 2026"), uživatel může změnit.

## Capabilities

### New Capabilities

(žádné nové)

### Modified Capabilities

- `event-registrations`: přibyde nový requirement „Finance Manager Records Transaction From Registrations List" popisující dostupnost akce „Vložit/Strhnout" v řádcích seznamu přihlášených pro uživatele s `FINANCE:MANAGE`.

## Impact

- **Backend**: žádné nové API endpointy — znovuvyužije se stávající endpoint pro deposit/charge na member-account. Přibyde pouze HAL link/affordance v reprezentaci registrace (např. `recordTransaction` linkující na účet člena), zveřejněný jen pro `FINANCE:MANAGE`.
- **Frontend**: znovupoužití existujícího sjednoceného transakčního dialogu z `member-accounts`. Komponenta seznamu přihlášených (`EventRegistrationsList`) přibyde sloupec/akce, která dialog otevře. Předvyplnění poznámky názvem události.
- **Authorization**: stejný autorizační princip jako v `member-accounts` — `FINANCE:MANAGE` rozhoduje o viditelnosti akce a o úspěšnosti API volání.
- **Spec impact**: delta v `event-registrations`. `member-accounts` se nemění — pouze se znovu použije.
