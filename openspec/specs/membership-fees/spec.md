# Capability: Membership Fees

## Purpose

Allows membership administrators to maintain a catalog of fee levels, run fee selection campaigns for a calendar year, and manage member assignments. Members can choose their fee level before a voting deadline. The system enforces sanctions for missing choices and generates yearly membership fee charges automatically.

## Requirements

### Requirement: Membership Fee Type Catalog

The system SHALL allow a membership administrator to maintain a catalog of membership fee types. Each type defines a human-readable name, a yearly membership fee amount in CZK, and a set of membership payment rules. A type is created with its name and yearly fee; its membership payment rules are added afterwards on the type's detail page. The catalog is a working template that is reused when publishing types for a calendar year; editing the catalog SHALL NOT affect any type already published for a year.

#### Scenario: Administrator creates a fee level

- **WHEN** a membership administrator creates a fee type with a name and a yearly membership fee amount
- **THEN** the type is added to the catalog and is available for publishing
- **AND** the type has no membership payment rules until the administrator adds them on the type's detail page

#### Scenario: Administrator edits a catalog level

- **WHEN** a membership administrator edits the name, yearly fee, or rules of a catalog type
- **THEN** the catalog type reflects the changes
- **AND** types already published for a calendar year remain unchanged

#### Scenario: Non-administrator cannot manage the catalog

- **WHEN** a member without membership administration permission attempts to create or edit a catalog type
- **THEN** the system rejects the request as unauthorized

### Requirement: Membership Payment Rules by Event Type and Ranking

A membership fee type SHALL define membership payment rules that determine how much a member with that type contributes towards an event entry. Each rule applies to a combination of event type and event ranking. A rule expresses the member's contribution either as a percentage of the event's base entry price or as a fixed amount in CZK.

#### Scenario: Administrator defines a percentage rule

- **WHEN** a membership administrator adds a rule for a given event type and ranking expressed as a percentage
- **THEN** the rule is stored on the type as a percentage of the base entry price

#### Scenario: Administrator defines a fixed amount rule

- **WHEN** a membership administrator adds a rule for a given event type and ranking expressed as a fixed amount in CZK
- **THEN** the rule is stored on the type as a fixed amount

#### Scenario: Rule combination is unique within a level

- **WHEN** a membership administrator adds a rule for an event type and ranking combination that already has a rule on the same type
- **THEN** the system rejects the request as a duplicate rule
- **AND** the administrator sees a Czech message saying that the type already has a rule for that event type and ranking, naming the event type by its name and the ranking by its label
- **AND** the message contains no internal identifier of the event type

### Requirement: Starting a Fee Selection Campaign

The system SHALL allow a membership administrator to start a fee selection campaign for a specific calendar year by selecting a set of fee types and setting a voting deadline. The deadline SHALL be a future date. A new campaign MAY only be started when no other campaign is currently active. Starting a campaign SHALL create, for each selected type, a frozen-on-demand snapshot independent of the catalog.

#### Scenario: Administrator starts a fee selection campaign

- **WHEN** a membership administrator starts a campaign for a calendar year with a voting deadline in the future and a set of catalog types
- **THEN** each selected type becomes a published type for that year holding a complete snapshot of its yearly fee and rules
- **AND** members can choose among the published types for that year

#### Scenario: Voting deadline must be in the future

- **WHEN** a membership administrator attempts to start a campaign with a voting deadline set to today or a past date
- **THEN** the system rejects the request as invalid
- **AND** a Czech message saying that the voting deadline must be a future date is shown next to the voting deadline field
- **AND** the start campaign form stays open with the entered values

#### Scenario: Only one active campaign at a time

- **WHEN** a membership administrator attempts to start a new campaign while another campaign is still active
- **THEN** the system rejects the request
- **AND** the administrator must wait until the active campaign closes before starting a new one

#### Scenario: Voting deadline applies to the whole campaign

- **WHEN** a membership administrator sets the voting deadline while starting a campaign
- **THEN** the single deadline governs the choice window for every type published in that campaign

