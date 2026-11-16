## ADDED Requirements

### Requirement: Country Selection

The system SHALL let users choose nationality and address country from the most common countries, shown by Czech name: Česko, Slovensko, Rakousko, Polsko, Německo, Švédsko, Norsko, Dánsko. Option "Jiný stát…" SHALL let the user enter any other two-letter country code, showing its Czech name next to the code. This applies to nationality in the registration and administrator edit forms and to address country in every member form.

#### Scenario: Admin picks a common country

- **WHEN** admin opens the "Státní příslušnost" field in the registration form
- **THEN** the offered countries are Česko, Slovensko, Rakousko, Polsko, Německo, Švédsko, Norsko, Dánsko and "Jiný stát…"
- **WHEN** admin chooses "Slovensko" and registers the member
- **THEN** the member's nationality is Slovensko

#### Scenario: Member enters a country outside the common list

- **WHEN** an adult member edits their own address, chooses "Jiný stát…" in the "Stát" field and types "FR"
- **THEN** "Francie" is shown next to the entered code
- **AND** after saving, the member's address country is Francie

#### Scenario: Unknown code is flagged while typing

- **WHEN** a user chooses "Jiný stát…" and types a two-letter code that names no country
- **THEN** the field indicates that the code is not a known country

#### Scenario: Stored country outside the common list opens as other country

- **GIVEN** a member whose address country is Francie
- **WHEN** the member opens their edit form
- **THEN** the "Stát" field shows "Jiný stát…" with code "FR" and name "Francie"

#### Scenario: Registration form pre-fills Czechia

- **WHEN** admin opens the member registration form
- **THEN** "Česko" is pre-selected for both "Státní příslušnost" and the address "Stát"
- **AND** the birth number field is shown

### Requirement: Country Code Validation

The system SHALL accept only codes of existing countries (ISO 3166-1 alpha-2) for nationality and address country. A code that has the right format but names no country SHALL be refused, and nothing SHALL be saved.

#### Scenario: Registration with an unknown country code is refused

- **WHEN** admin registers a member with nationality code "QQ"
- **THEN** the form shows an error
- **AND** no member is created

#### Scenario: Address update with an unknown country code is refused

- **WHEN** an adult member saves their address with country code "QQ"
- **THEN** the form shows an error
- **AND** no changes are saved

### Requirement: Country Names On Member Detail

The member detail page SHALL show nationality and address country by their Czech country names instead of country codes.

#### Scenario: Detail shows Czech country names

- **GIVEN** a member with nationality "CZ" and address country "SK"
- **WHEN** an administrator opens the member's detail page
- **THEN** nationality is shown as "Česko"
- **AND** address country is shown as "Slovensko"

## MODIFIED Requirements

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
- **AND** first name, last name, date of birth, gender, nationality and birth number are shown with their values as read-only
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

- **WHEN** user with MEMBERS:UPDATE permission opens a member's edit form
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

### Requirement: Holder Of Profile Editing Sees All Member Data

The system SHALL show a user holding "Úprava údajů člena" over a member all of that member's data, including the administrator-reserved data (first name, last name, date of birth, gender, nationality, birth number), health-related data such as dietary restrictions, and the minor's legal guardians. Viewing a birth number SHALL be audited for such a user in the same way as for an administrator.

#### Scenario: Holder sees reserved and sensitive data

- **GIVEN** a user holds "Úprava údajů člena" over a member
- **WHEN** the user opens that member's detail page
- **THEN** the birth number, date of birth, gender, nationality and dietary restrictions of the member are displayed

#### Scenario: Holder's view of a birth number is audited

- **GIVEN** a user holds "Úprava údajů člena" over a member with a birth number
- **WHEN** the user opens that member's detail page
- **THEN** the system records an audit log entry that the user accessed the member's birth number
