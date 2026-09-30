## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a user account and a member profile in a single flow. Whether the new member is a minor (younger than 18 today) or an adult is determined from the date of birth, and the registration form switches its sections accordingly. A minor SHALL be registered together with at least one legal guardian; each guardian is either chosen from the legal guardian candidates or entered as a new non-member guardian in the same step. An adult SHALL be registered without legal guardians. A member registered by hand SHALL be complete; registration does not send any e-mail. If any part of the registration fails, nothing is created.

#### Scenario: Admin registers a new adult member

- **WHEN** admin with MEMBERS:CREATE permission navigates to the registration page
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

#### Scenario: Failed guardian creation leaves nothing behind

- **WHEN** admin registers a minor with a new guardian whose e-mail is already in use
- **THEN** the form shows an error that the e-mail is already in use
- **AND** neither the member nor any guardian is created

#### Scenario: Adult registration takes over an existing non-member guardian

- **GIVEN** a non-member legal guardian "Eva Svobodová"
- **WHEN** admin registers an adult member and chooses to take over "Eva Svobodová"
- **THEN** the form is pre-filled with her name, e-mail and telephone
- **AND** after submission she becomes a club member and remains the legal guardian of her minors
- **AND** she keeps logging in with her login number EXTnnnn

#### Scenario: Registration button not shown without permission

- **WHEN** user without MEMBERS:CREATE permission views the member list
- **THEN** the "Registrovat člena" button is not displayed

#### Scenario: Registration form shows validation errors

- **WHEN** admin submits the registration form with invalid or missing data
- **THEN** the form shows inline error messages for each invalid field
- **AND** no member is created

### Requirement: Contact Information

The system SHALL require an e-mail address and a telephone number for every member. An adult member SHALL provide their own. For a minor the requirement is met by the minor's own contacts or by those of any of their legal guardians; the e-mail and the telephone MAY come from different people.

#### Scenario: Adult member's own contact details required

- **WHEN** adult member's registration form is filled
- **THEN** an e-mail address and telephone number must be provided for the member

#### Scenario: Minor member's own contact details are optional

- **WHEN** registration form is filled for a minor (under 18) with at least one legal guardian
- **THEN** the member's own e-mail and telephone are optional

#### Scenario: Missing contact information shows error

- **WHEN** admin submits a registration form for an adult without an e-mail or without a telephone
- **THEN** the form shows an error indicating that contact information is required

### Requirement: Member Detail Page Layout

The member detail page SHALL use a two-column layout driven by the available PATCH template fields, without client-side role detection.

#### Scenario: Detail page with full template (admin or self)

- **WHEN** member detail response includes a PATCH template
- **THEN** the page renders in a two-column layout: left column (personal data, contact, legal guardians of a minor, address), right column (supplementary info, documents and licenses)
- **AND** an "Upravit profil" button is shown

#### Scenario: Detail page without template (other member)

- **WHEN** member detail response has no PATCH template
- **THEN** only contact section and address section are displayed
- **AND** no action buttons are shown

#### Scenario: Admin detail shows action buttons with icons

- **WHEN** member detail response includes a full PATCH template (admin view)
- **THEN** the page header shows action buttons: "Upravit profil" (pencil icon), "Oprávnění" (shield icon, visible only if permissions link present), "Ukončit členství" (user-x icon, red)

#### Scenario: Own profile shows membership and edit buttons

- **WHEN** member detail response includes a self-edit PATCH template (own profile view)
- **THEN** the page header shows: "Členské příspěvky" button and "Upravit profil" button
- **AND** "Oprávnění" and "Ukončit členství" buttons are NOT shown

#### Scenario: Legal guardians section on a minor's detail

- **WHEN** an admin or the minor themself views a minor's detail page
- **THEN** a "Zákonní zástupci" section lists each guardian of the minor's legal guardian group with name, e-mail and telephone
- **AND** an admin sees the "Upravit zástupce" action in that section

#### Scenario: Legal guardians section of a minor without guardians

- **WHEN** an admin views the detail of a minor who has no legal guardian group
- **THEN** the "Zákonní zástupci" section states that the minor has no legal guardian
- **AND** the "Upravit zástupce" action is available

#### Scenario: Other members do not see a minor's guardians

- **WHEN** a member without MEMBERS:MANAGE permission views another member's detail page
- **THEN** no "Zákonní zástupci" section is shown

#### Scenario: Group navigation buttons shown when member belongs to a group

