# Design Decisions (ADRs)

This file records non-obvious architectural choices for the Klabis backend as Architecture Decision Records (ADRs). Read it before proposing significant architectural changes — the reasoning behind existing decisions is documented here. Add a new `ADR-NNN` section for any similarly-sized decision introduced in the future.

## ADR-001: Cross-module ports live in `<module>.application`, consume primary ports only

**Status:** Accepted

**Context:**

The Modulith exposes module functionality to other modules through jMolecules ports (`@PrimaryPort` / `@SecondaryPort`). Before this decision the convention was inconsistent: some cross-module ports lived in the module root package, others in `<module>.application`, and at least one consumer reached past a module's public API straight into a foreign repository. `KlabisUserDetailsService` (authorizationserver) injected `com.klabis.common.users.domain.UserPermissionsRepository` directly — a secondary port — carrying a standing `// TODO` acknowledging it should not depend on another module's repository.

This inconsistency made dependency rules unpredictable, let new modules couple to foreign repositories, and weakened the named-interface boundaries that Spring Modulith can verify.

**Decision:**

1. Cross-module ports (ports consumed across Modulith module boundaries) live in the `<module>.application` package, exposed via `@org.springframework.modulith.NamedInterface("application")` declared in that package's `package-info.java`. The module root package is NOT used for cross-module ports.

   ```java
   // com/klabis/<module>/application/package-info.java
   @org.springframework.modulith.NamedInterface("application")
   package com.klabis.<module>.application;
   ```

   `finance/application/package-info.java` is the reference. As part of this change `EventDataProvider`, `EventScheduleQuery`, and `MemberFinancialStatePort` moved from their module root packages into the respective `<module>.application` packages.

2. A module consumes another module's **primary port**, never a foreign repository or any other secondary port. The motivating example: `KlabisUserDetailsService` now consumes `com.klabis.common.users.application.PermissionService` (a primary port) instead of `UserPermissionsRepository` (a secondary port), removing the standing `// TODO`.

**Consequences:**

- Uniform, predictable dependency rules across the Modulith: cross-module collaboration always flows through a primary port published in `<module>.application`.
- Future modules cannot quietly depend on a foreign repository; the secondary-port/persistence layer of a module stays private to that module.
- The Modulith `ModuleStructureVerificationTest` enforces the named-interface boundary, so violations fail the build rather than being caught only in review.
- Slightly more indirection: a module that only needs read data must still expose (or reuse) a primary port rather than letting consumers reach into its repository.

**References:** OpenSpec change `unify-cross-module-ports`.
