package com.klabis.groups.traininggroup.application;

import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * Full end-state snapshot for a training group update. Every field carries a concrete value;
 * callers take {@link #from(TrainingGroup)} as the baseline and overlay only the fields their
 * PATCH request changed. The "undefined vs. present-null" distinction lives in the REST mapper,
 * not here — see backend-patterns rest-adapter.md "PATCH endpoints".
 */
@RecordBuilder
public record UpdateTrainingGroupCommand(
        String name,
        AgeRange ageRange,
        Set<MemberId> trainers
) {
    public UpdateTrainingGroupCommand {
        Assert.hasText(name, "name is required");
        Assert.notNull(ageRange, "ageRange is required");
        Assert.notEmpty(trainers, "at least one trainer is required");
    }

    /** The baseline command: every field at the group's current value, so applying it is a no-op. */
    public static UpdateTrainingGroupCommand from(TrainingGroup group) {
        return new UpdateTrainingGroupCommand(
                group.getName(),
                group.getAgeRange(),
                Set.copyOf(group.getTrainers()));
    }
}
