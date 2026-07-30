package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.domain.DrivingLicenseGroup;
import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

@RecordBuilder
public record UpdateMemberRequest(
        @Size(max = 255, message = "Email must not exceed 255 characters")
        JsonNullable<String> email,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        JsonNullable<String> phone,

        @Valid
        JsonNullable<AddressRequest> address,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        JsonNullable<String> firstName,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        @NotBlank(message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        JsonNullable<String> lastName,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        JsonNullable<LocalDate> dateOfBirth,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        JsonNullable<Gender> gender,

        @Pattern(regexp = "^[0-9]+$", message = "Chip number must contain only digits")
        @Size(max = 50, message = "Chip number must not exceed 50 characters")
        JsonNullable<String> chipNumber,

        @Size(min = 2, max = 2, message = "Nationality must be a 2-letter ISO 3166-1 alpha-2 code")
        JsonNullable<String> nationality,

        @Valid
        JsonNullable<IdentityCardDto> identityCard,

        JsonNullable<MedicalCourseDto> medicalCourse,

        JsonNullable<TrainerLicenseDto> trainerLicense,

        JsonNullable<RefereeLicenseDto> refereeLicense,

        JsonNullable<DrivingLicenseGroup> drivingLicenseGroup,

        @Size(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        JsonNullable<String> dietaryRestrictions,

        @HasAuthority(Authority.MEMBERS_MANAGE)
        JsonNullable<String> birthNumber,

        @Size(max = 50, message = "Bank account number must not exceed 50 characters")
        JsonNullable<String> bankAccountNumber,

        JsonNullable<GuardianDTO> guardian
) {
}
