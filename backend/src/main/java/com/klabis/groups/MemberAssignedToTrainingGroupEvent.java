package com.klabis.groups;

import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.members.MemberId;
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
