package com.klabis.members.groups.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code TrainingGroup} aggregates.
 * Stub — full implementation comes in Phase 2.
 */
@ValueObject
public record TrainingGroupFilter() implements GroupFilter {

    public static TrainingGroupFilter all() {
        return new TrainingGroupFilter();
    }
}
