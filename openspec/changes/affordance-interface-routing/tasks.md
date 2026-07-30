## 1. Baseline

- [x] 1.1 Capture the full HAL body of one representative endpoint per module (`getMember`,
      `getEvent`, `getGroup`, `getTier`, `listCalendarItems`, `getUserPermissions`, an account
      endpoint) against the current build; store under `/mnt/ramdisk/klabis` as the diff baseline
      — **revised in practice:** a throwaway `@WebMvcTest` writing the body to a file proved a better
      capture mechanism than a running server (no OAuth flow, exact diff). It found the
      `readOnly: true` defect immediately. Rather than keep throwaway capture files per module, each
      module's commit adds a permanent assertion on the template it fixes; the capture test is
      deleted once its finding is encoded that way.
- [x] 1.2 Record the current full-suite result (test count, failures) so any later delta is
      attributable to this change
- [x] 1.3 Confirm every controller still resolves: re-run the audit that all 20 controllers implement
      a generated `*Api` and all 97 `methodOn(...)`-referenced methods have an interface counterpart

## 2. Pilot: common module (4 call sites)

- [x] 2.1 Convert `methodOn(XController.class)` to `methodOn(XApi.class)` in
      `com.klabis.common.**.restapi`, including `PermissionController` — one of the two overrides
      missing `@RequestBody`
- [x] 2.2 Remove `@RequestBody` from the converted overrides; leave `@Parameter` in place (springdoc
      reads it off the concrete class)
- [x] 2.3 Run the common-module tests; then diff the `getUserPermissions` HAL body against the 1.1
      baseline — expect an identical body except for `updatePermissions` gaining its input metadata
- [x] 2.4 Verify against a running application (`./runLocalEnvironment.sh`), not only `@WebMvcTest`
      slices, that `@EnableMethodSecurity(proxyTargetClass = true)` and interface proxying coexist
- [x] 2.5 Commit the pilot separately, so the mechanism is reviewable on its own

## 3. Remaining modules, one commit each

- [x] 3.1 finance (12) — includes `MemberAccountController.deposit`, the second defective override.
      The defect turned out to be wider than described: `deposit`, `charge` **and** `reverse` all
      rendered every property `readOnly: true`, including the required `amount`. Two of the 12 sites
      are cross-module (`methodOn(MemberController.class)` → `MembersApi`, permitted by the
      `@NamedInterface("members.rest")` on the members restapi package).
- [x] 3.2 members (13, incl. 2 cross-module refs to `PermissionsApi`). No latent defect here — these
      overrides did carry `@RequestBody`, so the HAL bodies are byte-identical before and after.
      Surfaced the HV000151 constraint-redefinition rule now recorded in design.md D2: removing only
      `@RequestBody` while leaving `@NotNull` on a sibling parameter fails at request time.
- [x] 3.3 calendar (14, incl. one cross-module ref to `EventsApi`). Third occurrence of the same
      defect: `createCalendarItem` and `updateCalendarItem` rendered all four properties `readOnly`.
      Only diff in the captured bodies is 12 vanished `readOnly: true` entries.
- [x] 3.4 membershipfees (38 across 5 controllers). No latent defect — these overrides carried
      `@RequestBody` (without `@Valid`), so no HV000151 risk either. The module's own tests already
      assert `options.inline` on the `addRule` / `editRule` templates, which is exactly the metadata
      that would vanish if the routing were wrong, so they serve as the guard here.
- [x] 3.5 events + oris (52 across 6 controllers). `getAccommodationListAsCsv` is not a `methodOn`
      target, so its missing interface counterpart never mattered. `cancelEvent`'s
      `@RequestBody(required = false)` matched the interface exactly and was safe to drop. No latent
      defect — the overrides carried `@RequestBody`.
- [x] 3.6 groups (62 across 4 controllers). Note `FreeGroupController` implements `GroupsApi`, not
      the `FreeGroupsApi` the naming convention would suggest. `cancelInvitation`'s
      `@RequestBody(required = false)` matched the interface and was safe to drop.