### Requirement: Changing the Voting Deadline of an Active Campaign

The system SHALL allow a membership administrator to change the voting deadline of a currently active campaign. The new deadline SHALL NOT be set to a past date.

#### Scenario: Administrator extends the voting deadline

- **WHEN** a membership administrator changes the voting deadline of an active campaign to a future date
- **THEN** the campaign's deadline is updated and the new deadline governs the choice window

#### Scenario: Administrator sets deadline to today

- **WHEN** a membership administrator changes the voting deadline of an active campaign to today's date
- **THEN** the campaign's deadline is updated to today

#### Scenario: Deadline cannot be moved to the past

- **WHEN** a membership administrator attempts to change the voting deadline of an active campaign to a date in the past
- **THEN** the system rejects the request as invalid

#### Scenario: Closed campaign deadline cannot be changed

- **WHEN** a membership administrator attempts to change the voting deadline of a campaign that is already closed
- **THEN** the system rejects the request

### Requirement: Emergency Assignment by Administrator

The system SHALL allow a membership administrator to assign or change a member's fee type for a year, including after the voting deadline. This emergency assignment overrides the locked state of a member's choice.

#### Scenario: Administrator assigns a level after the deadline

- **WHEN** a membership administrator assigns a published type to a member after the voting deadline
- **THEN** the member becomes assigned to that type for that year

#### Scenario: Administrator changes a member's level after the deadline

- **WHEN** a membership administrator changes a member's already assigned type after the voting deadline
- **THEN** the member's assignment moves to the newly selected type

### Requirement: Sanction for a Missing Choice

When a member has not chosen a fee type by the voting deadline, the system SHALL prevent that member from registering for new events and SHALL deregister that member from all events whose registrations are still open. The sanction takes effect the day after the voting deadline.

#### Scenario: Member who missed the choice is blocked and deregistered

- **WHEN** the voting deadline has passed and a member did not choose a type
- **THEN** the member is prevented from registering for new events
- **AND** the member is deregistered from all events that still have open registrations

#### Scenario: Member who chose a level is not sanctioned

- **WHEN** the voting deadline has passed and a member did choose a type before it
- **THEN** the member is not blocked and remains registered for their events

### Requirement: Lifting the Sanction by Emergency Assignment

When a membership administrator performs an emergency assignment for a member who was sanctioned for a missing choice, the system SHALL lift the registration block for that member. Previously deregistered registrations SHALL NOT be restored automatically.

#### Scenario: Emergency assignment unblocks future registrations

- **WHEN** a membership administrator assigns a type to a member who was blocked for a missing choice
- **THEN** the member is allowed to register for events again

#### Scenario: Deregistered registrations are not restored automatically

- **WHEN** a membership administrator assigns a type to a member who was deregistered for a missing choice
- **THEN** the previously cancelled registrations remain cancelled
- **AND** restoring them requires a manual re-registration

### Requirement: Editing a Published Type Until First Surcharge

The system SHALL allow a membership administrator to edit the yearly fee and membership payment rules of a published type only until the first surcharge has been calculated on the basis of that published type. Once the first surcharge has been calculated, the published type's snapshot is frozen and SHALL NOT be edited; corrections require publishing a new type.

#### Scenario: Administrator edits a published level before any surcharge

- **WHEN** a membership administrator edits the rules of a published type for which no surcharge has yet been calculated
- **THEN** the published type's snapshot reflects the changes

#### Scenario: Editing is rejected after the first surcharge

- **WHEN** a membership administrator attempts to edit a published type for which at least one surcharge has already been calculated
- **THEN** the system rejects the request because the snapshot is frozen

### Requirement: Generating the Yearly Membership Fee

The system SHALL post the yearly membership fee to each member's financial account based on the type the member is assigned to for the year. The yearly fee SHALL be posted the day after the voting deadline. The yearly fee for a given member and year SHALL be posted at most once.

#### Scenario: Yearly fee is posted the day after the deadline

