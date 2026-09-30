package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.MemberOwnedGroupsPort;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@SecondaryAdapter
@Component
class LegalGuardianGroupSuspensionBlockersAdapter implements MemberOwnedGroupsPort {

    private final LegalGuardianGroupRepository repository;

    LegalGuardianGroupSuspensionBlockersAdapter(LegalGuardianGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OwnedGroup> findGroupsBlockingSuspension(MemberId memberId) {
        UserId userId = memberId.toUserId();
        return repository.findAll(LegalGuardianGroupFilter.all().withGuardianIs(userId)).stream()
                .filter(group -> group.hasMinors() && group.isLastGuardian(userId))
                .map(LegalGuardianGroupSuspensionBlockersAdapter::toOwnedGroup)
                .toList();
    }

    private static OwnedGroup toOwnedGroup(LegalGuardianGroup group) {
        return new OwnedGroup(group.getId().uuid().toString(), group.getName(), LegalGuardianGroup.TYPE_DISCRIMINATOR);
    }
}
