## ADDED Requirements

### Requirement: The Club Key Is Managed In The Synchronisation Settings

The club key setting SHALL be reached through the "Nastavení synchronizace" tab of the synchronisation overview, in a section "Klíč klubu ORIS" that shows whether a key is held and offers the actions to supply and discard it. The club key SHALL NOT be offered as an entry of the main menu.

#### Scenario: Sync manager finds the club key in the synchronisation settings

- **WHEN** a user with the synchronisation permission opens "Synchronizace" and selects "Nastavení synchronizace"
- **THEN** they see the section "Klíč klubu ORIS" telling them whether a club key is held
- **AND** they are offered the actions to supply a club key and to discard the held one

#### Scenario: Supplying the key from the settings tab

- **GIVEN** no club key is held
- **WHEN** a sync manager supplies a club key in the "Klíč klubu ORIS" section
- **THEN** the section reports that a club key is held
- **AND** the sync manager stays on the "Nastavení synchronizace" tab

#### Scenario: The club key is not in the main menu

- **WHEN** a user with the synchronisation permission looks at the main menu
- **THEN** no entry for the club key is shown, neither under its own name nor under a technical identifier
