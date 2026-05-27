## MODIFIED Requirements

### Requirement: Finance Manager Manages Any Account

The system SHALL allow users with FINANCE:MANAGE authority to view, deposit to, charge from, and reverse transactions on any member's account. From the member list, a finance manager SHALL be able to record a deposit or a charge on any member's account directly through an inline dialog, without navigating away. Navigation to the full member account page SHALL remain available from the member detail page.

#### Scenario: Finance manager records a transaction directly from the member list

- **WHEN** a user with FINANCE:MANAGE authority views the member list
- **THEN** each member row offers an action that opens an inline dialog for recording a deposit or a charge on that member's account
- **AND** the dialog displays the member's name, registration number, and current balance

#### Scenario: Finance manager opens any member's account from the member detail page

- **WHEN** a user with FINANCE:MANAGE authority views a member's detail page
- **THEN** an action is available to open that member's full financial account page

#### Scenario: Member list does not offer navigation to the full account page

- **WHEN** a user with FINANCE:MANAGE authority views the member list
- **THEN** no action in the member row navigates to the full member account page
- **AND** access to the full account page is reachable through the member detail page

#### Scenario: Finance manager sees deposit, charge, and reverse controls

- **WHEN** a user with FINANCE:MANAGE authority views any member's account
- **THEN** controls for recording a deposit, recording a charge, and reversing existing transactions are shown

### Requirement: Recording a Deposit

The system SHALL allow a finance manager to record a deposit (positive amount) on any member's account. A deposit increases the account balance. The deposit input SHALL be available within a unified transaction dialog that also offers the charge operation, with the type of operation selected by a tab control. When only the deposit operation is permitted for the current user, the dialog SHALL show the deposit form without a tab control.

#### Scenario: Finance manager records a deposit

- **WHEN** a finance manager submits a deposit with a positive amount, an occurrence date, and an optional note
- **THEN** the transaction is appended to the account history
- **AND** the account balance increases by that amount

#### Scenario: Deposit with non-positive amount is rejected

- **WHEN** a finance manager submits a deposit with a zero or negative amount
- **THEN** the system rejects the request with a validation error

#### Scenario: Finance manager selects deposit in the unified transaction dialog

- **WHEN** a finance manager opens the transaction dialog for a member and selects the deposit tab
- **THEN** the dialog shows the deposit form
- **AND** the primary action button visually communicates a deposit (e.g. green styling) and confirms the deposit

### Requirement: Recording a Charge

The system SHALL allow a finance manager to record a charge (deduction) on any member's account. A charge decreases the account balance. The charge input SHALL be available within a unified transaction dialog that also offers the deposit operation, with the type of operation selected by a tab control. When only the charge operation is permitted for the current user, the dialog SHALL show the charge form without a tab control.

#### Scenario: Finance manager records a charge within available balance

- **WHEN** a finance manager submits a charge with a positive amount, an occurrence date, and an optional note
- **AND** the resulting balance stays at or above the configured overdraft limit
- **THEN** the transaction is appended to the account history
- **AND** the account balance decreases by that amount

#### Scenario: Charge with non-positive amount is rejected

- **WHEN** a finance manager submits a charge with a zero or negative amount
- **THEN** the system rejects the request with a validation error

#### Scenario: Charge that would breach the overdraft limit is rejected

- **WHEN** a finance manager submits a charge whose amount would push the balance below the configured overdraft limit
- **THEN** the system rejects the request with an error stating that the overdraft limit would be exceeded
- **AND** the account history and balance remain unchanged

#### Scenario: Finance manager selects charge in the unified transaction dialog

- **WHEN** a finance manager opens the transaction dialog for a member and selects the charge tab
- **THEN** the dialog shows the charge form
- **AND** the primary action button visually communicates a charge (e.g. destructive/red styling) and confirms the charge

### Requirement: Account Endpoint Is Reachable from the Member Detail

The system SHALL expose a navigable link to a member's financial account from the member detail page (for users with FINANCE:MANAGE authority) and from the main menu (for the account owner). The member list SHALL NOT offer navigation to the full account page; it offers only the inline transaction dialog.

#### Scenario: Finance manager navigates from member detail to account

- **WHEN** a user with FINANCE:MANAGE authority views a member's detail page and follows the financial account link
- **THEN** the member's financial account opens

#### Scenario: Member navigates from main menu to own account

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** their own financial account opens

## ADDED Requirements

### Requirement: Unified Transaction Dialog

The system SHALL provide a unified transaction dialog that lets a finance manager record either a deposit or a charge on a member's account from a single overlay. The dialog SHALL show the member's identity (name and registration number) and the current account balance so the finance manager can confirm the context before submitting. The dialog SHALL switch between deposit and charge operations using a tab control. The values entered into shared fields (amount, note) SHALL be preserved when the user switches between tabs. The dialog SHALL be reachable both from the member list and from the member's full account page; in both cases it SHALL behave identically.

#### Scenario: Dialog shows member identity and current balance

- **WHEN** a finance manager opens the unified transaction dialog for a member
- **THEN** the dialog displays the member's first name, last name, registration number, and current account balance before any input is given

#### Scenario: Field values are preserved when switching tabs

- **WHEN** a finance manager enters an amount and a note while the deposit tab is active
- **AND** then switches to the charge tab
- **THEN** the amount and note remain filled in with the previously entered values

#### Scenario: Dialog remembers the last selected operation across sessions

- **WHEN** a finance manager closes the dialog with the charge tab active
- **AND** later opens the dialog again for any member
- **THEN** the charge tab is selected by default

#### Scenario: Dialog hides tabs when only one operation is permitted

- **WHEN** a user has authority for only one of the two operations (deposit or charge) on a member's account
- **THEN** the dialog opens without tab controls and shows only the form for the permitted operation

#### Scenario: Action is not offered when no operation is permitted

- **WHEN** a user has no authority for either deposit or charge on a member's account
- **THEN** no action to open the unified transaction dialog is shown in the member list or on the account page

#### Scenario: Dialog opens with full context loaded

- **WHEN** a finance manager triggers the unified transaction dialog
- **THEN** the dialog appears only once the member identity and account information are fully loaded
- **AND** no partial header (without name or balance) is shown

#### Scenario: Same dialog is used from the member list and from the account page

- **WHEN** a finance manager opens the unified transaction dialog from the member list and submits a deposit
- **AND** another finance manager opens the unified transaction dialog from the account page and submits a deposit on the same member
- **THEN** both submissions produce equivalent transactions in the account history

### Requirement: Member Account Resource Links to Its Owner

The system SHALL expose, on the member account resource, a navigable link to the member who owns the account so that clients can retrieve the owner's basic profile information (name, registration number) without prior knowledge of the member identifier.

#### Scenario: Client follows the account owner link

- **WHEN** a client retrieves a member account resource and follows the link to its owner
- **THEN** the client receives the corresponding member resource with basic profile information

#### Scenario: Owner link is present for any account the client may view

- **WHEN** a client retrieves a member account resource it is permitted to see
- **THEN** the response includes a link to the member who owns the account
