## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a user account and a member profile in a single flow. Whether the new member is a minor (younger than 18 today) or an adult is determined from the date of birth, and the registration form switches its sections accordingly. A minor SHALL be registered together with at least one legal guardian; each guardian is either chosen from the legal guardian candidates or entered as a new non-member guardian in the same step. An adult SHALL be registered without legal guardians. A member registered by hand SHALL be complete; registration does not send any e-mail. If any part of the registration fails, nothing is created.

#### Scenario: Admin registers a new adult member

- **WHEN** admin with MEMBERS:MANAGE permission navigates to the registration page
- **AND** submits the registration form for an adult with valid data
- **THEN** the new member appears in the member list
- **AND** a user account awaiting activation is created for the member
- **AND** no e-mail is sent to the member

#### Scenario: Admin registers a minor with an existing guardian

- **WHEN** admin fills in a date of birth of a person younger than 18
- **AND** chooses an adult member as the legal guardian and submits the form
- **THEN** the minor appears in the member list
- **AND** the chosen member is listed as the minor's legal guardian

#### Scenario: Admin registers a minor with a new non-member guardian

- **WHEN** admin registers a minor and enters a new guardian with first name, last name, e-mail and telephone
- **THEN** the minor is registered with that guardian
- **AND** the guardian gets a user account awaiting activation with a login number from the series EXT0001, EXT0002, …
- **AND** no e-mail is sent to anyone

#### Scenario: Admin registers a minor with two guardians

- **WHEN** admin registers a minor with one existing member and one new non-member as guardians
- **THEN** both are listed as the minor's legal guardians

#### Scenario: Minor registered without guardians is rejected

- **WHEN** admin submits the registration form for a minor without any legal guardian
- **THEN** the form shows an error that a legal guardian is required for minors
- **AND** no member is created

#### Scenario: Adult registered with guardians is rejected

- **WHEN** a registration of an adult is submitted together with legal guardians
- **THEN** the registration is rejected with an error that adults cannot have legal guardians
- **AND** no member is created

#### Scenario: Registration form switches sections by date of birth

- **WHEN** admin enters a date of birth of a person younger than 18
- **THEN** the form shows the "Zákonní zástupci" section and marks the member's own e-mail and telephone as optional
- **WHEN** admin changes the date of birth to a person aged 18 or older
- **THEN** the "Zákonní zástupci" section is hidden and the member's own e-mail and telephone are required

#### Scenario: Guardian takeover is offered before a date of birth is entered

- **WHEN** admin opens the registration form
- **THEN** the "Převzetí zákonného zástupce" section is shown before any date of birth is entered
- **AND** it stays shown whatever date of birth the admin enters

#### Scenario: Taking over a guardian as a minor is rejected

- **GIVEN** a non-member legal guardian "Eva Svobodová"
- **WHEN** admin chooses to take over "Eva Svobodová", enters a date of birth of a person younger than 18 and submits the form
- **THEN** the form shows an error that a minor cannot take over the role of a legal guardian
- **AND** no member is created
- **AND** "Eva Svobodová" stays a non-member legal guardian

#### Scenario: Failed guardian creation leaves nothing behind

- **WHEN** admin registers a minor with a new guardian whose e-mail is already in use
- **THEN** the form shows an error that the e-mail is already in use
- **AND** neither the member nor any guardian is created

#### Scenario: Adult registration takes over an existing non-member guardian

- **GIVEN** a non-member legal guardian "Eva Svobodová"
- **WHEN** admin registers an adult member and chooses to take over "Eva Svobodová"
- **THEN** the form is pre-filled with her name, e-mail and telephone
- **AND** after submission she becomes a club member and remains the legal guardian of her minors
- **AND** she gets a registration number by the usual rules, shown as her registration number everywhere in the application
- **AND** she can log in with either her new registration number or her former login number EXTnnnn

#### Scenario: Registration button not shown without permission

- **WHEN** user without MEMBERS:MANAGE permission views the member list
- **THEN** the "Registrovat člena" button is not displayed

