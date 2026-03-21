package com.klabis.members.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.common.validation.ValidPatchFieldSize;
import com.klabis.members.domain.DrivingLicenseGroup;
import jakarta.validation.Valid;

/**
 * Request DTO for member self-edit (PATCH /api/members/{id}/self).
 * <p>
 * Contains only member-editable fields. Admin-only fields (firstName, lastName,
 * dateOfBirth, gender, birthNumber) are intentionally excluded so the HAL+FORMS
 * template does not advertise them to members viewing their own profile.
 */
record SelfUpdateMemberRequest(
        @ValidPatchFieldSize(max = 255, message = "Email must not exceed 255 characters")
        PatchField<String> email,

        @ValidPatchFieldSize(max = 50, message = "Phone must not exceed 50 characters")
        PatchField<String> phone,

        PatchField<AddressRequest> address,

        @ValidPatchFieldSize(max = 50, message = "Chip number must not exceed 50 characters")
        PatchField<String> chipNumber,

        @ValidPatchFieldSize(max = 3, message = "Nationality must not exceed 3 characters")
        PatchField<String> nationality,

        @ValidPatchFieldSize(max = 50, message = "Bank account number must not exceed 50 characters")
        PatchField<String> bankAccountNumber,

        PatchField<GuardianDTO> guardian,

        @Valid
        PatchField<IdentityCardDto> identityCard,

        PatchField<DrivingLicenseGroup> drivingLicenseGroup,

        PatchField<MedicalCourseDto> medicalCourse,

        PatchField<TrainerLicenseDto> trainerLicense,

        PatchField<RefereeLicenseDto> refereeLicense,

        @ValidPatchFieldSize(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        PatchField<String> dietaryRestrictions
) {
}
