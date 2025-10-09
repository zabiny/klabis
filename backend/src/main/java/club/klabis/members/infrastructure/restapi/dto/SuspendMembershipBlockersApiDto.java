package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * describes conditions which may prevent membership suspension and their actual status
 */

@Schema(name = "SuspendMembershipBlockers", description = "describes conditions which may prevent membership suspension and their actual status")
@JsonTypeName("SuspendMembershipBlockers")
@RecordBuilder
public record SuspendMembershipBlockersApiDto(

        @NotNull
        @Valid
        @Schema(name = "finance", requiredMode = Schema.RequiredMode.REQUIRED)
        SuspendMembershipBlockersFinanceApiDto finance

) {

}

