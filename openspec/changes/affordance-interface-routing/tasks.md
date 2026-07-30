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

- [ ] 3.1 finance (12) — includes `MemberAccountController.deposit`, the second defective override
- [ ] 3.2 members (12)
- [ ] 3.3 calendar (14)
- [ ] 3.4 membershipfees (38)
- [ ] 3.5 events + oris (52) — note `getAccommodationListAsCsv` has no interface counterpart but is
      not an affordance target, so it stays as-is
- [ ] 3.6 groups (62)
- [ ] 3.7 For each module: run that module's tests before committing, and diff its 1.1 baseline body

## 4. Plain `linkTo` call sites

- [ ] 4.1 Convert the 13 `linkTo(methodOn(...))` calls that build a `Location` header — no behavior
      change, done for consistency
- [ ] 4.2 Convert the 2 that build a `Link` while bypassing `klabisLinkTo`'s authorization check
      (`EventController:375`, `MembershipFeeTierController:165`), preserving the bypass, and record
      the open question from design.md as a follow-up issue rather than changing it here

## 5. Guard against regression

- [ ] 5.1 Add a reflective test enumerating `@RestController` beans: for every generated-interface
      method with a `@RequestBody` parameter, assert `methodOn(Api.class)` yields an affordance whose
      input payload metadata is non-empty
- [ ] 5.2 Assert authorization outcomes in the same test, not just metadata presence, so
      `METHOD_AUTH_CACHE` resolving annotations off the interface stays covered
- [ ] 5.3 Verify the guard actually fails: temporarily point one affordance back at its controller
      and confirm a red test, then revert

## 6. Documentation

- [ ] 6.1 Update `.claude/skills/backend-patterns/SKILL.md`: state the rule in "HATEOAS Rules
      (NON-NEGOTIABLE)" and correct the 8 `methodOn(XController.class)` occurrences in its examples
- [ ] 6.2 State the `@RequestBody` (interface) vs `@Parameter` (controller) asymmetry explicitly —
      "remove the annotations the interface already has" is the intuitive but wrong generalization
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
