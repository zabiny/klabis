package com.klabis.groups.common.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code TrainingGroup} aggregates.
 * All fields are optional — null means no restriction on that dimension.
 * Multiple non-null fields are combined with AND.
 */
@ValueObject
public record TrainingGroupFilter(
        MemberId memberIs,
        MemberId trainerIs,
        AgeRangeOverlap overlap
) implements GroupFilter {

    public static TrainingGroupFilter all() {
        return new TrainingGroupFilter(null, null, null);
    }

    public TrainingGroupFilter withMemberIs(MemberId memberId) {
        return new TrainingGroupFilter(memberId, this.trainerIs, this.overlap);
    }

    public TrainingGroupFilter withTrainerIs(MemberId trainerId) {
        return new TrainingGroupFilter(this.memberIs, trainerId, this.overlap);
    }

    public TrainingGroupFilter withOverlap(AgeRangeOverlap overlap) {
        return new TrainingGroupFilter(this.memberIs, this.trainerIs, overlap);
    }
}
