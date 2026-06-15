package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.application.MemberFeeHistoryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record MemberFeeHistoryResponse(List<AssignmentResponse> assignments) {

    record AssignmentResponse(
            int year,
            UUID groupId,
            String groupName,
            LocalDate joinedAt,
            String source
    ) {}

    static MemberFeeHistoryResponse from(List<MemberFeeHistoryPort.LevelAssignment> assignments) {
        return new MemberFeeHistoryResponse(
                assignments.stream()
                        .map(a -> new AssignmentResponse(
                                a.year(),
                                a.groupId().value(),
                                a.groupName(),
                                a.joinedAt(),
                                a.source().name()))
                        .toList());
    }
}
