## 1. Backend: prefill affordance

- [x] 1.1 Update `docs/openapi/spec/events.yaml` — getRegistration `x-hal-templates`: add `registerForEvent` (present only on the new=true prefill response, when registrations are open and the member is not blocked), narrow `editRegistration`/`unregisterFromEvent` descriptions to the existing-registration (new=false) response
- [x] 1.2 Extend `RegistrationView` with `boolean prefill` and set it in `EventRegistrationController` (new=true branch → true; existing registration and list → false)
- [x] 1.3 `RegistrationDetailsPostprocessor`: inject `MemberRegistrationSanctionPort`; on prefill attach `registerForEvent` affordance (prompted `categoryId` options) gated on `EventAffordanceSupport.shouldOfferRegistration` + `!isMemberBlocked`, and stop attaching `editRegistration`/`unregisterFromEvent`; existing-registration branch unchanged
- [x] 1.4 Tests in `EventRegistrationControllerTest.NewRegistrationDefaultsTests`: prefill carries POST `registerForEvent` with prompted categoryId options and no edit/unregister affordances; no affordance when registrations closed; no affordance for blocked member; existing registration GET still carries `editRegistration`
- [x] 1.5 Run backend tests via test-runner agent (EventRegistrationControllerTest), code review, commit

## 2. Frontend: self-contained dialog

- [x] 2.1 Regenerate API types (`npm run openapi`) — `GetRegistrationHal` gains `registerForEvent`
- [x] 2.2 Create hook `useRegistrationDialogData(registration: Link | null)` — chained queries (registration → event link), derived mode/template from affordance, memberName, initialValues, eventContext, isLoading/error
- [x] 2.3 Rewrite `EventRegistrationDialog` — props `{registration: Link | null, onClose, onRegistered?}`; null renders nothing; presentational inner component fed by the hook; missing-affordance error state; preserve testids
- [x] 2.4 Update callers — EventDetailPage (edit state → `Link | null`, new opens `newRegistration` link), EventsPage (`newRegistrationState` → `Link | null`), UpcomingDeadlinesWidget + useUpcomingDeadlines (item keeps `newRegistration?: Link`, drop dialog-only fields)
- [x] 2.5 Tests: new `useRegistrationDialogData.test.tsx`; rewrite `EventRegistrationDialog.test.tsx`; update EventDetailPage/EventsPage/UpcomingDeadlinesWidget/useUpcomingDeadlines tests
- [x] 2.6 Run frontend tests via test-runner agent (6 files, sequential), `npm run build`, `npm run lint`, code review, commit

## 3. Delivery

- [x] 3.1 `npm run refresh-backend-server-resources`
- [x] 3.2 Manual QA on http://localhost:3000 — new registration from dashboard/list/detail with shared-offer gating, edit own registration with member chip, admin edits foreign registration, closed registrations show error state
- [x] 3.3 Tick tasks, commit remaining openspec changes
