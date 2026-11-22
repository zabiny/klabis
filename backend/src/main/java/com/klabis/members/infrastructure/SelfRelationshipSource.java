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
import java.util.Optional;
import java.util.Set;

/**
 * Grants a user permissions over themself, replacing the former owner-visible rule. Every user may view their
 * own profile; every user except a minor member may edit it (a minor's data is maintained by guardians and
 * administrators), which also covers a non-member legal guardian's own profile. Only a member may register
 * themself for events. Age is evaluated against today, so the edit grant starts on the 18th birthday
 * without any intervention.
 */
@Component
class SelfRelationshipSource implements RelationshipSource {

    private final MemberRepository memberRepository;

    SelfRelationshipSource(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        Set<TargetRef> self = Set.of(TargetRef.member(userId.uuid()));
        Optional<LocalDate> dateOfBirth = memberRepository.findDateOfBirth(MemberId.fromUserId(userId));
        boolean minor = dateOfBirth.map(PersonalInformation::isMinor).orElse(false);

        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        grants.put(Authority.MEMBERS_VIEW_PROFILE, self);
        if (dateOfBirth.isPresent()) {
            grants.put(Authority.EVENTS_REGISTER_MEMBER, self);
        }
        if (!minor) {
            grants.put(Authority.MEMBERS_EDIT_PROFILE, self);
        }
        return grants;
    }
}
