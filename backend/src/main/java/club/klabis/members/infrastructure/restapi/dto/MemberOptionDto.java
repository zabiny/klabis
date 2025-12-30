package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Member option DTO for HAL+Forms field selection
 * <p>
 * Used by the /members/options endpoint to return members formatted as options
 * for dropdown/select field controls in forms.
 */
public record MemberOptionDto(
        @NotNull
        @Schema(name = "value", description = "Member ID value for the option", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("value")
        Integer value,

        @NotNull
        @Schema(name = "prompt", description = "Display text for the option", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("prompt")
        String prompt
) {
}
