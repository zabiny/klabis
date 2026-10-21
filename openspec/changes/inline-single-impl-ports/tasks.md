## 1. Decision record

- [ ] 1.1 Add ADR-011 "Intra-module application services are concrete classes" to `docs/design-decisions.md` (rule, kept-interface categories, discovery-job outcome from 4.3)
- [ ] 1.2 Update `backend-patterns` skill: `SKILL.md` module layout, `references/domain-layer.md` (service example without interface), `references/aggregate-checklist.md` (no `@PrimaryPort` interface unless cross-module/SPI)

## 2. Members module (one vertical slice per service, each compiles and its tests pass)

- [ ] 2.1 `ManagementPort` → `ManagementService` (`@PrimaryPort`, public); retarget controllers, sync adapter, mocks
- [ ] 2.2 `RegistrationPort` → `RegistrationService`; move `RegisterNewMember`/`ImportMember` records and `@Transactional`
- [ ] 2.3 `MemberAgeOutPort`, `MemberAccountActivationPort` → their services
- [ ] 2.4 Merge `MemberCompletenessService` into `MemberContactService`; delete both ports; merge their unit tests
- [ ] 2.5 `LegalGuardianPort`, `GuardianCandidatesPort` → services; `GuardianContactResolver` interface removed, `MemberAndLegalGuardianContactResolver` renamed to `GuardianContactResolver`
- [ ] 2.6 `LegalGuardianGroupPort` → `LegalGuardianGroupService` (nested types moved)
- [ ] 2.7 Run members module tests

## 3. Events module

- [ ] 3.1 `EventManagementPort`, `EventRegistrationPort`, `AccommodationListPort` → services
- [ ] 3.2 `CategoryPresetManagementPort`, `DisciplineManagementPort` → services
- [ ] 3.3 Merge `OrisEventBulkImportService` and `OrisBulkSyncService` into `OrisEventImportService`; delete the three ORIS ports; merge their tests
- [ ] 3.4 Run events module tests

## 4. Architecture checks

- [ ] 4.1 `grep` confirms no remaining intra-module single-implementation `*Port` interface in members/events outside the kept list in proposal.md
- [ ] 4.2 `JMoleculesArchitectureTest`, `ModuleStructureVerificationTest`, `LayerArchitectureTest` pass
- [ ] 4.3 Try removing `MemberDiscoveryPort` and `OrisEventRegistrationDiscoveryPort`; keep them if 4.2 fails, record outcome in ADR-011
- [ ] 4.4 Full backend test suite passes
