package com.klabis.membershipfees.infrastructure;

import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@PrimaryAdapter
class MembershipFeeEventListener {

    private static final Logger log = LoggerFactory.getLogger(MembershipFeeEventListener.class);

    private final MemberRegistrationSanctionPort sanctionPort;

    MembershipFeeEventListener(MemberRegistrationSanctionPort sanctionPort) {
        this.sanctionPort = sanctionPort;
    }

    @ApplicationModuleListener
    public void handle(MemberMissedFeeSelectionEvent event) {
        log.info("Received MemberMissedFeeSelectionEvent for member {} (year {})",
                event.memberId(), event.year());
        sanctionPort.applyMissedSelectionSanction(event.memberId());
    }

    @ApplicationModuleListener
    public void handle(MemberFeeSelectionResolvedEvent event) {
        log.info("Received MemberFeeSelectionResolvedEvent for member {} (year {}), lifting registration block",
                event.memberId(), event.year());
        sanctionPort.unblockMember(event.memberId());
    }
}
