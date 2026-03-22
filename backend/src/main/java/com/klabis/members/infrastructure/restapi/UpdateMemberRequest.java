package com.klabis.members.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.application.ValidationPatterns;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@RecordBuilder
public record UpdateMemberRequest(
        @Size(max = 255, message = "Email must not exceed 255 characters")
        PatchField<String> email,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        PatchField<String> phone,

        @Valid
        PatchField<AddressRequest> address,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        PatchField<String> firstName,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        @NotBlank(message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        PatchField<String> lastName,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        PatchField<LocalDate> dateOfBirth,

        @HasAuthority(Authority.MEMBERS_MANAGE)
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

        @HasAuthority(Authority.MEMBERS_MANAGE)
        PatchField<String> birthNumber,

        @Size(max = 50, message = "Bank account number must not exceed 50 characters")
        PatchField<String> bankAccountNumber,

        PatchField<GuardianDTO> guardian
) {
}
