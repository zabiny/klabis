## ADDED Requirements

### Requirement: Account Page Shows Owner Identity

Any page that displays a member's financial account SHALL show the identity of the account owner — first name, last name, and registration number — alongside the balance and transaction history. This applies both to a member viewing their own account and to a finance manager viewing any member's account, so that the viewer can always confirm whose account they are looking at before reading the balance or recording a transaction.

#### Scenario: Member viewing own account sees their own identity

- **WHEN** an authenticated member opens the "Finance" item in the main menu
- **THEN** the page header shows the member's first name, last name, and registration number
- **AND** the current balance and transaction history are shown as before

#### Scenario: Finance manager viewing another member's account sees that member's identity

- **WHEN** a user with FINANCE:MANAGE authority navigates from a member's detail page to that member's financial account
- **THEN** the page header shows the target member's first name, last name, and registration number
- **AND** the current balance and transaction history are shown as before

#### Scenario: Owner identity stays consistent with the account being viewed

- **WHEN** a finance manager navigates between two different members' account pages in the same session
- **THEN** each account page shows the identity of the member that account belongs to
- **AND** the identity shown matches the account whose balance and history are displayed on that page
