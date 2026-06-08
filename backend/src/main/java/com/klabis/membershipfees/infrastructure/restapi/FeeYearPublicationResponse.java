package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.FeeYearPublication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record FeeYearPublicationResponse(
        UUID id,
        int year,
        LocalDate votingDeadline,
        Instant deadlineProcessedAt
) {
    static FeeYearPublicationResponse from(FeeYearPublication publication) {
        return new FeeYearPublicationResponse(
                publication.getId().uuid(),
                publication.getYear(),
                publication.getVotingDeadline(),
                publication.getDeadlineProcessedAt()
        );
    }
}
