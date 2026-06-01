## Context

The Klabis backend is a Spring Modulith modular monolith with hexagonal architecture (jMolecules `@PrimaryPort` / `@SecondaryPort` / `@Port`). Two inconsistencies break the otherwise uniform dependency rules:

1. **`authorizationserver` → foreign secondary port.** `KlabisUserDetailsService` injects `com.klabis.common.users.domain.UserPermissionsRepository` — a `@SecondaryPort` owned by the `users` module — and inlines `findById(userId).orElse(UserPermissions.empty(userId))`. This is the only place in the codebase where one module depends on another module's repository. The class already documents the intent to fix it: `// TODO: hide permissions repository call behind UserService interface`.

2. **Inconsistent cross-module port placement.** Cross-module ports should live in `<module>.application` (the convention: `finance.application.DepositPort`, `users.application.PermissionService`). Three ports instead sit in the module **root** package:
   - `events.EventDataProvider` (`@SecondaryPort`, consumed by calendar)
   - `events.EventScheduleQuery` (`@SecondaryPort`, consumed by calendar)
   - `members.MemberFinancialStatePort` (`@SecondaryPort`, consumed by `members.application` itself, implemented by `finance`)

This change is purely structural — no spec, no observable behavior is affected (see proposal's gate-check).

## Goals / Non-Goals

**Goals**
- `authorizationserver` depends only on a primary port of `users`, never a repository.
- All cross-module ports live in `<module>.application`, exposed via `@NamedInterface("application")`.
- Modulith verification (`ModuleStructureVerificationTest`) keeps passing — same cross-module access, no more.

**Non-Goals**
- Not touching `events.domain.Events` / `EventRepository` — that split (public `@Port` read interface in `domain` vs internal `@SecondaryPort` write interface) is already correct and intentional.
- Not moving repositories out of `domain` — repositories are domain secondary ports and stay there.
- No change to any port's method contract, no new methods, no signature changes.

## Decisions

### Decision 1 — authorizationserver consumes `PermissionService`, not the repository

Swap the `UserPermissionsRepository` dependency in `KlabisUserDetailsService` for `PermissionService`:

```java
// before
UserPermissions permissions = permissionsRepository.findById(user.getId())
        .orElse(UserPermissions.empty(user.getId()));
// after
UserPermissions permissions = permissionService.getUserPermissions(user.getId());
```

`PermissionServiceImpl.getUserPermissions()` is `permissionsRepository.findById(userId).orElse(UserPermissions.empty(userId))` — byte-identical semantics, including the lazy "missing → empty authorities" rule. `getUserPermissions` is `@Transactional(readOnly = true)`; `loadKlabisUserDetails` already runs in a read context, so this is safe. Remove the `// TODO`.

**Alternative considered:** add the lookup to `UserService` (as the TODO literally suggests). Rejected — `PermissionService` is the existing, purpose-built primary port for permissions; `UserService` is about authentication/identity. Routing permissions through `UserService` would conflate two concerns. Consuming `PermissionService` is the smaller, more correct move.

### Decision 2 — move the three ports into `<module>.application`

| Port | From | To |
|---|---|---|
| `EventDataProvider` | `com.klabis.events` | `com.klabis.events.application` |
| `EventScheduleQuery` | `com.klabis.events` | `com.klabis.events.application` |
| `MemberFinancialStatePort` | `com.klabis.members` | `com.klabis.members.application` |

Each move is: relocate the interface, update its `*Impl`/adapter and all consumers' imports. No logic changes.

`MemberFinancialStatePort` is a members-owned secondary port implemented by `finance` (`finance.application.MemberFinancialStateAdapter`) and consumed by `members.application.ManagementService`. The move keeps that arrangement — only the package changes; finance still implements it, members still consumes it.

### Decision 3 — expose `application` as a Modulith named interface where needed

Today the ports are reachable cross-module because they sit in the module **root**, which Modulith exposes by default. Moving them into a sub-package hides them unless `application` is a named interface. So:

- `events/application/package-info.java` — create with `@NamedInterface("application")`.
- `members/application/package-info.java` — add `@NamedInterface("application")` (file exists but is bare).

This mirrors `finance/application/package-info.java`. Consumers in other modules import `com.klabis.<module>.application.<Port>`; Modulith allows it via the named interface.

> Risk: over-exposing `application` could leak internal services (e.g. `EventManagementService`) cross-module. Mitigation — Modulith only flags *actual* illegal references at verification time; we run `ModuleStructureVerificationTest` after the move. If a leak appears, narrow to a dedicated named interface (e.g. `events.ports`) instead of the whole `application` package. Start with the `finance`-style whole-package exposure since that is the established precedent.

## Migration sequence (vertical slices, each independently committable + testable)

1. **authorizationserver swap** — Decision 1. Smallest, isolated; run authorizationserver + users tests.
2. **EventDataProvider + EventScheduleQuery move** — Decision 2 + 3 for events; both ports together (same target package + same consumer module). Run events + calendar + Modulith tests.
3. **MemberFinancialStatePort move** — Decision 2 + 3 for members. Run members + finance + Modulith tests.

Each slice is a separate commit. Order is independent — slice 1 has no dependency on 2/3.

## Risks / Trade-offs

- **Modulith named-interface over-exposure** — mitigated by verification test + fallback to a narrower named interface (see Decision 3).
- **Stale references in tests** — tests importing old package paths must be updated; caught at compile time.
- **Low overall risk** — no runtime logic changes; the compiler and existing test suite are the safety net.

## Open Questions

- Should `events.application` expose the *whole* package (finance-style) or a narrow `@NamedInterface` listing only the two ports? Default: whole-package for consistency with `finance`; revisit only if verification flags a leak.