- **WHEN** the member detail response carries a `trainingGroup` HAL link
- **THEN** the page header additionally shows a "Tréninková skupina" button (dumbbell icon, secondary style)
- **AND** clicking it navigates the user to the linked training group detail page
- **WHEN** the member detail response carries a `legalGuardianGroup` HAL link (admins only)
- **THEN** the page header additionally shows a "Zákonní zástupci" button (heart icon, secondary style)
- **AND** clicking it navigates the user to the linked legal guardian group detail page
- **WHEN** the member detail response carries neither link
- **THEN** neither group navigation button is shown
- **AND** this rule applies uniformly to every view of the member detail page (admin view, self-profile view, any other variant)

### Requirement: Member Registration Page Layout

The member registration page SHALL use a two-column layout.

#### Scenario: Registration page layout

- **WHEN** admin navigates to member registration page
- **THEN** the left column contains: personal data section, contact section, legal guardians section (only for a minor), address section
- **AND** the right column contains supplementary information section only (no documents or licenses)
- **AND** "Zrušit" and "Registrovat člena" (user-plus icon) buttons appear at the bottom of the form

#### Scenario: Legal guardians section lets admin choose or create guardians

- **WHEN** admin works with the legal guardians section of the registration form
- **THEN** they can add any number of guardians, each either chosen from the guardian candidates or entered as a new person with first name, last name, e-mail and telephone
- **AND** they can remove a guardian from the list before submitting

### Requirement: Member Update

The system SHALL allow members to update their own profile data, and admins to update any member's data. Field access is role-based. Legal guardians are not part of the member edit form; they are managed separately on the minor's detail page.

#### Scenario: Member updates their own information

- **WHEN** authenticated member edits their own profile and saves the changes
- **THEN** the updated information is saved
- **AND** the member is taken to their updated profile

#### Scenario: Admin updates a member's information

- **WHEN** user with MEMBERS:UPDATE permission edits a member's profile and saves the changes
- **THEN** the updated information is saved including admin-only fields (first name, last name, date of birth, gender, birth number)

#### Scenario: Member cannot edit another member's profile

- **WHEN** authenticated member attempts to edit a different member's profile without admin permission
- **THEN** the system shows a permission denied error
- **AND** no changes are saved

#### Scenario: Form shows validation errors on invalid update

- **WHEN** user submits an update with invalid data
- **THEN** the form shows inline validation errors
- **AND** no changes are saved

#### Scenario: Member-editable fields

- **WHEN** a member (without admin permission) opens their own edit form
- **THEN** the form shows fields: email, phone, address, chip number, nationality, bank account, identity card, driving license, medical course, trainer license, dietary restrictions

#### Scenario: Admin-only fields

- **WHEN** user with MEMBERS:UPDATE permission opens a member's edit form
- **THEN** the form additionally shows admin-only fields: first name, last name, date of birth, gender, birth number

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

- **WHEN** member changes nationality to Czech (CZ) in the edit form
- **THEN** the birth number field becomes available for input

#### Scenario: Changing nationality away from Czech clears birth number

- **WHEN** member changes nationality from Czech to non-Czech in the edit form
- **THEN** the birth number field is cleared and hidden

#### Scenario: Partial update preserves unchanged fields

- **WHEN** member submits a PATCH update with only some fields
- **THEN** only those fields are updated
- **AND** all other fields remain unchanged

#### Scenario: Empty update rejected

- **WHEN** member submits a PATCH update with an empty request body
- **THEN** the form shows an error that at least one field must be provided

### Requirement: Incomplete Member Data

A member brought in from ORIS may lack details that registering by hand requires; minors from ORIS never have legal guardians. The system SHALL treat a member as incomplete when any of the following is true:

- an adult member has no own e-mail address; a minor has none and none of their legal guardians has one;
- an adult member has no own telephone number; a minor has none and none of their legal guardians has one;
- the member is a Czech national without a birth number;
- the member is a minor without a legal guardian;
- the member has no complete address (street, city, postal code and country).

Completeness SHALL always follow from the member's current details and their legal guardians and SHALL never be set or cleared by hand. The member list mark and the "Jen neúplní" filter reflect the member's completeness as of the member's last save or the day they turned 18; they MAY lag behind later changes of the member's guardians or of the guardians' contacts, while the member detail always shows the current state. Only users with MEMBERS:MANAGE authority SHALL see whether a member is incomplete and what is missing.

#### Scenario: Admin sees which members are incomplete

- **WHEN** user with MEMBERS:MANAGE authority views the member list
- **THEN** each incomplete member's row is marked "Neúplné údaje"
- **AND** complete members carry no such mark

#### Scenario: Admin filters the list to incomplete members

