# Membership Fee Selection Campaign — Full Lifecycle QA Testing

Date: 2026-06-11 · Spec: openspec/specs/membership-fees/spec.md

## Scenarios

### Campaign management (admin) — covered by run 01, re-verified here
- [ ] **CAMP-1**: Admin starts a campaign (select levels, future deadline) → 201, detail page
- [ ] **CAMP-2**: Reject second active campaign → 409
- [ ] **CAMP-3**: Reject deadline today/past on create → 400
- [ ] **CAMP-4**: Change deadline of active campaign to future → 200
- [ ] **CAMP-5**: Reject change deadline to past → 400
- [ ] **CAMP-6**: changeDeadline affordance present on active, absent on closed
- [ ] **CAMP-7**: List + detail HAL navigation, groups/levels table

### Member selection (ZBM9500)
- [ ] **SEL-1**: Member chooses a published level before deadline → assigned
- [ ] **SEL-2**: Member changes choice before deadline → reassigned
- [ ] **SEL-3**: Member cannot change after deadline (locked)
- [ ] **SEL-4**: Previous year level pre-filled as non-binding default

### Profile widget
- [ ] **PROF-1**: Profile shows current assigned level
- [ ] **PROF-2**: No-choice member prompted to choose while voting open

### Emergency assignment (admin)
- [ ] **EMRG-1**: Admin assigns level to member after deadline
- [ ] **EMRG-2**: Admin changes member's level after deadline

### Published level snapshot editing (admin)
- [ ] **EDIT-1**: Admin edits published level (fee+rules) before any surcharge
- [ ] **EDIT-2**: editSnapshot affordance absent when status != EDITABLE (if observable)

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| CAMP-1 | PASS | run 01: 201 + detail page |
| CAMP-2 | PASS | run 01: 409 second active campaign |
| CAMP-3 | PASS | run 01: 400 deadline today/past on create |
| CAMP-4 | PASS | changed 2027 deadline today->31.12.2026 via UI, 200, value updated |
| CAMP-5 | PASS | run 01: 400 deadline past on change |
| CAMP-6 | PASS | changeDeadline button present on active campaign (closed not testable) |
| CAMP-7 | PASS | list+detail HAL nav, groups table (1 group "Základ") |
| EDIT-1 | PASS | edited fee 1000->1200 CZK, PATCH 204, snapshot reflects, status EDITABLE |
| EMRG-1 | FAIL | assign-member modal has NO member picker; POST to .../members/ (no memberId) -> 404 |
| EMRG-2 | FAIL | same root cause as EMRG-1 |
| SEL-1 | PASS | /fee-choice/2027 shows levels, "Zvolit tier" -> assigned, shows "Aktuální tier: Základ 1200 Kč/rok" |
| SEL-2 | BLOCKED | campaign 2027 has only ONE published level -> nothing to switch to |
| SEL-3 | BLOCKED | cannot test post-deadline lock without a closed campaign with open voting first |
| SEL-4 | BLOCKED | single-level campaign + no prior-year data for Eva -> prefill default not observable |
| PROF-1 | FAIL | profile widget hardcodes year=2026; 2027 assignment not shown |
| PROF-2 | FAIL | widget shows "Lhůta pro výběr uplynula" for 2026; never looks at open 2027 campaign |
| EDIT-2 | BLOCKED | cannot reach SURCHARGES_STARTED state via UI to verify affordance removal |

## Findings (severity ordered)

1. [HIGH] Emergency assignment broken — `Přiřadit člena` modal exposes only `year`, no member
   selector. Submitting POSTs to `/api/membership-fee-groups/{id}/members/` (no memberId) -> 404
   "No static resource". assignMember affordance/UI cannot pick a target member. Blocks EMRG-1/2.
   Component: frontend (MembershipFeeGroupDetailPage assignMember form) + affordance URL template.

2. [HIGH] Profile fee widget queries wrong year — MemberDetailPage.tsx:117 hardcodes
   `new Date().getFullYear()` (=2026). Active campaign is for upcoming year 2027. Widget shows
   "Lhůta pro výběr uplynula" and never surfaces the open 2027 choice. Breaks PROF-1, PROF-2 and
   the entire member entry-point to choosing (spec "Member Sees Their Current Fee Level").
   Component: frontend.

3. [MED] Untranslated form field labels — create-campaign form shows raw `levelIds`, `year`;
   assign-member modal shows `year*`. Component: frontend labels / template field i18n.

4. [MED] Raw English backend problem-detail messages shown in Czech UI modals (e.g.
   "Deadline Not In Future", 409 detail "Fee levels for year 2027 have already been published").
   Stale "published" wording vs new campaign terminology. Component: backend messages + frontend.

5. [LOW] Stale "Vypsat rok" label on create button/dialog (old terminology) vs new
   "Kampaň volby členského příspěvku". Component: frontend labels.

6. [LOW/observation] Bootstrap tier "Základ" had 1 payment rule, but its published-level
   snapshot in campaign 2027 shows "Žádná pravidla spoluúčasti" (no rules). Possible rule-snapshot
   copy gap when publishing, or expected (single level published without rules). Needs backend check.

### Notes on coverage
- Single-level campaign (only "Základ" published) blocks change-of-choice and prefill scenarios.
- No closed campaign available -> closed-deadline rejection and post-deadline lock untested via UI.
- Test data mutated: campaign 2027 deadline now 31.12.2026; group "Základ" fee 1000->1200 CZK;
  Eva Svobodová (ZBM9500) assigned to "Základ" for 2027.
