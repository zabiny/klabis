# Member Detail Buttons - QA Testing

## Scenarios

### Group Navigation Buttons in Action Bar
- [x] **MBT-1**: Member in training group → "Tréninková skupina" button appears in action bar, click navigates to group detail
- [x] **MBT-2**: Member in family group → "Rodina" button appears in action bar, click navigates to group detail
- [x] **MBT-3**: Member in both groups → both buttons present and each navigates correctly
- [~] **MBT-4**: Member in neither group → no group navigation button in action bar
- [x] **MBT-5**: No "Vložit / Vybrat" button on any member detail view (admin, self, other)
- [x] **MBT-6**: Regular member (ZBM9500) on own profile → group navigation button(s) shown if in a group
- [x] **MBT-7**: No embedded "Tréninková skupina" / "Rodinná skupina" sections below main content

---

## Results

### Iteration 1

**Test data:**
- Jan Novák (ZBM9000) — admin; member of "Hobby" training group + "QA Test Rodinná skupina" family group
- Tomáš Král (ZBM8800) — member of "NAV-4 Test Rodina" family group only (no training group)
- Eva Svobodová (ZBM9500) — trainer of "NAV-5 Test Tréninková 2" training group + member of family group

| Scenario | Result | Note |
|----------|--------|------|
| MBT-1 | PASS | Jan Novák: "Tréninková skupina" button present, click navigated to `/training-groups/be79ac90...` ("Hobby") |
| MBT-2 | PASS | Jan Novák: "Rodina" button present, click navigated to `/family-groups/70bff351...` ("QA Test Rodinná skupina") |
| MBT-3 | PASS | Jan Novák: both buttons visible simultaneously, each navigates to correct group detail |
| MBT-4 | PARTIAL | No bootstrap member exists with neither group. Partial coverage via Tomáš Král (has familyGroup but no trainingGroup): "Tréninková skupina" button correctly absent, "Rodina" button correctly present. Full scenario untested. |
| MBT-5 | PASS | No "Vložit / Vybrat" button found in admin view (Jan Novák), self view (Eva Svobodová), or other-member view (Tomáš Král) |
| MBT-6 | PASS | Eva Svobodová (ZBM9500) on own profile (self-edit view): both "Tréninková skupina" and "Rodina" buttons present; "Tréninková skupina" click navigated to `/training-groups/7fe14f3a...` ("NAV-5 Test Tréninková 2") |
| MBT-7 | PASS | No embedded "Tréninková skupina" / "Rodinná skupina" sections below main content on any checked member detail page |

**Result: 6 PASS, 0 FAIL, 1 PARTIAL (MBT-4 — no zero-group bootstrap member)**
