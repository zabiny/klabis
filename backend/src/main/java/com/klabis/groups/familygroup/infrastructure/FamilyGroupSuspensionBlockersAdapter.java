package com.klabis.groups.familygroup.infrastructure;

import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.MemberOwnedGroupsPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@SecondaryAdapter
@Component
class FamilyGroupSuspensionBlockersAdapter implements MemberOwnedGroupsPort {

    private final FamilyGroupRepository repository;

    FamilyGroupSuspensionBlockersAdapter(FamilyGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OwnedGroup> findGroupsBlockingSuspension(MemberId memberId) {
        return repository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId))
                .filter(group -> group.isLastParent(memberId))
                .map(FamilyGroupSuspensionBlockersAdapter::toOwnedGroup)
                .stream().toList();
    }

    private static OwnedGroup toOwnedGroup(FamilyGroup group) {
        return new OwnedGroup(group.getId().uuid().toString(), group.getName(), FamilyGroup.TYPE_DISCRIMINATOR);
    }
}
