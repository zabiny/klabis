## Why

Klub potřebuje evidovat finance členů — kdo má u klubu kolik na předplaceném zůstatku, kdo si co kdy strhl nebo dorovnal. Bez vlastní evidence se dnes informace o platbách rozptylují mimo systém (excely, e-maily) a nelze ji propojit s členstvím (např. ukončení člena s otevřeným záporným zůstatkem). Tato změna staví doménové základy, na kterých později vyrostou strukturované poplatky (startovné, členské příspěvky) a integrace s bankou.

Issue: [gh-273](https://github.com/zabiny/klabis/issues/273).

## What Changes

- Každý člen má **finanční účet** s **klubovým kreditem** (předplacený zůstatek).
- Účet vzniká automaticky při registraci člena a zaniká až s členem (suspendovaný účet zůstává plně funkční pro doúčtování).
- Transakce jsou **append-only**, nelze je mazat ani editovat.
- Typy transakcí: **DEPOSIT** (kladná, vklad/vrácenka), **OTHER** (záporná, výdaj/poplatek).
- Storno se zapisuje jako **další transakce s flagem** `reversesTransactionId` odkazujícím na původní transakci (žádný "REVERSAL" typ, jen flag).
- **Globální limit přečerpání** (konfigurace v `application.yml`), např. -500 Kč. Platí na běžné výdaje, storno limit obchází.
- Nová authorita **`FINANCE:MANAGE`** pro správce financí: připisuje, strhává, stornuje, čte kterýkoli účet.
- Člen vidí jen vlastní zůstatek a historii (žádný self-service vklad ani strhávání).
- Měna pouze CZK, modelovaná jako Money value object pro pozdější rozšíření.
- **Historie účtu**: stránkování, řazení (datum / typ / částka asc+desc), filtr (datum od-do, typ).
- **Hlavní menu — položka "Finance"**: pro každého přihlášeného člena směřuje na jeho vlastní účet.
- **Detail člena + tabulka členů**: pro `FINANCE:MANAGE` přibývá akce/ikona pro otevření účtu daného člena.
- **Ukončení členství** dostává nový pre-check: záporný zůstatek vede k HTTP 409 s informací, jaký je stav účtu, a dialog na FE žádá dorovnání před opakovaným pokusem (analogicky k existujícímu pravidlu "last owner skupin"). Žádný override v dialogu — odpis pohledávky se řeší explicitní finanční operací.

## Capabilities

### New Capabilities

- `member-accounts`: finanční účet člena, historie transakcí, vklad/výdaj/storno, limit přečerpání, autorizace, zobrazení vlastního i cizího účtu.

### Modified Capabilities

- `members`: ukončení členství pre-check na záporný zůstatek a UI dialog odkazující na účet.
- `application-navigation`: nová položka "Finance" v hlavním menu pro všechny přihlášené členy.

## Impact

- **Nový backend modul** `com.klabis.finance` (Spring Modulith): doména, application, infrastructure (JDBC + REST API s HATEOAS).
- **Nová databázová tabulka(y)** pro `MemberAccount` a `Transaction` (append-only).
- **Domain event listener** na `MemberRegisteredEvent` (z `members` modulu) — auto-vznik účtu.
- **Cross-module query port** `MemberAccountQueryPort` čtený z `members` aplikační vrstvy při ukončení členství.
- **Frontend**: nová stránka "Finance" (vlastní účet), stránka účtu konkrétního člena, formuláře pro vklad/výdaj/storno (jen FINANCE:MANAGE přes HAL afordance), rozšíření existujícího suspend dialogu o 409 handling pro záporný zůstatek.
- **Konfigurace**: `klabis.finance.overdraft-limit` v `application.yml`.
- **Permission systém**: nová authorita `FINANCE:MANAGE` v seznamu authorit a v UI pro správu oprávnění.
- **Specs**: nový capability `member-accounts`; delta-spec do `members` a `application-navigation`.
