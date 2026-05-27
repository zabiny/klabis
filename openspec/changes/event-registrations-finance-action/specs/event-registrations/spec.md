## ADDED Requirements

### Requirement: Finance Manager Records Transaction From Registrations List

The system SHALL allow a user with FINANCE:MANAGE authority to record a deposit or a charge on the financial account of any member listed in an event's registrations, directly from that registrations list, without navigating away from the event. The action SHALL open the same unified transaction dialog used elsewhere for recording deposits and charges on a member account, and SHALL prefill the transaction note with the event name so the resulting account history entry identifies the source event. After a successful submission, the user SHALL remain on the registrations list and the dialog SHALL close.

Users without FINANCE:MANAGE authority SHALL NOT see the action in any registration row.

#### Scenario: Finance manager sees the transaction action on every registration row

- **GIVEN** an authenticated user holds FINANCE:MANAGE authority
- **WHEN** the user views the registrations list of an event with at least one registration
- **THEN** every registration row offers an action that opens the unified transaction dialog for that registered member's account

#### Scenario: Member without FINANCE:MANAGE does not see the action

- **GIVEN** an authenticated member without FINANCE:MANAGE authority
- **WHEN** the member views the registrations list of any event
- **THEN** no row offers an action to open the unified transaction dialog

#### Scenario: Dialog opens with the registered member's identity and balance

- **GIVEN** a finance manager views the registrations list of an event
- **WHEN** the finance manager triggers the transaction action on a row
- **THEN** the unified transaction dialog opens
- **AND** the dialog header shows that member's first name, last name, registration number, and current account balance
- **AND** the dialog does not appear before the identity and balance are loaded

#### Scenario: Note is prefilled with the event name

- **GIVEN** a finance manager opens the unified transaction dialog from an event's registrations list
- **WHEN** the dialog is shown
- **THEN** the note field is prefilled with text identifying the event (for example, the event name)
- **AND** the finance manager can edit or clear the note before submitting

#### Scenario: Finance manager records a charge for the event entry fee

- **GIVEN** the unified transaction dialog has been opened from an event's registrations list for a member with sufficient balance
- **WHEN** the finance manager selects the charge tab, enters a positive amount and submits
- **THEN** the charge is recorded on that member's account with the event-derived note
- **AND** the dialog closes
- **AND** the user remains on the registrations list of the same event

#### Scenario: Finance manager records a deposit (refund) from the registrations list

- **GIVEN** the unified transaction dialog has been opened from an event's registrations list
- **WHEN** the finance manager selects the deposit tab, enters a positive amount and submits
- **THEN** the deposit is recorded on that member's account with the event-derived note
- **AND** the dialog closes
- **AND** the user remains on the registrations list of the same event

#### Scenario: Direct API attempt without FINANCE:MANAGE is refused

- **GIVEN** an authenticated user without FINANCE:MANAGE authority
- **WHEN** the user attempts a direct API request to record a transaction on a registered member's account via the registrations list resource
- **THEN** the request is refused with an authorization error
- **AND** no transaction is recorded
