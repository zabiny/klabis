package com.klabis.groups.application;

import com.klabis.groups.LastOwnershipChecker;
import com.klabis.groups.common.domain.FamilyGroupFilter;
import com.klabis.groups.common.domain.MembersGroupFilter;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.membersgroup.domain.MembersGroup;
import com.klabis.groups.membersgroup.domain.MembersGroupRepository;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SecondaryAdapter
@Component
class LastOwnershipCheckerImpl implements LastOwnershipChecker {

    private final FamilyGroupRepository familyGroupRepository;
    private final MembersGroupRepository membersGroupRepository;
    private final TrainingGroupRepository trainingGroupRepository;

    LastOwnershipCheckerImpl(FamilyGroupRepository familyGroupRepository,
                             MembersGroupRepository membersGroupRepository,
                             TrainingGroupRepository trainingGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
        this.membersGroupRepository = membersGroupRepository;
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<OwnedGroupInfo> findGroupsOwnedSolely(MemberId memberId) {
        List<OwnedGroupInfo> result = new ArrayList<>();

        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId))
                .filter(group -> group.isLastParent(memberId))
                .map(group -> new OwnedGroupInfo(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FamilyGroup.TYPE_DISCRIMINATOR))
                .ifPresent(result::add);

        membersGroupRepository.findAll(MembersGroupFilter.all().withOwnerOrMemberIs(memberId)).stream()
                .filter(group -> group.isLastOwner(memberId))
                .map(group -> new OwnedGroupInfo(
                        group.getId().uuid().toString(),
                        group.getName(),
                        MembersGroup.TYPE_DISCRIMINATOR))
                .forEach(result::add);

        trainingGroupRepository.findAll(TrainingGroupFilter.all().withTrainerIs(memberId)).stream()
                .filter(group -> group.isLastTrainer(memberId))
                .map(group -> new OwnedGroupInfo(
                        group.getId().uuid().toString(),
                        group.getName(),
                        TrainingGroup.TYPE_DISCRIMINATOR))
                .forEach(result::add);

        return result;
    }
}
