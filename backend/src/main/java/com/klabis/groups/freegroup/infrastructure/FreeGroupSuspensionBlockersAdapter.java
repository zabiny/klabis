package com.klabis.groups.freegroup.infrastructure;

import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.MemberOwnedGroupsPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@SecondaryAdapter
@Component
class FreeGroupSuspensionBlockersAdapter implements MemberOwnedGroupsPort {

    private final FreeGroupRepository repository;

    FreeGroupSuspensionBlockersAdapter(FreeGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OwnedGroup> findGroupsBlockingSuspension(MemberId memberId) {
        return repository.findAll(FreeGroupFilter.all().withOwnerOrMemberIs(memberId)).stream()
                .filter(group -> group.isLastOwner(memberId))
                .map(FreeGroupSuspensionBlockersAdapter::toOwnedGroup)
                .toList();
    }

    private static OwnedGroup toOwnedGroup(FreeGroup group) {
        return new OwnedGroup(group.getId().uuid().toString(), group.getName(), FreeGroup.TYPE_DISCRIMINATOR);
    }
}
