# Klabis best practises for backend tests

This file describes best practises for Backend tests. Use it whenever planning, designing writing, updating, refactoring or reviewing backend tests. 

# Structure of the tests - testing pyramid

# Unit testy

## Domain 
Tested class: Aggregate root, Complex value objects 

- Detailed pure JUnit test. 
- No mocks should be needed. 

## Application
Tested class: application Service classes

- Detailed pure JUnit test. 
- Mock infrastructure dependencies (ports). 
- Test only logic from service class (do not repeat tests from domain classes - aggregate roots, value objects) 

## Controllers 

Tested class: RestController classes 

- @MvcWebTest 
- mock primary ports (Service interface / class). 
- test logic from controller class
    - for GET methods, test various configurations of data returned from primary port (domain objects) and verify that JSON structure in the response 
    - call API with various combinations of valid parameters (including body for POST/PUT/PATCH/DELETE), verify that primary port method is called with expected parameters. 
    - mock exceptions thrown by service method and verify that expected API response is returned 
    - HTTP status codes (200, 201, 204, 400, 403, 404, etc.)
    - Service method invocation with expected parameters (using Mockito.verify)
    - HATEOAS links and templates presence and basic structure
    - Exception handling and error responses
    - various validations (required attributes, special value formats validated in ResponseDTOs)

Example test: ../examples/MemberControllerApiTest.java

## Mappers 

Tested class: Domain -> ResponseDTO mappers 

- detailed tests of various domain object states / data and their mapping to DTOs used as API responses 

## Repository 

Tested class: implementation of Domain Repository interface. 

- @DataJdbcTest + import of domain repository implementation class
- test against real database 
- prepare data needed for test in DB using @Sql annotation (either on test class or on test method)
- test various combinations of input paramters (query parameters, various configurations of domain object for save method, etc) and verify in detail expected result (returned data from query methods, data saved into DB)

Example test: ../examples/MemberRepositoryTest.java

# Integration tests

Tested subject: Happy path of use cases triggered through primary adapter methods (controllers, message listeners, etc) 

- @ApplicationModuleTest + Scenario
- primarily test happy path flows.
- avoid testing various input parameters for same method call (that is done at unit tests level).
- do not assert unnecessary details here (detailed assertions should be at unit tests). Assertion usually should be based on fact that operation ended in expected outcome (success, expected failure). 
- usually use MockMvc trigger for Controller tests and direct method call for other primary adapters (listeners, etc). 

# E2E test

Tested subject: identified multistep workflows. Usually at least 1 E2E test for every AggregateRoot testing full lifecycle of such aggregate root. 
 
- @E2EIntegrationTest test 
- test should concentrate on verify expected outcome of whole workflow (succeess, expected failure)
- avoid tests concentrating on unnecessary details (avoid testing multiple inputs for same operation, etc) which should be tested by unit tests.
- 1 test per aggregate root verifying aggregate progression through it's full lifecycle. 
- triggers - primary adapter calls (MockMvc for controllers, direct method calls for other primary ports) 


