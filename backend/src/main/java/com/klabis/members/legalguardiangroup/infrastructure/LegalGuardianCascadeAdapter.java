package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MemberId;
import com.klabis.members.application.LegalGuardianCascadePort;
import com.klabis.members.application.NotSoleGuardianGroupException;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@SecondaryAdapter
@Component
class LegalGuardianCascadeAdapter implements LegalGuardianCascadePort {

    private final LegalGuardianGroupRepository repository;

    LegalGuardianCascadeAdapter(LegalGuardianGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MemberId> minorsOfSoleGuardianGroups(MemberId guardian, Set<LegalGuardianGroupId> groupIds) {
        return groupIds.stream()
                .map(id -> repository.findById(id)
                        .filter(group -> group.isLastGuardian(guardian.toUserId()))
                        .orElseThrow(() -> new NotSoleGuardianGroupException(id)))
                .flatMap(group -> group.getMinors().stream())
                .map(minor -> minor.memberId())
                .distinct()
                .toList();
    }
}
