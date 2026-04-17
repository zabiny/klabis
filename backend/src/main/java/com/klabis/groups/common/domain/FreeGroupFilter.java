package com.klabis.groups.common.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Filter criteria for querying {@code FreeGroup} aggregates.
 * All fields are optional — null means no restriction on that dimension.
 * Multiple non-null fields are combined with AND.
 */
@ValueObject
public record FreeGroupFilter(
        MemberId ownerOrMemberIs,
        MemberId pendingInvitationFor
) implements GroupFilter {

    public static FreeGroupFilter all() {
        return new FreeGroupFilter(null, null);
    }

    public FreeGroupFilter withOwnerOrMemberIs(MemberId memberId) {
        return new FreeGroupFilter(memberId, this.pendingInvitationFor);
    }

    public FreeGroupFilter withPendingInvitationFor(MemberId memberId) {
        return new FreeGroupFilter(this.ownerOrMemberIs, memberId);
    }
}
