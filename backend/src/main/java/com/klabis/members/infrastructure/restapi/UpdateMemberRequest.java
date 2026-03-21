package com.klabis.members.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.members.application.*;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for updating a member's information.
 * <p>
 * This DTO is used for PATCH requests to update member information.
 * All fields use PatchField to support partial updates. Only the fields that are
 * provided will be updated.
 * <p>
 * Validation rules:
 * - firstName: if provided, must not be blank
 * - lastName: if provided, must not be blank
 * - dateOfBirth: if provided, must not be in the future
 * - chipNumber: if provided, must be numeric only
 * - dietaryRestrictions: if provided, max 500 characters
 * - Value objects (IdentityCard, MedicalCourse, TrainerLicense, Address) validate themselves
 * <p>
 * Some fields may be restricted to admin-only users (enforced at application layer).
 *
 * @param email               Member's new email address (optional)
 * @param phone               Member's new phone number in E.164 format (optional)
 * @param address             Member's new postal address (optional)
 * @param firstName           Member's new first name (optional, admin-only)
 * @param lastName            Member's new last name (optional, admin-only)
 * @param dateOfBirth         Member's new date of birth (optional, admin-only)
 * @param gender              Member's new gender (optional, admin-only)
 * @param chipNumber          Member's new chip number (optional, admin-only, numeric only)
 * @param nationality         Member's new nationality (optional)
 * @param identityCard        Member's new identity card information (optional, admin-only)
 * @param medicalCourse       Member's new medical course information (optional, admin-only)
 * @param trainerLicense      Member's new trainer license information (optional, admin-only)
 * @param drivingLicenseGroup Member's new driving license group (optional, admin-only)
 * @param dietaryRestrictions Member's new dietary restrictions (optional, max 500 chars)
 * @param birthNumber         Member's new birth number (optional, admin-only, Czech nationals only)
 * @param bankAccountNumber   Member's new bank account number (optional, admin-only)
 * @param guardian            Member's new guardian information (optional)
 */
@RecordBuilder
public record UpdateMemberRequest(
        @Size(max = 255, message = "Email must not exceed 255 characters")
        PatchField<String> email,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        PatchField<String> phone,

        @Valid
        PatchField<AddressRequest> address,

        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        PatchField<String> firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        PatchField<String> lastName,

        PatchField<LocalDate> dateOfBirth,

        PatchField<Gender> gender,

        @Pattern(regexp = ValidationPatterns.NUMERIC_ONLY_PATTERN, message = "Chip number must contain only digits")
        @Size(max = 50, message = "Chip number must not exceed 50 characters")
        PatchField<String> chipNumber,

        @Size(max = 3, message = "Nationality must not exceed 3 characters")
        PatchField<String> nationality,

        @Valid
        PatchField<IdentityCardDto> identityCard,

        PatchField<MedicalCourseDto> medicalCourse,

        PatchField<TrainerLicenseDto> trainerLicense,

        PatchField<RefereeLicenseDto> refereeLicense,

        PatchField<DrivingLicenseGroup> drivingLicenseGroup,

        @Size(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        PatchField<String> dietaryRestrictions,

        PatchField<String> birthNumber,

        @Size(max = 50, message = "Bank account number must not exceed 50 characters")
        PatchField<String> bankAccountNumber,

        PatchField<GuardianDTO> guardian
) {
}
