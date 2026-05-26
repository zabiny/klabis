## ADDED Requirements

### Requirement: Main Menu Offers "Finance" Entry for Every Authenticated Member

The system SHALL include a "Finance" entry in the main menu for every authenticated member. Following the entry SHALL open the current user's own financial account. The entry SHALL appear in the everyday (main) section of the navigation, not in the administrative section.

#### Scenario: Authenticated member sees the Finance entry

- **WHEN** an authenticated member opens the application
- **THEN** the main menu shows a "Finance" entry

#### Scenario: Following Finance opens own account

- **WHEN** an authenticated member follows the "Finance" entry from the main menu
- **THEN** their own financial account opens

#### Scenario: Finance entry is part of the everyday section on desktop

- **WHEN** a user views the desktop sidebar
- **THEN** the "Finance" entry appears in the main everyday section
- **AND** does not appear in the Administrace section

#### Scenario: Finance entry appears in mobile bottom navigation

- **WHEN** a user views the application on a mobile device
- **THEN** the bottom navigation includes the "Finance" entry alongside the other everyday destinations
