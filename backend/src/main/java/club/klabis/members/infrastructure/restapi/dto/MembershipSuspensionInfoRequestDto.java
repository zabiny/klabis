package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * MembershipSuspensionInfoApiDto
 */

@JsonTypeName("MembershipSuspensionRequest")
@RecordBuilder
public record MembershipSuspensionInfoRequestDto(

        @Schema(name = "force", description = "tells if member account should be suspended even when there are some unfinished things (canSuspend=false)", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, defaultValue = "false")
        boolean force

) {

}

