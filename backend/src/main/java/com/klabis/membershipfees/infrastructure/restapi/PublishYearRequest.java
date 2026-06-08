package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record PublishYearRequest(
        @Positive int year,
        @NotNull LocalDate votingDeadline,
        @NotEmpty List<UUID> levelIds
) {
    FeeYearPublicationManagementPort.PublishYearCommand toCommand() {
        List<MembershipFeeLevelId> ids = levelIds.stream()
                .map(MembershipFeeLevelId::new)
                .toList();
        return new FeeYearPublicationManagementPort.PublishYearCommand(year, votingDeadline, ids);
    }
}
