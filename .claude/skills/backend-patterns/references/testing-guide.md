# Klabis Backend Testing Guide

Testing patterns derived from the `members` module as the canonical reference.

## Test Types & When to Use Each

| Test Type | Annotation | Scope | Use for |
|-----------|-----------|-------|---------|
| Domain and Value object unit | `@ExtendWith(MockitoExtension.class)` | No Spring | Business rules, invariants, state transitions |
| Service unit | `@ExtendWith(MockitoExtension.class)` | No Spring | Service logic, mock interactions |
| Repository | `@DataJdbcTest` | JDBC slice | CRUD, custom queries |
| Controller (REST adapter) | `@<Module>WebMvcTest` (`@WebMvcTest` + `@ModuleSlicing`) | One shared MVC slice per module | HTTP status, request mapping, service calls, security (401/403), HAL links/affordances incl. postprocessors |
| Integration | `@KlabisModuleTest` | Module + direct deps | Full flow with real database |
| E2E | `@E2ETest` | All dependencies | Complete user scenarios |

## Domain & Service Unit Tests

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ManagementService Unit Tests")
class ManagementServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock UserService userService;

    private ManagementService testedSubject;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testedSubject = new ManagementServiceImpl(memberRepository, userService);
        testMember = MemberTestDataBuilder.aMember().withFirstName("Jan").build();
    }

    @Nested
    @DisplayName("Suspend Member")
    class SuspendMemberTests {
        @DisplayName("it should suspend user along suspending member")
        @Test
        void shouldSuspendMemberAndSyncUser() {
            when(memberRepository.findById(any())).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new Member.SuspendMembership(...);
            testedSubject.suspendMember(testMember.getId(), command);

            verify(memberRepository).save(any(Member.class));
            verify(userService).suspendUser(testMember.getId().toUserId());
        }
    }
}
```

Use `Strictness.LENIENT` when `@BeforeEach` sets up mocks that not every test exercises.

## Repository Tests

```java
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
    type = FilterType.ANNOTATION,
    value = {Repository.class}))
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM members")
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired MemberRepositoryAdapter repository;

    @Test
    void shouldSaveAndLoadMember() {
        Member member = MemberTestDataBuilder.aMember().build();
        Member saved = repository.save(member);
        Optional<Member> loaded = repository.findById(saved.getId());

        assertThat(loaded).isPresent();
        MemberAssert.assertThat(loaded.get()).hasFirstName("Jan");
    }
}
```

Notes:
- `@AutoConfigureTestDatabase(Replace.NONE)` — use configured H2, not auto-created
- `includeFilters` with `@Repository` annotation type because jMolecules uses its own `@Repository`
- `DELETE FROM` in `@Sql` rather than relying on rollback for cleaner isolation

## Controller Tests (@WebMvcTest)

The REST adapter is tested by **one primary test per controller: a `@WebMvcTest`** sharing a **single Spring context per module**. Everything that concerns the controller's HTTP contract lives there — request mapping, status codes, security (401/403 via the real filter chain with `@WithKlabisMockUser`), and links/affordances rendered by postprocessors (assert them on the controller's response in a `@Nested` class; no separate postprocessor/link-processor unit tests, no `@KlabisModuleTest` security tests). Full-stack scenarios stay in `@E2ETest` classes in the module root package (e.g. `com.klabis.members.MemberLifecycleE2ETest`), not in `restapi`.

Reference implementation: `com.klabis.members.MembersWebMvcTest` (members module). Modules not migrated yet still use the legacy `@WithPostprocessors` form (below).

### Per-module meta-annotation

```java
@WebMvcTest                                   // no `controllers` filter — all module controllers share one context
@ModuleSlicing(module = "members", extraIncludes = {"groups", "calendar", "sync"},
        mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class})
@MembersWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@GroupsWebMvcMockitoBeans
@CalendarWebMvcMockitoBeans
@SyncWebMvcMockitoBeans
public @interface MembersWebMvcTest {}
```

- `@ModuleSlicing` (Spring Modulith) loads the real beans of the module (controllers, postprocessors, converters) plus shared `common`; use `mode = STANDALONE`. The meta-annotation must live in `com.klabis.<module>` and name `module` explicitly.
- `extraIncludes` — only modules whose postprocessors add links/affordances to the responses the tests actually assert (members: `groups` → `trainingGroup` link, `calendar` → `ical-token` link, `sync` → sync link/state). A module whose port is injected into other modules' controllers (e.g. `sync`'s `SynchronizationPort`) is provided by adding the module to `extraIncludes` and composing its `*WebMvcMockitoBeans` — never by mocking the port directly in the consumer's annotation. Every included module loads **all** its web beans, so each needs its `*WebMvcMockitoBeans`. `@KlabisModuleTest` of a module whose controllers need a foreign primary port (e.g. members' `MemberController` -> `SynchronizationPort`) likewise needs `extraIncludes = "sync"`.
- **Mock only primary ports.** The only beans a REST adapter test mocks are the module's primary ports (`@PrimaryPort`) and the same from other modules, plus infrastructure forced by the framework (`UserDetailsService`). Never mock:
  - beans from the REST adapter package under test (renderers, converters, link-support implementations, mappers) — load the real ones;
  - application/domain internals (repositories, `@Port` secondary ports, domain readers). A controller or postprocessor that needs one is bypassing the primary port — extend the primary port instead of mocking the repository.
  An external client (e.g. `OrisApiClient`) is a justified exception only when the adapter calls it directly.
- `<Module>WebMvcMockitoBeans` (test source of the owning module, e.g. `com.klabis.groups.GroupsWebMvcMockitoBeans`) bundles `@MockitoBean(types=...)` for all ports/repositories the module's web beans need. Consumers compose it without knowing the ports. Each type is mocked in exactly one such annotation — the module that defines it (`common` owns `UserService`, `UserDetailsService`, `PasswordChangePort`, `PermissionService`, `OrisClubKeyPort`). When a web bean of the module gains a dependency, add it there, not in a test.
- Optional / feature-flag / profile-gated beans (injected as `Optional<T>`, e.g. `MemberDiscoveryPort`, `OrisEventImportPort`) never go into a `*WebMvcMockitoBeans` or the `@<Module>WebMvcTest`. The test that needs the feature declares the mock itself (`@MockitoBean` on a dedicated test class, e.g. `MemberOrisImportEnabledApiTest`); the shared-context test class keeps a `...DisabledTests` nested class verifying the "feature off" behaviour (endpoint 404, affordance absent). A required dependency must not be `Optional<T>` just for test convenience.

```java
@MembersWebMvcTest
class MemberControllerApiTest {

    @Autowired ManagementPort managementService;      // mocks are injected with @Autowired, never @MockitoBean
    @Autowired MockMvc mockMvc;

