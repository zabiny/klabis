---
name: project-halformssupport-static-instance
description: HalFormsSupport static INSTANCE is fail-fast; Spring tests bind it via a listener, plain JUnit tests via PlainHalFormsSupportExtension
metadata:
  type: project
---

`HalFormsSupport.klabisAfford*/klabisLinkTo` throw `IllegalStateException` when no instance is bound (was fail-open).
`HalFormsSupportInstanceTestExecutionListener` only runs for Spring tests; plain JUnit tests (no SpringExtension) never get TestExecutionListeners, so they previously passed only thanks to an instance leaked from an earlier context (order-dependent). `PlainHalFormsSupportExtension` (auto-detected via META-INF/services) now binds one over `SecurityContextAuthorizationEvaluator`.

**Why:** B10 of rebac-2 simplify review.
**How to apply:** a new plain unit test of a postprocessor needs no setup; a Spring slice test must import `KlabisWebMvcSliceConfiguration`. See [[project-rebac2-test-snapshot-infra]].
