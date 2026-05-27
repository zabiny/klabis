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

The system SHALL allow users with FINANCE:MANAGE authority to view, deposit to, charge from, and reverse transactions on any member's account.

#### Scenario: Finance manager opens any member's account from the member list

- **WHEN** a user with FINANCE:MANAGE authority views the member list
- **THEN** each member row offers an action to open that member's financial account

#### Scenario: Finance manager opens any member's account from the member detail page

- **WHEN** a user with FINANCE:MANAGE authority views a member's detail page
- **THEN** an action is available to open that member's financial account

#### Scenario: Finance manager sees deposit, charge, and reverse controls

- **WHEN** a user with FINANCE:MANAGE authority views any member's account
- **THEN** controls for recording a deposit, recording a charge, and reversing existing transactions are shown

### Requirement: Recording a Deposit

The system SHALL allow a finance manager to record a deposit (positive amount) on any member's account. A deposit increases the account balance.

#### Scenario: Finance manager records a deposit

- **WHEN** a finance manager submits a deposit with a positive amount, an occurrence date, and an optional note
- **THEN** the transaction is appended to the account history
- **AND** the account balance increases by that amount

#### Scenario: Deposit with non-positive amount is rejected

- **WHEN** a finance manager submits a deposit with a zero or negative amount
- **THEN** the system rejects the request with a validation error

### Requirement: Recording a Charge

The system SHALL allow a finance manager to record a charge (deduction) on any member's account. A charge decreases the account balance.

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

The system SHALL expose a navigable link from a member's profile (for users with FINANCE:MANAGE authority) and from the main menu (for the account owner) to the corresponding financial account.

#### Scenario: Finance manager navigates from member detail to account

- **WHEN** a user with FINANCE:MANAGE authority views a member's detail page and follows the financial account link
- **THEN** the member's financial account opens

#### Scenario: Member navigates from main menu to own account

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** their own financial account opens
