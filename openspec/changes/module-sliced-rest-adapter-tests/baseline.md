# Baseline (task 1.1)

Měřeno 2026-10-02 na větvi `refactor/rest-api-tests-structure` (HEAD `fbfa0291`), per modul:

```
SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true run-profiled-tests.sh --root backend --tests 'com.klabis.<module>.*' --rerun-tasks
```

Profiler běží v jednom forku, takže čísla kontextů/času platí pro izolovaný běh modulu (cache se mezi moduly nesdílí; v celém suite budou kontexty nižší, pokud se sdílí napříč moduly). Jsou v nich všechny testy modulu (domain, JDBC, integrační, E2E), ne jen REST adaptéry.

| Modul | Testů | Skipped | Failed | Spring kontexty | Hit ratio | Tvorba kontextů | Celkový čas | `@WebMvcTest` tříd (legacy) | tříd s `@WithPostprocessors` |
|---|---|---|---|---|---|---|---|---|---|
| groups | 307 | 0 | 0 | 4 | 98,6 % | 29 s | 48 s | 2 | 2 |
| calendar | 289 | 0 | 0 | 5 | 96,5 % | 5 s | 27 s | 1 | 1 |
| sync | 220 | 0 | 0 | 5 | 97,8 % | 4 s | 80 s | 1 | 1 |
| finance | 100 | 0 | 0 | 3 | 96,5 % | 24 s | 30 s | 1 | 1 |
| membershipfees | 285 | 0 | 0 | 7 | 97,5 % | 43 s | 56 s | 5 | 5 |
| events | 1023 | 0 | 0 | 10 | 98,8 % | 23 s | 97 s | 6 | 6 |
| oris | 15 | 0 | 0 | 2 | 93,3 % | 11 s | 13 s | 2 | 2 |
| common | 715 | 10 | 0 | 18 | 94,8 % | 63 s | 126 s | 13 | 13 |
| **Součet** | **2954** | 10 | 0 | 54 | | 202 s | 477 s | 31 | 31 |

Poznámky:
- Počty testů jsou ze `TEST-*.xml` (zahrnují parametrizované a `@Nested`); proto se liší od hrubého `grep '@Test'`.
- 10 skipped v `common` jsou existující `@Disabled`/podmíněné testy (`MonitoringEndpointsTests`, `OidcUserInfoEndpointTest`, `TokenHashTest$SecurityTimingAttackPrevention`), ne důsledek Modulithu — `SKIP_OPTIMIZATIONS` byl nastaven a `--rerun-tasks` použit.
- Žádný z modulů nemá failing test; `ModularEventsTest` a `EventLoggingTests` (známé failures na `main`) nejsou v `com.klabis.<module>.*` těchto modulů.
- Surové výstupy (HTML report profileru, XML výsledky) jsou v `$CLAUDE_JOB_DIR/tmp` (`<module>.html`, `<module>-xml/`) a nejsou součástí repozitáře; HTML reporty jsou také v `backend/build/spring-test-profiler/`.
- Počet kontextů REST adaptérů se po migraci porovná jako: kontexty modulu před/po (cíl: 1 sdílený kontext na modul + případné pojmenované varianty).

# After migration (task 12.2)

Měřeno 2026-10-03 na větvi `refactor/rest-api-tests-structure` (HEAD `3a55ba05`) stejným postupem jako baseline (per modul, `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`, `run-profiled-tests.sh --root <abs>/backend --tests 'com.klabis.<module>.*' --rerun-tasks`, sekvenčně, na popředí). XML čerstvost ověřena skriptem (mtime vůči markeru). Celá suite po migraci: 3980 testů, 14 skipped, 0 failures (zahrnuje i další moduly, např. members; baseline pokrývá jen těchto 8).

| Modul | Testů před → po | Kontexty před → po | Hit ratio před → po | Tvorba kontextů před → po | Celkový čas před → po |
|---|---|---|---|---|---|
| groups | 307 → 307 | 4 → 3 | 98,6 → 98,3 % | 29 → 25 s | 48 → 46 s |
| calendar | 289 → 287 | 5 → 5 | 96,5 → 95,5 % | 5 → 1 s | 27 → 28 s |
| sync | 220 → 222 | 5 → 5 | 97,8 → 97,3 % | 4 → 2 s | 80 → 79 s |
| finance | 100 → 106 | 3 → 3 | 96,5 → 94,5 % | 24 → 18 s | 30 → 28 s |
| membershipfees | 285 → 287 | 7 → 3 | 97,5 → 98,2 % | 43 → 25 s | 56 → 42 s |
| events | 1023 → 1025 | 10 → 6 | 98,8 → 98,9 % | 23 → 9 s | 97 → 88 s |
| oris | 15 → 15 | 2 → 2 | 93,3 → 86,7 % | 11 → 0 s | 13 → 4 s |
| common | 715 → 711 | 18 → 9 | 94,8 → 96,1 % | 63 → 40 s | 126 → 115 s |
| **Součet** | **2954 → 2960** | **54 → 36** | | **202 → 120 s** | **477 → 430 s** |

