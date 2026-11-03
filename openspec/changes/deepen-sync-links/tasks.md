## 1. Pairing query and sync link module (`sync`)

- [ ] 1.1 Add `SyncPairing` enum (`NONE`, `ACTIVE`, `RETIRED`) to `sync.application`
- [ ] 1.2 Write a JDBC integration test for `SyncRecordRepository.pairingOf(SyncTarget)` covering no record → `NONE`, non-retired record → `ACTIVE`, retired record → `RETIRED`, and a target with records for two systems where one is not retired → `ACTIVE`; confirm it fails
- [ ] 1.3 Implement `SyncRecordRepository.pairingOf` in `SyncRecordRepositoryAdapter` / `SyncRecordJdbcRepository`, reading only `retired_at` by `entity_type` + `entity_id` (no aggregate load, no system resolution); 1.2 passes
- [ ] 1.4 Add `pairingOf(SyncTarget)` to `SyncPairingLookup` and `SyncPairingLookupService` (`@Transactional(readOnly = true)`), with no dependency on `SynchronizationAdapterRegistry`
- [ ] 1.5 Write `SyncLinksTest`: for every `SyncEntityType`, `ACTIVE` and `RETIRED` add rel `sync` with href `/api/{pathSegment}/{id}/sync`; `NONE` adds nothing; confirm it fails
- [ ] 1.6 Implement `SyncLinks` (`@MvcComponent`, public, `sync.infrastructure.restapi`) with `addTo(RepresentationModel<?>, SyncTarget)`, using `SyncEntityTypeResolver.toParam` and `klabisLinkTo(methodOn(SyncApi.class).getSyncState(...))`; 1.5 passes
- [ ] 1.7 Run sync module tests

## 2. Disciplines slice (`events`)

- [ ] 2.1 Write a regression test in `DisciplineControllerTest`: a list row of an archived ORIS discipline (pairing `RETIRED`) offers no `updateDiscipline` affordance and carries a `sync` link; confirm it fails on the current code
- [ ] 2.2 `DisciplineDetailsPostprocessor`: lock = `pairingOf != NONE`, link via `SyncLinks.addTo`; delete `EnrolledDisciplineIds` and the `setDeferredContext` block in `listDisciplines`; `DisciplineController` no longer injects `SynchronizationPort`
- [ ] 2.3 `DisciplineManagementService.update`: lock via `SyncPairingLookup.pairingOf != NONE`; `restore` keeps `findByTarget`; retarget `DisciplineManagementServiceTest` stubs
- [ ] 2.4 Retarget remaining `DisciplineControllerTest` mocks from `SynchronizationPort` to `SyncPairingLookup` (assertions unchanged); 2.1 passes
- [ ] 2.5 Run events module tests

## 3. Events slice (`events`)

- [ ] 3.1 `EventSummaryPostprocessor`: call `SyncLinks.addTo` only for DRAFT/ACTIVE events; delete `EnrolledEventIds` and the `setDeferredContext` block in `listEvents`
- [ ] 3.2 `EventDetailsPostprocessor`: link via `SyncLinks.addTo`; ORIS registration discovery = `pairingOf == ACTIVE` (no `SyncStatus`/`ExternalSystem` check); delete `EventAffordanceSupport.addSyncLinkIfEnrolled`
- [ ] 3.3 `EventController.java` no longer imports `SynchronizationPort`, `SyncRecord`, `SyncStatus`, `ExternalSystem`, `SyncApi`
- [ ] 3.4 Retarget `EventControllerTest`, `EventDetailEmbeddedRegistrationsHalParityTest`, `OrisEventControllerTest` mocks to `SyncPairingLookup`; make sure existing scenarios for FINISHED/CANCELLED rows (no indicator) and ACTIVE detail discovery still pass with unchanged assertions
- [ ] 3.5 Run events module tests

## 4. Event registrations slice (`events`)

- [ ] 4.1 `RegistrationSummaryPostprocessor` / `RegistrationDetailsPostprocessor`: link via `SyncLinks.addTo(new SyncTarget(EVENT_REGISTRATION, RegistrationSyncId.format()))`; delete `RegistrationSyncLinks`
- [ ] 4.2 Retarget `EventRegistrationControllerTest` mocks; run events module tests

## 5. Members slice (`members`)

- [ ] 5.1 `MemberDetailsPostprocessor` / `MemberSummaryPostprocessor`: link via `SyncLinks.addTo`; delete `EnrolledMemberIds`, `MemberSelfLinkSupport.addSyncLinkIfEnrolled` and the `setDeferredContext` block in `listMembers`; `MemberController` no longer injects `SynchronizationPort`
- [ ] 5.2 Retarget `MemberControllerApiTest`, `UpdateMemberApiTest`, `BirthNumberAuditControllerTest` mocks; run members module tests

## 6. Cleanup and documentation

- [ ] 6.1 Remove `SynchronizationPort.findActiveByTargets` and its `SynchronizationService` delegate if `grep` shows no remaining callers; keep `SyncPairingLookup.findActiveByTargets` (`OrisEventRegistrationDiscoveryJob`)
- [ ] 6.2 `grep` confirms no `Enrolled*Ids`, `addSyncLinkIfEnrolled` or `SynchronizationPort` import left in `events/infrastructure/restapi` and `members/infrastructure/restapi`
- [ ] 6.3 Update `.claude/skills/backend-patterns/references/synchronization-adapter.md` (the "Add a `sync` link" bullet): use `SyncLinks.addTo`, express pairing rules via `SyncPairingLookup.pairingOf`, and note that discovery-style rules assume one external system per entity type (design.md decision 4)
- [ ] 6.4 `ModuleStructureVerificationTest`, `JMoleculesArchitectureTest` and `AffordanceRoutingArchitectureTest` pass
- [ ] 6.5 Full backend test suite passes
