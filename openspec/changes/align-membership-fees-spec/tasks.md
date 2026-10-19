# Tasks

The behaviour is already implemented, so no code changes are needed:

- Creating a level with a name and a yearly fee, and adding rules on the level detail page: this already exists. The E2E scenario "Administrator creates a fee level" already follows this flow.
- Withdrawing a choice (`removeChoice`) before the deadline, and locking after the deadline or once the campaign is closed: this already exists. Commit `6266c9c7` changed the choice form so that it uses the server-provided withdraw target.
- Relevant year on the profile: commit `6266c9c7` (`fix(membership-fees): keep member fee summary on the campaign year and pre-select last year's tier`).

## 1. E2E coverage for the new spec scenarios

- [ ] 1.1 Add the E2E scenario "Member withdraws the choice before the deadline" to `frontend/e2e/features/membership-fees.feature`. The member opens the choice form, clicks "Zrušit volbu", and then the profile prompts them to choose a tier. Verify with `npm run test:e2e` against a running local environment.
- [ ] 1.2 Add the E2E scenario "Member withdraws a level assigned by an administrator". The administrator assigns a tier, the member withdraws it on the choice form, and then no tier is shown. Verify with `npm run test:e2e`.
- [ ] 1.3 Add the E2E scenario "Member cannot withdraw the choice after the deadline". After the campaign is closed, the choice form shows "Volba je uzamčena." and no "Zrušit volbu" button. Verify with `npm run test:e2e`.
- [ ] 1.4 Add the E2E scenario "Member still sees the chosen level after the campaign is closed". The member chooses a tier and the campaign is closed. The profile then still shows the tier and "Volba je uzamčena.". Verify with `npm run test:e2e`.

## Workflow follow-up

- Archive the change once the E2E scenarios pass, which syncs the delta into `openspec/specs/membership-fees/spec.md`.
