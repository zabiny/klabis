## ADDED Requirements

### Requirement: Administrator Can Manually Close an Active Campaign

The system SHALL allow a membership administrator to immediately close an active fee selection campaign before its voting deadline. Closing the campaign SHALL trigger the same processing as the automatic end-of-deadline flow: fee groups are frozen, yearly membership fee charges are generated for members with a choice, and members who have not made a choice receive a missed-selection sanction. The voting deadline is NOT changed.

The close action SHALL only be available while the campaign is active and has not yet been processed.

#### Scenario: Administrator closes an active campaign

- **WHEN** a membership administrator opens the detail of an active fee selection campaign
- **THEN** the page offers an action to close the campaign

#### Scenario: Campaign is closed immediately

- **WHEN** a membership administrator confirms closing the campaign
- **THEN** the campaign is closed immediately
- **AND** yearly fee charges are generated for members who had made a choice
- **AND** members who had not made a choice receive a missed-selection sanction
- **AND** the campaign no longer appears as active

#### Scenario: Close action is not available on an already closed campaign

- **WHEN** a membership administrator opens the detail of an already closed (processed) campaign
- **THEN** the page does NOT offer the action to close the campaign

#### Scenario: Close action requires MEMBERS:MANAGE authority

- **WHEN** a user without MEMBERS:MANAGE authority opens the campaign detail
- **THEN** the page does NOT offer the action to close the campaign
