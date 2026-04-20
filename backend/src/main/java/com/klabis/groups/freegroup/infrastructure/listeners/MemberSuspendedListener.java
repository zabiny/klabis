package com.klabis.groups.freegroup.infrastructure.listeners;

import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspendedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@PrimaryAdapter
@Component
class MemberSuspendedListener {

    private static final Logger log = LoggerFactory.getLogger(MemberSuspendedListener.class);

    static final String SYSTEM_CANCEL_REASON = "Member was deactivated";

    private final FreeGroupRepository freeGroupRepository;

    MemberSuspendedListener(FreeGroupRepository freeGroupRepository) {
        this.freeGroupRepository = freeGroupRepository;
    }

    @ApplicationModuleListener
    void on(MemberSuspendedEvent event) {
        MemberId deactivatedMember = event.memberId();

        List<FreeGroup> groupsWithPendingInvitations = freeGroupRepository
                .findAll(FreeGroupFilter.all().withPendingInvitationFor(deactivatedMember));

        if (groupsWithPendingInvitations.isEmpty()) {
            return;
        }

        log.info("Auto-cancelling pending free-group invitations for deactivated member {}", deactivatedMember);

        groupsWithPendingInvitations.forEach(group -> {
            try {
                group.getPendingInvitations().stream()
                        .filter(inv -> inv.isForUser(deactivatedMember.toUserId()))
                        .forEach(inv -> group.cancelInvitation(inv.getId(), Optional.empty(), SYSTEM_CANCEL_REASON));
                freeGroupRepository.save(group);
            } catch (Exception e) {
                log.error("Failed to auto-cancel invitations in group {} for deactivated member {}: {}",
                        group.getId(), deactivatedMember, e.getMessage(), e);
            }
        });
    }
}
