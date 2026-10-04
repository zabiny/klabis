# Tasks

## 1. Authority describes target type and grant forms

- [x] 1.1 Write failing unit tests for `Authority`: every constant has `targetType` and `grantForms` per design D1; `assignableAuthorities()` equals authorities with `ALL` minus standard and `DEVELOPER`; `delegatable()` is empty today; verify they fail
- [x] 1.2 Implement D1, remove `Authority.Scope` and `AuthorizationPolicy.checkGlobalAuthorityNotGrantedViaGroup` (and its test); verify 1.1 passes and `PermissionController` tests still list the same dialog options

## 2. Permission snapshot, relationship sources and evaluator core

- [x] 2.1 Write failing unit tests for `AuthorizationSnapshot` (`has` over everything vs. over a target, target created later covered by over-everything, union of two sources, source grant for an `{ALL}`-only authority ignored); verify they fail
- [x] 2.2 Create `com.klabis.common.authorization` (named interface), `TargetType`, `TargetRef`, `RelationshipSource`, `AuthorizationSnapshot`; verify 2.1 passes and `ModuleStructureVerificationTest` passes
- [x] 2.3 Write failing tests for the request-scoped lazy `AuthorizationSnapshotProvider`: loads once per request, not at all when nothing asks, `client_credentials` uses token authorities and no targets; verify they fail
- [x] 2.4 Implement the provider over `PermissionService` + all `RelationshipSource` beans; verify 2.3 passes
- [x] 2.5 Write failing unit tests for `AuthorizationEvaluator` rule (D5: any-of authorities, with/without target, owner-visible self path); implement it; verify tests pass
- [x] 2.6 Extend `WithKlabisMockUser` with `targetGrants` installing a fixed snapshot (D8); verify an existing `@WebMvcTest` and a new one using `targetGrants` pass

## 3. Permissions read per request instead of from the token

- [ ] 3.1 Write a failing integration test: user logs in, admin revokes `GROUPS:TRAINING`, the same access token no longer reaches the training groups API; and the reverse for granting; verify it fails
- [ ] 3.2 Make `KlabisJwtAuthenticationToken.getAuthorities()` delegate to the snapshot for user tokens, stop reading the `authorities` claim for them, stop emitting it in the authorization server token customizer (keep `client_credentials` expansion); verify 3.1 passes and the `client_credentials` tests pass
- [ ] 3.3 Verify `@PreAuthorize` SpEL usages still evaluate correctly with a test covering one existing `@PreAuthorize` endpoint

## 4. Method security and offered actions through the evaluator

- [ ] 4.1 Change `@HasAuthority` to accept a list (any of) and introduce `@TargetId(TargetType)` replacing `@OwnerId`; write failing interceptor tests for list semantics and targeted grants; verify they fail
- [ ] 4.2 Rewrite `HasAuthorityMethodInterceptor` to delegate to `AuthorizationEvaluator.canInvoke`; verify 4.1 passes and all existing security tests pass
- [ ] 4.3 Rewrite `HalFormsSupport` method authorization to the same `canInvoke`; add a test that an affordance is present exactly when the invocation is allowed for: over-everything grant, targeted grant on the target, targeted grant on another target, self; verify it passes

## 5. Field visibility and request-body field checks through the evaluator

- [ ] 5.1 Write failing tests: response field with `[A, B]` visible with a targeted `B` grant on the record's target and hidden on another record; request field rejected without write authority; verify they fail
- [ ] 5.2 Route `FieldSecurityBeanSerializerModifier` / `SecuredBeanPropertyWriter` and `RequestBodyFieldAuthorizationAdvice` through the evaluator; verify 5.1 and existing field-security tests pass
- [ ] 5.3 Add `@ReadAuthority` and make HAL-FORMS properties readable-but-not-writable render as `readOnly: true` (unreadable stay hidden); verify with a `HalFormsSupport` test

## 6. OpenAPI extensions and generator

- [ ] 6.1 Extend `tools/openapi-bundle/lib/validate.mjs` (and its tests) for list `x-klabis-authority`, `x-klabis-target-id: <TYPE>` with target-type matching, `x-klabis-read-authority` on request schemas only, reading `targetType`/`grantForms` from `Authority.java`; verify `npm test` in `tools/openapi-bundle` passes
- [ ] 6.2 Update `api.mustache`, `pojo.mustache`, `pathParams.mustache` to emit `@HasAuthority({…})`, `@TargetId(…)`, `@ReadAuthority(…)`; verify generated sources compile
- [ ] 6.3 Replace every `x-klabis-owner-id: true` in `docs/openapi/spec/*.yaml` with `x-klabis-target-id: MEMBER`; verify the bundle validates and the backend builds with all tests green
- [ ] 6.4 Update `docs/openapi/spec/README.md` extension tables; verify the documented examples match the validator

## 7. Imperative checks migrated (behavior preserved)

- [ ] 7.1 `members`: `MemberController`, `OwnProfileEditRule`, `ManagementService.getMemberAndRecordView` caller, `GuardianListAccess`, `CurrentUserData.hasAuthority` ask the evaluator; verify members tests pass unchanged
- [ ] 7.2 `events`: `EventController`, `EventManagementService` flags; verify events tests pass unchanged
- [ ] 7.3 `groups` (`TrainingGroupController`) and `membershipfees` (`MembershipFeeTierController`); verify their tests pass unchanged
- [ ] 7.4 Add the ArchUnit rule from D5; verify it passes and fails on a deliberately introduced direct `getAuthorities()` check (then remove it)

## 8. Documentation

- [ ] 8.1 Add ADR-010 "Relationship-based authorization" to `docs/design-decisions.md` (D1, D2, D4, D5 with alternatives); verify it follows the existing ADR structure
- [ ] 8.2 Update `backend-patterns` and `klabis-api-spec` skills for `@HasAuthority` lists, `@TargetId`, `@ReadAuthority`, `RelationshipSource`, `AuthorizationEvaluator`; verify no stale `@OwnerId` references remain (`grep`)
- [ ] 8.3 Update the developer manual via the `developer-manual-maintainer` skill; verify the security page describes the snapshot and evaluator

## 9. Integration checks

- [ ] 9.1 Run full backend, tooling and frontend test suites; verify all green
- [ ] 9.2 On http://localhost:3000: as ZBM9500, revoke/grant a permission as ZBM9000 in another session and verify the change applies after a page reload without re-login; spot-check that admin and member see the same actions as before the change
- [ ] 9.3 Measure member list and member detail response time with the snapshot (example-data); verify it stays under 500 ms
