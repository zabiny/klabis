# Design — routing HAL affordances at generated `*Api` interfaces

## Context

`HalFormsSupport.modifyAffordanceForHalForms` (≈ line 272) walks the parameters of the method
recorded by `methodOn(...)` looking for `@RequestBody`. If it finds none it returns the affordance
untouched, skipping `HalFormsInputPayloadMetadata` entirely — which supplies both inline `options`
and `@HalForms(access = …)` handling. Because `methodOn` currently records the **controller** method,
and Java does not inherit parameter annotations from an interface, that lookup depends on each
override remembering to repeat `@RequestBody`.

Spring HATEOAS supports proxying an interface directly:
`DummyInvocationUtils.getProxyWithInterceptor` branches on `type.isInterface()` (3.0.3, line 198),
and `WebMvcLinkBuilder.linkTo` resolves the path from `method.getDeclaringClass()` (line 126), which
for an interface proxy is the interface itself. The generated `*Api` interfaces carry
`@RequestMapping`, `@RequestBody`, `@HasAuthority` and `@OwnerId` — everything the link builder and
`isMethodAuthorized` need.

Measured scope in the current tree:

| Location | Count | Converts? |
|---|---|---|
| `klabisLinkTo` / `klabisAfford` + plain `linkTo` in `backend/src/main/java` | 196 | yes |
| distinct methods referenced through `methodOn(...)` | 97 | all 97 have an interface counterpart |
| REST controllers | 20 | all 20 implement a generated `*Api` |
| `methodOn(...)` in `backend/src/test/java` | 24 | only 1 — the other 23 target test-local controllers |

## Goals / Non-Goals

**Goals:**

- Make it impossible for a controller override to silently lose HAL-FORMS input metadata.
- Leave the emitted hypermedia byte-identical: same links, affordance names, `_templates` and
  authorization outcomes.
- Update `backend-patterns` so the skill teaches the new rule rather than the one being removed.

**Non-Goals:**

- Changing which affordances appear, or under what conditions. The `if` logic stays in controllers.
- Touching the 23 test-local `methodOn(TestController.class)` call sites — those controllers
  deliberately have no interface and exercise `HalFormsSupport` directly.
- Removing `@Operation` / `@ApiResponse` / `@Parameter` from controllers: springdoc cannot see them
  through the interface, and `klabis-full.json` would lose its summaries.
- Reworking `@ExposesResourceFor` / `ControllerEntityLinks`, which scan controller classes for a
  different purpose and are unaffected.

## Decisions

### D1. Convert `klabisLinkTo` / `klabisAfford`, and plain `linkTo` only where it builds a link

The 16 plain `linkTo(methodOn(...))` calls split into two kinds:

- **13 build a `Location` header** (`.created(...)`, `.location(...)`). These deliberately bypass
  `klabisLinkTo`, because a Location header must be emitted regardless of whether the caller may
  `GET` the target. They gain nothing from interface routing — no affordance, no payload metadata —
  but converting them keeps one rule instead of two, and the resolved URL is identical either way.
  **Convert them**, for consistency rather than correctness.
- **2 build a `Link`** (`EventController:375`, `MembershipFeeTierController:165`) while skipping
  `klabisLinkTo`'s authorization check. Converting the target type does not change that; whether the
  bypass is correct is a separate question. **Convert, and flag each with a follow-up note** rather
  than silently changing behavior here.

### D2. Remove `@RequestBody` from overrides, keep `@Parameter`

Once affordances resolve against the interface, `@RequestBody` on an override is dead weight — Spring
MVC reads it from the interface for dispatch, and `HalFormsSupport` now reads it from the interface
too. Removing it eliminates the drift that caused this whole problem.

`@Parameter` (springdoc) stays: springdoc scans the concrete class, so stripping it would empty out
`klabis-full.json`'s parameter descriptions. This asymmetry is worth stating explicitly in the skill,
since "remove the annotations the interface already has" is the intuitive but wrong generalization.

### D3. Guard with a reflective test over all controllers, not per-endpoint assertions

The failure mode is silent, so the guard must be structural. A single test enumerates the
`@RestController` beans, and for each generated-interface method with a `@RequestBody` parameter
asserts that `methodOn(Api.class)` yields an affordance whose input metadata is non-empty. This
catches a future override that reintroduces the divergence, which 200 individual link assertions
would not.

Rejected: asserting `@RequestBody` is absent from overrides. That tests the cleanup, not the
property we care about, and would fail for legitimate hand-written endpoints.

### D4. Module-by-module, one commit each

20 controllers is too large for one reviewable change, and each module is independently verifiable
(its own `*ControllerTest` plus link assertions). Order: start with `common` (4 call sites, includes
`PermissionController`, one of the two known-defective overrides), then finance
(`MemberAccountController`, the other), then by ascending size.

Rejected: a single mechanical `sed`. The conversion is not purely textual — the target interface
name differs per controller, imports change, and the two plain-`linkTo` special cases need judgement.

## Risks / Trade-offs

- **[`METHOD_AUTH_CACHE` keyed on `Method` alone]** → Its javadoc warns this is sound only while
  every caller derives `targetClass` from `method.getDeclaringClass()`. After the change that class
  is the interface rather than the controller, which is still self-derived, so the invariant holds —
  but the annotations must actually resolve from the interface. Mitigation: the D3 guard test
  asserts authorization outcomes, not just metadata presence, and the first module converted
  (`common`) contains `@HasAuthority`, `@OwnerId` and plain endpoints together.
- **[CGLIB vs JDK proxying]** → `@EnableMethodSecurity(proxyTargetClass = true)` was required
  precisely because controllers implement generated interfaces. `methodOn` proxying is a separate
  mechanism (`ProxyFactory` inside `DummyInvocationUtils`) and unaffected, but the interaction is
  non-obvious. Mitigation: the first converted module is verified against a running application, not
  only `@WebMvcTest` slices.
- **[Wire-format regression is invisible to unit tests]** → Link assertions mostly compare hrefs,
  which do not change. Mitigation: capture the full HAL body of a representative endpoint per module
  before and after and diff it; treat any difference as a defect, not as an expected update.
- **[Trade-off: two annotation rules]** → `@RequestBody` moves to the interface while `@Parameter`
  stays on the controller. Slightly harder to remember than "all annotations live in one place", but
  the alternative is losing the published API documentation. Documented in the skill and the ADR.

## Migration Plan

1. Convert `common`, verify, commit. This module alone proves the mechanism end to end.
2. Convert the remaining 19 modules, one commit each, running that module's tests per commit.
3. Add the D3 guard test.
4. Update `backend-patterns` (rule + 8 examples + `aggregate-checklist.md`) and add the ADR.
5. Full suite plus a manual check of one HAL response against `main`.

Rollback is per-commit: no schema, spec, or wire change is involved, so reverting a module's commit
restores the previous behavior exactly.

## Open Questions

- The two authorization-bypassing plain `linkTo` calls (D1) — is the bypass intentional? Out of scope
  here; this change preserves the behavior and records the question.
