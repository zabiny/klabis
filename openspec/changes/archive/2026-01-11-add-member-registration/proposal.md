# Change: Add Member Registration

## Why

The club needs a way to manually register new members with their personal information, contact details, and
automatically provision them with system access. Currently, there is no member management capability in the system.

This is a foundational feature required before implementing event registration, financial tracking, and other
member-dependent functionality.

## What Changes

- Add member creation API endpoint with HATEOAS support (HAL+FORMS)
- Implement member data model with validation:
    - Personal information (name, date of birth, nationality, gender)
    - Registration number generation (XXXYYDD format where YY is birth year, e.g., ZBM0501)
    - Address and contact details (email, phone)
    - Optional chip number and bank account
    - Conditional logic for "rodne cislo" based on nationality (CZ only)
- Implement guardian contact data for minors
- Create authorization check for MEMBERS:CREATE permission
- Send welcome email with OAuth2 account activation link

## Out of Scope (Deferred)

The following features are intentionally excluded and will be added in separate changes:

- Training group assignment (added with user groups feature)
- ORIS/CUS integration (separate integration change)
- Distribution list management
- Member profile editing
- Member deactivation/activation functionality (database supports it via is_active field, but UI/API endpoints deferred)

## Impact

- **Affected specs**: `members` (new capability)
- **Affected code**:
    - New domain: `com.klabis.members.domain`
    - New API controller: `com.klabis.members.presentation.MemberController`
    - New service: `com.klabis.members.application.MemberRegistrationService`
    - Email service integration
- **External dependencies**:
    - SMTP server for welcome emails
    - Spring Authorization Server for OAuth2 account activation
- **Configuration**:
    - `klabis.club.code` property for registration number generation
    - SMTP settings for email
- **Security**: Requires MEMBERS:CREATE permission implementation
- **GDPR compliance**: Handling sensitive personal data (address, birth date, contacts, rodne cislo)

## Breaking Changes

None - this is a new capability.
