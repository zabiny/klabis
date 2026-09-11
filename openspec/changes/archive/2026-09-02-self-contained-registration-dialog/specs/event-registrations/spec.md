## ADDED Requirements

### Requirement: Registration form driven by the registration representation

The registration form (new registration and registration edit) SHALL be built from the affordances and data carried by the registration representation addressed by the link the member follows — the `newRegistration` link of an event for a new registration, the registration's own link for editing. The form SHALL work identically from every place that offers such a link (dashboard, events list, event detail). When the followed representation carries no registration affordance, the form SHALL show an error and MUST NOT offer a submit action.

#### Scenario: Member opens a new registration form from any entry point

- **WHEN** a member follows the new-registration link of an event with open registrations — from the dashboard widget, the events list, or the event detail page
- **THEN** the same registration form opens with the member's name context, the event context, and an SI card number prefilled from their profile
- **AND** the form submits as a new registration

#### Scenario: New registration representation does not offer editing

- **WHEN** a member opens the new registration form via the event's new-registration link
- **THEN** the form is driven by the register affordance of the returned registration defaults
- **AND** no edit or unregister affordance is offered by that response

#### Scenario: Registration form without an available register action shows an error

- **WHEN** a member opens the registration form for an event whose registrations are closed
- **THEN** the form shows an error instead of the submit action

#### Scenario: Blocked member does not receive the register action

- **GIVEN** a member is blocked from registering by a sanction
- **WHEN** the member opens the new registration form via the event's new-registration link
- **THEN** the form shows an error instead of the submit action

#### Scenario: Member edits their registration from the registration link

- **WHEN** a member follows the link of their existing registration to edit it
- **THEN** the form opens in edit mode showing the member chip with their name, the current SI card number and category
- **AND** the form submits as an update of the existing registration