#### Scenario: Registration form shows validation errors

- **WHEN** admin submits the registration form with invalid or missing data
- **THEN** the form shows inline error messages for each invalid field
- **AND** no member is created

### Requirement: Authorization for Member Operations

The system SHALL show or hide member actions based on the user's permissions.

#### Scenario: Register member button shown to authorized user

- **WHEN** user with MEMBERS:MANAGE permission views the member list
- **THEN** the "Registrovat člena" button is displayed

#### Scenario: Unauthenticated user cannot access members

- **WHEN** unauthenticated user attempts to navigate to the members section
- **THEN** they are redirected to the login page

### Requirement: Member Detail

The system SHALL display complete member details. Inactive members are not accessible to users without MEMBERS:MANAGE authority, except to users holding "Úprava údajů člena" over that member. The active/inactive status indicator on the detail page is only shown to users with MEMBERS:MANAGE authority.

#### Scenario: Authorized user views active member detail

- **WHEN** user with MEMBERS:READ permission navigates to a member's detail page
- **AND** the member is active
- **THEN** all member information is displayed including personal data, address, contact, and supplementary info

#### Scenario: Admin views suspended member detail

- **WHEN** user with MEMBERS:MANAGE authority navigates to an inactive member's detail page
- **THEN** the member detail is displayed including suspension reason, date, and note

#### Scenario: Holder of profile editing views suspended member detail

- **GIVEN** a user holds "Úprava údajů člena" over a member whose membership is suspended
- **WHEN** the user navigates to that member's detail page
- **THEN** the member detail is displayed
- **AND** the "Upravit profil" action is available

#### Scenario: Regular user cannot access inactive member detail

- **WHEN** user without MEMBERS:MANAGE authority and without "Úprava údajů člena" over the member navigates to an inactive member's detail page
- **THEN** the page shows a not-found error

#### Scenario: Edit button shown to authorized user

- **WHEN** user with MEMBERS:MANAGE permission, or a user holding "Úprava údajů člena" over the member, views a member detail page
- **THEN** an "Upravit profil" button is displayed

#### Scenario: Edit button not shown without permission

- **WHEN** user without MEMBERS:MANAGE permission and without "Úprava údajů člena" over the member views a member detail page
- **THEN** no edit button is shown for modifying the member's data

#### Scenario: Permissions button shown to authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views a member detail page
- **THEN** an "Oprávnění" button is displayed

#### Scenario: Suspend button shown for active member

- **WHEN** user with MEMBERS:MANAGE permission views an active member's detail page
- **THEN** an "Ukončit členství" button is displayed

#### Scenario: Suspend button not shown for suspended member

- **WHEN** user views a suspended member's detail page
- **THEN** no "Ukončit členství" button is shown

#### Scenario: Admin sees active/inactive status indicator

- **WHEN** user with MEMBERS:MANAGE authority views a member's detail page
- **THEN** an "Aktivní" or "Neaktivní" status indicator is shown next to the member's name

#### Scenario: Regular user does not see active/inactive status indicator

- **WHEN** user without MEMBERS:MANAGE authority views a member's detail page
- **THEN** no active/inactive status indicator is shown next to the member's name

### Requirement: Member Update

The system SHALL allow a member's data to be updated by a member administrator or by a user holding "Úprava údajů člena" over that member. Every adult member holds that permission over themself; a minor (under 18 years) does not, so a minor cannot update their own profile. Field access is role-based: the administrator-reserved fields can be changed only by a member administrator. Legal guardians are not part of the member edit form; they are managed separately on the minor's detail page.

#### Scenario: Member updates their own information

- **WHEN** authenticated adult member edits their own profile and saves the changes
- **THEN** the updated information is saved
- **AND** the member is taken to their updated profile

#### Scenario: Minor cannot edit own profile

- **WHEN** a minor member views their own profile
- **THEN** the "Upravit" action is not displayed
- **AND** an attempt to save changes to their own profile is refused with a permission denied error
- **AND** no changes are saved

