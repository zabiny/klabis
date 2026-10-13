## ADDED Requirements

### Requirement: SI Policy Of An Event

A manager or event coordinator SHALL be able to set, per event, how the event uses SI cards: "Nepoužívá se", "Volitelný" or "Povinný", and whether participants may rent an SI card from the organiser. When creating an event in Klabis, the manager MUST choose the SI usage in the event form; the rental option is unticked by default. An event imported from ORIS requires an SI card and allows rental. The policy is not synchronised with ORIS. The policy can be changed while the event is DRAFT or ACTIVE, even when members are already registered; SI card numbers already recorded on registrations are kept.

#### Scenario: Manager chooses the SI usage when creating an event

- **WHEN** a user with EVENTS:MANAGE permission opens the form to create an event
- **THEN** the form shows the SI usage choice "Nepoužívá se" / "Volitelný" / "Povinný" with nothing preselected
- **AND** the "Půjčení čipu povoleno" tick box is unticked

#### Scenario: Event cannot be created without choosing the SI usage

- **WHEN** a manager submits the create event form without choosing the SI usage
- **THEN** the form shows an error that the SI usage must be chosen
- **AND** the event is not created

#### Scenario: Event imported from ORIS requires an SI card and allows rental

- **WHEN** an event is imported from ORIS
- **THEN** the event requires an SI card on registration
- **AND** SI card rental is allowed

#### Scenario: Synchronisation with ORIS does not change the SI policy

- **GIVEN** an ORIS-imported event whose SI policy the manager changed to "Volitelný"
- **WHEN** the event is synchronised with ORIS afterwards
- **THEN** the event keeps the SI policy "Volitelný"
- **AND** no decision about a difference against ORIS is asked of anyone

#### Scenario: Manager sets an event that does not use SI

- **WHEN** a manager creates or edits an event and chooses "Nepoužívá se" for SI cards
- **THEN** the event is saved with SI cards not used
- **AND** the "Půjčení čipu povoleno" tick box is not offered

#### Scenario: Manager allows SI card rental

- **WHEN** a manager edits an event that uses SI cards and ticks "Půjčení čipu povoleno"
- **THEN** the event is saved with SI card rental allowed

#### Scenario: Coordinator changes the SI policy

- **GIVEN** a DRAFT or ACTIVE event that lists the current user as a coordinator
- **WHEN** the coordinator edits the event and changes the SI usage or the rental option
- **THEN** the event is updated with the new SI policy
- **AND** no EVENTS:MANAGE permission is required

#### Scenario: Inline edit on the event detail page includes the SI policy

- **WHEN** a manager or coordinator edits an event inline on the detail page
- **THEN** the form shows the SI usage choice and the "Půjčení čipu povoleno" tick box reflecting the current settings

#### Scenario: Changing the policy keeps recorded SI card numbers

- **GIVEN** an ACTIVE event requiring SI cards with members already registered with their SI card numbers
- **WHEN** a manager changes the SI usage to "Nepoužívá se"
- **THEN** the event is updated without an error
- **AND** the SI card numbers already recorded on the registrations are retained

#### Scenario: SI policy cannot be changed on a finished or cancelled event

- **WHEN** a user attempts to change the SI policy of a FINISHED or CANCELLED event
- **THEN** the system shows an error that the event can no longer be modified

#### Scenario: Event detail shows the SI policy

- **WHEN** a member opens the detail page of an event
- **THEN** the page shows whether the event requires, accepts or does not use an SI card
- **AND** whether SI card rental is available
