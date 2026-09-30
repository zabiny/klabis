---
name: backend_test_run_2026_09_30_legal_guardians_complete
description: Backend test run on legal-guardians-refactor; 262/263 passed; LegalGuardianControllerTest email required field failure
metadata:
  type: project
---

## Test Execution Results (2026-09-30)

**Branch:** legal-guardians-refactor  
**Environment:** SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true  
**Duration:** ~2 minutes total

### Summary
- **Total Tests:** 263
- **Passed:** 262
- **Failed:** 1

### Test Breakdown

#### Legal Guardian Packages (com.klabis.members.legalguardian*)
- **Tests:** 152
- **Passed:** 151
- **Failed:** 1

**Failure Detail:**
```
Test: com.klabis.members.legalguardian.infrastructure.restapi.LegalGuardianControllerTest$GetGuardian.returns profile with self link and update affordance for MEMBERS:MANAGE

Error: java.lang.AssertionError: No matching value at JSON path "$._templates.updateLegalGuardian.properties[?(@.name=='email')].required"

Location: LegalGuardianControllerTest.java:78
```

The test expects the `email` property in the `updateLegalGuardian` HAL-FORMS template to have a `required` field, but it's missing from the response.

#### MemberControllerApiTest
- **Tests:** 111
- **Passed:** 111
- **Failed:** 0

✓ All tests passed

### Root Cause
The failure indicates that the `email` field in the HAL-FORMS template for updating legal guardians is not being marked as required. This is likely a specification or template assembly issue where the `email` property needs the `required` attribute added to the `updateLegalGuardian` template in the HAL-FORMS response.
