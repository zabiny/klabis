# Tasks

Conventions for every group (see frontend/CLAUDE.md):
- Red-Green-Refactor: write the failing Testing Library test first (`getByLabelText` / `getByRole(..., {name})`), then the minimal fix.
- Label texts come from `@/localization` (`getFieldLabel`, `getTemplateLabel`, `labels.*`) — no hard-coded strings in components.
- A group is done when `npm run test`, `npm run lint` and `npm run build` pass in `frontend/` (build catches errors `tsc --noEmit` misses). Each group is independently committable.

## 1. Action buttons announced by their visible text

- [ ] 1.1 Rewrite "should have proper aria-label" in `frontend/src/components/HalNavigator2/HalFormButton.test.tsx` to assert `getByRole('button', {name: 'Create Member'})` and absence of an `aria-label` attribute; add a case for `hideLabel` asserting the icon-only button is found by `getByRole('button', {name: 'Create Member'})`; verify the first case fails (red)
- [ ] 1.2 In `HalFormTemplateButton.tsx` set `aria-label` only when `hideLabel` is true (value `displayText`); verify both 1.1 tests pass and `grep -rn "Select .* form" frontend/src` returns no hits outside `e2e/`

## 2. Shared field label and text inputs (HalFormsInput, HalFormsTextArea, HalFormsDateTime)

- [ ] 2.1 Add failing tests rendering each component with `renderMode="input"` and asserting `getByRole('textbox', {name: <getFieldLabel(name)>})` (DateTime: `getByLabelText`), plus a case where `prop.prompt` wins over the localized label; extend the existing `HalFormsTextArea.test.tsx` / `HalFormsDateTime.test.tsx`, create `HalFormsInput.test.tsx`
- [ ] 2.2 Introduce one shared field-label helper (`prop.prompt || getFieldLabel(prop.name)`) beside the field components and use it for the visible label in `field` mode and for `aria-label` in `input` mode in all three components (DateTime switches from raw `prop.name` to the localized label); refactor HalFormsMultiSelect to the same helper; verify 2.1 tests and the existing MultiSelect tests pass

## 3. Selection fields (HalFormsSelect, HalFormsMemberId, HalFormsCheckbox)

- [ ] 3.1 Add failing `input`-mode tests: HalFormsSelect → `getByRole('combobox', {name: <label>})`; HalFormsMemberId → `getByRole('combobox', {name: <label>})` while the search box keeps its `labels.ui.optionSearch` name (extend `HalFormsMemberId.test.tsx`); HalFormsCheckbox → `getByRole('checkbox', {name: <label>})`
- [ ] 3.2 Apply the shared helper to the three components (`field` mode of HalFormsSelect switches from raw `prop.name` to the localized label); verify 3.1 tests pass

## 4. Option groups (HalFormsRadio, HalFormsCheckboxGroup)

- [ ] 4.1 Add failing `input`-mode tests asserting `getByRole('group', {name: <label>})` for HalFormsRadio (new test file) and HalFormsCheckboxGroup (extend `HalFormsCheckboxGroup.test.tsx`)
- [ ] 4.2 Add an optional group-name prop to `RadioGroup` and the group mode of `CheckboxField` (applied to the `role="group"` container only when no visible label is rendered) and pass the shared field label from both HAL-FORMS components in `input` mode (HalFormsRadio `field` mode switches to the localized label); verify 4.1 tests and existing group tests pass

## 5. Page-level label parity

- [ ] 5.1 In `EventDetailPage.test.tsx` add a test that switches a draft event to inline edit mode and finds the location and date fields by `getByLabelText(labels.fields.location)` / `getByLabelText(labels.fields.eventDate)` — the same texts shown in the `DetailRow`; fix any divergent caller found
- [ ] 5.2 In `KlabisFieldsFactory.test.tsx` add `input`-mode tests for a composite field and the license field asserting each sub-field is found by its `DetailRow` label (e.g. "Stupeň", "Platnost"); verify `LegalGuardianInputField.integration.test.tsx` still passes and the member picker is reachable by its label

## 6. E2E without accessibility workarounds

- [ ] 6.1 In `frontend/e2e/steps/form.steps.ts` reduce `field()` to `page.getByLabel(label, {exact: true})` and `I click` to `getByRole('button', {name: label, exact: true})` or link; delete the two workaround comments
- [ ] 6.2 Start the app (`./runLocalEnvironment.sh`, after checking ports 8443/3000) and run `cd frontend && npm run test:e2e`; verify all scenarios pass. A failure means a control still lacks a name — fix it in the matching group with a unit test first

## Workflow follow-up

- Code review (code-review agent) before committing each group.
- Archive the change after implementation and review; this adds the two requirements to `openspec/specs/non-functional-requirements/spec.md`.
