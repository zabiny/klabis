## 1. Frontend: Add GROUPS:TRAINING to Permissions Dialog

- [x] 1.1 Add `GROUPS:TRAINING` entry to the static permissions list in the dialog (label: "Správa tréninkových skupin", description: "Umožňuje vytvářet a spravovat tréninkové skupiny a jejich členy.")
- [x] 1.2 Write test: permissions dialog renders a toggle for `GROUPS:TRAINING`
- [x] 1.3 Verify toggle correctly reflects current state (assigned / not assigned)

## 2. Backend: Verify GROUPS:TRAINING is Handled by Permissions Endpoint

- [x] 2.1 Verify that the permissions save endpoint accepts and persists `GROUPS:TRAINING`
- [x] 2.2 Write test: saving permissions with `GROUPS:TRAINING` enabled assigns the authority to the member
- [x] 2.3 Write test: saving permissions with `GROUPS:TRAINING` disabled removes the authority from the member
