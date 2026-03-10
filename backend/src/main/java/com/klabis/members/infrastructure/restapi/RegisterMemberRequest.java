package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.Gender;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Request DTO for member registration endpoint.
 * <p>
 * Represents the JSON payload for POST /api/members
 */
@RecordBuilder
@Schema(description = "Member registration request")
public record RegisterMemberRequest(
        @Schema(description = "Member's first name", example = "Jan", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Member's last name", example = "Novák", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Member's date of birth", example = "2005-05-15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Schema(description = "Nationality (ISO 3166-1 alpha-2 or alpha-3 code)", example = "CZ", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Nationality is required")
        @Size(min = 2, max = 3, message = "Nationality must be 2 or 3 characters (ISO code)")
        String nationality,

        @Schema(description = "Member's gender", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Gender is required")
        Gender gender,

        @Schema(description = "Email address", example = "jan.novak@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Phone number", example = "+420777123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[0-9\\s]{7,20}$", message = "Phone number must be in E.164 format (starts with +)")
        String phone,

        @Schema(description = "Postal address", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotNull(message = "Address is required")
        AddressRequest address,

        @Schema(description = "Guardian information (required for minors under 18)")
        @Valid
        GuardianDTO guardian,

        @Schema(description = "Birth number (rodné číslo) - only for Czech nationals, format RRMMDD/XXXX or RRMMDDXXXX", example = "900101/1234")
        String birthNumber,

        @Schema(description = "Bank account number (IBAN or domestic Czech format)", example = "CZ6508000000192000145399")
        String bankAccountNumber
) {
}
