## Context

ADR-001 regulates ports consumed **across** modules. Inside a module, the codebase applied the same port-interface + implementation pair to every application service by habit, although nothing substitutes the implementation: Spring proxies concrete classes (CGLIB, `proxyTargetClass=true` by default), and Mockito mocks concrete classes. jMolecules stereotypes (`@PrimaryPort`) are `@Target(TYPE)` and may annotate a class.

## Goals / Non-Goals

**Goals:**
- One file per application service inside a module.
- Keep hexagonal classification checked by `JMoleculesArchitectureTest` green.
- Fewer, more cohesive services where two classes share dependencies and purpose.

**Non-Goals:**
- Cross-module ports, SPIs and secondary ports (repositories, `LoginNumberSequence`) — unchanged.
- Modules other than `members` and `events`.
- Any change in method bodies, transaction boundaries or published events.

## Decisions

1. **`@PrimaryPort` moves onto the service class.** It keeps the jMolecules `@Service` (DDD) stereotype. Class becomes `public` when a controller/job in another package uses it; constructor stays package-private where possible.
   *Alternative:* keep interfaces and generate them — rejected, adds tooling for no gain.
2. **Annotations from interfaces move to the class.** `@Transactional` on `RegistrationPort` methods, `@NonNull` parameters on `EventRegistrationPort`, javadoc from interface methods (only where it documents intent, not behaviour restated).
3. **Nested records move with the interface.** `RegistrationPort.RegisterNewMember` / `ImportMember` become `RegistrationService.RegisterNewMember` / `ImportMember`; `LegalGuardianPort` nested types move to `LegalGuardianService`; same for `LegalGuardianGroupPort`.
4. **`MemberContactService` absorbs `MemberCompletenessService`.** Both depend on `LegalGuardianGroupService` + guardian contact resolution; the merged class exposes `effectiveContactOf(..)`, `guardianContactsOf(..)`, `contactsOfChosenGuardians(..)`, `missingData(..)`.
5. **`OrisEventImportService` absorbs bulk import and bulk sync.** None of the three is `@Transactional` (deliberately — the engine owns short transactions), so moving `importEventsFromOris` (which calls `importEventFromOris`) into the same class introduces no self-invocation transaction issue. `ImportedOrisEventsService` stays separate: it implements the `oris` module's SPI.
6. **`GuardianContactResolver`**: interface removed, implementation `MemberAndLegalGuardianContactResolver` renamed to `GuardianContactResolver`.
7. **Discovery job ports** (`MemberDiscoveryPort`, `OrisEventRegistrationDiscoveryPort`): implemented by `@Application`-classified jobs that controllers call. Try removing them; if `JMoleculesArchitectureTest.hexagonalArchitectureShouldBeRespected` rejects a `@PrimaryAdapter` → `@Application` dependency, keep both interfaces and note it in ADR-011.

## Risks / Trade-offs

- [Mockito `@MockitoBean` on a concrete class with `final` methods silently calls real code] → services have no final methods; verify after retargeting mocks.
- [Package-private service becomes public, widening visibility inside the module] → Modulith named interfaces still restrict cross-module access; `ModuleStructureVerificationTest` guards it.
- [Merged services grow] → merged classes stay under ~150 lines; split again only if a future feature makes them incohesive.
- [ADR-001 readers may assume every service has a port] → ADR-011 states the rule explicitly.
