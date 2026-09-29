## ADDED Requirements

### Requirement: Registration Form Adapts to Minor or Adult

The registration form SHALL determine from the entered date of birth whether the new member is a minor (under 18 on the day of registration) or an adult, and SHALL show the sections that apply. For a minor, the form SHALL show the guardians section and SHALL mark the member's own e-mail, phone and bank account as optional. For an adult, the form SHALL hide the guardians section and SHALL require the member's own e-mail and phone; the bank account stays optional.

#### Scenario: Entering a minor's date of birth shows the guardians section

- **WHEN** admin enters a date of birth for which the new member is under 18
- **THEN** the registration form shows the guardians section with at least one guardian entry
- **AND** the member's own e-mail, phone and bank account are marked as optional

#### Scenario: Entering an adult's date of birth hides the guardians section

- **WHEN** admin enters a date of birth for which the new member is 18 or older
- **THEN** the registration form does not show the guardians section
- **AND** the member's own e-mail and phone are marked as required

#### Scenario: Changing the date of birth from minor to adult discards guardians

- **WHEN** admin has entered guardians for a minor and then changes the date of birth so the member is an adult
- **THEN** the guardians section is hidden
- **AND** the entered guardians are not saved with the member

#### Scenario: Form shows guardian section only after date of birth is known

- **WHEN** admin opens the registration form and has not entered a date of birth yet
- **THEN** neither the guardians section nor the minor/adult contact requirements are shown as decided

#### Scenario: Adult registered with guardians is rejected

- **WHEN** a registration of an adult member is submitted together with guardians
- **THEN** the registration is refused with an error that guardians can only be entered for minors
- **AND** no member is created

## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a user account and a member profile in a single flow. A member registered by hand SHALL be complete; registration does not send any e-mail. When a minor is registered, the system SHALL also record the minor's guardians and create user accounts for guardians who do not have one yet.

#### Scenario: Admin registers a new member

- **WHEN** admin with MEMBERS:CREATE permission navigates to the registration page
- **AND** submits the registration form with valid data
- **THEN** the new member appears in the member list
- **AND** a user account awaiting activation is created for the member
- **AND** no e-mail is sent to the member

#### Scenario: Admin registers a minor with two guardians

- **WHEN** admin registers a minor with one guardian who is a club member and one new non-member guardian
- **THEN** the minor appears in the member list
- **AND** both guardians are listed on the minor's detail page
- **AND** a user account awaiting activation is created for the non-member guardian
- **AND** no e-mail is sent to the minor or to either guardian

#### Scenario: Minor with only a guardian's e-mail is registered

- **WHEN** admin registers a minor whose only e-mail address is their guardian's
- **THEN** the member is registered successfully
- **AND** a user account is created for the minor that the minor cannot use until it is started

#### Scenario: Registration button not shown without permission

- **WHEN** user without MEMBERS:CREATE permission views the member list
- **THEN** the "Registrovat člena" button is not displayed

#### Scenario: Registration form shows validation errors

- **WHEN** admin submits the registration form with invalid or missing data
- **THEN** the form shows inline error messages for each invalid field
- **AND** no member is created

#### Scenario: Registration by hand cannot create an incomplete member

- **WHEN** admin submits the registration form for a minor without a legal guardian
- **THEN** the form shows an error that a legal guardian is required for minors
- **AND** no member is created

### Requirement: Contact Information

The system SHALL require at least one email address and one phone number for every member. For an adult member these SHALL be the member's own. For a minor the requirement SHALL be met by the contact details of any of the minor's guardians; the minor's own e-mail and phone are optional.

#### Scenario: Adult member's own contact details required

- **WHEN** adult member's registration form is filled
- **THEN** an email address and phone number must be provided for the member

#### Scenario: Minor member's guardian contact details required

- **WHEN** registration form is filled for a minor (under 18)
- **THEN** at least one guardian with an email address and phone number must be provided
- **AND** the member's own email and phone are optional

#### Scenario: Contact requirement met by a second guardian

- **WHEN** admin registers a minor with two guardians who both have an e-mail and a phone
- **THEN** the contact requirement is met
- **AND** the member's own e-mail and phone may be left empty

#### Scenario: Missing contact information shows error

- **WHEN** admin submits a registration form for an adult without their own email or phone, or for a minor without any guardian
- **THEN** the form shows an error indicating that contact information is required

### Requirement: Incomplete Member Data

A member brought in from ORIS, or a minor who has become an adult, may lack details that registering by hand requires. The system SHALL treat a member as incomplete when any of the following is true:

- the member is an adult without an e-mail address of their own;
- the member is an adult without a telephone number of their own;
- the member is a minor without any legal guardian;
- the member is a Czech national without a birth number;
- the member has no complete address (street, city, postal code and country).

Completeness SHALL always follow from the member's current details and SHALL never be set or cleared by hand. Only users with MEMBERS:MANAGE authority SHALL see whether a member is incomplete and what is missing.

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

#### Scenario: Filling in the missing details makes the member complete

- **GIVEN** an incomplete member whose only missing item is a legal guardian
- **WHEN** an admin adds a guardian with an e-mail address and telephone number and saves
- **THEN** the member is no longer marked incomplete
- **AND** the member no longer appears under the "Jen neúplní" filter

#### Scenario: A minor with own contacts becomes complete on turning 18

- **GIVEN** a minor who is incomplete only because they have no legal guardian
- **AND** the minor has an e-mail address and phone number of their own
- **WHEN** they turn 18
- **THEN** their detail no longer lists any missing item

#### Scenario: A member turning 18 without own contacts becomes incomplete

- **GIVEN** a complete minor whose only e-mail and phone are their guardians'
- **WHEN** the member turns 18
- **THEN** the member is marked incomplete
- **AND** the detail warning lists the missing own e-mail and phone
- **AND** the member's guardians stay listed until an admin removes them

#### Scenario: Adding own contacts completes a member who turned 18

- **GIVEN** a member who turned 18 and is incomplete only because they have no e-mail and phone of their own
- **WHEN** an admin fills in the member's own e-mail and phone and saves
- **THEN** the member is no longer marked incomplete

### Requirement: Member Update

The system SHALL allow members to update their own profile data, and admins to update any member's data. Field access is role-based. A guardian of a minor SHALL be able to update the minor's profile with the same fields the minor could update on their own profile. Guardians themselves are managed in the guardians section of the member detail, not in the edit form.

#### Scenario: Member updates their own information

- **WHEN** authenticated member edits their own profile and saves the changes
- **THEN** the updated information is saved
- **AND** the member is taken to their updated profile

#### Scenario: Guardian updates their child's information

- **WHEN** a guardian of a minor opens the minor's profile, edits the minor's phone number and saves
- **THEN** the updated information is saved
- **AND** the guardian is taken to the minor's updated profile

#### Scenario: Admin updates a member's information

- **WHEN** user with MEMBERS:UPDATE permission edits a member's profile and saves the changes
- **THEN** the updated information is saved including admin-only fields (first name, last name, date of birth, gender, birth number)

#### Scenario: Member cannot edit another member's profile

- **WHEN** authenticated member attempts to edit a different member's profile without admin permission and without being that member's guardian
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

- **WHEN** an adult member submits an update that would remove their own e-mail address or phone number
- **THEN** the form shows an error that the member's own e-mail and phone are required

#### Scenario: Minor's own contact can be removed

- **WHEN** an admin or guardian removes the own e-mail address of a minor who has a guardian
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
