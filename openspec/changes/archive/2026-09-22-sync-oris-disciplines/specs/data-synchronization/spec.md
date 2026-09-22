# Spec Delta

## ADDED Requirements

### Requirement: Reference Data Is Discovered And Brought In Automatically

For a kind of entity that is external reference data with no single natural record for a user to name and bring in one at a time, the system SHALL discover every such record in the external system on its own and bring each one in automatically, without waiting for a user action.

#### Scenario: A reference-data record is discovered and brought in on its own

- **WHEN** the external system holds a reference-data record that Klabis does not yet have
- **THEN** the system creates the corresponding entity in Klabis and links it to the external record the next time it looks
- **AND** it is kept in step from that point on the same way as any other linked entity, with no user having brought it in explicitly
