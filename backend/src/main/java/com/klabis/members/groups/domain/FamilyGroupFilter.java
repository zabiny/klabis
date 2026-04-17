package com.klabis.members.groups.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code FamilyGroup} aggregates.
 * All fields are optional — null means no restriction on that dimension.
 */
@ValueObject
public record FamilyGroupFilter(
        MemberId memberOrParentIs
) implements GroupFilter {

    public static FamilyGroupFilter all() {
        return new FamilyGroupFilter(null);
    }

    public FamilyGroupFilter withMemberOrParentIs(MemberId memberId) {
        return new FamilyGroupFilter(memberId);
    }
}
