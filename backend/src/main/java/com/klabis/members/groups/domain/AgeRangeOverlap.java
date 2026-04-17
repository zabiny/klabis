package com.klabis.members.groups.domain;

import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * Overlap predicate bundling an age range with an optional group to exclude.
 * Used in {@link TrainingGroupFilter#withOverlap(AgeRangeOverlap)} to detect
 * conflicting age-range assignments while optionally excluding the group being edited.
 */
@ValueObject
public record AgeRangeOverlap(AgeRange range, TrainingGroupId excludeId) {

    public AgeRangeOverlap {
        Assert.notNull(range, "range must not be null");
    }
}
