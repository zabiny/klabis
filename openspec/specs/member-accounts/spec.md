# Capability: Member Accounts

## Purpose

Provides each club member with a single prepaid financial account in Czech crowns (CZK). Finance managers can deposit, charge, and reverse transactions; members can view their own account and history. The transaction history is append-only and a globally configured overdraft limit protects against unintended charges.

## Requirements

### Requirement: Every Member Has a Financial Account

The system SHALL provide each club member with a single financial account holding a prepaid balance in Czech crowns (CZK). The account SHALL be created automatically when a member is registered and SHALL persist for the lifetime of the membership, including while the membership is suspended.

#### Scenario: Account is created together with a new member

- **WHEN** a new member is registered in the system
- **THEN** a financial account is automatically created for that member with a zero balance

#### Scenario: Account remains available while membership is suspended

- **WHEN** a member's membership is suspended
- **THEN** the member's financial account remains accessible and can still receive transactions

### Requirement: Member Sees Their Own Account and History

The system SHALL allow any authenticated member to view their own financial account: current balance and a paginated, filterable, sortable history of transactions. The member SHALL NOT see any other member's account.

#### Scenario: Member opens "Finance" from the main menu

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** the member sees their current balance prominently displayed
- **AND** a history of their transactions below it

#### Scenario: Member cannot view another member's account

- **WHEN** a member without FINANCE:MANAGE authorization attempts to view another member's account
- **THEN** the system denies access

#### Scenario: Member cannot record any transaction on their own account

- **WHEN** a member without FINANCE:MANAGE authorization views their own account
- **THEN** no controls for depositing, charging, or reversing transactions are shown

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

### Requirement: Configurable Overdraft Limit

The system SHALL enforce a single, globally configured overdraft limit (a non-positive amount) for all members. Charges that would push the balance below this limit SHALL be rejected. Reversals are exempt from this limit.

#### Scenario: Overdraft limit blocks ordinary charges

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** a finance manager attempts to charge 200 CZK
- **THEN** the charge is rejected because it would result in -600 CZK

#### Scenario: Overdraft limit permits charges that stay within it

- **WHEN** the overdraft limit is set to -500 CZK
- **AND** a member's balance is -400 CZK
- **AND** a finance manager records a charge of 50 CZK
- **THEN** the charge is recorded and the balance becomes -450 CZK

### Requirement: Reversing a Transaction

The system SHALL allow a finance manager to reverse any existing transaction by recording a new transaction that references the original. The reversal SHALL apply the opposite-sign amount and SHALL bypass the overdraft limit. Reversals SHALL never modify or delete the original transaction.

#### Scenario: Reversing a deposit decreases balance back

- **WHEN** a finance manager reverses an existing deposit of 300 CZK
- **THEN** a new transaction is appended that decreases the balance by 300 CZK
- **AND** both transactions remain visible in the history
- **AND** the new transaction is clearly marked as a reversal of the original

#### Scenario: Reversing a charge increases balance back

- **WHEN** a finance manager reverses an existing charge of 300 CZK
- **THEN** a new transaction is appended that increases the balance by 300 CZK
- **AND** both transactions remain visible in the history
- **AND** the new transaction is clearly marked as a reversal of the original

#### Scenario: Reversal bypasses the overdraft limit

- **WHEN** a member's balance is already below the overdraft limit because of a prior mistake
- **AND** a finance manager reverses a transaction whose reversal would temporarily push the balance even lower
- **THEN** the reversal is recorded regardless of the overdraft limit

#### Scenario: A transaction cannot be reversed twice

- **WHEN** a finance manager attempts to reverse a transaction that has already been reversed
- **THEN** the system rejects the request
- **AND** indicates which transaction is the existing reversal

#### Scenario: A reversal itself may be reversed

- **WHEN** a finance manager reverses a transaction that is itself a reversal of another transaction
- **THEN** a new transaction is appended that reverses the reversal
- **AND** the history shows the full chain of reversals

### Requirement: Append-Only History

The system SHALL treat the transaction history as immutable: no transaction can be deleted, edited, or otherwise modified after it is recorded. Corrections SHALL be made by recording further transactions (typically reversals followed by corrected entries).

#### Scenario: Existing transaction cannot be edited

- **WHEN** any user attempts to change the amount, type, or note of an existing transaction
- **THEN** the system does not offer such an action
- **AND** rejects any direct attempt as an error

#### Scenario: Existing transaction cannot be deleted

- **WHEN** any user attempts to delete an existing transaction
- **THEN** the system does not offer such an action
- **AND** rejects any direct attempt as an error

### Requirement: Transaction Records What Happened, When, and By Whom

Each transaction SHALL carry the following information: type (deposit or charge), signed amount in CZK, an optional human-readable note, the date the underlying event occurred (entered by the finance manager), the timestamp the entry was recorded by the system, the identity of the user who recorded it, and, if the transaction is a reversal, a reference to the original transaction.

#### Scenario: Recording a transaction captures both occurrence date and record time

- **WHEN** a finance manager records a transaction with an occurrence date of yesterday
- **THEN** the saved transaction's occurrence date is yesterday
- **AND** the saved transaction's record timestamp is the current server time

#### Scenario: Recording a transaction captures who recorded it

- **WHEN** a finance manager submits any transaction
- **THEN** the saved transaction is associated with that finance manager's user identity

