# Training Group Trainer Refactor - QA Testing

## Scenarios

### Create Training Group
- [x] **TG-CREATE-1**: Admin creates training group — form includes trainer selection field

### Detail View
- [x] **TG-DETAIL-1**: Training group detail shows "TRENÉŘI" section label
- [x] **TG-DETAIL-2**: Trainer displayed with name and link to member profile
- [x] **TG-EDIT-1**: Single "Upravit skupinu" button (not separate name/age range buttons)

### Authorization
- [x] **TG-AUTH-1**: Admin sees edit/delete/add trainer affordances on detail page
- [x] **TG-AUTH-2**: Regular member (ZBM9500) cannot access training groups page

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| TG-CREATE-1 | PASS | Form has trainerId combobox with member selection. **Issue**: field label shows "trainerId*" instead of Czech "Trenér*" (missing localization) |
| TG-DETAIL-1 | PASS | Section heading shows "TRENÉŘI" correctly |
| TG-DETAIL-2 | PASS | Trainer "Jan Novák (ZBM9000)" displayed correctly |
| TG-EDIT-1 | PASS | Single "Upravit skupinu" button present, no separate age-range button |
| TG-AUTH-1 | PASS | Admin sees: Upravit skupinu, Smazat tréninkovou skupinu, Přidat trenéra, Přidat člena |
| TG-AUTH-2 | PASS | "Tréninkové skupiny" link not visible in navigation for regular member |

## Issues Found

### ISSUE-1: trainerId field label not localized (LOW)
- **Where**: Create training group form
- **Expected**: Field label "Trenér*"
- **Actual**: Field label "trainerId*"
- **Root cause**: Missing `trainerId` key in `labels.ts` fields section
- **Component**: Frontend (localization)
