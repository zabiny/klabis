## Context

`GET /api/oris/events` (modul `com.klabis.oris`, `OrisController`) vrací `List<OrisEventSummary>` přímo z ORIS API filtrovaný regionem a obdobím (dnes až +1 rok). Modul `oris` je nezávislý na modulu `events` a nemá přístup k `EventRepository`. Detekce duplicit dnes existuje jen na úrovni importu: `EventRepository.existsByOrisId(int)` + `DuplicateOrisImportException` (409) při single importu, resp. per-závod `failed` u dávky.

Dávkový import validuje `ImportBatchRequest` přes `@NotEmpty` + `@Size(max = 50)`. Frontend `ImportOrisEventModal` tento limit nezná — pošle libovolný počet a chybu zjistí až z odpovědi. Footer dialogu zobrazuje počet vybraných dvakrát: `labels.orisImport.selectedCount(n)` ("Vybráno: N") vlevo a `labels.orisImport.importSelected(n)` ("Importovat vybrané (N)") v tlačítku.

## Goals / Non-Goals

**Goals:**
- Seznam dostupných ORIS závodů vylučuje ty, které už v aplikaci existují (podle ORIS ID).
- Filtrování je server-side — jeden zdroj pravdy, frontend nemusí znát existující eventy.
- Frontend dialog respektuje horní hranici dávky (max 50) a sladí UI s backend validací.
- Footer dialogu zobrazuje počet vybraných jen jednou.
- Overlay je na pozadí řízen HAL-FORMS affordancí `importEventsBatch` (target, method, properties/limit, skládání body) — **bez jakékoli změny vzhledu a uživatelského chování**.

**Non-Goals:**
- Změna datového modelu `Event` nebo způsobu ukládání `orisId`.
- Změna single-event importu a dávkového import chování (per-závod výsledek, tolerance selhání).
- Konfigurovatelný limit dávky (zůstává konstanta 50).
- Zobrazení už importovaných závodů „našedle" v seznamu — rozhodnuto je je **skrýt**.
- **Změna vzhledu dialogu** — vizuál i dvoufázový tok zůstávají; přepracovává se jen mechanika na pozadí.
- Přepis overlaye na generický `HalFormButton`/`HalFormsForm` rendering — view zůstává custom (dvoufázový tok s externím dynamickým ORIS seznamem HAL-FORMS template nepokrývá).
- Vystavení nového HAL linku na `GET /api/oris/events` z events resource (events root dnes takový rel nemá) — mimo rozsah; GET zdroj zůstává region-parametrizovaný endpoint.

## Decisions

**1. Cross-module dotaz: events modul vystaví už importovaná ORIS ID, oris adaptér je odečte.**
Events modul publikuje primary port (např. `ImportedOrisEventsPort` / metoda `Set<Integer> findImportedOrisIds()` nebo predikát `boolean isImported(int orisId)`), který interně volá `EventRepository`. `OrisController` (modul oris) si tento port injektuje a po načtení seznamu z ORIS odfiltruje závody, jejichž ID je už importováno.
- *Proč ne filtrovat na frontendu:* events listing nemusí obsahovat `orisId` a tahalo by se víc dat; navíc duplikace pravidla do FE. Server-side filtr je jediný zdroj pravdy.
- *Proč ne přesouvat endpoint do events modulu:* `GET /api/oris/events` je dnes v oris modulu a je čistě o ORIS datech; stačí tenká závislost na events portu. Dodržuje dependency rule (oris → events port, ne naopak).
- *Tvar portu:* preferovat `Set<Integer> findImportedOrisIds(Collection<Integer> candidateOrisIds)` (dotaz omezený na právě načtená ID) před načítáním všech — vyhne se zbytečnému přenosu a `existsByOrisId` per závod v cyklu. Implementace přidá do `EventRepository` dávkový `exists/in` dotaz.

**2. Limit dávky čte frontend z HAL-FORMS affordance (ne hardcoded konstanta).**
Ověřeno na běžícím backendu: affordance `importEventsBatch` na `GET /api/events` už nese limit v metadatech —
```json
"importEventsBatch": {
  "method": "POST",
  "target": ".../api/events/import-batch",
  "properties": [ { "name": "orisIds", "type": "number", "multi": true, "min": 0, "max": 50 } ]
}
```
Spring HATEOAS odvodil `max: 50` z `@Size(max=50)` na `ImportBatchRequest.orisIds`. Frontend tedy přečte limit z `_templates.importEventsBatch.properties` (vlastnost `orisIds`, pole `max`) místo duplikace konstanty — konzistentní s HAL-FORMS-driven přístupem projektu. Při dosažení limitu výběru se další nevybrané checkboxy znemožní (disabled) a zobrazí se hint. "Vybrat vše" vybere nanejvýš `max` (viz Open Questions). Backend validace zůstává jako pojistka.
- *Proč ne hardcoded konstanta `MAX_ORIS_IMPORT_BATCH`:* limit už je v affordance; čtení z metadat zabrání rozjetí FE/BE hodnot, pokud se `@Size` v budoucnu změní.
- *Fallback:* pokud affordance limit z nějakého důvodu neobsahuje, použít rozumný default (50) a neblokovat výběr nad rámec — backend stejně odmítne.
- *Proč ne jen spoléhat na backend:* uživatel by narazil na chybu až po odeslání; defenzivní UX dle proposalu.

