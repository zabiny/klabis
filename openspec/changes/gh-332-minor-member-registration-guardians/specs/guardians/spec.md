## Purpose

Covers legal guardians of minor club members: who can be a guardian, how guardians are entered and managed, how a guardian gets into the application, and how a minor's own account is started once the minor has their own e-mail address.

## ADDED Requirements

### Requirement: Legal Guardians of a Minor

The system SHALL allow a minor member to have any number of legal guardians, and SHALL require at least one guardian for every minor registered by hand. A guardian SHALL be either an existing club member, or a person who is not a club member, entered with first name, last name, relationship to the minor, e-mail address and phone number. Every guardian SHALL have an e-mail address and a phone number.

#### Scenario: Admin adds an existing club member as guardian

- **WHEN** admin filling in the guardians section picks an existing club member as a guardian and selects the relationship (parent or legal guardian)
- **THEN** the member is recorded as a guardian of the minor
- **AND** the guardian's name, e-mail and phone are taken from the member's profile

#### Scenario: Admin adds a guardian who is not a club member

- **WHEN** admin filling in the guardians section enters a new guardian with first name, last name, relationship, e-mail and phone
- **THEN** the person is recorded as a guardian of the minor
- **AND** a user account is created for the guardian, awaiting activation

#### Scenario: Admin adds two guardians

- **WHEN** admin adds a second guardian to a minor who already has one
- **THEN** both guardians are listed for the minor

#### Scenario: Guardian without e-mail or phone is rejected

- **WHEN** admin enters a new non-member guardian without an e-mail address or without a phone number
- **THEN** the form shows an error that the guardian's e-mail and phone are required
- **AND** the guardian is not added

#### Scenario: Existing member without contact details cannot become guardian

- **WHEN** admin picks as guardian a club member whose profile has no e-mail address or no phone number
- **THEN** the form shows an error that the guardian must have an e-mail and a phone
- **AND** the guardian is not added

#### Scenario: Same guardian entered for a sibling reuses the guardian

- **GIVEN** a non-member guardian with e-mail "novakova@example.com" is already a guardian of one child
- **WHEN** admin enters a new guardian with the e-mail "novakova@example.com" for another child
- **THEN** the system links the existing guardian to the second child
- **AND** no second user account is created for that e-mail address

#### Scenario: Same person cannot be added twice to one minor

- **WHEN** admin adds a guardian who is already a guardian of the same minor
- **THEN** the form shows an error that the person is already a guardian of this member

#### Scenario: Member cannot be their own guardian

- **WHEN** admin picks the minor themselves as their guardian
- **THEN** the form shows an error that a member cannot be their own guardian

### Requirement: Guardians on Member Detail

The system SHALL list all guardians of a member in the contact section of the member detail page, showing each guardian's name, relationship, e-mail and phone. A guardian who is a club member SHALL be shown with a link to their member profile.

#### Scenario: Member detail lists all guardians

- **WHEN** user who can see a minor's contact section opens the minor's detail page
- **THEN** every guardian is listed with name, relationship, e-mail and phone

#### Scenario: Guardian who is a club member links to their profile

- **WHEN** user opens the detail of a minor whose guardian is a club member
- **THEN** the guardian's name links to that member's detail page

#### Scenario: Adult member without guardians shows no guardians section

- **WHEN** user opens the detail of a member who has no guardians
- **THEN** no guardians are shown in the contact section

### Requirement: Managing a Minor's Guardians

The system SHALL allow users with MEMBERS:UPDATE permission and any guardian of the member to add and remove the member's guardians from the member detail page. The last guardian of a minor SHALL NOT be removable. Guardians of an adult member MAY all be removed.

#### Scenario: Admin adds a guardian from the member detail

- **WHEN** user with MEMBERS:UPDATE permission opens a minor's detail page and chooses "Přidat zákonného zástupce"
- **AND** picks an existing member or enters a new guardian and confirms
- **THEN** the guardian is added to the minor's guardians

#### Scenario: Guardian adds a second guardian

- **WHEN** a guardian of a minor opens the minor's detail page and adds another guardian
- **THEN** the new guardian is added to the minor's guardians

#### Scenario: Admin removes one of two guardians

- **WHEN** user with MEMBERS:UPDATE permission removes one guardian of a minor who has two guardians
- **THEN** the removed person is no longer listed as the minor's guardian

#### Scenario: Last guardian of a minor cannot be removed

- **WHEN** a user attempts to remove the only guardian of a minor
- **THEN** the system rejects the action
- **AND** displays a message that a minor must have at least one legal guardian

#### Scenario: Guardians of an adult member can be removed

