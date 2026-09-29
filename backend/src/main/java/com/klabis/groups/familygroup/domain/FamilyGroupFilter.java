package com.klabis.groups.familygroup.domain;

import com.klabis.common.users.UserId;
import com.klabis.groups.common.domain.GroupFilter;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code FamilyGroup} aggregates.
 * All fields are optional — null means no restriction on that dimension.
 */
@ValueObject
public record FamilyGroupFilter(
        UserId memberOrParentIs
) implements GroupFilter {

    public static FamilyGroupFilter all() {
        return new FamilyGroupFilter(null);
    }

    public FamilyGroupFilter withMemberOrParentIs(UserId userId) {
        return new FamilyGroupFilter(userId);
    }
}
