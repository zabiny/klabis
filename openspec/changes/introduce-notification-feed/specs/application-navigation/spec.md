# Spec Delta

## ADDED Requirements

### Requirement: Main Menu Offers A "Nastavení" Entry To Every Logged-In User

The system SHALL include a "Nastavení" entry in the main section of the desktop sidebar for every logged-in user. Following the entry SHALL open the "Nastavení" page with the user's personal settings, organised into tabs; its first tab is "Nastavení notifikací". The entry SHALL NOT be shown in the mobile bottom navigation; on mobile devices the settings are reached from the "Notifikace" page.

#### Scenario: User sees the Nastavení entry

- **WHEN** a logged-in user views the desktop sidebar
- **THEN** the main section shows a "Nastavení" entry
- **AND** the Administrace section does not contain it

#### Scenario: Following the entry opens the notification settings tab

- **WHEN** a user follows the "Nastavení" entry
- **THEN** the "Nastavení" page opens with the "Nastavení notifikací" tab selected

#### Scenario: Non-member guardian sees the entry

- **WHEN** a logged-in non-member legal guardian views the desktop sidebar
- **THEN** the main section shows a "Nastavení" entry

#### Scenario: Nastavení is reached through the feed on mobile

- **WHEN** a user on a mobile device opens the "Notifikace" page from the bell and follows its settings link
- **THEN** the "Nastavení notifikací" tab opens
- **AND** the bottom navigation does not show a "Nastavení" entry
