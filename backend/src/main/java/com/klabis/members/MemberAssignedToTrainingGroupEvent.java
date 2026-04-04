package com.klabis.members;

import com.klabis.members.traininggroup.domain.TrainingGroupId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;

@RecordBuilder
@DomainEvent
public record MemberAssignedToTrainingGroupEvent(
        MemberId memberId,
        TrainingGroupId groupId,
        String groupName,
        Instant occurredAt
) {
}
