package com.klabis.members.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.common.validation.ValidPatchFieldSize;
import com.klabis.members.domain.DrivingLicenseGroup;

/**
 * Request DTO for member self-edit (PATCH /api/members/{id}/self).
 * <p>
 * Contains only member-editable fields. Admin-only fields (firstName, lastName,
 * dateOfBirth, gender, birthNumber) are intentionally excluded so the HAL+FORMS
 * template does not advertise them to members viewing their own profile.
 */
record SelfUpdateMemberRequest(
        PatchField<String> email,

        PatchField<String> phone,

        PatchField<AddressRequest> address,

        PatchField<String> chipNumber,

        PatchField<String> nationality,

        PatchField<String> bankAccountNumber,

        PatchField<GuardianDTO> guardian,

        PatchField<IdentityCardDto> identityCard,

        PatchField<DrivingLicenseGroup> drivingLicenseGroup,

        PatchField<MedicalCourseDto> medicalCourse,

        PatchField<TrainerLicenseDto> trainerLicense,

        PatchField<RefereeLicenseDto> refereeLicense,

        @ValidPatchFieldSize(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        PatchField<String> dietaryRestrictions
) {
}
