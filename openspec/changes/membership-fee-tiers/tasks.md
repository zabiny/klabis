## 1. Rename Level → Tier (foundation)

> **MUST use IntelliJ MCP rename-refactoring tools** for all symbol renames (per design D1). Do NOT use textual find-and-replace. Do NOT touch ORIS `OrisApiClient.listLevels()` / `LevelListEntry` — those are unrelated. Build + run the full membership-fees test suite after each batch.

- [x] 1.1 Rename domain types: `MembershipFeeLevel` → `MembershipFeeTier`, `MembershipFeeLevelId` → `MembershipFeeTierId`, `MembershipFeeLevelRepository` → `MembershipFeeTierRepository`, and any `*Level*` exceptions/events in the domain package
- [x] 1.2 Rename application layer: `MembershipFeeLevelManagementPort`/`Service`, `MembershipFeeLevelNotFoundException`, `CreateLevelCommand`/`EditLevelCommand` → `*Tier*` equivalents
- [x] 1.3 Rename infrastructure: JDBC mementos/adapters (`MembershipFeeLevelMemento`, `MembershipFeeLevelJdbcRepository`, `MembershipFeeLevelRepositoryAdapter`, `PublishedLevelRefMemento`, etc.) → `*Tier*`
- [x] 1.4 Rename REST layer: controller, request/response DTOs, postprocessors; change request mapping `/api/membership-fee-levels` → `/api/membership-fee-tiers` and all link rels (`membership-fee-levels` → `membership-fee-tiers`)
- [x] 1.5 Update V001 migration DDL: rename tables/columns `*level*` → `*tier*` for the catalog aggregate (keep published-snapshot tables consistent)
- [x] 1.6 Run full backend test suite; fix any remaining references; confirm green
- [x] 1.7 Frontend rename: `MembershipFeeLevelsPage`/`MembershipFeeLevelDetailPage` → `*Tier*`, routes, and the `interface FeeLevelDetail` → `FeeTierDetail`; update `App.tsx` routing
- [x] 1.8 Update Czech labels in `src/localization/labels.ts` (level → úroveň/tier wording per UX); regenerate OpenAPI types (`npm run openapi`) after backend URL change
- [x] 1.9 Run frontend test suite; confirm green

## 2. Tier creation without rules (vertical slice)

- [x] 2.1 Write failing test: creating a tier with name + yearly fee produces an empty-rules tier; `CreateTierCommand` no longer carries rules
- [x] 2.2 Remove `rules` from `CreateTierCommand` and `CreateMembershipFeeTierRequest`; change `MembershipFeeTier.create(name, yearlyFee)` to start with no rules; remove `replaceRules`
- [x] 2.3 Refactor; confirm domain + service + controller tests green
- [x] 2.4 Verify frontend create form posts name + yearly fee only (no rules); adjust if needed

## 3. Add a rule (vertical slice)

- [x] 3.1 Write failing domain test: `addRule` adds a rule; adding a duplicate `(eventTypeId, ranking)` throws `DuplicatePaymentRuleException`
- [x] 3.2 Write failing REST test: `POST /api/membership-fee-tiers/{id}/rules` adds a percentage rule and a fixed-amount rule; duplicate combination is rejected
- [x] 3.3 Implement the add-rule command on the management port + service, and the `POST .../rules` controller endpoint (requires MEMBERS:MANAGE)
- [x] 3.4 Expose the `addRule` affordance on the tier-detail representation; refactor; confirm green

## 4. Ranking dropdown from ORIS (vertical slice)

- [x] 4.1 Write failing test: the add-rule affordance carries ranking inline options sourced from the ORIS ranking list; empty options when ORIS is unavailable (no error)
- [x] 4.2 Add a ranking-options port in membership-fees backed by `OrisApiClient.listLevels()` (mirror `listDisciplineOptions` incl. graceful degradation + warning log)
- [x] 4.3 Attach ranking inline options (and event-type options) to the `addRule` affordance; refactor; confirm green
- [x] 4.4 Frontend: replace the free-text ranking input in the rule form with a dropdown bound to the affordance's ranking options

## 5. Edit a rule's value (vertical slice)

- [ ] 5.1 Write failing domain test: `editRuleValue(eventTypeId, ranking, value)` updates the value and keeps the key; editing a non-existent rule is rejected
- [ ] 5.2 Write failing REST test: `PATCH /api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}` updates percentage/fixed amount; key stays unchanged
- [ ] 5.3 Implement `editRuleValue` on the aggregate, the edit command/service, and the `PATCH .../rules/{eventTypeId}/{ranking}` endpoint (requires MEMBERS:MANAGE)
- [ ] 5.4 Expose the `editRule` affordance per rule row; refactor; confirm green
- [ ] 5.5 Frontend: wire the per-row edit form to the `PATCH` endpoint (value fields only; key read-only)

## 6. Remove a rule (vertical slice)

- [ ] 6.1 Write failing domain test: `removeRule(eventTypeId, ranking)` removes the matching rule
- [ ] 6.2 Write failing REST test: `DELETE /api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}` removes the rule
- [ ] 6.3 Implement `removeRule` on the aggregate, the service, and the `DELETE .../rules/{eventTypeId}/{ranking}` endpoint (requires MEMBERS:MANAGE)
- [ ] 6.4 Expose the `deleteRule` affordance per rule row; refactor; confirm green
- [ ] 6.5 Frontend: wire the per-row delete button to the `DELETE` endpoint

## 7. Typed event-type id in rule payload

- [ ] 7.1 Write failing test: the rule response serializes `eventTypeId` via its typed id value object (not a bare UUID string)
- [ ] 7.2 Update `PaymentRuleResponse` to carry the typed id; adjust serializer/mixin as needed; refactor; confirm green
- [ ] 7.3 Frontend: confirm event-type name still resolves via `useEventTypes()`; adjust the rule table mapping if the field shape changed

## 8. End-to-end verification

- [ ] 8.1 Run full backend + frontend test suites; confirm green and coverage targets met (100% domain, >80% overall)
- [ ] 8.2 `npm run refresh-backend-server-resources` to publish frontend changes; restart backend
- [ ] 8.3 Manual/QA smoke: create a tier, add a percentage rule and a fixed-amount rule (ranking from dropdown), edit a rule value, remove a rule — verify the tier detail reflects each step
- [ ] 8.4 Code review (review agent) before commit
