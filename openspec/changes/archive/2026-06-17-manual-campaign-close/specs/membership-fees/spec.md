## MODIFIED Requirements

### Requirement: Active Campaign Shown Inline on the Membership Fees Page

When a fee selection campaign is currently active, the membership fees page SHALL show that campaign's details inline, including its year, its voting deadline, the action to change the voting deadline, the action to close the campaign, and the campaign's fee groups. When no campaign is active, the page SHALL NOT show an active-campaign section.

#### Scenario: Active campaign is shown inline

- **WHEN** a membership administrator opens the membership fees page while a campaign is active
- **THEN** the page shows the active campaign's year and voting deadline inline
- **AND** the page offers the action to change the voting deadline
- **AND** the page offers the action to close the campaign
- **AND** the page lists the active campaign's fee groups
- **AND** the administrator can open the detail of any of those fee groups

#### Scenario: No active campaign hides the section

- **WHEN** a membership administrator opens the membership fees page while no campaign is active
- **THEN** the page does not show an active-campaign section
