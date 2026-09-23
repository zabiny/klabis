# Test Runner Agent Memory

- [buildSrc test runner quirk](buildsrc_test_runner_quirk.md) — `--module :buildSrc` may report "No fresh JUnit XML" despite a green run; check XML mtimes, re-parse with --skip-run
- [Frontend test run I1 (2026-09-23)](frontend_test_run_i1.md) — 2004/2004 passed; SyncStatusIndicator introduced
- [Frontend test run I2 (2026-09-23)](frontend_test_run_i2.md) — 2006/2006 passed; SyncStatusIndicator wired into EventDetailPage
- [Frontend test run I4 (2026-09-23)](frontend_test_run_i4.md) — 2023/2023 passed; SyncStatusOverlay added
- [Frontend test run I5 (2026-09-23)](frontend_test_run_i5.md) — 2023/2023 passed; SYNC:MANAGE overlay added; lint+build clean
- [Frontend test run I6 (2026-09-23)](frontend_test_run_i6.md) — 2023/2023 passed; post-feature passes (env/scope, spec regen, type regen restoring missing disciplines endpoints); lint+build clean
- [Backend test run (2026-09-23)](backend_test_run_2026_09_23.md) — 3632/3632 passed after I6-BE OAuth2 SYNC scope registration; 37/37 in authorization-server package
- [Backend test run (2026-09-17)](backend_test_run_2026_09_17.md) — 3502/3502 passed after OrisEventFieldsGatewayService folded into OrisEventSyncAdapter
- [Backend test run (2026-09-04)](backend_test_run_2026_09_04.md) — 3399/3399 passed after sync/oris.eventsync/events cleanup
- [HATEOAS duplicate links](feedback_hateoas_duplicate_links.md) — same-relation links merge into arrays; split assembler vs controller responsibilities
- [Post-refactor fixes (2026-03-03)](feedback_post_refactor_fixes.md) — visibility + import-update gaps after package moves in calendar and events