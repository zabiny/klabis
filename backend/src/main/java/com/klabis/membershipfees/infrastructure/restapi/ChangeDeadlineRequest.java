package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

record ChangeDeadlineRequest(
        @NotNull LocalDate votingDeadline
) {
    FeeSelectionCampaignManagementPort.ChangeDeadlineCommand toCommand() {
        return new FeeSelectionCampaignManagementPort.ChangeDeadlineCommand(votingDeadline);
    }
}
