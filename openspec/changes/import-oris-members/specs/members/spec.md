## MODIFIED Requirements

### Requirement: Registration Number Generation

The system SHALL automatically assign a unique registration number in format XXXYYSS (club code + 2-digit birth year + 2-digit sequence), except where the member is registered with a registration number already issued elsewhere — as happens for a member brought in from ORIS, which issues the number identifying them across the whole sport. In that case the system SHALL adopt the number supplied and SHALL NOT assign one of its own.

#### Scenario: Registration number assigned on first member for a birth year

- **WHEN** the first member born in 2001 is registered
- **THEN** their registration number ends with 0100 (e.g., ZBM0100 for club ZBM)

#### Scenario: Subsequent members born in the same year get incremented numbers

- **WHEN** additional members born in 2001 are registered
- **THEN** each receives the next sequence number (e.g., ZBM0101, ZBM0102)

#### Scenario: Each birth year has its own sequence

- **WHEN** a member born in 2005 is registered after a member born in 2001
- **THEN** the new member's registration number starts a fresh sequence for 2005 (e.g., ZBM0500)

#### Scenario: A registration number issued elsewhere is adopted

- **WHEN** a member is registered with a registration number already issued to them elsewhere
- **THEN** they carry that registration number
- **AND** no number is assigned from the club's own sequence

#### Scenario: A registration number already in use is refused

- **WHEN** a member is registered with a registration number another member already carries
- **THEN** the registration is refused
- **AND** no member is created
