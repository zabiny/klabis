## ADDED Requirements

### Requirement: Event Ranking (Series)

An event SHALL carry a ranking attribute that identifies which competition series or ranking level the event belongs to (e.g. Regional Ranking, Ranking B, Czech Cup, Czech Championship). The ranking is a single value independent of the event type. The ranking SHALL be displayed by its short label and full Czech name on the event detail. An event MAY have no ranking (for example, manually created training events), in which case ranking is simply absent.

#### Scenario: Event detail shows the ranking

- **WHEN** a user views the detail of an event that has a ranking
- **THEN** the event detail shows the ranking with its short label and full name

#### Scenario: Event detail shows no ranking when absent

- **WHEN** a user views the detail of an event that has no ranking
- **THEN** the event detail shows no ranking value

### Requirement: Event Base Entry Fee

An event SHALL carry a base entry fee expressed as a monetary amount with its currency (typically Czech crowns). The base entry fee represents the entry fee of the event's main adult category and is used as the reference price for member contribution calculations. An event MAY have no base entry fee (for example, manually created events or events for which no fee is available), in which case the base entry fee is simply absent.

#### Scenario: Event detail shows the base entry fee

- **WHEN** a user views the detail of an event that has a base entry fee
- **THEN** the event detail shows the base entry fee amount together with its currency

#### Scenario: Event detail shows no fee when absent

- **WHEN** a user views the detail of an event that has no base entry fee
- **THEN** the event detail shows no base entry fee value

### Requirement: ORIS Import Includes Ranking And Base Entry Fee

The system SHALL import an event's ranking and base entry fee from ORIS, both when importing an event for the first time and when synchronizing an already imported event. The ranking is taken from the event's ranking level in ORIS. The base entry fee amount is derived as the highest entry fee among all of the event's categories in ORIS, which corresponds to the main adult category fee and disregards discounted or zero-fee category variants; its currency is taken from the event's currency in ORIS. When ORIS provides no ranking or no usable category fee, the corresponding attribute is left absent on the imported event.

#### Scenario: Import event with a ranking from ORIS

- **WHEN** an event manager imports an event from ORIS that has a ranking level
- **THEN** the imported event has the ranking set from the ORIS ranking level, including its short label and full name

#### Scenario: Import event with category fees from ORIS

- **WHEN** an event manager imports an event from ORIS whose categories have different entry fees
- **THEN** the imported event has the base entry fee amount set to the highest category fee
- **AND** the base entry fee currency set to the event's currency from ORIS

#### Scenario: Import event without a ranking from ORIS

- **WHEN** an event manager imports an event from ORIS that has no ranking level
- **THEN** the imported event has no ranking set

#### Scenario: Import event without usable category fees from ORIS

- **WHEN** an event manager imports an event from ORIS that has no category with a usable entry fee
- **THEN** the imported event has no base entry fee set

#### Scenario: Synchronizing refreshes ranking and fee from ORIS

- **WHEN** an event manager synchronizes an already imported event from ORIS
- **THEN** the event's ranking and base entry fee are updated to the current values from ORIS

### Requirement: Manual Editing Of Ranking And Base Entry Fee

An event manager SHALL be able to set or change an event's ranking and base entry fee manually when editing the event, including for events that were not imported from ORIS. A subsequent synchronization from ORIS overwrites manually entered values with the values from ORIS.

#### Scenario: Manager sets ranking and fee on a manually created event

- **WHEN** an event manager edits a manually created event and sets its ranking and base entry fee
- **THEN** the event detail shows the entered ranking and base entry fee

#### Scenario: Manager corrects an imported event's fee

- **WHEN** an event manager edits an imported event and changes its base entry fee
- **THEN** the event detail shows the corrected base entry fee
