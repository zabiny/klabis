# Members Specification

## Purpose

Covers the full lifecycle of club members: registration, viewing and editing member profiles, and suspending memberships. Defines what information is collected, who can see and edit it, and how the system guides users through each workflow.

## Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a user account and a member profile in a single flow.

#### Scenario: Admin registers a new member

- **WHEN** admin with MEMBERS:CREATE permission navigates to the registration page
- **AND** submits the registration form with valid data
- **THEN** the new member appears in the member list
- **AND** a welcome email is sent to the member's email address

#### Scenario: Registration button not shown without permission

- **WHEN** user without MEMBERS:CREATE permission views the member list
- **THEN** the "Registrovat člena" button is not displayed

#### Scenario: Registration form shows validation errors

- **WHEN** admin submits the registration form with invalid or missing data
- **THEN** the form shows inline error messages for each invalid field
- **AND** no member is created

### Requirement: Mandatory Personal Information

The system SHALL require the following personal information for all new members: first name, last name, date of birth, nationality, gender, and full postal address.

#### Scenario: Form submitted with all required fields

- **WHEN** admin fills in all mandatory fields and submits the registration form
- **THEN** validation passes
- **AND** the member is created successfully

#### Scenario: Form submitted with a missing mandatory field

- **WHEN** admin submits the registration form with any mandatory field missing
- **THEN** the form shows an error indicating which field is required
- **AND** the member is not created

### Requirement: Registration Number Generation

The system SHALL automatically assign a unique registration number in format XXXYYSS (club code + 2-digit birth year + 2-digit sequence).

#### Scenario: Registration number assigned on first member for a birth year

- **WHEN** the first member born in 2001 is registered
- **THEN** their registration number ends with 0100 (e.g., ZBM0100 for club ZBM)

#### Scenario: Subsequent members born in the same year get incremented numbers

- **WHEN** additional members born in 2001 are registered
- **THEN** each receives the next sequence number (e.g., ZBM0101, ZBM0102)

#### Scenario: Each birth year has its own sequence

- **WHEN** a member born in 2005 is registered after a member born in 2001
- **THEN** the new member's registration number starts a fresh sequence for 2005 (e.g., ZBM0500)

### Requirement: Birth Number Management

For Czech nationals, the system SHALL require a birth number (rodné číslo). The birth number field is hidden for non-Czech nationals and is stored encrypted.

#### Scenario: Birth number field shown for Czech nationality

- **WHEN** user views member form with Czech (CZ) nationality selected
- **THEN** the birth number field is visible and marked as required

#### Scenario: Birth number field hidden for non-Czech nationality

- **WHEN** user views member form with a non-Czech nationality selected
- **THEN** the birth number field is not shown

#### Scenario: Changing nationality from Czech clears birth number

- **WHEN** user changes nationality from Czech to non-Czech in the member form
- **THEN** the birth number field is hidden
- **AND** any previously entered birth number is cleared

#### Scenario: Czech member must provide birth number

- **WHEN** admin submits registration form for a Czech national without a birth number
- **THEN** the form shows an error that birth number is required for Czech nationals

#### Scenario: Valid birth number accepted

- **WHEN** user enters a birth number in valid format (e.g., "901231/1234" or "9012311234")
- **THEN** the form accepts the value

#### Scenario: Birth number without slash is normalized

- **WHEN** user enters a birth number without a slash separator (e.g., "9012311234")
- **THEN** the system stores it normalized to the format with slash (RRMMDD/XXXX)

#### Scenario: Invalid birth number format rejected

- **WHEN** user enters a birth number in an invalid format (e.g., "abc123", "90123")
- **THEN** the form shows an error that the birth number format is invalid

#### Scenario: Birth number date inconsistency shows warning

- **WHEN** user enters a birth number whose date component does not match the member's date of birth
- **THEN** the system shows a warning message
- **AND** the form can still be submitted

#### Scenario: Birth number gender inconsistency shows warning

- **WHEN** user enters a birth number whose month component indicates a different gender than selected
- **THEN** the system shows a warning message
- **AND** the form can still be submitted

