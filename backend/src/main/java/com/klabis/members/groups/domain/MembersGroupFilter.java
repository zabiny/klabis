package com.klabis.members.groups.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code MembersGroup} aggregates.
 * All fields are optional — null means no restriction on that dimension.
 * Multiple non-null fields are combined with AND.
 */
@ValueObject
public record MembersGroupFilter(
        MemberId ownerOrMemberIs,
        MemberId pendingInvitationFor
) implements GroupFilter {

    public static MembersGroupFilter all() {
        return new MembersGroupFilter(null, null);
    }

    public MembersGroupFilter withOwnerOrMemberIs(MemberId memberId) {
        return new MembersGroupFilter(memberId, this.pendingInvitationFor);
    }

    public MembersGroupFilter withPendingInvitationFor(MemberId memberId) {
        return new MembersGroupFilter(this.ownerOrMemberIs, memberId);
    }
}
