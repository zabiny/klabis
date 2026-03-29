# Klabis Backend Testing Guide

Testing patterns derived from the `members` module as the canonical reference.

## Test Types & When to Use Each

| Test Type | Annotation | Scope | Use for |
|-----------|-----------|-------|---------|
| Domain and Value object unit | `@ExtendWith(MockitoExtension.class)` | No Spring | Business rules, invariants, state transitions |
| Service unit | `@ExtendWith(MockitoExtension.class)` | No Spring | Service logic, mock interactions |
| Repository | `@DataJdbcTest` | JDBC slice | CRUD, custom queries |
| Controller | `@WebMvcTest` | MVC slice | HTTP status, request mapping, service calls |
| Integration | `@ApplicationModuleTest` | Module + direct deps | Full flow with real database |
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

**Critical**: `SecurityConfiguration` depends on `UserService` — always mock it:

```java
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class})
@Import({MemberMapperImpl.class})
@MockitoBean(types = {UserService.class, UserDetailsService.class})  // REQUIRED
class MemberControllerApiTest {

    @MockitoBean ManagementService managementService;
    @MockitoBean MemberRepository memberRepository;

    @TestBean EntityLinks entityLinks;  // Use HateoasTestingSupport

    static EntityLinks entityLinks() {
        return HateoasTestingSupport.createModuleEntityLinks(MemberController.class);
    }

    @Autowired MockMvc mockMvc;

    @Test
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
    void shouldReturnMemberDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(memberRepository.findById(new MemberId(id)))
            .thenReturn(Optional.of(MemberTestDataBuilder.aMemberWithId(id).build()));

        mockMvc.perform(get("/api/members/{id}", id)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jan"))
            .andExpect(jsonPath("$._links.self.href").exists());
    }
}
```

## Integration Tests (@ApplicationModuleTest)

```java
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

`@E2ETest` is a meta-annotation combining `@ApplicationModuleTest(ALL_DEPENDENCIES)`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@CleanupTestData`, and `@Import(TestApplicationConfiguration.class)`.

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

## Mapper Tests

Test Domain → ResponseDTO mappers as standalone unit tests (no Spring context):

```java
@DisplayName("MemberMapper Tests")
class MemberMapperTest {

    private final MemberMapper mapper = Mappers.getMapper(MemberMapper.class);

    @Test
    void shouldMapActiveAdultMemberToDetailsResponse() {
        Member member = MemberTestDataBuilder.aMember().withAllOptionalFields().build();
        MemberDetailsResponse response = mapper.toDetailsResponse(member);

        assertThat(response.firstName()).isEqualTo(member.getFirstName());
        assertThat(response.email()).isEqualTo(member.getEmail().value());
        assertThat(response.guardian()).isNull();
    }

    @Test
    void shouldMapMinorMemberWithGuardian() {
        Member member = MemberTestDataBuilder.aMember().withGuardian(...).build();
        MemberDetailsResponse response = mapper.toDetailsResponse(member);

        assertThat(response.guardian()).isNotNull();
        assertThat(response.guardian().email()).isEqualTo(...);
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
| `@WebMvcTest` fails with `UnsatisfiedDependencyException` | Add `@MockitoBean(types = {UserService.class, UserDetailsService.class})` |
| Domain events not fired in E2E test | Add `SyncTaskExecutor` `@TestConfiguration` |
| `@DataJdbcTest` doesn't find custom repos | Add `includeFilters = @Filter(type = ANNOTATION, value = Repository.class)` |
| Tests interfere with each other in H2 | Use `@CleanupTestData` or `@Sql(statements = "DELETE FROM ...")` |
| `EntityLinks` not available in `@WebMvcTest` | Provide `@TestBean EntityLinks` via `HateoasTestingSupport.createModuleEntityLinks()` |
| `@WithMockUser` causes ClassCastException | Replace with `@WithKlabisMockUser` |