- [x] 3.7 For each module: run that module's tests before committing, and diff its 1.1 baseline body.
      Defects found and fixed: `common` (updatePermissions), `finance` (deposit/charge/reverse),
      `calendar` (create/updateCalendarItem). No defect in members, membershipfees, events or groups —
      their overrides already declared `@RequestBody`.

## 4. Plain `linkTo` call sites

- [x] 4.1 Convert the 13 `linkTo(methodOn(...))` calls that build a `Location` header — no behavior
      change, done for consistency. Completed as part of sections 2–3: the per-module conversion
      covered plain `linkTo` alongside `klabisLinkTo`. Zero `methodOn(XController.class)` call sites
      remain anywhere in `backend/src/main/java`.
- [x] 4.2 Convert the ones that build a `Link` while bypassing `klabisLinkTo`'s authorization check,
      preserving the bypass. **Correction to the earlier count:** only one of the two is a `Link` —
      `EventController:375` builds `withRel("event")` on the accommodation list. The other,
      `MembershipFeeTierController:164`, turned out to be a `Location` header (`.created(...).toUri()`)
      and therefore belongs to 4.1. The open question in design.md stands for the single remaining
      site.

## 5. Guard against regression

- [x] 5.1 **Revised approach.** A reflective enumeration would have to instantiate each affordance
      outside a request context to inspect its metadata, which `HalFormsSupport` does not support.
      Two narrower guards cover the same invariant more directly:
      `AffordanceRoutingArchitectureTest` asserts no production call site passes a `*Controller.class`
      to `methodOn(...)` (source-level, because ArchUnit sees the call but not the class literal
      argument), and a new `InterfaceRoutedInputMetadata` case in `AffordanceAuthorizationTest`
      exercises the real mechanism end to end: an override that omits `@RequestBody`, an affordance
      recorded against the interface, and an assertion that the field is not `readOnly`.
- [x] 5.2 Already covered: `AffordanceAuthorizationTest` has an
      `InterfaceLevelKlabisLinkToAuthorization` nested class asserting both the authorized and
      unauthorized outcome for `@HasAuthority` declared only on the interface. The new fixture
      extends the same `AffordanceApi`, so `METHOD_AUTH_CACHE` resolution off the interface stays
      exercised.
- [x] 5.3 Both guards verified red then green: pointing the test affordance back at
      `AffordanceTestController` failed the metadata assertion (17 tests, 1 failed), and reverting
      `MemberPermissionsLinkProcessor:88` to `PermissionController` failed the architecture test.

## 6. Documentation

- [ ] 6.1 Update `.claude/skills/backend-patterns/SKILL.md`: state the rule in "HATEOAS Rules
      (NON-NEGOTIABLE)" and correct the 8 `methodOn(XController.class)` occurrences in its examples
- [ ] 6.2 State the `@RequestBody` (interface) vs `@Parameter` (controller) asymmetry explicitly —
      "remove the annotations the interface already has" is the intuitive but wrong generalization —
      and the HV000151 rule from design.md D2 (an override declares either all of the interface's
      parameter constraints or none of them; `@Valid` is exempt, `@NotNull` is not)
- [ ] 6.3 Update `references/aggregate-checklist.md`, which tells implementers to add affordances
      without saying against which type
- [ ] 6.4 Add an ADR to `docs/design-decisions.md` recording why affordances resolve against the
      interface

## 7. Verification

- [ ] 7.1 Run the full backend suite; confirm the count matches 1.2 with no new failures
- [ ] 7.2 Confirm `docs/openapi/klabis-full.json` and `frontend/src/api/klabisApi.d.ts` are unchanged
      — this change must not touch either baseline
- [ ] 7.3 Diff every 1.1 baseline body; treat any difference other than the two overrides gaining
      input metadata as a defect, not as an expected update
- [ ] 7.4 Code review before the final commit, per CLAUDE.md
