## MODIFIED Requirements

### Requirement: Member Detail Response — Conditional Edit Template

The system SHALL include a PATCH template in the member detail response only for users authorized to edit the member, and the template SHALL contain only the fields the caller is permitted to modify.

- Users with MEMBERS:MANAGE authority: template contains all editable fields (firstName, lastName, dateOfBirth, gender, nationality, birthNumber, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup, email, phone, address, dietaryRestrictions, bankAccountNumber, guardian)
- Users viewing their own profile (without MEMBERS:MANAGE): template contains only self-editable fields (email, phone, address, dietaryRestrictions)
- Users viewing another member's profile (without MEMBERS:MANAGE): no PATCH template is included

#### Scenario: Admin retrieves member detail

- **WHEN** user with MEMBERS:MANAGE authority requests GET /api/members/{id}
- **THEN** response includes a PATCH template with all editable fields
- **AND** response includes personal data, supplementary info, and documents sections

#### Scenario: Member retrieves own profile

- **WHEN** authenticated member requests GET /api/members/{id} where id matches their own member profile
- **THEN** response includes a PATCH template with only self-editable fields: email, phone, address, dietaryRestrictions
- **AND** personal data, supplementary info, and documents are included in the response

#### Scenario: Member retrieves another member's profile

- **WHEN** authenticated member requests GET /api/members/{id} for a different member
- **THEN** response does NOT include a PATCH template
- **AND** contact and address data are included in the response

## ADDED Requirements

### Requirement: Member Detail Page — Layout Matches Mockup

The member detail page SHALL use a two-column layout and display content according to the available HAL template fields, without any client-side role detection logic.

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

### Requirement: Member Edit Form — Action Bar at Bottom

Edit forms (admin edit, self edit) SHALL place action buttons at the bottom of the form, not at the top.

#### Scenario: Edit form actions are at the bottom

- **WHEN** user opens member edit form
- **THEN** "Zrušit" and "Uložit změny" buttons appear after all form sections
- **AND** admin edit shows a badge "Admin — editace všech polí" in the page header
- **AND** in self-edit mode, fields not present in the template are displayed as read-only

### Requirement: Member Registration Page — Two-Column Layout

The member registration page SHALL use a two-column layout matching the approved mockup (`NIoA4` in `pencil/klabis-members.pen`).

#### Scenario: Registration page layout

- **WHEN** admin navigates to member registration page
- **THEN** the left column contains: personal data section, contact section, address section
- **AND** the right column contains supplementary information section only (no documents or licenses)
- **AND** "Zrušit" and "Registrovat člena" (user-plus icon) buttons appear at the bottom of the form