- **WHEN** the day after the voting deadline arrives
- **THEN** each member assigned to a published type for that year has the type's yearly fee posted to their financial account

#### Scenario: Yearly fee is not posted twice

- **WHEN** the yearly fee generation runs again for a member and year that already received the yearly fee
- **THEN** no additional fee is posted for that member and year

#### Scenario: Member without an assigned level receives no yearly fee

- **WHEN** the yearly fee generation runs and a member has no assigned type for that year
- **THEN** no yearly fee is posted for that member

### Requirement: Single Administrative Page for Membership Fees

The system SHALL provide a single administrative page where a membership administrator can, in one place, see the currently active fee selection campaign, manage the fee type catalog, and review past fee selection campaigns. This page SHALL be the membership administrator's entry point for all membership fee administration.

#### Scenario: Administrator opens the membership fees page

- **WHEN** a membership administrator opens the membership fees page
- **THEN** the page shows the type catalog
- **AND** the page shows the active campaign when one exists
- **AND** the page shows the list of past campaigns

#### Scenario: Non-administrator cannot reach the membership fees administration page

- **WHEN** a member without membership administration permission attempts to open the membership fees administration page
- **THEN** the system does not offer the active-campaign and past-campaigns content to that member

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

### Requirement: Member Chooses a Fee Type for the Upcoming Year

The system SHALL allow each adult member, and the legal guardian of a minor member on the minor's behalf, to choose, change or withdraw one published membership fee type for the upcoming year in the "Členský příspěvek" section until the voting deadline. A minor never chooses for themself. After the deadline or once the campaign is closed the choice is locked. Nobody else - neither a membership administrator nor an owner of a group the member belongs to - can choose or withdraw a type.

#### Scenario: Member chooses a type

- **WHEN** a member selects one of the offered membership fee types in the "Členský příspěvek" section before the voting deadline
- **THEN** the member becomes assigned to that type for that year
- **AND** the member does not leave the "Členské příspěvky" page to make the choice

#### Scenario: Offered types show their yearly price

- **WHEN** a member opens the "Členské příspěvky" page while voting is open
- **THEN** every membership fee type offered in the "Členský příspěvek" section shows its name and its yearly price

#### Scenario: Member changes the choice before the deadline

- **WHEN** a member who already chose a type selects a different offered type before the voting deadline
- **THEN** the member's assignment moves to the newly selected type

#### Scenario: Member cannot change the choice after the deadline

- **WHEN** a member attempts to change their chosen type after the voting deadline
- **THEN** the system rejects the request as the voting is closed

#### Scenario: Previous year's type is offered as a default

- **WHEN** a member opens the "Členský příspěvek" section for the upcoming year and chose a type in the previous year that is also published for the upcoming year
- **THEN** the section pre-selects the previous year's type as a non-binding default
- **AND** the choice is recorded only after the member explicitly confirms it

#### Scenario: Member withdraws the choice before the deadline

- **WHEN** a member who chose a type for the upcoming year withdraws the choice in the "Členský příspěvek" section before the voting deadline
- **THEN** the member is no longer assigned to any type for that year
- **AND** the section prompts the member to choose a type again
- **AND** if the member does not choose a type before the voting deadline, the sanction for a missing choice applies

#### Scenario: Member withdraws a type assigned by an administrator

- **GIVEN** a membership administrator assigned a type to a member for the upcoming year
- **WHEN** the member withdraws that type in the "Členský příspěvek" section before the voting deadline
- **THEN** the member is no longer assigned to any type for that year

#### Scenario: Member cannot withdraw the choice after the deadline

- **WHEN** a member with an assigned type opens the "Členské příspěvky" page after the voting deadline while the campaign is still active
- **THEN** the section shows the type as locked
- **AND** the option to change or withdraw the choice is not offered

#### Scenario: Legal guardian chooses a type for their minor

- **GIVEN** voting is open and the minor has not chosen a type
- **WHEN** the minor's legal guardian opens the minor's "Členské příspěvky" page and selects one of the offered types in the "Členský příspěvek" section
- **THEN** the minor becomes assigned to that type for that year