- **GIVEN** a member who has turned 18 and still has one guardian
- **WHEN** user with MEMBERS:UPDATE permission removes that guardian
- **THEN** the member has no guardians

#### Scenario: Guardian actions hidden from other users

- **WHEN** a user who has neither MEMBERS:UPDATE permission nor is a guardian of the member views the member detail page
- **THEN** no actions for adding or removing guardians are displayed

#### Scenario: Admin corrects a non-member guardian's contact details

- **WHEN** user with MEMBERS:UPDATE permission changes the phone number of a non-member guardian
- **THEN** the new phone number is shown for that guardian on every minor they are a guardian of

### Requirement: Guardian Access to the Application

The system SHALL let every guardian log in to the application with their own user account. A guardian who is a club member SHALL use their existing member account. A guardian who is not a club member SHALL log in with their e-mail address as login name. After logging in, a guardian SHALL be able to open the detail page of each minor they are a guardian of.

#### Scenario: Non-member guardian logs in with e-mail

- **GIVEN** a non-member guardian whose account has been activated
- **WHEN** the guardian enters their e-mail address and password on the login page
- **THEN** the guardian is logged in

#### Scenario: Guardian opens their child's profile

- **WHEN** a logged-in guardian opens the detail page of a minor they are a guardian of
- **THEN** the minor's detail page is displayed including the guardians section

#### Scenario: Non-member guardian does not see member-only features

- **WHEN** a logged-in non-member guardian views the application navigation
- **THEN** features available only to club members (such as their own member profile or membership fees) are not offered

### Requirement: Guardian Account Activation

The system SHALL NOT send any e-mail to a guardian when their account is created. A guardian SHALL request an activation link themselves from the login page by entering their e-mail address.

#### Scenario: No e-mail is sent to a new guardian

- **WHEN** admin registers a minor with a new non-member guardian
- **THEN** a user account awaiting activation is created for the guardian
- **AND** no e-mail is sent to the guardian

#### Scenario: Guardian requests an activation link

- **WHEN** a non-member guardian whose account awaits activation enters their e-mail address on the "request activation link" page
- **THEN** an activation link is sent to that e-mail address
- **AND** the page shows the same confirmation it shows when no link is sent

#### Scenario: Guardian sets a password from the activation link

- **WHEN** the guardian opens the activation link and sets a valid password
- **THEN** the guardian's account is activated
- **AND** the guardian can log in with their e-mail address

### Requirement: Starting a Minor's Own Account

The system SHALL create a user account for every member, including minors, but SHALL NOT make a minor's account usable until someone deliberately starts it. Once a minor has an e-mail address of their own, users with MEMBERS:UPDATE permission and guardians of the minor SHALL be offered a "Založit účet" action that sends an activation link to the minor's own e-mail address. The action SHALL never send the link to a guardian's e-mail address.

#### Scenario: Minor's account is not started at registration

- **WHEN** admin registers a minor
- **THEN** a user account is created for the minor
- **AND** no e-mail is sent to the minor or to the guardians
- **AND** the minor cannot log in

#### Scenario: Admin starts the account of a minor who has their own e-mail

- **GIVEN** a minor whose account has not been started and who has an e-mail address of their own
- **WHEN** user with MEMBERS:UPDATE permission clicks "Založit účet" on the minor's detail page
- **THEN** an activation link is sent to the minor's own e-mail address
- **AND** the page confirms that the activation link was sent

#### Scenario: Guardian starts the account of their child

- **GIVEN** a minor with an e-mail address of their own whose account has not been started
- **WHEN** a guardian of the minor clicks "Založit účet" on the minor's detail page
- **THEN** an activation link is sent to the minor's own e-mail address

#### Scenario: Action not offered without the minor's own e-mail

- **WHEN** user with MEMBERS:UPDATE permission opens the detail of a minor who has no e-mail address of their own
- **THEN** the "Založit účet" action is not displayed

#### Scenario: Adding the minor's e-mail does not start the account

- **WHEN** an admin or guardian adds an e-mail address to a minor's profile and saves
- **THEN** no e-mail is sent to the minor
- **AND** the "Založit účet" action becomes available

#### Scenario: Action not offered once the account is active

- **WHEN** user opens the detail of a minor whose account has already been activated
- **THEN** the "Založit účet" action is not displayed

### Requirement: Notifications for a Minor Go to All Guardians

The system SHALL send every e-mail notification that concerns a minor member and is addressed to the minor's guardian to all of the minor's guardians.

#### Scenario: Both guardians receive a notification about their child

- **GIVEN** a minor with two guardians
- **WHEN** the system sends a notification concerning the minor to the minor's guardian
- **THEN** each of the two guardians receives the notification at their own e-mail address
