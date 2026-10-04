---
name: rebac2-field-security-evaluator
description: Field security (serializer, request advice, HAL-FORMS properties) all decide via AuthorizationEvaluator.canReadField/canWriteField/canReadRequestField; HalFormsSupport needs a bean instance or properties are unfiltered
metadata:
  type: project
---

Since rebac-2 group 5, `FieldSecurityBeanSerializerModifier`, `RequestBodyFieldAuthorizationAdvice` and HAL-FORMS property filtering in `HalFormsSupport` call `AuthorizationEvaluator` (accessor `Method` + record / `TargetRef`). Record target = component with `@TargetId`/`@OwnerId`, else the single UUID-convertible component when any field is `@OwnerVisible`; a Collection-valued target (event coordinators) is a set of owners, not a target.

**Why:** one code path so offered form fields match enforced checks.

**How to apply:** a `@WebMvcTest` that asserts HAL-FORMS property filtering needs a `HalFormsSupport` bean (provided by `KlabisWebMvcSliceConfiguration`) — with no bean the static INSTANCE is null and properties are unrestricted (method link auth is skipped the same way). Frontend submits all template properties including `readOnly` ones, so a read-only-because-unwritable PATCH field would be sent back and rejected with 403 unless the frontend drops them. See [[project-rebac2-test-snapshot-infra]].
