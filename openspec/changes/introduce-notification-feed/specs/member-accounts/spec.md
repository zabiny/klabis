# Spec Delta

## ADDED Requirements

### Requirement: Member Is Notified When The Balance Turns Negative

When a member's balance drops from zero or above to below zero, the system SHALL send the member a "Záporný zůstatek" notification once, whatever caused the drop. Further charges while the balance stays negative SHALL NOT send another notification; a new drop after the balance returned to zero or above SHALL. The feed and e-mail SHALL state the new balance; the push notification SHALL NOT. Finance managers SHALL NOT be notified.

#### Scenario: Registration payment drops the balance below zero

- **GIVEN** a member with a balance of 100 CZK
- **WHEN** the member pays an event registration of 150 CZK from the account
- **THEN** the member receives a "Záporný zůstatek" notification stating the balance of -50 CZK
- **AND** clicking it opens the member's financial account

#### Scenario: Yearly membership fee drops the balance below zero

- **GIVEN** a member with a balance of 0 CZK
- **WHEN** the system charges the member the yearly membership fee
- **THEN** the member receives a "Záporný zůstatek" notification

#### Scenario: Finance manager's charge or reversal drops the balance below zero

- **GIVEN** a member with a balance of 200 CZK
- **WHEN** a finance manager reverses a deposit of 300 CZK
- **THEN** the member receives a "Záporný zůstatek" notification stating the balance of -100 CZK

#### Scenario: Further charge while already negative

- **GIVEN** a member with a balance of -50 CZK
- **WHEN** a finance manager records a charge of 100 CZK
- **THEN** no new notification is sent

#### Scenario: Balance becomes negative again after a deposit

- **GIVEN** a member who was notified about a negative balance and then deposited enough to reach 20 CZK
- **WHEN** a charge of 50 CZK drops the balance to -30 CZK
- **THEN** the member receives a new "Záporný zůstatek" notification

#### Scenario: Balance exactly zero is not negative

- **GIVEN** a member with a balance of 100 CZK
- **WHEN** a charge of 100 CZK drops the balance to 0 CZK
- **THEN** no notification is sent

#### Scenario: Push notification does not reveal the amount

- **GIVEN** a member with push switched on for negative balance notifications
- **WHEN** the member's balance drops below zero
- **THEN** the push notification says that the balance is negative without stating the amount
