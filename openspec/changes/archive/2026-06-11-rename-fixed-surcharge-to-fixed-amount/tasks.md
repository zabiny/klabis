## 1. Backend domain rename

- [x] 1.1 Rename `RuleValue.FixedSurcharge` → `RuleValue.FixedAmount` in `MembershipPaymentRule.java`
- [x] 1.2 Rename factory method `fixedSurcharge(...)` → `fixedAmount(...)` in `MembershipPaymentRule.java`
- [x] 1.3 Update DB discriminator `"FIXED_SURCHARGE"` → `"FIXED_AMOUNT"` in `MembershipPaymentRuleMemento.java` and `MembershipPaymentRuleSnapshotMemento.java`
- [x] 1.4 Update REST infrastructure: `CreateMembershipFeeLevelRequest.java` and `MembershipFeeLevelResponse.java`
- [x] 1.5 Update all test usages of `fixedSurcharge(...)` and `"FIXED_SURCHARGE"` string constant in `MembershipPaymentRuleTest`, `MembershipFeeLevelTest`, `MembershipFeeGroupTest`, `MembershipFeeLevelWithRulesIntegrationTest`, `MembershipFeeLevelPersistenceTest`
- [x] 1.6 Run backend tests — confirm no failures

## 2. Frontend labels

- [x] 2.1 Rename key `fixedSurcharge` → `fixedAmount` and update label to `'Fixní částka (Kč)'` in `src/localization/labels.ts`
- [x] 2.2 Update enum value `"FIXED_SURCHARGE"` → `"FIXED_AMOUNT"` and prompt `"Fixní příplatek (CZK)"` → `"Fixní částka (Kč)"` in `KlabisFieldsFactory.tsx`
- [x] 2.3 Replace hardcoded `"Fixní příplatek"` → `"Fixní částka"` in `MembershipFeeLevelDetailPage.tsx` and `MembershipFeeGroupDetailPage.tsx`
- [x] 2.4 Run frontend tests — confirm no failures

## 3. Spec terminology sync

- [x] 3.1 In `openspec/specs/membership-fees/spec.md` apply delta from change specs: replace "fixed surcharge amount" → "fixed amount", "fixed surcharge rule" → "fixed amount rule", "fixed surcharge" (rule-type context) → "fixed amount" — leave "surcharge" unchanged where it refers to the calculated event charge result (lines 139–148)
