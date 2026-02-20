# Report: Testovací struktura modulu members

Vygenerováno: 2026-02-20

## Přehled všech testovacích tříd

### Unit Tests

#### Domain (AggregateRoot)
| Třída | Kategorie | Hodnocení |
|-------|-----------|-----------|
| `MemberTest` | ✅ Domain unit test | **OK** - Kompletní testy creation, guardiáni, kontaktů, domain events |
| `MemberTerminationTest` | ✅ Domain unit test | **OK** - Testy terminate příkazů a stavových přechodů |
| `MemberUpdateTest` | ✅ Domain unit test | **OK** - Testy update logiky |
| `BirthNumberTest` | ✅ Value object test | **OK** |
| `BankAccountNumberTest` | ✅ Value object test | **OK** |
| `EmailAddressTest` | ✅ Value object test | **OK** |
| `PhoneNumberTest` | ✅ Value object test | **OK** |
| `AddressTest` | ✅ Value object test | **OK** |
| `PersonNameTest` | ✅ Value object test | **OK** |
| `PersonalInformationTest` | ✅ Value object test | **OK** |
| `GuardianInformationTest` | ✅ Value object test | **OK** |
| `NationalityTest` | ✅ Value object test | **OK** |
| `ExpiringDocumentTest` | ✅ Value object test | **OK** |
| `RegistrationNumberTest` | ✅ Value object test | **OK** |
| `RegistrationNumberGeneratorTest` | ✅ Domain service test | **OK** |

#### Application (Service)
| Třída | Kategorie | Hodnocení |
|-------|-----------|-----------|
| `RegistrationServiceTest` | ✅ Application unit test | **OK** - Mocky repozitářů, testuje jen service logiku |
| `ManagementServiceTest` | ✅ Application unit test | **OK** - Mocky repozitářů, testuje jen service logiku |

#### Controller
| Třída | Kategorie | Hodnocení |
|-------|-----------|-----------|
| `MemberControllerApiTest` | ✅ Controller @MvcWebTest | **OK** - Mocky service, testuje jen controller logiku |
| `UpdateMemberApiTest` | ✅ Controller @MvcWebTest | **OK** - Mocky service, testuje jen controller logiku |
| `MemberControllerSecurityTest` | ⚠️ Controller test | **MOžná PŘEBYTEČNÁ** - Zkontrolovat, zda nepřebíjí z MemberControllerApiTest |
| `MemberPermissionsLinkProcessorTest` | ✅ Component test | **OK** - Testuje HATEOAS link processor |

#### Repository (Unit)
| Třída | Kategorie | Hodnocení |
|-------|-----------|-----------|
| `MemberRepositoryTest` | ✅ Repository @DataJdbcTest | **OK** - Testy JDBC persistence, memento mapping |
| `MemberMementoTest` | ✅ Memento unit test | **OK** - Testy Memento pattern |

#### Domain Events
| Třída | Kategorie | Hodnocení |
|-------|-----------|-----------|
| `MemberCreatedEventTest` | ✅ Domain event test | **OK** |
| `MemberDomainEventTest` | ❓ Zkontrolovat | **NEJASÁ** - Možná duplikace s MemberTest.DomainEvents |

---

### Integration Tests (@ApplicationModuleTest)

| Třída | Trigger | Cíle | Hodnocení |
|-------|---------|------|-----------|
| `MemberRegistrationIntegrationTest` | MockMvc POST /api/members | Happy path + unique reg numbers | **OK** - 2 testy (happy path, duplicitní reg numbers) |
| `RegisterMemberAutoProvisioningTest` | ❓ Nezjištěno | Auto-provisioning | **OK** - Integration test pro specifický případ |
| `MemberMappingTests` | ❓ Nezjištěno | MapStruct mapping | **OK** - Testuje DTO mapping |

---

### E2E Tests (@E2EIntegrationTest)

| Třída | Popis | Aggregate lifecycle | Status |
|-------|-------|-------------------|--------|
| `MemberRegistrationE2ETest` | API → DB → Outbox → Email | ❌ **NEKOMPLETNÍ** - Jen registrace | ⚠️ **TODO v kódu** - Komentář uvádí kroky 5-10 chybí |
| `GetMemberE2ETest` | Create → Get by ID | ❌ Není lifecycle test | ⚠️ **POZNÁMKA v kódu** - "refactor into WebMvcTest" |
| `MemberTerminationE2ETest` | Register → Terminate → Verify | ✅ **ANO** - Register → Terminate workflow | **OK** - Plný lifecycle test |
| `PasswordSetupFlowE2ETest` | Password reset flow | ❓ Nezjištěno | **OK** - Specifický flow |

