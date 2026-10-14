# Spec Delta

## ADDED Requirements

### Requirement: Member Profile Editing Permission

The system SHALL provide the permission "Úprava údajů člena", which is held over individual members and arises only from relationships: every adult member holds it over themself, and groups may delegate it to their owners. It SHALL NOT be grantable in the permissions dialog. Holding MEMBERS:MANAGE does not imply it.

#### Scenario: Adult member holds the permission over themself

- **WHEN** an adult member opens their own detail page
- **THEN** the "Upravit profil" action is available

#### Scenario: Minor does not hold the permission over themself

- **WHEN** a minor opens their own detail page
- **THEN** all of their own data is shown
- **AND** the "Upravit profil" action is not available

#### Scenario: Permission is not offered in the permissions dialog

- **WHEN** an administrator opens the permissions dialog of a member
- **THEN** "Úprava údajů člena" is not among the offered permissions

#### Scenario: Holder over one member cannot edit another

- **GIVEN** a user holds "Úprava údajů člena" over member A only
- **WHEN** the user opens the detail page of member B
- **THEN** the "Upravit profil" action is not available for member B

### Requirement: Holder Of Profile Editing Sees All Member Data

The system SHALL show a user holding "Úprava údajů člena" over a member all of that member's data, including the administrator-reserved data (first name, last name, date of birth, gender, birth number), health-related data such as dietary restrictions, and the minor's legal guardians. Viewing a birth number SHALL be audited for such a user in the same way as for an administrator.

#### Scenario: Holder sees reserved and sensitive data

- **GIVEN** a user holds "Úprava údajů člena" over a member
- **WHEN** the user opens that member's detail page
- **THEN** the birth number, date of birth, gender and dietary restrictions of the member are displayed

#### Scenario: Holder's view of a birth number is audited

- **GIVEN** a user holds "Úprava údajů člena" over a member with a birth number
- **WHEN** the user opens that member's detail page
- **THEN** the system records an audit log entry that the user accessed the member's birth number

## MODIFIED Requirements

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

- **WHEN** user with MEMBERS:UPDATE permission, or a user holding "Úprava údajů člena" over the member, views a member detail page
- **THEN** an "Upravit profil" button is displayed

#### Scenario: Edit button not shown without permission

- **WHEN** user without MEMBERS:UPDATE permission and without "Úprava údajů člena" over the member views a member detail page
- **THEN** no edit button is shown for modifying the member's data

#### Scenario: Permissions button shown to authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views a member detail page
- **THEN** an "Oprávnění" button is displayed

#### Scenario: Suspend button shown for active member

- **WHEN** user with MEMBERS:UPDATE permission views an active member's detail page
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

#### Scenario: Minor views own detail without edit action

- **WHEN** a minor views their own detail page
- **THEN** the page shows all of the minor's data in the two-column layout
- **AND** no "Upravit profil" button is shown

#### Scenario: Admin detail shows action buttons with icons

- **WHEN** member detail response includes a full PATCH template (admin view)
- **THEN** the page header shows action buttons: "Upravit profil" (pencil icon), "Oprávnění" (shield icon, visible only if permissions link present), "Ukončit členství" (user-x icon, red)

#### Scenario: Own profile shows membership and edit buttons

- **WHEN** member detail response includes a self-edit PATCH template (own profile view)
- **THEN** the page header shows: "Členské příspěvky" button and "Upravit profil" button
- **AND** "Oprávnění" and "Ukončit členství" buttons are NOT shown

#### Scenario: Holder of profile editing sees edit button only

- **WHEN** a user holding "Úprava údajů člena" over another member, without MEMBERS:MANAGE, views that member's detail page
- **THEN** the page header shows the "Upravit profil" button
- **AND** "Členské příspěvky", "Oprávnění" and "Ukončit členství" buttons are NOT shown

#### Scenario: Legal guardians section on a minor's detail

- **WHEN** an admin, the minor themself, or a user holding "Úprava údajů člena" over the minor views a minor's detail page
- **THEN** a "Zákonní zástupci" section lists each guardian of the minor's legal guardian group with name, e-mail and telephone
- **AND** only an admin sees the "Upravit zástupce" action in that section

#### Scenario: Legal guardians section of a minor without guardians

- **WHEN** an admin views the detail of a minor who has no legal guardian group
- **THEN** the "Zákonní zástupci" section states that the minor has no legal guardian
- **AND** the "Upravit zástupce" action is available

#### Scenario: Other members do not see a minor's guardians

- **WHEN** a member without MEMBERS:MANAGE permission and without "Úprava údajů člena" over the minor views another member's detail page
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

### Requirement: Member Edit Form Layout

Edit forms SHALL place action buttons at the bottom of the form.

#### Scenario: Edit form actions are at the bottom

- **WHEN** user opens member edit form
- **THEN** "Zrušit" and "Uložit změny" buttons appear after all form sections
- **AND** admin edit shows a badge "Admin — editace všech polí" in the page header
- **AND** in self-edit mode, fields not present in the template are displayed as read-only

#### Scenario: Reserved fields are read-only for a holder of profile editing

- **WHEN** a user holding "Úprava údajů člena" over a member, without MEMBERS:MANAGE, opens that member's edit form
- **THEN** the form is pre-filled with the member's current data
- **AND** first name, last name, date of birth, gender and birth number are shown with their values as read-only
- **AND** saving the form does not attempt to change those fields

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

- **WHEN** user with MEMBERS:UPDATE permission edits a minor member's profile and saves the changes
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

- **WHEN** user with MEMBERS:UPDATE permission edits a member's profile and saves the changes
- **THEN** the updated information is saved including admin-only fields (first name, last name, date of birth, gender, birth number)

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
