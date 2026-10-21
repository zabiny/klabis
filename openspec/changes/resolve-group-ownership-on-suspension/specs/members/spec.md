## MODIFIED Requirements

### Requirement: Membership Suspension

The system SHALL allow users with MEMBERS:MANAGE permission to suspend a member's membership. Before suspension, the system SHALL check group ownership and the state of the member's financial account. When the member is the last owner of one or more groups, the system SHALL let the user resolve each of those groups from the warning dialog itself, without leaving the suspension flow.

#### Scenario: Admin suspends an active member

- **WHEN** user with MEMBERS:MANAGE permission submits the suspension form with a valid reason (ODHLASKA, PRESTUP, OTHER)
- **AND** the member is not the last owner of any user group
- **AND** the member's financial account balance is zero or positive
- **THEN** the membership is suspended immediately
- **AND** the member detail page reflects the suspended status

#### Scenario: Suspension form requires a reason

- **WHEN** user submits the suspension form without selecting a reason
- **THEN** the form shows an error that reason is required

#### Scenario: Suspension blocked when member is last owner of a training group

- **WHEN** user attempts to suspend a member who is the sole trainer of a training group
- **THEN** the system displays a warning listing the affected training groups
- **AND** requires a successor trainer to be designated before suspension can proceed
- **AND** does not offer dissolving the training group

#### Scenario: Suspension blocked when member is last owner of a family or free group

- **WHEN** user attempts to suspend a member who is the sole owner of a free group
- **THEN** the system displays a warning listing the affected free groups
- **AND** requires the user to either designate a successor owner or dissolve the group before suspension can proceed

#### Scenario: Suspension blocked when member is the last legal guardian of a group

- **WHEN** user attempts to suspend a member who is the sole legal guardian of a group with minors
- **THEN** the system displays a warning listing the affected legal guardian groups
- **AND** offers "Pozastavit celou skupinu" and "Určit nástupce" for each such group
- **AND** does not offer dissolving the legal guardian group

#### Scenario: Group with several legal guardians does not block suspension

- **GIVEN** a legal guardian group has two guardians
- **WHEN** user suspends one of them
- **THEN** the suspension is not blocked because of that group

#### Scenario: Admin chooses to suspend the whole legal guardian group

- **GIVEN** the warning dialog lists a legal guardian group with minors "Anna Nováková" and "Petr Novák"
- **WHEN** the user clicks "Pozastavit celou skupinu" for that group
- **THEN** a confirmation lists the guardian and the minors that will be suspended together
- **AND** after the user confirms, the group is marked as resolved by suspending the whole group
- **AND** nothing is suspended yet

#### Scenario: Whole-group suspension can be cancelled

- **WHEN** the user clicks "Pozastavit celou skupinu" and cancels the confirmation
- **THEN** the group stays unresolved
- **WHEN** the user has already confirmed the whole-group suspension and clicks "Zrušit" on that group
- **THEN** the group becomes unresolved again

#### Scenario: Whole legal guardian group is suspended together

- **GIVEN** the user chose to suspend the whole legal guardian group and resolved all other groups in the dialog
- **WHEN** the user clicks "Ukončit členství" in the dialog
- **THEN** the guardian and every minor of the group are suspended
- **AND** all of them have the same reason and note as entered in the suspension form
- **AND** the member detail page reflects the suspended status

#### Scenario: Whole-group suspension is blocked by a minor's negative balance

- **GIVEN** the user chose to suspend the whole legal guardian group and one of its minors has a negative financial account balance
- **WHEN** the user clicks "Ukončit členství" in the dialog
- **THEN** nobody from the group is suspended
- **AND** the dialog tells which minor blocks the suspension and why

#### Scenario: Whole-group suspension is blocked by a minor being last owner of another group

- **GIVEN** the user chose to suspend the whole legal guardian group and one of its minors is the sole owner of a free group
- **WHEN** the user clicks "Ukončit členství" in the dialog
- **THEN** nobody from the group is suspended
- **AND** the dialog tells which minor blocks the suspension and in which group

