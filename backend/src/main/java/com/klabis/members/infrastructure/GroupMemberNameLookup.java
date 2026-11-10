package com.klabis.members.infrastructure;

import com.klabis.common.groups.domain.MemberNameLookup;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class GroupMemberNameLookup implements MemberNameLookup {

    private final Members members;

    GroupMemberNameLookup(Members members) {
        this.members = members;
    }

    @Override
    public Optional<String> nameOf(Object memberId) {
        return memberIdOf(memberId)
                .flatMap(members::findById)
                .map(member -> member.firstName() + " " + member.lastName());
    }

    private static Optional<MemberId> memberIdOf(Object memberId) {
        return switch (memberId) {
            case MemberId id -> Optional.of(id);
            case UserId id -> Optional.of(MemberId.fromUserId(id));
            default -> Optional.empty();
        };
    }
}
