package com.klabis.members.groups.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code FamilyGroup} aggregates.
 * Stub — full implementation comes in Phase 3.
 */
@ValueObject
public record FamilyGroupFilter() implements GroupFilter {

    public static FamilyGroupFilter all() {
        return new FamilyGroupFilter();
    }
}
