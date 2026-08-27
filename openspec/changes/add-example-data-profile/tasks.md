# Tasks: add-example-data-profile

## 1. Profile gating implementation

- [x] 1.1 `application.yml`: add `example-data` to default `spring.profiles.active`; add profile group `test-bootstrap: example-data`
- [x] 1.2 Remove `@Profile({"!test", "test-bootstrap"})` from `BootstrapDataLoader`
- [x] 1.3 Add `@Profile("example-data")` to `MembersDataBootstrap`, `TrainingGroupDataBootstrap`, `EventsDataBootstrap`, `MembershipFeeTiersDataBootstrap`
- [x] 1.4 Add `example-data` to `SPRING_PROFILES_ACTIVE` in `runLocalEnvironment.sh`

## 2. Documentation

- [x] 2.1 `backend/CLAUDE.md`: document `example-data` and `test-bootstrap` profiles, updated default profiles, clean-DB run command, bootstrap split
- [x] 2.2 `backend/README.md`: dev mode with demo data (default), clean-database (ORIS) run snippet, production note
- [x] 2.3 Root `CLAUDE.md`: note ZBM9000/ZBM9500 exist only when `example-data` active
- [x] 2.4 `backend/.env.example`: commented `SPRING_PROFILES_ACTIVE` example without `example-data`

## 3. Verification

- [x] 3.1 Targeted test run: OIDC bootstrap tests (`OidcRegisteredClientsBootstrapTest`, `AuthorizationServerPromptNoneTest`, `LocalDevRefreshTokenFlowTest`, `OidcFlowE2ETest`), `MemberLifecycleE2ETest`, `EventManagementE2ETest`, `EventTypeRepositoryAdapterTest`, `EventPublishingIntegrationTest`, `UserEventsListeningTests`
- [x] 3.2 Full backend test suite passes (3167 passed / 1 pre-existing date-sensitive failure in `OrisEventImportServiceTest`, unrelated: hard-coded 2026-08-01 event date, no Spring context)
- [x] 3.3 Manual: default local run bootstraps demo data (all 7 initializers, 17 members); clean-DB run (`SPRING_PROFILES_ACTIVE=h2,ssl,debug,metrics,oris`) bootstraps only admin user, OAuth2 clients, event types (3 initializers)
