package com.klabis.usergroups;

import com.klabis.members.MemberId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@DomainEvent
public record MemberAssignedToTrainingGroupEvent(
        UUID eventId,
        MemberId memberId,
        UserGroupId trainingGroupId,
        String trainingGroupName,
        Instant occurredAt
) {

    public MemberAssignedToTrainingGroupEvent {
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(memberId, "Member ID is required");
        Objects.requireNonNull(trainingGroupId, "Training group ID is required");
        Objects.requireNonNull(trainingGroupName, "Training group name is required");
        Objects.requireNonNull(occurredAt, "Occurred at is required");
    }

    public static MemberAssignedToTrainingGroupEvent of(MemberId memberId, UserGroupId trainingGroupId, String trainingGroupName) {
        return new MemberAssignedToTrainingGroupEvent(
                UUID.randomUUID(),
                memberId,
                trainingGroupId,
                trainingGroupName,
                Instant.now()
        );
    }

    @Override
    public String toString() {
        return "MemberAssignedToTrainingGroupEvent{eventId=" + eventId +
               ", memberId=" + memberId +
               ", trainingGroupId=" + trainingGroupId +
               ", trainingGroupName='" + trainingGroupName + '\'' +
               ", occurredAt=" + occurredAt + "}";
    }
}
