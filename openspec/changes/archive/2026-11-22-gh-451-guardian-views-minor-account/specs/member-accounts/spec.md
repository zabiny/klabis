## ADDED Requirements

### Requirement: Legal Guardian Views Their Minor Child's Account

The system SHALL allow a user who holds "Zobrazení financí člena" over a member to view that member's financial account: the owner's identity, the current balance and the full transaction history with the same paging, sorting, filtering and recorder information the owner sees. A legal guardian holds this permission over each of their minor children (see `legal-guardians`). The access is read-only.

#### Scenario: Guardian opens their child's account from the child's detail page

- **GIVEN** a member is the legal guardian of a minor
- **WHEN** the guardian opens the minor's detail page and follows the action to the financial account
- **THEN** the minor's financial account opens
- **AND** the page header shows the minor's first name, last name and registration number
- **AND** the current balance and transaction history are shown

#### Scenario: Guardian sees who recorded each transaction

- **WHEN** a legal guardian views their minor child's account
- **THEN** each transaction in the history shows the name of the user who recorded it, the same as for the account owner

#### Scenario: Guardian cannot record any transaction on the child's account

- **WHEN** a legal guardian without FINANCE:MANAGE views their minor child's account
- **THEN** no controls for depositing, charging, or reversing transactions are shown
- **AND** the unified transaction dialog is not offered

#### Scenario: Non-member guardian views their child's account

- **GIVEN** a non-member legal guardian logged in with their EXT login number
- **WHEN** the guardian opens their minor child's detail page and follows the action to the financial account
- **THEN** the minor's balance and transaction history are shown

#### Scenario: Account action is not offered on an unrelated minor's detail

- **WHEN** a legal guardian without FINANCE:MANAGE opens the detail page of a minor who is not their child
- **THEN** no action to open that minor's financial account is shown

### Requirement: Finance Page Switches Between Accounts the User May View

The "Finance" page SHALL offer an account switcher to a user who may view the accounts of their minor children. The switcher SHALL list the user's own account (if they have one) and the accounts of their minor children ordered by last name and first name. A member opens their own account by default; a user without an own account opens the first child's account by default. A user who can view only one account sees no switcher.

#### Scenario: Member guardian opens Finance and sees their own account first

- **GIVEN** a member is the legal guardian of two minors
- **WHEN** the member opens "Finance" from the main menu
- **THEN** their own account is shown
- **AND** an account switcher offers their own account and the accounts of both minors

#### Scenario: Member guardian switches to a child's account

- **GIVEN** a member guardian has "Finance" open
- **WHEN** the member selects one of their children in the account switcher
- **THEN** the page shows that child's identity, balance and transaction history

#### Scenario: Non-member guardian opens Finance

- **GIVEN** a non-member legal guardian of two minors
- **WHEN** the guardian opens "Finance" from the main menu
- **THEN** the account of the minor who comes first by last name and first name is shown
- **AND** the account switcher offers only the accounts of the two minors

#### Scenario: Non-member guardian of one child sees no switcher

- **GIVEN** a non-member legal guardian of a single minor
- **WHEN** the guardian opens "Finance" from the main menu
- **THEN** the minor's account is shown without an account switcher

#### Scenario: Member without minor children sees no switcher

- **WHEN** a member who is not a legal guardian of any minor opens "Finance"
- **THEN** their own account is shown without an account switcher

#### Scenario: Child disappears from the switcher after turning 18

- **GIVEN** a member guardian of one minor
- **WHEN** the minor turns 18 and the daily run has removed them from the legal guardian group
- **AND** the member opens "Finance"
- **THEN** their own account is shown without an account switcher

## MODIFIED Requirements

### Requirement: Member Sees Their Own Account and History

The system SHALL allow any authenticated member to view their own financial account: current balance and a paginated, filterable, sortable history of transactions. The member SHALL NOT see any other member's account, except the accounts of members over whom they hold "Zobrazení financí člena" (see "Legal Guardian Views Their Minor Child's Account").

#### Scenario: Member opens "Finance" from the main menu

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** the member sees their current balance prominently displayed
- **AND** a history of their transactions below it

#### Scenario: Member cannot view another member's account

- **WHEN** a member without FINANCE:MANAGE authorization who is not a legal guardian of the account owner attempts to view another member's account
- **THEN** the system denies access

#### Scenario: Member cannot record any transaction on their own account

- **WHEN** a member without FINANCE:MANAGE authorization views their own account
- **THEN** no controls for depositing, charging, or reversing transactions are shown

### Requirement: Account Endpoint Is Reachable from the Member Detail

The system SHALL expose a navigable link to a member's financial account from the member detail page (for users with FINANCE:MANAGE authority and for users who hold "Zobrazení financí člena" over that member) and from the main menu (for the account owner and, through the account switcher, for legal guardians). The member list SHALL NOT offer navigation to the full account page; it offers only the inline transaction dialog.

#### Scenario: Finance manager navigates from member detail to account

- **WHEN** a user with FINANCE:MANAGE authority views a member's detail page and follows the financial account link
- **THEN** the member's financial account opens

#### Scenario: Legal guardian navigates from the child's detail to the child's account

- **WHEN** a legal guardian views their minor child's detail page and follows the financial account link
- **THEN** the child's financial account opens

#### Scenario: Member navigates from main menu to own account

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** their own financial account opens

### Requirement: Account Page Shows Owner Identity

Any page that displays a member's financial account SHALL show the identity of the account owner — first name, last name, and registration number — alongside the balance and transaction history. This applies to a member viewing their own account, to a legal guardian viewing their minor child's account, and to a finance manager viewing any member's account, so that the viewer can always confirm whose account they are looking at before reading the balance or recording a transaction. The page SHALL use "Finance" as its main heading (H1), consistent with the corresponding main menu item.

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

#### Scenario: Legal guardian viewing their child's account sees the child's identity

- **WHEN** a legal guardian opens their minor child's financial account
- **THEN** the page header shows the child's first name, last name, and registration number
- **AND** the page's main heading reads "Finance"

#### Scenario: Owner identity stays consistent with the account being viewed

- **WHEN** a finance manager navigates between two different members' account pages in the same session
- **THEN** each account page shows the identity of the member that account belongs to
- **AND** the identity shown matches the account whose balance and history are displayed on that page
