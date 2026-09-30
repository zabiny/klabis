---
name: backend_test_run_2026_09_30_legal_guardians_controller_test
description: LegalGuardianControllerTest single class run - 9/10 passed, email required field missing from updateLegalGuardian template
metadata:
  type: project
  date: 2026-09-30
---

## Test Run Summary

- **Test class**: `com.klabis.members.legalguardian.infrastructure.restapi.LegalGuardianControllerTest`
- **Total tests**: 10
- **Passed**: 9
- **Failed**: 1
- **Duration**: ~1m 47s
- **Environment**: `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`

## Failed Test

**Test name**: `com.klabis.members.legalguardian.infrastructure.restapi.LegalGuardianControllerTest$GetGuardian.returns profile with self link and update affordance for MEMBERS:MANAGE`

**Failure message**: 
```
java.lang.AssertionError: No matching value at JSON path "$._templates.updateLegalGuardian.properties[?(@.name=='email')].required"
```

**Root cause**: The `email` property in the `updateLegalGuardian` HAL-FORMS template is missing the `required` field. The test expects the email field to have a `required` property, but it's not present in the response.

**Location**: `com/klabis/members/legalguardian/infrastructure/restapi/LegalGuardianControllerTest.java:78`

## Fix Required

The updateLegalGuardian template in the OpenAPI spec needs to mark email as a required field, or the HAL-FORMS template assembler needs to include this property.
