---
name: Klabis Tests pyramid 
description: Reviews HATEOAS links for best practises (link to GET api only, affordance to POST/PUT/PATCH/DELETE api,..) 
category: Klabis
tags: [klabis, review]
allowed-tools: Read(./*), Glob(./*), Grep(./*)
---

Review backend tests for alignment with following tests structure. Show list of all test classes with target test category and suggestions for improvements or cleanups.  

# Unit testy
- domain: AggregateRoot unit test
- application: Service class unit test (mock repository/other dependencies). Test only logic from service class (do not repeat tests from domain)
- controller: @MvcWebTest test with application layer dependencies mocked (service class, etc). Test only logic from controller class
- repository: unit tests for repository adapter class and memento classes

# Integration test
@ApplicationModuleTest which calls primary adapter methods. 1 test for happy path + 1 test for expected error path (missing user, 1 test for incorrect data, etc.) 
- triggers: either MockMvc calling API endpoints or calling primary adapter functions directly. 

# E2E test 
- @E2EIntegrationTest test 
- 1 test per aggregate root. Test verifies aggregate progression through it's full lifecycle. 
- triggers - MockMvc "calling" APIs


