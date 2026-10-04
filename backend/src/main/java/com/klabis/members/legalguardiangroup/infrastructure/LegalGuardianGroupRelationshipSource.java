package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Guardians hold what the legal guardian group delegates over every minor of the group. Age is deliberately not
 * checked: a minor turning 18 leaves the group in the daily run, so the guardian's grant ends with it.
 */
@Component
class LegalGuardianGroupRelationshipSource implements RelationshipSource {

    private final LegalGuardianGroupRepository groups;

    LegalGuardianGroupRelationshipSource(LegalGuardianGroupRepository groups) {
        this.groups = groups;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        for (LegalGuardianGroup group : groups.findAll(LegalGuardianGroupFilter.all().withGuardianIs(userId))) {
            Set<TargetRef> minors = minorsOf(group);
            if (minors.isEmpty()) {
                continue;
            }
            for (Authority authority : group.delegatedAuthorities()) {
                grants.computeIfAbsent(authority, key -> new HashSet<>()).addAll(minors);
            }
        }
        return grants;
    }

    private static Set<TargetRef> minorsOf(LegalGuardianGroup group) {
        Set<TargetRef> minors = new HashSet<>();
        for (GroupMembership<UserId> minor : group.getMembers()) {
            minors.add(TargetRef.member(minor.memberId().uuid()));
        }
        return minors;
    }
}
