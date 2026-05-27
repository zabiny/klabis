## ADDED Requirements

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

## MODIFIED Requirements

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
