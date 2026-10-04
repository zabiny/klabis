package com.klabis.members.infrastructure;

import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.MemberRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Every adult member may edit their own profile; a minor's data is maintained by guardians and
 * administrators. Age is evaluated against today, so the grant starts on the 18th birthday without
 * any intervention.
 */
@Component
class SelfProfileRelationshipSource implements RelationshipSource {

    private final MemberRepository memberRepository;

    SelfProfileRelationshipSource(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        return memberRepository.findById(MemberId.fromUserId(userId))
                .filter(member -> !member.getPersonalInformation().isMinor())
                .map(member -> Map.of(Authority.MEMBERS_EDIT_PROFILE, Set.of(TargetRef.member(userId.uuid()))))
                .orElseGet(Map::of);
    }
}
