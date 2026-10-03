## 1. Aplikační služba

- [ ] 1.1 Přidat do `events.application` record `AccommodationList` a `AccommodationListRow` a metodu služby (autorizace, filtr `wantsSharedAccommodation`, dohledání `MemberAccommodationDto`)
- [ ] 1.2 Unit test služby: oprávnění (koordinátor, `EVENTS:REGISTRATIONS`, jinak odepřeno), vypnuté sdílené ubytování, filtr registrací, chybějící údaje člena

## 2. Controller a postprocessory

- [ ] 2.1 `EventController.getAccommodationList` a `getAccommodationListAsCsv` volají službu, odstranit privátní helpery a závislosti, které controller už nepotřebuje
- [ ] 2.2 `AccommodationListItemPostprocessor` čte `eventId` z `AccommodationListRow`; `AccommodationListPostprocessor` z domény seznamu
- [ ] 2.3 Odstranit `AccommodationListContext` a jeho `setContext`/`findContext`
- [ ] 2.4 Upravit `EventControllerTest` (mock služby) a testy postprocessorů

## 3. Kontexty, které si postprocessor zjistí sám

- [ ] 3.1 `ClubKeyHeld`: `MemberOrisImportAffordancePostprocessor` injektuje porty, odstranit record a `setContext` v `MemberController.listMembers`
- [ ] 3.2 `EnrolledMemberIds` v detailu: `MemberDetailsPostprocessor` používá `SynchronizationPort`, `getMember` kontext nenastavuje
- [ ] 3.3 `EnrolledEventIds` v detailu: totéž pro `EventDetailsPostprocessor` / `EventController.getEvent`
- [ ] 3.4 `EnrolledDisciplineIds` v detailu: totéž pro `DisciplineDetailsPostprocessor` / `DisciplineController.getDiscipline`
- [ ] 3.5 Doplnit `WebMvcMockitoBeans` / testy postprocessorů o potřebné porty

## 4. Ověření

- [ ] 4.1 Existující testy (včetně 403 a CSV) projdou beze změny asercí
- [ ] 4.2 Spustit celou backend sadu a architektonické testy