#### Scenario: Admin updates a minor's information

- **WHEN** user with MEMBERS:MANAGE permission edits a minor member's profile and saves the changes
- **THEN** the updated information is saved

#### Scenario: Holder of profile editing updates another member's information

- **GIVEN** a user holds "Úprava údajů člena" over a member
- **WHEN** the user edits that member's profile and saves the changes
- **THEN** the updated information is saved
- **AND** the change history records the user as the author of the change

#### Scenario: Member can edit own profile after turning 18

- **GIVEN** a member who was a minor and has turned 18
- **WHEN** the member views their own profile
- **THEN** the "Upravit" action is displayed

#### Scenario: Admin updates a member's information

- **WHEN** user with MEMBERS:MANAGE permission edits a member's profile and saves the changes
- **THEN** the updated information is saved including admin-only fields (first name, last name, date of birth, gender, nationality, birth number)

#### Scenario: Member cannot edit another member's profile

- **WHEN** authenticated member attempts to edit a different member's profile without admin permission and without "Úprava údajů člena" over that member
- **THEN** the system shows a permission denied error
- **AND** no changes are saved

#### Scenario: Form shows validation errors on invalid update

- **WHEN** user submits an update with invalid data
- **THEN** the form shows inline validation errors
- **AND** no changes are saved

#### Scenario: Member-editable fields

- **WHEN** an adult member (without admin permission) opens their own edit form, or a holder of "Úprava údajů člena" opens the edit form of a member they hold it over
- **THEN** the form shows fields: email, phone, address, chip number, bank account, identity card, driving license, medical course, trainer license, dietary restrictions

#### Scenario: Admin-only fields

- **WHEN** user with MEMBERS:MANAGE permission opens a member's edit form
- **THEN** the form additionally shows admin-only fields: first name, last name, date of birth, gender, nationality, birth number

#### Scenario: Member cannot change their own nationality

- **WHEN** an adult member without MEMBERS:MANAGE opens their own edit form
- **THEN** their nationality is shown read-only
- **AND** an attempt to change it is ignored like any other admin-only field

#### Scenario: Non-admin submits admin-only fields

- **WHEN** member submits a form update that includes admin-only fields (e.g., firstName)
- **THEN** those fields are silently ignored
- **AND** only member-editable fields are updated

#### Scenario: Address update requires all fields

- **WHEN** user submits an update with only some address fields
- **THEN** the form shows an error that all address fields (street, city, postal code, country) are required
- **AND** no changes are saved

#### Scenario: Contact removal prevented

- **WHEN** an adult member submits an update that would remove their e-mail address
- **THEN** the form shows an error that at least one email is required

#### Scenario: Minor's own contact can be removed when a guardian covers it

- **GIVEN** a minor whose legal guardian has an e-mail address
- **WHEN** an admin removes the minor's own e-mail and saves
- **THEN** the change is saved

#### Scenario: Changing nationality to Czech enables birth number field

- **WHEN** admin changes nationality to Czech (CZ) in the edit form
- **THEN** the birth number field becomes available for input

#### Scenario: Changing nationality away from Czech clears birth number

- **WHEN** admin changes nationality from Czech to non-Czech in the edit form
- **THEN** the birth number field is cleared and hidden

#### Scenario: Partial update preserves unchanged fields

- **WHEN** member submits a PATCH update with only some fields
- **THEN** only those fields are updated
- **AND** all other fields remain unchanged

#### Scenario: Empty update rejected

- **WHEN** member submits a PATCH update with an empty request body
- **THEN** the form shows an error that at least one field must be provided

### Requirement: Membership Suspension

The system SHALL allow users with MEMBERS:MANAGE permission to suspend a member's membership. Before suspension, the system SHALL check group ownership and the state of the member's financial account.

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

- **WHEN** user without MEMBERS:MANAGE permission views an active member's detail page
- **THEN** no "Ukončit členství" button is shown
