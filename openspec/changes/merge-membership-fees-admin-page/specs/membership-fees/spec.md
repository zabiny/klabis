## ADDED Requirements

### Requirement: Single Administrative Page for Membership Fees

The system SHALL provide a single administrative page where a membership administrator can, in one place, see the currently active fee selection campaign, manage the fee tier catalog, and review past fee selection campaigns. This page SHALL be the membership administrator's entry point for all membership fee administration.

#### Scenario: Administrator opens the membership fees page

- **WHEN** a membership administrator opens the membership fees page
- **THEN** the page shows the tier catalog
- **AND** the page shows the active campaign when one exists
- **AND** the page shows the list of past campaigns

#### Scenario: Non-administrator cannot reach the membership fees administration page

- **WHEN** a member without membership administration permission attempts to open the membership fees administration page
- **THEN** the system does not offer the active-campaign and past-campaigns content to that member

### Requirement: Active Campaign Shown Inline on the Membership Fees Page

When a fee selection campaign is currently active, the membership fees page SHALL show that campaign's details inline, including its year, its voting deadline, the action to change the voting deadline, and the campaign's fee groups. When no campaign is active, the page SHALL NOT show an active-campaign section.

#### Scenario: Active campaign is shown inline

- **WHEN** a membership administrator opens the membership fees page while a campaign is active
- **THEN** the page shows the active campaign's year and voting deadline inline
- **AND** the page offers the action to change the voting deadline
- **AND** the page lists the active campaign's fee groups
- **AND** the administrator can open the detail of any of those fee groups

#### Scenario: No active campaign hides the section

- **WHEN** a membership administrator opens the membership fees page while no campaign is active
- **THEN** the page does not show an active-campaign section

### Requirement: Past Campaigns List Excludes the Active Campaign

The membership fees page SHALL list past (closed) fee selection campaigns, each navigable to its detail. The currently active campaign SHALL NOT appear in the past-campaigns list, because it is already presented inline as the active campaign.

#### Scenario: Past campaigns are listed and navigable

- **WHEN** a membership administrator opens the membership fees page
- **THEN** the page lists the past fee selection campaign years
- **AND** the administrator can open the detail of any past campaign

#### Scenario: Active campaign is not duplicated in the past list

- **WHEN** a campaign is currently active and the membership administrator opens the membership fees page
- **THEN** the active campaign's year does not appear in the past-campaigns list

#### Scenario: Returning from a campaign detail goes back to the membership fees page

- **WHEN** a membership administrator opens a past campaign's detail from the membership fees page and then navigates back
- **THEN** the administrator returns to the membership fees page
