# Tasks

## 1. Prefill fix in HAL+FORMS form data

- [x] 1.1 Write a failing test for `useHalFormData`: when the template target equals the current resource and the same URL is already cached, `formData` equals the passed `resourceData`. Fix `useHalFormData` to use fetched data only when `shouldFetch` is true. Verify: the new test passes.
- [x] 1.2 Write a failing `EventRegistrationDialog` test: edit mode shows category, notes, rental request and shared service choices from the existing registration. Verify: test passes after 1.1.
- [x] 1.3 Run the whole frontend suite and manually reopen member edit and other inline-edit forms to confirm no regression. Verify: suite green, no value lost in those forms.

## 2. Edit registration on the event detail

- [x] 2.1 Write a failing `EventDetailPage` test: with `editRegistration` on the event, an "Upravit přihlášku" button opens the dialog and no "Odhlásit se z akce" button is rendered; without the affordance neither edit button appears. Replace the `unregisterFromEvent` button accordingly and refetch after save. Verify: tests pass; in the browser unregister works through the dialog.

## 3. Edit registration on the dashboard

- [x] 3.1 Write failing tests for `useMyUpcomingRegistrations` (exposes `editRegistration` target per item) and `HomePage` (edit button only when the affordance is present, click opens the dialog and does not navigate). Implement the button and dialog wiring with refetch after save. Verify: tests pass; in the browser the widget shows the button for registered events with open registrations.

## 4. Verification

- [ ] 4.1 Run `npm run build` and `npm run lint`; QA the three entry points with Playwright on http://localhost:3000 (user ZBM9500): prefilled values, edit save, unregister from the dialog. Verify: build clean, scenarios from the delta specs pass.
- [ ] 4.2 Run `npm run refresh-backend-server-resources` before committing frontend changes. Verify: backend static resources updated and staged.
