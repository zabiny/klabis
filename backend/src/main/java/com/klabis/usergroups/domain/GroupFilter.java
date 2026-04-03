package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record GroupFilter(
        UserGroupId id,
        GroupType type,
        MemberId owner,
        MemberId member,
        MemberId pendingInvitationForMember,
        MemberId memberOrOwner
) {

    public static GroupFilter all() {
        return new GroupFilter(null, null, null, null, null, null);
    }

    public static GroupFilter byId(UserGroupId id) {
        return new GroupFilter(id, null, null, null, null, null);
    }

    public static GroupFilter byMember(MemberId memberId) {
        return new GroupFilter(null, null, null, memberId, null, null);
    }

    public static GroupFilter byOwner(MemberId memberId) {
        return new GroupFilter(null, null, memberId, null, null, null);
    }

    public static GroupFilter byType(GroupType type) {
        return new GroupFilter(null, type, null, null, null, null);
    }

    public static GroupFilter byTypeAndMember(GroupType type, MemberId memberId) {
        return new GroupFilter(null, type, null, memberId, null, null);
    }

    public static GroupFilter byTypeAndMemberOrOwner(GroupType type, MemberId memberId) {
        return new GroupFilter(null, type, null, null, null, memberId);
    }

    public static GroupFilter byPendingInvitation(MemberId memberId) {
        return new GroupFilter(null, null, null, null, memberId, null);
    }
}