- **WHEN** user with MEMBERS:MANAGE authority turns on the "Jen neúplní" filter in the member list
- **THEN** only incomplete members are shown
- **AND** the filter combines with the other list filters

#### Scenario: Admin sees what is missing on the member detail

- **WHEN** user with MEMBERS:MANAGE authority opens the detail of an incomplete member
- **THEN** a warning lists each missing item, for example "Chybí: rodné číslo, zákonný zástupce"
- **AND** the "Upravit profil" button lets them fill the details in

#### Scenario: Regular user sees nothing about completeness

- **WHEN** user without MEMBERS:MANAGE authority views the member list or a member's detail
- **THEN** no incompleteness mark, filter or warning is shown

#### Scenario: Minor imported from ORIS lacks a guardian

- **WHEN** a minor is brought in from ORIS
- **THEN** the minor is marked incomplete with a missing legal guardian

#### Scenario: Adding a guardian makes the minor complete

- **GIVEN** an incomplete minor whose only missing item is a legal guardian
- **WHEN** an admin sets a guardian with an e-mail address and telephone number on the minor's detail page
- **THEN** the minor's detail no longer lists any missing item

#### Scenario: Guardian without contact does not cover the minor's contact

- **GIVEN** a minor without own contacts whose only guardian is a member without a telephone
- **WHEN** an admin opens the minor's detail
- **THEN** the warning lists a missing telephone

#### Scenario: A minor becomes an adult

- **GIVEN** a minor who has no own e-mail and whose guardian has an e-mail
- **WHEN** they turn 18
- **THEN** their detail no longer lists legal guardians
- **AND** their detail lists a missing e-mail

### Requirement: Edits Never Leave A Member Less Complete

Editing a member in Klabis SHALL NOT remove a detail whose absence would make the member incomplete. An incomplete member MAY be saved while still incomplete, as long as the edit does not add a new missing item. This keeps members who are complete today complete, while letting administrators fill in imported members step by step. The only exception is a corrected date of birth that turns an adult into a minor: the change is saved and the member becomes incomplete until a legal guardian is set.

#### Scenario: Complete member cannot lose a required detail

- **GIVEN** a complete Czech member
- **WHEN** an admin clears the member's birth number and saves
- **THEN** the form shows an error that the birth number is required
- **AND** no changes are saved

#### Scenario: Incomplete member can be edited without completing everything

- **GIVEN** a member missing both a birth number and a legal guardian
- **WHEN** an admin fills in only the birth number and saves
- **THEN** the change is saved
- **AND** the member remains incomplete with only the guardian missing

#### Scenario: Unrelated edit on an incomplete member is saved

- **GIVEN** a member missing a telephone number
- **WHEN** an admin changes only the member's address and saves
- **THEN** the change is saved
- **AND** the member remains incomplete

#### Scenario: Incomplete member cannot lose another required detail

- **GIVEN** a member missing a telephone number but holding an e-mail address
- **WHEN** an admin removes the e-mail address and saves
- **THEN** the form shows an error that contact information is required
- **AND** no changes are saved

#### Scenario: Correcting the date of birth of an adult to a minor

- **GIVEN** a complete adult member without legal guardians
- **WHEN** an admin changes the date of birth so that the member is younger than 18 and saves
- **THEN** the change is saved
- **AND** the member is marked incomplete with a missing legal guardian
- **AND** the "Upravit zástupce" action becomes available on the member's detail

## ADDED Requirements

### Requirement: Creating A Minor's Own Account

A minor SHALL NOT be able to activate their user account by themself. A user with MEMBERS:MANAGE permission MAY decide that a minor gets their own login by using the "Založit účet" action on the minor's detail page, which sends an activation link to the minor's own e-mail. The action is offered only for a minor whose account still awaits activation and who has an own e-mail. After turning 18 the member activates their account as any adult member.

#### Scenario: Admin creates an account for a minor

- **GIVEN** a minor with an own e-mail whose account awaits activation
- **WHEN** an admin clicks "Založit účet" on the minor's detail page
- **THEN** an activation link is sent to the minor's own e-mail
- **AND** the page confirms that the link was sent

#### Scenario: Action hidden for a minor without own e-mail

- **WHEN** an admin opens the detail of a minor without an own e-mail
- **THEN** no "Založit účet" action is shown

#### Scenario: Action hidden for an activated account or an adult

- **WHEN** an admin opens the detail of a minor whose account is already active, or the detail of an adult member
- **THEN** no "Založit účet" action is shown

#### Scenario: Action hidden without MEMBERS:MANAGE

- **WHEN** a user without MEMBERS:MANAGE permission opens a minor's detail page
- **THEN** no "Založit účet" action is shown
