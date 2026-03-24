## 1. Backend Validation

- [x] 1.1 Add test: registering CZ member without birth number fails with validation error
- [x] 1.2 Add test: updating CZ member removing birth number (setting to null) fails with validation error
- [x] 1.3 Update `Member.validateBirthNumberNationality()` to require birth number when nationality is CZ
- [x] 1.4 Update `MembersDataBootstrap` to include valid birth numbers for bootstrap CZ members (Jan Novak: `900315/1234`, Eva Svobodova: `955722/1234`)
- [x] 1.5 Fix any existing tests broken by the new required validation

## 2. Frontend

- [x] 2.1 Member form: show birth number field only when nationality is CZ, mark as required
- [x] 2.2 Member form: hide birth number field and clear value when nationality changes from CZ to non-CZ
- [x] 2.3 Member form: show birth number field when nationality changes to CZ
