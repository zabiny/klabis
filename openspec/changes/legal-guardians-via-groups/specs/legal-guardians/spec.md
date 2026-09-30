## ADDED Requirements

### Requirement: Legal Guardians Of A Minor

The system SHALL track the legal guardians of a minor member exclusively through legal guardian groups. A legal guardian is any user of the system: either an adult club member or a non-member legal guardian with their own profile. A minor MAY have any number of legal guardians (at least one for a complete record). A legal guardian MAY represent minors in several legal guardian groups. Guardians are shown only while the member is a minor; once the member turns 18 no legal guardians are shown for them.

#### Scenario: Minor with two guardians from different households

- **GIVEN** a minor whose legal guardians are a club member and a non-member
- **WHEN** an admin opens the minor's detail page
- **THEN** both legal guardians are listed with their name, e-mail and telephone

#### Scenario: One guardian represents children from two different groups

- **GIVEN** a parent who is the sole guardian of one child and, together with another person, guardian of a second child
- **WHEN** an admin views both children
- **THEN** the parent is listed as a legal guardian of each child

#### Scenario: Adult member shows no legal guardians

- **GIVEN** a member who has turned 18
- **WHEN** an admin opens the member's detail page
- **THEN** no legal guardians are shown for the member

### Requirement: Legal Guardian Groups

The system SHALL group minors who share exactly the same set of legal guardians into one legal guardian group. Two legal guardian groups SHALL never have the same set of guardians, a minor SHALL belong to at most one group, and only minors SHALL be members of a group. The group's name SHALL be derived automatically from the guardians' surnames in alphabetical order, joined by " a ", and SHALL follow every change of guardians. Groups are created, merged and removed automatically as guardians of minors change; they cannot be created or deleted by hand.

#### Scenario: Siblings with the same guardians share a group

- **GIVEN** a legal guardian group with guardians "Jan Novák" and "Eva Svobodová" and one child
- **WHEN** an admin registers the child's sibling with the same two guardians
- **THEN** the sibling joins the existing group
- **AND** no new group is created

#### Scenario: Half-siblings get separate groups

- **GIVEN** a child whose guardians are "Jan Novák" and "Eva Svobodová"
- **WHEN** an admin registers a half-sibling whose only guardian is "Jan Novák"
- **THEN** the half-sibling is placed in a separate group whose only guardian is "Jan Novák"

#### Scenario: Group name follows the guardians

- **WHEN** an admin views a group whose guardians are "Eva Svobodová" and "Jan Novák"
- **THEN** the group is named "Novák a Svobodová"

### Requirement: Setting The Legal Guardians Of A Minor

The system SHALL let a user with MEMBERS:MANAGE permission set the complete list of legal guardians of a minor from the minor's detail page. Each guardian in the list is either chosen from the legal guardian candidates or entered as a new non-member legal guardian (first name, last name, e-mail and telephone). The list SHALL NOT be empty. Changing the guardians of one minor SHALL NOT change the guardians of that minor's siblings.

#### Scenario: Admin adds a second guardian to a minor

- **GIVEN** a minor whose only guardian is "Jan Novák"
- **WHEN** an admin opens "Upravit zástupce" on the minor's detail page, adds "Eva Svobodová" and saves
- **THEN** the minor's detail lists both guardians

#### Scenario: Adding a guardian to one sibling does not affect the other

- **GIVEN** two siblings sharing the guardians "Jan Novák" and "Eva Svobodová"
- **WHEN** an admin adds "Petr Dvořák" as guardian of only one sibling
- **THEN** that sibling lists three guardians
- **AND** the other sibling still lists only "Jan Novák" and "Eva Svobodová"

#### Scenario: Minor joins the group that already has the new set of guardians

- **GIVEN** a minor whose only guardian is "Jan Novák"
- **AND** a group whose guardians are "Jan Novák" and "Eva Svobodová"
- **WHEN** an admin sets "Jan Novák" and "Eva Svobodová" as the minor's guardians
- **THEN** the minor becomes a member of that existing group
- **AND** the minor's previous group disappears if no child remains in it

#### Scenario: Admin enters a new non-member guardian