#### Scenario: Non-member legal guardian chooses a type for their minor

- **GIVEN** voting is open and the legal guardian is not a club member
- **WHEN** the guardian selects an offered type for the minor on the minor's "Členské příspěvky" page
- **THEN** the minor becomes assigned to that type for that year

#### Scenario: Legal guardian withdraws or changes the minor's choice before the deadline

- **GIVEN** voting is open and the minor has a type for the upcoming year
- **WHEN** the minor's legal guardian changes the type or withdraws it
- **THEN** the minor's assignment moves to the new type or the minor is no longer assigned to any type

#### Scenario: Administrator cannot choose a type for a member

- **WHEN** a membership administrator who is not the member's legal guardian opens the member's "Členské příspěvky" page while voting is open
- **THEN** the page does not show the "Členský příspěvek" section

#### Scenario: Legal guardian cannot choose for a member who is not their minor

- **WHEN** a legal guardian attempts to choose a type for a member who is not their minor
- **THEN** the system rejects the request as forbidden

#### Scenario: Minor cannot choose a type for themself

- **GIVEN** voting is open
- **WHEN** a minor member opens their own profile
- **THEN** the "Členské příspěvky" button is not displayed
- **AND** the minor cannot choose, change or withdraw a type

#### Scenario: Free group owner cannot choose for a group member

- **GIVEN** voting is open and a user owns a free group that delegates "Úprava údajů člena"
- **WHEN** the owner opens the detail of a member of that group who is not their minor
- **THEN** the "Členské příspěvky" button is not displayed

### Requirement: Fee Type Section on the Member's Fees Page

The "Členské příspěvky" page of a member SHALL show a "Členský příspěvek" section above the history only while a fee selection campaign is active, and only to an adult member and to a legal guardian who may choose for them. The section shows the chosen type or a prompt to choose one and holds the controls for choosing, changing and withdrawing it. Outside an active campaign the chosen type is visible only in the history.

#### Scenario: Member sees the section with the assigned type during an active campaign

- **GIVEN** a fee selection campaign is active and voting is open
- **WHEN** a member who chose a type opens their profile and clicks "Členské příspěvky"
- **THEN** the page shows the "Členský příspěvek" section above the history with the member's chosen type
- **AND** the section offers to change or withdraw it

#### Scenario: Member with no chosen type sees a prompt to choose

- **GIVEN** a fee selection campaign is active and voting is open
- **WHEN** a member who has not chosen a type opens the "Členské příspěvky" page
- **THEN** the section prompts the member to choose one of the offered types

#### Scenario: Section is locked after the voting deadline

- **GIVEN** the voting deadline has passed and the campaign has not been closed yet
- **WHEN** a member who chose a type opens the "Členské příspěvky" page
- **THEN** the section shows the chosen type and indicates that the choice is locked

#### Scenario: Member who missed the deadline is told the voting is closed

- **GIVEN** the voting deadline has passed and the campaign has not been closed yet
- **WHEN** a member who did not choose a type opens the "Členské příspěvky" page
- **THEN** the section says that the voting is closed and no type was chosen

#### Scenario: Section is hidden when no campaign is active

- **WHEN** a member opens the "Členské příspěvky" page while no fee selection campaign is active
- **THEN** the page does not show the "Členský příspěvek" section
- **AND** the member's chosen type for past years is visible only in the history table

#### Scenario: Section is hidden after the campaign is closed

- **GIVEN** a member chose a type in a campaign that has since been closed
- **WHEN** the member opens the "Členské příspěvky" page
- **THEN** the page does not show the "Členský příspěvek" section
- **AND** the history shows the chosen type for the campaign year

#### Scenario: Legal guardian sees the section for their minor

- **GIVEN** a fee selection campaign is active and voting is open
- **WHEN** the minor's legal guardian opens the minor's "Členské příspěvky" page
- **THEN** the page shows the "Členský příspěvek" section with the minor's chosen type, or a prompt to choose one
- **AND** the section offers to choose, change or withdraw it

