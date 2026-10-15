# Tasks

## 1. Common unregister endpoint

- [x] 1.1 Spec and codegen: in `docs/openapi/spec/events.yaml` move `unregisterFromEvent` to `/api/events/{eventId}/registrations/{memberId}` (delete), add `x-klabis-authority: EVENTS_REGISTRATIONS` and `x-klabis-owner-visible: true`, drop `ActingMember` args, update descriptions of the `unregisterFromEvent` affordance; regenerate backend interfaces and frontend types
- [x] 1.2 Backend: `EventRegistrationController.unregisterFromEvent` takes `memberId` from the path; `withEditAndUnregisterAffordances` offers the template by authorization (no ownership check); `EventController` list item passes `currentMemberId` in the URL; WebMvcTest: member unregisters self, member cannot unregister another (403), manager unregisters anyone, template shown to manager on another member's registration and not shown to a plain member or coordinator
- [x] 1.3 Update backend tests and docs that reference the old path (`EventRegistrationControllerTest`, `EventControllerTest`, `OrisEventRegistrationSyncScenarioIntegrationTest`, others found by grep) and verify the backend suite is green

## 2. Frontend check

- [x] 2.1 Frontend: verify with vitest in `EventRegistrationDialog.test.tsx` that "Odhlásit" appears and submits to the template target when the loaded registration belongs to another member; adjust code only if the test shows a gap; `npm run build`
