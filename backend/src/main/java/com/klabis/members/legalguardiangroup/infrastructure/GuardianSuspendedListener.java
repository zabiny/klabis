package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MemberSuspendedEvent;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * A suspended member must not stay on the guardian list of a group that has other guardians. A group whose sole
 * guardian is suspended is left alone: its minors are suspended with them or a successor was set beforehand.
 * Resuming the member does not bring the link back.
 */
@PrimaryAdapter
@Component
class GuardianSuspendedListener {

    private static final Logger log = LoggerFactory.getLogger(GuardianSuspendedListener.class);

    private final LegalGuardianGroupPort legalGuardianGroupPort;

    GuardianSuspendedListener(LegalGuardianGroupPort legalGuardianGroupPort) {
        this.legalGuardianGroupPort = legalGuardianGroupPort;
    }

    @ApplicationModuleListener
    void on(MemberSuspendedEvent event) {
        legalGuardianGroupPort.removeGuardianFromGroupsWithOtherGuardians(event.memberId().toUserId());
        log.debug("Suspended member {} removed from guardians of groups with other guardians", event.memberId());
    }
}
