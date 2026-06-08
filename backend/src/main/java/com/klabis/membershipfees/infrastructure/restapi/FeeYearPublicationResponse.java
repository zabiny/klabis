package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.FeeYearPublication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record FeeYearPublicationResponse(
        UUID id,
        int year,
        LocalDate votingDeadline,
        Instant deadlineProcessedAt,
        List<UUID> publishedGroupIds
) {
    static FeeYearPublicationResponse from(FeeYearPublication publication) {
        return new FeeYearPublicationResponse(
                publication.getId().uuid(),
                publication.getYear(),
                publication.getVotingDeadline(),
                publication.getDeadlineProcessedAt(),
                publication.getPublishedGroupIds().stream().map(id -> id.uuid()).toList()
        );
    }
}
