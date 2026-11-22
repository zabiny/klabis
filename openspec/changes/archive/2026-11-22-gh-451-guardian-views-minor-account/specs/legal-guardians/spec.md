## ADDED Requirements

### Requirement: Legal Guardians View Their Minors' Financial Accounts

A legal guardian group SHALL always delegate "Zobrazení financí člena" to its guardians over its minors, in addition to "Úprava údajů člena"; this cannot be changed. A legal guardian therefore sees the financial account of each of their minor children, read-only. The guardian loses this permission over a child when the child leaves the group after turning 18, or when the guardian stops being the child's legal guardian.

#### Scenario: Guardian sees their child's balance

- **GIVEN** a member is the legal guardian of a minor
- **WHEN** the guardian opens the minor's financial account
- **THEN** the minor's current balance and transaction history are shown

#### Scenario: Non-member guardian sees their child's balance

- **GIVEN** a non-member legal guardian logged in with their EXT login number
- **WHEN** the guardian opens their minor child's financial account
- **THEN** the minor's current balance and transaction history are shown

#### Scenario: Guardian of siblings sees both accounts

- **GIVEN** a legal guardian group with two minor siblings
- **WHEN** a guardian of the group opens "Finance"
- **THEN** the guardian can view the financial accounts of both siblings

#### Scenario: Guardian loses access when the child turns 18

- **GIVEN** a legal guardian of a minor
- **WHEN** the minor turns 18 and the daily run has removed them from the legal guardian group
- **THEN** the former guardian can no longer view the member's financial account

#### Scenario: Removed guardian loses access

- **GIVEN** a legal guardian of a minor
- **WHEN** an administrator changes the minor's legal guardians so that this person is no longer among them
- **THEN** that person can no longer view the minor's financial account
