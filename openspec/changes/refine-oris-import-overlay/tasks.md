## 1. Backend — filtrovat již importované ORIS závody (TDD)

- [x] 1.1 Přidat do `EventRepository` dávkový dotaz na již importovaná ORIS ID (např. `Set<Integer> findImportedOrisIds(Collection<Integer> candidateOrisIds)`); implementovat v `EventJdbcRepository` + `EventRepositoryAdapter` (`SELECT oris_id ... WHERE oris_id IN (:ids)`)
- [x] 1.2 Napsat test repository dotazu: vrátí jen ta z kandidátních ID, která už v DB existují
- [x] 1.3 Vystavit z events modulu primary port pro oris modul (např. `ImportedOrisEventsPort` s metodou vracející importovaná ID z kandidátů); implementovat tenkou service delegující na `EventRepository`
- [x] 1.4 Napsat test portu/service: predikát/množina importovaných ID
- [x] 1.5 V `OrisController.listOrisEvents` odfiltrovat ze seznamu závody, jejichž ORIS ID je už importováno (injektovat events port, odečíst před vrácením)
- [x] 1.6 `@WebMvcTest`/integrace pro `GET /api/oris/events`: již importované závody nejsou v odpovědi; ostatní zůstávají; prázdný seznam když vše importováno
- [x] 1.7 Ověřit Spring Modulith závislost oris → events port (`ModularityTests` / module verification) prochází

## 2. Frontend — overlay řízený affordancí na pozadí (vzhled beze změny)

- [x] 2.1 Změnit signaturu `useOrisEventImport`: místo `batchImportHref: string` přijmout celou affordanci `importEventsBatch` (`HalFormsTemplate | undefined` z `api/types.ts`); `EventsPage` předá `activeImportTemplate` (ne jen `.target`)
- [x] 2.2 V hooku odvodit submit z affordance: `target` → URL, `method` → HTTP metoda (`useAuthorizedMutation` s metodou z template; fallback POST), request body poskládat podle `properties` (vlastnost `orisIds`, `multi:true` → pole) místo natvrdo `{orisIds: [...]}`
- [x] 2.3 Odvodit limit výběru z affordance `properties.orisIds.max` (ověřeno = 50; fallback 50) a vynutit ho při výběru: při dosažení limitu znemožnit přidání dalších (disabled nevybrané checkboxy) + zobrazit hint o limitu; "Vybrat vše" vybere nanejvýš limit
- [x] 2.4 Zachovat dvoufázový tok a celý vzhled overlaye (region radio → `GET /api/oris/events?region=…` → checkbox seznam → výsledkový panel) — žádná vizuální změna; ověřit, že guard `isOpen && affordance` funguje stejně jako dnešní `isOpen && batchImportHref`
- [x] 2.5 Frontend testy: hook skládá submit z affordance (správný target/method/body), limit čtený z affordance blokuje další výběr a "vybrat vše" ho respektuje, fallback bez affordance nepadá

## 3. Frontend — duplicitní počet, oprava výsledku a dark theme

- [ ] 3.1 Odstranit duplicitní indikaci počtu vybraných ve footeru `ImportOrisEventModal` (ponechat počet jen v tlačítku "Importovat vybrané (N)", odebrat levý "Vybráno: N")
- [ ] 3.2 Opravit porovnání statusu ve výsledkovém panelu na case-insensitive vůči `imported`/`failed` (úspěch → zelené zaškrtnutí), upravit FE typ `BulkImportResultItem.status` / normalizaci tak, aby přijal hodnotu z API (`IMPORTED`/`FAILED`)
- [ ] 3.3 Opravit dark-theme styl vybrané položky seznamu: použít theme-aware třídy tak, aby vybraný stav byl jasně odlišný od hover highlightu v light i dark theme
- [ ] 3.4 Frontend testy: vybraná položka má rozlišitelný styl; footer ukazuje počet jen jednou; výsledkový panel renderuje success ikonu i pro uppercase status z API; seznam nezobrazuje již importované (na úrovni dat z API)

## 4. Spec sync a uzavření

- [ ] 4.1 Spustit testy (backend events + oris, frontend dotčené) a ověřit zelený stav
- [ ] 4.2 Synchronizovat delta spec do `openspec/specs/events/spec.md` (MODIFIED requirement Multi-Event ORIS Import) a archivovat změnu
