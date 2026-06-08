## ADDED Requirements

### Requirement: Automatic Yearly Membership Fee Posting

The system SHALL accept an automatically generated yearly membership fee as a charge on a member's financial account. The generated charge SHALL appear in the account history like any other transaction and SHALL be recorded as system-generated. The amount is determined by the member's assigned membership fee level for the year.

#### Scenario: Yearly membership fee appears in the account history

- **WHEN** the yearly membership fee is generated for a member
- **THEN** a charge for the yearly fee amount is appended to the member's account history
- **AND** the account balance decreases by that amount

#### Scenario: Yearly membership fee is recorded as system-generated

- **WHEN** a member or finance manager reviews the account history containing a yearly membership fee
- **THEN** the entry is identifiable as an automatically generated yearly membership fee
