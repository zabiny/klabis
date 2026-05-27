> **Související GitHub issue:** [#283 Detail akce: export seznamu pro ubytování (CSV / XLS)](https://github.com/zabiny/klabis/issues/283)

## Why

"Seznam pro ubytování" je dnes pouze tisková stránka (HTML pro print). Koordinátoři akcí potřebují seznam předávat ubytovatelům často v elektronické podobě (e-mailem, jako vstup do jejich systému) — k tomu je vhodnější strukturovaný formát.

## What Changes

- Na stránce "Seznam pro ubytování" přidat akci **"Stáhnout CSV"** (vedle existující akce "Tisknout")
- Export obsahuje **stejné sloupce a řádky jako existující tabulka** (jméno, příjmení, číslo OP, platnost OP, datum narození, adresa) — žádné nové údaje
- Formát: **CSV s UTF-8 BOM** a středníkem jako oddělovačem (otevíratelné přímo v české lokalizaci MS Excel)
- Pro chybějící údaje (např. číslo OP) export obsahuje stejný text "neuvedeno" jako tisková verze
- **Autorizace identická s existujícím seznamem** — koordinátor akce + `EVENTS:REGISTRATIONS`; ostatní uživatelé export nevidí v UI a API zamítne neoprávněný požadavek

## Capabilities

### Modified Capabilities
- `event-registrations`: rozšířit požadavek "Generate Accommodation List for Event Registrations" o možnost stáhnout seznam jako CSV soubor (vedle existujícího tiskového zobrazení)

## Impact

- **Backend:** nový endpoint (nebo content-type negotiation na existujícím) vracející CSV; reuse stávající logiky sestavení seznamu
- **Frontend:** akce "Stáhnout CSV" v UI tiskové stránky; download trigger v prohlížeči
- **Žádná změna domény** — žádný nový atribut, žádná nová entita
- **Žádná změna autorizace** — využijí se existující kontroly
- **XLS / XLSX formát out of scope** — pokud bude později vyžadován, lze přidat samostatně bez konfliktu s tímto changem

## Out of Scope

- XLS / XLSX export (potenciální budoucí rozšíření)
- Vlastní výběr sloupců v exportu / custom layout
- Změna autorizačního modelu pro seznam
- Změna nebo rozšíření tiskové verze HTML stránky
