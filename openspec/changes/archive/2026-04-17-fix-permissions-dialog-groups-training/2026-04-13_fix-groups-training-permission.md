# Team Coordination File
## Proposal: fix-permissions-dialog-groups-training
## Date: 2026-04-13

## Summary
Add `GROUPS:TRAINING` permission toggle to the member permissions dialog in the frontend. Backend verification that the permissions endpoint handles this permission.

## Goal
- Frontend: Add `GROUPS:TRAINING` entry (label: "Správa tréninkových skupin", description: "Umožňuje vytvářet a spravovat tréninkové skupiny a jejich členy.") to the static permissions list in the dialog.
- Backend: Verify and add tests for `GROUPS:TRAINING` handling in the permissions endpoint.

## Tasks (from tasks.md)
- [ ] 1.1 Add `GROUPS:TRAINING` entry to the static permissions list in the dialog
- [ ] 1.2 Write test: permissions dialog renders a toggle for `GROUPS:TRAINING`
- [ ] 1.3 Verify toggle correctly reflects current state (assigned / not assigned)
- [ ] 2.1 Verify that the permissions save endpoint accepts and persists `GROUPS:TRAINING`
- [ ] 2.2 Write test: saving permissions with `GROUPS:TRAINING` enabled assigns the authority to the member
- [ ] 2.3 Write test: saving permissions with `GROUPS:TRAINING` disabled removes the authority from the member

## Progress Log

### 2026-04-13 — Team Lead (implementation + review complete)

**Frontend (tasks 1.1–1.3):**
- Added `'GROUPS:TRAINING'` entry to `labels.permissions` in `frontend/src/localization/labels.ts` with label "Správa tréninkových skupin" and the specified description
- Added `'GROUPS:TRAINING': 'bg-orange-100 text-orange-600'` color to `PERMISSION_COLORS` in `PermissionsDialog.tsx`
- Added 3 tests to `PermissionsDialog.test.tsx`: renders toggle, reflects assigned state (true), reflects unassigned state (false)

**Backend (tasks 2.1–2.3):**
- Verified `GROUPS_TRAINING` is already defined in `Authority.java` and fully supported by the generic `PermissionServiceImpl.updateUserPermissions()` — no code changes needed
- Added 2 tests to `PermissionControllerTest.java`: accepts GROUPS:TRAINING when enabled (204), removes GROUPS:TRAINING when not in request

**Code review:** Removed one unnecessary "what" comment in backend test. All other patterns correct.

**Test results:** 22/22 frontend, 13/13 backend (PermissionControllerTest) — all green.
