# Family Group Parent Refactor - QA Testing

## Scenarios

### Detail View
- [x] **FG-DETAIL-1**: Family group detail shows "RODIČE" section label
- [x] **FG-DETAIL-2**: Parents displayed with names and links to member profiles
- [x] **FG-AUTH-1**: Admin sees add/remove parent affordances

### Create Family Group
- [x] **FG-CREATE-1**: Family group creation form includes parentIds field

### Authorization
- [x] **FG-AUTH-2**: Regular member (ZBM9500) cannot see parent management actions

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| FG-DETAIL-1 | PASS | Section heading shows "RODIČE" correctly |
| FG-DETAIL-2 | PASS | Parent "Jan Novák (ZBM9000)" displayed; members table shows both parent+member correctly |
| FG-AUTH-1 | PASS | Admin sees: Přidat rodiče, Smazat rodinnou skupinu |
| FG-CREATE-1 | PASS | Form has parentIds checkboxes + memberIds checkboxes. **Issue**: labels show "parentIds*" and "memberIds*" instead of Czech |
| FG-AUTH-2 | PASS | Regular member gets HTTP 403 on family groups page (no access to management). Note: nav link "Rodinné skupiny" still visible — pre-existing issue |

## Issues Found

### ISSUE-1: parentIds and memberIds field labels not localized (LOW)
- **Where**: Create family group form
- **Expected**: "Rodiče*" and "Členové*"
- **Actual**: "parentIds*" and "memberIds*"
- **Root cause**: `parentIds` label added in localization but may not match HAL form field name; `memberIds` label was pre-existing issue
- **Component**: Frontend (localization)
