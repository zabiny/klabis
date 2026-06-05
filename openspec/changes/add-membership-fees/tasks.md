# Implementation Tasks — add-membership-fees

> Each numbered group is an end-to-end vertical slice that is independently committable and testable.
> Follow Red-Green-Refactor TDD. Domain logic: 100% coverage; overall >80%.
> Naming follows the glossary in `design.md`.

## 1. Module scaffolding

- [x] 1.1 Create module `com.klabis.membership-fees` with `package-info.java` declaring `@ApplicationModule(displayName = "Členské příspěvky")`
- [x] 1.2 Add architecture test expectation so the new module passes `JMoleculesArchitectureTest` (allowed value-object refs: `MemberId`, `EventTypeId`; allowed ports: `AllMembersPort`, `ChargePort`)
- [x] 1.3 Add DDL for all `membership-fees` tables into V001 (catalog, rules, `fee_year_publication` with `deadline_processed_at`, `membership_fee_group` with snapshot + members, yearly fee idempotency marker); no changes to `user_groups`

## 2. Fee level catalog (slice: define a level template end-to-end)

- [ ] 2.1 Write failing domain tests for `MembershipFeeLevel` aggregate: create with name + yearly fee + rules, edit name/fee/rules, identity `MembershipFeeLevelId`
- [ ] 2.2 Write failing domain tests for `MembershipPaymentRule` value object: percentage vs fixed surcharge, unique `(EventTypeId, rankingShortName)` per level, duplicate rejected
- [ ] 2.3 Implement `MembershipFeeLevel` + `MembershipPaymentRule` to make domain tests pass; refactor
- [ ] 2.4 Implement own persistence: `MembershipFeeLevelMemento` + repository + adapter (DDL already in V001); write repository round-trip test
- [ ] 2.5 Implement application port `MembershipFeeLevelManagementPort` + service (create / edit / get / list) with `MEMBERS:ADMIN` authorization
- [ ] 2.6 Implement HAL+FORMS `MembershipFeeLevelController` (catalog CRUD) with `_links` and afford templates; write web slice test
- [ ] 2.7 Verify spec scenarios: "Membership Fee Level Catalog", "Membership Payment Rules by Event Type and Ranking"

## 3. Publishing levels for a year (slice: publish a year end-to-end)

- [ ] 3.1 Write failing domain tests for `FeeYearPublication`: holds year + single voting deadline + set of `MembershipFeeGroupId` refs; cannot publish a year twice; `deadlineProcessedAt` null until scheduler runs; `markProcessed(at)` sets it
- [ ] 3.2 Write failing domain tests for `MembershipFeeGroup`: snapshot of yearly fee + rules copied from catalog, year of validity, state `EDITABLE`, own membership logic (`addMember` / `removeMember` / `hasMember` over `Set<MemberId>`)
- [ ] 3.3 Implement publishing: copying catalog level into a frozen-on-demand snapshot; implement aggregates; refactor
- [ ] 3.4 Implement own persistence for `FeeYearPublication` and `MembershipFeeGroup` as separate aggregate roots (DDL already in V001); repository round-trip test for each
- [ ] 3.5 Implement application port + service for publishing a year (`MEMBERS:ADMIN`)
- [ ] 3.6 Implement HAL+FORMS endpoints to publish a year and to list published levels per year; web slice test
- [ ] 3.7 Verify spec scenarios: "Publishing Fee Levels for a Calendar Year"

## 4. Member chooses a level (slice: member self-selection end-to-end)

- [ ] 4.1 Write failing domain tests: member joins a `MembershipFeeGroup` before deadline; changing choice moves membership; choice locked after deadline
- [ ] 4.2 Write failing test for "previous year's level offered as default": application service resolves pre-fill from year N-1 matched by `MembershipFeeLevelId` (sourceLevelId); default is non-binding (not a choice until confirmed)
- [ ] 4.3 Implement choice + change + lock-after-deadline domain logic; refactor
- [ ] 4.4 Implement application port + service for member choice and change (member acting on self)
- [ ] 4.5 Implement HAL+FORMS member-facing endpoints for choosing/changing the level for the upcoming year, with conditional afford metadata (hidden after deadline); web slice test
- [ ] 4.6 Verify spec scenarios: "Member Chooses a Fee Level for the Upcoming Year"

## 5. Emergency assignment by administrator (slice)

- [ ] 5.1 Write failing tests: `MEMBERS:ADMIN` assigns/changes a member's level after the deadline, overriding the lock
- [ ] 5.2 Implement emergency assignment domain + service path; refactor
- [ ] 5.3 Implement HAL+FORMS admin endpoint for emergency assignment; web slice test
- [ ] 5.4 Verify spec scenarios: "Emergency Assignment by Administrator"

