## ADDED Requirements

### Requirement: Synchronisation Overview Is Organised Into Tabs

The synchronisation overview SHALL be organised into tabs in this order: "Vyžaduje pozornost", "Všechny pokusy" and "Nastavení synchronizace". Opening the overview SHALL show "Vyžaduje pozornost". A tab SHALL be offered only when the user may see its content; a tab bar with a single tab SHALL NOT be shown.

#### Scenario: Sync manager lands on what needs attention

- **WHEN** a user with the synchronisation permission opens "Synchronizace" from the main menu
- **THEN** the overview shows the tabs "Vyžaduje pozornost", "Všechny pokusy" and "Nastavení synchronizace" in this order
- **AND** the "Vyžaduje pozornost" tab is selected

#### Scenario: Sync manager switches to the list of attempts

- **WHEN** a sync manager on the synchronisation overview selects "Všechny pokusy"
- **THEN** they see the list of recorded synchronisation attempts

#### Scenario: Selected tab survives a page reload

- **GIVEN** a sync manager has selected the "Nastavení synchronizace" tab
- **WHEN** they reload the page or share its address
- **THEN** the "Nastavení synchronizace" tab is selected again

#### Scenario: Settings tab is absent when there is nothing to set

- **WHEN** the application runs without the ORIS integration
- **THEN** the synchronisation overview does not offer the "Nastavení synchronizace" tab

## MODIFIED Requirements

### Requirement: Synchronisation Overview Lists Every Recorded Attempt

The system SHALL offer a user with the synchronisation permission a synchronisation overview page listing every recorded synchronisation attempt across all linked entities, newest first and in pages. Each attempt SHALL show when it ran, which entity it concerned, what triggered it, which direction it went, how it ended, why it failed if it did, and who performed it if a user did.

#### Scenario: Sync manager opens the synchronisation overview

- **WHEN** a user with the synchronisation permission opens "Synchronizace" from the main menu and selects "Všechny pokusy"
- **THEN** they see a list of recorded synchronisation attempts across members, events, disciplines and registrations
- **AND** the most recent attempt is shown first

#### Scenario: Each attempt shows what happened

- **WHEN** a sync manager looks at an attempt in the list
- **THEN** they see when it ran, the entity it concerned, whether it was started by schedule, by a change in Klabis or by a user, the direction it went and its outcome

#### Scenario: A failed attempt shows why it failed

- **WHEN** a sync manager looks at an attempt that failed
- **THEN** the reason of the failure is shown with it

#### Scenario: An attempt performed by a user shows who performed it

- **WHEN** a sync manager looks at an attempt that a user performed themselves, such as synchronising now, confirming a difference, resolving it or restarting a stopped record
- **THEN** the user who performed it is shown with it

#### Scenario: Older attempts are reached page by page

- **WHEN** there are more attempts than fit on one page
- **THEN** the sync manager can move to further pages to see older attempts

#### Scenario: Attempts removed by retention are no longer listed

- **WHEN** attempts become older than the configured history retention period
- **THEN** they no longer appear in the overview
