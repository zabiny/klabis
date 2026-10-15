# Design

## Context

`unregisterFromEvent` is `DELETE /api/events/{eventId}/registrations`; it takes the member from `@ActingMember` and has no authority annotation. `editRegistration` is `PUT /api/events/{eventId}/registrations/{memberId}` with `x-klabis-authority: EVENTS_REGISTRATIONS` and `x-klabis-owner-visible: true` (member over self, manager over anyone). The `unregisterFromEvent` template is attached to the registration self link (`withEditAndUnregisterAffordances`, only when `memberId` equals the acting member) and to the event list item self link (only for the registered caller). The frontend renders the `unregisterFromEvent` template through the generic template form using its `target`, so it is agnostic of the URL shape. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One unregister operation for "myself" and "someone else", authorized exactly like `editRegistration`.
- Offer the template to everyone allowed to use it, so existing UI (edit dialog) works for managers without new frontend code.

**Non-Goals:**
- Changing timing rules or domain behaviour of `Event.unregisterMember`.
- Letting coordinators (read-only grant) unregister.
- Adding an unregister action to rows of the registrations list.

## Decisions

**D1 – Move the operation to `DELETE /api/events/{eventId}/registrations/{memberId}` and remove the old path.** The operation id `unregisterFromEvent` is kept, so existing affordance code and the frontend's template name stay valid. The operation gets `x-klabis-authority: EVENTS_REGISTRATIONS` and `x-klabis-owner-visible: true`, mirroring `editRegistration`; `ActingMember` and the `x-spring-provide-args` entry are dropped. Authorization of the self case comes from the owner-visible rule, not from controller code. Alternative – keep the old endpoint next to a new manager-only one – rejected: two operations and two templates for one business action.

**D2 – The controller passes the path `memberId` straight to `registrationService.unregisterMember`.** No domain or application-service change: `unregisterMember(eventId, memberId)` already takes the target member. Existing errors (not registered, after event date, after deadline) are unchanged.

**D3 – Affordance offering follows authorization, not a hand-written ownership check.** The registration detail drops the `memberId.equals(actingMember)` condition in `withEditAndUnregisterAffordances`, and the event list keeps offering the template for the registered caller, now with `memberId = currentMemberId` in the URL. Because `klabisAfford` already filters affordances by the operation's authority rules, a manager sees the template on others' registrations and a plain member does not. Coordinators hold only a read grant, so they get no template. The "registrations open" precondition stays as is.

**D4 – No frontend code change expected.** `EventRegistrationDialog` shows "Odhlásit" from the presence of the `unregisterFromEvent` template on the loaded registration and submits to the template `target`; `EventsPage` does the same for the list row. A frontend test confirms the dialog shows the button for a template whose target carries another member id.

## Risks / Trade-offs

- [Breaking path change for any external client of the old endpoint] → the application is in development with only the bundled frontend as a client, which follows the template target.
- [A manager can unregister a member whose registration is synchronised with ORIS] → existing synchronisation behaviour ("unregistering while the ORIS entry still exists" waits for a decision) applies unchanged to this path.
- [Owner-visible rule must permit DELETE for self] → verified by WebMvcTest: member unregisters self, member cannot unregister another, manager can unregister anyone.

## REST API

- `DELETE /api/events/{eventId}/registrations/{memberId}` (operation `unregisterFromEvent`, tag EventRegistrations): unregister member `memberId`. Authority EVENTS:REGISTRATIONS, or the member themselves (owner-visible). `204` on success; `400/401/403/404/409/422` as before (`403` for another member without the authority, `404` when the event or the registration does not exist).
- Removed: `DELETE /api/events/{eventId}/registrations`.
- HAL+FORMS: `unregisterFromEvent` template on `GET /api/events/{eventId}/registrations/{memberId}` (existing-registration response, registrations open) for the owner and for users with EVENTS:REGISTRATIONS; on the event list item self link for the registered caller. Its target is the new path.

## Domain

No changes.

## Glossary

- **Správce přihlášek:** user holding EVENTS:REGISTRATIONS.
