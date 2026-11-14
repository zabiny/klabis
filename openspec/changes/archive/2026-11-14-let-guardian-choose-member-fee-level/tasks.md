# Tasks

## 1. Read access to current level for guardian and administrator (vertical slice)

- [x] 1.1 Write failing integration test: guardian (member and non-member) and administrator get 200 on `getFeeSummary`/`getChoice` of a minor, unrelated member gets 403 (extend `MemberFeeHistoryAccessIntegrationTest`)
- [x] 1.2 In `docs/openapi/spec/membershipfees.yaml` set `x-klabis-authority: [MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` and `x-klabis-target-id: MEMBER` on `getFeeSummary` and `getChoice`, remove their `x-spring-provide-args`, update the header comment; regenerate backend bundle and frontend types; test 1.1 passes
- [x] 1.3 Update controller signatures, postprocessor `methodOn(...)` calls and unit tests (`MemberFeeSummaryControllerTest`); `./gradlew test` for the module passes
- [x] 1.4 Frontend: show the current level section for administrator and guardian; `MemberFeeSection.test.tsx` covers administrator without choice links; `npm run build` passes

## 2. Guardian chooses, changes and withdraws (vertical slice)

- [x] 2.1 Write failing integration test: guardian (member and non-member) can `chooseTier` and `removeChoice` for the minor, administrator and unrelated guardian get 403, affordances absent for administrator
- [x] 2.2 In `membershipfees.yaml` set `x-klabis-authority: [MEMBERS_EDIT_PROFILE]` and `x-klabis-target-id: MEMBER` on `chooseTier` and `removeChoice`, remove `x-spring-provide-args`; regenerate; test 2.1 passes
- [x] 2.3 Frontend: render choose/change links only when the summary has the `chooseTier` affordance; `MemberFeeChoicePage` works for a guardian; update `MemberFeeSection.test.tsx` and `MemberFeeChoicePage.test.tsx`; `npm run build` passes
- [x] 2.4 E2E: guardian Eva chooses a level for Sofie (`membership-fee-history.feature` or `membership-fee-choice.feature`), administrator Jan sees no choice controls; `npm run test:e2e` for these features passes
- [x] 2.5 Update `references/field-security.md` and the module API notes that call these operations owner-only; grep finds no remaining "owner-only" claim for the four operations

## 3. Integration check

- [x] 3.1 Full backend suite (final verification, SKIP_OPTIMIZATIONS) and `openspec validate let-guardian-choose-member-fee-level --strict` pass
