package com.klabis.members.management;

import com.klabis.common.validation.ValidOptionalNotBlank;
import com.klabis.common.validation.ValidOptionalPattern;
import com.klabis.common.validation.ValidOptionalSize;
import com.klabis.members.DrivingLicenseGroup;
import com.klabis.members.Gender;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Request DTO for updating a member's information.
 * <p>
 * This DTO is used for PATCH requests to update member information.
 * All fields are optional to support partial updates. Only the fields that are
 * provided (non-null) will be updated.
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
 * @param identityCard        Member's new identity card information (optional, admin-only)
 * @param medicalCourse       Member's new medical course information (optional, admin-only)
 * @param trainerLicense      Member's new trainer license information (optional, admin-only)
 * @param drivingLicenseGroup Member's new driving license group (optional, admin-only)
 * @param dietaryRestrictions Member's new dietary restrictions (optional, max 500 chars)
 */
record UpdateMemberRequest(
        Optional<String> email,

        Optional<String> phone,

        Optional<AddressRequest> address,

        @ValidOptionalNotBlank(message = "First name must not be blank")
        Optional<String> firstName,

        @ValidOptionalNotBlank(message = "Last name must not be blank")
        Optional<String> lastName,

        Optional<LocalDate> dateOfBirth,

        Optional<Gender> gender,

        @ValidOptionalPattern(regexp = ValidationPatterns.NUMERIC_ONLY_PATTERN, message = "Chip number must contain only digits")
        Optional<String> chipNumber,

        Optional<IdentityCardDto> identityCard,

        Optional<MedicalCourseDto> medicalCourse,

        Optional<TrainerLicenseDto> trainerLicense,

        Optional<DrivingLicenseGroup> drivingLicenseGroup,

        @ValidOptionalSize(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        Optional<String> dietaryRestrictions
) {
}