## 6. Sanction for a missing choice (slice: cross-module via events)

- [ ] 6.1 Write failing tests for sanction trigger: day after deadline, members without a choice are identified via `AllMembersPort` (including suspended) minus members present in any `MembershipFeeGroup` for that year
- [ ] 6.2 Implement scheduler publishing `MemberMissedFeeSelectionEvent` for members who missed the choice; call `FeeYearPublication.markProcessed(now)` and persist; idempotent (sanctioning already-sanctioned member is a no-op in `events`)
- [ ] 6.3 In module `events`: write failing listener test — on `MemberMissedFeeSelectionEvent` block new registrations + deregister from events with open registrations; log affected registrations for manual restore
- [ ] 6.4 Implement the `events` listener; refactor
- [ ] 6.5 Verify spec scenarios: "Sanction for a Missing Choice" (member sanctioned vs not sanctioned)

## 7. Lifting the sanction (slice)

- [ ] 7.1 Write failing tests: emergency assignment of a sanctioned member publishes `MemberFeeSelectionResolvedEvent`
- [ ] 7.2 Implement publishing of `MemberFeeSelectionResolvedEvent` from emergency assignment path
- [ ] 7.3 In module `events`: write failing listener test — on `MemberFeeSelectionResolvedEvent` lift the registration block; previously deregistered registrations are NOT auto-restored
- [ ] 7.4 Implement the `events` listener; refactor
- [ ] 7.5 Verify spec scenarios: "Lifting the Sanction by Emergency Assignment"

## 8. Editing window of a published level (slice)

- [ ] 8.1 Write failing tests: published level editable while `EDITABLE`; edit rejected once `FROZEN`
- [ ] 8.2 Write failing test: snapshot transitions `EDITABLE` → `FROZEN` automatically when `votingDeadline` passes
- [ ] 8.3 Implement freeze-on-deadline logic (snapshot transitions `EDITABLE` → `FROZEN` when `votingDeadline` passes); refactor
- [ ] 8.4 Implement HAL+FORMS endpoint to edit a published level (afford present only while `EDITABLE`); web slice test
- [ ] 8.5 Verify spec scenarios: "Editing a Published Level Until First Surcharge"

## 9. Yearly membership fee posting (slice: charge to account end-to-end)

- [ ] 9.1 Write failing tests for the yearly-fee scheduler: runs day after the voting deadline; posts each member's assigned level yearly fee; idempotent per member+year; member without a level gets nothing
- [ ] 9.2 Implement scheduler calling `finance.ChargePort.charge(...)` with idempotency marker (member, year); refactor
- [ ] 9.3 In module `finance` / `member-accounts`: write failing test — generated yearly fee appears in account history and is identifiable as system-generated
- [ ] 9.4 Implement the member-accounts side so the auto-generated yearly fee is recorded as system-generated; refactor
- [ ] 9.5 Verify spec scenarios: "Generating the Yearly Membership Fee" + member-accounts "Automatic Yearly Membership Fee Posting"

## 10. Member visibility and audit trail (slice)

- [ ] 10.1 Write failing tests: member sees current assigned level; prompt to choose when none and voting open; past-year assignment retrievable (audit trail)
- [ ] 10.2 Implement query for current/past level assignment; refactor
- [ ] 10.3 Implement HAL+FORMS endpoint feeding the profile widget (current level) with `_links`; web slice test
- [ ] 10.4 Verify spec scenarios: "Member Sees Their Current Fee Level", "Audit Trail of Level Assignments"

## 11. Frontend — administration

- [ ] 11.1 Add Administration section: define/edit fee level catalog (name, yearly fee, payment rules by event type + ranking, percentage vs fixed)
- [ ] 11.2 Add UI to publish levels for a year with a single voting deadline and to track member choices
- [ ] 11.3 Wire HAL+FORMS afford-driven forms (catalog CRUD, publishing, emergency assignment)

## 12. Frontend — member

- [ ] 12.1 Add member page to choose the level for the upcoming year (pre-filled with previous year's level as non-binding default, hidden after deadline)
- [ ] 12.2 Add profile widget showing the member's current fee level (with prompt to choose when none and voting open)

## 13. Integration and wrap-up

- [ ] 13.1 Add `@ApplicationModuleTest` covering the cross-module event flows (sanction, lifting) between `membership-fees`, `events`, and yearly fee posting to `finance`
- [ ] 13.2 Run module-boundary verification (`JMoleculesArchitectureTest`) and full test suite; confirm coverage targets
- [ ] 13.3 Code review (proper agent) before commit
- [ ] 13.4 Add label `BackendCompleted` to GitHub issue #274