### Requirement: History Pagination, Sorting, and Filtering

The system SHALL present the transaction history with pagination. The user SHALL be able to sort by occurrence date, type, or amount (both ascending and descending) and SHALL be able to filter by occurrence date range and by type.

#### Scenario: History is paginated

- **WHEN** an account has more transactions than fit on a single page
- **THEN** the history view shows controls to move between pages

#### Scenario: User sorts history by occurrence date

- **WHEN** the user chooses to sort by occurrence date descending
- **THEN** the most recent transactions appear first

#### Scenario: User sorts history by amount

- **WHEN** the user chooses to sort by amount ascending
- **THEN** the smallest amounts (most negative first) appear first

#### Scenario: User filters history by date range

- **WHEN** the user supplies a "from" and "to" date
- **THEN** only transactions whose occurrence date falls within that range are shown

#### Scenario: User filters history by type

- **WHEN** the user selects the deposit type filter
- **THEN** only deposits appear in the history
- **AND** charges and reversals matching other types do not appear

### Requirement: Reversed Transactions Are Visually Distinguishable

The system SHALL visually mark transactions that have been reversed and SHALL show the link between a reversal and the transaction it reverses.

#### Scenario: Original transaction shows it has been reversed

- **WHEN** the user views a transaction that has been reversed
- **THEN** the transaction is visually marked as reversed
- **AND** offers a way to navigate to the reversing transaction

#### Scenario: Reversal transaction shows what it reverses

- **WHEN** the user views a reversal transaction
- **THEN** the transaction is visually marked as a reversal
- **AND** offers a way to navigate to the original transaction

### Requirement: Account Endpoint Is Reachable from the Member Detail

The system SHALL expose a navigable link to a member's financial account from the member detail page (for users with FINANCE:MANAGE authority) and from the main menu (for the account owner). The member list SHALL NOT offer navigation to the full account page; it offers only the inline transaction dialog.

#### Scenario: Finance manager navigates from member detail to account

- **WHEN** a user with FINANCE:MANAGE authority views a member's detail page and follows the financial account link
- **THEN** the member's financial account opens

#### Scenario: Member navigates from main menu to own account

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** their own financial account opens

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

### Requirement: Account Page Shows Owner Identity

Any page that displays a member's financial account SHALL show the identity of the account owner — first name, last name, and registration number — alongside the balance and transaction history. This applies both to a member viewing their own account and to a finance manager viewing any member's account, so that the viewer can always confirm whose account they are looking at before reading the balance or recording a transaction. The page SHALL use "Finance" as its main heading (H1), consistent with the corresponding main menu item.

#### Scenario: Member viewing own account sees their own identity

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** the page header shows the member's first name, last name, and registration number
- **AND** the page's main heading reads "Finance"
- **AND** the current balance and transaction history are shown as before

#### Scenario: Finance manager viewing another member's account sees that member's identity

- **WHEN** a user with FINANCE:MANAGE authority navigates from a member's detail page to that member's financial account
- **THEN** the page header shows the target member's first name, last name, and registration number
- **AND** the page's main heading reads "Finance"
- **AND** the current balance and transaction history are shown as before

#### Scenario: Owner identity stays consistent with the account being viewed

- **WHEN** a finance manager navigates between two different members' account pages in the same session
- **THEN** each account page shows the identity of the member that account belongs to
- **AND** the identity shown matches the account whose balance and history are displayed on that page

### Requirement: Transaction History Shows Who Recorded Each Transaction

The system SHALL display, for every transaction in the account history, the full name of the user who recorded the transaction. This applies both to a member viewing their own account and to a finance manager viewing any member's account, so that the viewer can always audit who is responsible for a given record. When the recording user cannot be determined (for example because the user account no longer exists or cannot be looked up), the system SHALL display a dash ("—") instead of a name. The recorder's name SHALL NOT be a navigable link to the recorder's profile.

#### Scenario: Member viewing own account sees who recorded each transaction

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** every row in the transaction history shows the full name of the user who recorded that transaction

#### Scenario: Finance manager viewing another member's account sees who recorded each transaction

- **WHEN** a user with FINANCE:MANAGE authority navigates to another member's financial account
- **THEN** every row in the transaction history shows the full name of the user who recorded that transaction

#### Scenario: Recorder identity cannot be resolved

- **WHEN** a transaction's recording user cannot be determined or looked up
- **THEN** the transaction row shows a dash ("—") instead of a name

#### Scenario: Recorder name is not navigable

- **WHEN** a user views any transaction row in the account history
- **THEN** the recorder's name is displayed as plain text and does not act as a link to the recorder's profile

### Requirement: Transaction Resource Links to Its Recorder

The system SHALL expose, on every transaction resource in the API, a navigable link to the user who recorded the transaction so that clients can retrieve the recorder's basic profile information (name, registration number) without prior knowledge of the user identifier. The flat recorder identifier already present on the transaction resource SHALL remain available as a stable identifier.

#### Scenario: Client follows the transaction recorder link

- **WHEN** a client retrieves a transaction resource and follows the link to its recorder
- **THEN** the client receives the corresponding member resource with basic profile information

#### Scenario: Recorder link is present for any transaction the client may view

- **WHEN** a client retrieves a transaction resource it is permitted to see
- **THEN** the response includes a link to the user who recorded the transaction whenever that user is known
