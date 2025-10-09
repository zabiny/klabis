package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * SuspendMembershipBlockersFinanceApiDto
 */

@JsonTypeName("SuspendMembershipBlockers_finance")
@RecordBuilder
public record SuspendMembershipBlockersFinanceApiDto(

        @NotNull
        @Schema(name = "status", description = "tells if finance account balance permits membership suspension", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean status

) {

}

