package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MinorAgedOutEvent;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@PrimaryAdapter
@Component
class MinorAgedOutListener {

    private static final Logger log = LoggerFactory.getLogger(MinorAgedOutListener.class);

    private final LegalGuardianGroupPort legalGuardianGroupPort;

    MinorAgedOutListener(LegalGuardianGroupPort legalGuardianGroupPort) {
        this.legalGuardianGroupPort = legalGuardianGroupPort;
    }

    @ApplicationModuleListener
    void on(MinorAgedOutEvent event) {
        legalGuardianGroupPort.removeMinor(event.memberId());
        log.info("Member {} left the legal guardian group after turning 18", event.memberId());
    }
}
