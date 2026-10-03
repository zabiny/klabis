## 1. Slice: Shared evaluator replaces duplicated checks (behaviour-preserving)

- [ ] 1.1 Write tests for `AuthorityEvaluator` (global authority granted/denied, no target, unauthenticated) and for parity between method call and HAL affordance for existing `@HasAuthority` / `@OwnerVisible` endpoints
- [ ] 1.2 Introduce targeted authorities (`*` wildcard), the authorities snapshot on `KlabisJwtAuthenticationToken`, `AuthorityEvaluator` and the `RelationshipAuthorityProvider` SPI in `common.security` (no providers yet; token authorities map to `*`)
- [ ] 1.3 Switch `HasAuthorityMethodInterceptor` and `HalFormsSupport.isMethodAuthorized` to the evaluator; keep `@OwnerVisible` as an additional OR alternative
- [ ] 1.4 Run existing security-related slice tests; fix regressions

## 2. Slice: `@HasAuthority(target)` and repeatable OR

- [ ] 2.1 Write tests: repeated `@HasAuthority` OR semantics on a test controller, SpEL `target` resolved from path variable, target resolved through a generated `*Api` interface method, HAL affordance agrees with the call
- [ ] 2.2 Add `target` and `@HasAuthority.List` to `@HasAuthority`, meta-annotate with `@PreAuthorize` template, implement the `@HasAuthority` `AuthorizationManager` (replaces the custom advisor), update `MethodSecurityAnnotations` lookups for the container
- [ ] 2.3 Make `HalFormsSupport` evaluate `target` SpEL from the dummy invocation arguments and standard `@PreAuthorize` on controller methods
- [ ] 2.4 Verify parameter names are kept (`-parameters`) and add a startup check that every `target` expression resolves

## 3. Slice: Free group delegates `MEMBER:EDIT_DETAILS` end-to-end (domain, persistence, provider, PoC endpoint)

- [ ] 3.1 Write domain tests for `MemberGroup.delegatedAuthorities` (immutable, only `MEMBER`, empty default) at 100% coverage
- [ ] 3.2 Add `Authority.Scope.MEMBER` and `Authority.MEMBER_EDIT_DETAILS` (`MEMBER`); switch `AuthorizationPolicy.checkGlobalAuthorityNotGrantedViaGroup` to allow only `MEMBER`; keep it out of the permissions dialog and direct-assignment validation (users spec)
- [ ] 3.3 Add `delegatedAuthorities` to `MemberGroup`; add `groups.user_group_authorities` table to V001; map it in `GroupMemento` / `GroupJdbcRepository`
- [ ] 3.4 Write integration tests for the provider query (owner of group with authority over a member; non-owner; non-member target; member leaves group) then implement `RelationshipAuthorityProvider` over the unified group tables
- [ ] 3.5 Build the authorities snapshot (targeted authorities, token authorities with `*`, provider results unioned, lazily memoized per request) on the authentication; `CurrentUserData.hasAuthority(a, target)`; test the union, `*` semantics, no providers registered, and that a list of N members issues one provider query
- [ ] 3.6 Update `docs/openapi/spec/groups.yaml` and `members.yaml`: `x-klabis-authority` list entries with target; extend generator templates to emit repeated `@HasAuthority`; regenerate the bundle and frontend types; verify zero-diff on untouched operations
- [ ] 3.7 Members module: self-relationship provider (every user holds `MEMBER_EDIT_DETAILS` over themselves); replace `x-klabis-owner-visible` in `members.yaml` with `x-klabis-authority` lists (fields use the record's `@OwnerId` as target; admin-only fields stay `MEMBERS_MANAGE`); secure `updateMember` with `MEMBERS_MANAGE` OR `MEMBER_EDIT_DETAILS` over `#id`; narrow `OwnProfileEditRule` to the acting user's own minor profile; write WebMvc tests (403/200 matrix, affordance on detail and list rows, delegated holder sees editable fields but not admin-only ones, own data still visible, minor cannot self-edit)

## 4. Slice: Free group creation and invitation show delegated permissions

- [ ] 4.1 Write tests: create free group with/without delegated permissions, invalid permission rejected with 422, group detail and pending invitation expose the set, group edit does not accept it
- [ ] 4.2 Add `delegatedAuthorities` to the create request / HAL-FORMS template (options: delegable authorities), group detail and `PendingInvitationResponse`
- [ ] 4.3 Frontend: create-group dialog permission selector; invitation view lists what owners will be able to do; localized labels in `labels.ts`; frontend tests

## 5. Slice: Legal guardians edit their minors

- [ ] 5.1 Write tests: guardian edits a minor (200, affordance present), guardian vs unrelated member (403, no affordance), minor turns 18 and leaves group, admin-only fields not editable
- [ ] 5.2 `LegalGuardianGroup` created with predefined set `{MEMBER_EDIT_DETAILS}`; training group predefined (empty) set; example-data bootstrap and existing groups get their sets
- [ ] 5.3 Expose `delegatedAuthorities` read-only on training and legal guardian group details; update spec files and regenerate bundle and types

## 6. Documentation and verification

- [ ] 6.1 Add an ADR to `docs/design-decisions.md` (ReBAC via group-delegated authorities; shared evaluator; immutable sets)
- [ ] 6.2 Update `backend-patterns` skill (`@HasAuthority` target / repeatable, evaluator, provider SPI) and `backend/CLAUDE.md` extension points; update developer manual via `developer-manual-maintainer` if the security chapter changes
- [ ] 6.3 Run the full backend and frontend suites once, `npm run build`, and QA the guardian-edits-minor flow in the UI on `http://localhost:3000`
- [ ] 6.4 Code review of the changes before committing