    @Nested class EndpointSecurityTests { ... }       // 401/403 + merged security tests
    @Nested class LegalGuardianGroupLinksTests { ... } // postprocessor links asserted on the controller response
}
```

Slice-context rules:
- **One context per module.** Tests declare no `@MockitoBean`, `@Import`, `@ActiveProfiles` or `@TestPropertySource` of their own — any difference creates a new context and loses the sharing. Stub mocks via `@Autowired` fields (`reset` is automatic). A genuinely different configuration (e.g. feature flag on via a locally mocked `Optional` bean) is a separate, deliberately named test class — keep their number minimal, each one is an extra context.
- `Converter<S,T>` beans are always included by `WebMvcTypeExcludeFilter` — never `@Import` or mock a converter. The same visibility is why a `Converter` must have no module-specific constructor dependencies and no `uses = <PlainMapper>` (see `dto-mapping.md`).
- `EntityLinks` comes from the real context; no `@TestBean EntityLinks`.
- Duplicate `@MockitoBean` of the same type (including via two composed annotations) fails context bootstrap — keep type ownership in one `*WebMvcMockitoBeans`.

### Legacy form (modules not yet migrated)

Every `@WebMvcTest(controllers = ...)` carries `@WithPostprocessors` (`com.klabis.common`) — a meta-annotation with `@MockitoBean`s for security infra and every dependency of an `@MvcComponent` postprocessor; per-test `@MockitoBean` for the controller's ports; a postprocessor's new dependency is added to `@WithPostprocessors`, stubbed in tests via `@Autowired`; feature-flag (`Optional<T>`) beans are never added there. `@TestBean EntityLinks` via `HateoasTestingSupport.createModuleEntityLinks(Controller.class)` only when the controller uses `EntityLinks`. `@MvcComponent` beans are never listed in `controllers`/`@Import`. Migrate a module to the per-module form above when touching its REST tests.

## Integration Tests (@KlabisModuleTest)

Never use `@ApplicationModuleTest` directly — use `@KlabisModuleTest` (`com.klabis`), which wraps it with
`verifyAutomatically = false` and `@ActiveProfiles("test")` and exposes `mode`, `module` and
`extraIncludes`. Automatic verification runs a full ArchUnit import per test class; the module structure
is verified once by `ModuleStructureVerificationTest`.

```java
@KlabisModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@DisplayName("Member Registration Integration Tests")
class MemberRegistrationIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithKlabisMockUser(username = "admin", authorities = {Authority.MEMBERS_CREATE})
    void shouldRegisterAdult() throws Exception {
        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .content("""
                    {"firstName":"Jan","lastName":"Novák",...}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/members")));
    }
}
```

Use `BootstrapMode.DIRECT_DEPENDENCIES` for security/controller tests that don't need the full graph.

Integration test scope rules:
- Test **single module** — mock dependencies from other business modules
- Test only the **happy path** — edge cases and input variations belong in unit tests
- **Do not assert unnecessary details** — assert only that the operation ended with expected outcome
- Use MockMvc for controller triggers, direct method calls for listeners/other primary adapters

## E2E Tests

```java
@E2ETest
@Sql(scripts = "/sql/test-member-lifecycle-setup.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Member Lifecycle E2E Test")
class MemberLifecycleE2ETest {

    @TestBean EmailService emailServiceStub;

    // Make async event handlers run synchronously
    @TestConfiguration
    static class SyncConfig {
        @Bean TaskExecutor taskExecutor() { return new SyncTaskExecutor(); }
    }
}
```

`@E2ETest` is a meta-annotation combining `@KlabisModuleTest(mode = ALL_DEPENDENCIES)`, `@AutoConfigureMockMvc`, `@CleanupTestData`, and `@Import(TestApplicationConfiguration.class)`.

E2E scope rules:
- 1 test per aggregate root verifying its **full lifecycle** (register → update → suspend → resume)
- **Verify progression only** — status codes and minimal navigation checks
- **Never inject repositories** — verify state only through API responses
- **Not in E2E:** response JSON structure (→ controller unit tests), domain events (→ integration tests)

## Security: @WithKlabisMockUser

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithKlabisMockUserSecurityContextFactory.class)
public @interface WithKlabisMockUser {
    String userId() default "";      // Random UUID if blank
    String memberId() default "";    // Blank = user without member record
    String username() default "";    // Default "ZBM8001"
    Authority[] authorities() default {};
}
```

Usage:
```java
@Test
@WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
void adminCanReadMembers() { }

@Test
@WithKlabisMockUser(memberId = "uuid", username = "ZBM0101")
void memberCanUpdateOwnProfile() { }
```

Never use `@WithMockUser` — it creates a generic principal incompatible with `KlabisJwtAuthenticationToken`.

## Converter Tests

Test `Converter<S,T>` implementations with a plain Spring context importing only the generated impls — no MVC slice:

```java
@ExtendWith(SpringExtension.class)
@Import({MemberDetailsConverterImpl.class})
class MemberMappingTests {

    @Autowired MemberDetailsConverter detailsConverter;

    @Test
    void shouldMapMinorMemberWithGuardian() {
        Member member = MemberTestDataBuilder.aMember().withGuardian(...).build();
        MemberDetailsResponse response = detailsConverter.convert(member);

        assertThat(response.guardian()).isNotNull();
    }
}
```

Test various domain object states — active/suspended, adult/minor, with/without optional fields.

## Test Data Builders

Always use fluent builders — never construct domain objects inline:

```java
// Domain objects
Member member = MemberTestDataBuilder.aMember()
    .withId(UUID.randomUUID())
    .withFirstName("Jan")
    .withLastName("Novák")
    .build();

// Registration command
Member.RegisterMember command = MemberTestDataBuilder.aMember().toRegisterMemberCommand();

```

Builder provides sensible defaults — override only what is relevant to the test.
When adding a new aggregate, create corresponding `<Aggregate>TestDataBuilder`.

Never mock data objects (entities, value objects, DTOs) — always use real instances via builders.

## Custom Assertions

Use `MemberAssert` instead of raw `assertThat` for domain objects:

```java
MemberAssert.assertThat(result)
    .hasFirstName("Jan")
    .hasLastName("Novák")
    .isActive()
    .hasGuardian(null);
```

When adding a new aggregate, create a corresponding `<Aggregate>Assert` extending `AbstractAssert`.

## Memento Round-Trip Tests

Every Memento class needs a round-trip test:

```java
@Test
void shouldPreserveAllDataInRoundTrip() {
    Member original = MemberTestDataBuilder.aMember().withAllOptionalFields().build();

    MemberMemento memento = MemberMemento.from(original);
    Member reconstructed = memento.toMember();

    assertThat(reconstructed).usingRecursiveComparison().isEqualTo(original);
}

@Test
void shouldHandleNullOptionalFields() {
    Member original = MemberTestDataBuilder.aMember().build();  // minimal

    Member reconstructed = MemberMemento.from(original).toMember();

    assertThat(reconstructed).usingRecursiveComparison().isEqualTo(original);
}
```

## SQL Test Data

Pre-populate state for integration/E2E tests via `@Sql` scripts in `src/test/resources/sql/`:

```java
@Sql(scripts = "/sql/test-members-setup.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
void shouldUpdateMember() { }
```

Use `@CleanupTestData` on E2E tests — tests share a single H2 instance.

## Gotchas

| Problem | Solution |
|---------|----------|
| `@WebMvcTest` fails with `UnsatisfiedDependencyException` / `NoSuchBeanDefinition` | Migrated module: a web bean's dependency is missing in a `*WebMvcMockitoBeans` (also check `common` and `extraIncludes` modules). Legacy: missing `@WithPostprocessors` or a dependency not listed in it |
| Asserting published domain events | `@RecordApplicationEvents` + `ApplicationEvents`, not a custom `@EventListener` bean (`@Import` on the class splits the context cache key) |
| Domain events not fired in E2E test | Add `SyncTaskExecutor` `@TestConfiguration` |
| `@DataJdbcTest` doesn't find custom repos | Add `includeFilters = @Filter(type = ANNOTATION, value = Repository.class)` |
| Tests interfere with each other in H2 | Use `@CleanupTestData` or `@Sql(statements = "DELETE FROM ...")` |
| `EntityLinks` not available in legacy `@WebMvcTest(controllers=...)` | Provide `@TestBean EntityLinks` via `HateoasTestingSupport.createModuleEntityLinks()` (not needed with `@ModuleSlicing`) |
| Duplicate-mock error on context bootstrap | Type already mocked by `@WithPostprocessors` or a `*WebMvcMockitoBeans` — use `@Autowired` |
| Test needs a mock of a repository, secondary port or adapter-package bean | Design smell — expose the data via a primary port (and load adapter beans for real); see "Mock only primary ports" |
| Another Spring context is created for a controller test | The test class adds its own `@MockitoBean`/`@Import`/profile — move it into the module's meta-annotation |
| `@WithMockUser` causes ClassCastException | Replace with `@WithKlabisMockUser` |
| Link authorization (`klabisLinkTo`/`klabisAfford`) depends on test order | The static `HalFormsSupport` instance is bound per test method by `HalFormsSupportInstanceTestExecutionListener` (`spring.factories`) and is `null` outside it — plain unit tests and `@BeforeAll` code get unfiltered links; a class declaring `@TestExecutionListeners` must use `MERGE` |
