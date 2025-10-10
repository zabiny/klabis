package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * MembershipSuspensionInfoApiDto
 */

@JsonTypeName("MembershipSuspensionInfo")
@RecordBuilder
public record MembershipSuspensionInfoApiDto(


        @Schema(name = "isSuspended", description = "tells if member account is currently suspended", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        boolean isSuspended,

        @Schema(name = "canSuspend", description = "tells if member account can be suspended", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        boolean canSuspend,

        @NotNull
        @Valid
        @Schema(name = "details", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        SuspendMembershipBlockersApiDto details,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @JsonUnwrapped
        MembershipSuspensionInfoRequestDto requestDto

) {

}

