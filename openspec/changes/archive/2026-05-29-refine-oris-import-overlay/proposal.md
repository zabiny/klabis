## Why

Dialog hromadného importu z ORIS dnes nabízí i závody, které už v aplikaci existují (po importu zůstávají v seznamu a po výběru jen selžou jako duplicita), zobrazuje počet vybraných závodů dvakrát vedle sebe a nehlídá hranici dávky, kterou backend vynucuje (max 50). Navíc má dvě vizuální vady: v dark theme nejdou rozeznat vybrané položky (mají stejné pozadí jako hover highlight) a ve výsledkovém panelu se úspěšně naimportované závody zobrazují s červeným křížkem místo zeleného zaškrtnutí. Tyto drobnosti kazí jinak hotový multi-import — správce ztrácí čas na akcích, které stejně nelze naimportovat, může narazit na nečekanou chybu z překročeného limitu a po dokončení dostane matoucí výsledek.

## What Changes

- **Seznam k importu skrývá už naimportované závody.** Dialog "Importovat z ORIS" zobrazí jen ty ORIS závody, které ještě nejsou v aplikaci. Filtrování probíhá na backendu (`GET /api/oris/events` vyřadí závody, jejichž ORIS ID už mezi eventy existuje), takže dialog je jediným zdrojem pravdy.
- **Odstranění duplicitního počtu vybraných.** Ve footeru dialogu se počet vybraných závodů zobrazoval dvakrát (text "Vybráno: N" vlevo i v textu tlačítka "Importovat vybrané (N)"). Ponechá se jediná indikace.
- **Dialog respektuje limit dávky.** Backend omezuje dávku na max 50 závodů; dialog tuto hranici dodrží — při dosažení limitu znemožní přidání dalších a srozumitelně to sdělí, takže uživatel nenarazí na validační chybu až po odeslání.
- **Oprava: výsledkový panel ukazuje úspěch správně.** Úspěšně naimportované závody zobrazí zelené zaškrtnutí (dnes kvůli neshodě velikosti písmen mezi backendem a frontendem dostávaly červený křížek).
- **Oprava: dark theme rozliší vybrané položky.** Vybraná položka v seznamu má v dark theme vizuálně odlišené pozadí od pouhého hover highlightu.
- **Overlay řízený affordancí na pozadí (beze změny vzhledu).** Dialog si zachová **současný vzhled i chování viditelné uživatelem** (region radio, checkbox seznam ORIS závodů, dvoufázový tok, výsledkový panel). Mění se jen mechanika na pozadí: na vstupu dostane celou HAL-FORMS affordanci `importEventsBatch` (ne jen URL string). Z affordance čte `target` + `method` pro submit a `properties.orisIds.max` pro limit výběru; request body skládá podle properties affordance. Tím se odstraní hardcoded URL, metoda i limit. Dvoufázový tok (výběr regionu → načtení ORIS seznamu → multi-select) i jeho custom view zůstávají — řeší externí dynamický zdroj, který HAL-FORMS template nepokrývá.

## Capabilities

### New Capabilities
<!-- žádné nové capability — jde o rozšíření existujícího events bounded contextu -->

### Modified Capabilities
- `events`: Požadavek na multi-event ORIS import se rozšiřuje — seznam dostupných závodů vylučuje ty už naimportované, dialog zobrazuje počet vybraných jen jednou a respektuje horní hranici dávky.

## Impact

- **Backend** (`com.klabis.oris` + `com.klabis.events`): `GET /api/oris/events` vyfiltruje závody, jejichž ORIS ID už existuje v events modulu. Vyžaduje cross-module dotaz — events modul vystaví seznam/predikát již importovaných ORIS ID (znovupoužije `EventRepository.existsByOrisId`), který oris adaptér použije k odečtení. Validační pravidla dávkového importu (`@NotEmpty`, `@Size(max=50)` na `ImportBatchRequest`) zůstávají beze změny — slouží jako referenční hodnota pro frontend.
- **Frontend** (`frontend/src/`): `ImportOrisEventModal` — odstranění duplicitního "Vybráno: N" z footeru, vynucení limitu výběru (max 50) s indikací, oprava porovnání statusu výsledku (case-insensitive vůči `imported`/`failed`), oprava dark-theme stylu vybrané položky; `useOrisEventImport` — případné odvození/hlídání limitu výběru.
- **Kontrakt statusu**: výsledkový panel závisí na hodnotě `status` z `BulkImportResult`. Backend serializuje enum jako `IMPORTED`/`FAILED`, frontend porovnává `imported`/`failed` → neshoda. Oprava sjednotí kontrakt (frontend porovná case-insensitive, případně backend serializuje lowercase) bez změny tvaru API.
- **Bez dopadu** na datový model `Event`, na single-event import a na samotné dávkové import chování (per-závod výsledek, tolerance částečného selhání).
