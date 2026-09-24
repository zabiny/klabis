# ORIS Club Key Specification

## Purpose

Manages the secret club key issued by ORIS that Klabis needs for club-scoped ORIS operations (such as reading the club membership list): who may set or discard it, and the guarantee that it is never disclosed once supplied.

## Requirements

### Requirement: The Club Key Is Set Without Ever Being Readable

Reading the club's membership list out of ORIS requires a secret the club administrator obtains from ORIS. The system SHALL let a user with the synchronisation permission supply that secret, and SHALL tell anyone who may set it whether one is currently held. The system SHALL NOT disclose the secret itself to anyone, in full or in part, once it has been supplied — it can only be replaced, never read back.

#### Scenario: The administrator supplies the club key for the first time

- **WHEN** a user with the synchronisation permission supplies the club key
- **THEN** the system confirms a club key is now held
- **AND** the value they supplied is not shown back to them

#### Scenario: The held key is never disclosed

- **WHEN** any user looks at the club key setting
- **THEN** they are told only whether a key is held or not
- **AND** they are never shown the key itself, not even partially masked

#### Scenario: Replacing a key that is already held

- **GIVEN** a club key is already held
- **WHEN** a user with the synchronisation permission supplies a different one
- **THEN** the new key replaces the previous one
- **AND** the previous key is not shown at any point

#### Scenario: A user without the permission cannot set the key

- **WHEN** a user without the synchronisation permission views the settings
- **THEN** the action to supply a club key is not offered to them
- **AND** they are not told whether a key is held

#### Scenario: An empty key is refused

- **WHEN** a user with the synchronisation permission supplies a blank club key
- **THEN** the system refuses it and explains a value is required
- **AND** any previously held key remains in force

### Requirement: The Club Key Can Be Discarded

Because supplying a blank value is refused, discarding a key the club no longer wants used SHALL be a deliberate action of its own. A user with the synchronisation permission SHALL be able to discard the held key without restarting the application, leaving the system as though none had ever been supplied.

#### Scenario: The administrator discards the held key

- **GIVEN** a club key is held
- **WHEN** a user with the synchronisation permission discards it
- **THEN** the system reports that no club key is held
- **AND** work that needs the key stops running until a new one is supplied

#### Scenario: Discarding when no key is held

- **WHEN** a user with the synchronisation permission discards the club key while none is held
- **THEN** the system still reports that no club key is held
- **AND** nothing is reported as having gone wrong

#### Scenario: A user without the permission cannot discard the key

- **WHEN** a user without the synchronisation permission views the settings
- **THEN** the action to discard the club key is not offered to them

### Requirement: The Club Key Does Not Survive A Restart

The club key SHALL be held only for as long as the application is running. When the application restarts, the system SHALL behave as though no key had ever been supplied, so that the administrator is asked for it again.

#### Scenario: The key is forgotten on restart

- **GIVEN** a club key was supplied before the application restarted
- **WHEN** a user with the synchronisation permission looks at the club key setting afterwards
- **THEN** they are told no club key is held
- **AND** they can supply one again

### Requirement: Work Needing The Club Key Stops Clearly When It Is Missing

Anything that needs the club key SHALL refuse to run while no key is held, and SHALL say plainly that the club key has not been set rather than failing as though ORIS were at fault. This SHALL apply to every operation that needs the key, including ones added later.

#### Scenario: Reading club members without a key

- **GIVEN** no club key is held
- **WHEN** the system tries to read the club's membership list from ORIS
- **THEN** it does not contact ORIS
- **AND** it reports that the club key has not been set

#### Scenario: Work resumes once the key is supplied

- **GIVEN** work was refused because no club key was held
- **WHEN** a user with the synchronisation permission supplies a valid club key
- **THEN** that work runs normally from then on

#### Scenario: Reading data that does not need the key is unaffected

- **GIVEN** no club key is held
- **WHEN** the system reads information from ORIS that needs no club key
- **THEN** that information is read normally
