package com.klabis.members.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.common.validation.ValidPatchFieldNotBlank;
import com.klabis.common.validation.ValidPatchFieldPattern;
import com.klabis.common.validation.ValidPatchFieldSize;
import com.klabis.members.application.*;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;

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
        PatchField<String> email,

        PatchField<String> phone,

        PatchField<AddressRequest> address,

        @ValidPatchFieldNotBlank(message = "First name must not be blank")
        PatchField<String> firstName,

        @ValidPatchFieldNotBlank(message = "Last name must not be blank")
        PatchField<String> lastName,

        PatchField<LocalDate> dateOfBirth,

        PatchField<Gender> gender,

        @ValidPatchFieldPattern(regexp = ValidationPatterns.NUMERIC_ONLY_PATTERN, message = "Chip number must contain only digits")
        PatchField<String> chipNumber,

        PatchField<String> nationality,

        PatchField<IdentityCardDto> identityCard,

        PatchField<MedicalCourseDto> medicalCourse,

        PatchField<TrainerLicenseDto> trainerLicense,

        PatchField<RefereeLicenseDto> refereeLicense,

        PatchField<DrivingLicenseGroup> drivingLicenseGroup,

        @ValidPatchFieldSize(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        PatchField<String> dietaryRestrictions,

        PatchField<String> birthNumber,

        PatchField<String> bankAccountNumber,

        PatchField<GuardianDTO> guardian
) {
}
