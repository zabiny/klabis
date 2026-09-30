---
name: frontend_test_run_2026_09_30_build_error
description: Frontend test suite (2026-09-30) — 2124/2124 passed but build failed with TS2345
metadata:
  type: project
---

## Test Results
- **Tests:** 2124/2124 passed (100%)
- **Build:** FAILED

## Build Error

**File:** `src/hooks/useHalFormData.test.tsx`
**Line:** 893, column 38
**Error Code:** TS2345

```
Argument of type '{ method: string; target: string; }' is not assignable to parameter of type 'HalFormsTemplate'.
  Property 'properties' is missing in type '{ method: string; target: string; }' but required in type 'HalFormsTemplate'.
```

### Context

Line 889 in the test creates a template:
```typescript
const template = {...mockHalFormsTemplate, method: 'PUT', target: '/api/legal-guardian-groups/1/guardians'};
```

The issue: TypeScript treats the overlay `{ method: 'PUT', target: '...' }` as a separate type that overrides the spread from `mockHalFormsTemplate`. The literal object type `{ method: string; target: string; }` does not include the `properties` field, and TypeScript's type narrowing sees this.

### Function Signature

`useHalFormData` expects:
```typescript
selectedTemplate: HalFormsTemplate | null
```

Where `HalFormsTemplate` requires a `properties` field (per the validation on line 91 of useHalFormData.ts).

## Date
2026-09-30
