# Klabis best practises for backend tests

This file describes best practises for Backend tests. Use it whenever planning, designing writing, updating, refactoring or reviewing backend tests. 

# Best practises
- use AssertJ for test assertions 

## **TestDataBuilder Pattern:**
- Create test data builders in `src/test/java/` following existing patterns
- Examples: `EventTestDataBuilder`, `MemberTestDataBuilder`, `CalendarItemTestDataBuilder`
- Use fluent API with `withXxx()` methods and static factory `anXxx()`

# Structure of the tests - testing pyramid

## Unit testy

### Domain 
Tested class: Aggregate root, Complex value objects 

- `src/test/java/com/klabis/{module}/domain/*Test.java`
- Use pure JUnit 5, no Spring context. No mocks should be needed


### Application
Tested class: application Service classes

- `src/test/java/com/klabis/{module}/application/*Test.java`
- Use pure JUnit5, AssertJ. no Spring context.  
- Mock infrastructure dependencies (ports). 
- Test only logic from service class (do not repeat tests from domain classes - aggregate roots, value objects) 

### Controllers 

Tested class: RestController classes 

- @MvcWebTest
- Location: `src/test/java/com/klabis/{module}/infrastructure/restapi/*Test.java` or `*ApiTest.java`
- Mocked service layer (primary ports) - does NOT hit database
- Tests HTTP status codes (200, 201, 204, 400, 403, 404), validation, error responses

Example test: ../examples/MemberControllerApiTest.java

### Mappers 

Tested class: Domain -> ResponseDTO mappers 

- detailed tests of various domain object states / data and their mapping to DTOs used as API responses 

### Repository 

Tested class: implementation of Domain Repository interface. 

- @DataJdbcTest + import of domain repository implementation class
- test against real database 
- prepare data needed for test in DB using @Sql annotation (either on test class or on test method)
- test various combinations of input paramters (query parameters, various configurations of domain object for save method, etc) and verify in detail expected result (returned data from query methods, data saved into DB)

Example test: ../examples/MemberRepositoryTest.java

## Integration tests

Tested subject: Happy path of use cases triggered through primary adapter methods (controllers, message listeners, etc) 

- Repository: `src/test/java/com/klabis/{module}/infrastructure/persistence/*IntegrationTest.java`
- Service/Controller integration: `src/test/java/com/klabis/{module}/infrastructure/restapi/*IntegrationTest.java` with `@ApplicationModuleTest` (and usually Scenario)
- tests usually single module (no dependencies). Dependencies from other business modules are mocked in the the test. 
- Tests happy path flows with real service layer and database
- avoid testing various input parameters for same method call (that is done at unit tests level).
- do not assert unnecessary details here (detailed assertions should be at unit tests). Assertion usually should be based on fact that operation ended in expected outcome (success, expected failure). 
- usually use MockMvc trigger for Controller tests and direct method call for other primary adapters (listeners, etc). 

### Additional Integration tests best practises

**Module Boundaries & @Sql:**
- Don't inject cross-module repositories - use `@Sql` for FK setup
- `@Sql` requires compile-time constants (no string concat)
- Check schema constraints (column lengths, FK refs)
- Use realistic test values (e.g., "OOB" not "Organizer A")

## E2E test

Tested subject: identified multistep workflows. Usually at least 1 E2E test for every AggregateRoot testing full lifecycle of such aggregate root. 

- Location: `src/test/java/com/klabis/{module}/*E2ETest.java` with `@E2ETest` meta-annotation
- Tests complete lifecycle flows (register → list → activate → update → terminate)
- test should concentrate on verify expected outcome of whole workflow (succeess, expected failure)
- avoid tests concentrating on unnecessary details (avoid testing multiple inputs for same operation, etc) which should be tested by unit tests.
- 1 test per aggregate root verifying aggregate progression through it's full lifecycle. 
- triggers - primary adapter calls (MockMvc for controllers, direct method calls for other primary ports) 
- **Scope:** Verify lifecycle PROGRESSION only — status codes and minimal navigation checks
- **Not in E2E:** Response JSON structure (→ controller unit tests), domain events (→ integration tests)
- **Never inject repositories** in E2E tests — too low-level; verify state only through API responses