**3. Footer: jediná indikace počtu vybraných.**
Odstraní se levý text "Vybráno: N" (`selectedCount` label se přestane v footeru používat); počet zůstane v textu tlačítka "Importovat vybrané (N)". Případně se ponechá levý souhrn a zjednoduší tlačítko — zvolí se jedna varianta, ne obě (viz Open Questions).

**4. Oprava status kontraktu výsledkového panelu (case mismatch).**
Backend serializuje `ImportStatus` defaultně jako `name()` → `IMPORTED`/`FAILED`. Frontend (`ImportOrisEventModal`) i typ `BulkImportResultItem.status` počítají s `imported`/`failed`. Důsledek: `item.status === 'imported'` je vždy false → úspěch dostane červený křížek.
- *Volba:* opravit na **frontendu** porovnáním case-insensitive (např. `item.status?.toLowerCase() === 'imported'`) a rozšířit FE typ na `'imported' | 'failed' | 'IMPORTED' | 'FAILED'` nebo normalizovat při příjmu. Důvod: nejmenší dopad, neláme případné jiné konzumenty API. Alternativa (lowercase serializace na BE přes `@JsonValue`) by změnila API tvar a vyžadovala úpravu BE testů — vyhneme se jí, pokud to není nutné.
- Pokrýt testem ve `ImportOrisEventModal.test.tsx`, aby úspěšná položka renderovala success ikonu i pro uppercase status z API.

**5. Dark-theme styl vybrané položky.**
Vybraná položka dnes používá `bg-blue-50 border border-blue-600`, hover `hover:bg-surface-raised`. V dark theme `bg-blue-50` (světlá) nedostatečně kontrastuje / splývá s hoverem. Použít theme-aware třídy (design tokeny projektu, např. `bg-primary/10` či dedikovaný `surface-selected` token + výraznější border), aby vybraný stav byl jasně odlišný od hoveru v light i dark theme. Sladit s konvencí ostatních výběrových komponent projektu.

**6. Overlay napájený affordancí na pozadí — hybrid, view zůstává custom.**
Dnes `EventsPage` předává do `useOrisEventImport` jen `activeImportTemplate?.target` (URL string) a hook má natvrdo: GET `/api/oris/events`, metodu POST (`useAuthorizedMutation({method:'POST'})`) a body `{orisIds: Array.from(selectedIds)}`. Cíl: předat do hooku **celou affordanci** (`HalFormsTemplate` z `_templates.importEventsBatch`) a z ní odvodit:
- **submit target** = `template.target` (místo předaného holého href),
- **submit method** = `template.method` (místo natvrdo POST),
- **limit výběru** = `properties` → vlastnost `orisIds`, pole `max` (viz rozhodnutí #2),
- **request body** = poskládat podle `properties` (klíč `orisIds`, `multi:true` → pole hodnot) místo ručně zadrátovaného tvaru.

Typy už existují (`HalFormsTemplate`, `HalFormsProperty` v `api/types.ts`); hook je bude konzumovat místo `string` parametru.
- *Co zůstává custom (záměrně):* dvoufázový tok (region radio → `GET /api/oris/events?region=…` → checkbox seznam → výsledkový panel) a celý vzhled. Generický `HalFormsForm`/`HalFormButton` rendering se nepoužije — ORIS seznam je externí dynamický zdroj závislý na vybraném regionu, který HAL-FORMS template properties nepokrývají (žádné URI-template options). To je vědomá hranice (viz Non-Goals).
- *Proč:* odstraní hardcoded URL/method/limit/body, sladí FE s kontraktem API (jediný zdroj pravdy = affordance) a zlevní budoucí změny (např. úprava `@Size` se promítne sama).
- *Fallback:* chybí-li affordance property/limit, použít rozumný default (POST, limit 50) a nepadat.

## Risks / Trade-offs

- **Cross-module závislost oris → events** → Mitigace: tenký primary port s jedinou read-only metodou; žádná zpětná závislost. Konzistentní s hexagonální architekturou projektu.
- **Filtrování zmenší seznam až na prázdný** (vše už importováno) → Mitigace: dialog už má empty stav (`emptyHeading`/`emptyHint`); ověřit, že dává smysl i pro „vše naimportováno".
- **Race: závod naimportován jinde mezi načtením seznamu a odesláním** → Mitigace: backend dávkový import to už řeší per-závod jako `failed` (duplicate); filtr je jen UX vylepšení, ne garance.
- **"Vybrat vše" vs. limit 50** při >50 dostupných → drobná UX nejednoznačnost; řešeno rozhodnutím v Open Questions.

## Open Questions

- **"Vybrat vše" při více než 50 dostupných závodech:** vybrat prvních 50, nebo tristate „vybrat vše" zakázat a nechat jen ruční výběr do 50? (Po filtrování už importovaných bude reálných závodů typicky méně; navrhuji jednodušší variantu — vybrat nanejvýš 50 a hint o limitu.)
- **Footer:** ponechat levý souhrn „Vybráno: N" a zjednodušit tlačítko na „Importovat vybrané", nebo odstranit levý souhrn a nechat počet v tlačítku? (Navrhuji ponechat počet v tlačítku, odstranit levý duplikát — méně místa, jasná akce.)
