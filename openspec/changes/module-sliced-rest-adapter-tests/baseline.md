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