- **WHEN** an admin adds a new guardian with first name, last name, e-mail and telephone and saves
- **THEN** the new person is listed as a guardian of the minor
- **AND** the person can afterwards be chosen as a guardian of other minors

#### Scenario: Last guardian cannot be removed

- **GIVEN** a minor with a single legal guardian
- **WHEN** an admin removes the guardian and saves
- **THEN** the form shows an error that a minor must have at least one legal guardian
- **AND** no changes are saved

#### Scenario: New guardian without contact details is rejected

- **WHEN** an admin enters a new guardian without an e-mail or without a telephone and saves
- **THEN** the form shows an error that a legal guardian must have both e-mail and telephone
- **AND** no changes are saved

#### Scenario: New guardian with an e-mail already in use is rejected

- **WHEN** an admin enters a new guardian whose e-mail belongs to an existing legal guardian or to an adult member
- **THEN** the form shows an error that the e-mail is already in use and the existing person should be chosen from the list
- **AND** no changes are saved

#### Scenario: Guardian editing is not offered for adults

- **WHEN** an admin opens the detail page of a member who is 18 or older
- **THEN** no "Upravit zástupce" action is shown

#### Scenario: Guardian editing requires MEMBERS:MANAGE

- **WHEN** a user without MEMBERS:MANAGE permission opens a minor's detail page
- **THEN** no "Upravit zástupce" action is shown

### Requirement: Legal Guardian Candidates

The system SHALL offer as legal guardian candidates every non-member legal guardian and every active club member aged 18 or older. Candidates SHALL be searchable by name, and each candidate SHALL be shown with enough detail to tell namesakes apart: the registration number for members and the e-mail for non-members. Candidates are available only to users with MEMBERS:CREATE or MEMBERS:MANAGE permission.

#### Scenario: Admin searches for a guardian

- **WHEN** an admin types part of a name into the guardian picker
- **THEN** matching non-member guardians and adult active members are offered
- **AND** each member shows their registration number and each non-member shows their e-mail

#### Scenario: Minors and suspended members are not offered

- **WHEN** an admin opens the guardian picker
- **THEN** members younger than 18 and members whose membership has ended are not offered

### Requirement: Non-Member Legal Guardian Profile

The system SHALL keep a profile for every legal guardian who is not a club member, holding first name, last name, e-mail and telephone, and showing the guardian's login number. E-mail and telephone SHALL always be present. The profile is visible and editable by users with MEMBERS:MANAGE permission and by the guardian themself.

#### Scenario: Admin edits a guardian's telephone

- **WHEN** an admin opens a non-member guardian's profile, changes the telephone and saves
- **THEN** the new telephone is shown on the profile
- **AND** it is shown for this guardian on the detail of each of their minors

#### Scenario: Guardian edits their own profile

- **WHEN** a logged-in non-member guardian opens "Můj profil", changes their e-mail and saves
- **THEN** the new e-mail is saved
- **AND** they continue to log in with their login number

#### Scenario: Profile shows the login number

- **WHEN** an admin opens a non-member guardian's profile
- **THEN** the guardian's login number (for example EXT0001) is shown

#### Scenario: Guardian cannot clear required contact

- **WHEN** a guardian or an admin clears the e-mail or telephone on a guardian profile and saves
- **THEN** the form shows an error that e-mail and telephone are required
- **AND** no changes are saved

#### Scenario: Guardian profile is reachable from the minor's detail

- **WHEN** an admin clicks a non-member guardian on a minor's detail page
- **THEN** the guardian's profile opens
- **AND** clicking a guardian who is a member opens that member's detail instead

#### Scenario: Other users cannot see a guardian profile

- **WHEN** a user without MEMBERS:MANAGE permission, who is not the guardian, tries to open a guardian profile
- **THEN** the profile is not shown

### Requirement: Non-Member Legal Guardian Account

The system SHALL create a user account for every new non-member legal guardian. Its login number SHALL be assigned from a single club-wide series with the prefix EXT: the first guardian gets EXT0001, the second EXT0002, and so on; a number is never reused. The account awaits activation, and the guardian activates it themself with their login number and their own e-mail. A non-member guardian has only the permission to view club members; they do not yet have access to their minors' personal data.

