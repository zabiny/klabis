package com.klabis.members.infrastructure;

import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.PersonalInformation;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Grants a user permissions over themself, replacing the former owner-visible rule. Every member may
 * register themself for events; only an adult may edit their own profile (a minor's data is maintained
 * by guardians and administrators). Age is evaluated against today, so the edit grant starts on the
 * 18th birthday without any intervention.
 */
@Component
class SelfRelationshipSource implements RelationshipSource {

    private final MemberRepository memberRepository;

    SelfRelationshipSource(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        return memberRepository.findDateOfBirth(MemberId.fromUserId(userId))
                .map(dateOfBirth -> grantsOverSelf(userId, dateOfBirth))
                .orElseGet(Map::of);
    }

    private Map<Authority, Set<TargetRef>> grantsOverSelf(UserId userId, LocalDate dateOfBirth) {
        Set<TargetRef> self = Set.of(TargetRef.member(userId.uuid()));
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        grants.put(Authority.EVENTS_REGISTER_MEMBER, self);
        if (!PersonalInformation.isMinor(dateOfBirth)) {
            grants.put(Authority.MEMBERS_EDIT_PROFILE, self);
        }
        return grants;
    }
}
