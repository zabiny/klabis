package com.klabis.members.familygroup.infrastructure;

import com.klabis.members.FamilyGroupProvider;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SecondaryAdapter
@Component
class FamilyGroupProviderAdapter implements FamilyGroupProvider {

    private final FamilyGroupRepository familyGroupRepository;

    FamilyGroupProviderAdapter(FamilyGroupRepository familyGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FamilyGroupData> findFamilyGroupForMember(MemberId memberId) {
        return familyGroupRepository.findByMemberOrParent(memberId)
                .map(group -> new FamilyGroupData(group.getId().uuid()));
    }
}
