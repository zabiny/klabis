package com.klabis.members.infrastructure;

import com.klabis.members.application.LastOwnershipChecker;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.groups.domain.FamilyGroupFilter;
import com.klabis.members.groups.domain.MembersGroupFilter;
import com.klabis.members.groups.domain.TrainingGroupFilter;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupRepository;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
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
    private final TrainingGroupRepository trainingGroupRepository;

    LastOwnershipCheckerAdapter(FamilyGroupRepository familyGroupRepository,
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
