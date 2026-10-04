---
name: project_rebac2_test_snapshot_infra
description: How tests get a permission snapshot - it rides on KlabisJwtAuthenticationToken, slices import KlabisWebMvcSliceConfiguration
metadata:
  type: project
---

The snapshot is carried by `KlabisJwtAuthenticationToken` (memoized supplier); `AuthorizationSnapshotProvider.current()` (plain `@Component` class) reads it from the SecurityContext, `AuthorizationSnapshotLoader` loads it from DB/sources. Tests need no provider mocks.

- Token with fixed snapshot: `KlabisAuthenticationFactory.createAuthenticationToken(jwtParams[, snapshot])`; `@WithKlabisMockUser` uses it (targetGrants -> snapshot). No `Authentication.details` hack, no BeanPostProcessor.
- `@WebMvcTest` slices: every `*WebMvcTest` / `CommonInfrastructureWebMvcSetup` imports `KlabisWebMvcSliceConfiguration` (clock, encryption, HalFormsSupport, AuthorizationEvaluator, AuthorizationSnapshotProvider).
- Hand-built evaluator: `SecurityContextAuthorizationEvaluator.create()` or `new AuthorizationEvaluator(new AuthorizationSnapshotProvider(), conversionServiceProvider)`.
- Non-Klabis authentications (`@WithMockUser`, `TestingAuthenticationToken`) become a snapshot of their known authorities via `AuthorizationSnapshot.ofGrantedAuthorities`.

**Why:** nested non-`@TestConfiguration` configs in test classes get component-scanned into full contexts; use `@TestConfiguration` or `@Bean(autowireCandidate = false)` for test beans.
**How to apply:** never build a `Jwt` + converter by hand in a test; `ResourceServerSecurityConfiguration` resolves the loader through an `ObjectProvider` because slices have no `PermissionService`.
