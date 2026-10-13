# Proposal

## Why

Every registration today requires an SI card number. That does not fit every event: trainings and social events do not use SI at all, some events accept registrations without a card known in advance, and many races let a participant rent a card from the organiser. Registrations synchronised from ORIS (`oris-event-registration-sync`) can also arrive without an SI card number when the participant rents one. The event needs to say how it uses SI, and the registration needs to record a rental.

## What Changes

- An event gets an **SI policy** set by the manager or coordinator in the event form:
  - **SI usage**: not used / optional / required — the event manager chooses it in the event form when creating an event in Klabis; there is no preselected value.
  - **SI rental allowed**: yes / no (unticked by default in the form; meaningful only when SI is used).
  - Events imported from ORIS get "required, rental allowed"; the manager or coordinator can change it afterwards.
- The registration form and edit form follow the policy:
  - not used → no SI card field;
  - optional → SI card field not required;
  - required → SI card field required, unless the member ticks "Chci si půjčit čip" where rental is allowed.
- A registration records whether the member wants to rent an SI card ("půjčení čipu"); it is shown on the registration detail.
- Changing the policy on an event with existing registrations is always allowed; SI card numbers already recorded are kept.

```mermaid
flowchart TD
    P{SI usage} -->|not used| N[no SI field]
    P -->|optional| O[SI field optional]
    P -->|required| R{rental allowed?}
    R -->|no| RQ[SI field required]
    R -->|yes| RR[SI field required OR 'Chci si půjčit čip']
    O --> OR{rental allowed?}
    OR -->|yes| ORT['Chci si půjčit čip' offered]
```

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `events`: the event form gains the SI policy (usage + rental), editable while DRAFT or ACTIVE.
- `event-registrations`: registration and edit forms follow the event's SI policy; registrations record SI rental; SI card validation applies only when a number is entered or required.

## Impact

- **Backend (`events` module):** `Event` gains an SI policy; `EventRegistration` gains the rental flag and an optional SI card number; `events.events` gains two columns and `events.event_registrations.si_card_number` becomes nullable (V001 updated in place). Existing events get "required, no rental".
- **API (`docs/openapi/spec/events.yaml`):** additive — SI policy on event create/update/detail, `wantsToRentSi` on registration request/response, `siCardNumber` no longer required in requests (validated against the policy by the domain).
- **Frontend:** event form fields; registration dialog renders the SI field and rental tick box from HAL-FORMS metadata.
- **Follow-up:** `oris-event-registration-sync` maps ORIS SI / RentSI onto this model.