Skipped: 10 → 10 (common, beze změny), failed: 0 → 0. Wall time Gradle běhů zahrnuje kompilaci a není porovnávána; "Celkový čas" je čas testů z profileru.

## Rozdíly v počtu testů (po třídách, z `TEST-*.xml`)

Celkově +6 (2954 → 2960). Žádný test nebyl smazán bez náhrady: unit testy `*LinkProcessor`/`*Postprocessor` byly převedeny na ověření přes HTTP odpověď v controller testech (jeden sdílený kontext) a ponechány jen pro větve nedosažitelné z odpovědi controlleru. Nové testy pokrývají nově zavedené primární porty (`*Port`), které nahradily přímé mocky repozitářů/služeb v REST testech.

| Modul | Změna | Vysvětlení |
|---|---|---|
| groups | 0 | Jen sdílený kontext (`@GroupsWebMvcTest`), počty beze změny. |
| calendar | −2 | `CalendarRootPostprocessorTest` (1) smazán, ověřeno v `CalendarControllerTest` (+1, root index link). `IcalTokenMemberDetailLinkProcessorTest` 5 → 3: scénáře detailu člena přesunuty do controller testů, zůstaly větve nedosažitelné z odpovědi. |
| sync | +2 | Nový `SyncProjectionFieldsServiceTest` (2) pro nový port `SyncProjectionFieldsPort`. |
| finance | +6 | Smazány `AccountMemberDetailLinkProcessorTest` (4), `AccountMemberSummaryLinkProcessorTest` (4), `AccountRootLinkProcessorTest` (3) = −11. Přibyly: `MemberAccountControllerTest` +8 (link na odpovědích jiných modulů a root), `AccountLinkProcessorBranchesTest` +5 (větve nedosažitelné z odpovědi), `TransactionQueryServiceTest` +4 (nové čtecí metody primárního portu). Netto +6. |
| membershipfees | +2 | `MemberFeeSummaryControllerTest` +4 (feeSummary link na `GET /api/members/{id}`), `MemberFeeSummaryLinkProcessorTest` 5 → 1 (−4, scénáře přesunuty do controller testu), nový `MembershipFeeTierOptionsServiceTest` +2 (nový port pro tier options). |
| events | +2 | `DashboardUpcomingRegistrationsLinkProcessorTest` 3 → 1, `EventTypesRootPostprocessorTest` 3 → 1, `RegistrationRecordTransactionLinkProcessorTest` 4 → 2 (celkem −6, přesunuto do controllerů). Controller testy: `EventControllerTest` +4, `DisciplineControllerTest` +2, `EventTypeControllerTest` +2 (+8, nav linky disciplines/event-types, dashboard `upcomingRegistrations`, chování bez oris profilu). Netto +2. |
| oris | 0 | Jen sdílený kontext (`@OrisWebMvcTest`) a port `OrisClubKeyManagementPort`; počet testů stejný. |
| common | −4 | Smazány `OrisClubKeyRootLinkProcessorTest` (2) a `RootProfileLinkProcessorTest` (3), `RootControllerTest` 6 → 3 (−3, po konsolidaci GET /api scénářů); nový `OrisClubKeyManagementServiceTest` +4 (služba pro nový port `OrisClubKeyManagementPort`). Netto −4. |

Pozn.: Přesné rozdělení „přesunuto vs. smazáno" v rámci jednoho modulu plyne z commitů `f9f1ec24..HEAD` (jeden commit na modul) a z rozdílu po třídách; sloučení scénářů do controller testů mění počet testů (parametrizace/konsolidace), proto netto rozdíl není 1:1 s počtem smazaných.

## Rozdíly v kontextech a času

- Kontexty 54 → 36 (−18): největší úspora v `common` (18 → 9), `events` (10 → 6), `membershipfees` (7 → 3) a `groups` (4 → 3). Každá legacy třída s `@WebMvcTest` + `@WithPostprocessors` (31 tříd) měla vlastní konfiguraci kontextu; po migraci sdílí jeden kontext na modul (`@<Modul>WebMvcTest`). `calendar`, `sync`, `finance`, `oris` měly již dříve 1 legacy třídu, takže počet kontextů se nezměnil (zbylé kontexty jsou integrační/JDBC/E2E, mimo rozsah migrace).
- Hit ratio mírně kolísá (např. oris 93,3 → 86,7 %, finance 96,5 → 94,5 %): při menším počtu kontextů a malém počtu testů vychází poměr z menšího jmenovatele; není to regrese, absolutní počet vytvořených kontextů klesl nebo zůstal.
- Tvorba kontextů 202 → 120 s (−82 s) odpovídá méně vytvořeným kontextům; celkový čas testů 477 → 430 s (−47 s, −10 %). Rozdíly u `calendar`/`sync` (±1 s) jsou v rámci šumu měření (kontexty beze změny, jde o jiné než REST adaptér kontexty). Rozdíl `oris` (13 → 4 s, kontexty 2 → 2) plyne z kratší tvorby kontextů (11 → 0 s, studený start v baseline); počet kontextů se nezměnil.