#### Scenario: New guardian activates their account

- **GIVEN** a non-member guardian created during a minor's registration
- **WHEN** the guardian requests account activation with their login number and their e-mail address
- **THEN** an activation link is sent to their e-mail

#### Scenario: Guardians get consecutive login numbers

- **GIVEN** the last non-member guardian got the login number EXT0007
- **WHEN** an admin enters a new non-member guardian
- **THEN** the new guardian gets the login number EXT0008

#### Scenario: Guardian logs in with their login number

- **WHEN** an activated non-member guardian logs in with their login number and password
- **THEN** they are logged in to the application
- **AND** the menu offers the member list
- **AND** the home page offers "Můj profil" leading to their guardian profile

### Requirement: Legal Guardian Groups Page

The system SHALL provide a "Zákonní zástupci" page listing all legal guardian groups, available only to users with MEMBERS:MANAGE permission. The group detail shows the guardians and the minors of the group. An admin MAY set the complete list of guardians of the whole group; the change applies to all minors of the group and follows the same rules as setting the guardians of a single minor.

#### Scenario: Admin browses legal guardian groups

- **WHEN** an admin opens "Zákonní zástupci" in the Administrace section
- **THEN** the page lists every group with its name, guardians and number of minors

#### Scenario: Admin views a group detail

- **WHEN** an admin opens a group
- **THEN** the guardians and the minors of the group are listed
- **AND** each minor and each guardian can be opened from the list

#### Scenario: Admin adds a guardian to the whole group

- **GIVEN** a group with two siblings and guardians "Jan Novák" and "Eva Svobodová"
- **WHEN** an admin adds "Petr Dvořák" to the group's guardians and saves
- **THEN** both siblings list all three guardians

#### Scenario: Group change merges with an existing group

- **GIVEN** a group A with guardian "Jan Novák" and a group B with guardians "Jan Novák" and "Eva Svobodová"
- **WHEN** an admin adds "Eva Svobodová" to the guardians of group A
- **THEN** the minors of group A join group B
- **AND** group A disappears

#### Scenario: Groups cannot be created or deleted by hand

- **WHEN** an admin views the "Zákonní zástupci" page or a group detail
- **THEN** no action for creating or deleting a group is offered

#### Scenario: Page is hidden without MEMBERS:MANAGE

- **WHEN** a user without MEMBERS:MANAGE permission views the application navigation
- **THEN** the "Zákonní zástupci" item is not shown

### Requirement: Minors Leave Guardian Groups When They Turn 18

The system SHALL remove, once a day, every member who has turned 18 from their legal guardian group, and SHALL remove a group that no longer has any minor. The member's completeness is re-evaluated at the same time.

#### Scenario: Member turning 18 leaves the group

- **GIVEN** a minor in a legal guardian group with a sibling
- **WHEN** the minor turns 18 and the daily run has passed
- **THEN** the member is no longer listed in the group
- **AND** the sibling stays in the group

#### Scenario: Group without minors disappears

- **GIVEN** a legal guardian group with a single minor
- **WHEN** the minor turns 18 and the daily run has passed
- **THEN** the group is no longer listed on the "Zákonní zástupci" page

#### Scenario: Adult without own contact becomes incomplete

- **GIVEN** a minor without an own e-mail whose guardian has an e-mail
- **WHEN** the minor turns 18 and the daily run has passed
- **THEN** the member is marked "Neúplné údaje" with a missing e-mail

### Requirement: Notifications About A Minor Reach Their Guardians

Any e-mail notification the system sends about a minor member SHALL be sent to all of the minor's legal guardians and, if the minor has an own e-mail, also to the minor. Notifications about an adult member SHALL be sent only to the member's own e-mail.

#### Scenario: Notification about a minor with two guardians

- **GIVEN** a minor with an own e-mail and two legal guardians
- **WHEN** the system sends a notification about the minor
- **THEN** the notification is sent to both guardians and to the minor

#### Scenario: Notification about an adult

- **WHEN** the system sends a notification about an adult member
- **THEN** the notification is sent only to the member's own e-mail
