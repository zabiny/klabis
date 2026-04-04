package com.klabis.members.infrastructure;

import com.klabis.members.application.LastOwnershipChecker;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SecondaryAdapter
@Component
class LastOwnershipCheckerAdapter implements LastOwnershipChecker {

    private final FamilyGroupRepository familyGroupRepository;
    private final MembersGroupRepository membersGroupRepository;

    LastOwnershipCheckerAdapter(FamilyGroupRepository familyGroupRepository,
                                MembersGroupRepository membersGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
        this.membersGroupRepository = membersGroupRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<OwnedGroupInfo> findGroupsOwnedSolely(MemberId memberId) {
        List<OwnedGroupInfo> result = new ArrayList<>();

        familyGroupRepository.findByMemberOrParent(memberId)
                .filter(group -> isSoleParent(group, memberId))
                .map(group -> new OwnedGroupInfo(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FamilyGroup.TYPE_DISCRIMINATOR))
                .ifPresent(result::add);

        membersGroupRepository.findGroupsForMember(memberId).stream()
                .filter(group -> group.isLastOwner(memberId))
                .map(group -> new OwnedGroupInfo(
                        group.getId().uuid().toString(),
                        group.getName(),
                        MembersGroup.TYPE_DISCRIMINATOR))
                .forEach(result::add);

        return result;
    }

    private boolean isSoleParent(FamilyGroup group, MemberId memberId) {
        return group.getParents().size() == 1 && group.getParents().contains(memberId);
    }
}