#### Scenario: Birth number visible in member detail

- **WHEN** authorized user views member detail
- **THEN** the birth number is displayed (or absent if not set)

#### Scenario: Birth number access is audited

- **WHEN** user views member data containing a birth number
- **THEN** the system records an audit log entry for the birth number access

#### Scenario: Birth number modification is audited

- **WHEN** user creates or updates a birth number
- **THEN** the system records an audit log entry for the birth number change

### Requirement: Contact Information

The system SHALL require at least one email address and one phone number — either from the member directly or from their legal guardian (for minors).

#### Scenario: Adult member's own contact details required

- **WHEN** adult member's registration form is filled
- **THEN** an email address and phone number must be provided for the member

#### Scenario: Minor member's guardian contact details required

- **WHEN** registration form is filled for a minor (under 18)
- **THEN** an email address and phone number must be provided for the legal guardian
- **AND** the member's own email and phone are optional

#### Scenario: Missing contact information shows error

- **WHEN** admin submits a registration form with no email or no phone for either member or guardian
- **THEN** the form shows an error indicating that contact information is required

### Requirement: Optional Member Data

The system SHALL accept optional chip number and bank account number. Chip number must contain only digits.

#### Scenario: Member created with optional fields provided

- **WHEN** user provides chip number and/or bank account in the form
- **THEN** these values are saved with the member

#### Scenario: Member created without optional fields

- **WHEN** user omits the chip number and bank account fields
- **THEN** the member is created successfully without those values

#### Scenario: Invalid chip number shows error

- **WHEN** user enters a non-numeric chip number
- **THEN** the form shows an error that chip number must contain only digits

### Requirement: Bank Account Management

The system SHALL allow an optional bank account number in IBAN or Czech domestic format. The field is labelled as being for expense reimbursement.

#### Scenario: Bank account field is optional

- **WHEN** user views the bank account field in the member form
- **THEN** the field is marked as optional
- **AND** help text states the field is for reimbursement of travel expenses and other club-related costs

#### Scenario: Valid IBAN format accepted

- **WHEN** user enters a bank account in valid IBAN format (e.g., "CZ6508000000192000145399")
- **THEN** the form accepts the value

#### Scenario: IBAN with spaces accepted and normalized

- **WHEN** user enters an IBAN with spaces (e.g., "CZ65 0800 0000 1920 0014 5399")
- **THEN** the system accepts and stores it without spaces

#### Scenario: Valid Czech domestic format accepted

- **WHEN** user enters a bank account in Czech domestic format (e.g., "123456/0800")
- **THEN** the form accepts the value

#### Scenario: Invalid bank account format shows error

- **WHEN** user enters a bank account number in an invalid format
- **THEN** the form shows an error describing the validation issue

#### Scenario: Bank account can be removed

- **WHEN** user clears the bank account number field and saves
- **THEN** the bank account is removed from the member profile

### Requirement: Welcome Email on Registration

When a new member is registered, the system SHALL send a welcome email containing the member's name, registration number, and an activation link valid for 72 hours.

#### Scenario: Adult member receives welcome email

- **WHEN** admin registers an adult member with an email address
- **THEN** a welcome email is sent to that email address
- **AND** the email contains an activation link that expires in 72 hours

#### Scenario: Minor receives welcome email at guardian address

- **WHEN** admin registers a minor without a member email address
- **AND** guardian has an email address
- **THEN** the welcome email is sent to the guardian's email address

#### Scenario: Member without email does not receive welcome email

- **WHEN** admin registers a member without any email address
- **THEN** no welcome email is sent
- **AND** the registration still succeeds

### Requirement: Authorization for Member Operations

The system SHALL show or hide member actions based on the user's permissions.

#### Scenario: Register member button shown to authorized user

- **WHEN** user with MEMBERS:CREATE permission views the member list
- **THEN** the "Registrovat člena" button is displayed

#### Scenario: Unauthenticated user cannot access members

- **WHEN** unauthenticated user attempts to navigate to the members section
- **THEN** they are redirected to the login page