#### Scenario: Legal guardian group is resolved by setting another guardian

- **GIVEN** the warning dialog lists a legal guardian group
- **WHEN** the user clicks "Určit nástupce" for that group
- **THEN** the user is taken to the group's page in "Zákonní zástupci", where the group's guardians can be set

#### Scenario: Admin designates a successor trainer from the warning dialog

- **GIVEN** the warning dialog lists a training group and the user holds GROUPS:TRAINING permission
- **WHEN** the user clicks "Určit nástupce" for that group and selects a member as the new trainer
- **THEN** the member becomes a trainer of the training group
- **AND** the group is marked as resolved in the dialog

#### Scenario: Admin designates a successor owner of a free group from the warning dialog

- **GIVEN** the warning dialog lists a free group that has other members
- **WHEN** the user clicks "Určit nástupce" for that group and selects one of the group's members
- **THEN** that member becomes an owner of the free group
- **AND** the group is marked as resolved in the dialog

#### Scenario: Free group without other members can only be dissolved

- **GIVEN** the warning dialog lists a free group whose only person is the member being suspended
- **THEN** "Určit nástupce" is not offered for that group
- **AND** "Rozpustit skupinu" is offered

#### Scenario: Admin dissolves a free group from the warning dialog

- **WHEN** the user clicks "Rozpustit skupinu" for a free group and confirms
- **THEN** the free group is removed together with its memberships
- **AND** the group is marked as resolved in the dialog

#### Scenario: Dissolving requires confirmation

- **WHEN** the user clicks "Rozpustit skupinu" for a free group and cancels the confirmation
- **THEN** the free group is not removed
- **AND** the group stays unresolved in the dialog

#### Scenario: Training group successor cannot be designated without GROUPS:TRAINING

- **GIVEN** the warning dialog lists a training group and the user does not hold GROUPS:TRAINING permission
- **THEN** no action is offered for that group
- **AND** the dialog tells the user that a training groups administrator has to designate the successor trainer

#### Scenario: Suspension blocked when member has a negative financial account balance

- **WHEN** user attempts to suspend a member whose financial account balance is negative
- **THEN** the system displays a warning showing the current balance
- **AND** offers a link to open the member's financial account
- **AND** requires that the balance be brought to zero or positive before suspension can proceed

#### Scenario: Negative-balance suspension cannot be overridden in the dialog

- **WHEN** an admin is shown the negative-balance warning in the suspension dialog
- **THEN** no checkbox or override action is offered in the dialog to suspend regardless
- **AND** the admin must resolve the balance through a financial-account operation first

#### Scenario: Several groups are resolved one by one

- **GIVEN** the warning dialog lists several groups
- **WHEN** the user resolves one of them
- **THEN** that group is shown as resolved while the others remain actionable

#### Scenario: Suspension is finished from the dialog once all groups are resolved

- **GIVEN** the user has resolved every group listed in the warning dialog
- **WHEN** the user clicks "Ukončit členství" in the dialog
- **THEN** the suspension is submitted with the reason and note entered earlier
- **AND** the membership is suspended
- **AND** the member detail page reflects the suspended status

#### Scenario: Suspension cannot be finished while a group is unresolved

- **GIVEN** at least one group listed in the warning dialog is still unresolved
- **THEN** "Ukončit členství" is not available in the dialog

#### Scenario: Suspension finished from the dialog is blocked again by a new blocker

- **GIVEN** the user has resolved every group listed in the warning dialog
- **WHEN** the user clicks "Ukončit členství" and the member has meanwhile become the last owner of another group or has a negative balance
- **THEN** the dialog shows the current blockers instead of suspending the member

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

- **WHEN** user without MEMBERS:MANAGE permission views an active member's detail page
- **THEN** no "Ukončit členství" button is shown
