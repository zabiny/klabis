# Spec Delta

## ADDED Requirements

### Requirement: Main Menu Offers a "Synchronizace" Entry for Sync Managers

The system SHALL include a "Synchronizace" entry in the Administrace section of the main menu for users with the synchronisation permission. Following the entry SHALL open the synchronisation overview page. Users without the permission SHALL NOT see the entry.

#### Scenario: Sync manager sees the Synchronizace entry in Administrace

- **WHEN** a user with the synchronisation permission views the desktop sidebar
- **THEN** the Administrace section shows a "Synchronizace" entry

#### Scenario: Following the entry opens the synchronisation overview

- **WHEN** a sync manager follows the "Synchronizace" entry
- **THEN** the synchronisation overview page opens

#### Scenario: User without the permission does not see the entry

- **WHEN** a user without the synchronisation permission views the main menu
- **THEN** the "Synchronizace" entry is not shown

#### Scenario: Synchronizace entry is not in the mobile bottom navigation

- **WHEN** a sync manager uses the application on a mobile device
- **THEN** the "Synchronizace" entry is not shown in the bottom navigation, like the other administrative destinations