#### Scenario: Administrator without the right to choose does not see the section

- **GIVEN** a fee selection campaign is active
- **WHEN** a membership administrator who is neither the member nor their legal guardian opens the member's "Členské příspěvky" page
- **THEN** the page does not show the "Členský příspěvek" section
- **AND** the page shows the member's fee type history

### Requirement: Audit Trail of Fee Type Assignments

The system SHALL keep a record of the membership fee type each member was assigned in each year, and the "Členské příspěvky" page SHALL show it as a history with one entry per year, newest first. Each entry shows the year, the type name, the yearly price of the type as published for that year, and how the type was assigned. Only an adult member themself, a membership administrator and a legal guardian of the member may open the page; a minor member cannot open their own fee page.

#### Scenario: Member opens their fee type history from the profile

- **WHEN** a member opens their profile and clicks "Členské příspěvky"
- **THEN** the system shows the "Členské příspěvky" page with the member's fee type history
- **AND** the history lists every year in which the member had an assigned type, newest year first

#### Scenario: History entry shows year, type, price and how it was assigned

- **WHEN** a member opens the "Členské příspěvky" page for a year in which they chose a type themselves
- **THEN** the entry for that year shows the year and the type name
- **AND** the entry shows the yearly price the member pays for that type in that year
- **AND** the entry shows that the type was the member's own choice

#### Scenario: Price in the history is not changed by later catalog edits

- **GIVEN** a member was assigned a type in a past year
- **WHEN** a membership administrator later changes the yearly fee of that type in the catalog
- **THEN** the history entry for the past year still shows the price that was published for that year

#### Scenario: Type assigned by an administrator is marked as such

- **WHEN** a membership administrator assigned a member's type for a year, and the "Členské příspěvky" page is opened for that member
- **THEN** the entry for that year shows that the type was assigned by an administrator

#### Scenario: Past assignments remain available

- **GIVEN** a member was assigned a type in a fee selection campaign that is now closed
- **WHEN** a membership administrator opens that member's detail and clicks "Členské příspěvky"
- **THEN** the history shows the type and price the member was assigned in that year

#### Scenario: Administrator sees only the history of another member

- **WHEN** a membership administrator opens the "Členské příspěvky" page of another member
- **THEN** the page shows the member's fee type history
- **AND** the page does not show the "Členský příspěvek" section

#### Scenario: Legal guardian sees the fee type history of their minor

- **GIVEN** a minor was assigned a fee type in a past year
- **WHEN** the minor's legal guardian opens the minor's detail and clicks "Členské příspěvky"
- **THEN** the history shows the type and price the minor was assigned in that year

#### Scenario: Legal guardian cannot open the fee page of a member who is not their minor

- **WHEN** a legal guardian without membership administration permission opens the detail of a member they are not guardian of
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Year without an assignment is not listed

- **GIVEN** a member had no fee type in some year, for example because they missed the choice
- **WHEN** the member's "Členské příspěvky" page is opened
- **THEN** that year does not appear in the history

#### Scenario: Fee page of an inactive member is not offered

- **WHEN** a membership administrator opens the detail of an inactive member
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Member without any assignment sees an empty history

- **WHEN** a member who has never had a fee type assigned opens their "Členské příspěvky" page
- **THEN** the history says that the member has no fee type history yet

#### Scenario: Member cannot open another member's fee page

- **WHEN** a member without membership administration permission opens another member's detail
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Returning from the fee page

- **WHEN** a user on a member's "Členské příspěvky" page clicks the back link
- **THEN** the system shows the detail of that member again

#### Scenario: Minor cannot open their own fee page

- **WHEN** a minor member opens their own profile
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Free group owner cannot open a group member's fee page

- **GIVEN** a user owns a free group and has no membership administration permission
- **WHEN** the owner opens the detail of a member of that group who is not their minor
- **THEN** the "Členské příspěvky" button is not displayed