### Requirement: Member List

The system SHALL display a paginated and sortable list of members. Users with MEMBERS:MANAGE authority see all members including inactive ones and see email and active status. Other users see only active members.

#### Scenario: Admin views member list with email and status

- **WHEN** user with MEMBERS:MANAGE authority views the member list
- **THEN** each member row shows the member's email address and active status

#### Scenario: Regular user does not see email and active status

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** email and active status columns are not shown

#### Scenario: Admin sees all members including inactive

- **WHEN** user with MEMBERS:MANAGE authority views the member list
- **THEN** both active and inactive (suspended) members are shown

#### Scenario: Regular user sees only active members

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** only active members are shown in the list

#### Scenario: Admin sees suspend action for active member

- **WHEN** user with MEMBERS:MANAGE authority views an active member in the list
- **THEN** a "suspend membership" action is available for that member

#### Scenario: Admin sees resume action for inactive member

- **WHEN** user with MEMBERS:MANAGE authority views an inactive member in the list
- **THEN** a "resume membership" action is available for that member

#### Scenario: Regular user sees no management actions

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** no suspend, resume, or update actions are shown

#### Scenario: Permissions link shown for active members to authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views the member list
- **THEN** a permissions link is available for each active member

#### Scenario: Permissions link not shown for inactive members

- **WHEN** user with MEMBERS:PERMISSIONS authority views the member list
- **THEN** inactive members do not show a permissions link

#### Scenario: Member list is paginated

- **WHEN** user views the member list
- **THEN** the list shows 10 members per page by default
- **AND** pagination controls allow navigating between pages

#### Scenario: Member list is sortable

- **WHEN** user sorts the member list by first name, last name, or registration number
- **THEN** the list reorders accordingly

#### Scenario: Invalid sort field shows error

- **WHEN** user attempts to sort by an unsupported field (e.g., email)
- **THEN** the system shows an error listing the allowed sort fields (firstName, lastName, registrationNumber)

### Requirement: Member Detail

The system SHALL display complete member details. Inactive members are not accessible to users without MEMBERS:MANAGE authority.

#### Scenario: Authorized user views active member detail

- **WHEN** user with MEMBERS:READ permission navigates to a member's detail page
- **AND** the member is active
- **THEN** all member information is displayed including personal data, address, contact, and supplementary info

#### Scenario: Admin views suspended member detail

- **WHEN** user with MEMBERS:MANAGE authority navigates to an inactive member's detail page
- **THEN** the member detail is displayed including suspension reason, date, and note

#### Scenario: Regular user cannot access inactive member detail

- **WHEN** user without MEMBERS:MANAGE authority navigates to an inactive member's detail page
- **THEN** the page shows a not-found error

#### Scenario: Edit button shown to authorized user

- **WHEN** user with MEMBERS:UPDATE permission views a member detail page
- **THEN** an "Upravit profil" button is displayed

#### Scenario: Edit button not shown without permission

- **WHEN** user without MEMBERS:UPDATE permission views a member detail page
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

### Requirement: Member Detail Page Layout

The member detail page SHALL use a two-column layout driven by the available PATCH template fields, without client-side role detection.

#### Scenario: Detail page with full template (admin or self)

- **WHEN** member detail response includes a PATCH template
- **THEN** the page renders in a two-column layout: left column (personal data, contact, address), right column (supplementary info, documents and licenses)
- **AND** an "Upravit profil" button is shown

#### Scenario: Detail page without template (other member)

- **WHEN** member detail response has no PATCH template
- **THEN** only contact section and address section are displayed
- **AND** no action buttons are shown

#### Scenario: Admin detail shows action buttons with icons

- **WHEN** member detail response includes a full PATCH template (admin view)
- **THEN** the page header shows action buttons: "Upravit profil" (pencil icon), "Vložit / Vybrat" (banknote icon), "Oprávnění" (shield icon, visible only if permissions link present), "Ukončit členství" (user-x icon, red)

#### Scenario: Own profile shows membership and edit buttons

