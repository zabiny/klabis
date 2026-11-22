## MODIFIED Requirements

### Requirement: Main Menu Offers "Finance" Entry for Every Authenticated Member

The system SHALL include a "Finance" entry in the main menu for every authenticated member and for every non-member legal guardian who has at least one minor child. Following the entry SHALL open the current user's own financial account; a non-member legal guardian, who has no own account, gets the account of their first minor child instead (see the account switcher in `member-accounts`). The entry SHALL appear in the everyday (main) section of the navigation, not in the administrative section.

#### Scenario: Authenticated member sees the Finance entry

- **WHEN** an authenticated member opens the application
- **THEN** the main menu shows a "Finance" entry

#### Scenario: Following Finance opens own account

- **WHEN** an authenticated member follows the "Finance" entry from the main menu
- **THEN** their own financial account opens

#### Scenario: Non-member guardian sees the Finance entry

- **GIVEN** a non-member legal guardian with at least one minor child
- **WHEN** the guardian opens the application
- **THEN** the main menu shows a "Finance" entry

#### Scenario: Non-member guardian follows Finance

- **GIVEN** a non-member legal guardian with at least one minor child
- **WHEN** the guardian follows the "Finance" entry from the main menu
- **THEN** the financial account of their first minor child opens

#### Scenario: Non-member without minor children has no Finance entry

- **GIVEN** a non-member legal guardian whose only child has turned 18 and left the legal guardian group
- **WHEN** the user opens the application
- **THEN** the main menu shows no "Finance" entry

#### Scenario: Finance entry is part of the everyday section on desktop

- **WHEN** a user views the desktop sidebar
- **THEN** the "Finance" entry appears in the main everyday section
- **AND** does not appear in the Administrace section

#### Scenario: Finance entry appears in mobile bottom navigation

- **WHEN** a user views the application on a mobile device
- **THEN** the bottom navigation includes the "Finance" entry alongside the other everyday destinations
