# Spec Delta

## MODIFIED Requirements

### Requirement: Main Menu Offers a "Členské příspěvky" Entry for Membership Administrators

The system SHALL include a single "Členské příspěvky" entry in the Administrace section of the main menu for members authorized to administer membership fees. Following the entry SHALL open the membership fees administration page. The system SHALL NOT present membership fee types and fee selection campaigns as two separate menu entries.

#### Scenario: Membership administrator sees one membership fees entry in Administrace

- **WHEN** a member with membership administration authorization views the desktop sidebar
- **THEN** the Administrace section shows a single "Členské příspěvky" entry
- **AND** no separate fee type catalog or fee selection campaigns entries are shown

#### Scenario: Following the entry opens the membership fees page

- **WHEN** a membership administrator follows the "Členské příspěvky" entry
- **THEN** the membership fees administration page opens

#### Scenario: Member without membership administration does not see the entry

- **WHEN** a member without membership administration authorization views the main menu
- **THEN** the "Členské příspěvky" entry is not shown