- **WHEN** member detail response includes a self-edit PATCH template (own profile view)
- **THEN** the page header shows: "Členské příspěvky" button and "Upravit profil" button
- **AND** "Oprávnění" and "Ukončit členství" buttons are NOT shown

### Requirement: Member Edit Form Layout

Edit forms SHALL place action buttons at the bottom of the form.

#### Scenario: Edit form actions are at the bottom

- **WHEN** user opens member edit form
- **THEN** "Zrušit" and "Uložit změny" buttons appear after all form sections
- **AND** admin edit shows a badge "Admin — editace všech polí" in the page header
- **AND** in self-edit mode, fields not present in the template are displayed as read-only

### Requirement: Member Registration Page Layout

The member registration page SHALL use a two-column layout.

#### Scenario: Registration page layout

- **WHEN** admin navigates to member registration page
- **THEN** the left column contains: personal data section, contact section, address section
- **AND** the right column contains supplementary information section only (no documents or licenses)
- **AND** "Zrušit" and "Registrovat člena" (user-plus icon) buttons appear at the bottom of the form

### Requirement: Member Update

The system SHALL allow members to update their own profile data, and admins to update any member's data. Field access is role-based.

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
- **THEN** the form shows fields: email, phone, address, chip number, nationality, bank account, guardian contact, identity card, driving license, medical course, trainer license, dietary restrictions

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

- **WHEN** member submits an update that would remove all email addresses (own and guardian)
- **THEN** the form shows an error that at least one email is required

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

### Requirement: Identity Card, Licenses, and Supplementary Fields

The system SHALL allow members to update identity card, driving license, medical course, trainer license, and dietary restrictions.

#### Scenario: Member updates identity card

- **WHEN** member provides a valid identity card number and validity date (not in the past)
- **THEN** the identity card information is saved

#### Scenario: Identity card with expired validity rejected

- **WHEN** member provides an identity card with a past validity date
- **THEN** the form shows an error that the ID card is expired

#### Scenario: Member updates medical course

- **WHEN** member provides a medical course completion date
- **THEN** the medical course is saved
- **AND** an optional validity date (must be after completion date) can also be provided

#### Scenario: Medical course validity before completion rejected

- **WHEN** member provides a validity date before the completion date for medical course
- **THEN** the form shows an error that validity must be after completion date

#### Scenario: Member updates trainer license

- **WHEN** member provides a trainer license number and validity date (not in the past)
- **THEN** the trainer license information is saved

#### Scenario: Member sets driving license group

- **WHEN** member selects a driving license group from the allowed values (B, BE, C, C1, D, D1, etc.)
- **THEN** the driving license group is saved

#### Scenario: Invalid driving license group rejected

- **WHEN** member provides a driving license group value not in the allowed set
- **THEN** the form shows an error listing valid driving license groups

#### Scenario: Member sets dietary restrictions

- **WHEN** member enters text in the dietary restrictions field (optional, max 500 characters)
- **THEN** the dietary restrictions are saved

#### Scenario: Member clears dietary restrictions

- **WHEN** member clears the dietary restrictions field and saves
- **THEN** dietary restrictions are removed from the profile

### Requirement: Membership Suspension

The system SHALL allow users with MEMBERS:UPDATE permission to suspend a member's membership. Before suspension, the system SHALL check group ownership.

#### Scenario: Admin suspends an active member

- **WHEN** user with MEMBERS:UPDATE permission submits the suspension form with a valid reason (ODHLASKA, PRESTUP, OTHER)
- **AND** the member is not the last owner of any user group
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

#### Scenario: Suspension proceeds after group ownership resolved

- **WHEN** admin resolves all group ownership conflicts
- **AND** resubmits the suspension request
- **THEN** the membership is suspended

#### Scenario: Suspended member detail shows suspension info

- **WHEN** admin views a suspended member's detail page
- **THEN** the suspension reason, date, and note (if provided) are displayed

#### Scenario: Unauthorized user cannot suspend

- **WHEN** user without MEMBERS:UPDATE permission views an active member's detail page
- **THEN** no "Ukončit členství" button is shown
