## MODIFIED Requirements

### Requirement: Recording a Charge

The system SHALL allow a finance manager to record a charge (deduction) on any member's account. A charge decreases the account balance and is always recorded regardless of the resulting balance — a finance-manager charge is NOT constrained by the overdraft limit and MAY drive the balance below it. The charge input SHALL be available within a unified transaction dialog that also offers the deposit operation, with the type of operation selected by a tab control. When only the charge operation is permitted for the current user, the dialog SHALL show the charge form without a tab control.

#### Scenario: Finance manager records a charge

- **WHEN** a finance manager submits a charge with a positive amount, an occurrence date, and an optional note
- **THEN** the transaction is appended to the account history
- **AND** the account balance decreases by that amount

#### Scenario: Finance manager charge may push the balance below the overdraft limit

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** a finance manager records a charge of 300 CZK
- **THEN** the charge is recorded and the balance becomes -700 CZK

#### Scenario: Charge with non-positive amount is rejected

- **WHEN** a finance manager submits a charge with a zero or negative amount
- **THEN** the system rejects the request with a validation error

#### Scenario: Finance manager selects charge in the unified transaction dialog

- **WHEN** a finance manager opens the transaction dialog for a member and selects the charge tab
- **THEN** the dialog shows the charge form
- **AND** the primary action button visually communicates a charge (e.g. destructive/red styling) and confirms the charge

### Requirement: Configurable Overdraft Limit

The system SHALL enforce a single, globally configured overdraft limit (a non-positive amount) for all members. The overdraft limit SHALL apply only to charges originating from a member's event-registration payment: a registration charge that would push the balance below the limit SHALL be rejected. Charges recorded by a finance manager and automatic membership-fee charges SHALL NOT be constrained by this limit. Reversals are exempt from this limit.

#### Scenario: Overdraft limit blocks a registration payment

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** the member attempts to pay an event registration costing 200 CZK from the account
- **THEN** the payment is rejected because it would result in -600 CZK
- **AND** the account history and balance remain unchanged

#### Scenario: Overdraft limit permits a registration payment that stays within it

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** the member pays an event registration costing 50 CZK from the account
- **THEN** the payment is recorded and the balance becomes -450 CZK

#### Scenario: Overdraft limit does not block a finance-manager charge

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** a finance manager records a charge of 200 CZK
- **THEN** the charge is recorded and the balance becomes -600 CZK

#### Scenario: Overdraft limit does not block an automatic membership-fee charge

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** the system charges that member a yearly membership fee of 300 CZK
- **THEN** the charge is recorded and the balance becomes -700 CZK