---

## Provedené změny

### ✅ Fáze 1: Oprava MemberTerminationIntegrationTest

**Problém:** `MemberTerminationIntegrationTest` byl chybně pojmenovaný - ve skutečnosti to byl unit test s Mockito mocks, nikoliv integration test.

**Řešení:**
- Všechny termination testy byly přesunuty do `ManagementServiceTest.java`
- `MemberTerminationIntegrationTest.java` byl smazán
- Testy byly zorganizovány jako vnořené třídy:
  - `SuccessfulTerminationTests` - 3 testy
  - `AlreadyTerminatedTests` - 1 test
  - `ConcurrentTerminationTests` - 1 test
  - `AuthorizationTests` - 2 testy
  - `MemberNotFoundTests` - 1 test

**Výsledek:** `ManagementServiceTest` nyní obsahuje 10 testů (2 původní update testy + 8 termination testů).

---

## Plánované změny

### ⏳ Fáze 2: Vytvořit E2E test pro plný Member lifecycle

**Cíl:** Jeden komplexní E2E test pokrývající kompletní životní cyklus Member aggregate:

1. **Register** member → vytvoření v DB
2. **Setup password** → aktivace uživatele
3. **Get** member → ověření dat
4. **Update** member → změna dat
5. **Terminate** member → deaktivace
6. **Verify** termination → ověření stavu

### ⏳ Fáze 3: Vyčistit fragmentované E2E testy

**E2E testy k odstranění:**
- `GetMemberE2ETest` - refaktorovat do @WebMvcTest nebo začlenit do komplexního E2E
- `MemberRegistrationE2ETest` - začlenit do komplexního E2E (využít existující assertions)

**Assertions k zachování:**
(Shromážděny z existujících testů)

#### Z GetMemberE2ETest:
- Ověření vytvoření member a získání přes GET /api/members/{id}
- Ověření všech polí (firstName, lastName, dateOfBirth, nationality, gender, email, phone, address)
- Ověření guardian informací pro nezletilé členy
- Ověření birthNumber a bankAccountNumber
- Ověření 404 při neexistujícím ID

#### Z MemberRegistrationE2ETest:
- Ověření Location header po registraci
- Ověření MemberCreatedEvent v outbox
- Ověření odeslání emailu pro password setup

#### Z MemberTerminationE2ETest:
- Ověření terminace (active=false, deactivationReason, deactivatedAt, deactivationNote)
- Ověření that second termination is rejected
- Ověření that terminated member appears in list with active=false

---

## Statistika

| Kategorie | Původní počet | Počet úprav | Aktuální stav |
|-----------|---------------|-------------|---------------|
| Domain unit tests | 16 | 0 | ✅ 16 |
| Application unit tests | 2 | +1 | ✅ 3 (merged) |
| Controller tests | 3 | 0 | ✅ 3 |
| Repository unit tests | 2 | 0 | ✅ 2 |
| Integration tests | 4 | -1 | ⏳ 3 |
| E2E tests | 4 | -2 (plán) | ⏳ 2 (po fázi 3) |

---

## Celkové hodnocení

**Před změnami:**
- ✅ Unit tests: Výborné pokrytí domény a aplikační vrstvy
- ⚠️ Integration tests: 1 test s matoucím názvem
- ⚠️ E2E tests: Fragmentované, testují jen části lifecycle

**Po změnách (cíl):**
- ✅ Unit tests: Správně organizované
- ✅ Integration tests: Jasné názvy, správná anotace
- ✅ E2E tests: 1 komplexní test per aggregate root (Member)

**Soulad s definicemi:**

| Definice | Požadavek | Stav |
|----------|-----------|------|
| Unit test - Domain | AggregateRoot unit test | ✅ MemberTest, MemberTerminationTest |
| Unit test - Application | Service class with mocks | ✅ RegistrationServiceTest, ManagementServiceTest |
| Unit test - Controller | @MvcWebTest with mocked service | ✅ MemberControllerApiTest |
| Unit test - Repository | Repository adapter unit tests | ✅ MemberRepositoryTest |
| Integration test | @ApplicationModuleTest, 1 happy + 1 error path | ✅ MemberRegistrationIntegrationTest |
| E2E test | 1 test per aggregate, full lifecycle | ⏳ Fáze 2+3 |
