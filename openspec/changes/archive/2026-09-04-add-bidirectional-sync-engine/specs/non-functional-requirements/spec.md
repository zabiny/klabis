# Non-Functional Requirements Specification

## ADDED Requirements

### Requirement: Synchronisation Data Protection At Rest

The stored copies of the data exchanged with an external system SHALL be encrypted at rest, using the same encryption applied to other sensitive stored data. The values used to detect change SHALL be digests of a complete exchanged record, never of an individual field, so that a stored digest cannot be used to recover a single value from a small range of possibilities.

The synchronisation history SHALL NOT store copies of the exchanged data, so that the data exists in one place only.

#### Scenario: Stored copies of exchanged data are unreadable in the database

- **WHEN** the copies of the data exchanged with an external system are stored
- **THEN** they are encrypted at rest and cannot be read directly from the database

#### Scenario: Change detection uses whole-record digests

- **WHEN** the system records what the two sides looked like
- **THEN** the digest it keeps covers the complete exchanged record
- **AND** no digest of an individual field is stored

#### Scenario: Stored copies can be rebuilt

- **GIVEN** stored copies of exchanged data that can no longer be read
- **WHEN** the affected records are synchronised again
- **THEN** the copies are rebuilt from both sides
- **AND** no data is lost, because these copies are derived rather than authoritative

### Requirement: Synchronisation History Retention

Entries in the synchronisation history SHALL be removed automatically once they are older than a configurable retention period, with a default of 30 days. Removing history SHALL never remove a synchronisation record itself, so that the last successful synchronisation of a long-lived record stays available after its individual attempts have been removed.

#### Scenario: Old history entries are removed

- **WHEN** entries in the synchronisation history are older than the configured retention period
- **THEN** they are removed automatically

#### Scenario: Removing history keeps the record

- **WHEN** all history entries of a synchronisation record have been removed by retention
- **THEN** the record itself remains
- **AND** its last successful synchronisation information is still available

### Requirement: Synchronisation Operational Configuration

The operational limits of synchronisation SHALL be configurable rather than fixed in code, with defaults suitable for a club-sized installation:

- the number of consecutive failures after which a record stops being attempted (default 5),
- the delay before the first retry, the growth of that delay per further failure, and its upper limit (defaults 15 minutes, doubling, 24 hours),
- how long one synchronisation run holds a record before another run may take it (default 5 minutes),
- how often the system looks for records that changed locally or are due for a retry (default every 15 minutes),
- when the full comparison of every active record runs (default nightly),
- how long synchronisation history is kept (default 30 days).

#### Scenario: Limits are adjusted without a code change

- **WHEN** an operator changes the number of consecutive failures allowed, a retry delay, a scan cadence or the retention period in configuration
- **THEN** synchronisation follows the new values after a restart
- **AND** no code change is required

#### Scenario: Defaults apply when nothing is configured

- **WHEN** no synchronisation configuration is provided
- **THEN** the documented defaults apply

### Requirement: One Synchronisation At A Time Per Record

A synchronisation record SHALL be processed by only one synchronisation run at a time, so that a scheduled run and a run triggered by a user cannot act on the same record simultaneously. A hold placed on a record SHALL expire, so that a run interrupted by a restart does not block that record permanently. Records other than the held one SHALL remain available to other runs.

#### Scenario: A user-triggered run does not collide with a scheduled one

- **GIVEN** a scheduled synchronisation run is processing a record
- **WHEN** a user asks for the same record to be synchronised
- **THEN** the record is not processed twice at the same time

#### Scenario: A user-triggered run proceeds for other records

- **GIVEN** a scheduled synchronisation run is processing one record
- **WHEN** a user asks for a different record to be synchronised
- **THEN** that request is processed without waiting for the scheduled run to finish

#### Scenario: An interrupted run does not block a record permanently

- **GIVEN** a synchronisation run held a record and was interrupted by a restart
- **WHEN** the hold expires
- **THEN** the record is processed again by the next run
