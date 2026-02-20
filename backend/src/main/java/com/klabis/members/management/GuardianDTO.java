package com.klabis.members.management;

import com.klabis.members.domain.GuardianInformation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Shared DTO for guardian information.
 * <p>
 * This DTO is used across application and presentation layers to eliminate duplication.
 * Contains guardian personal information and contact details.
 * <p>
 * Used by:
 * - RegisterMemberCommand (application layer)
 * - RegisterMemberRequest (presentation layer)
 * - MemberDetailsResponse (presentation layer)
 */
@Schema(description = "Guardian information for minors")
public record GuardianDTO(
        @Schema(description = "Guardian's first name", example = "Pavel", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian first name is required")
        @Size(max = 100, message = "Guardian first name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Guardian's last name", example = "Novák", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian last name is required")
        @Size(max = 100, message = "Guardian last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Relationship to member (e.g., PARENT, LEGAL_GUARDIAN)", example = "PARENT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian relationship is required")
        @Size(max = 50, message = "Guardian relationship must not exceed 50 characters")
        String relationship,

        @Schema(description = "Guardian's email address", example = "pavel.novak@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian email is required")
        @Email(message = "Invalid guardian email format")
        String email,

        @Schema(description = "Guardian's phone number", example = "+420987654321", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian phone is required")
        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Invalid guardian phone number format")
        String phone
) {
    /**
     * Creates a GuardianDTO from a GuardianInformation domain object.
     *
     * @param guardian the domain GuardianInformation object
     * @return GuardianDTO with all guardian information, or null if input is null
     */
    public static GuardianDTO from(GuardianInformation guardian) {
        if (guardian == null) {
            return null;
        }

        return new GuardianDTO(
                guardian.getFirstName(),
                guardian.getLastName(),
                guardian.getRelationship(),
                guardian.getEmail().value(),
                guardian.getPhone().value()
        );
    }
}
