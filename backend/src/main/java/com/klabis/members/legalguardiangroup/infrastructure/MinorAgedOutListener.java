package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MemberId;
import com.klabis.members.MinorAgedOutEvent;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@PrimaryAdapter
@Component
class MinorAgedOutListener {

    private static final Logger log = LoggerFactory.getLogger(MinorAgedOutListener.class);

    private final LegalGuardianGroupRepository groupRepository;

    MinorAgedOutListener(LegalGuardianGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @ApplicationModuleListener
    void on(MinorAgedOutEvent event) {
        MemberId memberId = event.memberId();
        groupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(memberId.toUserId()))
                .ifPresent(group -> removeFromGroup(group, memberId));
    }

    private void removeFromGroup(LegalGuardianGroup group, MemberId memberId) {
        group.removeMinor(memberId);
        if (group.hasMinors()) {
            groupRepository.save(group);
        } else {
            groupRepository.delete(group.getId());
        }
        log.info("Member {} left legal guardian group {} after turning 18", memberId, group.getId());
    }
}
