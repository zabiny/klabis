# Change: Add Member Self-Edit and Admin Edit API

**GitHub Issue**: https://github.com/zabiny/klabis/issues/4

## Why

Members need to be able to update their own personal information to keep their records current without requiring
administrator intervention. This self-service capability reduces administrative overhead and ensures data accuracy.

Additionally, administrators need the ability to correct and update core member information (name, date of birth) when
needed for data accuracy corrections.

## What Changes

- **NEW API Endpoint**: `PATCH /api/members/{id}` for updating member information
- **Dual Authorization Model**:
    - **Member Self-Edit**: Members can only edit their own information (based on OAuth2 subject match)
    - **Admin Edit**: Users with MEMBERS:UPDATE permission can edit any member's information

- **Member Self-Edit Fields** (limited set):
    - Chip number (Cislo cipu) - numeric only
    - Nationality - required field
    - Address (Ulice, city, postal code, country) - required field
    - Own contact (Email, Telefon) - at least one email and one phone required
    - Bank account number (cislo bankovniho účtu)
    - Legal guardian contact (email, Telefon)
    - ID card number and validity (cislo OP a platnost)
    - Dietary restrictions (dieta) - text field for food accommodation requests
    - Driving license group (skupina ridickeho opravneni: B, BE, C, D, etc.)
    - Medical course with validity (zdravotnicky kurz + platnost)
    - Trainer license with validity (trenerska licence a platnost)

- **Admin-Only Fields** (requires MEMBERS:UPDATE permission):
    - First name (Jmeno)
    - Last name (Prijmeni)
    - Date of birth

- **Validation Rules**:
    - At least one email address must be provided (either member's own or guardian's)
    - At least one phone number must be provided (either member's own or guardian's)
    - Czech nationality enables Rodne Cislo field (conditional)
    - All address fields required (street, city, postal code, country)

- **Response Format**: HAL+FORMS with updated member representation and hypermedia links
- **Error Handling**: Comprehensive validation with problem+json error responses

## Impact

- **Affected specs**: members (new update endpoint added with dual authorization model)
- **Affected code**:
    - MemberController (new PATCH endpoint)
    - MemberService (update logic with role-based field access control)
    - Member domain model (new value objects: IdentityCard, MedicalCourse, TrainerLicense; new fields:
      DrivingLicenseGroup enum, dietary restrictions text)
    - Authorization layer (self-edit vs admin edit permission checks, role-based field filtering)
    - HAL+FORMS templates (dynamic fields based on user role)
