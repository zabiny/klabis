## MODIFIED Requirements

### Requirement: Membership Suspension

The system SHALL allow users with MEMBERS:UPDATE permission to suspend a member's membership. Before suspension, the system SHALL check group ownership and the state of the member's financial account.

#### Scenario: Admin suspends an active member

- **WHEN** user with MEMBERS:UPDATE permission submits the suspension form with a valid reason (ODHLASKA, PRESTUP, OTHER)
- **AND** the member is not the last owner of any user group
- **AND** the member's financial account balance is zero or positive
- **THEN** the membership is suspended immediately
- **AND** the member detail page reflects the suspended status

#### Scenario: Suspension form requires a reason

- **WHEN** user submits the suspension form without selecting a reason
- **THEN** the form shows an error that reason is required

#### Scenario: Suspension blocked when member is last owner of a training group

- **WHEN** user attempts to suspend a member who is the sole owner of a training group
- **THEN** the system displays a warning listing the affected training groups
- **AND** requires the admin to designate a successor owner before suspension can proceed

#### Scenario: Suspension blocked when member is last owner of a family or free group

- **WHEN** user attempts to suspend a member who is the sole owner of a family or free group
- **THEN** the system displays a warning listing the affected groups
- **AND** requires the admin to either designate a successor or dissolve the group before suspension can proceed

#### Scenario: Suspension blocked when member has a negative financial account balance

- **WHEN** user attempts to suspend a member whose financial account balance is negative
- **THEN** the system displays a warning showing the current balance
- **AND** offers a link to open the member's financial account
- **AND** requires that the balance be brought to zero or positive before suspension can proceed

#### Scenario: Negative-balance suspension cannot be overridden in the dialog

- **WHEN** an admin is shown the negative-balance warning in the suspension dialog
- **THEN** no checkbox or override action is offered in the dialog to suspend regardless
- **AND** the admin must resolve the balance through a financial-account operation first

#### Scenario: Suspension proceeds after group ownership resolved

- **WHEN** admin resolves all group ownership conflicts
- **AND** the member's financial account balance is zero or positive
- **AND** resubmits the suspension request
- **THEN** the membership is suspended

#### Scenario: Suspension proceeds after negative balance is resolved

- **WHEN** the member's negative balance is brought to zero or positive (for example by recording a deposit)
- **AND** the admin resubmits the suspension request
- **THEN** the membership is suspended

#### Scenario: Suspended member detail shows suspension info

- **WHEN** admin views a suspended member's detail page
- **THEN** the suspension reason, date, and note (if provided) are displayed

#### Scenario: Unauthorized user cannot suspend

- **WHEN** user without MEMBERS:UPDATE permission views an active member's detail page
- **THEN** no "Ukončit členství" button is shown
