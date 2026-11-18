package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.authorization.DelegatedGrants;
import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guardians hold what the legal guardian group delegates over every minor of the group, and ownership of the
 * group itself. Age is deliberately not
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
        List<LegalGuardianGroup> guarded = groups.findAll(LegalGuardianGroupFilter.all().withGuardianIs(userId));
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        grants.putAll(DelegatedGrants.overMembersOf(guarded, UserId::uuid));
        grants.putAll(DelegatedGrants.ownershipOf(guarded.stream().map(group -> group.getId().uuid()).toList()));
        return grants;
    }
}
