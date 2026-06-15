package com.klabis.membershipfees.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.time.LocalDate;

@ValueObject
public record FeeGroupMembership(
        MemberId memberId,
        LocalDate joinedAt,
        AssignmentSource source,
        @Nullable MemberId assignedBy
) {

    public FeeGroupMembership {
        Assert.notNull(memberId, "MemberId is required");
        Assert.notNull(joinedAt, "JoinedAt is required");
        Assert.notNull(source, "AssignmentSource is required");
    }
}
